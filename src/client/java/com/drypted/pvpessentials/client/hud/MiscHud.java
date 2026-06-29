package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class MiscHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private String getSide() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_SIDE);
        return opt != null ? opt.value : "Auto";
    }

    private int getVerticalOffset() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_VERTICAL_OFFSET);
        if (opt != null) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) return;

        Item[] targetItems = {
                Items.GOLDEN_APPLE,
                Items.ENDER_PEARL,
                Items.COBWEB,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.EXPERIENCE_BOTTLE
        };

        List<ItemStack> itemsToRender = new ArrayList<>();
        int containerSize = player.getInventory().getContainerSize();
        for (Item targetItem : targetItems) {
            int totalCount = 0;
            for (int i = 0; i < containerSize; i++) {
                ItemStack slotStack = player.getInventory().getItem(i);
                if (!slotStack.isEmpty() && slotStack.is(targetItem)) {
                    totalCount += slotStack.getCount();
                }
            }
            if (totalCount > 0) {
                ItemStack displayStack = new ItemStack(targetItem);
                displayStack.setCount(totalCount);
                itemsToRender.add(displayStack);
            }
        }

        if (itemsToRender.isEmpty()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

        int middleX = screenWidth / 2;
        int baseYPos = screenHeight - 22 + getVerticalOffset();

        String side = getSide();
        boolean forceLeft = side.equals("Left");
        boolean forceRight = side.equals("Right");
        boolean auto = side.equals("Auto");

        int maxAvailableWidth;
        if (forceLeft) {
            maxAvailableWidth = middleX - 10;
        } else if (forceRight) {
            maxAvailableWidth = screenWidth - (middleX + 10);
        } else {
            maxAvailableWidth = isRightHanded ? (screenWidth - (middleX + 91 + 7)) : (middleX - 91 - 7);
            if (maxAvailableWidth < 0) maxAvailableWidth = 40;
        }

        int itemsPerRow = 2;
        while (itemsPerRow > 1 && ((itemsPerRow * 20) + 2) > maxAvailableWidth) {
            itemsPerRow--;
        }

        int totalItems = itemsToRender.size();
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * 20) + 1;
        int renderedWidth = textureWidth + 1;

        int startX;
        if (forceLeft) {
            startX = 10;
        } else if (forceRight) {
            startX = screenWidth - 10 - renderedWidth;
        } else {
            if (isRightHanded) {
                startX = middleX + 91 + 7;
            } else {
                startX = (middleX - 91 - 7) - renderedWidth;
            }
        }
        startX = Math.max(2, Math.min(startX, screenWidth - renderedWidth - 2));

        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0 : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22 : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            currentY -= srcHeight;
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, currentY, 0, srcV, textureWidth, srcHeight, 182, 22);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + textureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
        }

        for (int i = 0; i < totalItems; i++) {
            ItemStack stack = itemsToRender.get(i);
            int rowIndex = i / itemsPerRow;
            int slotOffset = i % itemsPerRow;
            int itemX = startX + 3 + (slotOffset * 20);
            int itemY = (baseYPos + 3) - (rowIndex * 20);
            RenderUtil.drawScaledItemFactor(graphics, stack, itemX, itemY, 1.0f);
        }
    }
}