// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BurpTitleAndIconTest {

    private static BufferedImage squareImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, size, size);
        graphics.dispose();
        return image;
    }

    // ImageIcon waits for the image to be fully loaded, so the size is reliable
    private static int loadedWidth(Image image) {
        return new ImageIcon(image).getIconWidth();
    }

    @Test
    void buildIconListReturnsOneIconPerSize() {
        List<Image> icons = BurpTitleAndIcon.buildIconList(squareImage(256));
        assertEquals(BurpTitleAndIcon.ICON_SIZES.length, icons.size());
        for (int i = 0; i < icons.size(); i++) {
            assertEquals(BurpTitleAndIcon.ICON_SIZES[i], loadedWidth(icons.get(i)));
        }
    }

    @Test
    void buildIconListUpscalesSmallImages() {
        List<Image> icons = BurpTitleAndIcon.buildIconList(squareImage(8));
        assertEquals(BurpTitleAndIcon.ICON_SIZES.length, icons.size());
        for (int i = 0; i < icons.size(); i++) {
            assertEquals(BurpTitleAndIcon.ICON_SIZES[i], loadedWidth(icons.get(i)));
        }
    }

    @Test
    void buildIconListReturnsEmptyListForNullImage() {
        assertTrue(BurpTitleAndIcon.buildIconList(null).isEmpty());
    }

    @Test
    void iconSizesAreAscendingSoLastIsTheLargest() {
        // applyOsTaskbarIcon uses the last list entry as the largest icon
        for (int i = 1; i < BurpTitleAndIcon.ICON_SIZES.length; i++) {
            assertTrue(BurpTitleAndIcon.ICON_SIZES[i] > BurpTitleAndIcon.ICON_SIZES[i - 1]);
        }
    }

    @Test
    void customWindowsAppIdIsStable() {
        // The taskbar groups windows by this value, so it must not change between releases
        assertEquals("ninja.burpsuite.sharpener.customicon", BurpTitleAndIcon.CUSTOM_WINDOWS_APP_ID);
    }
}
