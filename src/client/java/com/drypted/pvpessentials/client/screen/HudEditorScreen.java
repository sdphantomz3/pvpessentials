package com.drypted.pvpessentials.client.screen;

import com.drypted.dlib.client.config.ConfigManager;
import com.drypted.pvpessentials.client.PVPEssentialsClient;
import com.drypted.pvpessentials.client.hud.Anchor;
import com.drypted.pvpessentials.client.hud.ArmorHud;
import com.drypted.pvpessentials.client.hud.ArrowHud;
import com.drypted.pvpessentials.client.hud.DamageIndicatorHud;
import com.drypted.pvpessentials.client.hud.HudLayoutStorage;
import com.drypted.pvpessentials.client.hud.MiscHud;
import com.drypted.pvpessentials.client.hud.PotionHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    // Whether auto-adjust is enabled (if true, show error instead of HUD previews)
    private boolean autoAdjustLocked = false;

    // Previous screen to restore on close (DLib config)
    private final Screen previousScreen;

    public HudEditorScreen() {
        this(null);
    }

    public HudEditorScreen(Screen previousScreen) {
        super(TITLE);
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        screenWidth = mc.getWindow().getGuiScaledWidth();
        screenHeight = mc.getWindow().getGuiScaledHeight();

        // Check if auto-adjust mode is active
        var modeOpt = ConfigManager.getOption(PVPEssentialsClient.KEY_HUD_AUTO_ADJUST);
        autoAdjustLocked = modeOpt != null && "Auto Adjust".equals(modeOpt.value);

        // Load current positions from HudLayoutStorage
        loadPositionsFromStorage();

        // Cache originals for discard
        for (String id : posX.keySet()) {
            originalX.put(id, posX.get(id));
            originalY.put(id, posY.get(id));
            originalAnchors.put(id, anchors.get(id));
        }

        // Hotbar-sized button panel: 182px wide, 22px tall, centered, flush with hotbar top
        int panelWidth = 182;
        int panelHeight = 22;
        int panelX = screenWidth / 2 - panelWidth / 2;
        int panelY = screenHeight - 22;

        // 3 touching buttons: Save (green), Discard (red), Reset (blue)
        int btnWidth = panelWidth / 3; // 60px each, touching (60+60+60=180, leaves 2px)
        int btnHeight = 18;

        saveButton = Button.builder(
                Component.literal("\u2713").withColor(0x55FF55),
                btn -> { saveAndClose(); })
                .pos(panelX + 1, panelY + 2)
                .size(btnWidth, btnHeight)
                .build();

        discardButton = Button.builder(
                Component.literal("\u2717").withColor(0xFF5555),
                btn -> { discardAndClose(); })
                .pos(panelX + 1 + btnWidth, panelY + 2)
                .size(btnWidth, btnHeight)
                .build();

        resetButton = Button.builder(
                Component.literal("\u21BB").withColor(0x5555FF),
                btn -> { resetToDefault(); })
                .pos(panelX + 1 + 2 * btnWidth, panelY + 2)
                .size(btnWidth, btnHeight)
                .build();

        addRenderableWidget(saveButton);
        addRenderableWidget(discardButton);
        addRenderableWidget(resetButton);
    }

    // ---------- Rendering ----------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();

        if (autoAdjustLocked) {
            // Auto Adjust mode: don't render HUD previews, show error message
            drawHotbarReference(graphics);
            String msg1 = "HUD Layout Editor is locked in Auto Adjust mode!";
            String msg2 = "Switch to Set Manually in the config to unlock.";
            graphics.drawCenteredString(mc.font, msg1, screenWidth / 2, screenHeight / 2 - 14, 0xFFFF4444);
            graphics.drawCenteredString(mc.font, msg2, screenWidth / 2, screenHeight / 2, 0xFFFF4444);
            super.render(graphics, mouseX, mouseY, delta);
            return;
        }

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
        super.render(graphics, mouseX, mouseY, delta);

        // Draw instructions
        graphics.drawCenteredString(mc.font, "Drag to reposition. Right-click HUD to cycle anchor.",
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

    private void renderHudPreviews(GuiGraphics graphics, Minecraft mc) {
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

    private void drawHudOutlines(GuiGraphics graphics) {
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

    private void drawHudLabels(GuiGraphics graphics) {
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
            graphics.drawCenteredString(mc.font, labelText, x + w / 2, labelY, 0xFFFFFF00);
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

    private void drawHotbarReference(GuiGraphics graphics) {
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
        graphics.drawCenteredString(Minecraft.getInstance().font, "HOTBAR (fixed, cannot place HUDs here)",
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
        closeAndRestore();
    }

    private void discardAndClose() {
        for (String id : originalX.keySet()) {
            posX.put(id, originalX.get(id));
            posY.put(id, originalY.get(id));
        }
        for (String id : originalAnchors.keySet()) {
            anchors.put(id, originalAnchors.get(id));
        }
        savePositionsToStorage();
        clearPreviewMode();
        closeAndRestore();
    }

    /** Close this screen and restore the previous screen (DLib config). */
    private void closeAndRestore() {
        if (this.minecraft != null) {
            if (previousScreen != null) {
                this.minecraft.setScreen(previousScreen);
            } else {
                this.onClose();
            }
        }
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