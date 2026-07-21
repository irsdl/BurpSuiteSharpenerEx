// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

public interface PreferenceListener {
    void onPreferenceSet(Object source, String settingName, Object newValue);
}
