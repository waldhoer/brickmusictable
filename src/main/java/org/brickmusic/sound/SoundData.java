package org.brickmusic.sound;

import org.brickmusic.bricklogic.Brick;
import org.brickmusic.bricklogic.InstrumentColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * SoundData contains all OSC relevant information that will be transmitted to SonicPi
 */
public class SoundData {

    /**
     * Instrument Channel
     */
    private final int channel;

    /**
     * Duration of the note in ms
     */
    private final int duration;

    /**
     * Key of the sound in MIDI notational int
     */
    private final int key;

    /**
     * The volume of the sound
     */
    private final double volume;

    /**
     * Rotation in degrees, sound property is handled in sonic pi
     */
    private final int rotationProperty;

    /**
     * Minimum deepest note playable
     */
    public static final int BASE_KEY_SHIFT = 48;

    /**
     * Converts a Brick with specific position to SoundData
     *
     * @param yLocation     The y location = note height of the brick
     * @param brick         The brick
     * @param speed         The speed of playing, required to calculate the note duration
     * @param generalVolume The general volume as starting volume
     */
    public SoundData(int yLocation, @NotNull Brick brick, int speed, double generalVolume, int pitch) {
        int keyShift = BASE_KEY_SHIFT + pitch, octaveShift = 12, volumeIncreaseFactor = 3;
        for (InstrumentColor extension : brick.getExtensions()) {
            if (extension.equals(InstrumentColor.GREEN)) keyShift += octaveShift;
            if (extension.equals(InstrumentColor.RED)) keyShift -= octaveShift;
            if (extension.equals(InstrumentColor.BLACK)) generalVolume *= volumeIncreaseFactor;
        }
        this.channel = brick.getColor().ordinal();
        this.rotationProperty = (int) Math.round(brick.getRotation());
        this.duration = speed * brick.getWidth();
        this.key = yLocation + keyShift;
        this.volume = generalVolume;
    }

    /**
     * @return Instrument Channel
     */
    public int getChannel() {
        return channel;
    }

    /**
     * @return Duration in milliseconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @return Base key
     */
    public int getKey() {
        return key;
    }

    /**
     * @return Sound volume
     */
    public double getVolume() {
        return volume;
    }

    /**
     * @return Rotation property value
     */
    public int getRotationProperty() {
        return rotationProperty;
    }

    @Override
    public String toString() {
        return "SoundData{" +
                "channel=" + channel +
                ", duration=" + duration +
                ", key=" + key +
                ", volume=" + volume +
                '}';
    }

    /**
     * Gets the note name of a given MIDI heigt value
     *
     * @param height The note height as number
     * @return A string concatenating the note+octave, e.g. "D3"
     */
    @NotNull
    @Contract(pure = true)
    public static String midiHeightToKeyString(int height) {
        String[] keys = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return keys[height % 12] + (height / keys.length - 1);
    }
}
