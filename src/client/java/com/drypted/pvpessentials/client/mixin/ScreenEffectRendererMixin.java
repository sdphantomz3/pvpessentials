package com.drypted.pvpessentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;

import net.minecraft.client.renderer.ScreenEffectRenderer;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @ModifyArg(
            method = "renderFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V" // Changed from (DDD)V to (FFF)V
            ),
            index = 1
    )
    private static float pvpessentials$modifyHudFireHeight(float y) { // Changed double to float
        var enabledOpt = ConfigManager.getOption(PVPEssentialsClient.KEY_LOWFIRE_ENABLED);
        boolean enabled = enabledOpt != null && Boolean.parseBoolean(enabledOpt.value);
        if (!enabled) {
            return y;
        }

        var offsetOpt = ConfigManager.getOption(PVPEssentialsClient.KEY_LOWFIRE_YOFFSET);
        float offset = 0.3f;
        if (offsetOpt != null) {
            try {
                offset = Float.parseFloat(offsetOpt.value);
            } catch (NumberFormatException ignored) {}
        }

        return y - offset; // y and offset are both floats
    }
}