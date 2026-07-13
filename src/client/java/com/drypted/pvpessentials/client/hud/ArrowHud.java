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

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static int previewX = 0;
    public static int previewY = 0;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARROW_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private int getPosX(int screenWidth) {
        if (previewMode) return previewX;
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_ARROW_X);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        // Default: right of crosshair
        return screenWidth / 2 + 12;
    }

    private int getPosY(int screenHeight) {
        if (previewMode) return previewY;
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_ARROW_Y);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        // Default: near crosshair center
        return screenHeight / 2 - 8;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;
        if (previewMode) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) return;

        ItemStack weaponStack = getWeaponStack(player);
        if (weaponStack.isEmpty()) return;

        updateCache(minecraft, player, weaponStack);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int renderX = getPosX(screenWidth);
        int renderY = getPosY(screenHeight);

        renderArrow(graphics, renderX, renderY);
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     */
    public static void renderPreview(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
        Player player = mc.player;
        if (player == null) return;

        ItemStack weaponStack = getWeaponStack(player);
        if (weaponStack.isEmpty()) return;

        updateCache(mc, player, weaponStack);
        renderArrow(graphics, x, y);
    }

    private static ItemStack getWeaponStack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.getItem() instanceof ProjectileWeaponItem) {
            return mainHand;
        } else if (offHand.getItem() instanceof ProjectileWeaponItem) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private static void updateCache(Minecraft minecraft, Player player, ItemStack weaponStack) {
        long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : -1;
        if (gameTime == lastGameTime) return;

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

    private static void renderArrow(GuiGraphicsExtractor g, int renderX, int renderY) {
        g.item(cachedProjectile, renderX, renderY);
        String countText = cachedIsCreative ? "\u221E" : String.valueOf(cachedAmmoCount);
        int textX = renderX + 18;
        int textY = renderY + 4;
        RenderUtil.drawScaledText(g, countText, 1.0f, textX, textY, 0xFFFFFF, 1.0f, true);
    }
}