// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.objects;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class TabFeaturesObjectStyleTest {

    @Test
    void colorIsStoredAsHexAndReadBack() {
        TabFeaturesObjectStyle style = new TabFeaturesObjectStyle("test", "Dialog", 12, false, false, true, Color.RED, "", 0);
        assertEquals("#ff0000", style.colorCode);
        assertEquals(Color.RED, style.getColor());
    }

    @Test
    void setColorUpdatesColorCode() {
        TabFeaturesObjectStyle style = new TabFeaturesObjectStyle("test", "Dialog", 12, false, false, true, Color.RED, "", 0);
        style.setColor(Color.decode("#010101"));
        assertEquals("#010101", style.colorCode);
        assertEquals(Color.decode("#010101"), style.getColor());
    }

    @Test
    void invalidColorCodeFallsBackToBlack() {
        TabFeaturesObjectStyle style = new TabFeaturesObjectStyle();
        style.colorCode = "not-a-color";
        assertEquals(Color.BLACK, style.getColor());
    }

    @Test
    void equalsIgnoreColorIgnoresOnlyTheColor() {
        TabFeaturesObjectStyle style1 = new TabFeaturesObjectStyle("a", "Dialog", 12, true, false, true, Color.RED, "", 0);
        TabFeaturesObjectStyle style2 = new TabFeaturesObjectStyle("b", "Dialog", 12, true, false, true, Color.BLUE, "", 0);
        assertFalse(style1.equals(style2));
        assertTrue(style1.equalsIgnoreColor(style2));
    }

    @Test
    void equalsComparesAllStyleValues() {
        TabFeaturesObjectStyle style1 = new TabFeaturesObjectStyle("a", "Dialog", 12, true, false, true, Color.RED, "", 0);
        TabFeaturesObjectStyle style2 = new TabFeaturesObjectStyle("b", "Dialog", 12, true, false, true, Color.RED, "", 0);
        TabFeaturesObjectStyle style3 = new TabFeaturesObjectStyle("c", "Dialog", 14, true, false, true, Color.RED, "", 0);
        assertTrue(style1.equals(style2));
        assertFalse(style1.equals(style3));
    }

    @Test
    void equalsComparesFontNamesByValue() {
        // font names read from the UI are new string objects, so a value comparison is needed
        TabFeaturesObjectStyle style1 = new TabFeaturesObjectStyle("a", new String("Dialog"), 12, true, false, true, Color.RED, "", 0);
        TabFeaturesObjectStyle style2 = new TabFeaturesObjectStyle("b", new String("Dialog"), 12, true, false, true, Color.RED, "", 0);
        assertTrue(style1.equals(style2));
    }

    @Test
    void equalStylesHaveTheSameHashCode() {
        // the name field is not part of equality, so it must not change the hash code
        TabFeaturesObjectStyle style1 = new TabFeaturesObjectStyle("a", "Dialog", 12, true, false, true, Color.RED, "", 0);
        TabFeaturesObjectStyle style2 = new TabFeaturesObjectStyle("b", "Dialog", 12, true, false, true, Color.RED, "", 0);
        assertTrue(style1.equals(style2));
        assertEquals(style1.hashCode(), style2.hashCode());
    }

    @Test
    void equalTabFeaturesObjectsHaveTheSameHashCode() {
        TabFeaturesObject tfo1 = new TabFeaturesObject(0, "Tab Title", new String[]{}, "Dialog", 12, true, false, true, Color.RED, "", 0);
        TabFeaturesObject tfo2 = new TabFeaturesObject(1, "tab title", new String[]{}, "Dialog", 12, true, false, true, Color.RED, "", 0);
        assertTrue(tfo1.equals(tfo2)); // titles are compared trimmed and in lowercase, the index is ignored
        assertEquals(tfo1.hashCode(), tfo2.hashCode());
    }

    @Test
    void duplicateCreatesAnEqualButIndependentCopy() {
        TabFeaturesObjectStyle original = new TabFeaturesObjectStyle("a", "Dialog", 12, true, false, true, Color.RED, "icon", 16);
        TabFeaturesObjectStyle copy = original.duplicate();

        assertTrue(original.equals(copy));

        copy.setColor(Color.BLUE);
        assertEquals(Color.RED, original.getColor());
        assertEquals(Color.BLUE, copy.getColor());
    }
}
