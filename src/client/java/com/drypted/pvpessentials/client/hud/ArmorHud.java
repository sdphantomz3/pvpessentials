package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ArmorHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");
    private static final int SLOT_PX = 20;
    private static final int MAX_COLS = 4;

    private static final int ELEM_WIDTH = MAX_COLS * SLOT_PX + 2;
    private static final int ELEM_HEIGHT = 22;

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static float previewX = 0f;
    public static float previewY = 0f;
    public static Anchor previewAnchor = Anchor.TOP_LEFT;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARMOR_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private boolean startWithHead() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARMOR_START_WITH_HEAD);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private static boolean isAutoAdjust() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_AUTO_ADJUST);
        return opt != null && "Auto Adjust".equals(opt.value);
    }

    private int[] getPosition(int screenWidth, int screenHeight) {
        if (previewMode) {
            return previewAnchor.toPixel(previewX, previewY, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
        }
        if (isAutoAdjust()) {
            float xp = getDefaultXPercent(screenWidth);
            float yp = getDefaultYPercent(screenHeight);
            return Anchor.BOTTOM_LEFT.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
        }
        float xp = HudLayoutStorage.getX("armor", getDefaultXPercent(screenWidth));
        float yp = HudLayoutStorage.getY("armor", getDefaultYPercent(screenHeight));
        Anchor anchor = HudLayoutStorage.getAnchor("armor", Anchor.BOTTOM_LEFT);
        return anchor.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
    }

    /**
     * Default X percentage: adjacent to offhand on left side of hotbar.
     * Subtracts hotbar(91), gap(7), offhand(22), gap(7), and HUD width.
     */
    public static float getDefaultXPercent(int screenWidth) {
        return (float) (screenWidth / 2f - 127f - ELEM_WIDTH) / screenWidth;
    }

    /** Default Y percentage: flush with bottom of screen (anchor handles ELEM_HEIGHT offset). */
    public static float getDefaultYPercent(int screenHeight) {
        return 1.0f;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;
        if (previewMode) return; // In preview mode, renderPreview is called instead

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator())
            return;

        List<ItemStack> armorItems = startWithHead() ? List.of(
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        ) : List.of(
                player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD)
        );

        boolean hasAnyArmor = armorItems.stream().anyMatch(s -> !s.isEmpty());
        if (!hasAnyArmor) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int[] pos = getPosition(screenWidth, screenHeight);
        int startX = pos[0];
        int baseYPos = pos[1];

        renderAt(graphics, armorItems, startX, baseYPos);
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     */
    public static void renderPreview(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
        Player player = mc.player;
        if (player == null) return;

        List<ItemStack> armorItems = List.of(
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        );

        renderAt(graphics, armorItems, x, y);
    }

    private static void renderAt(GuiGraphicsExtractor g, List<ItemStack> items, int startX, int baseYPos) {
        int totalItems = items.size();
        int itemsPerRow = MAX_COLS;
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * SLOT_PX) + 1;

        // Draw hotbar-style background rows
        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0
                    : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22
                    : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            currentY -= srcHeight;
            g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE,
                    startX, currentY, 0, srcV, textureWidth, srcHeight, 182, 22);
            g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE,
                    startX + textureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
        }

        // Render items
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            int rowIndex = i / itemsPerRow;
            int slotOffset = i % itemsPerRow;
            int itemX = startX + 3 + (slotOffset * SLOT_PX);
            int itemY = (baseYPos + 3) - (rowIndex * SLOT_PX);
            RenderUtil.drawScaledItemFactor(g, stack, itemX, itemY, 1.0f);
        }
    }
}