package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    private static final int ELEM_WIDTH = 30;
    private static final int ELEM_HEIGHT = 18;

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static float previewX = 0f;
    public static float previewY = 0f;
    public static Anchor previewAnchor = Anchor.TOP_LEFT;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARROW_ENABLED);
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
            return Anchor.TOP_LEFT.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
        }
        float xp = HudLayoutStorage.getX("arrow", getDefaultXPercent(screenWidth));
        float yp = HudLayoutStorage.getY("arrow", getDefaultYPercent(screenHeight));
        Anchor anchor = HudLayoutStorage.getAnchor("arrow", Anchor.TOP_LEFT);
        return anchor.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
    }

    /**
     * Default X percentage: on the right side of the crosshair.
     * Position the left edge of the arrow HUD with a comfortable gap from center.
     */
    public static float getDefaultXPercent(int screenWidth) {
        return (float) (screenWidth / 2f + 16f) / screenWidth;
    }

    /** Default Y percentage: centered vertically. */
    public static float getDefaultYPercent(int screenHeight) {
        return (float) (screenHeight / 2f - ELEM_HEIGHT / 2f) / screenHeight;
    }

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;
        if (previewMode) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.options.hideGui) return;

        ItemStack weaponStack = getWeaponStack(player);
        if (weaponStack.isEmpty()) return;

        updateCache(minecraft, player, weaponStack);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int[] pos = getPosition(screenWidth, screenHeight);
        int renderX = pos[0];
        int renderY = pos[1];

        renderArrow(graphics, renderX, renderY);
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     */
    public static void renderPreview(GuiGraphics graphics, Minecraft mc, int x, int y) {
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

    private static void renderArrow(GuiGraphics g, int renderX, int renderY) {
        g.renderItem(cachedProjectile, renderX, renderY);
        String countText = cachedIsCreative ? "\u221E" : String.valueOf(cachedAmmoCount);
        int textX = renderX + 18;
        int textY = renderY + 4;
        RenderUtil.drawScaledText(g, countText, 1.0f, textX, textY, 0xFFFFFF, 1.0f, true);
    }
}