package org.brickmusic;

import org.brickmusic.externals.Settings;
import org.brickmusic.playmanagement.PlayerManager;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main {
    public static final Settings SETTINGS;

    static {
        try {
            SETTINGS = new Settings("settings.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Loader.load(opencv_java.class);

        final Logger LOGGER = Logger.getLogger(Main.class.getName());

        LOGGER.info("BrickMusic Started");

        try {
            PlayerManager player = new PlayerManager();
            player.start();

            System.out.println("Press the return key to terminate");
            new Scanner(System.in).next();

            player.stop();

            LOGGER.info("BrickMusic Terminated");
            System.exit(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal exception", e);
        }
    }
}