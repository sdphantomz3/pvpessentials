package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class MiscHud {

    // Helper class to store tracked items and their total counted sizes
    private static class TrackedItem {
        public final ItemStack displayStack;
        public final int totalCount;

        public TrackedItem(Item item, int totalCount) {
            this.displayStack = new ItemStack(item);
            this.totalCount = totalCount;
        }
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.options.hideGui) {
            return;
        }

        // 1. Core items requested to track
        Item[] targetItems = {
            Items.GOLDEN_APPLE,
            Items.ENDER_PEARL,
            Items.COBWEB,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.EXPERIENCE_BOTTLE
        };

        // 2. Tally up totals across the entire inventory (unbound by max stack size limits)
        List<TrackedItem> itemsToRender = new ArrayList<>();
        int containerSize = player.getInventory().getContainerSize();

        for (Item targetItem : targetItems) {
            int totalCount = 0;
            for (int i = 0; i < containerSize; i++) {
                ItemStack slotStack = player.getInventory().getItem(i);
                if (!slotStack.isEmpty() && slotStack.is(targetItem)) {
                    totalCount += slotStack.getCount();
                }
            }
            
            // Only add to rendering queue if the player actually possesses at least one
            if (totalCount > 0) {
                itemsToRender.add(new TrackedItem(targetItem, totalCount));
            }
        }

        // If the player isn't carrying any of these combat supplies, halt rendering
        if (itemsToRender.isEmpty()) {
            return;
        }

        // 3. Coordinate Positioning Math (Anchor next to the Hotbar / Offhand Slot)
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Hotbar center is screenWidth / 2. 
        // Vanilla left-edge of the hotbar is at (centerX - 91). 
        // The offhand slot sits an additional ~29 pixels to the left of the hotbar.
        int centerX = screenWidth / 2;
        int anchorX = centerX - 91 - 29 - 18; // Shift left of the offhand bounds safely
        int anchorY = screenHeight - 22;      // Align baseline height gracefully alongside hotbar slots

        // 4. Render Grid Logic (Max 2 items per row, grows upwards)
        int itemsPerRow = 2;
        int slotSpacingX = 22; // Horizontal spacing between icons
        int slotSpacingY = 20; // Vertical row spacing (pushes rows up as inventory fills)

        for (int i = 0; i < itemsToRender.size(); i++) {
            TrackedItem tracked = itemsToRender.get(i);

            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            // Math layout shifting rows upwards (- row * slotSpacingY)
            int renderX = anchorX + (col * slotSpacingX);
            int renderY = anchorY - (row * slotSpacingY);

            // Render raw item sprite without default vanilla number overlay overlays
            graphics.item(tracked.displayStack, renderX, renderY);

            // Format inventory pool total text
            String countText = String.valueOf(tracked.totalCount);
            
            // Calculate accurate positioning offsets matching standard text decoration profiles
            // Positioned at bottom right quadrant corner relative to the item icon
            int textX = renderX + 17 - minecraft.font.width(countText);
            int textY = renderY + 9;

            // Render clean, shadow-backed text stack limits
            RenderUtil.drawScaledText(graphics, countText, 1.0f, textX, textY, 0xFFFFFF, 1.0f, true);
        }
    }
}