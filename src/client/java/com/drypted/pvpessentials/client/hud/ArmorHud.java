package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ArmorHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");

    private boolean isEnabled() {
        return ConfigManager.getBoolean(PVPEssentialsClient.MOD_ID, "Armor HUD", "Enabled");
    }

    private boolean startWithHead() {
        return ConfigManager.getBoolean(PVPEssentialsClient.MOD_ID, "Armor HUD", "Start with Head");
    }

    private String getSide() {
        return ConfigManager.getString(PVPEssentialsClient.MOD_ID, "Armor HUD", "Side");
    }

    private int getVerticalOffset() {
        return (int) ConfigManager.getNumber(PVPEssentialsClient.MOD_ID, "Armor HUD", "Vertical Offset");
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator())
            return;

        List<ItemStack> armorItems = startWithHead() ? List.of(
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        ) : List.of(
                player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD)
        );

        boolean hasAnyArmor = armorItems.stream().anyMatch(s -> !s.isEmpty());
        if (!hasAnyArmor) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;
        boolean hasOffhand = !player.getOffhandItem().isEmpty();

        int middleX = screenWidth / 2;
        int yPos = screenHeight - 22 + getVerticalOffset();
        int hudWidth = 82;

        // Determine side based on config
        String side = getSide();
        boolean forceLeft = side.equals("Left");
        boolean forceRight = side.equals("Right");
        boolean auto = side.equals("Auto");

        int startX;
        if (forceLeft) {
            startX = 10; // simple left margin
        } else if (forceRight) {
            startX = screenWidth - 10 - hudWidth - 4; // right margin
        } else { // Auto
            if (isRightHanded) {
                startX = hasOffhand ? (middleX - 120 - 7 - hudWidth) : (middleX - 91 - 7 - hudWidth);
            } else {
                startX = hasOffhand ? (middleX + 120 + 7) : (middleX + 91 + 7);
            }
        }

        // Clamp to screen bounds
        startX = Math.max(2, Math.min(startX, screenWidth - hudWidth - 2));

        graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, yPos, 0, 0, 81, 22, 182, 22);
        graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + 81, yPos, 181, 0, 1, 22, 182, 22);

        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            if (stack.isEmpty()) continue;
            int itemX = startX + 3 + (i * 20);
            int itemY = yPos + 3;
            RenderUtil.drawScaledItemFactor(graphics, stack, itemX, itemY, 1.0f);
        }
    }
}