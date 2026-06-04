package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class DamageIndicatorHud {

    // --- CONFIGURABLE SCALE ---
    // 1.5f scales the 9x9 heart sprite to ~13.5px, roughly matching your 16x16 arrow icons.
    private static final float CONFIG_SCALE = 1.2f;

    private static final Identifier HEART_FULL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_CONTAINER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/container.png");

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
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
            // BOTH are now securely grouped exclusively on the left side of the screen
            int renderX = centerX - 35 + (int) ind.horizontalSpawnOffset;
            int renderY = centerY - 4 - driftY + (int) ind.verticalSpawnOffset;

            // --- VALUE UNIT CALCULATION ---
            float displayAmount = ind.amount;
            if (DamageTracker.DISPLAY_AS_HEARTS) {
                displayAmount /= 2.0f; 
            }
            String hitText = String.format("%.1f", displayAmount);

            // Open global translation + scale stack
            graphics.pose().pushMatrix();
            graphics.pose().translate(renderX, renderY);
            graphics.pose().scale(CONFIG_SCALE, CONFIG_SCALE);

            // --- RENDER SLIGHTLY TILTED HEART SPRITE ---
            graphics.pose().pushMatrix();
            
            // Origin is now local due to outer stack translation
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

            // --- RENDER UNROTATED STABLE TEXT NEXT TO IT ---
            // Rendered at local (0,0) as the parent stack controls scaling and position seamlessly
            if (ind.isDamageTaken) {
                RenderUtil.drawScaledText(graphics, hitText, 0.75f, 0, 0, 0xFF5555, alpha, true);
            } else {
                RenderUtil.drawScaledText(graphics, hitText, 0.75f, 0, 0, 0xFFFFFF, alpha, true);
            }

            // Close global translation + scale stack
            graphics.pose().popMatrix();
        }
    }
}