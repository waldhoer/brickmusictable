package org.brickmusic.visuals;

import org.brickmusic.bricklogic.BrickMap;
import org.brickmusic.playmanagement.BlockingMode;
import org.brickmusic.playmanagement.Player;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brickmusic.Main.SETTINGS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

/**
 * A VisualManager is responsible for managing the input, processing and updating of image frames.
 */
public class VisualManager extends TimerTask {
    protected static final Logger LOGGER = Logger.getLogger(VisualManager.class.getName());

    /**
     * Player getting updated from this visualManager
     */
    private final Player player;

    /**
     * Image Recognizer attached for image analysis
     */
    private final ImageRecognizer recognizer;

    /**
     * Delay between frames for image recognition and capturing
     */
    private final int frameDelay;

    /**
     * Video capture for webcam collection
     */
    private VideoCapture capture;

    private static final int CAMERA_WIDTH = 1920;
    private static final int CAMERA_HEIGHT = 1080;

    private final int cameraIndex;
    private final int captureMode;
    private static final int MAXIMAL_FRAME_ERROR_THRESHOLD = 5;
    private int frameErrorCounter = 0;

    /**
     * @param cameraIndex Index of the used camera
     * @param captureMode Mode for video Capture, default 0
     * @param player      Manager to be updated after analysis
     */
    public VisualManager(int cameraIndex, int captureMode, @NotNull Player player) {
        this.player = player;
        this.recognizer = new ImageRecognizer();
        this.frameDelay = SETTINGS.getInt("FRAME_DELAY");
        this.cameraIndex = cameraIndex;
        this.captureMode = captureMode;

        LOGGER.info("VideoCapture Initialization at camera index " + cameraIndex);
        openCamera();
        LOGGER.info("VideoCapture Initialization complete");

        if (!capture.isOpened())
            throw new CameraAccessException("Camera (specified as " + cameraIndex + ") cannot be accessed by VisualManager");
    }

    /**
     * Runs this VisualManager with provided configuration
     */
    @Override
    public void run() {
        if (!capture.isOpened()) {
            throw new CameraAccessException("Camera capture (specified as " + cameraIndex + ") closed before run() execution");
        } else if (capture.isOpened() && !player.isBlocked()) {
            Mat frame = new Mat();
            try {
                // Check for recurring image failure and reopen camera if required
                if (frameErrorCounter > MAXIMAL_FRAME_ERROR_THRESHOLD) {
                    LOGGER.warning("Recurring image grabbing failure: Resetting video capture after " + MAXIMAL_FRAME_ERROR_THRESHOLD + " attempts failed");
                    capture.release();
                    openCamera();
                }
                capture.read(frame);
                BrickMap generatedBrickMap = recognizer.analyzeFrame(frame);
                player.updateMap(generatedBrickMap);
                Thread.sleep(frameDelay);
                frameErrorCounter = 0;
            } catch (ImageGridException imageError) {
                if (SETTINGS.getBoolean("DEBUG_LEVEL_FINE")) LOGGER.info(imageError.getMessage());
                player.block(BlockingMode.GPDF);
                frameErrorCounter++;
            } catch (InvalidImageException imageException) {
                LOGGER.warning(imageException.getMessage());
                player.block(BlockingMode.GPDF);
                frameErrorCounter++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "An exception occurred while processing images", e);
            }
        }
    }

    /**
     * Validates if the attached VideoCapture can grab a correct image
     *
     * @return true if a grabbed sample frame is formatted correctly, false otherwise
     */
    public boolean validateCameraInput() {
        Mat frame = new Mat();
        capture.read(frame);
        return !frame.empty() && frame.channels() == 3 && frame.height() > 0 && frame.height() > 0;
    }

    /**
     * Sets a new Video capture and opens the camera with given cameraIndex and captureMode
     */
    private void openCamera() {
        capture = new VideoCapture();
        capture.open(cameraIndex, captureMode);
        capture.set(CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
        capture.set(CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);
    }

    /**
     * Terminates the visual manager
     */
    public void terminate() {
        capture.release();
        LOGGER.info("Terminated WebcamManager");
    }
}
