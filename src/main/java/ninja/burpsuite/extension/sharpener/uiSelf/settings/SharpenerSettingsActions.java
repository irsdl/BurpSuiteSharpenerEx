// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import ninja.burpsuite.extension.sharpener.*;
import ninja.burpsuite.extension.sharpener.capabilities.implementations.PwnFoxSettings;
import ninja.burpsuite.extension.sharpener.capabilities.implementations.ManualHighlighterSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsStyleHandler;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutsDialog;
import ninja.burpsuite.libs.burp.generic.*;
import ninja.burpsuite.libs.generic.*;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.PreferenceListener;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.Preferences;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;
import java.util.function.*;

/** Shared control definitions and effects. Existing settings classes still own registration/defaults. */
public final class SharpenerSettingsActions implements AutoCloseable {
    public static final String MANUAL_HIGHLIGHTER_PATH = "Global Settings/Supported Capabilities/Manual Highlighter (Proxy and Logger)/";
    public static final String MANUAL_HIGHLIGHTER_ENABLED = MANUAL_HIGHLIGHTER_PATH + "Enabled";
    public static final String MANUAL_HIGHLIGHTER_LIMIT = MANUAL_HIGHLIGHTER_PATH + "Limit Logger inspection to 64 KiB";
    public static final String BURP_ICON_FOLDER = "icons";
    public static final int BURP_ICON_MENU_WIDTH = 16;
    public enum Kind { ACTION, CHECK, RADIO }
    public record Control(String path, Kind kind, String description, BooleanSupplier selected,
                          Consumer<Boolean> setter, Icon icon) {
        public String parentPath() {
            // The original issue action has a literal slash in its caption.
            return path.startsWith("Report Bug/Feature") ? "" : path.substring(0, Math.max(0, path.lastIndexOf('/')));
        }
        public String label() { return parentPath().isEmpty() ? path : path.substring(parentPath().length() + 1); }
    }
    private final ExtensionSharedParameters p;
    public final SettingsDialogs dialogs;
    private final List<Runnable> listeners = new ArrayList<>();
    private final PreferenceListener preferenceListener = (source, key, value) -> changed();
    private List<ResourceIconCache.NamedIcon> icons = List.of();
    private Icon logo;
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicBoolean refreshQueued = new java.util.concurrent.atomic.AtomicBoolean();

    public SharpenerSettingsActions(ExtensionSharedParameters p) {
        this.p = p; dialogs = new SettingsDialogs(p);
        p.preferences.addSettingListener(preferenceListener);
        p.backgroundTasks.submit("settings images", () -> {
            var loaded = ResourceIconCache.getIcons(p.extensionClass, BURP_ICON_FOLDER, BURP_ICON_MENU_WIDTH);
            Image image = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(p.extensionClass, "/sharpener.png"), 24);
            Icon loadedLogo = image == null ? null : new ImageIcon(image);
            SwingUtilities.invokeLater(() -> {
                if (closed || p.isUnloaded()) return;
                icons = loaded; logo = loadedLogo; changed();
            });
        });
    }
    public void addListener(Runnable listener) { listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }
    private void changed() {
        if (closed || p.isUnloaded() || !refreshQueued.compareAndSet(false, true)) return;
        SwingUtilities.invokeLater(() -> {
            refreshQueued.set(false);
            if (!closed && !p.isUnloaded()) for (Runnable listener : List.copyOf(listeners)) listener.run();
        });
    }
    public void invoke(Control control, boolean selected) {
        if (closed || p.isUnloaded()) return;
        try { control.setter().accept(selected); }
        finally { changed(); }
    }
    private void global(String key, Object value) { p.preferences.safeSetSetting(key, value, Preferences.Visibility.GLOBAL); }
    private void project(String key, String value) { p.preferences.safeSetSetting(key, value, Preferences.Visibility.PROJECT); }
    private String string(String key) { return p.preferences.safeGetStringSetting(key); }
    public void setTool(BurpUITools.MainTabs tool, boolean enabled) {
        global("isUnique_" + tool, enabled);
        if (enabled) MainTabsStyleHandler.setMainTabsStyle_noUiLock(p, tool);
        else MainTabsStyleHandler.unsetMainTabsStyle_noUiLock(p, tool);
    }
    public void setUpdateCheck(boolean enabled) {
        boolean before = p.preferences.safeGetBooleanSetting("checkForUpdate");
        global("checkForUpdate", enabled);
        if (enabled && !before) ((ExtensionMainClass)p.burpExtender).checkForUpdate();
    }
    public void setTitle(String value) {
        JFrame frame = p.get_mainFrameUsingMontoya();
        if (value != null && !value.trim().isEmpty() && frame != null && !frame.getTitle().equals(value)) {
            BurpTitleAndIcon.setTitle_noUiLock(p, value); project("BurpTitle", value);
        }
    }
    public void setCustomTheme(String path) {
        if (path == null || path.isEmpty()) return;
        global("ToolsThemeName", "custom"); global("ToolsThemeCustomPath", path);
        MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(p);
    }
    public void setIcon(String path, boolean resource) {
        if (path == null || path.trim().isEmpty()) return;
        BurpTitleAndIcon.setIcon(p, path, resource);
        project("BurpResourceIconName", resource ? path : ""); project("BurpIconCustomPath", resource ? "" : path);
        // Preserve the existing menu's scope argument; the registered GLOBAL scope remains authoritative.
        if (!resource) p.preferences.safeSetSetting("LastBurpIconCustomPath", path, Preferences.Visibility.PROJECT);
    }
    public void showShortcuts() { if (!closed && !p.isUnloaded()) ShortcutsDialog.show(p); }
    private Control action(String path, Runnable run) { return new Control(path, Kind.ACTION, "", () -> false, v -> run.run(), null); }
    private Control check(String path, String key) { return new Control(path, Kind.CHECK, "", () -> p.preferences.safeGetBooleanSetting(key), v -> global(key, v), null); }
    private Control radio(String path, String key, String value, Runnable effect) {
        return new Control(path, Kind.RADIO, "", () -> string(key).equalsIgnoreCase(value), v -> { global(key, value); effect.run(); }, null);
    }
    public List<Control> controls() {
        List<Control> c = new ArrayList<>();
        String style = "Global Settings/Tools' Template And Style/";
        c.add(action(style + "Enable All", () -> { for (var tool : BurpUITools.MainTabs.values()) setTool(tool, true); }));
        c.add(action(style + "Disable All", () -> { for (var tool : BurpUITools.MainTabs.values()) setTool(tool, false); }));
        for (String theme : new String[]{"none", "halloween", "game", "hacker", "gradient", "mobster", "office", "@irsdl"})
            c.add(radio(style + "Icons' Theme/" + theme, "ToolsThemeName", theme.equals("none") ? "" : theme, () -> MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(p)));
        c.add(new Control(style + "Icons' Theme/custom directory", Kind.RADIO, "Select a directory of tool icons", () -> string("ToolsThemeName").equals("custom"),
                v -> setCustomTheme(dialogs.choose(string("ToolsThemeCustomPath"), true, null)), null));
        for (String size : new String[]{"48", "32", "24", "20", "16", "14", "12", "10", "8", "6"})
            c.add(radio(style + "Icons' Size/" + size, "ToolsIconSize", size, () -> MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(p)));
        for (var tool : BurpUITools.MainTabs.values()) {
            if (tool == BurpUITools.MainTabs.None || tool == BurpUITools.MainTabs.Extender || tool == BurpUITools.MainTabs.UserOptions || tool == BurpUITools.MainTabs.ProjectOptions) continue;
            c.add(new Control(style + tool.toRawString(), Kind.CHECK, "", () -> p.preferences.safeGetBooleanSetting("isUnique_" + tool), v -> setTool(tool, v), null));
        }
        c.add(new Control("Global Settings/Scrollable Tool Pane", Kind.CHECK, "", () -> p.preferences.safeGetBooleanSetting("isToolTabPaneScrollable"), v -> {
            global("isToolTabPaneScrollable", v);
            JTabbedPane pane = p.get_rootTabbedPaneUsingMontoya();
            if (pane != null) pane.setTabLayoutPolicy(v ? JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT);
        }, null));
        c.add(check("Global Settings/Use Last Screen Position And Size When Opened", "useLastScreenPositionAndSize"));
        c.add(check("Global Settings/Detect Off Screen Window Position", "detectOffScreenPosition"));
        for (var setting : p.allSettings.capabilitySettingsList) {
            if (setting instanceof ManualHighlighterSettings manual) {
                c.add(new Control(MANUAL_HIGHLIGHTER_ENABLED, Kind.CHECK, setting.capability.description, setting::isEnabled, setting::setEnabled, null));
                c.add(new Control(MANUAL_HIGHLIGHTER_LIMIT, Kind.CHECK,
                        "Inspect only the first 65,536 bytes of each HTTP message for Logger highlighting. Turn off to inspect complete messages. Proxy temporary-marker removal is unchanged.",
                        manual::isLoggerInspectionLimited, manual::setLoggerInspectionLimited, null));
            } else c.add(new Control("Global Settings/Supported Capabilities/" + setting.capability.name, Kind.CHECK, setting.capability.description, setting::isEnabled, setting::setEnabled, null));
            if (setting instanceof PwnFoxSettings fox)
                c.add(new Control("Global Settings/Supported Capabilities/PwnFox: Remove the color header", Kind.CHECK,
                        "Remove X-PwnFox-Color before sending; disable to keep it for other tools.", fox::isHeaderRemovalEnabled, fox::setHeaderRemovalEnabled, null));
        }
        for (var level : BurpExtensionSharedParameters.DebugLevels.values())
            c.add(new Control("Global Settings/Debug Settings/" + level.getName(), Kind.RADIO, "", () -> p.debugLevel == level.getValue(), v -> {
                global("debugLevel", level.getValue()); p.debugLevel = level.getValue();
            }, null));
        c.add(action("Project Settings/Change Burp Suite Title", () -> { JFrame f = p.get_mainFrameUsingMontoya(); if (f != null) setTitle(dialogs.titleInput(f.getTitle())); }));
        for (var icon : icons) {
            String path = "/icons/" + icon.fileName();
            c.add(new Control("Project Settings/Change Burp Suite Icon/" + icon.fileName().replaceAll("\\..*$", ""), Kind.RADIO, "", () -> string("BurpResourceIconName").equalsIgnoreCase(path) && string("BurpIconCustomPath").isBlank(), v -> setIcon(path, true), icon.icon()));
        }
        c.add(new Control("Project Settings/Change Burp Suite Icon/Custom", Kind.RADIO, "", () -> !string("BurpIconCustomPath").isBlank(),
                v -> setIcon(dialogs.choose(string("LastBurpIconCustomPath"), false, new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes())), false), null));
        c.add(action("Project Settings/Reset Burp Suite Title", () -> {
            if (dialogs.confirm("Sharpener Extension: Reset Title", "Are you sure?")) { BurpTitleAndIcon.resetTitle(p); project("BurpTitle", ""); }
        }));
        c.add(action("Project Settings/Reset Burp Suite Icon", () -> {
            if (dialogs.confirm("Sharpener Extension: Reset Icon", "Are you sure?")) { BurpTitleAndIcon.resetIcon(p); project("BurpIconCustomPath", ""); }
        }));
        c.add(action("Keyboard Shortcuts", this::showShortcuts));
        c.add(action("Unload Extension", () -> { if (dialogs.confirm("Sharpener Extension Unload", "Are you sure you want to unload the extension?")) p.montoyaApi.extension().unload(); }));
        c.add(action("Remove All Settings & Unload", () -> {
            if (dialogs.confirm("Sharpener Extension: Reset All Settings & Unload", "Are you sure you want to remove all the settings and unload the extension?")) { p.preferences.resetAll(); p.montoyaApi.extension().unload(); }
        }));
        c.add(new Control("Check for Update on Start", Kind.CHECK, "Opt in to accessing the GitHub repository; enabling also checks now.", () -> p.preferences.safeGetBooleanSetting("checkForUpdate"), this::setUpdateCheck, null));
        c.add(action("Project Page (Opens a Browser)", () -> browse(p.extensionURL)));
        c.add(action("Report Bug/Feature (Opens a Browser)", () -> browse(p.extensionIssueTracker)));
        c.add(new Control("About " + p.extensionName, Kind.ACTION, "About this extension", () -> false, v -> dialogs.option(
                "Burp Suite " + p.extensionName + " - version " + p.version + "\n" + p.extensionCopyrightMessage + "\nProject link: " + p.extensionURL,
                "About this extension", new String[]{"OK"}, 0), logo));
        return List.copyOf(c);
    }
    private void browse(String url) { p.backgroundTasks.submit("open browser", () -> Desktop.getDesktop().browse(new URI(url))); }
    @Override public void close() {
        if (closed) return;
        closed = true; p.preferences.removeSettingListener(preferenceListener); listeners.clear(); dialogs.close();
    }
}
