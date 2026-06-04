package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.util.RenderUtil;
import com.mojang.math.Axis; // Required for 1.21+ rotations
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;

public class DamageIndicatorHud {

    // --- CONFIGURABLE SCALE ---
    // 1.5f scales the 9x9 heart sprite to ~13.5px, roughly matching your 16x16 arrow icons.
    private static final float CONFIG_SCALE = 1.2f;

    private static final ResourceLocation HEART_FULL_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final ResourceLocation HEART_CONTAINER_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/container.png");

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
            graphics.pose().pushPose(); 
            // 1.21.1 requires the Z-axis parameter (0.0f for standard 2D translation)
            graphics.pose().translate(renderX, renderY, 0.0f);
            // 1.21.1 requires the Z-axis parameter (1.0f for standard 2D scaling)
            graphics.pose().scale(CONFIG_SCALE, CONFIG_SCALE, 1.0f);

            // --- RENDER SLIGHTLY TILTED HEART SPRITE ---
            graphics.pose().pushPose(); 
            
            // Origin is now local due to outer stack translation
            graphics.pose().translate(-7.5f, 0.5f, 0.0f);
            
            // 1.21.1 uses mulPose and requires defining the axis (Axis.ZP for 2D screen rotation)
            float rotationRadians = (float) Math.toRadians(ind.rotationDegrees);
            graphics.pose().mulPose(Axis.ZP.rotation(rotationRadians));
            
            graphics.pose().translate(-4.5f, -4.5f, 0.0f);

            if (ind.isDamageTaken) {
                graphics.blit(RenderType::guiTextured, HEART_CONTAINER_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            } else {
                graphics.blit(RenderType::guiTextured, HEART_FULL_TEXTURE, 0, 0, 0, 0, 9, 9, 9, 9);
            }
            
            graphics.pose().popPose(); 

            // --- RENDER UNROTATED STABLE TEXT NEXT TO IT ---
            // Rendered at local (0,0) as the parent stack controls scaling and position seamlessly
            if (ind.isDamageTaken) {
                RenderUtil.drawScaledText(graphics, hitText, 0.75f, 0, 0, 0xFF5555, alpha, true);
            } else {
                RenderUtil.drawScaledText(graphics, hitText, 0.75f, 0, 0, 0xFFFFFF, alpha, true);
            }

            // Close global translation + scale stack
            graphics.pose().popPose(); 
        }
    }
}