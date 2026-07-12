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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MiscHud {

    private static final Identifier HOTBAR_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/hotbar.png");
    private static final int SLOT_PX = 20;

    // Cached resolved item list — recalculated when config changes
    private static List<Item> cachedTrackedItems = null;
    private static String lastConfigValue = null;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private String getSide() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_SIDE);
        return opt != null ? opt.value : "HOTBAR";
    }

    private int getVerticalOffset() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_VERTICAL_OFFSET);
        if (opt != null) {
            try { return Integer.parseInt(opt.value); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    /**
     * Reads the item_select_multi config and resolves item IDs to actual Item objects.
     * Results are cached until the config value changes.
     */
    private List<Item> getTrackedItems() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_ITEMS);
        String raw = opt != null ? opt.value : "";
        if (raw == null) raw = "";

        if (cachedTrackedItems != null && raw.equals(lastConfigValue)) {
            return cachedTrackedItems;
        }

        lastConfigValue = raw;
        // Use LinkedHashSet to preserve order and deduplicate
        Set<Item> itemSet = new LinkedHashSet<>();
        if (!raw.isEmpty()) {
            for (String id : raw.split(",")) {
                id = id.trim();
                if (id.isEmpty()) continue;
                try {
                    String[] parts = id.split(":");
                    if (parts.length == 2) {
                        Identifier identifier = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
                        Item item = BuiltInRegistries.ITEM.getValue(identifier);
                        if (item != null) {
                            itemSet.add(item);
                        }
                    }
                } catch (Exception ignored) {
                    // Skip malformed item IDs
                }
            }
        }

        cachedTrackedItems = new ArrayList<>(itemSet);
        return cachedTrackedItems;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!isEnabled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator() || minecraft.gui.hud.isHidden()) return;

        List<Item> targetItems = getTrackedItems();
        if (targetItems.isEmpty()) return;

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

        // Dynamic column count — fit as many as the zone allows
        int maxCols = HudLayoutManager.getMaxColumns(anchor, SLOT_PX);
        int totalItems = itemsToRender.size();

        // Limit to a reasonable max and wrap to multiple rows
        int itemsPerRow = Math.min(totalItems, maxCols);
        int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);

        int slotsOnBottomRow = totalItems - (totalRows - 1) * itemsPerRow;
        if (slotsOnBottomRow <= 0) slotsOnBottomRow = itemsPerRow;

        // HUD dimensions — bottom row may be narrower
        int bottomTextureWidth = (slotsOnBottomRow * SLOT_PX) + 1;
        int fullTextureWidth = (itemsPerRow * SLOT_PX) + 1;
        int renderedWidth = fullTextureWidth + 1;
        int hudHeight = totalRows > 1 ? totalRows * SLOT_PX + 2 : 22;

        HudLayoutManager.register("misc", anchor, renderedWidth, hudHeight, getVerticalOffset());
        int xOff = HudLayoutManager.getX("misc");
        int yOff = HudLayoutManager.getY("misc");

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

        // Draw the hotbar-style background for each row
        int currentY = baseYPos + 22;
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            boolean isTopRow = (rowIndex == 0);
            boolean isBottomRow = (rowIndex == totalRows - 1);
            int slotsInThisRow = isBottomRow ? slotsOnBottomRow : itemsPerRow;

            int srcV;
            int srcHeight;
            if (totalRows == 1) {
                srcV = 0;
                srcHeight = 22;
            } else if (isTopRow) {
                srcV = 1;
                srcHeight = 21;
            } else if (isBottomRow) {
                srcV = 0;
                srcHeight = 21;
            } else {
                srcV = 1;
                srcHeight = 20;
            }

            int rowTextureWidth = (slotsInThisRow * SLOT_PX) + 1;
            currentY -= srcHeight;
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, currentY, 0, srcV, rowTextureWidth, srcHeight, 182, 22);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + rowTextureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
        }

        // Render items into their slots
        for (int i = 0; i < totalItems; i++) {
            ItemStack stack = itemsToRender.get(i);
            int rowIndex = i / itemsPerRow;
            int slotOffset = i % itemsPerRow;
            int itemX = startX + 3 + (slotOffset * SLOT_PX);
            int itemY = (baseYPos + 3) - (rowIndex * SLOT_PX);
            RenderUtil.drawScaledItemFactor(graphics, stack, itemX, itemY, 1.0f);
        }
    }
}