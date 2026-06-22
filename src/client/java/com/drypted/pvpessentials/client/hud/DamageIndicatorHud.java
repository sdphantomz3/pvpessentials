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

    private boolean isEnabled() {
        return ConfigManager.getBoolean(PVPEssentialsClient.MOD_ID, "Damage Indicator", "Enabled");
    }

    private boolean isHeartsMode() {
        return ConfigManager.getString(PVPEssentialsClient.MOD_ID, "Damage Indicator", "Display Mode").equals("Hearts");
    }

    private float getScale() {
        return (float) ConfigManager.getNumber(PVPEssentialsClient.MOD_ID, "Damage Indicator", "Scale");
    }

    private int getLifetime() {
        return (int) ConfigManager.getNumber(PVPEssentialsClient.MOD_ID, "Damage Indicator", "Lifetime (ticks)");
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        boolean heartsMode = isHeartsMode();
        float scale = getScale();
        int maxAge = getLifetime();

        for (DamageTracker.IndicatorInstance ind : DamageTracker.getActiveIndicators()) {
            // Override lifetime from config
            ind.maxAgeTicks = maxAge;

            float progress = (float) ind.currentAgeTicks / ind.maxAgeTicks;
            float alpha = 1.0f - progress;

            int driftY = (int) (progress * 24);

            int renderX = centerX - 35 + (int) ind.horizontalSpawnOffset;
            int renderY = centerY - 4 - driftY + (int) ind.verticalSpawnOffset;

            float displayAmount = ind.amount;
            if (heartsMode) {
                displayAmount /= 2.0f;
            }
            String hitText = String.format("%.1f", displayAmount);

            graphics.pose().pushMatrix();
            graphics.pose().translate(renderX, renderY);
            graphics.pose().scale(scale, scale);

            // Heart sprite
            graphics.pose().pushMatrix();
            graphics.pose().translate(-7.5f, 0.5f);
            float rotationRadians = (float) Math.toRadians(ind.rotationDegrees);
            graphics.pose().rotate(rotationRadians);
            graphics.pose().translate(-4.5f, -4.5f);

            if (ind.isDamageTaken) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_FULL_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            }
            graphics.pose().popMatrix();

            // Text
            int textColor = ind.isDamageTaken ? 0xFF5555 : 0xFFFFFF;
            RenderUtil.drawScaledText(graphics, hitText, 0.75f, 0, 0, textColor, alpha, true);

            graphics.pose().popMatrix();
        }
    }
}