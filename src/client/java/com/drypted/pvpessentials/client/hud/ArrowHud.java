package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;

public class ArrowHud {

    private boolean isEnabled() {
        return ConfigManager.getBoolean(PVPEssentialsClient.MOD_ID, "Arrow HUD", "Enabled");
    }

    private int getHorizontalOffset() {
        return (int) ConfigManager.getNumber(PVPEssentialsClient.MOD_ID, "Arrow HUD", "Horizontal Offset");
    }

    private int getVerticalOffset() {
        return (int) ConfigManager.getNumber(PVPEssentialsClient.MOD_ID, "Arrow HUD", "Vertical Offset");
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack weaponStack = ItemStack.EMPTY;

        if (mainHand.getItem() instanceof ProjectileWeaponItem) {
            weaponStack = mainHand;
        } else if (offHand.getItem() instanceof ProjectileWeaponItem) {
            weaponStack = offHand;
        }

        if (weaponStack.isEmpty()) return;

        ItemStack projectileStack = player.getProjectile(weaponStack);
        if (projectileStack.isEmpty()) {
            projectileStack = new ItemStack(Items.ARROW);
        }

        int totalAmmoCount = 0;
        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, projectileStack)) {
                totalAmmoCount += slotStack.getCount();
            }
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int renderX = centerX + getHorizontalOffset();
        int renderY = centerY - 8 + getVerticalOffset();

        graphics.item(projectileStack, renderX, renderY);

        String countText = player.isCreative() ? "∞" : String.valueOf(totalAmmoCount);
        int textX = renderX + 18;
        int textY = centerY - 4 + getVerticalOffset();
        RenderUtil.drawScaledText(graphics, countText, 1.0f, textX, textY, 0xFFFFFF, 1.0f, true);
    }
}