package org.brickmusic.visuals;

import org.brickmusic.bricklogic.BrickMap;
import org.brickmusic.bricklogic.BrickType;
import org.brickmusic.bricklogic.InstrumentColor;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class ImageRecognizerTest {

    private static ImageRecognizer recognizer;

    /**
     * Sets if some identification tests are strict, meaning no visuals are allowed, or not.
     */
    private static final boolean strict = false;

    @NotNull
    @Contract(" -> new")
    public static ImageRecognizer getRecognizer() {
        return new ImageRecognizer();
    }

    @BeforeAll
    public static void setUp() {
        Loader.load(opencv_java.class);
        recognizer = new ImageRecognizer();
    }

    /**
     * Learns images in iterations (for average imitation)
     *
     * @param fileName The image name to learn without extension and number (e.g. "default" or "pinsonly")
     * @return the resulting map
     */
    private BrickMap analyzeImage(String fileName) throws ImageGridException, InvalidImageException {
        BrickMap map = null;
        for (int i = 0; i < 16; i++) {
            map = recognizer.analyzeFrame(imread("src/test/resources/" + fileName + " (1).jpg"));
            recognizer.analyzeFrame(imread("src/test/resources/" + fileName + " (2).jpg"));
            recognizer.analyzeFrame(imread("src/test/resources/" + fileName + " (3).jpg"));
        }
        return map;
    }

    /**
     * Checks if invalid images lead to expected exceptions
     */
    @Test
    public void checkInvalidImageHandling() {
        Mat emptyMat = new Mat();
        Mat blackMat = Mat.zeros(1920, 1080, CvType.CV_8UC3);
        assertThrows(ImageGridException.class, () -> recognizer.analyzeFrame(blackMat));
        assertThrows(InvalidImageException.class, () -> recognizer.analyzeFrame(emptyMat));
        assertThrows(InvalidImageException.class, () -> recognizer.analyzeFrame(null));
    }

    /**
     * Checks the correct brick amount of an image
     */
    @Test
    public void checkBrickAmount() {
        try {
            BrickMap map = analyzeImage("default");
            int bricks = 0;
            for (int i = 0; i < 32; i++) {
                bricks += map.getBrick(i).size();
            }
            assertEquals(5, bricks);
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Checks the correct amount of different colors in the image
     */
    @Test
    public void checkColorAmount() {
        try {
            BrickMap map = analyzeImage("default");
            ArrayList<InstrumentColor> colors = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                map.getBrick(i).forEach((y, b) -> {
                    if (!colors.contains(b.getColor())) colors.add(b.getColor());
                });
            }
            assertEquals(3, colors.size());
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Checks the correct amount of different sizes in the image
     */
    @Test
    public void checkCorrectSizes() {
        try {
            ArrayList<BrickType> sizes = new ArrayList<>();
            for (int i = 0; i < 4; i++) sizes.add(BrickType.SOLID_2x2);
            sizes.add(BrickType.SOLID_4x2);

            BrickMap map = analyzeImage("default");
            ArrayList<BrickType> analyzedSizes = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                map.getBrick(i).forEach((y, b) -> analyzedSizes.add(b.getType()));
            }
            Collections.sort(analyzedSizes);
            Collections.sort(sizes);
            assertEquals(analyzedSizes, sizes);
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Checks the correct amount of different extension in the image
     */
    @Test
    public void checkCorrectExtensions() {
        try {
            ArrayList<InstrumentColor> colors = new ArrayList<>();
            colors.add(InstrumentColor.GREEN);
            if (strict) colors.add(InstrumentColor.RED);
            colors.add(InstrumentColor.BLACK);

            BrickMap map = analyzeImage("extensions");
            ArrayList<InstrumentColor> analyzedColors = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                map.getBrick(i).forEach((y, b) -> b.getExtensions().forEach((extension) -> {
                    if (!analyzedColors.contains(extension)) analyzedColors.add(extension);
                }));
            }
            Collections.sort(analyzedColors);
            Collections.sort(colors);
            assertEquals(colors, analyzedColors);
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Checks the correct rotation of a brick in the image
     */
    @Test
    public void checkCorrectRotation() {
        try {
            double rotation = 35;
            BrickMap map = analyzeImage("rotated");

            boolean done = false;
            double actual = 0;
            for (int i = 0; i < 32; i++) {
                for (var mapping : map.getBrick(i).entrySet()) {
                    actual = mapping.getValue().getRotation();
                    done = true;
                    break;
                }
                if (done) break;
            }

            assertEquals(rotation, actual, 5);
        } catch (Exception e) {
            fail(e);
        }
    }
}