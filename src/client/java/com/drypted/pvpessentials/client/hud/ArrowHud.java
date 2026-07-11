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

    private static long lastGameTime = -1;
    private static ItemStack cachedProjectile = ItemStack.EMPTY;
    private static int cachedAmmoCount = 0;
    private static boolean cachedIsCreative = false;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARROW_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private int getHorizontalOffset() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARROW_HORIZONTAL_OFFSET);
        if (opt != null) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 12;
    }

    private int getVerticalOffset() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARROW_VERTICAL_OFFSET);
        if (opt != null) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 0;
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

        long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : -1;
        if (gameTime != lastGameTime) {
            lastGameTime = gameTime;
            cachedProjectile = player.getProjectile(weaponStack);
            if (cachedProjectile.isEmpty()) {
                cachedProjectile = new ItemStack(Items.ARROW);
            }
            cachedIsCreative = player.isCreative();

            cachedAmmoCount = 0;
            int containerSize = player.getInventory().getContainerSize();
            for (int i = 0; i < containerSize; i++) {
                ItemStack slotStack = player.getInventory().getItem(i);
                if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, cachedProjectile)) {
                    cachedAmmoCount += slotStack.getCount();
                }
            }
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int renderX = centerX + getHorizontalOffset();
        int renderY = centerY - 8 + getVerticalOffset();

        graphics.item(cachedProjectile, renderX, renderY);

        String countText = cachedIsCreative ? "\u221E" : String.valueOf(cachedAmmoCount);
        int textX = renderX + 18;
        int textY = centerY - 4 + getVerticalOffset();
        RenderUtil.drawScaledText(graphics, countText, 1.0f, textX, textY, 0xFFFFFF, 1.0f, true);
    }
}