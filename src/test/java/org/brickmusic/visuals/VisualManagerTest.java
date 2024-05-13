package org.brickmusic.visuals;

import org.brickmusic.playmanagement.Player;
import org.brickmusic.playmanagement.PlayerManager;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencv.videoio.Videoio.CAP_DSHOW;

class VisualManagerTest {

    @BeforeAll
    public static void setUp() {
        Loader.load(opencv_java.class);
    }

    /**
     * Checks if the camera access fails controlled upon invalid opening
     */
    @Test
    void validateCameraAccessOnCTOR() throws IOException {
        PlayerManager manager = new PlayerManager();
        Player player = new Player(manager);
        assertThrows(CameraAccessException.class, () -> new VisualManager(-1, CAP_DSHOW, player));
        manager.stop();
    }

    /**
     * Checks if the camera access fails controlled upon termination while running
     */
    @Test
    void validateCameraAccessAfterClose() throws IOException {
        PlayerManager playerManager = new PlayerManager();
        Player player = new Player(playerManager);
        VisualManager manager = new VisualManager(0, CAP_DSHOW, player);
        assertThrows(CameraAccessException.class, () -> {
            for (int i = 0; i < 3; i++) {
                manager.run();
                manager.terminate();
            }
        });
        playerManager.stop();
    }
}