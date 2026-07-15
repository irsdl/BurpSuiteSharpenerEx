// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

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
