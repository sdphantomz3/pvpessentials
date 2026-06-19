package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class PotionHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");
    
    // Globally exposed to allow MiscHud to properly offset without overlapping
    public static int renderedWidth = 0;
    public static int startX = 0;

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) {
            renderedWidth = 0;
            return;
        }

        // 1. Combine matching potions across the entire inventory
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

        if (trackedPotions.isEmpty()) {
            renderedWidth = 0;
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

        int middleX = screenWidth / 2;
        int baseYPos = screenHeight - 22;

        // 2. Dynamic auto-shrink columns based on side boundaries
        int itemsPerRow = 4;
        int maxAvailableWidth = isRightHanded ? (screenWidth - (middleX + 91 + 7)) : (middleX - 91 - 7);
        
        while (itemsPerRow > 1 && ((itemsPerRow * 20) + 2) > maxAvailableWidth) {
            itemsPerRow--;
        }

        int totalItems = trackedPotions.size();
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * 20) + 1;
        renderedWidth = textureWidth + 1;

        if (isRightHanded) {
            startX = middleX + 91 + 7;
        } else {
            startX = (middleX - 91 - 7) - renderedWidth;
        }

        // 3. Render hotbar sliced grid backdrops
        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0 : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22 : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            
            currentY -= srcHeight;
            int rowY = currentY;

            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, rowY, 0, srcV, textureWidth, srcHeight, 182, 22);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + textureWidth, rowY, 181, srcV, 1, srcHeight, 182, 22);
        }

        // 4. Render items (Count overlay drawn natively via item factor)
        for (int i = 0; i < totalItems; i++) {
            ItemStack potionStack = trackedPotions.get(i);
            int rowIndex = i / itemsPerRow;
            int slotOffsetIndex = i % itemsPerRow;
            
            int itemX = startX + 3 + (slotOffsetIndex * 20);
            int itemY = (baseYPos + 3) - (rowIndex * 20);

            // Using vanilla item rendering with combined total counts
            RenderUtil.drawScaledItemFactor(graphics, potionStack, itemX, itemY, 1.0f);
        }
    }

    private boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}