package com.drypted.pvpessentials.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Manages a sliding error/info popup that renders on top of any screen.
 * Used to show "HUD Layout Editor unavailable in Auto Adjust mode" message.
 */
public class PopupManager {

    private static volatile boolean active = false;
    private static long startTime = 0;
    private static final long DURATION_MS = 3000;

    public static void show() {
        active = true;
        startTime = System.currentTimeMillis();
    }

    public static boolean isActive() {
        if (!active) return false;
        if (System.currentTimeMillis() - startTime > DURATION_MS) {
            active = false;
            return false;
        }
        return true;
    }

    /**
     * Render the sliding popup. Call from any HUD render method.
     * Returns true if the popup was rendered.
     */
    public static boolean render(GuiGraphicsExtractor graphics) {
        if (!isActive()) return false;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        long elapsed = System.currentTimeMillis() - startTime;

        // Slide-in from left (first 300ms)
        float slideProgress = Math.min(1f, elapsed / 300f);
        int targetX = 10;
        int startX = -260;
        int popupX = (int) (startX + (targetX - startX) * slideProgress);

        // Fade out near the end (last 500ms)
        float alpha = 1f;
        if (elapsed > DURATION_MS - 500) {
            alpha = 1f - (float)(elapsed - (DURATION_MS - 500)) / 500f;
        }

        int popupY = screenHeight / 2 - 20;
        int popupW = 250;
        int popupH = 40;

        int bgColor = ((int)(alpha * 0xCC) << 24) | 0xAA0000;
        int borderColor = ((int)(alpha * 0xFF) << 24) | 0xFF4444;
        int textColor = ((int)(alpha * 0xFF) << 24) | 0xFFFFFF;

        // Background
        graphics.fill(popupX, popupY, popupX + popupW, popupY + popupH, bgColor);
        // Border
        graphics.fill(popupX, popupY, popupX + popupW, popupY + 1, borderColor);
        graphics.fill(popupX, popupY + popupH - 1, popupX + popupW, popupY + popupH, borderColor);
        graphics.fill(popupX, popupY, popupX + 1, popupY + popupH, borderColor);
        graphics.fill(popupX + popupW - 1, popupY, popupX + popupW, popupY + popupH, borderColor);

        graphics.centeredText(mc.font, "HUD Layout Editor unavailable", popupX + popupW / 2, popupY + 8, textColor);
        graphics.centeredText(mc.font, "Switch to Set Manually mode first", popupX + popupW / 2, popupY + 22, textColor);

        return true;
    }
}
