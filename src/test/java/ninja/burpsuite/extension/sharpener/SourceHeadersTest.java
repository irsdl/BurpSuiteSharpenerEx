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

// Guards the legal notices required by the NOTICE file (AGPL section 7 terms):
// every source file must keep the copyright notice and the developer attribution.
public class SourceHeadersTest {

    private static final String COPYRIGHT_LINE = "// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)";
    private static final String ATTRIBUTION_LINE = "// Developed by Soroush Dalili (@irsdl)";
    private static final String LICENSE_LINE = "// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files";

    @Test
    void licenseAndNoticeFilesExist() {
        assertTrue(Files.exists(Path.of("LICENSE")), "LICENSE must exist in the project root");
        assertTrue(Files.exists(Path.of("NOTICE")), "NOTICE must exist in the project root");
    }

    @Test
    void noticeDeclaresSectionSevenTerms() throws IOException {
        String notice = Files.readString(Path.of("NOTICE"));
        assertTrue(notice.contains("Copyright (C) 2021-2026 Soroush Dalili"));
        assertTrue(notice.contains("section 7(b)"));
        assertTrue(notice.contains("Developed by Soroush Dalili (@irsdl)"));
    }

    @Test
    void everyJavaFileKeepsTheHeaderNotices() throws IOException {
        // vendored third-party files carry their own header, enforced by ThirdPartyHeadersTest
        Path vendoredRoot = Path.of("src", "main", "java", "ninja", "burpsuite", "libs", "thirdparty");
        for (Path root : List.of(Path.of("src", "main", "java"), Path.of("src", "test", "java"))) {
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.startsWith(vendoredRoot))
                        .forEach(p -> {
                    String head;
                    try {
                        head = String.join("\n", Files.readAllLines(p).stream().limit(10).toList());
                    } catch (IOException e) {
                        throw new AssertionError("Cannot read " + p, e);
                    }
                    assertTrue(head.contains(COPYRIGHT_LINE), "Missing copyright notice in " + p);
                    assertTrue(head.contains(ATTRIBUTION_LINE), "Missing attribution line in " + p);
                    assertTrue(head.contains(LICENSE_LINE), "Missing license line in " + p);
                });
            }
        }
    }
}
