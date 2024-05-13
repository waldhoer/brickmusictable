package org.brickmusic.visuals;

/**
 * Exception thrown upon missing visual identification.
 * Indicates that either a groundPlate is missing or visual conditions are insufficient.
 */
public class ImageGridException extends Exception {

    public final static int NO_GROUND_PLATE = 0;
    public final static int NO_VISUALS = 1;

    /**
     * Creates a new exception caused by visual problems
     *
     * @param type The type of the exception. See NO_GROUND_PLATE and NO_VISUALS
     */
    public ImageGridException(int type) {
        super(
                switch (type) {
                    case 0:
                        yield "No ground plate was found. Hint: Try improving the visual environment.";
                    case 1:
                        yield "No visual objects were found. Hint: Ensure that the correct camera is selected.";
                    case 2:
                        yield "The detected ground plate candidate exhibits abnormal size(s).";
                    default:
                        yield "An error occurred while detecting visual objects";
                }
        );
    }

}
