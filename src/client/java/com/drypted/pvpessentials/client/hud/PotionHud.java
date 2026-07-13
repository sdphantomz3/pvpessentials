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

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static int previewX = 0;
    public static int previewY = 0;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_POTION_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private int getPosX(int screenWidth) {
        if (previewMode) return previewX;
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_POTION_X);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        // Default: right of hotbar
        return screenWidth / 2 + 91 + 7;
    }

    private int getPosY(int screenHeight) {
        if (previewMode) return previewY;
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_POTION_Y);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        // Default: bottom of screen
        return screenHeight - 22;
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
        int startX = getPosX(screenWidth);
        int baseYPos = getPosY(screenHeight);

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