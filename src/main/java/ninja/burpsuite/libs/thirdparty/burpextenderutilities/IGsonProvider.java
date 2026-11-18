// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import com.google.gson.Gson;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;

public interface IGsonProvider {
    Gson getGson();

    /**
     * Register a type adapter for the given class.
     * This defines how to de/serialize an object.
     * Required if storing custom types as preferenceComponentMap.
     * @param type
     * @param typeAdapter
     */
    void registerTypeAdapter(Type type, Object typeAdapter);

    void registerTypeHierarchyAdapter(Class<?> clazz, Object adapter);

    void registerTypeAdapterFactory(TypeAdapterFactory factory);
}
