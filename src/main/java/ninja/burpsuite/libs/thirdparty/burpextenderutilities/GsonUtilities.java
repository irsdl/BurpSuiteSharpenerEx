// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class GsonUtilities{
  public static <T> T clone(T src, Type type, Gson gson){
    String jsonDefaultValue = gson.toJson(src);
    return gson.fromJson(jsonDefaultValue, type);
  }
}
