package org.brickmusic.view;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static org.opencv.imgcodecs.Imgcodecs.imread;

/**
 * Basic class for real-time result presentation
 */
public class Viewer {
    /**
     * Component holding the image
     */
    private final JLabel image;

    /**
     * Currently stored Mat object ("frame")
     */
    private Mat frame;

    /**
     * Starts a new Viewer that shows the loaded frame (if available)
     */
    public Viewer(int refreshRate, String title) {
        JFrame frame = new JFrame("Brick Music: " + title);
        frame.setIconImage(matToBufferedImage(imread("icon.png")));
        image = new JLabel();
        image.setSize(960, 540);
        frame.add(image);
        frame.pack();
        frame.setVisible(true);
        frame.setSize(960, 540);

        Timer timer = new Timer(refreshRate, e -> updateImage());
        timer.start();
    }

    /**
     * Set the current frame
     *
     * @param mat The new Mat to show
     */
    public void displayMat(@NotNull Mat mat) {
        frame = mat.clone();
    }

    /**
     * Shows the current frame if available
     */
    private void updateImage() {
        if (frame != null) {
            this.image.setIcon(new ImageIcon(matToBufferedImage(frame).getScaledInstance(960, -1, Image.SCALE_SMOOTH)));
        }
    }

    /**
     * Reference
     * <a href="https://riptutorial.com/opencv/example/21963/converting-an-mat-object-to-an-bufferedimage-object">Mat Conversion: RipTutorial</a>
     * This methods body was taken of the given source.
     *
     * @param m The Mat to convert
     * @return The converted BufferImage
     */
    @NotNull
    private BufferedImage matToBufferedImage(@NotNull Mat m) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

}
