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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class PotionHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");
    private static final int MAX_COLS = 4;

    private static final int ELEM_WIDTH = 82;
    private static final int ELEM_HEIGHT = 22;

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static float previewX = 0f;
    public static float previewY = 0f;
    public static Anchor previewAnchor = Anchor.TOP_LEFT;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_POTION_ENABLED);
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
            return Anchor.BOTTOM_RIGHT.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
        }
        float xp = HudLayoutStorage.getX("potion", getDefaultXPercent(screenWidth));
        float yp = HudLayoutStorage.getY("potion", getDefaultYPercent(screenHeight));
        Anchor anchor = HudLayoutStorage.getAnchor("potion", Anchor.BOTTOM_RIGHT);
        return anchor.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
    }

    /**
     * Default X percentage: right side of the hotbar with a 7px gap.
     * Because Anchor is BOTTOM_RIGHT, this calculates the RIGHT edge of the HUD.
     */
    public static float getDefaultXPercent(int screenWidth) {
        // Center + hotbar(91) + gap(7) + HUD width
        return (float) (screenWidth / 2f + 98f + ELEM_WIDTH) / screenWidth;
    }

    /** Default Y percentage: flush with bottom of screen (anchor handles ELEM_HEIGHT offset). */
    public static float getDefaultYPercent(int screenHeight) {
        return 1.0f;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;
        if (previewMode) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) return;

        List<ItemStack> trackedPotions = new ArrayList<>();
        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isPotion(stack)) {
                boolean combined = false;
                for (ItemStack tracked : trackedPotions) {
                    if (ItemStack.isSameItemSameComponents(tracked, stack)) {
                        tracked.setCount(tracked.getCount() + stack.getCount());
                        combined = true;
                        break;
                    }
                }
                if (!combined) {
                    trackedPotions.add(stack.copy());
                }
            }
        }

        if (trackedPotions.isEmpty()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int[] pos = getPosition(screenWidth, screenHeight);
        int startX = pos[0];
        int baseYPos = pos[1];

        renderPotions(graphics, trackedPotions, startX, baseYPos);
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     */
    public static void renderPreview(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
        Player player = mc.player;
        if (player == null) return;

        List<ItemStack> trackedPotions = new ArrayList<>();
        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))) {
                boolean combined = false;
                for (ItemStack tracked : trackedPotions) {
                    if (ItemStack.isSameItemSameComponents(tracked, stack)) {
                        tracked.setCount(tracked.getCount() + stack.getCount());
                        combined = true;
                        break;
                    }
                }
                if (!combined) {
                    trackedPotions.add(stack.copy());
                }
            }
        }

        renderPotions(graphics, trackedPotions, x, y);
    }

    private static void renderPotions(GuiGraphicsExtractor g, List<ItemStack> items, int startX, int baseYPos) {
        int totalItems = items.size();
        int itemsPerRow = Math.min(MAX_COLS, totalItems);
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * 20) + 1;

        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0 : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22 : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            currentY -= srcHeight;
            g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, currentY, 0, srcV, textureWidth, srcHeight, 182, 22);
            g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + textureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
        }
        for (int i = 0; i < totalItems; i++) {
            int rowIndex = i / itemsPerRow;
            int slotOffset = i % itemsPerRow;
            int itemX = startX + 3 + (slotOffset * 20);
            int itemY = (baseYPos + 3) - (rowIndex * 20);
            RenderUtil.drawScaledItemFactor(g, items.get(i), itemX, itemY, 1.0f);
        }
    }

    private boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}