package com.drypted.pvpessentials.client.handler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import java.util.*;

public class DamageTracker {

    // Set to true to show hearts (2 HP = 1 Heart). Set to false to show raw HP values.
    public static final boolean DISPLAY_AS_HEARTS = true;

    public static class IndicatorInstance {
        public final boolean isDamageTaken; 
        public final float amount;
        public int currentAgeTicks = 0;
        public int maxAgeTicks = 22; 
        public final double horizontalSpawnOffset;
        public final double verticalSpawnOffset;
        public final float rotationDegrees; 

        public IndicatorInstance(boolean isDamageTaken, float amount) {
            this.isDamageTaken = isDamageTaken;
            this.amount = amount;
            this.horizontalSpawnOffset = (Math.random() - 0.5) * 14;
            this.verticalSpawnOffset = (Math.random() - 0.5) * 10;
            this.rotationDegrees = -25.0f + (float) (Math.random() * 50.0); 
        }
    }

    private static final List<IndicatorInstance> ACTIVE_INDICATORS = new ArrayList<>();
    private static final Map<UUID, Float> CACHED_ENTITY_HEALTH = new HashMap<>();
    private static final Map<UUID, Long> RECENTLY_MELEE_ATTACKED = new HashMap<>();
    private static final Map<UUID, Long> RECENTLY_PROJECTILE_ATTACKED = new HashMap<>();
    private static float cachedPlayerHealth = -1.0f;
    private static boolean wasSwingingLastTick = false;

    public static List<IndicatorInstance> getActiveIndicators() {
        return ACTIVE_INDICATORS;
    }

    private static float getTotalHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Player player = client.player;
            if (player == null || client.level == null) {
                ACTIVE_INDICATORS.clear();
                CACHED_ENTITY_HEALTH.clear();
                RECENTLY_MELEE_ATTACKED.clear();
                RECENTLY_PROJECTILE_ATTACKED.clear();
                cachedPlayerHealth = -1.0f;
                wasSwingingLastTick = false;
                return;
            }

            long currentGameTime = client.level.getGameTime();

            // 1. Progress active indicator life timelines
            Iterator<IndicatorInstance> iterator = ACTIVE_INDICATORS.iterator();
            while (iterator.hasNext()) {
                IndicatorInstance instance = iterator.next();
                instance.currentAgeTicks++;
                if (instance.currentAgeTicks >= instance.maxAgeTicks) {
                    iterator.remove();
                }
            }

            // 2. Track Melee Attacks
            if (player.swinging && !wasSwingingLastTick) {
                if (client.hitResult instanceof EntityHitResult entityHit) {
                    RECENTLY_MELEE_ATTACKED.put(entityHit.getEntity().getUUID(), currentGameTime);
                }
            }
            wasSwingingLastTick = player.swinging;

            // 3. Proactive Projectile Threat Scanner
            // Scans the area around the player's active projectiles to defensively tag targets before impact
            for (Entity levelEntity : client.level.entitiesForRendering()) {
                if (levelEntity instanceof Projectile projectile && projectile.getOwner() == player) {
                    for (Entity target : client.level.entitiesForRendering()) {
                        if (target instanceof LivingEntity le && target != player) {
                            // If projectile is within 6 blocks of a mob, tag them as threatened
                            if (projectile.distanceToSqr(le) <= 36.0f) {
                                RECENTLY_PROJECTILE_ATTACKED.put(le.getUUID(), currentGameTime);
                            }
                        }
                    }
                }
            }

            // 4. Scan Client-Side Damage Taken
            float playerHp = getTotalHealth(player);
            if (cachedPlayerHealth != -1.0f && playerHp < cachedPlayerHealth) {
                float delta = cachedPlayerHealth - playerHp;
                if (delta >= 0.1f) {
                    ACTIVE_INDICATORS.add(new IndicatorInstance(true, delta));
                }
            }
            cachedPlayerHealth = playerHp;

            // 5. Scan Client-Side Damage Given
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity target && target != player) {
                    UUID id = target.getUUID();
                    float targetHp = getTotalHealth(target);

                    if (CACHED_ENTITY_HEALTH.containsKey(id)) {
                        float previousHp = CACHED_ENTITY_HEALTH.get(id);
                        if (targetHp < previousHp) {
                            float deltaDealt = previousHp - targetHp;
                            boolean validPlayerHit = false;
                            boolean isMeleeHit = false;

                            // Check valid melee hit
                            if (RECENTLY_MELEE_ATTACKED.containsKey(id) && (currentGameTime - RECENTLY_MELEE_ATTACKED.get(id) <= 20)) {
                                validPlayerHit = true;
                                isMeleeHit = true;
                                RECENTLY_MELEE_ATTACKED.remove(id); 
                            }

                            // Check valid projectile hit (Did an arrow fly near them within the last second?)
                            if (!validPlayerHit && RECENTLY_PROJECTILE_ATTACKED.containsKey(id) && (currentGameTime - RECENTLY_PROJECTILE_ATTACKED.get(id) <= 20)) {
                                validPlayerHit = true;
                                // We purposefully do not remove the tag in case of splash potions hitting multiple targets
                            }

                            if (validPlayerHit && deltaDealt >= 0.1f) {
                                // Fatal melee blow override to bypass lethal health clamping
                                if (targetHp <= 0f && isMeleeHit) {
                                    try {
                                        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                                        float attackStrengthScale = player.getAttackStrengthScale(0.5F);
                                        float cooldownMultiplier = 0.2F + attackStrengthScale * attackStrengthScale * 0.8F;
                                        float potentialDamage = baseDamage * cooldownMultiplier;

                                        // Apply critical hit modifier if client conditions are met
                                        boolean isCrit = player.getDeltaMovement().y < 0.0 && player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater() && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
                                        if (isCrit) {
                                            potentialDamage *= 1.5F;
                                        }

                                        if (potentialDamage > deltaDealt) {
                                            deltaDealt = potentialDamage;
                                        }
                                    } catch (Exception ignored) {}
                                }

                                ACTIVE_INDICATORS.add(new IndicatorInstance(false, deltaDealt));
                            }
                        }
                    }
                    CACHED_ENTITY_HEALTH.put(id, targetHp);
                }
            }

            // Periodic map leak sweep
            if (currentGameTime % 120 == 0) {
                CACHED_ENTITY_HEALTH.keySet().removeIf(uuid -> {
                    for (Entity e : client.level.entitiesForRendering()) {
                        if (e.getUUID().equals(uuid)) return false;
                    }
                    return true;
                });
                RECENTLY_MELEE_ATTACKED.keySet().removeIf(uuid -> currentGameTime - RECENTLY_MELEE_ATTACKED.get(uuid) > 100);
                RECENTLY_PROJECTILE_ATTACKED.keySet().removeIf(uuid -> currentGameTime - RECENTLY_PROJECTILE_ATTACKED.get(uuid) > 100);
            }
        });
    }
}