package org.brickmusic.playmanagement;

import org.brickmusic.bricklogic.BrickMap;
import org.brickmusic.visuals.ImageGridException;
import org.brickmusic.visuals.ImageRecognizer;
import org.brickmusic.visuals.ImageRecognizerTest;
import org.brickmusic.visuals.InvalidImageException;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opencv.imgcodecs.Imgcodecs.imread;

class PlayerTest {

    @BeforeAll
    public static void setUp() {
        Loader.load(opencv_java.class);
    }

    /**
     * Checks if the speed/volume pins change the metadata correctly.
     */
    @Test
    void checkMetaDataValidity() throws IOException, InterruptedException, ImageGridException, InvalidImageException {
        PlayerManager manager = new PlayerManager();
        Player player = new Player(manager);
        ImageRecognizer recognizer = ImageRecognizerTest.getRecognizer();

        int originalBpm = player.getMetaData().bpm();
        int actualBpm = 100;

        double originalVol = player.getMetaData().volume();
        double actualVol = 100;
        for (int i = 0; i < 16; i++) {
            player.run();
            BrickMap map = recognizer.analyzeFrame(imread("src/test/resources/metadata (1).jpg"));
            recognizer.analyzeFrame(imread("src/test/resources/metadata (2).jpg"));
            recognizer.analyzeFrame(imread("src/test/resources/metadata (3).jpg"));
            player.updateMap(map);
            actualBpm = player.getMetaData().bpm();
            actualVol = player.getMetaData().volume();
            Thread.sleep(100);
        }
        manager.stop();
        System.out.println("Original: " + originalBpm + ", Actual: " + actualBpm);
        assertNotEquals(originalBpm, actualBpm);

        System.out.println("Original: " + originalVol + ", Actual: " + actualVol);
        assertNotEquals(originalBpm, actualVol);
    }
}