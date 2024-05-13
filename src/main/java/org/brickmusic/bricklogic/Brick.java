package org.brickmusic.bricklogic;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A brick is a visual-independent digital presentation of a brick.
 * It does not contain information on position, only on size, shape, color and rotation.
 * For Mappings see the BrickMapTranslator and BrickMap
 *
 * @see BrickMapTranslator
 * @see BrickMap
 * @see BrickType
 */
public class Brick implements Comparable<Brick> {
    private static final Logger LOGGER = Logger.getLogger(Brick.class.getName());

    /**
     * Type of this brick, specifying width and height
     */
    private final BrickType type;

    /**
     * Rotation of this brick in degree
     */
    private final double rotation;

    /**
     * Color of this brick
     */
    private InstrumentColor color;

    /**
     * Brick extensions placed on this brick. Extensions pins provide colors which can affect sound properties.
     */
    private final ArrayList<InstrumentColor> extensions;

    /**
     * Creates a new brick from a given BrickType
     *
     * @param type     The type of brick
     * @param rotation Brick rotation
     * @param color    Color of the brick
     */
    public Brick(BrickType type, double rotation, InstrumentColor color) {
        this.type = type;
        this.rotation = rotation;
        this.color = color;
        extensions = new ArrayList<>();
    }

    /**
     * Copy constructor
     *
     * @param copy The brick to copy from
     */
    public Brick(@NotNull Brick copy) {
        type = copy.getType();
        rotation = copy.getRotation();
        color = copy.getColor();
        extensions = new ArrayList<>(copy.getExtensions());
    }

    /**
     * @return The width of the brick
     */
    public int getWidth() {
        return type.getWidth();
    }

    /**
     * @return The rotation of the brick
     */
    public double getRotation() {
        return rotation;
    }

    /**
     * @return The height of the brick
     */
    public int getHeight() {
        return type.getHeight();
    }

    /**
     * @return The color of this brick
     */
    public InstrumentColor getColor() {
        return color;
    }

    /**
     * @return The type of the brick
     */
    public BrickType getType() {
        return type;
    }

    /**
     * @return True if the brick is a small brick (1x1), False otherwise
     */
    public boolean isSmall() {
        return type.equals(BrickType.PIN);
    }

    /**
     * Adds an extension pin color to the brick
     *
     * @param color The color of the pin attached
     */
    public void addExtension(InstrumentColor color) {
        if (extensions.size() > type.getWidth() * type.getHeight() / 2) {
            LOGGER.warning("Bricks containing more extension pins than half of their pin spaces may cause miscalculation");
        }
        if (color != null && !extensions.contains(color)) extensions.add(color);
    }

    /**
     * @return The color extensions added to this brick
     */
    public ArrayList<InstrumentColor> getExtensions() {
        return extensions;
    }

    @Override
    public String toString() {
        return "Brick{" + "height=" + type.getHeight() + ", width=" + type.getWidth() + ", rotation=" + rotation + ", color=" + color + ", type=" + type + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Brick brick = (Brick) o;
        return Double.compare(rotation, brick.rotation) == 0 && type == brick.type && Objects.equals(color, brick.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rotation, color);
    }

    @Override
    public int compareTo(@NotNull Brick b) {
        if (this.equals(b)) return 0;
        return type.getWidth() < b.type.getWidth() ? -1 : 1;
    }

    /**
     * Updates the color of this brick
     *
     * @param color The new color to set
     */
    public void setColor(@NotNull InstrumentColor color) {
        this.color = color;
    }
}
