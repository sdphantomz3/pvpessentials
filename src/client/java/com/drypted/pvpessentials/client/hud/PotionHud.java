package com.drypted.pvpessentials.client.hud;

import com.drypted.pvpessentials.client.util.RenderUtil;
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
    private static final int ITEMS_PER_ROW = 4; // Keeps uniform layout matching 4 armor slots

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator())
            return;

        // 1. Scan player inventory and combine identical potion items together
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

        // 2. Setup resolution calculations and mirroring dimensions
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        boolean isRightHanded = player.getMainArm() == HumanoidArm.RIGHT;

        int middleX = screenWidth / 2;
        int baseYPos = screenHeight - 22; // Baseline hotbar height standard
        int hudWidth = 82; 
        
        // FIX 1: PotionHUD remains stationary, ignoring offhand shifting entirely
        int startX;
        if (isRightHanded) {
            // Armor HUD is on Left (moves with offhand) -> Potion HUD is anchored on the RIGHT
            startX = middleX + 91 + 7;
        } else {
            // Armor HUD is on Right (moves with offhand) -> Potion HUD is anchored on the LEFT
            startX = (middleX - 91 - 7) - hudWidth;
        }

        // 3. Compute row counts
        int totalItems = trackedPotions.size();
        int totalRows = (int) Math.ceil((double) totalItems / ITEMS_PER_ROW);

        // FIX 2: Slicing texture coordinates dynamically to build "One Big Connected Container"
        int currentY = baseYPos + 22; // Track bottom-up boundary rendering cursor

        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            int srcV;
            int srcHeight;
            
            if (totalRows == 1) {
                srcV = 0;
                srcHeight = 22; // Default full hotbar texture size
            } else if (rowIndex == 0) {
                // Bottom row: Keep bottom border shadow, shave off top inner border row
                srcV = 1;
                srcHeight = 21;
            } else if (rowIndex == totalRows - 1) {
                // Top row: Keep top shiny highlight border, shave off bottom inner border row
                srcV = 0;
                srcHeight = 21;
            } else {
                // Middle rows: Shave off both top and bottom outer border pixel tracks
                srcV = 1;
                srcHeight = 20;
            }
            
            currentY -= srcHeight; // Seamlessly shift cursor up by the exact sliced pixel height
            int rowY = currentY;

            // Render the continuous frame textures
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX, rowY, 0, srcV, 81, srcHeight, 182, 22);
            graphics.blit(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, startX + 81, rowY, 181, srcV, 1, srcHeight, 182, 22);
        }

        // 4. Render items on top using a fixed grid coordinate offset tracking
        for (int i = 0; i < totalItems; i++) {
            ItemStack potionStack = trackedPotions.get(i);
            
            int rowIndex = i / ITEMS_PER_ROW;
            int slotOffsetIndex = i % ITEMS_PER_ROW;
            
            int itemX = startX + 3 + (slotOffsetIndex * 20);
            // Uses fixed absolute mathematical grid alignment independent of the texture row slices
            int itemY = (baseYPos + 3) - (rowIndex * 20);

            RenderUtil.drawScaledItemFactor(graphics, potionStack, itemX, itemY, 1.0f);
        }
    }

    private boolean isPotion(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}