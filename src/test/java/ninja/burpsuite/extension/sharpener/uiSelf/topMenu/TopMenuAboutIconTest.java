// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiSelf.topMenu;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.generic.ImageHelper;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

// The About menu item shows the extension logo. The old rotated and cropped
// image looked cut off, so the item now uses the square icon at menu size.
public class TopMenuAboutIconTest {

    @Test
    void aboutLogoResourceIsSquare() {
        BufferedImage logo = ImageHelper.loadImageResource(ExtensionSharedParameters.class, "/sharpener.png");
        assertNotNull(logo, "the About logo resource must exist in the jar");
        assertEquals(logo.getWidth(), logo.getHeight(), "the logo must be square so the menu item is not distorted");
    }

    @Test
    void aboutLogoScalesToMenuIconSize() {
        BufferedImage logo = ImageHelper.loadImageResource(ExtensionSharedParameters.class, "/sharpener.png");
        Image scaled = ImageHelper.scaleImageToWidth(logo, 24);
        ImageIcon icon = new ImageIcon(scaled); // ImageIcon waits until the scaled image is ready
        assertEquals(24, icon.getIconWidth());
        assertEquals(24, icon.getIconHeight());
    }

    @Test
    void oldRotatedLogoResourcesAreGone() {
        // these cropped images were only used by the About menu item and were removed
        assertNull(ExtensionSharedParameters.class.getResource("/sharpener_rotated_small.png"));
        assertNull(ExtensionSharedParameters.class.getResource("/sharpener_rotated_normal.png"));
    }
}
