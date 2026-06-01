package com.drypted.pvpessentials.client;

import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.PotionHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class PVPEssentialsClient implements ClientModInitializer {

    public static final String MOD_ID = "pvpessentials";
    
    private final ArmorHud armorHud = new ArmorHud();
    private final DamageIndicatorHud damageIndicatorHud = new DamageIndicatorHud();
    private final PotionHud potionHud = new PotionHud();

    @Override
    public void onInitializeClient() {
        // Initialize background tick trackers
        DamageTracker.initialize();

        // Register Armor HUD
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"),
                this.armorHud::render
        );

        // Register Potion HUD (Anchored next to the hotbar/armor elements alignment)
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "potion_hud"),
                this.potionHud::render
        );

        // Register Damage Given/Taken Crosshair Indicators
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(MOD_ID, "damage_indicators"),
                this.damageIndicatorHud::render
        );
    }
}