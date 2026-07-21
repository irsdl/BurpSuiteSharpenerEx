// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities.typeadapter;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerTypeAdapter
        implements JsonSerializer<AtomicInteger>, JsonDeserializer<AtomicInteger> {
    @Override public JsonElement serialize(AtomicInteger src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.incrementAndGet());
    }

    @Override public AtomicInteger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        int intValue = json.getAsInt();
        return new AtomicInteger(--intValue);
    }
}
