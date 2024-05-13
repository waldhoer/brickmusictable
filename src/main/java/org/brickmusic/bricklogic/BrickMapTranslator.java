package org.brickmusic.bricklogic;

import org.brickmusic.visuals.GridColor;
import org.brickmusic.visuals.ImageProcessing;
import org.brickmusic.visuals.SimpleVisual;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.abs;
import static org.brickmusic.Main.SETTINGS;

/**
 * The Brick Map translator translates visual objects to a brick map considering already known frames
 *
 * @see SimpleVisual
 * @see BrickMap
 */
public class BrickMapTranslator {

    private static final Logger LOGGER = Logger.getLogger(BrickMapTranslator.class.getName());

    /**
     * The ground plate required for grid operations
     */
    private final Size groundPlate;

    /**
     * Points that are allocated with the pin positions ("nobs on the plate")
     */
    private final ArrayList<Point> gridPoints;

    /**
     * Points that are allocated with the spaces between plate pins
     */
    private final ArrayList<Point> spacePoints;

    /**
     * Unscaled mapping of bricks and their position
     */
    private final BrickMap map;

    /**
     * Initializes a new Translator and generates the image grid
     *
     * @param groundPlate The ground plate to consider
     */
    public BrickMapTranslator(Size groundPlate) {
        this.groundPlate = groundPlate;
        this.map = new BrickMap();
        gridPoints = generateGroundPlateGrid(groundPlate, true);
        spacePoints = generateGroundPlateGrid(groundPlate, false);
    }

    /**
     * Translates a visual object into a temporary brick map type and stores it, obtainable using get();
     *
     * @param object   The visual object to translate
     * @param rawImage The raw image used for color detection
     * @param debugImg A contour frame to draw on for debugging
     * @see BrickMapTranslator#get()
     */
    public void translate(SimpleVisual object, Mat rawImage, @NotNull final Mat debugImg) {
        final int border = 15;
        SimpleVisual visual = new SimpleVisual(object), originalVisual = new SimpleVisual(object);

        BrickType type = BrickType.identify(visual.getRectangle(), rawImage.size(),
                (visual.getCenter().x < border || visual.getCenter().x > debugImg.width() - border ||
                        visual.getCenter().y < border || visual.getCenter().y > debugImg.height() - border));

        // Correct object scaling according to determined identifiedBrick type
        ImageProcessing.rescaleVisualObject(visual,
                type.getWidth() * groundPlate.width / BrickType.GROUND_PLATE.getWidth(),
                type.getHeight() * groundPlate.height / BrickType.GROUND_PLATE.getHeight());

        RotatedRect visualRect = visual.getRectangle();

        // Find nearest neighbour grid point
        Point rescaledCenter = findGridAlignedCenter((type.equals(BrickType.PIN)) ? gridPoints : spacePoints,
                visualRect.center, type.equals(BrickType.SOLID_1x2));

        // Move rescaled visual object to updated center (Ignore small angle errors)
        visualRect = new RotatedRect(rescaledCenter, visualRect.size,
                (type == BrickType.PIN) ? 0 : SimpleVisual.normalizeAngle(visualRect.angle));
        visual = new SimpleVisual(visualRect);

        // Get color and brick from visual
        Scalar color = ImageProcessing.getAverageColor(rawImage, rescaledCenter);
        Brick newBrick = new Brick(type, type.rotatable() ? visualRect.angle : 0, InstrumentColor.findNearest(color,
                type.equals(BrickType.PIN)));

        // Calculate map points
        final double xScalingFactor = BrickType.GROUND_PLATE.getWidth() / groundPlate.width;
        final double yScalingFactor = BrickType.GROUND_PLATE.getHeight() / groundPlate.height;

        // Calculate position including corrections (Pins require an additional correction)
        final int xCorrection = 0;
        final int yCorrection = -2;

        int x = (int) (visual.getPoints()[0].x * xScalingFactor) + xCorrection;
        int y = (int) (visual.getPoints()[0].y * yScalingFactor) + yCorrection + (newBrick.getType().equals(BrickType.PIN) ? 1
                : 0);

        // As on-border-bricks are slightly cropped due to ground plate cropping the positions need to be updated
        if (y > 16) y--;
        if (x > 32) x--;
        if (y < 0) y++;
        if (x < 0) x++;
        if (x <= 5 && x > 0) x++; // TODO: The outer x axis area often has an x-offset of 1, This is a temporary fix

        // Draw debug preview
        if (SETTINGS.getBoolean("DEBUG_MODE_ACTIVE")) {
            if (debugImg.empty() || debugImg.width() == 0 || debugImg.height() == 0) {
                LOGGER.log(Level.WARNING, "Debug preview omitted: The debug image provided is invalid!");
            } else {
                final Scalar oldVisualObjectColor = GridColor.BLUE;
                final Scalar newVisualObjectColor = GridColor.GREEN;

                // Draw grid
                if (SETTINGS.getBoolean("DEBUG_LEVEL_FINE")) {
                    for (Point p : gridPoints)
                        Imgproc.drawMarker(debugImg, p, GridColor.WHITE, Imgproc.MARKER_DIAMOND, 3, 2, Imgproc.LINE_4);
                    for (Point p : spacePoints)
                        Imgproc.drawMarker(debugImg, p, GridColor.GREY, Imgproc.MARKER_DIAMOND, 2, 1, Imgproc.LINE_4);
                }

                // Draw adapted visual object
                Imgproc.putText(debugImg,
                        newBrick.getType().toString() + ":" + x + "," + y + ";" + Math.round(newBrick.getRotation()),
                        visual.getCenter(), 2, 0.6,
                        newVisualObjectColor);
                ImageProcessing.drawContours(debugImg, visual, newVisualObjectColor, 2, true);

                // Draw old visual object
                Imgproc.putText(debugImg,
                        BrickType.identify(originalVisual.getRectangle(), rawImage.size()).toString() +
                                ":" + Math.round(originalVisual.getAngle()),
                        new Point(originalVisual.getCenter().x, originalVisual.getCenter().y + 15), 2, 0.6,
                        oldVisualObjectColor);
                ImageProcessing.drawContours(debugImg, originalVisual, oldVisualObjectColor, 1, true);
            }
        }

        map.addBrick(x, y, newBrick);
    }

    /**
     * Rescales the previously translated mappings to a BrickMap
     *
     * @return The generated BrickMap
     */
    public BrickMap get() {
        map.clean();
        return map;
    }

    /**
     * Creates an image grid for the ground plate.
     *
     * @param groundPlate The ground plate
     * @param dots        Set to true if the pins should be exported, Set False for the spaces between the pins
     * @return An image grid that presents pins or their in-between-spaces
     */
    @NotNull
    private static ArrayList<Point> generateGroundPlateGrid(@NotNull Size groundPlate, boolean dots) {
        ArrayList<Point> gridPoints = new ArrayList<>();

        final int horizontalSplits = (dots) ? 32 : 63;
        final int verticalSplits = (dots) ? 16 : 31;
        final double borderOffset = 12;

        final double yRatioInnerBound = 8 / 126.4;
        final double yStepCorrection = 0.2, xStepCorrection = 0.2;
        final double stepLength = yRatioInnerBound * groundPlate.height / ((dots) ? 1 : 2);

        for (int i = 0; i < horizontalSplits; i++) {
            double x = borderOffset + i * (stepLength + xStepCorrection);
            for (int j = 0; j < verticalSplits; j++) {
                if (dots || i % 2 != 0 || j % 2 != 0) {
                    double y = borderOffset + j * (stepLength + yStepCorrection);
                    gridPoints.add(new Point(x, y));
                }
            }
        }
        return gridPoints;
    }

    /**
     * Finds the nearest Neighbours of a given origin point and returns the mean gridPoint
     *
     * @param spacePoints Grid points of the ground plate (spaces between lego pins)
     * @param origin      Origin point which searches neighbours
     * @param plate       If the brick must be handled as plate candidate, i.e. if its center must lie between two nobs
     * @return Average center of the block according to nearest neighbours
     */
    private static Point findGridAlignedCenter(@NotNull ArrayList<Point> spacePoints, Point origin, boolean plate) {
        Point nearestPoint = null;
        for (Point candidate : spacePoints) {
            if (nearestPoint == null || abs(origin.x - candidate.x) + abs(origin.y - candidate.y) <= abs(origin.x - nearestPoint.x) + abs(origin.y - nearestPoint.y)) {
                if (plate && candidate.y % 2 > 0) {
                    candidate.y -= 1;
                }
                nearestPoint = candidate;
            }
        }
        return nearestPoint;
    }
}
