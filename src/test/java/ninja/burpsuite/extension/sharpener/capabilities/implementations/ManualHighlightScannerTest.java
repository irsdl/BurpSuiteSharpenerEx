// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class ManualHighlightScannerTest {
    static final String COLORS = "Red|Orange|Yellow|Green|Cyan|Blue|Pink|Magenta|Gray";
    final ManualHighlightScanner scanner = new ManualHighlightScanner(COLORS);

    /** A slice-aware ByteArray spy that rejects unbounded copies and out-of-budget reads. */
    static final class Bytes {
        final byte[] raw;
        final int allowedEnd;
        int maxRead, copied;
        Bytes(String text) { this(text.getBytes(StandardCharsets.ISO_8859_1), text.length()); }
        Bytes(byte[] raw, int allowedEnd) { this.raw = raw; this.allowedEnd = allowedEnd; }
        ByteArray array() { return slice(0, raw.length, true); }
        ByteArray slice(int start, int end, boolean root) {
            return (ByteArray) Proxy.newProxyInstance(ByteArray.class.getClassLoader(), new Class<?>[]{ByteArray.class}, (proxy, method, args) -> switch (method.getName()) {
                case "length" -> end - start;
                case "subArray" -> {
                    int from = start + (int) args[0], to = start + (int) args[1];
                    assertTrue(from >= start && to >= from && to <= end && to <= allowedEnd, "Read beyond inspection end");
                    maxRead = Math.max(maxRead, to);
                    yield slice(from, to, false);
                }
                case "getBytes" -> {
                    assertFalse(root, "Must not copy the complete message through getBytes()");
                    copied += end - start;
                    yield Arrays.copyOfRange(raw, start, end);
                }
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new AssertionError("Unexpected ByteArray operation: " + method.getName());
            });
        }
    }
    static ByteArray bytes(String text) { return new Bytes(text).array(); }
    HighlightColor scan(String text, boolean request) { return scanner.scan(bytes(text), text.length(), request); }

    @ParameterizedTest @ValueSource(ints = {65535, 65536, 65537})
    void exactEndAndCrossingMarkersRespectRawBudget(int length) {
        int end = Math.min(length, 65536);
        for (int displacement : new int[]{0, 1, 16}) {
            byte[] raw = new byte[length + 32]; Arrays.fill(raw, (byte)'x');
            byte[] marker = "tempcolormagenta".getBytes(StandardCharsets.US_ASCII);
            int start = end - marker.length + displacement;
            System.arraycopy(marker, 0, raw, start, marker.length);
            Bytes source = new Bytes(raw, end);
            assertEquals(displacement == 0 ? HighlightColor.MAGENTA : null, scanner.scan(source.array(), end, true));
            assertEquals(end, source.maxRead);
        }
    }
    @ParameterizedTest @ValueSource(ints = {1, 4, 8, 9, 12, 15})
    void markersSplitAcrossChunksAreRecognized(int beforeBoundary) {
        String input = "x".repeat(ManualHighlightScanner.CHUNK_SIZE - beforeBoundary) + "TeMpCoLoRmAgEnTa";
        assertEquals(HighlightColor.MAGENTA, scan(input, true));
    }
    @Test void whitelistCaseSubstringAndPrecedenceMatchExistingRegex() {
        for (String color : COLORS.split("\\|")) {
            HighlightColor expected = HighlightColor.valueOf(color.toUpperCase(Locale.ROOT));
            assertEquals(expected, scan("\u00ff\0TEMPCOLOR" + color.toUpperCase(Locale.ROOT) + "suffix\u0080", true));
            assertEquals(expected, scan("permcolor" + color + "suffix", false));
        }
        assertEquals(HighlightColor.RED, scan("permcolorblue-tempcolorred-tempcolorgreen", true));
        assertEquals(HighlightColor.RED, scan("tempcolorred-permcolorblue", true));
        assertEquals(HighlightColor.BLUE, scan("permcolorblue-permcolorred", true));
        assertNull(scan("tempcolorinvalid-permcolorblack-tempcolorred", false));
        assertNull(scan("tempcolorinvalid-permcolorgrey", true));
    }
    @Test void permanentInsideBudgetWinsWhenTemporaryIsOutsideAndUnlimitedRestoresPrecedence() {
        String message = "permcolorblue" + "x".repeat(65536) + "tempcolorred";
        Bytes limited = new Bytes(message.getBytes(StandardCharsets.ISO_8859_1), 65536);
        assertEquals(HighlightColor.BLUE, scanner.scan(limited.array(), 65536, true));
        assertEquals(HighlightColor.RED, scan(message, true));
        assertEquals(HighlightColor.BLUE, scan(message, false));
    }
    @Test void earlyTemporaryMatchStopsBeforeLaterChunksAndEmptyInputReadsNothing() {
        Bytes input = new Bytes("tempcolorred" + "x".repeat(1_000_000));
        assertEquals(HighlightColor.RED, scanner.scan(input.array(), input.raw.length, true));
        assertTrue(input.maxRead <= ManualHighlightScanner.CHUNK_SIZE + 15);
        Bytes empty = new Bytes(""); assertNull(scanner.scan(empty.array(), 0, true)); assertEquals(0, empty.copied);
    }
    @Test void randomizedAsciiMarkerParityWithLegacyRegexSurroundedByArbitraryBytes() {
        Pattern temp = Pattern.compile("tempcolor(" + COLORS + ")", Pattern.CASE_INSENSITIVE);
        Pattern perm = Pattern.compile("permcolor(" + COLORS + ")", Pattern.CASE_INSENSITIVE);
        Random random = new Random(78231);
        for (int sample = 0; sample < 400; sample++) {
            byte[] noise = new byte[random.nextInt(22000)]; random.nextBytes(noise);
            String input = new String(noise, StandardCharsets.ISO_8859_1);
            String[] markers = {"permcolorREDsuffix", "tempcolorBLUE", "TeMpCoLoRinvalid", "permcolormagenta", "tempcolorgrey"};
            for (int insert = 0; insert < 4; insert++) {
                int at = random.nextInt(input.length() + 1);
                input = input.substring(0, at) + markers[random.nextInt(markers.length)] + input.substring(at);
            }
            for (boolean request : List.of(false, true)) {
                var temporary = temp.matcher(input); var permanent = perm.matcher(input);
                String match = request && temporary.find() ? temporary.group(1) : permanent.find() ? permanent.group(1) : null;
                assertEquals(match == null ? null : HighlightColor.valueOf(match.toUpperCase(Locale.ROOT)), scan(input, request));
            }
        }
    }
}
