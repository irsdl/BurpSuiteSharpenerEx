// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BurpVersionNumberTest {

    @Test
    void realBuildNumbersAreDecoded() {
        // values read from real Burp jars with a probe, see CLAUDE.md
        assertEquals(2026, BurpVersionNumber.majorFromBuildNumber(20260403000047818L));
        assertEquals(4.3, BurpVersionNumber.minorFromBuildNumber(20260403000047818L));

        assertEquals(2025, BurpVersionNumber.majorFromBuildNumber(20250304000038446L));
        assertEquals(3.4, BurpVersionNumber.minorFromBuildNumber(20250304000038446L));
    }

    @Test
    void patchAndReleaseBoundariesAreDecoded() {
        // Burp 2024.2 exactly, no patch
        assertEquals(2024, BurpVersionNumber.majorFromBuildNumber(20240200000000000L));
        assertEquals(2.0, BurpVersionNumber.minorFromBuildNumber(20240200000000000L));

        // release 10 and a two digit patch, matches Double.parseDouble("10.12")
        assertEquals(10.12, BurpVersionNumber.minorFromBuildNumber(20251012000000001L));
    }

    @Test
    void unknownLayoutsReturnZero() {
        assertEquals(0, BurpVersionNumber.majorFromBuildNumber(0));
        assertEquals(0, BurpVersionNumber.minorFromBuildNumber(0));
        // too short (16 digits) and too long (18 digits)
        assertEquals(0, BurpVersionNumber.majorFromBuildNumber(2026040300004781L));
        assertEquals(0, BurpVersionNumber.majorFromBuildNumber(202604030000478180L));
        // 17 digits but an impossible year
        assertEquals(0, BurpVersionNumber.majorFromBuildNumber(99990403000047818L));
        assertEquals(0, BurpVersionNumber.minorFromBuildNumber(99990403000047818L));
        assertEquals(0, BurpVersionNumber.majorFromBuildNumber(-20260403000047818L));
    }
}
