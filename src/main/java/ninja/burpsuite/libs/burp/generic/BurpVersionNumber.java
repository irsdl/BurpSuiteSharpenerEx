// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.burp.generic;

/**
 * Converts the Montoya buildNumber() long into version values.
 * The deprecated Version.major() and Version.minor() methods must not be used.
 * <p>
 * Verified layout on Burp 2025.3.4 and 2026.4.3 (17 digits): YYYY RR PP XXX BBBBBB
 * where YYYY is the year, RR is the release, PP is the patch,
 * XXX is unused, and BBBBBB is the internal build number.
 * Example: Burp 2026.4.3 build 47818 gives 20260403000047818.
 * <p>
 * PortSwigger does not document this layout, so it may change.
 * When the layout is not recognised, these helpers return 0 (unknown version).
 */
public class BurpVersionNumber {

    // 17 digit numbers only: anything else is an unknown layout
    private static final long MIN_VALID = 10_000_000_000_000_000L;
    private static final long MAX_VALID = 99_999_999_999_999_999L;

    private BurpVersionNumber() {
    }

    /**
     * The Burp year, for example 2026. Returns 0 when the build number is not recognised.
     */
    public static double majorFromBuildNumber(long buildNumber) {
        if (!isValid(buildNumber))
            return 0;
        return buildNumber / 10_000_000_000_000L;
    }

    /**
     * The Burp release and patch as one number, for example 4.3 for Burp 2026.4.3.
     * Returns 0 when the build number is not recognised.
     */
    public static double minorFromBuildNumber(long buildNumber) {
        if (!isValid(buildNumber))
            return 0;
        long release = (buildNumber / 100_000_000_000L) % 100;
        long patch = (buildNumber / 1_000_000_000L) % 100;
        // same result as parsing "release.patch" as a double, for example "4.3" or "4.10"
        return release + (patch < 10 ? patch / 10.0 : patch / 100.0);
    }

    private static boolean isValid(long buildNumber) {
        if (buildNumber < MIN_VALID || buildNumber > MAX_VALID)
            return false;
        long year = buildNumber / 10_000_000_000_000L;
        return year >= 2020 && year <= 2100;
    }
}
