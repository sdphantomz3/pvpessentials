package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class DamageIndicatorHud {

    private static final Identifier HEART_FULL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_CONTAINER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/container.png");

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static int previewX = 0;
    public static int previewY = 0;

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

    private int getPosX(int screenWidth) {
        if (previewMode) return previewX;
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_DAMAGE_X);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        // Default: left of crosshair
        return screenWidth / 2 - 35;
    }

    private int getPosY(int screenHeight) {
        if (previewMode) return previewY;
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_DAMAGE_Y);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        // Default: near crosshair center
        return screenHeight / 2 - 4;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;
        if (previewMode) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int baseX = getPosX(screenWidth);
        int baseY = getPosY(screenHeight);

        renderDamageGraphics(graphics, baseX, baseY, getScale(), getLifetime(), isHeartsMode());
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     * Renders a static sample indicator so the user can see positioning.
     */
    public static void renderPreview(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
        // Draw a static sample indicator for preview
        float scale = 1.2f;

        // Sample taken damage indicator
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        // Render heart icon
        graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);

        // Render sample text
        graphics.text(mc.font, "2.0", 10, 0, 0xFF5555, true);

        graphics.pose().popMatrix();
    }

    private static void renderDamageGraphics(GuiGraphicsExtractor g, int baseX, int baseY, float scale, int maxAge, boolean heartsMode) {
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