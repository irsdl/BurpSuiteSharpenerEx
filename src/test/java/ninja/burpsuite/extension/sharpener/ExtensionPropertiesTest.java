// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionPropertiesTest {

    private static final Properties props = new Properties();

    @BeforeAll
    static void loadProperties() throws Exception {
        try (InputStream in = ExtensionPropertiesTest.class.getResourceAsStream("/extension.properties")) {
            assertNotNull(in, "extension.properties must be on the classpath");
            props.load(in);
        }
    }

    @Test
    void versionIsParseableAsDouble() {
        // the update checker compares this value numerically
        double version = Double.parseDouble(props.getProperty("version"));
        assertTrue(version > 0);
    }

    @Test
    void copyrightShowsLicenseAndDeveloperOnly() {
        // shown in the About dialog; the MDSec history moved to the README
        String copyright = props.getProperty("copyright");
        assertNotNull(copyright);
        assertTrue(copyright.contains("AGPL"));
        assertTrue(copyright.contains("Soroush Dalili"));
        assertFalse(copyright.contains("MDSec"));
    }

    @Test
    void minSupportedBurpVersionIs2024_2() {
        // Burp 2024.2 is the first release that requires Java 21.
        // The jar contains Java 21 bytecode, so older Burp versions cannot load it.
        // Major is the year, minor is the release number (verified on Burp 2025.3.4 and 2026.4.3).
        assertEquals(2024, Double.parseDouble(props.getProperty("minSupportedMajorVersionInclusive")));
        assertEquals(2, Double.parseDouble(props.getProperty("minSupportedMinorVersionInclusive")));
    }
}
