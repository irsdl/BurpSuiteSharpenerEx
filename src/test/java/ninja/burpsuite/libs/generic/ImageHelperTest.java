// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// These tests cover the headless component capture helpers. They paint a component
// into an image without using the screen, so they must work without a display.
public class ImageHelperTest {

    private JPanel buildRedPanel(int width, int height) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.RED);
        panel.setOpaque(true);
        panel.setSize(width, height);
        return panel;
    }

    @Test
    void captureComponentPaintsTheComponentPixels() {
        BufferedImage image = ImageHelper.captureComponent(buildRedPanel(20, 10));

        assertNotNull(image);
        assertEquals(20, image.getWidth());
        assertEquals(10, image.getHeight());
        assertEquals(Color.RED.getRGB(), image.getRGB(10, 5));
    }

    @Test
    void captureComponentReturnsNullForInvalidComponents() {
        assertNull(ImageHelper.captureComponent(null));
        // a component that has no size yet cannot be painted
        assertNull(ImageHelper.captureComponent(new JPanel()));
    }

    @Test
    void saveComponentAsPngWritesAReadablePngFile(@TempDir Path tempDir) throws Exception {
        File outputFile = tempDir.resolve("panel.png").toFile();

        assertTrue(ImageHelper.saveComponentAsPng(buildRedPanel(20, 10), outputFile.getAbsolutePath()));

        BufferedImage savedImage = ImageIO.read(outputFile);
        assertNotNull(savedImage);
        assertEquals(20, savedImage.getWidth());
        assertEquals(10, savedImage.getHeight());
        assertEquals(Color.RED.getRGB(), savedImage.getRGB(10, 5));
    }

    @Test
    void saveComponentAsPngFailsSafelyForInvalidInput(@TempDir Path tempDir) {
        File outputFile = tempDir.resolve("empty.png").toFile();

        assertFalse(ImageHelper.saveComponentAsPng(new JPanel(), outputFile.getAbsolutePath()));
        assertFalse(outputFile.exists());
        // an unwritable path must not throw
        assertFalse(ImageHelper.saveComponentAsPng(buildRedPanel(5, 5), tempDir.resolve("no-dir").resolve("x.png").toString()));
    }
}
