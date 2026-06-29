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
    public static final String MOD_DISPLAY_NAME = "PVP Essentials";

    // Unique keys for all config options – used both for registration and lookup
    public static final String KEY_ARMOR_ENABLED = "pvpessentials.armorhud.enabled";
    public static final String KEY_ARMOR_START_WITH_HEAD = "pvpessentials.armorhud.startwithhead";
    public static final String KEY_ARMOR_SIDE = "pvpessentials.armorhud.side";
    public static final String KEY_ARMOR_VERTICAL_OFFSET = "pvpessentials.armorhud.verticaloffset";

    public static final String KEY_POTION_ENABLED = "pvpessentials.potionhud.enabled";
    public static final String KEY_POTION_SIDE = "pvpessentials.potionhud.side";
    public static final String KEY_POTION_VERTICAL_OFFSET = "pvpessentials.potionhud.verticaloffset";

    public static final String KEY_MISC_ENABLED = "pvpessentials.mischud.enabled";
    public static final String KEY_MISC_SIDE = "pvpessentials.mischud.side";
    public static final String KEY_MISC_VERTICAL_OFFSET = "pvpessentials.mischud.verticaloffset";

    public static final String KEY_ARROW_ENABLED = "pvpessentials.arrowhud.enabled";
    public static final String KEY_ARROW_HORIZONTAL_OFFSET = "pvpessentials.arrowhud.horizontaloffset";
    public static final String KEY_ARROW_VERTICAL_OFFSET = "pvpessentials.arrowhud.verticaloffset";

    public static final String KEY_DAMAGE_ENABLED = "pvpessentials.damageindicator.enabled";
    public static final String KEY_DAMAGE_DISPLAY_MODE = "pvpessentials.damageindicator.displaymode";
    public static final String KEY_DAMAGE_SCALE = "pvpessentials.damageindicator.scale";
    public static final String KEY_DAMAGE_LIFETIME = "pvpessentials.damageindicator.lifetime";

    public static final String KEY_LOWFIRE_ENABLED = "pvpessentials.lowfire.enabled";
    public static final String KEY_LOWFIRE_YOFFSET = "pvpessentials.lowfire.yoffset";

    public static final String KEY_SHIELD_LOW = "pvpessentials.shield.low";
    public static final String KEY_SHIELD_SIDE = "pvpessentials.shield.side";

    private final ArmorHud armorHud = new ArmorHud();
    private final PotionHud potionHud = new PotionHud();
    private final DamageIndicatorHud damageIndicatorHud = new DamageIndicatorHud();
    private final ArrowHud arrowHud = new ArrowHud();
    private final MiscHud miscHud = new MiscHud();

    public void createConfig() {
        // ---- Armor HUD ----
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Armor HUD", "Enabled",
                KEY_ARMOR_ENABLED, "toggle", "true", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Armor HUD", "Start with Head",
                KEY_ARMOR_START_WITH_HEAD, "toggle", "false", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Armor HUD", "Side",
                KEY_ARMOR_SIDE, "cycle", "Auto", List.of("Auto", "Left", "Right"), null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Armor HUD", "Vertical Offset",
                KEY_ARMOR_VERTICAL_OFFSET, "number", "0", null, null);

        // ---- Potion HUD ----
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Potion HUD", "Enabled",
                KEY_POTION_ENABLED, "toggle", "true", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Potion HUD", "Side",
                KEY_POTION_SIDE, "cycle", "Auto", List.of("Auto", "Left", "Right"), null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Potion HUD", "Vertical Offset",
                KEY_POTION_VERTICAL_OFFSET, "number", "0", null, null);

        // ---- Misc HUD ----
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Misc HUD", "Enabled",
                KEY_MISC_ENABLED, "toggle", "true", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Misc HUD", "Side",
                KEY_MISC_SIDE, "cycle", "Auto", List.of("Auto", "Left", "Right"), null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Misc HUD", "Vertical Offset",
                KEY_MISC_VERTICAL_OFFSET, "number", "0", null, null);

        // ---- Arrow HUD ----
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Arrow HUD", "Enabled",
                KEY_ARROW_ENABLED, "toggle", "true", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Arrow HUD", "Horizontal Offset",
                KEY_ARROW_HORIZONTAL_OFFSET, "number", "12", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Arrow HUD", "Vertical Offset",
                KEY_ARROW_VERTICAL_OFFSET, "number", "0", null, null);

        // ---- Damage Indicator ----
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Enabled",
                KEY_DAMAGE_ENABLED, "toggle", "true", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Display Mode",
                KEY_DAMAGE_DISPLAY_MODE, "cycle", "Hearts", List.of("Hearts", "Raw HP"), null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Scale",
                KEY_DAMAGE_SCALE, "number", "1.2", null, null);
        ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Lifetime (ticks)",
                KEY_DAMAGE_LIFETIME, "number", "22", null, null);

        // ---- Low Fire ----
        // ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Low Fire", "Enabled",
        //         KEY_LOWFIRE_ENABLED, "toggle", "true", null, null);
        // ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Low Fire", "HUD YOffset",
        //         KEY_LOWFIRE_YOFFSET, "number", "0.3", null, null);

        // // ---- Shield ----
        // ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Low Shield",
        //         KEY_SHIELD_LOW, "toggle", "true", null, null);
        // ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Side Shield",
        //         KEY_SHIELD_SIDE, "toggle", "false", null, null);

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