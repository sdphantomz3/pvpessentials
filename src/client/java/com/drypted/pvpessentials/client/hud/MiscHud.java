package com.drypted.pvpessentials.client.hud;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

    private static final int ELEM_WIDTH = 22;  // one slot wide + border
    private static final int ELEM_HEIGHT = 22;

    // Preview mode for HUD editor
    public static boolean previewMode = false;
    public static float previewX = 0f;
    public static float previewY = 0f;
    public static Anchor previewAnchor = Anchor.TOP_LEFT;

    // Cached resolved item list — recalculated when config changes
    private static List<Item> cachedTrackedItems = null;
    private static String lastConfigValue = null;

    private boolean isEnabled() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_ENABLED);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private static boolean isAutoAdjust() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_AUTO_ADJUST);
        return opt != null && "Auto Adjust".equals(opt.value);
    }

    private static boolean isVerticalStack() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_VERTICAL_STACK);
        return opt != null && Boolean.parseBoolean(opt.value);
    }

    private int[] getPosition(int screenWidth, int screenHeight) {
        if (previewMode) {
            return previewAnchor.toPixel(previewX, previewY, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
        }
        if (isAutoAdjust()) {
            float xp = getDefaultXPercent(screenWidth);
            float yp = getDefaultYPercent(screenHeight);
            return Anchor.BOTTOM_RIGHT.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
        }
        float xp = HudLayoutStorage.getX("misc", getDefaultXPercent(screenWidth));
        float yp = HudLayoutStorage.getY("misc", getDefaultYPercent(screenHeight));
        Anchor anchor = HudLayoutStorage.getAnchor("misc", Anchor.BOTTOM_RIGHT);
        return anchor.toPixel(xp, yp, screenWidth, screenHeight, ELEM_WIDTH, ELEM_HEIGHT);
    }

    /**
     * Default X percentage: flush with right border (anchor handles ELEM_WIDTH offset).
     */
    public static float getDefaultXPercent(int screenWidth) {
        return 1.0f;
    }

    /** Default Y percentage: flush with bottom of screen (anchor handles ELEM_HEIGHT offset). */
    public static float getDefaultYPercent(int screenHeight) {
        return 1.0f;
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
        if (previewMode) return;

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
        int[] pos = getPosition(screenWidth, screenHeight);
        int startX = pos[0];
        int baseYPos = pos[1];

        renderMisc(graphics, itemsToRender, startX, baseYPos, isVerticalStack());
    }

    /**
     * Called by HudEditorScreen to render a preview at the given position.
     */
    public static void renderPreview(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
        Player player = mc.player;
        if (player == null) return;

        List<Item> targetItems = getTrackedItemsStatic();
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

        renderMisc(graphics, itemsToRender, x, y, isVerticalStack());
    }

    private static List<Item> getTrackedItemsStatic() {
        var opt = ConfigManager.getOption(PVPEssentialsClient.KEY_MISC_ITEMS);
        String raw = opt != null ? opt.value : "";
        if (raw == null) raw = "";

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
                } catch (Exception ignored) {}
            }
        }
        return new ArrayList<>(itemSet);
    }

    private static void renderMisc(GuiGraphicsExtractor g, List<ItemStack> items, int startX, int baseYPos, boolean verticalStack) {
        int totalItems = items.size();

        if (verticalStack) {
            // Vertical stacking: one item per row, expanding upward from bottom
            int itemsPerRow = 1;
            int totalRows = totalItems;

            int currentY = baseYPos + 22;
            for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
                boolean isTopRow = (rowIndex == 0);
                boolean isBottomRow = (rowIndex == totalRows - 1);

                int srcV;
                int srcHeight;
                if (totalRows == 1) {
                    srcV = 0; srcHeight = 22;
                } else if (isTopRow) {
                    srcV = 1; srcHeight = 21;
                } else if (isBottomRow) {
                    srcV = 0; srcHeight = 21;
                } else {
                    srcV = 1; srcHeight = 20;
                }

                int rowTextureWidth = SLOT_PX + 1;
                currentY -= srcHeight;
                g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, currentY, 0, srcV, rowTextureWidth, srcHeight, 182, 22);
                g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + rowTextureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
            }

            for (int i = 0; i < totalItems; i++) {
                int itemX = startX + 3;
                int itemY = (baseYPos + 3) - (i * SLOT_PX);
                RenderUtil.drawScaledItemFactor(g, items.get(i), itemX, itemY, 1.0f);
            }
        } else {
            // Horizontal layout: up to 4 items per row
            int MAX_COLS = 4;
            int itemsPerRow = Math.min(MAX_COLS, totalItems);
            int totalRows = (int) Math.ceil((double) totalItems / itemsPerRow);
            int slotsOnBottomRow = totalItems - (totalRows - 1) * itemsPerRow;
            if (slotsOnBottomRow <= 0) slotsOnBottomRow = itemsPerRow;

            int currentY = baseYPos + 22;
            for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
                boolean isTopRow = (rowIndex == 0);
                boolean isBottomRow = (rowIndex == totalRows - 1);
                int slotsInThisRow = isBottomRow ? slotsOnBottomRow : itemsPerRow;

                int srcV;
                int srcHeight;
                if (totalRows == 1) {
                    srcV = 0; srcHeight = 22;
                } else if (isTopRow) {
                    srcV = 1; srcHeight = 21;
                } else if (isBottomRow) {
                    srcV = 0; srcHeight = 21;
                } else {
                    srcV = 1; srcHeight = 20;
                }

                int rowTextureWidth = (slotsInThisRow * SLOT_PX) + 1;
                currentY -= srcHeight;
                g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, currentY, 0, srcV, rowTextureWidth, srcHeight, 182, 22);
                g.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + rowTextureWidth, currentY, 181, srcV, 1, srcHeight, 182, 22);
            }

            for (int i = 0; i < totalItems; i++) {
                int rowIndex = i / itemsPerRow;
                int slotOffset = i % itemsPerRow;
                int itemX = startX + 3 + (slotOffset * SLOT_PX);
                int itemY = (baseYPos + 3) - (rowIndex * SLOT_PX);
                RenderUtil.drawScaledItemFactor(g, items.get(i), itemX, itemY, 1.0f);
            }
        }
    }
}