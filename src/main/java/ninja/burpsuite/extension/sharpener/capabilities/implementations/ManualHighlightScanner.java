// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.HighlightColor;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Literal ASCII marker matching without decoding or retaining message contents. */
final class ManualHighlightScanner {
    static final int CHUNK_SIZE = 8192;
    private static final byte[] TEMPORARY = "tempcolor".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PERMANENT = "permcolor".getBytes(StandardCharsets.US_ASCII);
    private final byte[][] names;
    private final HighlightColor[] colors;
    private final int longestMarker;

    ManualHighlightScanner(String supportedNames) {
        String[] supported = supportedNames.split("\\|");
        names = new byte[supported.length][];
        colors = new HighlightColor[supported.length];
        int longest = 0;
        for (int i = 0; i < supported.length; i++) {
            names[i] = supported[i].toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
            colors[i] = HighlightColor.valueOf(supported[i].toUpperCase(Locale.ROOT));
            longest = Math.max(longest, TEMPORARY.length + names[i].length);
        }
        longestMarker = longest;
    }

    /** end is exclusive and must be within message.length(); no reads extend beyond it. */
    HighlightColor scan(ByteArray message, int end, boolean request) {
        HighlightColor permanent = null;
        for (int offset = 0; offset < end;) {
            int positions = Math.min(CHUNK_SIZE, end - offset);
            // Bounded lookahead handles markers spanning chunks. Positions are visited once.
            int count = (int) Math.min((long) positions + longestMarker - 1, end - offset);
            byte[] chunk = message.subArray(offset, offset + count).getBytes();
            for (int i = 0; i < positions; i++) {
                int first = chunk[i] | 0x20;
                boolean temporary = request && first == 't' && matches(chunk, i, TEMPORARY);
                if (!temporary && !(permanent == null && first == 'p' && matches(chunk, i, PERMANENT))) continue;
                for (int color = 0; color < names.length; color++) {
                    if (!matches(chunk, i + TEMPORARY.length, names[color])) continue;
                    if (temporary || !request) return colors[color];
                    permanent = colors[color];
                    break;
                }
            }
            offset += positions;
        }
        return permanent;
    }

    private static boolean matches(byte[] bytes, int offset, byte[] literal) {
        if (offset + literal.length > bytes.length) return false;
        for (int i = 0; i < literal.length; i++) if ((bytes[offset + i] | 0x20) != literal[i]) return false;
        return true;
    }
}
