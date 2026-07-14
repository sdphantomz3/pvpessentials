package com.drypted.pvpessentials.client.mixin;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(LivingEntity.class)
public class NoPotionParticlesMixin {

    @ModifyVariable(
        method = "tickEffects",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0
    )
    private List<ParticleOptions> replaceParticles(List<ParticleOptions> original) {
        if ((Object) this instanceof LocalPlayer) {
            var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_POTION_PARTICLES_ENABLED);
            boolean enabled = opt == null || Boolean.parseBoolean(opt.value);
            if (!enabled) {
                return List.of();
            }
        }
        return original;
    }
}