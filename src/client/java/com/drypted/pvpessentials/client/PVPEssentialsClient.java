package com.drypted.pvpessentials.client;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.PotionHud;
import com.drypted.pvpessentials.client.hud.ArrowHud;
import com.drypted.pvpessentials.client.hud.MiscHud;
import com.drypted.pvpessentials.client.hud.HudLayoutStorage;
import com.drypted.pvpessentials.client.hud.PopupManager;
import com.drypted.pvpessentials.client.screen.HudEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public class PVPEssentialsClient implements ClientModInitializer {

        public static final String MOD_ID = "pvpessentials";
        public static final String MOD_DISPLAY_NAME = "PVP Essentials";

        // Unique keys for all config options – used both for registration and lookup
        public static final String KEY_ARMOR_ENABLED = "pvpessentials.armorhud.enabled";
        public static final String KEY_ARMOR_START_WITH_HEAD = "pvpessentials.armorhud.startwithhead";

        public static final String KEY_POTION_ENABLED = "pvpessentials.potionhud.enabled";

        public static final String KEY_MISC_ENABLED = "pvpessentials.mischud.enabled";
        public static final String KEY_MISC_ITEMS = "pvpessentials.mischud.items";
        public static final String KEY_MISC_VERTICAL_STACK = "pvpessentials.mischud.verticalstack";

        public static final String KEY_ARROW_ENABLED = "pvpessentials.arrowhud.enabled";

        public static final String KEY_DAMAGE_ENABLED = "pvpessentials.damageindicator.enabled";
        public static final String KEY_DAMAGE_DISPLAY_MODE = "pvpessentials.damageindicator.displaymode";
        public static final String KEY_DAMAGE_SCALE = "pvpessentials.damageindicator.scale";
        public static final String KEY_DAMAGE_LIFETIME = "pvpessentials.damageindicator.lifetime";

        // HUD layout position keys (X and Y as normalized percentages 0.0–1.0)
        public static final String KEY_HUD_ARMOR_X = "pvpessentials.hudlayout.armor_x";
        public static final String KEY_HUD_ARMOR_Y = "pvpessentials.hudlayout.armor_y";
        public static final String KEY_HUD_ARMOR_ANCHOR = "pvpessentials.hudlayout.armor_anchor";
        public static final String KEY_HUD_POTION_X = "pvpessentials.hudlayout.potion_x";
        public static final String KEY_HUD_POTION_Y = "pvpessentials.hudlayout.potion_y";
        public static final String KEY_HUD_POTION_ANCHOR = "pvpessentials.hudlayout.potion_anchor";
        public static final String KEY_HUD_MISC_X = "pvpessentials.hudlayout.misc_x";
        public static final String KEY_HUD_MISC_Y = "pvpessentials.hudlayout.misc_y";
        public static final String KEY_HUD_MISC_ANCHOR = "pvpessentials.hudlayout.misc_anchor";
        public static final String KEY_HUD_ARROW_X = "pvpessentials.hudlayout.arrow_x";
        public static final String KEY_HUD_ARROW_Y = "pvpessentials.hudlayout.arrow_y";
        public static final String KEY_HUD_ARROW_ANCHOR = "pvpessentials.hudlayout.arrow_anchor";
        public static final String KEY_HUD_DAMAGE_X = "pvpessentials.hudlayout.damage_x";
        public static final String KEY_HUD_DAMAGE_Y = "pvpessentials.hudlayout.damage_y";
        public static final String KEY_HUD_DAMAGE_ANCHOR = "pvpessentials.hudlayout.damage_anchor";

        public static final String KEY_HUD_AUTO_ADJUST = "pvpessentials.hudlayout.autoadjust";

        public static final String KEY_LOWFIRE_ENABLED = "pvpessentials.lowfire.enabled";
        public static final String KEY_LOWFIRE_YOFFSET = "pvpessentials.lowfire.yoffset";

        public static final String KEY_SHIELD_ENABLED = "pvpessentials.shield.enabled";
        public static final String KEY_SHIELD_IDLE_ROTATION = "pvpessentials.shield.idle.rotation";
        public static final String KEY_SHIELD_IDLE_ROTATION_DEGREES = "pvpessentials.shield.idle.rotationdegrees";
        public static final String KEY_SHIELD_IDLE_X = "pvpessentials.shield.idle.x";
        public static final String KEY_SHIELD_IDLE_Y = "pvpessentials.shield.idle.y";
        public static final String KEY_SHIELD_IDLE_Z = "pvpessentials.shield.idle.z";
        public static final String KEY_SHIELD_BLOCKING_X = "pvpessentials.shield.blocking.x";
        public static final String KEY_SHIELD_BLOCKING_Y = "pvpessentials.shield.blocking.y";
        public static final String KEY_SHIELD_BLOCKING_Z = "pvpessentials.shield.blocking.z";

        public static final String KEY_POTION_PARTICLES_ENABLED = "pvpessentials.potionparticles.enabled";

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

                // ---- Potion HUD ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Potion HUD", "Enabled",
                                KEY_POTION_ENABLED, "toggle", "true", null, null);

                // ---- Misc HUD ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Misc HUD", "Enabled",
                                KEY_MISC_ENABLED, "toggle", "true", null, null);
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Misc HUD", "Tracked Items",
                                KEY_MISC_ITEMS, "item_select_multi",
                                "minecraft:golden_apple,minecraft:ender_pearl,minecraft:cobweb,minecraft:enchanted_golden_apple,minecraft:experience_bottle",
                                null,
                                "Select items to count and display in the Misc HUD. Add as many as you like.");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Misc HUD", "Vertical Stack",
                                KEY_MISC_VERTICAL_STACK, "toggle", "true", null,
                                "When enabled, Misc HUD items expand vertically (one per row) instead of horizontally.");

                // ---- Arrow HUD ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Arrow HUD", "Enabled",
                                KEY_ARROW_ENABLED, "toggle", "true", null, null);

                // ---- Damage Indicator ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Enabled",
                                KEY_DAMAGE_ENABLED, "toggle", "true", null, null);
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Display Mode",
                                KEY_DAMAGE_DISPLAY_MODE, "cycle", "Hearts", List.of("Hearts", "Raw HP"), null);
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Scale",
                                KEY_DAMAGE_SCALE, "number", "1.2", null, null);
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Damage Indicator", "Lifetime (ticks)",
                                KEY_DAMAGE_LIFETIME, "number", "22", null, null);

                // ---- HUD Layout ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "HUD Layout", "Mode",
                                KEY_HUD_AUTO_ADJUST, "cycle", "Auto Adjust",
                                List.of("Auto Adjust", "Set Manually"),
                                "Auto Adjust: HUDs auto-position based on screen size. Set Manually: use the HUD Layout Editor to position HUDs freely.");

                // ---- HUD Layout Editor Action ----
                ConfigManager.registerAction(MOD_ID, MOD_DISPLAY_NAME, "HUD Layout", "Edit HUD Layout",
                                "pvpessentials.hudlayout.edit", "Open Editor",
                                () -> {
                                        var modeOpt = ConfigManager.getOption(KEY_HUD_AUTO_ADJUST);
                                        if (modeOpt != null && "Auto Adjust".equals(modeOpt.value)) {
                                                PopupManager.show();
                                                return;
                                        }
                                        Minecraft.getInstance().gui.setScreen(new HudEditorScreen());
                                },
                                "Opens a drag-and-drop editor to position all HUD elements on screen. Only available in Set Manually mode.");

                // ---- No Potion Particles ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Potion Particles", "Enabled",
                                KEY_POTION_PARTICLES_ENABLED, "toggle", "true", null, null);

                // ---- Low Fire ----
                // ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Low Fire", "Enabled",
                // KEY_LOWFIRE_ENABLED, "toggle", "true", null, null);
                // ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Low Fire", "HUD
                // YOffset",
                // KEY_LOWFIRE_YOFFSET, "number", "0.3", null, null);

                // ---- Shield ----
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Enabled",
                                KEY_SHIELD_ENABLED, "toggle", "true", null,
                                "Master switch for the shield rendering modifications.");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Idle Rotation",
                                KEY_SHIELD_IDLE_ROTATION, "toggle", "true", null,
                                "Rotate the shield sideways when not blocking.");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Idle Rotation Angle",
                                KEY_SHIELD_IDLE_ROTATION_DEGREES, "number", "90", null,
                                "Degrees to rotate the shield when idle (default 90).");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Idle X Offset",
                                KEY_SHIELD_IDLE_X, "number", "0.1", null,
                                "Left/Right shift of the shield when idle.");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Idle Y Offset",
                                KEY_SHIELD_IDLE_Y, "number", "-0.15", null,
                                "Vertical shift of the shield when idle (negative = lower).");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Idle Z Offset",
                                KEY_SHIELD_IDLE_Z, "number", "-0.2", null,
                                "Depth shift of the shield when idle (negative = away from camera).");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Blocking X Offset",
                                KEY_SHIELD_BLOCKING_X, "number", "0", null,
                                "Left/Right shift of the shield while blocking.");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Blocking Y Offset",
                                KEY_SHIELD_BLOCKING_Y, "number", "-0.1", null,
                                "Vertical shift of the shield while blocking (negative = lower).");
                ConfigManager.registerOption(MOD_ID, MOD_DISPLAY_NAME, "Shield", "Blocking Z Offset",
                                KEY_SHIELD_BLOCKING_Z, "number", "0", null,
                                "Depth shift of the shield while blocking.");

                ConfigManager.load();
        }

        @Override
        public void onInitializeClient() {
                createConfig();
                HudLayoutStorage.load();

                DamageTracker.initialize();

                HudElementRegistry.attachElementBefore(
                                VanillaHudElements.HOTBAR,
                                Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"),
                                this.armorHud::render);

                HudElementRegistry.attachElementBefore(
                                VanillaHudElements.HOTBAR,
                                Identifier.fromNamespaceAndPath(MOD_ID, "potion_hud"),
                                this.potionHud::render);

                HudElementRegistry.attachElementBefore(
                                VanillaHudElements.HOTBAR,
                                Identifier.fromNamespaceAndPath(MOD_ID, "misc_hud"),
                                this.miscHud::render);

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