package org.brickmusic.playmanagement;

public enum BlockingMode {

    /**
     * Indicates that a given map was empty
     */
    EMPTY_MAP,

    /**
     * Indicates that the player interrupted via pedal
     */
    INTERRUPTED,

    /**
     * Indicates that the player was interrupted but shall not be in future
     */
    RESUME,

    /**
     * Indicates a ground plate detection failure
     */
    GPDF,

    /**
     * Indicates no blocking
     */
    FREE
}
