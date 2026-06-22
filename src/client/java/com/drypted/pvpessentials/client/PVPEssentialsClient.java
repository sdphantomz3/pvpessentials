package com.drypted.pvpessentials.client;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.PotionHud;
import com.drypted.pvpessentials.client.hud.ArrowHud;
import com.drypted.pvpessentials.client.hud.MiscHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

import java.util.List;

public class PVPEssentialsClient implements ClientModInitializer {

    public static final String MOD_ID = "pvpessentials";

    private final ArmorHud armorHud = new ArmorHud();
    private final PotionHud potionHud = new PotionHud();
    private final DamageIndicatorHud damageIndicatorHud = new DamageIndicatorHud();
    private final ArrowHud arrowHud = new ArrowHud();
    private final MiscHud miscHud = new MiscHud();

    public void createConfig() {
        // ---- Armor HUD ----
        ConfigManager.registerOption(MOD_ID, "Armor HUD", "Enabled", "toggle", "true", null);
        ConfigManager.registerOption(MOD_ID, "Armor HUD", "Start with Head", "toggle", "false", null);
        ConfigManager.registerOption(MOD_ID, "Armor HUD", "Side", "cycle", "Auto", List.of("Auto", "Left", "Right"));
        ConfigManager.registerOption(MOD_ID, "Armor HUD", "Vertical Offset", "number", "0", null);

        // ---- Potion HUD ----
        ConfigManager.registerOption(MOD_ID, "Potion HUD", "Enabled", "toggle", "true", null);
        ConfigManager.registerOption(MOD_ID, "Potion HUD", "Side", "cycle", "Auto", List.of("Auto", "Left", "Right"));
        ConfigManager.registerOption(MOD_ID, "Potion HUD", "Vertical Offset", "number", "0", null);

        // ---- Misc HUD ----
        ConfigManager.registerOption(MOD_ID, "Misc HUD", "Enabled", "toggle", "true", null);
        ConfigManager.registerOption(MOD_ID, "Misc HUD", "Side", "cycle", "Auto", List.of("Auto", "Left", "Right"));
        ConfigManager.registerOption(MOD_ID, "Misc HUD", "Vertical Offset", "number", "0", null);

        // ---- Arrow HUD ----
        ConfigManager.registerOption(MOD_ID, "Arrow HUD", "Enabled", "toggle", "true", null);
        ConfigManager.registerOption(MOD_ID, "Arrow HUD", "Horizontal Offset", "number", "12", null);
        ConfigManager.registerOption(MOD_ID, "Arrow HUD", "Vertical Offset", "number", "0", null);

        // ---- Damage Indicator ----
        ConfigManager.registerOption(MOD_ID, "Damage Indicator", "Enabled", "toggle", "true", null);
        ConfigManager.registerOption(MOD_ID, "Damage Indicator", "Display Mode", "cycle", "Hearts", List.of("Hearts", "Raw HP"));
        ConfigManager.registerOption(MOD_ID, "Damage Indicator", "Scale", "number", "1.2", null);
        ConfigManager.registerOption(MOD_ID, "Damage Indicator", "Lifetime (ticks)", "number", "22", null);

        ConfigManager.load();
    }

    @Override
    public void onInitializeClient() {
        createConfig();

        DamageTracker.initialize();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"),
                this.armorHud::render);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "misc_hud"),
                this.miscHud::render);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "potion_hud"),
                this.potionHud::render);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(MOD_ID, "damage_indicators"),
                this.damageIndicatorHud::render);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(MOD_ID, "arrow_hud"),
                this.arrowHud::render);
    }
}