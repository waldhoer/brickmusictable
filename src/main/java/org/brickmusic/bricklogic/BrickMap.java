package org.brickmusic.bricklogic;

import org.brickmusic.sound.SoundData;
import org.brickmusic.visuals.GridColor;
import org.brickmusic.visuals.SimpleVisual;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

import static org.brickmusic.Main.SETTINGS;
import static org.brickmusic.sound.SoundData.midiHeightToKeyString;

/**
 * Map containing all bricks and their positions relative to the ground plate.
 */
public class BrickMap {
    /**
     * Bricks and their locations on the map
     */
    private Map<java.awt.Point, Brick> bricks;

    /**
     * Y coordinate of the row defining speed
     */
    private final static int SPEED_ROW_INDEX = 13;
    /**
     * Y coordinate of the row defining volume
     */
    private final static int VOLUME_ROW_INDEX = 14;

    /**
     * Y coordinate of the row defining the instrument color picking
     */
    private final static int INSTRUMENT_ROW_INDEX = 15;

    /**
     * Y coordinate of the first row that is handled as metadata row
     */
    private final static int METADATA_REGION_START_INDEX = 13;

    /**
     * Instantiates a new BrickMap
     */
    public BrickMap() {
        this.bricks = new HashMap<>();
    }

    /**
     * Adds a new brick to the brickMap
     *
     * @param x     Horizontal position on the groundPlate
     * @param y     Vertical position on the groundPlate
     * @param brick The brick to add
     */
    public void addBrick(int x, int y, Brick brick) {
        bricks.put(new java.awt.Point(x, y), brick);
    }

    /**
     * Gets a vector of bricks that starts at the given location, metadata bricks are omitted
     *
     * @param x The x location (beat location)
     * @return A map containing all bricks found at the x position and their corresponding y values
     */
    public HashMap<Integer, Brick> getBrick(int x) {
        HashMap<Integer, Brick> bricksFound = new HashMap<>();

        for (Map.Entry<java.awt.Point, Brick> mapping : bricks.entrySet()) {
            java.awt.Point point = mapping.getKey();
            if (point.x == x && !isMetaData(point)) {
                bricksFound.put(point.y, mapping.getValue());
            }
        }
        return bricksFound;
    }

    /**
     * @return True if this BrickMap does not contain any bricks, false otherwise
     */
    public boolean isEmpty() {
        return bricks.isEmpty();
    }

    /**
     * Draws the BrickMap to a Mat object
     *
     * @param factor The factor of rescaling, width and height are multiplied by this value
     * @return The generated Mat representing this BrickMap
     */
    public Mat draw(int factor, int beat) {
        int width = BrickType.GROUND_PLATE.getWidth() * factor, height = BrickType.GROUND_PLATE.getHeight() * factor;
        Mat image = Mat.zeros(new Size(width, height), CvType.CV_8UC3);
        final Point[] lines = new Point[2];

        // Draw Metadata area
        Point[] areaPoints = {new Point(0, 13 * factor), new Point(width, 13 * factor),
                new Point(width, height), new Point(0, height)};
        Imgproc.fillConvexPoly(image, new MatOfPoint(areaPoints), GridColor.METADATA_REGION);

        // Draw Grid
        for (int i = 0; i < BrickType.GROUND_PLATE.getWidth(); i++) {
            lines[0] = new Point(i * factor, height);
            lines[1] = new Point(i * factor, 0);
            Imgproc.drawContours(image, List.of(new MatOfPoint(lines)), -1, (i % 4 != 0) ? GridColor.GREEN : GridColor.BLUE, 2);
        }
        for (int i = 0; i < BrickType.GROUND_PLATE.getHeight(); i++) {
            lines[0] = new Point(width, i * factor);
            lines[1] = new Point(0, i * factor);
            Imgproc.drawContours(image, List.of(new MatOfPoint(lines)), -1, GridColor.GREEN, 2);
        }

        // Draw bricks in a sorted manner (smallest are drawn at last to show pins contained on other bricks)
        // For Map sorting see https://stackoverflow.com/a/23846961/9437524 - Shared under CC BY-SA 4 by Brian Goetz
        bricks.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).forEach(entry -> {
            Brick brick = entry.getValue();
            java.awt.Point point = entry.getKey();

            int brickWidth = brick.getWidth() * factor;
            int brickHeight = brick.getHeight() * factor;
            Point scaledPoint = new Point(point.x * factor, point.y * factor);

            // Calculate up-scaled rectangles for preview
            Point[] points = new Point[4];
            RotatedRect rect = new RotatedRect(
                    new Point(scaledPoint.x + (double) brickWidth / 2, scaledPoint.y + (double) brickHeight / 2),
                    new Size(brickWidth, brickHeight),
                    brick.getRotation());
            rect.points(points);

            // Draw brick rectangles (metadata pins colors are overwritten)
            Scalar color = (isMetaData(point)) ? GridColor.METADATA_PINS : brick.getColor().getScalar();
            Imgproc.fillConvexPoly(image, new MatOfPoint(points), color);

            // Draw color extensions as circles
            for (int i = 0; i < brick.getExtensions().size(); i++) {
                Scalar extensionColor = brick.getExtensions().get(i).getScalar();
                Imgproc.circle(image,
                        new Point(scaledPoint.x + 100 + i * 40, scaledPoint.y + (double) brickHeight / 2),
                        40, extensionColor, Imgproc.FILLED);
            }
        });

        // Draw note height and metadata text descriptions
        for (int i = 0; i < BrickType.GROUND_PLATE.getHeight(); i++) {
            if (i < METADATA_REGION_START_INDEX) {
                Imgproc.putText(image,
                        midiHeightToKeyString(SoundData.BASE_KEY_SHIFT + getMetaData().pitch() + (Math.abs(BrickType.GROUND_PLATE.getHeight()) - i)),
                        new Point(0, i * factor + 70), Imgproc.FONT_HERSHEY_SIMPLEX, 2, GridColor.RED, 4);
            } else {
                Imgproc.putText(image, MetaData.mappings()[i % METADATA_REGION_START_INDEX], new Point(0, i * factor + 70),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 2, GridColor.RED, 4);
            }
        }

        // Draw beat indicator
        lines[0] = new Point((beat - beat % 4) * factor, image.height());
        lines[1] = new Point((beat - beat % 4) * factor, 0);
        Imgproc.putText(image, String.valueOf((beat - beat % 4) / 4 + 1), new Point(lines[0].x + 5, lines[0].y - 15),
                Imgproc.FONT_HERSHEY_SIMPLEX, 5, GridColor.RED, 4);
        Imgproc.drawContours(image, List.of(new MatOfPoint(lines)), -1, GridColor.RED, 10);

        return image;
    }

    /**
     * Calculates the average brick map of a list of given brick maps.
     *
     * @param maps The maps to calculate the average of
     * @return A map containing bricks if the given bricks matched with the average threshold of brick mappings within the map.
     */
    @NotNull
    public static BrickMap getAverageBrickMap(ArrayList<BrickMap> maps) {
        BrickMap resultMap = new BrickMap();

        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 16; y++) {
                int foundBrickCounter = 0;
                Brick foundBrick = null;
                HashMap<InstrumentColor, Integer> foundExtensions = new HashMap<>();
                for (BrickMap map : maps) {
                    if (map.bricks.get(new java.awt.Point(x, y)) != null) {
                        foundBrickCounter++;
                        foundBrick = new Brick(map.bricks.get(new java.awt.Point(x, y)));

                        // Color extension average
                        for (InstrumentColor extension : foundBrick.getExtensions()) {
                            foundExtensions.put(extension, foundExtensions.getOrDefault(extension, 0) + 1);
                        }
                    }
                }

                // Add average brick if sufficient
                if (foundBrickCounter >= maps.size() / 2) {
                    for (Map.Entry<InstrumentColor, Integer> entry : foundExtensions.entrySet()) {
                        if (entry.getValue() >= maps.size() / 3) {
                            foundBrick.addExtension(entry.getKey());
                        }
                    }
                    resultMap.addBrick(x, y, foundBrick);
                }
            }
        }
        resultMap.clean();
        return resultMap;
    }

    /**
     * Cleans the brick map from invalid brick positions and manages brick extension placements.
     * Bricks which overlap and meet specific criteria are removed from the map, the others remain unchanged.<br><br>
     * Pins are either removed as individuals and added as extensions to a brick or added as individuals if they are not
     * contained in another brick.
     *
     * @see BrickMap#overlap(java.awt.Point, Brick, java.awt.Point, Brick)
     */
    public void clean() {
        HashMap<java.awt.Point, Brick> cleanedMap = new HashMap<>();
        for (var firstMapping : bricks.entrySet()) {
            boolean overlaps = false;
            Brick brick1 = firstMapping.getValue();
            java.awt.Point point1 = firstMapping.getKey();

            // For small bricks, check if the small brick is contained in another brick
            if (firstMapping.getValue().isSmall()) {
                boolean contained = false;
                for (var secondMapping : bricks.entrySet()) {
                    Brick brick2 = secondMapping.getValue();
                    java.awt.Point point2 = secondMapping.getKey();

                    if (!point1.equals(point2) && !brick2.isSmall() && overlap(point1, brick1, point2, brick2)) {
                        brick2.addExtension(brick1.getColor());
                        cleanedMap.put(point2, brick2);
                        contained = true;
                        break;
                    }
                }

                // Only add small bricks that are not contained in others, for color extension contained in other bricks
                // see the following code block overlapping region.
                if (!contained) cleanedMap.put(point1, brick1);
            }

            // Default brick cleaning
            else {
                for (var secondMapping : bricks.entrySet()) {
                    Brick brick2 = secondMapping.getValue();
                    java.awt.Point point2 = secondMapping.getKey();

                    // Overlapping brick cases
                    if (overlap(point1, brick1, point2, brick2)) {
                        // Discard pin bricks and add them as extension to the bigger brick
                        if (brick2.isSmall()) {
                            brick1.addExtension(brick2.getColor());
                            cleanedMap.put(point1, brick1);
                        }

                        // If two overlapping bricks have been found, the first which is found rotated will be removed
                        // If no of the overlapping bricks is rotated, the smaller one is removed
                        else if (brick2.getRotation() != 0) {
                            cleanedMap.put(point1, brick1);
                        } else if (brick1.getRotation() != 0) {
                            cleanedMap.put(point2, brick2);
                        } else {
                            if (brick1.compareTo(brick2) <= 0) cleanedMap.put(point2, brick2);
                            else cleanedMap.put(point1, brick1);
                        }
                        overlaps = true;
                    }
                }

                // If no overlapping is present, add normally
                if (!overlaps) cleanedMap.put(point1, brick1);
            }
        }

        bricks = cleanedMap;
    }

    /**
     * Checks if two brick mappings overlap
     *
     * @param point1 The position on the map of the first brick
     * @param brick1 The first brick
     * @param point2 The position on the map of the second brick
     * @param brick2 The second brick
     * @return True if at least one point overlaps, False otherwise
     */
    public boolean overlap(@NotNull java.awt.Point point1, @NotNull Brick brick1, @NotNull java.awt.Point point2,
                           @NotNull Brick brick2) {
        // These visuals shall only be used for overlap checking
        final SimpleVisual v1 = new SimpleVisual(new RotatedRect(
                new Point(point1.x + (double) brick1.getWidth() / 2, point1.y + (double) brick1.getHeight() / 2),
                new Size(brick1.getWidth(), brick1.getHeight()), brick1.getRotation()));
        final SimpleVisual v2 = new SimpleVisual(new RotatedRect(
                new Point(point2.x + (double) brick2.getWidth() / 2, point2.y + (double) brick2.getHeight() / 2),
                new Size(brick2.getWidth(), brick2.getHeight()), brick2.getRotation()));

        for (Point corner : v1.getPoints()) {
            if (Imgproc.pointPolygonTest(new MatOfPoint2f(v2.getPoints()), corner, false) > 0) return true;
        }
        return false;
    }

    /**
     * Reads the last three lines of the ground plate (metadata region) and returns the collected metadata.
     *
     * @return The metadata contained in the map
     */
    public MetaData getMetaData() {
        int bpmStepIncrease = 4;

        int pitch = 0;
        int bpm = SETTINGS.getInt("BPM");
        double volume = 0.5;

        // Read speed and volume pins
        for (int i = 0; i < 32; i++) {
            if (bricks.get(new java.awt.Point(i, SPEED_ROW_INDEX)) != null) {
                bpm += i * bpmStepIncrease;
            }
            if (bricks.get(new java.awt.Point(i, VOLUME_ROW_INDEX)) != null) {
                volume = i * (SETTINGS.getDouble("VOLUME") / 32);
            }
            if (bricks.get(new java.awt.Point(i, INSTRUMENT_ROW_INDEX)) != null) {
                pitch = i;
            }
        }

        return new MetaData(bpm, volume, pitch);
    }

    /**
     * Checks if a given point on map is in the region of metadata
     *
     * @param p The point to check
     * @return True if the point lies within the lower metadata region, False otherwise
     */
    public static boolean isMetaData(@NotNull java.awt.Point p) {
        return p.y >= METADATA_REGION_START_INDEX;
    }
}
