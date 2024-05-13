package org.brickmusic.visuals;

/**
 * Exception caused by invalid camera access. Camera access may be denied upon missing camera access or driver incompatibility.
 */
public class CameraAccessException extends RuntimeException {

    /**
     * Generates a new camera access exception
     *
     * @param message The detailed message
     */
    public CameraAccessException(String message) {
        super(message);
    }
}
