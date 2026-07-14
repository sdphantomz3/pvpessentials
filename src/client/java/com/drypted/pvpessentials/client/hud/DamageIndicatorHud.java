package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class DamageIndicatorHud {

    private static final Identifier HEART_FULL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_CONTAINER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/container.png");

    private static final int ELEM_WIDTH = 30;
    private static final int ELEM_HEIGHT = 14;

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static float previewX = 0f;
    public static float previewY = 0f;
    public static Anchor previewAnchor = Anchor.TOP_LEFT;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_DAMAGE_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private boolean isHeartsMode() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_DAMAGE_DISPLAY_MODE);
        return opt != null && opt.value.equals("Hearts");
    }

    private float getScale() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_DAMAGE_SCALE);
        if (opt != null) {
            try { return Float.parseFloat(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 1.2f;
    }

    private int getLifetime() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_DAMAGE_LIFETIME);
        if (opt != null) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 22;
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
        float xp = HudLayoutStorage.getX("damage", getDefaultXPercent(screenWidth));
        float yp = HudLayoutStorage.getY("damage", getDefaultYPercent(screenHeight));
        Anchor anchor = HudLayoutStorage.getAnchor("damage", Anchor.TOP_LEFT);
        return anchor.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
    }

    /**
     * Default X percentage: on the left side of the crosshair.
     * Position the right edge of the damage indicator just to the left of center.
     */
    public static float getDefaultXPercent(int screenWidth) {
        return (float) (screenWidth / 2f - ELEM_WIDTH - 4f) / screenWidth;
    }

    /** Default Y percentage: centered vertically near crosshair. */
    public static float getDefaultYPercent(int screenHeight) {
        return (float) (screenHeight / 2f - ELEM_HEIGHT / 2f) / screenHeight;
    }

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;
        if (previewMode) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int[] pos = getPosition(screenWidth, screenHeight);
        int baseX = pos[0];
        int baseY = pos[1];

        renderDamageGraphics(graphics, baseX, baseY, getScale(), getLifetime(), isHeartsMode());
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     * Renders a static sample indicator so the user can see positioning.
     */
    public static void renderPreview(GuiGraphics graphics, Minecraft mc, int x, int y) {
        // Draw a static sample indicator for preview
        float scale = 1.2f;

        // Sample taken damage indicator
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        // Render heart icon
        graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);

        // Render sample text
        graphics.drawString(mc.font, "2.0", 10, 0, 0xFF5555, true);

        graphics.pose().popMatrix();
    }

    private static void renderDamageGraphics(GuiGraphics g, int baseX, int baseY, float scale, int maxAge, boolean heartsMode) {
        for (DamageTracker.IndicatorInstance ind : DamageTracker.getActiveIndicators()) {
            ind.maxAgeTicks = maxAge;
            float progress = (float) ind.currentAgeTicks / ind.maxAgeTicks;
            float alpha = 1.0f - progress;
            int driftY = (int) (progress * 24);

            int renderX = baseX + (int) ind.horizontalSpawnOffset;
            int renderY = baseY - driftY + (int) ind.verticalSpawnOffset;

            float displayAmount = ind.amount;
            if (heartsMode) displayAmount /= 2.0f;
            String hitText = String.format("%.1f", displayAmount);

            g.pose().pushMatrix();
            g.pose().translate(renderX, renderY);
            g.pose().scale(scale, scale);

            g.pose().pushMatrix();
            g.pose().translate(-7.5f, 0.5f);
            float rotationRadians = (float) Math.toRadians(ind.rotationDegrees);
            g.pose().rotate(rotationRadians);
            g.pose().translate(-4.5f, -4.5f);

            if (ind.isDamageTaken) {
                g.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            } else {
                g.blit(RenderPipelines.GUI_TEXTURED, HEART_FULL_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            }
            g.pose().popMatrix();

            int textColor = ind.isDamageTaken ? 0xFF5555 : 0xFFFFFF;
            RenderUtil.drawScaledText(g, hitText, 0.75f, 0, 0, textColor, alpha, true);

            g.pose().popMatrix();
        }
    }
}