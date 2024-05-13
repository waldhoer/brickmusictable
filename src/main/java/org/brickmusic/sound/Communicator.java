package org.brickmusic.sound;

import org.jetbrains.annotations.NotNull;
import org.openrndr.extra.osc.OSC;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Responsible class for handling sound communication with Sonic Pi.
 */
public class Communicator {

    /**
     * Listening Channel preamble of Sonic Pi osc communication port. See the
     * <a href="https://sonic-pi.net/tutorial.html">Sonic Pi Tutorial</a> and check the port under "I/O>Incoming OSC-Port"
     */
    private static final String CHANNEL = "/midi";

    /**
     * The port to send from
     */
    private final OSC port;

    /**
     * Creates a new Sound manager
     *
     * @throws IOException If the port could not be opened
     */
    public Communicator() throws IOException {
        port = new OSC(InetAddress.getLocalHost(), 4561, 4560);
    }

    /**
     * Sends a sound message containing all relevant information
     */
    public void sendMessage(@NotNull SoundData note) {
        port.send(CHANNEL, note.getKey(), note.getDuration(), note.getChannel(), note.getVolume(), note.getRotationProperty());
    }

    /**
     * Sends a click beat OSC message.
     * For click messages channel 55 is reserved. Do not use this channel otherwise.
     */
    public void sendMessage(double generalVolume) {
        port.send(CHANNEL, 0, 0, 55, generalVolume, 0);
    }
}
