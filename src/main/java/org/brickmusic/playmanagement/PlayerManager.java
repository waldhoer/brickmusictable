package org.brickmusic.playmanagement;

import org.brickmusic.externals.NxtManager;
import org.brickmusic.visuals.VisualManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brickmusic.Main.SETTINGS;
import static org.opencv.videoio.Videoio.CAP_DSHOW;

/**
 * The player manager is responsible for handling the executing player and attached managers.
 */
public class PlayerManager {
    private static final Logger LOGGER = Logger.getLogger(PlayerManager.class.getName());

    /**
     * Executing player, responsible for handling continuous playback
     */
    private final Player player;

    /**
     * Visual manager attached to the player
     */
    private final VisualManager visualManager;

    /**
     * Nxt manager attached to the player
     */
    private final NxtManager nxtManager;

    /**
     * Scheduler responsible for handling non-player execution
     */
    private final static ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);

    /**
     * Scheduler responsible for handling player execution. This executor may be reassigned upon speed change.
     *
     * @see PlayerManager#changeSpeed(int)
     */
    private ScheduledExecutorService playerScheduler = Executors.newScheduledThreadPool(1);

    /**
     * Internal state for checking if the player is already running
     */
    private boolean running = false;

    /**
     * Launches a new Player Manager and creates required sub managers
     *
     * @throws IOException If manager creation fails
     */
    public PlayerManager() throws IOException {
        player = new Player(this);
        visualManager = new VisualManager(SETTINGS.getInt("CAMERA_INDEX"), CAP_DSHOW, player);
        nxtManager = new NxtManager(5555, player);

        if (!visualManager.validateCameraInput()) {
            LOGGER.log(Level.SEVERE, "BrickMusic Terminated due to errors on webcam access");
            System.exit(1);
        }
    }

    /**
     * Starts all sub managers handled by the player manager.
     */
    public void start() {
        if (running) {
            LOGGER.warning("PlayerManager start omitted: Manager is already running");
            return;
        }
        SCHEDULER.scheduleAtFixedRate(nxtManager, 0, 1, TimeUnit.MILLISECONDS);
        SCHEDULER.scheduleAtFixedRate(visualManager, 0, SETTINGS.getInt("FRAME_DELAY"), TimeUnit.MILLISECONDS);
        playerScheduler.scheduleAtFixedRate(player, 0, Player.bpmToSpeed(SETTINGS.getInt("BPM")), TimeUnit.MILLISECONDS);
        running = true;
    }

    /**
     * Stops all sub managers handled by the player manager.
     */
    public void stop() {
        nxtManager.terminate();
        visualManager.terminate();
        SCHEDULER.close();
        playerScheduler.close();
    }

    /**
     * Relaunches the player scheduler with a new speed. The old scheduler gets terminated and then reassigned.
     *
     * @param bpm The new speed to set
     */
    public void changeSpeed(int bpm) {
        playerScheduler.shutdown();
        playerScheduler = Executors.newScheduledThreadPool(2);
        playerScheduler.scheduleAtFixedRate(player, 0, Player.bpmToSpeed(bpm), TimeUnit.MILLISECONDS);
    }
}
