package org.brickmusic.bricklogic;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;

import java.util.Arrays;
import java.util.Comparator;

/**
 * BrickTypes describe LEGO bricks in LEGO pin units.
 * Visuals can be converted to BrickTypes using the identify method.
 */
public enum BrickType {
    /**
     * Indicating that an object was not mapped to a specific brick, possibly to missing
     * enum types or an invalid visual object.
     */
    UNIDENTIFIABLE(-1, -1, "!UIF"),

    GROUND_PLATE(32, 16, "GP"),
    SOLID_1x2(2, 1, "S2x1"),
    SOLID_2x2(2, 2, "S2x2"),
    SOLID_4x2(4, 2, "S4x2"),
    SOLID_6x2(6, 2, "S6x2"),
    SOLID_8x2(8, 2, "S8x2"),
    PIN(1, 1, "PI1x1");

    /**
     * Additional upscaling factor for width identification.
     *
     * @see BrickType#identify(RotatedRect, Size)
     */
    private static final double WIDTH_CORRECTION_FACTOR = 0.9;

    /**
     * Additional upscaling factor for height identification.
     * Especially required for on-border-brick cases
     *
     * @see BrickType#identify(RotatedRect, Size, boolean)
     */
    private static final double HEIGHT_CORRECTION_FACTOR = 1.1;

    /**
     * Width of the Brick in LEGO Pin Size
     */
    private final int width;

    /**
     * Height of the Brick in LEGO Pin Size
     */
    private final int height;

    /**
     * Short name of the Brick name for presentation
     */
    private final String name;

    /**
     * Creates a new Brick Type
     *
     * @param width  Width of brick
     * @param height Height of brick
     * @param name   The name of the brick
     */
    BrickType(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.name = name;
    }

    /**
     * @return Width of the Brick in LEGO Pin size
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return Height of the Brick in LEGO Pin size
     */
    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Defines if a brick shall be attached with rotation or not.
     * Note: Small bricks may not be rotated due to brick structure.
     *
     * @return True if the brick type can be rotated, false otherwise
     */
    public boolean rotatable() {
        return this != BrickType.PIN && this != BrickType.SOLID_1x2;
    }


    /**
     * Maps a rectangle block to a brick type with predefined size
     *
     * @param block       The block to identify
     * @param groundPlate The ground plate object
     * @return The BrickType most compatible with the given block
     */
    public static BrickType identify(@NotNull RotatedRect block, @NotNull Size groundPlate) {
        return identify(block, groundPlate, false);
    }

    /**
     * Maps a rectangle block to a brick type with predefined size
     *
     * @param block       The block to identify
     * @param groundPlate The ground plate object
     * @param borderCase  If the brick identified lies within the outer image boundary
     * @return The BrickType most compatible with the given block
     */
    public static BrickType identify(@NotNull RotatedRect block, @NotNull Size groundPlate, boolean borderCase) {
        final int width =
                (int) Math.round((GROUND_PLATE.width / groundPlate.width) * block.size.width * WIDTH_CORRECTION_FACTOR);
        final int height =
                (int) Math.round((GROUND_PLATE.height / groundPlate.height) * block.size.height *
                        ((borderCase) ? HEIGHT_CORRECTION_FACTOR : 1));
        return identify(width, height);
    }

    /**
     * Map a given height and width to a specific BrickType
     *
     * @param width  The width to determine
     * @param height The height to determine
     * @return The evaluated brick type or UNIDENTIFIABLE if nothing matched
     */
    public static BrickType identify(int width, int height) {
        return Arrays.stream(BrickType.values())
                .filter(v -> !v.equals(GROUND_PLATE) && !v.equals(UNIDENTIFIABLE))
                .min(Comparator.comparingDouble(o ->
                        Math.abs((double) o.width - width) + Math.abs((double) o.height - height)
                                + Math.abs((double) (o.width * o.height) - width * height))
                ).orElse(BrickType.UNIDENTIFIABLE);
    }
}

