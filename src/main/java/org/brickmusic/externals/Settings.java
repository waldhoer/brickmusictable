package org.brickmusic.externals;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple settings class for reading basic settings from json file.
 * <p>
 * "How to read JSON from a file using Gson in Java"
 * <a href="https://attacomsian.com/blog/gson-read-json-file">see source</a>
 *
 * @see Gson
 */
public class Settings {

    private static final Logger LOGGER = Logger.getLogger(Settings.class.getName());

    /**
     * Stores the loaded settings
     */
    private final Map<?, ?> settings;

    /**
     * Reads the settings file
     *
     * @param fileName The file to read
     */
    public Settings(String fileName) throws IOException {
        Path path = Path.of(fileName);
        if (!Files.exists(path))
            throw new IOException("Settings file (" + fileName + ") not found");
        Gson gson = new Gson();
        var reader = Files.newBufferedReader(path);
        settings = gson.fromJson(reader, Map.class);
    }

    /**
     * Convenience method for accessing a specific type from settings
     *
     * @param propertyName The property to read
     * @return The mapped value or null if not found
     */
    public Object get(String propertyName) {
        if (settings.get(propertyName) == null)
            LOGGER.warning("Reading settings from uninitialised Settings object for " + propertyName);
        return settings.get(propertyName);
    }

    /**
     * Convenience method for accessing a specific type from settings
     *
     * @param propertyName The property to read
     * @return The mapped int or null if not found
     */
    public int getInt(String propertyName) {
        return (get(propertyName) == null) ? -1 : (int) (double) get(propertyName);
    }

    /**
     * Convenience method for accessing a specific type from settings
     *
     * @param propertyName The property to read
     * @return The mapped boolean or false if not found
     */
    public boolean getBoolean(String propertyName) {
        return get(propertyName) != null && (boolean) get(propertyName);
    }

    /**
     * Convenience method for accessing a specific type from settings
     *
     * @param propertyName The property to read
     * @return The mapped String or an empty string if not found
     */
    public String getString(String propertyName) {
        return (get(propertyName) == null) ? "" : String.valueOf(get(propertyName));
    }

    public double getDouble(String propertyName) {
        return (get(propertyName) == null) ? -1 : (double) get(propertyName);
    }
}
