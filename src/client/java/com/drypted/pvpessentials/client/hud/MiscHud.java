package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class MiscHud {

    private static final ResourceLocation HOTBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.options.hideGui) {
            return;
        }

        Item[] targetItems = {
            Items.GOLDEN_APPLE,
            Items.ENDER_PEARL,
            Items.COBWEB,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.EXPERIENCE_BOTTLE
        };

        // 1. Tally items across inventory
        List<ItemStack> itemsToRender = new ArrayList<>();
        int containerSize = player.getInventory().getContainerSize();

        for (Item targetItem : targetItems) {
            int totalCount = 0;
            for (int i = 0; i < containerSize; i++) {
                ItemStack slotStack = player.getInventory().getItem(i);
                if (!slotStack.isEmpty() && slotStack.is(targetItem)) {
                    totalCount += slotStack.getCount();
                }
            }
            if (totalCount > 0) {
                ItemStack displayStack = new ItemStack(targetItem);
                // Force raw count past 64 directly into item's stack size container
                displayStack.setCount(totalCount);
                itemsToRender.add(displayStack);
            }
        }

        if (itemsToRender.isEmpty()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

        int middleX = screenWidth / 2;
        int baseYPos = screenHeight - 22;

        // 2. Chained calculation logic pushing from Potion Hud
        int startX;
        int maxAvailableWidth;

        if (isRightHanded) {
            if (PotionHud.renderedWidth > 0) {
                startX = PotionHud.startX + PotionHud.renderedWidth + 7;
            } else {
                startX = middleX + 91 + 7;
            }
            maxAvailableWidth = screenWidth - startX;
        } else {
            int rightBoundary;
            if (PotionHud.renderedWidth > 0) {
                rightBoundary = PotionHud.startX - 7;
            } else {
                rightBoundary = middleX - 91 - 7;
            }
            maxAvailableWidth = rightBoundary;
            startX = 0; // Derived below dynamically based on auto-shrink sizing
        }

        // 3. Auto-Shrink columns if screen space runs out
        int itemsPerRow = 2;
        while (itemsPerRow > 1 && ((itemsPerRow * 20) + 2) > maxAvailableWidth) {
            itemsPerRow--;
        }

        int totalItems = itemsToRender.size();
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * 20) + 1;
        int finalHudWidth = textureWidth + 1;

        if (!isRightHanded) {
            startX = maxAvailableWidth - finalHudWidth;
        }

        if (startX < 0 || (startX + finalHudWidth) > screenWidth) {
            return;
        }

        // 4. Render backdrops
        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0 : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22 : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            
            currentY -= srcHeight;
            int rowY = currentY;

            graphics.blit(HOTBAR_TEXTURE, startX, rowY, 0, srcV, textureWidth, srcHeight, 182, 22);
            graphics.blit(HOTBAR_TEXTURE, startX + textureWidth, rowY, 181, srcV, 1, srcHeight, 182, 22);
        }

        // 5. Render active items
        for (int i = 0; i < totalItems; i++) {
            ItemStack stack = itemsToRender.get(i);
            int rowIndex = i / itemsPerRow;
            int slotOffsetIndex = i % itemsPerRow;
            
            int itemX = startX + 3 + (slotOffsetIndex * 20);
            int itemY = (baseYPos + 3) - (rowIndex * 20);

            // Render via standard item utilities, displaying the count overlay identically to standard stack counts
            RenderUtil.drawScaledItemFactor(graphics, stack, itemX, itemY, 1.0f);
        }
    }
}