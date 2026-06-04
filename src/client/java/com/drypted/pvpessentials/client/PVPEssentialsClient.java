package com.drypted.pvpessentials.client;

import com.drypted.pvpessentials.client.handler.DamageTracker;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.ArrowHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.MiscHud;
import com.drypted.pvpessentials.client.hud.PotionHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class PVPEssentialsClient implements ClientModInitializer {

    public static final String MOD_ID = "pvpessentials";

    private final ArmorHud armorHud = new ArmorHud();
    private final PotionHud potionHud = new PotionHud();
    private final DamageIndicatorHud damageIndicatorHud = new DamageIndicatorHud();
    private final ArrowHud arrowHud = new ArrowHud();
    private final MiscHud miscHud = new MiscHud();

    @Override
    public void onInitializeClient() {
        DamageTracker.initialize();

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            armorHud.render(drawContext, tickDelta);
            miscHud.render(drawContext, tickDelta);
            potionHud.render(drawContext, tickDelta);
            damageIndicatorHud.render(drawContext, tickDelta);
            arrowHud.render(drawContext, tickDelta);
        });
    }
}