// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ReviewedGradleTest {
    @TempDir Path temp;
    static final String PREFIX = "gradle-9.9.1/";

    String sha(String value) throws IOException {
        return ReviewedGradle.hash(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
    }

    Path archive(String filename, Map<String, String> entries) throws IOException {
        Path file = temp.resolve(filename);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (var item : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(item.getKey()));
                zip.write(item.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

    Path distribution(String name) throws IOException {
        return archive(name, Map.of(ReviewedGradle.ALIAS, "old",
                PREFIX + "LICENSE", "original licence", PREFIX + "bin/gradle", "launcher"));
    }

    Path replacement() throws IOException { return Files.writeString(temp.resolve("fixed.jar"), "new"); }

    @Test void verifiedDownloadPublishesExactBytes() throws Exception {
        Path target = temp.resolve("download.jar");
        ReviewedGradle.copyVerified(new ByteArrayInputStream("new".getBytes(StandardCharsets.UTF_8)), target, sha("new"), 3);
        assertEquals("new", Files.readString(target));
    }

    @Test void wrongChecksumNeverPublishesOrRetainsPartialDownload() throws Exception {
        Path target = temp.resolve("download.jar");
        assertThrows(IOException.class, () -> ReviewedGradle.copyVerified(new ByteArrayInputStream(new byte[]{1}), target, sha("new"), 3));
        try (var children = Files.list(temp)) { assertEquals(0, children.count()); }
    }

    @Test void oversizedDownloadNeverPublishes() throws Exception {
        Path target = temp.resolve("download.jar");
        assertThrows(IOException.class, () -> ReviewedGradle.copyVerified(new ByteArrayInputStream(new byte[4]), target, sha("new"), 3));
        assertFalse(Files.exists(target));
        try (var children = Files.list(temp)) { assertEquals(0, children.count()); }
    }

    @Test void existingInputIsNeverOverwritten() throws Exception {
        Path target = Files.writeString(temp.resolve("download.jar"), "keep");
        assertThrows(IOException.class, () -> ReviewedGradle.copyVerified(new ByteArrayInputStream(new byte[0]), target, sha(""), 3));
        assertEquals("keep", Files.readString(target));
    }

    @Test void missingOfflineInputFailsWithoutCreatingCacheFile() {
        Path target = temp.resolve("absent.jar");
        IOException failure = assertThrows(IOException.class, () -> ReviewedGradle.acquireInput(target,
                "https://repo.maven.apache.org/maven2/absent", "0".repeat(64), false));
        assertTrue(failure.getMessage().contains("--sharpener-acquire-toolchain"));
        assertFalse(Files.exists(target));
    }

    @Test void damagedCachedInputIsRejectedEvenDuringAcquisition() throws Exception {
        Path target = Files.writeString(temp.resolve("cached.jar"), "damaged");
        assertThrows(IOException.class, () -> ReviewedGradle.acquireInput(target, "https://invalid.example/", sha("new"), true));
        assertEquals("damaged", Files.readString(target));
    }

    @Test void redirectPolicyRestrictsVendorDeliveryChain() {
        URI origin = URI.create("https://services.gradle.org/distributions/gradle-9.9.1-bin.zip");
        URI github = URI.create("https://github.com/gradle/gradle-distributions/releases/download/v9.9.1/gradle-9.9.1-bin.zip");
        assertTrue(ReviewedGradle.allowedGradleRedirect(origin, github));
        assertTrue(ReviewedGradle.allowedGradleRedirect(github, URI.create("https://release-assets.githubusercontent.com/official-delivery")));
        for (String value : List.of("http://github.com/gradle/gradle-distributions/releases/download/v9.9.1/gradle-9.9.1-bin.zip",
                "https://evil.example/gradle.zip", "https://github.com/other/repo/gradle-9.9.1-bin.zip",
                "https://user@github.com/file", "https://github.com:444/file"))
            assertFalse(ReviewedGradle.allowedGradleRedirect(origin, URI.create(value)));
        assertFalse(ReviewedGradle.allowedGradleRedirect(URI.create("https://repo.maven.apache.org/maven2/guava.jar"), github));
    }

    @Test void unsafeArchivePathsAreRejectedBeforeExtraction() throws Exception {
        int i = 0;
        for (String name : List.of("../outside", "/absolute", PREFIX + "../outside", "C:/outside",
                PREFIX + "lib\\outside", PREFIX + "./outside", PREFIX + "lib//outside", PREFIX + "line\nname")) {
            try (ZipFile zip = new ZipFile(archive("bad" + i++ + ".zip", Map.of(name, "bad")).toFile())) {
                assertThrows(IOException.class, () -> ReviewedGradle.entries(zip), name);
            }
        }
        assertFalse(Files.exists(temp.resolve("outside")));
    }

    @Test void archiveCaseCollisionsFailOnEveryPlatform() throws Exception {
        try (ZipFile zip = new ZipFile(archive("case.zip", Map.of(PREFIX + "lib/A.jar", "a", PREFIX + "lib/a.jar", "b")).toFile())) {
            assertThrows(IOException.class, () -> ReviewedGradle.entries(zip));
        }
    }

    @Test void archiveEntryCountIsBounded() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i <= ReviewedGradle.MAX_ENTRIES; i++) entries.put(PREFIX + "f" + i, "");
        try (ZipFile zip = new ZipFile(archive("excess.zip", entries).toFile())) {
            assertThrows(IOException.class, () -> ReviewedGradle.entries(zip));
        }
    }

    @Test void compositionChangesOnlyGuavaAndPreservesNotices() throws Exception {
        Path original = distribution("base.zip"), fixed = replacement(), output = Files.createDirectory(temp.resolve("output"));
        ReviewedGradle.assemble(original, fixed, output, sha("old"));
        assertEquals("new", Files.readString(output.resolve(ReviewedGradle.ALIAS)));
        assertEquals("original licence", Files.readString(output.resolve(PREFIX + "LICENSE")));
        String digest = ReviewedGradle.validateInstallation(original, fixed, output);
        assertEquals(64, digest.length());
        assertEquals(digest, ReviewedGradle.validateInstallation(original, fixed, output));
    }

    @Test void unexpectedOriginalGuavaCannotBePatched() throws Exception {
        Path output = Files.createDirectory(temp.resolve("output"));
        assertThrows(IOException.class, () -> ReviewedGradle.assemble(distribution("base.zip"), replacement(), output, sha("unreviewed")));
        assertEquals("old", Files.readString(output.resolve(ReviewedGradle.ALIAS)));
    }

    @Test void changedInstallationBytesAreRejected() throws Exception {
        Path original = distribution("base.zip"), fixed = replacement(), output = Files.createDirectory(temp.resolve("output"));
        ReviewedGradle.assemble(original, fixed, output, sha("old"));
        Files.writeString(output.resolve(PREFIX + "bin/gradle"), "tampered launcher");
        assertThrows(IOException.class, () -> ReviewedGradle.validateInstallation(original, fixed, output));
    }

    @Test void unexpectedInitializationScriptIsRejected() throws Exception {
        Path original = distribution("base.zip"), fixed = replacement(), output = Files.createDirectory(temp.resolve("output"));
        ReviewedGradle.assemble(original, fixed, output, sha("old"));
        Path script = output.resolve(PREFIX + "init.d/unreviewed.gradle");
        Files.createDirectories(script.getParent()); Files.writeString(script, "unreviewed code");
        assertThrows(IOException.class, () -> ReviewedGradle.validateInstallation(original, fixed, output));
    }

    @Test void wrongGuavaMetadataIsRejected() throws Exception {
        Path wrong = archive("wrong-guava.jar", Map.of("META-INF/maven/com.google.guava/guava/pom.properties",
                "groupId=com.google.guava\nartifactId=guava\nversion=33.4.6-jre\n"));
        assertThrows(IOException.class, () -> ReviewedGradle.verifyGuava(wrong));
        Path correct = archive("correct-guava.jar", Map.of("META-INF/maven/com.google.guava/guava/pom.properties",
                "groupId=com.google.guava\nartifactId=guava\nversion=33.8.0-jre\n"));
        assertDoesNotThrow(() -> ReviewedGradle.verifyGuava(correct));
    }

    @Test void canonicalPinsRejectLookalikesMovingVersionsAndMissingHashes() throws Exception {
        Properties pin = ReviewedGradle.readProperties(Path.of("gradle/security-toolchain.properties"));
        Properties wrapper = ReviewedGradle.readProperties(Path.of("gradle/wrapper/gradle-wrapper.properties"));
        assertDoesNotThrow(() -> ReviewedGradle.validatePins(pin, wrapper));
        for (var change : Map.of("guava.version", "latest", "guava.jar.url", "https://lookalike.example/guava.jar",
                "guava.pom.sha256", "", "wrapper.sha256", "not-a-hash").entrySet()) {
            Properties altered = new Properties(); altered.putAll(pin); altered.setProperty(change.getKey(), change.getValue());
            assertThrows(IOException.class, () -> ReviewedGradle.validatePins(altered, wrapper));
        }
        wrapper.setProperty("distributionUrl", "https://lookalike.example/gradle-9.9.1-bin.zip");
        assertThrows(IOException.class, () -> ReviewedGradle.validatePins(pin, wrapper));
    }

    @Test void unsafeCacheAncestorIsNotReplaced() throws Exception {
        Files.writeString(temp.resolve(".gradle"), "keep");
        assertThrows(IOException.class, () -> ReviewedGradle.privateCache(temp));
        assertEquals("keep", Files.readString(temp.resolve(".gradle")));
    }
}
