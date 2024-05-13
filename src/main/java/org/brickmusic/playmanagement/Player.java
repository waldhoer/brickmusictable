package org.brickmusic.playmanagement;

import org.brickmusic.bricklogic.Brick;
import org.brickmusic.bricklogic.BrickMap;
import org.brickmusic.bricklogic.BrickType;
import org.brickmusic.bricklogic.MetaData;
import org.brickmusic.externals.NxtManager;
import org.brickmusic.sound.Communicator;
import org.brickmusic.sound.SoundData;
import org.brickmusic.view.Viewer;
import org.brickmusic.visuals.VisualManager;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brickmusic.Main.SETTINGS;
import static org.opencv.imgcodecs.Imgcodecs.imread;

/**
 * The player is responsible for continuous playing of the obtained notes.
 * It provides a metronome visualiser for the obtained BrickMap.
 *
 * @see PlayerManager#changeSpeed(int)
 * @see PlayerManager
 * @see MetaData
 */
public class Player extends TimerTask {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    private static final Mat INTERRUPT_MESSAGE = imread("info-interruption.png");
    private static final Mat EMPTY_MESSAGE = imread("info-empty.png");
    private static final Mat GPDF_MESSAGE = imread("info-gpdf.png");

    /**
     * The brickMap to be played by this Player
     */
    private BrickMap map;

    /**
     * Metadata for continuous playing
     */
    private MetaData metaData;

    /**
     * The visualiser showing the brick map and a metronome
     */
    private final Viewer viewer;

    /**
     * The current beat position in the track
     */
    private int beat;

    /**
     * The sound manager handled by this player manager
     */
    private final Communicator communicator;

    /**
     * Defines if the Player Manager is currently running on an old brick map due to mapping issues
     *
     * @see Player#updateMap(BrickMap)
     * @see NxtManager
     */
    private BlockingMode fallbackMode = BlockingMode.FREE;

    /**
     * Timestamp for timeout evaluation. This value shall be set upon receiving a blocking call from the nxt handler.
     * If the timestamp is null blocking calls have already timed out or nothing has been received.
     *
     * @see NxtManager
     */
    private LocalDateTime lastBlockingCall = null;

    /**
     * Parent controlling manager that is responsible for launching and scheduling the player
     */
    private final PlayerManager controller;

    /**
     * Creates a new Player with an empty brick map, initialising all required sub managers
     *
     * @param controller Parent controlling manager to inform upon change speed
     * @see VisualManager
     * @see NxtManager
     * @see Communicator
     */
    public Player(@NotNull PlayerManager controller) throws IOException {
        this.controller = controller;
        metaData = new MetaData(SETTINGS.getInt("BPM"), SETTINGS.getInt("VOLUME"), 0);
        if (metaData.bpm() <= 0) throw new IllegalArgumentException("BPM must be >= 0");
        if (metaData.volume() < 0) throw new IllegalArgumentException("Volume must be > 0");

        this.map = new BrickMap();
        this.viewer = new Viewer(bpmToSpeed(metaData.bpm()), "Digital");
        this.communicator = new Communicator();

        beat = 0;
    }

    /**
     * Updates the players map with a new one.
     * If the new map is empty the old one will be kept and fallback mode will be activated
     *
     * @param newMap The new map to set
     * @see Player#fallbackMode
     */
    public void updateMap(@NotNull BrickMap newMap) {
        if (!newMap.isEmpty()) {
            map = newMap;
            fallbackMode = BlockingMode.FREE;
        } else {
            fallbackMode = BlockingMode.EMPTY_MAP;
        }
    }

    /**
     * Updates the metadata of the player, possibly leading to a relaunch of the executors thread.
     *
     * @param data The MetaData to update
     * @return True if the player should terminate due to speed change, False otherwise
     * @see PlayerManager#changeSpeed(int)
     */
    public boolean updateMetaData(@NotNull MetaData data) {
        boolean newSpeed = false;
        if (metaData.bpm() != data.bpm()) {
            newSpeed = true;
            controller.changeSpeed(data.bpm());
        }
        metaData = data;
        return newSpeed;
    }

    /**
     * Runs this PlayManager, i.e. playing of beats and visualisation
     */
    @Override
    public void run() {
        try {
            if (fallbackMode == BlockingMode.RESUME) {
                fallbackMode = BlockingMode.FREE;
                lastBlockingCall = null;
            } else if (lastBlockingCall != null) {
                long diff = java.time.Duration.between(lastBlockingCall, LocalDateTime.now()).getSeconds();
                if (diff > SETTINGS.getInt("BLOCKING_TIMEOUT")) {
                    LOGGER.log(Level.WARNING, "NXT Blocking Timeout: " + diff + " No messages received, Released Map");
                    fallbackMode = BlockingMode.FREE;
                    lastBlockingCall = null;
                }
            }

            final int mapScalingFactor = 100;

            if (updateMetaData(map.getMetaData())) return;
            Mat image = map.draw(mapScalingFactor, beat);

            if (fallbackMode == BlockingMode.INTERRUPTED) {
                Imgproc.resize(INTERRUPT_MESSAGE, INTERRUPT_MESSAGE, image.size());
                Core.addWeighted(image, 0.3, INTERRUPT_MESSAGE, 1, 0, image);
            } else if (fallbackMode == BlockingMode.EMPTY_MAP) {
                Imgproc.resize(EMPTY_MESSAGE, EMPTY_MESSAGE, image.size());
                Core.addWeighted(image, 1, EMPTY_MESSAGE, 1, 0, image);
            } else if (fallbackMode == BlockingMode.GPDF) {
                Imgproc.resize(GPDF_MESSAGE, GPDF_MESSAGE, image.size());
                Core.addWeighted(image, 1, GPDF_MESSAGE, 1, 0, image);
            }
            viewer.displayMat(image);

            try {
                if (SETTINGS.getBoolean("ENABLE_CLICK") && beat % 4 == 0) communicator.sendMessage(metaData.volume());
                for (Map.Entry<Integer, Brick> brickPair : map.getBrick(beat).entrySet()) {
                    Brick brickToSend = brickPair.getValue();
                    int reversedKey = Math.abs(BrickType.GROUND_PLATE.getHeight() - brickPair.getKey());
                    SoundData message = new SoundData(reversedKey, brickToSend, bpmToSpeed(metaData.bpm()), metaData.volume(),
                            metaData.pitch());
                    communicator.sendMessage(message);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            beat++;
            if (beat >= 32) beat = 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "An error occurred while handling visual presentation", e);
        }
    }

    /**
     * Converts BPM to LEGO play speed in ms
     *
     * @return Sleeping time between beats for quarter notes
     */
    public static int bpmToSpeed(int bpm) {
        return (60000 / bpm) / 4; // divide by 4 required as 32 pins are split upon 8 quarter notes
    }

    /**
     * @return True if the player manager is currently blocked, False otherwise
     */
    public boolean isBlocked() {
        return fallbackMode != BlockingMode.FREE && lastBlockingCall != null;
    }

    /**
     * Tells the manager to ignore current changes due to blocking or not
     *
     * @param mode The mode to set
     * @see Player#fallbackMode
     * @see Player#lastBlockingCall
     */
    public void block(BlockingMode mode) {
        if (mode == BlockingMode.RESUME && fallbackMode != BlockingMode.GPDF && fallbackMode != BlockingMode.EMPTY_MAP) {
            fallbackMode = BlockingMode.RESUME;
        } else if (mode != BlockingMode.RESUME) {
            fallbackMode = mode;
        }

        if (fallbackMode == BlockingMode.INTERRUPTED) {
            lastBlockingCall = LocalDateTime.now();
        }
    }

    public MetaData getMetaData() {
        return metaData;
    }
}
