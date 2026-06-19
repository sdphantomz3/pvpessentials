package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;

public class ArrowHud {

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) {
            return;
        }

        // 1. Check if the player is holding a projectile weapon (Bow / Crossbow)
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack weaponStack = ItemStack.EMPTY;

        if (mainHand.getItem() instanceof ProjectileWeaponItem) {
            weaponStack = mainHand;
        } else if (offHand.getItem() instanceof ProjectileWeaponItem) {
            weaponStack = offHand;
        }

        // If no ranged weapon is held, do not render anything
        if (weaponStack.isEmpty()) {
            return;
        }

        // 2. Resolve what projectile the weapon is currently set to fire
        ItemStack projectileStack = player.getProjectile(weaponStack);
        
        // If out of ammo completely, fallback to a standard arrow icon to display a '0' count
        if (projectileStack.isEmpty()) {
            projectileStack = new ItemStack(Items.ARROW);
        }

        // 3. Count total matching ammunition pool across the entire player inventory
        int totalAmmoCount = 0;
        int containerSize = player.getInventory().getContainerSize();
        
        for (int i = 0; i < containerSize; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            // Match exact item types and components (handles tipped arrows, fireworks, etc.)
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, projectileStack)) {
                totalAmmoCount += slotStack.getCount();
            }
        }

        // 4. Calculate alignment position relative to the center crosshair
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Position the icon 12 pixels right of the crosshair, vertically centered
        int renderX = centerX + 12;
        int renderY = centerY - 8;

        // 5. Render the raw item sprite WITHOUT vanilla decorations/durability bars
        graphics.item(projectileStack, renderX, renderY);

        // 6. Draw the text count using your RenderUtil engine
        String countText = player.isCreative() ? "∞" : String.valueOf(totalAmmoCount);
        int textX = renderX + 18; // Place text directly to the right of the 16x16 icon
        int textY = centerY - 4;  // Mathematically centers the text line with the crosshair

        // White color (0xFFFFFF) with full opacity (1.0f) and a crisp drop-shadow
        RenderUtil.drawScaledText(graphics, countText, 1.0f, textX, textY, 0xFFFFFF, 1.0f, true);
    }
}