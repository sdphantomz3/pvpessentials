package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import com.drypted.pvpessentials.client.util.HudLayoutManager;
import com.drypted.pvpessentials.client.util.HudLayoutManager.Anchor;
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
    private static final int SLOT_PX = 20;
    private static final int MAX_COLS = 4;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARMOR_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private boolean startWithHead() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARMOR_START_WITH_HEAD);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private String getSide() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARMOR_SIDE);
        return opt != null ? opt.value : "HOTBAR";
    }

    private int getVerticalOffset() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_ARMOR_VERTICAL_OFFSET);
        if (opt != null) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 0;
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
        int hotbarLeftEdge = middleX - 91 - 7;
        int hotbarRightEdge = middleX + 91 + 7;
        int yBase = screenHeight - 22 + getVerticalOffset();

        String side = getSide();
        Anchor anchor;
        if (side.equals("Left")) {
            anchor = Anchor.SCREEN_LEFT;
        } else if (side.equals("Right")) {
            anchor = Anchor.SCREEN_RIGHT;
        } else {
            anchor = isRightHanded ? Anchor.HOTBAR_LEFT : Anchor.HOTBAR_RIGHT;
        }

        // Dynamic column count — adapt to zone width
        int maxCols = HudLayoutManager.getMaxColumns(anchor, SLOT_PX);
        int itemsPerRow = Math.min(MAX_COLS, maxCols);

        int totalItems = 4; // head, chest, legs, feet
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * SLOT_PX) + 1;
        int renderedWidth = textureWidth + 1;
        int hudHeight = totalRows > 1 ? totalRows * SLOT_PX + 2 : 22;

        HudLayoutManager.register("armor", anchor, renderedWidth, hudHeight, getVerticalOffset());
        int xOff = HudLayoutManager.getX("armor");
        int yOff = HudLayoutManager.getY("armor");

        int startX;
        switch (anchor) {
            case HOTBAR_LEFT: {
                int baseL = hasOffhand ? (hotbarLeftEdge - 29) : hotbarLeftEdge;
                startX = baseL - xOff - renderedWidth;
                break;
            }
            case HOTBAR_RIGHT: {
                int baseR = hasOffhand ? (hotbarRightEdge + 29) : hotbarRightEdge;
                startX = baseR + xOff;
                break;
            }
            case SCREEN_LEFT:
                startX = 10 + xOff;
                break;
            case SCREEN_RIGHT:
                startX = screenWidth - 10 - xOff - renderedWidth;
                break;
            default:
                startX = 10;
        }
        startX = Math.max(2, Math.min(startX, screenWidth - renderedWidth - 2));
        int baseYPos = yBase - yOff;

        // Multi-row rendering (same pattern as PotionHud)
        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0
                    : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22
                    : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            currentY -= srcHeight;
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE,
                    startX, currentY, 0, srcV, textureWidth, srcHeight, 182, 22);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE,
                    startX + textureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
        }

        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            if (stack.isEmpty()) continue;
            int rowIndex = i / itemsPerRow;
            int slotOffset = i % itemsPerRow;
            int itemX = startX + 3 + (slotOffset * SLOT_PX);
            int itemY = (baseYPos + 3) - (rowIndex * SLOT_PX);
            RenderUtil.drawScaledItemFactor(graphics, stack, itemX, itemY, 1.0f);
        }
    }
}