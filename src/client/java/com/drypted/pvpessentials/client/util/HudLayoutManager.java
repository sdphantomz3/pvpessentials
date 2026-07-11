package com.drypted.pvpessentials.client.util;

import net.minecraft.client.Minecraft;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HudLayoutManager {

    public enum Anchor {
        HOTBAR_LEFT,
        HOTBAR_RIGHT,
        SCREEN_LEFT,
        SCREEN_RIGHT
    }

    private static final int EDGE_MARGIN = 10;
    private static final int HOTBAR_HALF = 91;
    private static final int OFFHAND_GAP = 7;
    private static final int GAP = 3;

    private static long lastGameTime = -1;
    private static final Map<String, HudDecl> declarations = new LinkedHashMap<>();
    private static final Map<String, int[]> resolved = new LinkedHashMap<>();
    private static boolean resolvedThisFrame = false;

    private static final Map<Anchor, Zone> zones = new EnumMap<>(Anchor.class);
    private static int lastScreenWidth = -1;

    private record HudDecl(String id, Anchor anchor, int width, int height, int verticalOffset) {}

    private static final class Zone {
        int startX;       // anchor X for this zone
        int direction;    // +1 = extends right, -1 = extends left
        int totalWidth;   // available horizontal space in px
        int accumX;       // next horizontal slot (reset per resolve)
        int accumY;       // accumulated vertical height (reset per resolve)
        int rowHeight;    // tallest HUD on the current row
    }

    private static void ensureNewFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long t = mc.level.getGameTime();
        if (t != lastGameTime) {
            declarations.clear();
            resolved.clear();
            resolvedThisFrame = false;
            lastGameTime = t;
        }
    }

    private static void ensureZones() {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        if (sw == lastScreenWidth) return;
        lastScreenWidth = sw;

        int mx = sw / 2;
        int hotbarLeft = mx - HOTBAR_HALF - OFFHAND_GAP;
        int hotbarRight = mx + HOTBAR_HALF + OFFHAND_GAP;

        Zone z = new Zone();
        z.startX = hotbarLeft;
        z.direction = -1;
        z.totalWidth = Math.max(1, hotbarLeft - EDGE_MARGIN);
        zones.put(Anchor.HOTBAR_LEFT, z);

        z = new Zone();
        z.startX = hotbarRight;
        z.direction = +1;
        z.totalWidth = Math.max(1, sw - hotbarRight - EDGE_MARGIN);
        zones.put(Anchor.HOTBAR_RIGHT, z);

        z = new Zone();
        z.startX = EDGE_MARGIN;
        z.direction = +1;
        z.totalWidth = Math.max(1, hotbarLeft - EDGE_MARGIN);
        zones.put(Anchor.SCREEN_LEFT, z);

        z = new Zone();
        z.startX = sw - EDGE_MARGIN;
        z.direction = -1;
        z.totalWidth = Math.max(1, sw - hotbarRight - EDGE_MARGIN);
        zones.put(Anchor.SCREEN_RIGHT, z);
    }

    /**
     * How many 20-px item slots fit in {@code anchor}'s zone.
     * Based purely on zone width — stable per screen size, never oscillates.
     */
    public static int getMaxColumns(Anchor anchor, int slotPixelWidth) {
        ensureNewFrame();
        ensureZones();
        Zone z = zones.get(anchor);
        if (z == null) return 1;
        return Math.max(1, (z.totalWidth - 2) / slotPixelWidth);
    }

    public static void register(String hudId, Anchor anchor, int width, int height, int verticalOffset) {
        ensureNewFrame();
        declarations.put(hudId, new HudDecl(hudId, anchor, width, height, verticalOffset));
        resolvedThisFrame = false;
    }

    public static int getX(String hudId) {
        ensureNewFrame();
        resolve();
        int[] p = resolved.get(hudId);
        return p != null ? p[0] : 0;
    }

    public static int getY(String hudId) {
        ensureNewFrame();
        resolve();
        int[] p = resolved.get(hudId);
        return p != null ? p[1] : 0;
    }

    private static void resolve() {
        if (resolvedThisFrame) return;
        resolvedThisFrame = true;
        resolved.clear();
        ensureZones();

        // Reset per-zone accumulators for this frame
        for (Zone z : zones.values()) {
            z.accumX = 0;
            z.accumY = 0;
            z.rowHeight = 0;
        }

        // Process in registration order — deterministic every frame
        for (HudDecl hud : declarations.values()) {
            Zone z = zones.get(hud.anchor);
            if (z == null) continue;

            // If this HUD doesn't fit on the current row, wrap to the next row
            if (z.accumX > 0 && z.accumX + hud.width > z.totalWidth) {
                z.accumX = 0;
                z.accumY += z.rowHeight + GAP;
                z.rowHeight = 0;
            }

            int x = z.accumX;
            int y = z.accumY + hud.verticalOffset;

            z.accumX += hud.width + GAP;
            z.rowHeight = Math.max(z.rowHeight, hud.height);

            resolved.put(hud.id, new int[] { x, y });
        }
    }
}