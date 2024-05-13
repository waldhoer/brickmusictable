package org.brickmusic.visuals;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

import java.util.Arrays;
import java.util.Objects;

import static org.brickmusic.Main.SETTINGS;

/**
 * A visual object only containing information regarding its shape.
 * This class encapsulates the RotatedRect class for simpler usage.
 * For logic orientated presentation use Bricks instead.
 *
 * @see org.brickmusic.bricklogic.Brick
 * @see org.opencv.core.RotatedRect
 */
public class SimpleVisual implements Comparable<Object> {

    /**
     * The rectangle body attached to the Visual object
     */
    private RotatedRect rectangle;

    /**
     * Corner points of the visual object
     */
    private final Point[] points;

    /**
     * Creates a new visual object and initializes the points
     *
     * @param rectangle The rectangle to attach
     */
    public SimpleVisual(RotatedRect rectangle) {
        this.points = new Point[4];
        this.rectangle = rectangle;
        this.rectangle.points(this.points);
        normalizeAngle();
    }

    /**
     * Copy constructor
     *
     * @param copyFrom The object to copy
     */
    public SimpleVisual(@NotNull SimpleVisual copyFrom) {
        this.rectangle = copyFrom.getRectangle().clone();
        this.points = copyFrom.getPoints().clone();
        normalizeAngle();
    }

    /**
     * @return the attached rectangle
     */
    public RotatedRect getRectangle() {
        return rectangle;
    }

    /**
     * @return corner points of the attached rectangle
     */
    public Point[] getPoints() {
        return points;
    }

    /**
     * Sets a new rotated rectangle as the objects new rectangle
     *
     * @param rectangle The rectangle to be used
     */
    public void setRectangle(@NotNull RotatedRect rectangle) {
        this.rectangle = rectangle;
        rectangle.points(points);
        normalizeAngle();
    }

    /**
     * Helper function for getting angle
     *
     * @return The angle of this visual object
     */
    public double getAngle() {
        return rectangle.angle;
    }

    /**
     * Helper function for getting center
     *
     * @return The center of this visual object
     */
    public Point getCenter() {
        return rectangle.center;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (equals(o)) return 0;
        if (getClass() != o.getClass()) return -1;
        SimpleVisual that = (SimpleVisual) o;
        if (that.rectangle.size.width * that.rectangle.size.height == rectangle.size.width * rectangle.size.height) return 0;
        return (that.rectangle.size.width * that.rectangle.size.height < rectangle.size.width * rectangle.size.height) ? -1 : 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleVisual that = (SimpleVisual) o;
        return Objects.equals(rectangle, that.rectangle) && Arrays.equals(points,
                that.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rectangle);
    }

    @Override
    public String toString() {
        return "SimpleVisual{" +
                "rectangle=" + rectangle +
                ", points=" + Arrays.toString(points) +
                '}';
    }

    /**
     * Performs angle normalisation for a given angle. This includes angle adaption for specific angles and
     * resetting to predefined angles (e.g. 0, 90) if the angle lies within a given threshold.
     *
     * @param angle The angle to normalize
     * @return A normalized angle, which can be either 90/0 or a (possibly) adapted angle for correct rotation presentation.
     * If 90 degree rotations are disabled, always values around 0 will be returned.
     */
    public static double normalizeAngle(double angle) {
        final int threshold = SETTINGS.getInt("MAX_ROTATION_DIFF");
        if (((90 - threshold < angle && angle <= 90) || (180 <= angle && angle < 180 + threshold))) {
            if (SETTINGS.getBoolean("90_DEGREE_ROTATION_ALLOWED")) {
                return 90;
            } else {
                return 0;
            }
        }
        if ((angle < 90 && angle <= threshold) || (angle > 90 && 270 - angle <= threshold)) {
            return 0;
        } else {
            return (angle > 180) ? angle + 90 : angle;
        }
    }

    /**
     * Calculates a new angle for this simple visual. By default, angles are only mapped within
     * a 0 to 90 degree range. This method is used to expand this range according to the length
     * of the rectangle side lengths.
     */
    private void normalizeAngle() {
        if (rectangle.size.width < rectangle.size.height) {
            rectangle.angle += 90;
        }
    }
    
    /**
     * Normalizes the angle of this visual that lies within a given error tolerance threshold
     */
    public void normalize() {
        rectangle.angle = normalizeAngle(rectangle.angle);
    }

}
