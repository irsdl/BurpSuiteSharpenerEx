// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Headless tests for the window geometry logic behind the off screen
// detection and restore feature. Only pure methods are tested because
// CI has no display.
class UIHelperTest {

    private static final Rectangle SINGLE_SCREEN = new Rectangle(0, 0, 1920, 1080);

    // isBoundsOutOfScreen

    @Test
    void frameFullyInsideIsNotOutOfScreen() {
        Rectangle frame = new Rectangle(200, 100, 800, 600);
        assertFalse(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0));
        assertFalse(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0.8));
    }

    @Test
    void framePartiallyOutIsDetectedWithZeroMargin() {
        Rectangle frame = new Rectangle(1900, 500, 800, 600);
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0));
    }

    @Test
    void framePartiallyOutWithinMarginIsTolerated() {
        // right edge is 480px (60% of the width) outside, allowed margin is 80%
        Rectangle frame = new Rectangle(1600, 500, 800, 600);
        assertFalse(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0.8));
    }

    @Test
    void frameBeyondMarginIsDetected() {
        // right edge is 780px (97% of the width) outside, more than the 80% margin
        Rectangle frame = new Rectangle(1900, 500, 800, 600);
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0.8));
    }

    @Test
    void frameFullyOffScreenIsAlwaysDetected() {
        Rectangle frame = new Rectangle(5000, 5000, 800, 600);
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0));
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0.8));
    }

    @Test
    void windowsMinimisedSentinelPositionIsDetected() {
        // Windows parks minimised windows at -32000,-32000
        Rectangle frame = new Rectangle(-32000, -32000, 800, 600);
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 0.8));
    }

    @Test
    void frameOnSecondaryScreenWithNegativeCoordinatesIsNotOutOfScreen() {
        // a monitor placed to the left of the primary produces a union with a negative x
        Rectangle union = new Rectangle(-1920, 0, 3840, 1080);
        Rectangle frame = new Rectangle(-1000, 100, 800, 600);
        assertFalse(UIHelper.isBoundsOutOfScreen(frame, union, 0));
    }

    @Test
    void invalidMarginIsTreatedAsZero() {
        Rectangle frame = new Rectangle(1900, 500, 800, 600);
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, 5));
        assertTrue(UIHelper.isBoundsOutOfScreen(frame, SINGLE_SCREEN, -1));
    }

    // isSizeTooSmall

    @Test
    void nullSizeIsTooSmall() {
        assertTrue(UIHelper.isSizeTooSmall(null, new Dimension(100, 100)));
    }

    @Test
    void tinySizeInEitherDirectionIsTooSmall() {
        Dimension min = new Dimension(100, 100);
        assertTrue(UIHelper.isSizeTooSmall(new Dimension(50, 500), min));
        assertTrue(UIHelper.isSizeTooSmall(new Dimension(500, 50), min));
        assertTrue(UIHelper.isSizeTooSmall(new Dimension(0, 0), min));
    }

    @Test
    void sizeAtOrAboveMinimumIsNotTooSmall() {
        Dimension min = new Dimension(100, 100);
        assertFalse(UIHelper.isSizeTooSmall(new Dimension(100, 100), min));
        assertFalse(UIHelper.isSizeTooSmall(new Dimension(1200, 800), min));
    }

    // getCenteredLocation

    @Test
    void centeredLocationIsInTheMiddleOfTheUsableArea() {
        Point location = UIHelper.getCenteredLocation(new Rectangle(0, 0, 1920, 1040), new Dimension(800, 600));
        assertEquals(new Point(560, 220), location);
    }

    @Test
    void centeredLocationHonoursTheUsableAreaOrigin() {
        // usable area starts below and right of the screen origin, for example due to a taskbar
        Point location = UIHelper.getCenteredLocation(new Rectangle(100, 50, 1000, 900), new Dimension(400, 300));
        assertEquals(new Point(400, 350), location);
    }

    @Test
    void frameLargerThanScreenIsClampedSoTheTitleBarStaysReachable() {
        Point location = UIHelper.getCenteredLocation(new Rectangle(0, 0, 1920, 1040), new Dimension(2400, 1600));
        assertEquals(new Point(0, 0), location);
    }
}
