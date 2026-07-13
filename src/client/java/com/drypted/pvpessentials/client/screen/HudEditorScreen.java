package com.drypted.pvpessentials.client.screen;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.ArrowHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.MiscHud;
import com.drypted.pvpessentials.client.hud.PotionHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public class HudEditorScreen extends Screen {

    private static final Component TITLE = Component.literal("HUD Layout Editor");

    // HUD element identifiers
    public static final String HUD_ARMOR = "armor";
    public static final String HUD_POTION = "potion";
    public static final String HUD_MISC = "misc";
    public static final String HUD_ARROW = "arrow";
    public static final String HUD_DAMAGE = "damage";

    private record HudElement(String id, String displayName, int defaultWidth, int defaultHeight) {}

    private static final HudElement[] HUD_ELEMENTS = {
            new HudElement(HUD_ARMOR, "Armor HUD", 82, 22),
            new HudElement(HUD_POTION, "Potion HUD", 82, 22),
            new HudElement(HUD_MISC, "Misc HUD", 82, 22),
            new HudElement(HUD_ARROW, "Arrow HUD", 40, 20),
            new HudElement(HUD_DAMAGE, "Damage Indicator", 80, 30),
    };

    // Current positions (in GUI-scaled coordinates) — these are the working copies
    private final Map<String, Integer> posX = new LinkedHashMap<>();
    private final Map<String, Integer> posY = new LinkedHashMap<>();

    // Original positions at screen open (for Discard)
    private final Map<String, Integer> originalX = new LinkedHashMap<>();
    private final Map<String, Integer> originalY = new LinkedHashMap<>();

    // Drag state
    private String draggingHud = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Buttons
    private Button saveButton;
    private Button discardButton;
    private Button resetButton;

    private int screenWidth;
    private int screenHeight;

    public HudEditorScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        screenWidth = mc.getWindow().getGuiScaledWidth();
        screenHeight = mc.getWindow().getGuiScaledHeight();

        // Load current positions from config
        loadPositionsFromConfig();

        // Cache originals for discard
        for (String id : posX.keySet()) {
            originalX.put(id, posX.get(id));
            originalY.put(id, posY.get(id));
        }

        // Buttons above the hotbar area
        int buttonY = screenHeight - 42;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int centerX = screenWidth / 2;

        saveButton = Button.builder(Component.literal("Save"), btn -> saveAndClose())
                .pos(centerX - 130, buttonY)
                .size(buttonWidth, buttonHeight)
                .build();

        discardButton = Button.builder(Component.literal("Discard"), btn -> discardAndClose())
                .pos(centerX - 40, buttonY)
                .size(buttonWidth, buttonHeight)
                .build();

        resetButton = Button.builder(Component.literal("Reset to Default"), btn -> resetToDefault())
                .pos(centerX + 50, buttonY)
                .size(buttonWidth + 20, buttonHeight)
                .build();

        addRenderableWidget(saveButton);
        addRenderableWidget(discardButton);
        addRenderableWidget(resetButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        // Set preview mode on all HUDs
        setAllPreviewPositions();

        // Render HUD previews using each HUD's renderPreview method
        renderHudPreviews(graphics, mc);

        // Draw selection outlines around each HUD
        drawHudOutlines(graphics);

        // Draw HUD names
        drawHudLabels(graphics);

        // Draw the hotbar reference area (semi-transparent)
        drawHotbarReference(graphics);

        // Render buttons on top
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Draw instructions
        graphics.centeredText(mc.font, "Drag HUDs to reposition them. Buttons are above the hotbar.", screenWidth / 2, 10, 0xCCCCCC);
    }

    private void setAllPreviewPositions() {
        ArmorHud.previewX = posX.getOrDefault(HUD_ARMOR, 0);
        ArmorHud.previewY = posY.getOrDefault(HUD_ARMOR, 0);
        ArmorHud.previewMode = true;

        PotionHud.previewX = posX.getOrDefault(HUD_POTION, 0);
        PotionHud.previewY = posY.getOrDefault(HUD_POTION, 0);
        PotionHud.previewMode = true;

        MiscHud.previewX = posX.getOrDefault(HUD_MISC, 0);
        MiscHud.previewY = posY.getOrDefault(HUD_MISC, 0);
        MiscHud.previewMode = true;

        ArrowHud.previewX = posX.getOrDefault(HUD_ARROW, 0);
        ArrowHud.previewY = posY.getOrDefault(HUD_ARROW, 0);
        ArrowHud.previewMode = true;

        DamageIndicatorHud.previewX = posX.getOrDefault(HUD_DAMAGE, 0);
        DamageIndicatorHud.previewY = posY.getOrDefault(HUD_DAMAGE, 0);
        DamageIndicatorHud.previewMode = true;
    }

    private void renderHudPreviews(GuiGraphicsExtractor graphics, Minecraft mc) {
        // Armor HUD preview
        ArmorHud.renderPreview(graphics, mc,
                posX.getOrDefault(HUD_ARMOR, 0),
                posY.getOrDefault(HUD_ARMOR, 0));

        // Potion HUD preview
        PotionHud.renderPreview(graphics, mc,
                posX.getOrDefault(HUD_POTION, 0),
                posY.getOrDefault(HUD_POTION, 0));

        // Misc HUD preview
        MiscHud.renderPreview(graphics, mc,
                posX.getOrDefault(HUD_MISC, 0),
                posY.getOrDefault(HUD_MISC, 0));

        // Arrow HUD preview
        ArrowHud.renderPreview(graphics, mc,
                posX.getOrDefault(HUD_ARROW, 0),
                posY.getOrDefault(HUD_ARROW, 0));

        // Damage Indicator preview
        DamageIndicatorHud.renderPreview(graphics, mc,
                posX.getOrDefault(HUD_DAMAGE, 0),
                posY.getOrDefault(HUD_DAMAGE, 0));
    }

    private void drawHudOutlines(GuiGraphicsExtractor graphics) {
        for (HudElement elem : HUD_ELEMENTS) {
            int x = posX.getOrDefault(elem.id, 0);
            int y = posY.getOrDefault(elem.id, 0);
            int w = elem.defaultWidth;
            int h = elem.defaultHeight;

            boolean isDragging = elem.id.equals(draggingHud);
            int color = isDragging ? 0xFFFFFF00 : 0x88FFFFFF;

            // Draw selection rectangle
            graphics.fill(x - 1, y - 1, x + w + 1, y, color);                         // top
            graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, color);                  // bottom
            graphics.fill(x - 1, y, x, y + h, color);                                   // left
            graphics.fill(x + w, y, x + w + 1, y + h, color);                           // right
        }
    }

    private void drawHudLabels(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        for (HudElement elem : HUD_ELEMENTS) {
            int x = posX.getOrDefault(elem.id, 0);
            int y = posY.getOrDefault(elem.id, 0);
            int w = elem.defaultWidth;
            int labelY = y - 10;
            if (labelY < 4) labelY = y + elem.defaultHeight + 2;
            graphics.centeredText(mc.font, elem.displayName, x + w / 2, labelY, 0xAAAAAA);
        }
    }

    private void drawHotbarReference(GuiGraphicsExtractor graphics) {
        int centerX = screenWidth / 2;
        int hotbarY = screenHeight - 23;

        // Semi-transparent hotbar outline
        int hotbarLeft = centerX - 91;
        int hotbarRight = centerX + 91;
        int alpha = 0x44FFFFFF;

        graphics.fill(hotbarLeft, hotbarY, hotbarRight, hotbarY + 1, alpha);
        graphics.fill(hotbarLeft, hotbarY + 21, hotbarRight, hotbarY + 22, alpha);
        graphics.fill(hotbarLeft, hotbarY, hotbarLeft + 1, hotbarY + 22, alpha);
        graphics.fill(hotbarRight - 1, hotbarY, hotbarRight, hotbarY + 22, alpha);

        // Label
        graphics.centeredText(Minecraft.getInstance().font, "HOTBAR (fixed, cannot place HUDs here)", centerX, screenHeight - 28, 0x666666);
    }

    // ---------- Mouse handling ----------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDragging) {
        if (!isDragging && event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            for (HudElement elem : HUD_ELEMENTS) {
                int x = posX.getOrDefault(elem.id, 0);
                int y = posY.getOrDefault(elem.id, 0);
                if (mouseX >= x && mouseX <= x + elem.defaultWidth &&
                        mouseY >= y && mouseY <= y + elem.defaultHeight) {
                    draggingHud = elem.id;
                    dragOffsetX = (int) mouseX - x;
                    dragOffsetY = (int) mouseY - y;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDragging);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingHud != null) {
            draggingHud = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingHud != null) {
            double mouseX = event.x();
            double mouseY = event.y();
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            // Clamp to screen bounds
            HudElement elem = findElement(draggingHud);
            if (elem != null) {
                newX = Math.max(0, Math.min(newX, screenWidth - elem.defaultWidth));
                newY = Math.max(0, Math.min(newY, screenHeight - elem.defaultHeight));
            }

            posX.put(draggingHud, newX);
            posY.put(draggingHud, newY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private HudElement findElement(String id) {
        for (HudElement e : HUD_ELEMENTS) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    // ---------- Button actions ----------

    private void saveAndClose() {
        savePositionsToConfig();
        clearPreviewMode();
        this.onClose();
    }

    private void discardAndClose() {
        // Restore original positions to config
        for (String id : originalX.keySet()) {
            posX.put(id, originalX.get(id));
            posY.put(id, originalY.get(id));
        }
        savePositionsToConfig();
        clearPreviewMode();
        this.onClose();
    }

    private void resetToDefault() {
        // Reset to calculated default positions
        int centerX = screenWidth / 2;
        posX.put(HUD_ARMOR, centerX - 91 - 7 - 82);     // left of hotbar
        posY.put(HUD_ARMOR, screenHeight - 22);

        posX.put(HUD_POTION, centerX + 91 + 7);          // right of hotbar
        posY.put(HUD_POTION, screenHeight - 22);

        posX.put(HUD_MISC, centerX + 91 + 7);            // right of hotbar, above potions
        posY.put(HUD_MISC, screenHeight - 44);

        posX.put(HUD_ARROW, centerX + 12);                // right of crosshair
        posY.put(HUD_ARROW, screenHeight / 2 - 8);

        posX.put(HUD_DAMAGE, centerX - 35);               // left of crosshair
        posY.put(HUD_DAMAGE, screenHeight / 2 - 4);
    }

    // ---------- Config persistence ----------

    private void loadPositionsFromConfig() {
        loadPosition(HUD_ARMOR, PVPEssentialsClient.KEY_HUD_ARMOR_X, PVPEssentialsClient.KEY_HUD_ARMOR_Y);
        loadPosition(HUD_POTION, PVPEssentialsClient.KEY_HUD_POTION_X, PVPEssentialsClient.KEY_HUD_POTION_Y);
        loadPosition(HUD_MISC, PVPEssentialsClient.KEY_HUD_MISC_X, PVPEssentialsClient.KEY_HUD_MISC_Y);
        loadPosition(HUD_ARROW, PVPEssentialsClient.KEY_HUD_ARROW_X, PVPEssentialsClient.KEY_HUD_ARROW_Y);
        loadPosition(HUD_DAMAGE, PVPEssentialsClient.KEY_HUD_DAMAGE_X, PVPEssentialsClient.KEY_HUD_DAMAGE_Y);
    }

    private void loadPosition(String hudId, String keyX, String keyY) {
        int centerX = screenWidth / 2;
        int defaultX, defaultY;
        switch (hudId) {
            case HUD_ARMOR:
                defaultX = centerX - 91 - 7 - 82;
                defaultY = screenHeight - 22;
                break;
            case HUD_POTION:
                defaultX = centerX + 91 + 7;
                defaultY = screenHeight - 22;
                break;
            case HUD_MISC:
                defaultX = centerX + 91 + 7;
                defaultY = screenHeight - 44;
                break;
            case HUD_ARROW:
                defaultX = centerX + 12;
                defaultY = screenHeight / 2 - 8;
                break;
            case HUD_DAMAGE:
                defaultX = centerX - 35;
                defaultY = screenHeight / 2 - 4;
                break;
            default:
                defaultX = 10;
                defaultY = 10;
        }

        int x = readConfigInt(keyX, defaultX);
        int y = readConfigInt(keyY, defaultY);
        posX.put(hudId, x);
        posY.put(hudId, y);
    }

    private int readConfigInt(String key, int defaultValue) {
        var opt = ConfigManager.getOption(key);
        if (opt != null && opt.value != null && !opt.value.isEmpty()) {
            try {
                return Integer.parseInt(opt.value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private void savePositionsToConfig() {
        savePosition(HUD_ARMOR, PVPEssentialsClient.KEY_HUD_ARMOR_X, PVPEssentialsClient.KEY_HUD_ARMOR_Y);
        savePosition(HUD_POTION, PVPEssentialsClient.KEY_HUD_POTION_X, PVPEssentialsClient.KEY_HUD_POTION_Y);
        savePosition(HUD_MISC, PVPEssentialsClient.KEY_HUD_MISC_X, PVPEssentialsClient.KEY_HUD_MISC_Y);
        savePosition(HUD_ARROW, PVPEssentialsClient.KEY_HUD_ARROW_X, PVPEssentialsClient.KEY_HUD_ARROW_Y);
        savePosition(HUD_DAMAGE, PVPEssentialsClient.KEY_HUD_DAMAGE_X, PVPEssentialsClient.KEY_HUD_DAMAGE_Y);
    }

    private void savePosition(String hudId, String keyX, String keyY) {
        writeConfigValue(keyX, posX.getOrDefault(hudId, 0));
        writeConfigValue(keyY, posY.getOrDefault(hudId, 0));
    }

    private void writeConfigValue(String key, int value) {
        var opt = ConfigManager.getOption(key);
        if (opt != null) {
            opt.value = String.valueOf(value);
        }
    }

    private static void clearPreviewMode() {
        ArmorHud.previewMode = false;
        PotionHud.previewMode = false;
        MiscHud.previewMode = false;
        ArrowHud.previewMode = false;
        DamageIndicatorHud.previewMode = false;
    }

    @Override
    public void onClose() {
        clearPreviewMode();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
