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
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;buildSpriteQuad"
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
            try { offset = Float.parseFloat(offsetOpt.value); } catch (NumberFormatException ignored) {}
        }

        // The original mixin used offset to modify the fire quad height.
        // Since the original code did not actually use the offset value (commented out),
        // we keep the same logic but now we have the value.
        // If you need to apply the offset, you would modify args here.
        // Currently the original code just returned if disabled.
        // We'll just leave it as a placeholder.
        // (The original code had a commented-out `offset` usage.)
    }
}