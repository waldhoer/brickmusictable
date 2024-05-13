package org.brickmusic.externals;

import org.brickmusic.playmanagement.BlockingMode;
import org.brickmusic.playmanagement.Player;
import org.brickmusic.visuals.ImageRecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brickmusic.Main.SETTINGS;

/**
 * The NxtManager class offers support for connected NXT Mindstorms Devices.
 */
public class NxtManager extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger(ImageRecognizer.class.getName());

    /**
     * Socket receiving messages from the NXT python program
     */
    private final ServerSocket serverSocket;

    /**
     * The player manager to notify upon change
     */
    private final Player player;

    /**
     * The executing sender (python) process
     */
    private final Process process;

    /**
     * Creates a new NxtManager and opens a new server socket
     *
     * @param port   The port to receive on, Set this equally to the port given in the python script
     * @param player The player manager to notify
     * @throws IOException Upon server error
     */
    public NxtManager(int port, Player player) throws IOException {
        serverSocket = new ServerSocket(port);
        this.player = player;

        // Initialize process
        try {
            // Get  path, see https://docs.oracle.com/javase/6/docs/api/java/lang/System.html#getProperty%28java.lang.String%29
            // Ensure that the python script is in src/main/python/
            Path mainPath = Paths.get(System.getProperty("user.dir"));
            ProcessBuilder processBuilder = new ProcessBuilder("python ",
                    mainPath + "\\src\\main\\python\\" + SETTINGS.getString("SCRIPT_NAME"));
            if (SETTINGS.getBoolean("DEBUG_MODE_ACTIVE"))
                processBuilder.redirectOutput(new File(mainPath + "\\src\\main\\python\\" + SETTINGS.getString("SCRIPT_OUTPUT")));
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            if (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String[] unwrapped = bufferedReader.readLine().split(",");
                boolean blocked = Boolean.parseBoolean(unwrapped[0]);
                player.block((blocked) ? BlockingMode.INTERRUPTED : BlockingMode.RESUME);
                socket.close();
            }
        } catch (SocketException s) {
            LOGGER.info("Socket closure: NxtHandles socket was closed.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while receiving", e);
        }
    }

    /**
     * Closes communication sockets and terminates the sender script
     */
    public void terminate() {
        try {
            serverSocket.close();
            process.destroy();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while closing server socket", e);
        }
    }
}
