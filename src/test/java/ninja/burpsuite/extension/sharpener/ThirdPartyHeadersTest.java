// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// Guards the provenance notices on vendored third-party code: the files under
// libs/thirdparty come from Burp-Montoya-Utilities by Corey Arthur and must keep his
// copyright and license notice instead of the Sharpener header. The Sharpener section 7
// additional terms must never be extended to this material.
public class ThirdPartyHeadersTest {

    private static final Path VENDORED_ROOT = Path.of("src", "main", "java", "ninja", "burpsuite", "libs", "thirdparty");

    private static final String VENDORED_FROM_LINE = "// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)";
    private static final String UPSTREAM_COMMIT_LINE = "// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)";
    private static final String COPYRIGHT_LINE = "// Copyright (C) Corey Arthur";
    private static final String LICENSE_FRAGMENT = "Released under AGPL v3.0, see the LICENSE file";
    private static final String EXCLUSION_FRAGMENT = "not covered by the";
    private static final String EXCLUSION_FRAGMENT_CONTINUED = "additional terms in the NOTICE file";

    @Test
    void everyVendoredFileKeepsTheUpstreamNotices() throws IOException {
        try (Stream<Path> files = Files.walk(VENDORED_ROOT)) {
            List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java")).toList();

            // guard against the directory moving and this test silently passing on nothing
            assertEquals(12, javaFiles.size(), "unexpected number of vendored files under " + VENDORED_ROOT);
            assertTrue(javaFiles.stream().anyMatch(p -> p.getFileName().toString().equals("Preferences.java")));

            for (Path p : javaFiles) {
                String head = String.join("\n", Files.readAllLines(p).stream().limit(10).toList());
                assertTrue(head.contains(VENDORED_FROM_LINE), "Missing vendored-from line in " + p);
                assertTrue(head.contains(UPSTREAM_COMMIT_LINE), "Missing upstream URL and commit in " + p);
                assertTrue(head.contains(COPYRIGHT_LINE), "Missing Corey Arthur copyright in " + p);
                assertTrue(head.contains(LICENSE_FRAGMENT), "Missing license line in " + p);
                assertTrue(head.contains(EXCLUSION_FRAGMENT) && head.contains(EXCLUSION_FRAGMENT_CONTINUED),
                        "Missing the section 7 exclusion note in " + p);
            }
        }
    }

    @Test
    void noticeDeclaresTheThirdPartyCodeSection() throws IOException {
        String notice = Files.readString(Path.of("NOTICE"));
        assertTrue(notice.contains("Third-party code"));
        assertTrue(notice.contains("Burp-Montoya-Utilities"));
        assertTrue(notice.contains("Corey Arthur"));
        assertTrue(notice.contains("b7faf563"));
        assertTrue(notice.contains("apply only to material authored"),
                "NOTICE must state the section 7 terms do not cover the vendored files");
    }
}
