package org.brickmusic.visuals;

/**
 * Exception thrown upon invalid image properties.
 * This does not indicate logical related aspects but rather image-related aspects like an empty image, wrong size etc.
 */
public class InvalidImageException extends Exception {
    public final static String EMPTY_FRAME = "The provided image is empty and cannot be processed. This is likely to be caused " +
            "by errors in image grabbing.";
    public final static String CORRUPTED_FRAME = "The provided image is corrupted, it may not contain enough channels or " +
            "contains other issues.";

    /**
     * Creates a new Invalid Frame Exception
     *
     * @param message The message to use
     */
    public InvalidImageException(String message) {
        super(message);
    }
}
