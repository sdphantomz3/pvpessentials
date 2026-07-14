package com.drypted.pvpessentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;

import net.minecraft.client.renderer.ScreenEffectRenderer;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @ModifyArgs(
            method = "buildFireQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;buildSpriteQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFFFI)V"
            )
    )
    private static void pvpessentials$modifyHudFireHeight(Args args) {
        var enabledOpt = ConfigManager.getOption(PVPEssentialsClient.KEY_LOWFIRE_ENABLED);
        boolean enabled = enabledOpt != null && Boolean.parseBoolean(enabledOpt.value);
        if (!enabled) {
            return;
        }

        var offsetOpt = ConfigManager.getOption(PVPEssentialsClient.KEY_LOWFIRE_YOFFSET);
        float offset = 0.3f;
        if (offsetOpt != null) {
            try { 
                offset = Float.parseFloat(offsetOpt.value); 
            } catch (NumberFormatException ignored) {}
        }

        float originalY0 = args.get(4);
        float originalY1 = args.get(6);

        args.set(4, originalY0 - offset); 
        args.set(6, originalY1 - offset);
    }
}