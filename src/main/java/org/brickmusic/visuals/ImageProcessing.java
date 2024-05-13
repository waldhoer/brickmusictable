package org.brickmusic.visuals;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.Vector;

import static org.brickmusic.Main.SETTINGS;

/**
 * Provides helper methods for image processing that do not contain any logical components.
 */
public final class ImageProcessing {

    private final static double WIDTH_FACTOR = SETTINGS.getDouble("CROP_AREA_HORIZONTAL_SCALE") /
            SETTINGS.getDouble("CROP_AREA_ZOOM");
    private final static double HEIGHT_FACTOR = SETTINGS.getDouble("CROP_AREA_VERTICAL_SCALE") /
            SETTINGS.getDouble("CROP_AREA_ZOOM");

    /**
     * Crops the image to a fixed position.
     *
     * @param image The image containing the LEGO plate
     */
    public static void cropImage(@NotNull Mat image) {
        final int width = (int) (image.width() * WIDTH_FACTOR);
        final int height = (int) (image.height() * HEIGHT_FACTOR);

        Rect cropArea = new Rect((image.width() - width) / 2 + SETTINGS.getInt("CROP_AREA_HORIZONTAL_SHIFT"),
                (image.height() - height) / 2 + SETTINGS.getInt("CROP_AREA_VERTICAL_SHIFT"),
                width, height);
        new Mat(image, cropArea).copyTo(image);
    }

    /**
     * Calculates the average color at a point within a given range of points
     *
     * @param image  The image to read from
     * @param origin The center point of color calculation
     * @return The mean color value within the range around the given point
     */
    @NotNull
    public static Scalar getAverageColor(Mat image, Point origin) {
        final int rangeSize = 10;
        Vector<Point> points = new Vector<>();

        for (int i = -rangeSize; i < rangeSize; i++) {
            for (int j = -rangeSize; j < rangeSize; j++) {
                if (origin.x + i > 0 && origin.y + j > 0 && origin.x + i < image.width() && origin.y + j < image.height())
                    points.add(new Point(origin.x + i, origin.y + j));
            }
        }

        Scalar color = new Scalar(0, 0, 0);
        for (Point point : points) {
            for (int j = 0; j < 3; j++) {
                double[] tColor = image.get((int) point.y, (int) point.x);
                if (tColor != null && tColor.length > j)
                    color.val[j] += tColor[j] / points.size();
            }
        }
        return color;
    }

    /**
     * Helper function for drawing visual object contours
     *
     * @param image        The image to draw to
     * @param simpleVisual The object to draw
     * @param color        Contour color
     * @param thickness    Contour thickness
     * @see Imgproc#drawContours(Mat, List, int, Scalar)
     */
    public static void drawContours(Mat image, SimpleVisual simpleVisual, Scalar color, int thickness, boolean drawCenter) {
        Imgproc.drawContours(image, List.of(new MatOfPoint(simpleVisual.getPoints())), -1, color, thickness);
        if (drawCenter)
            Imgproc.drawMarker(image, simpleVisual.getCenter(), color, Imgproc.MARKER_STAR, 6, thickness,
                    Imgproc.LINE_4);
    }

    /**
     * Rescales a visual object to match a specific size
     *
     * @param simpleVisual The object to scale
     * @param newWidth     The new x size
     * @param newHeight    The new y size
     */
    public static void rescaleVisualObject(@NotNull SimpleVisual simpleVisual, double newWidth, double newHeight) {
        double angle = simpleVisual.getAngle();
        RotatedRect correctedRect = new RotatedRect(simpleVisual.getCenter(), new Size(newWidth, newHeight), angle);
        simpleVisual.setRectangle(correctedRect);
    }
}
