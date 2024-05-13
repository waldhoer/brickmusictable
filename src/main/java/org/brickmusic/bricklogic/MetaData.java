package org.brickmusic.bricklogic;

/**
 * Metadata represents data that is not mapped to a specific brick but applies to the hole note sheet.
 *
 * @param bpm    Speed of playing in beats per minute
 * @param volume General play volume
 * @param pitch  The general note pitch
 */
public record MetaData(int bpm, double volume, int pitch) {
    /**
     * Row mapping descriptions of the metadata in increasing row order
     */
    private final static String[] ROW_MAPPINGS = {"Speed", "Volume", "Pitch"};

    /**
     * Creates new metadata
     *
     * @param bpm    The bpm to set
     * @param volume The volume to set
     * @param pitch  General pitch
     */
    public MetaData {
        if (bpm <= 0) {
            throw new IllegalArgumentException("Invalid BPM specification (" + bpm + ") as meta data: BPM must be > 0");
        } else if (volume < 0) {
            throw new IllegalArgumentException("Invalid volume specification (" + bpm + ") as meta data: Volume must be >= 0");
        }
    }

    /**
     * @return The mapping description of the metadata rows in increasing row index order
     */
    public static String[] mappings() {
        return ROW_MAPPINGS;
    }
}
