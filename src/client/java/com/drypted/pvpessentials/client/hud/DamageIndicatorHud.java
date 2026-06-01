package com.drypted.pvpessentials.client.hud;

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

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        for (DamageTracker.IndicatorInstance ind : DamageTracker.getActiveIndicators()) {
            float progress = (float) ind.currentAgeTicks / ind.maxAgeTicks;
            float alpha = 1.0f - progress; 

            int driftY = (int) (progress * 24);
            
            // --- SIDE SPLIT CALCULATIONS ---
            int renderX;
            if (ind.isDamageTaken) {
                // Damage Taken: Shifted to the right side of the crosshair
                renderX = centerX + 20 + (int) ind.horizontalSpawnOffset;
            } else {
                // Damage Given: Shifted to the left side of the crosshair
                renderX = centerX - 28 + (int) ind.horizontalSpawnOffset;
            }
            
            int renderY = centerY - 4 - driftY + (int) ind.verticalSpawnOffset;

            // --- VALUE UNIT CALCULATION ---
            float displayAmount = ind.amount;
            if (DamageTracker.DISPLAY_AS_HEARTS) {
                displayAmount /= 2.0f; 
            }
            String hitText = String.format("%.1f", displayAmount);

            // --- RENDER SLIGHTLY TILTED HEART SPRITE ---
            graphics.pose().pushMatrix();
            
            // Translate origin to center point of the 9x9 heart sprite box
            graphics.pose().translate(renderX - 7.5f, renderY + 0.5f);
            // Apply safe upright rotation degrees
            graphics.pose().rotate(ind.rotationDegrees);
            // Shift back by half dimensions to ensure rotation pivots on center
            graphics.pose().translate(-4.5f, -4.5f);

            if (ind.isDamageTaken) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, HEART_FULL_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            }
            
            graphics.pose().popMatrix();

            // --- RENDER UNROTATED STABLE TEXT NEXT TO IT ---
            if (ind.isDamageTaken) {
                RenderUtil.drawScaledText(graphics, hitText, 0.75f, renderX, renderY, 0xFF5555, alpha, true);
            } else {
                RenderUtil.drawScaledText(graphics, hitText, 0.75f, renderX, renderY, 0xFFFFFF, alpha, true);
            }
        }
    }
}