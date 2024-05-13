package org.brickmusic.visuals;

import org.brickmusic.bricklogic.BrickMap;
import org.brickmusic.bricklogic.BrickMapTranslator;
import org.brickmusic.view.Viewer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.brickmusic.Main.SETTINGS;


/**
 * This class or few selected code lines were taken and/or adapted from the given reference source.
 * Reference <a href="https://stackoverflow.com/q/18581633">StackOverflow: Rectangle Detection</a>
 * shared under <a href="https://creativecommons.org/licenses/by-sa/3.0/">CC BY-SA 3.0</a><br><br>
 * Also see <a href="https://docs.opencv.org/4.x/javadoc/org/opencv/imgproc/Imgproc.html">the java opencv docs</a>
 * for further details.
 */
public class ImageRecognizer {
    /**
     * Minimal size for visual objects. Objects smaller than this value will be discarded.
     */
    private static final int MINIMUM_VISUAL_OBJECT_SIZE = 20;

    /**
     * Minimal length of contours. Contours smaller than this value will be discarded.
     */
    private static final int MINIMAL_CONTOUR_LENGTH = 80;

    private static final Logger LOGGER = Logger.getLogger(ImageRecognizer.class.getName());

    private static final Viewer VIEWER_PROCESSED = new Viewer(500, "Processed");
    private static final Viewer VIEWER_CONTOURS = new Viewer(500, "Contours");

    /**
     * Brick map buffer for average calculation, stores the last calculated brick maps.
     */
    private final ArrayList<BrickMap> brickMapBuffer;

    /**
     * Initializes a new ImageRecognizer with default settings
     */
    protected ImageRecognizer() {
        this.brickMapBuffer = new ArrayList<>(SETTINGS.getInt("FRAME_BUFFER_SIZE"));
    }

    /**
     * Analyses an image for bricks. This method handles reset requests for image processing.
     *
     * @param image The image to analyse
     * @return A brick map that is either an average image (meaning it sums up all brick maps in the brick map buffer), or the
     * detected brick map itself if not configured or the frame buffer is not full yet.
     * @throws ImageGridException    If no image grid was found
     * @throws InvalidImageException If the provided image is invalid
     */
    public BrickMap analyzeFrame(Mat image) throws ImageGridException, InvalidImageException {
        if (image == null || image.empty()) {
            throw new InvalidImageException(InvalidImageException.EMPTY_FRAME);
        } else if (image.channels() != 3) {
            throw new InvalidImageException(InvalidImageException.CORRUPTED_FRAME);
        }

        final Mat rawImage = prepareImage(image);
        final Mat contourFrame = rawImage.clone();

        final ArrayList<MatOfPoint> contours = getContours(image);
        final ArrayList<SimpleVisual> simpleVisualCandidates = getVisualObjects(contours);

        if (simpleVisualCandidates.size() <= 1) {
            throw new ImageGridException(ImageGridException.NO_GROUND_PLATE);
        }

        if (SETTINGS.getBoolean("DEBUG_MODE_ACTIVE")) {
            Imgproc.drawContours(contourFrame, contours, -1, GridColor.RED, 2);
        }

        ArrayList<SimpleVisual> simpleVisuals = removeInvalidVisualObjects(simpleVisualCandidates, image.size());

        final BrickMapTranslator translator = new BrickMapTranslator(image.size());

        // Below operations regarding drawing and brick calculation are performed
        for (SimpleVisual simpleVisual : simpleVisuals) {
            translator.translate(simpleVisual, rawImage, contourFrame);

            if (SETTINGS.getBoolean("DEBUG_MODE_ACTIVE")) {
                VIEWER_CONTOURS.displayMat(contourFrame);
            }
        }

        // The brick map returned will probably not be equal to the average brick map
        // Unless the brick map buffer is filled up, the original non-average map will be used to increase stability
        if (brickMapBuffer.size() >= SETTINGS.getInt("FRAME_BUFFER_SIZE")) brickMapBuffer.remove(0);
        final BrickMap capturedMap = translator.get();
        brickMapBuffer.add(capturedMap);

        // If the returned map shall be used from average is depending on configuration
        if (SETTINGS.getBoolean("USE_HISTORY_AVERAGE")) {
            return (brickMapBuffer.size() < SETTINGS.getInt("FRAME_BUFFER_SIZE")) ? capturedMap :
                    BrickMap.getAverageBrickMap(brickMapBuffer);
        } else {
            return capturedMap;
        }
    }

    /**
     * Removes invalid visual objects from the list. This includes validation of size and position.
     *
     * @param toCheck The list to validate
     * @param image   The binary image required for evaluation
     * @return The list without invalid elements
     */
    @NotNull
    private static ArrayList<SimpleVisual> removeInvalidVisualObjects(@NotNull ArrayList<SimpleVisual> toCheck, Size image) {
        final int border = 10;
        return toCheck.stream().filter(c ->
                {
                    final RotatedRect candidateRect = c.getRectangle();
                    return candidateRect.size.width >= MINIMUM_VISUAL_OBJECT_SIZE && // minimum size
                            candidateRect.size.height >= MINIMUM_VISUAL_OBJECT_SIZE &&
                            candidateRect.center.x > border && candidateRect.center.y > border && // out of border center
                            candidateRect.center.x < image.width - border &&
                            candidateRect.center.y < image.height - border &&
                            candidateRect.size.width < image.width - border && // ground plate candidate
                            candidateRect.size.height < image.height - border;
                }
        ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Rotates and crops the image to match the ground plate section and recolors the background plate. The input image
     * will be converted to a binary image.
     * If set, assure that the boundary paper is bigger than the ground plate, but does not interfere with image borders
     *
     * @param image The image to prepare and adapt
     * @return The rotated and cropped original image without binary conversion or special editing
     * @throws ImageGridException If no grid was found
     */
    @NotNull
    @Contract("_ -> new")
    private Mat prepareImage(final @NotNull Mat image) throws ImageGridException {
        try {
            final Mat copy = new Mat();

            ImageProcessing.cropImage(image);
            image.copyTo(copy);

            // Contrast increase and blurring is required to remove unoccupied ground plate pins from being detected.
            image.convertTo(image, CvType.CV_8UC3, 1.2, 12);
            Imgproc.medianBlur(image, image, 17);

            // CANNY is performed within two different ranges, one for detecting the general rectangle and circle
            // contours and one for specifically detailed circle detection. Both are merged afterward.
            Mat generalRange = new Mat(), circleRange = new Mat();
            Imgproc.Canny(image, generalRange, 90, 70, 3, true);
            Imgproc.Canny(image, circleRange, 100, 260, 3, true);

            Core.bitwise_or(circleRange, generalRange, generalRange);

            Imgproc.morphologyEx(generalRange, generalRange, Imgproc.MORPH_DILATE,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                            new Size(3, 3)));

            generalRange.copyTo(image);
            VIEWER_PROCESSED.displayMat(generalRange);

            return copy;
        } catch (ArrayIndexOutOfBoundsException | CvException exception) {
            if (SETTINGS.getBoolean("DEBUG_LEVEL_FINE")) {
                LOGGER.log(Level.INFO, "Image preparation: No ground plate candidate found", exception);
            }
            throw new ImageGridException(ImageGridException.NO_GROUND_PLATE);
        }
    }

    /**
     * Reads an image and finds the contained contours
     *
     * @param image The image to analyse
     * @return Analysed contours
     */
    @NotNull
    private ArrayList<MatOfPoint> getContours(@NotNull Mat image) {
        final ArrayList<MatOfPoint> contours = new ArrayList<>();
        final ArrayList<MatOfPoint> approximatedContours = new ArrayList<>();

        // Add boundary contour to allow on-edge bricks to be identified
        final int offset = 2; // offset from image boundary
        final Point[] points = {new Point(offset, offset), new Point(image.width() - offset, offset),
                new Point(image.width() - offset, image.height() - offset), new Point(offset,
                image.height() - offset)};
        Imgproc.drawContours(image, List.of(new MatOfPoint(points)), -1, GridColor.WHITE, 1);
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Approximate contours to straighten lines
        for (MatOfPoint contour : contours) {
            MatOfPoint2f newContour = new MatOfPoint2f(contour.toArray());
            if (Imgproc.arcLength(newContour, false) < MINIMAL_CONTOUR_LENGTH) continue;
            if (Imgproc.arcLength(newContour, true) > (double) image.width() / 3) {
                Imgproc.approxPolyDP(newContour, newContour, 5, true);
                approximatedContours.add(new MatOfPoint(newContour.toArray()));
            } else {
                approximatedContours.add(contour);
            }
        }

        return approximatedContours;
    }

    /**
     * Converts MatOfPoint-Contours into visual objects.
     * Unnecessary objects like image-border contours are removed in this step.
     *
     * @param contours The contours to analyse
     * @return A pair of gathered visual objects and the original contours
     */
    @NotNull
    private ArrayList<SimpleVisual> getVisualObjects(@NotNull ArrayList<MatOfPoint> contours) throws ImageGridException {
        final ArrayList<SimpleVisual> simpleVisuals = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            final SimpleVisual visual = new SimpleVisual(Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray())));
            visual.normalize();
            simpleVisuals.add(visual);
        }

        if (simpleVisuals.isEmpty()) {
            throw new ImageGridException(ImageGridException.NO_VISUALS);
        }

        // Remove outer image boundary
        simpleVisuals.sort(SimpleVisual::compareTo);
        simpleVisuals.remove(0);

        return simpleVisuals;
    }
}
