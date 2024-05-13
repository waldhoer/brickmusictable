package org.brickmusic.bricklogic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.Scalar;

import java.awt.*;

/**
 * This class represents the mapping of instrument channel to a color.
 * Only predefined (field) colors are valid. They can be obtained using findNearest.
 *
 * @see InstrumentColor#findNearest(Scalar, Boolean)
 */
public enum InstrumentColor {
    WHITE(new Color(255, 255, 255), ColorType.BRICK_ONLY),
    BLACK(new Color(0, 0, 0), ColorType.PIN_AND_BRICK),
    RED(new Color(222, 56, 56), ColorType.PIN_AND_BRICK),
    GREEN(new Color(13, 206, 80), ColorType.PIN_AND_BRICK),
    BLUE(new Color(1, 24, 164), ColorType.BRICK_ONLY),
    GREY(new Color(173, 173, 173), ColorType.PIN_ONLY);

    /**
     * The attached color value used for comparison and findNearest method.
     *
     * @see InstrumentColor#findNearest(Scalar, Boolean)
     */
    private final Color color;

    private final ColorType type;

    /**
     * Creates a new Instrument color from a predefined color value
     *
     * @param color The color to set
     */
    InstrumentColor(Color color, ColorType type) {
        this.color = color;
        this.type = type;
    }

    /**
     * Finds the nearest matching color to one of the defined instrument colors
     *
     * @param color The color to map
     * @return The mapped color
     */
    public static InstrumentColor findNearest(@NotNull Scalar color, Boolean pin) {
        int r = (int) color.val[2], g = (int) color.val[1], b = (int) color.val[0];

        InstrumentColor nearest = null;
        for (InstrumentColor instrumentColor : values()) {
            if (pin && !instrumentColor.type.isForPin()) continue;
            if (!pin && !instrumentColor.type.isForBrick()) continue;
            if (nearest == null || (
                    Math.abs(instrumentColor.color.getRed() - r) <= Math.abs(nearest.color.getRed() - r) &&
                            Math.abs(instrumentColor.color.getBlue() - b) <= Math.abs(nearest.color.getBlue() - b) &&
                            Math.abs(instrumentColor.color.getGreen() - g) <= Math.abs(nearest.color.getGreen() - g)
            )) {
                nearest = instrumentColor;
            }
        }
        return nearest;
    }

    /**
     * @return The color as BGR scalar
     */
    @NotNull
    @Contract(" -> new")
    public Scalar getScalar() {
        return new Scalar(color.getBlue(), color.getGreen(), color.getRed());
    }
}