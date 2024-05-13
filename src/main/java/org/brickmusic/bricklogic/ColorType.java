package org.brickmusic.bricklogic;

/**
 * The color type defines the purpose of a color, i.e. for what it can be used to ease identification.
 */
public enum ColorType {
    PIN_ONLY(true, false),
    BRICK_ONLY(false, true),
    PIN_AND_BRICK(true, true);

    /**
     * Describes if the color can be applied to pins
     */
    private final boolean forPin;

    /**
     * Describes if the color can be applied to bricks
     */
    private final boolean forBrick;

    ColorType(boolean pin, boolean brick) {
        this.forPin = pin;
        this.forBrick = brick;
    }

    /**
     * @return True if the color can be applied to bricks, False otherwise
     */
    public boolean isForBrick() {
        return forBrick;
    }

    /**
     * @return True if the color can be applied to pins, False otherwise
     */
    public boolean isForPin() {
        return forPin;
    }
}
