// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionSharedParametersTest {

    @Test
    void oldBurpVersionsUseTheBackgroundForTabTextColour() {
        assertTrue(ExtensionSharedParameters.isTabTextColorSetByBackgroundForVersion(2023));
        assertTrue(ExtensionSharedParameters.isTabTextColorSetByBackgroundForVersion(2025.12));
        // an unknown version that could not be parsed stays on the old path
        assertTrue(ExtensionSharedParameters.isTabTextColorSetByBackgroundForVersion(0));
    }

    @Test
    void newBurpVersionsSetTheTabTextColourDirectly() {
        assertFalse(ExtensionSharedParameters.isTabTextColorSetByBackgroundForVersion(2026));
        assertFalse(ExtensionSharedParameters.isTabTextColorSetByBackgroundForVersion(2026.4));
        assertFalse(ExtensionSharedParameters.isTabTextColorSetByBackgroundForVersion(2027));
    }
}
