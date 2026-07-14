package com.drypted.pvpessentials.client.mixin;

import com.drypted.dlib.client.config.ConfigManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    private boolean pvpessentials$shouldCancelExplosionParticle(ParticleOptions particle) {
        if (particle.getType() == ParticleTypes.EXPLOSION || 
            particle.getType() == ParticleTypes.EXPLOSION_EMITTER ||
            particle.getType() == ParticleTypes.POOF ||
            particle.getType() == ParticleTypes.FLASH ||
            particle.getType() == ParticleTypes.SMOKE ||
            particle.getType() == ParticleTypes.LARGE_SMOKE) {
            
            var opt = ConfigManager.getOption("pvpessentials+explosion+disable_explosions");
            return opt != null && Boolean.parseBoolean(opt.value);
        }
        return false;
    }

    // 1. Intercept standard particle additions
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void pvpessentials$cancelStandardParticles(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (pvpessentials$shouldCancelExplosionParticle(particleData)) ci.cancel();
    }

    // 2. Intercept always-visible particle additions
    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void pvpessentials$cancelAlwaysVisibleParticles(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (pvpessentials$shouldCancelExplosionParticle(particleData)) ci.cancel();
    }
}