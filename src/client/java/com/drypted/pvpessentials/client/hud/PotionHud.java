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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class PotionHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_POTION_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private String getSide() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_POTION_SIDE);
        return opt != null ? opt.value : "HOTBAR";
    }

    private int getVerticalOffset() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_POTION_VERTICAL_OFFSET);
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

        List<ItemStack> trackedPotions = new ArrayList<>();
        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isPotion(stack)) {
                boolean combined = false;
                for (ItemStack tracked : trackedPotions) {
                    if (ItemStack.isSameItemSameComponents(tracked, stack)) {
                        tracked.setCount(tracked.getCount() + stack.getCount());
                        combined = true;
                        break;
                    }
                }
                if (!combined) {
                    trackedPotions.add(stack.copy());
                }
            }
        }

        if (trackedPotions.isEmpty()) return;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

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
            anchor = isRightHanded ? Anchor.HOTBAR_RIGHT : Anchor.HOTBAR_LEFT;
        }

        // Dynamic column count - based on zone width (deterministic!)
        int maxCols = HudLayoutManager.getMaxColumns(anchor, 20);
        int itemsPerRow = Math.min(4, maxCols);

        int totalItems = trackedPotions.size();
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
        int slotsToDraw = Math.min(totalItems, itemsPerRow);
        int textureWidth = (slotsToDraw * 20) + 1;
        int renderedWidth = textureWidth + 1;
        int hudHeight = totalRows > 1 ? totalRows * 20 + 2 : 22;

        HudLayoutManager.register("potion", anchor, renderedWidth, hudHeight, getVerticalOffset());
        int xOff = HudLayoutManager.getX("potion");
        int yOff = HudLayoutManager.getY("potion");

        int startX;
        switch (anchor) {
            case HOTBAR_LEFT:
                startX = hotbarLeftEdge - xOff - renderedWidth;
                break;
            case HOTBAR_RIGHT:
                startX = hotbarRightEdge + xOff;
                break;
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

        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV = (totalRows == 1) ? 0 : (rowIndex == 0 ? 1 : (rowIndex == totalRows - 1 ? 0 : 1));
            int srcHeight = (totalRows == 1) ? 22 : (rowIndex == 0 || rowIndex == totalRows - 1 ? 21 : 20);
            currentY -= srcHeight;
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, currentY, 0, srcV, textureWidth, srcHeight, 182, 22);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + textureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
        }

        for (int i = 0; i < totalItems; i++) {
            ItemStack potionStack = trackedPotions.get(i);
            int rowIndex = i / itemsPerRow;
            int slotOffset = i % itemsPerRow;
            int itemX = startX + 3 + (slotOffset * 20);
            int itemY = (baseYPos + 3) - (rowIndex * 20);
            RenderUtil.drawScaledItemFactor(graphics, potionStack, itemX, itemY, 1.0f);
        }
    }

    private boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}