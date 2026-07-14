package com.drypted.pvpessentials.client.hud;

/**
 * Defines how a HUD element's position is interpreted relative to the
 * normalized (percentage-based) coordinates stored in config.
 *
 * <p>For example, with {@code TOP_LEFT} and {@code xPercent=0.5, yPercent=0.5},
 * the element's top-left corner sits at 50% of the screen width and height.
 * With {@code BOTTOM_RIGHT}, the element's bottom-right corner sits at that point.</p>
 */
public enum Anchor {

    /** The percentage point is the top-left corner of the HUD element. */
    TOP_LEFT,

    /** The percentage point is the top-right corner of the HUD element. */
    TOP_RIGHT,

    /** The percentage point is the bottom-left corner of the HUD element. */
    BOTTOM_LEFT,

    /** The percentage point is the bottom-right corner of the HUD element. */
    BOTTOM_RIGHT,

    /** The percentage point is the center of the HUD element. */
    CENTER;

    /**
     * Converts normalized percentage coordinates to pixel coordinates
     * based on this anchor and the element's dimensions.
     *
     * @param xPercent      normalized x (0.0–1.0)
     * @param yPercent      normalized y (0.0–1.0)
     * @param screenWidth   current GUI-scaled screen width in pixels
     * @param screenHeight  current GUI-scaled screen height in pixels
     * @param elementWidth  width of the HUD element in pixels
     * @param elementHeight height of the HUD element in pixels
     * @return [pixelX, pixelY]
     */
    public int[] toPixel(float xPercent, float yPercent,
                         int screenWidth, int screenHeight,
                         int elementWidth, int elementHeight) {
        float baseX = xPercent * screenWidth;
        float baseY = yPercent * screenHeight;

        return switch (this) {
            case TOP_LEFT     -> new int[] { Math.round(baseX), Math.round(baseY) };
            case TOP_RIGHT    -> new int[] { Math.round(baseX - elementWidth), Math.round(baseY) };
            case BOTTOM_LEFT  -> new int[] { Math.round(baseX), Math.round(baseY - elementHeight) };
            case BOTTOM_RIGHT -> new int[] { Math.round(baseX - elementWidth), Math.round(baseY - elementHeight) };
            case CENTER       -> new int[] { Math.round(baseX - elementWidth / 2f), Math.round(baseY - elementHeight / 2f) };
        };
    }

    /**
     * Converts pixel coordinates back to normalized percentage coordinates.
     * This is the inverse of {@link #toPixel}.
     *
     * @param pixelX        pixel x of the top-left of the element
     * @param pixelY        pixel y of the top-left of the element
     * @param screenWidth   current GUI-scaled screen width in pixels
     * @param screenHeight  current GUI-scaled screen height in pixels
     * @param elementWidth  width of the HUD element in pixels
     * @param elementHeight height of the HUD element in pixels
     * @return [xPercent, yPercent]
     */
    public float[] fromPixel(int pixelX, int pixelY,
                             int screenWidth, int screenHeight,
                             int elementWidth, int elementHeight) {
        return switch (this) {
            case TOP_LEFT     -> new float[] { (float) pixelX / screenWidth, (float) pixelY / screenHeight };
            case TOP_RIGHT    -> new float[] { (float) (pixelX + elementWidth) / screenWidth, (float) pixelY / screenHeight };
            case BOTTOM_LEFT  -> new float[] { (float) pixelX / screenWidth, (float) (pixelY + elementHeight) / screenHeight };
            case BOTTOM_RIGHT -> new float[] { (float) (pixelX + elementWidth) / screenWidth, (float) (pixelY + elementHeight) / screenHeight };
            case CENTER       -> new float[] { (float) (pixelX + elementWidth / 2f) / screenWidth, (float) (pixelY + elementHeight / 2f) / screenHeight };
        };
    }

    /**
     * Returns the next anchor in cycle order (for UI cycling).
     */
    public Anchor next() {
        Anchor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
