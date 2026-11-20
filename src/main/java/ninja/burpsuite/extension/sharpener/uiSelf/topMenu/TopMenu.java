// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.topMenu;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.uiSelf.settings.*;
import javax.swing.*;

/** Original menu paths, backed by exactly the same actions as the native panel. */
public final class TopMenu extends JMenu implements AutoCloseable {
    private static final long serialVersionUID = 1L;
    public static final String BURP_ICON_FOLDER = SharpenerSettingsActions.BURP_ICON_FOLDER;
    public static final int BURP_ICON_MENU_WIDTH = SharpenerSettingsActions.BURP_ICON_MENU_WIDTH;
    private final SettingsControls controls;
    public TopMenu(ExtensionSharedParameters p) {
        super(p.extensionName);
        controls = new SettingsControls(p.settingsActions(), this);
    }
    public void updateTopMenuBar() { controls.refresh(); }
    @Override public void close() { controls.close(); }
}
