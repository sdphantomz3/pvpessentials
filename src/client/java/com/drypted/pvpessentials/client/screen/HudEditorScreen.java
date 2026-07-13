package com.drypted.pvpessentials.client.screen;

import com.drypted.pvpessentials.client.hud.Anchor;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.ArrowHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.HudLayoutStorage;
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

    // HUD element identifiers (must match HudLayoutStorage keys)
    public static final String HUD_ARMOR = "armor";
    public static final String HUD_POTION = "potion";
    public static final String HUD_MISC = "misc";
    public static final String HUD_ARROW = "arrow";
    public static final String HUD_DAMAGE = "damage";

    private record HudElement(String id, String displayName, int defaultWidth, int defaultHeight) {}

    private static final HudElement[] HUD_ELEMENTS = {
            new HudElement(HUD_ARMOR, "Armor HUD", 82, 22),
            new HudElement(HUD_POTION, "Potion HUD", 82, 22),
            new HudElement(HUD_MISC, "Misc HUD", 22, 22),
            new HudElement(HUD_ARROW, "Arrow HUD", 30, 18),
            new HudElement(HUD_DAMAGE, "Damage Indicator", 30, 14),
    };

    // Current positions stored as normalized percentages (0.0–1.0)
    private final Map<String, Float> posX = new LinkedHashMap<>();
    private final Map<String, Float> posY = new LinkedHashMap<>();
    private final Map<String, Anchor> anchors = new LinkedHashMap<>();

    // Original values at screen open (for Discard)
    private final Map<String, Float> originalX = new LinkedHashMap<>();
    private final Map<String, Float> originalY = new LinkedHashMap<>();
    private final Map<String, Anchor> originalAnchors = new LinkedHashMap<>();

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

        // Load current positions from HudLayoutStorage
        loadPositionsFromStorage();

        // Cache originals for discard
        for (String id : posX.keySet()) {
            originalX.put(id, posX.get(id));
            originalY.put(id, posY.get(id));
            originalAnchors.put(id, anchors.get(id));
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

    // ---------- Rendering ----------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        // Set preview mode on all HUDs
        setAllPreviewPositions();

        // Render HUD previews using each HUD's renderPreview method
        renderHudPreviews(graphics, mc);

        // Draw selection outlines around each HUD
        drawHudOutlines(graphics);

        // Draw HUD names and anchor info
        drawHudLabels(graphics);

        // Draw the hotbar reference area (semi-transparent)
        drawHotbarReference(graphics);

        // Render buttons on top
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Draw instructions
        graphics.centeredText(mc.font, "Drag to reposition. Right-click HUD to cycle anchor. Buttons are above hotbar.",
                screenWidth / 2, 10, 0xCCCCCC);
    }

    private void setAllPreviewPositions() {
        setHudPreview(HUD_ARMOR, ArmorHud.class);
        setHudPreview(HUD_POTION, PotionHud.class);
        setHudPreview(HUD_MISC, MiscHud.class);
        setHudPreview(HUD_ARROW, ArrowHud.class);
        setHudPreview(HUD_DAMAGE, DamageIndicatorHud.class);
    }

    private void setHudPreview(String id, Class<?> hudClass) {
        float xp = posX.getOrDefault(id, 0.5f);
        float yp = posY.getOrDefault(id, 0.5f);
        Anchor a = anchors.getOrDefault(id, Anchor.TOP_LEFT);

        if (hudClass == ArmorHud.class) {
            ArmorHud.previewX = xp; ArmorHud.previewY = yp;
            ArmorHud.previewAnchor = a; ArmorHud.previewMode = true;
        } else if (hudClass == PotionHud.class) {
            PotionHud.previewX = xp; PotionHud.previewY = yp;
            PotionHud.previewAnchor = a; PotionHud.previewMode = true;
        } else if (hudClass == MiscHud.class) {
            MiscHud.previewX = xp; MiscHud.previewY = yp;
            MiscHud.previewAnchor = a; MiscHud.previewMode = true;
        } else if (hudClass == ArrowHud.class) {
            ArrowHud.previewX = xp; ArrowHud.previewY = yp;
            ArrowHud.previewAnchor = a; ArrowHud.previewMode = true;
        } else if (hudClass == DamageIndicatorHud.class) {
            DamageIndicatorHud.previewX = xp; DamageIndicatorHud.previewY = yp;
            DamageIndicatorHud.previewAnchor = a; DamageIndicatorHud.previewMode = true;
        }
    }

    /** Compute pixel position for a HUD element from its stored percentage + anchor. */
    private int[] getPixelPos(String hudId) {
        HudElement elem = findElement(hudId);
        if (elem == null) return new int[] {0, 0};
        float xp = posX.getOrDefault(hudId, 0.5f);
        float yp = posY.getOrDefault(hudId, 0.5f);
        Anchor a = anchors.getOrDefault(hudId, Anchor.TOP_LEFT);
        return a.toPixel(xp, yp, screenWidth, screenHeight, elem.defaultWidth, elem.defaultHeight);
    }

    /** Convert pixel coordinates back to percentage using the HUD's anchor. */
    private void setPercentFromPixel(String hudId, int pixelX, int pixelY) {
        HudElement elem = findElement(hudId);
        if (elem == null) return;
        Anchor a = anchors.getOrDefault(hudId, Anchor.TOP_LEFT);
        float[] pct = a.fromPixel(pixelX, pixelY, screenWidth, screenHeight, elem.defaultWidth, elem.defaultHeight);
        posX.put(hudId, clampPercent(pct[0]));
        posY.put(hudId, clampPercent(pct[1]));
    }

    private static float clampPercent(float val) {
        return Math.max(0f, Math.min(1f, val));
    }

    private void renderHudPreviews(GuiGraphicsExtractor graphics, Minecraft mc) {
        for (HudElement elem : HUD_ELEMENTS) {
            int[] px = getPixelPos(elem.id);
            switch (elem.id) {
                case HUD_ARMOR   -> ArmorHud.renderPreview(graphics, mc, px[0], px[1]);
                case HUD_POTION  -> PotionHud.renderPreview(graphics, mc, px[0], px[1]);
                case HUD_MISC    -> MiscHud.renderPreview(graphics, mc, px[0], px[1]);
                case HUD_ARROW   -> ArrowHud.renderPreview(graphics, mc, px[0], px[1]);
                case HUD_DAMAGE  -> DamageIndicatorHud.renderPreview(graphics, mc, px[0], px[1]);
            }
        }
    }

    private void drawHudOutlines(GuiGraphicsExtractor graphics) {
        for (HudElement elem : HUD_ELEMENTS) {
            int[] px = getPixelPos(elem.id);
            int x = px[0], y = px[1];
            int w = elem.defaultWidth, h = elem.defaultHeight;

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
            int[] px = getPixelPos(elem.id);
            int x = px[0], y = px[1];
            int w = elem.defaultWidth;

            Anchor a = anchors.getOrDefault(elem.id, Anchor.TOP_LEFT);
            String labelText = elem.displayName + " [" + formatAnchor(a) + "]";

            int labelY = y - 12;
            if (labelY < 6) labelY = y + elem.defaultHeight + 4;
            // Dark backdrop for readability
            int textWidth = mc.font.width(labelText);
            graphics.fill(x + w / 2 - textWidth / 2 - 2, labelY - 1,
                    x + w / 2 + textWidth / 2 + 2, labelY + 9, 0xAA000000);
            graphics.centeredText(mc.font, labelText, x + w / 2, labelY, 0xFFFFFF);
        }
    }

    private static String formatAnchor(Anchor a) {
        return switch (a) {
            case TOP_LEFT     -> "TL";
            case TOP_RIGHT    -> "TR";
            case BOTTOM_LEFT  -> "BL";
            case BOTTOM_RIGHT -> "BR";
            case CENTER       -> "C";
        };
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
        graphics.centeredText(Minecraft.getInstance().font, "HOTBAR (fixed, cannot place HUDs here)",
                centerX, screenHeight - 28, 0x666666);
    }

    // ---------- Mouse handling ----------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDragging) {
        if (isDragging) return super.mouseClicked(event, isDragging);

        double mouseX = event.x();
        double mouseY = event.y();

        // Right-click: cycle anchor of the HUD under the cursor
        if (event.button() == 1) {
            for (HudElement elem : HUD_ELEMENTS) {
                int[] px = getPixelPos(elem.id);
                int x = px[0], y = px[1];
                if (mouseX >= x && mouseX <= x + elem.defaultWidth &&
                        mouseY >= y && mouseY <= y + elem.defaultHeight) {
                    Anchor current = anchors.getOrDefault(elem.id, Anchor.TOP_LEFT);
                    anchors.put(elem.id, current.next());
                    return true;
                }
            }
            return super.mouseClicked(event, false);
        }

        // Left-click: start dragging
        if (event.button() == 0) {
            for (HudElement elem : HUD_ELEMENTS) {
                int[] px = getPixelPos(elem.id);
                int x = px[0], y = px[1];
                if (mouseX >= x && mouseX <= x + elem.defaultWidth &&
                        mouseY >= y && mouseY <= y + elem.defaultHeight) {
                    draggingHud = elem.id;
                    dragOffsetX = (int) mouseX - x;
                    dragOffsetY = (int) mouseY - y;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, false);
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
            int newPixelX = (int) mouseX - dragOffsetX;
            int newPixelY = (int) mouseY - dragOffsetY;

            // Clamp to screen bounds
            HudElement elem = findElement(draggingHud);
            if (elem != null) {
                newPixelX = Math.max(0, Math.min(newPixelX, screenWidth - elem.defaultWidth));
                newPixelY = Math.max(0, Math.min(newPixelY, screenHeight - elem.defaultHeight));
            }

            // Convert clamped pixel back to percentage using the current anchor
            setPercentFromPixel(draggingHud, newPixelX, newPixelY);
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
        savePositionsToStorage();
        clearPreviewMode();
        this.onClose();
    }

    private void discardAndClose() {
        // Restore original positions and anchors
        for (String id : originalX.keySet()) {
            posX.put(id, originalX.get(id));
            posY.put(id, originalY.get(id));
        }
        for (String id : originalAnchors.keySet()) {
            anchors.put(id, originalAnchors.get(id));
        }
        savePositionsToStorage();
        clearPreviewMode();
        this.onClose();
    }

    private void resetToDefault() {
        // Reset to calculated default positions (percentages) and default anchors
        posX.put(HUD_ARMOR, ArmorHud.getDefaultXPercent(screenWidth));
        posY.put(HUD_ARMOR, ArmorHud.getDefaultYPercent(screenHeight));
        anchors.put(HUD_ARMOR, Anchor.BOTTOM_LEFT);

        posX.put(HUD_POTION, PotionHud.getDefaultXPercent(screenWidth));
        posY.put(HUD_POTION, PotionHud.getDefaultYPercent(screenHeight));
        anchors.put(HUD_POTION, Anchor.BOTTOM_RIGHT);

        posX.put(HUD_MISC, MiscHud.getDefaultXPercent(screenWidth));
        posY.put(HUD_MISC, MiscHud.getDefaultYPercent(screenHeight));
        anchors.put(HUD_MISC, Anchor.BOTTOM_RIGHT);

        posX.put(HUD_ARROW, ArrowHud.getDefaultXPercent(screenWidth));
        posY.put(HUD_ARROW, ArrowHud.getDefaultYPercent(screenHeight));
        anchors.put(HUD_ARROW, Anchor.TOP_LEFT);

        posX.put(HUD_DAMAGE, DamageIndicatorHud.getDefaultXPercent(screenWidth));
        posY.put(HUD_DAMAGE, DamageIndicatorHud.getDefaultYPercent(screenHeight));
        anchors.put(HUD_DAMAGE, Anchor.TOP_LEFT);
    }

    // ---------- Persistence (via HudLayoutStorage) ----------

    private void loadPositionsFromStorage() {
        loadPosition(HUD_ARMOR, ArmorHud.getDefaultXPercent(screenWidth), ArmorHud.getDefaultYPercent(screenHeight),
                Anchor.BOTTOM_LEFT);
        loadPosition(HUD_POTION, PotionHud.getDefaultXPercent(screenWidth), PotionHud.getDefaultYPercent(screenHeight),
                Anchor.BOTTOM_RIGHT);
        loadPosition(HUD_MISC, MiscHud.getDefaultXPercent(screenWidth), MiscHud.getDefaultYPercent(screenHeight),
                Anchor.BOTTOM_RIGHT);
        loadPosition(HUD_ARROW, ArrowHud.getDefaultXPercent(screenWidth), ArrowHud.getDefaultYPercent(screenHeight),
                Anchor.TOP_LEFT);
        loadPosition(HUD_DAMAGE, DamageIndicatorHud.getDefaultXPercent(screenWidth), DamageIndicatorHud.getDefaultYPercent(screenHeight),
                Anchor.TOP_LEFT);
    }

    private void loadPosition(String hudId, float defaultXPercent, float defaultYPercent, Anchor defaultAnchor) {
        float x = HudLayoutStorage.getX(hudId, defaultXPercent);
        float y = HudLayoutStorage.getY(hudId, defaultYPercent);
        Anchor a = HudLayoutStorage.getAnchor(hudId, defaultAnchor);
        posX.put(hudId, x);
        posY.put(hudId, y);
        anchors.put(hudId, a);
    }

    private void savePositionsToStorage() {
        savePosition(HUD_ARMOR);
        savePosition(HUD_POTION);
        savePosition(HUD_MISC);
        savePosition(HUD_ARROW);
        savePosition(HUD_DAMAGE);
        HudLayoutStorage.save();
    }

    private void savePosition(String hudId) {
        HudLayoutStorage.set(hudId,
                posX.getOrDefault(hudId, 0.5f),
                posY.getOrDefault(hudId, 0.5f),
                anchors.getOrDefault(hudId, Anchor.TOP_LEFT));
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