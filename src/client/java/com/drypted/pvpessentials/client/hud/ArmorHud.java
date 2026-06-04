package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ArmorHud {

    // Configuration Toggle
    private static final boolean START_WITH_HEAD = true;

    private static final ResourceLocation HOTBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator())
            return;

        // Gather armor pieces based on preference configuration
        List<ItemStack> armorItems = START_WITH_HEAD ? List.of(
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

        // Check if the HUD frame should be drawn
        boolean hasAnyArmor = false;
        for (ItemStack stack : armorItems) {
            if (!stack.isEmpty()) {
                hasAnyArmor = true;
                break;
            }
        }

        if (!hasAnyArmor) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;
        boolean hasOffhand = !player.getOffhandItem().isEmpty();

        int middleX = screenWidth / 2;
        int yPos = screenHeight - 22;
        int hudWidth = 82;
        
        // Calculate offhand dynamic positions
        int startX;
        if (isRightHanded) {
            startX = hasOffhand ? (middleX - 120 - 7 - hudWidth) : (middleX - 91 - 7 - hudWidth);
        } else {
            startX = hasOffhand ? (middleX + 120 + 7) : (middleX + 91 + 7);
        }

        // Draw HUD background frame
        graphics.blit(RenderType::guiTextured, HOTBAR_TEXTURE, startX, yPos, 0, 0, 81, 22, 182, 22);
        graphics.blit(RenderType::guiTextured, HOTBAR_TEXTURE, startX + 81, yPos, 181, 0, 1, 22, 182, 22);

        // Render active equipment items
        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            int itemX = startX + 3 + (i * 20);
            int itemY = yPos + 3;

            if (stack.isEmpty()) {
                continue; // Leaves background slot beautifully clean
            }

            RenderUtil.drawScaledItemFactor(graphics, stack, itemX, itemY, 1.0f);
        }
    }
}