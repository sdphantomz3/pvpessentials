package com.drypted.pvpessentials.client.mixin;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ShieldMixin {

    private static float getFloatOption(String key, float defaultValue) {
        var opt = ConfigManager.getOption(key);
        if (opt != null) {
            try { return Float.parseFloat(opt.value); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static boolean getBoolOption(String key, boolean defaultValue) {
        var opt = ConfigManager.getOption(key);
        return opt != null ? Boolean.parseBoolean(opt.value) : defaultValue;
    }

    @Inject(
        method = "renderItem",
        at = @At("HEAD")
    )
    private void modifyShieldRender(
        LivingEntity mob,
        ItemStack itemStack,
        ItemDisplayContext type,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        CallbackInfo ci
    ) {
        // 0. Master kill switch — read from DLib config
        if (!getBoolOption(PVPEssentialsClient.KEY_SHIELD_ENABLED, true)) {
            return;
        }

        // 1. Ensure we only modify the first-person perspective
        if (type == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
            type == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {

            // 2. Check if the item is a shield
            if (itemStack.is(Items.SHIELD)) {

                boolean isLeftHand = (type == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                boolean isBlocking = (mob.isUsingItem() && mob.getUseItem() == itemStack);

                if (isBlocking) {
                    // 3. BLOCKING STATE — read offsets from DLib config
                    float blockX = getFloatOption(PVPEssentialsClient.KEY_SHIELD_BLOCKING_X, 0.0F);
                    float blockY = getFloatOption(PVPEssentialsClient.KEY_SHIELD_BLOCKING_Y, -0.1F);
                    float blockZ = getFloatOption(PVPEssentialsClient.KEY_SHIELD_BLOCKING_Z, 0.0F);
                    float finalBlockX = isLeftHand ? -blockX : blockX;
                    poseStack.translate(finalBlockX, blockY, blockZ);
                } else {
                    // 4. IDLE STATE — read rotation + offsets from DLib config
                    if (getBoolOption(PVPEssentialsClient.KEY_SHIELD_IDLE_ROTATION, true)) {
                        float angle = getFloatOption(PVPEssentialsClient.KEY_SHIELD_IDLE_ROTATION_DEGREES, 90.0F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
                    }

                    float idleX = getFloatOption(PVPEssentialsClient.KEY_SHIELD_IDLE_X, 0.1F);
                    float idleY = getFloatOption(PVPEssentialsClient.KEY_SHIELD_IDLE_Y, -0.15F);
                    float idleZ = getFloatOption(PVPEssentialsClient.KEY_SHIELD_IDLE_Z, -0.2F);
                    float finalIdleX = isLeftHand ? -idleX : idleX;
                    poseStack.translate(finalIdleX, idleY, idleZ);
                }
            }
        }
    }
}