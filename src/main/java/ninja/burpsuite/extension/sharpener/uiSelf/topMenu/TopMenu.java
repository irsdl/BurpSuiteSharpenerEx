// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.topMenu;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionMainClass;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.implementations.PwnFoxSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsStyleHandler;
import ninja.burpsuite.extension.sharpener.uiControllers.shortcuts.ShortcutsDialog;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpTitleAndIcon;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.ImageHelper;
import ninja.burpsuite.libs.generic.ResourceIconCache;
import ninja.burpsuite.libs.generic.UIHelper;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;


public final class TopMenu extends javax.swing.JMenu {
    private static final long serialVersionUID = 1L;

    // the bundled Burp icons shown in the "Change Burp Suite Icon" menu, and their menu display width
    public static final String BURP_ICON_FOLDER = "icons";
    public static final int BURP_ICON_MENU_WIDTH = 16;

    private JMenu topMenuForExtension;
    private final transient ExtensionSharedParameters sharedParameters;
    private final String[] themeNames = {"none", "halloween", "game", "hacker", "gradient", "mobster", "office", "@irsdl"};
    private final String[] iconSizes = {"48", "32", "24", "20", "16", "14", "12", "10", "8", "6"};

    public TopMenu(ExtensionSharedParameters sharedParameters) {
        super(sharedParameters.extensionName);
        this.sharedParameters = sharedParameters;
        topMenuForExtension = this;
        updateTopMenuBar();
    }

    public void updateTopMenuBar() {
        SwingUtilities.invokeLater(() -> {
            removeAll();

            // Global menu
            JMenu globalMenu = new JMenu("Global Settings");

            // Style menu
            JMenu toolsUniqueStyleMenu = new JMenu("Tools' Template And Style");
            JMenuItem enableAll = new JMenuItem(new AbstractAction("Enable All") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (BurpUITools.MainTabs tool : BurpUITools.MainTabs.values()) {
                        sharedParameters.preferences.safeSetSetting("isUnique_" + tool, true, Preferences.Visibility.GLOBAL);
                        MainTabsStyleHandler.setMainTabsStyle_noUiLock(sharedParameters, tool);
                    }
                    updateTopMenuBar();
                }
            });
            toolsUniqueStyleMenu.add(enableAll);
            JMenuItem disableAll = new JMenuItem(new AbstractAction("Disable All") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (BurpUITools.MainTabs tool : BurpUITools.MainTabs.values()) {
                        sharedParameters.preferences.safeSetSetting("isUnique_" + tool, false, Preferences.Visibility.GLOBAL);
                        MainTabsStyleHandler.unsetMainTabsStyle_noUiLock(sharedParameters, tool);
                    }
                    updateTopMenuBar();
                }
            });
            toolsUniqueStyleMenu.add(disableAll);

            toolsUniqueStyleMenu.addSeparator();

            String themeName = sharedParameters.preferences.safeGetStringSetting("ToolsThemeName");
            JMenu toolsUniqueStyleThemeMenu = new JMenu("Icons' Theme");
            ButtonGroup themeGroup = new ButtonGroup();
            for (String definedThemeName : themeNames) {
                JRadioButtonMenuItem toolStyleTheme = new JRadioButtonMenuItem(definedThemeName);
                if (themeName.equalsIgnoreCase(definedThemeName) || (themeName.isEmpty() && definedThemeName.equalsIgnoreCase("none"))) {
                    toolStyleTheme.setSelected(true);
                }
                toolStyleTheme.addActionListener((e) -> {
                    String chosenOne = definedThemeName;
                    if (chosenOne.equalsIgnoreCase("none"))
                        chosenOne = "";
                    sharedParameters.preferences.safeSetSetting("ToolsThemeName", chosenOne, Preferences.Visibility.GLOBAL);
                    MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(sharedParameters);
                });
                themeGroup.add(toolStyleTheme);
                toolsUniqueStyleThemeMenu.add(toolStyleTheme);
            }

            JRadioButtonMenuItem toolStyleThemeCustom = new JRadioButtonMenuItem("custom directory");
            if (themeName.equalsIgnoreCase("custom")) {
                toolStyleThemeCustom.setSelected(true);
            }
            toolStyleThemeCustom.addActionListener((e) -> {
                String themeCustomPath = sharedParameters.preferences.safeGetStringSetting("ToolsThemeCustomPath");
                String customPath = UIHelper.showDirectoryDialog(themeCustomPath, sharedParameters.get_mainFrameUsingMontoya());
                if (!customPath.isEmpty()) {
                    sharedParameters.preferences.safeSetSetting("ToolsThemeName", "custom", Preferences.Visibility.GLOBAL);
                    sharedParameters.preferences.safeSetSetting("ToolsThemeCustomPath", customPath, Preferences.Visibility.GLOBAL);
                } else {
                    updateTopMenuBar();
                }
                MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(sharedParameters);
            });
            themeGroup.add(toolStyleThemeCustom);
            toolsUniqueStyleThemeMenu.add(toolStyleThemeCustom);
            toolsUniqueStyleMenu.add(toolsUniqueStyleThemeMenu);

            String iconSize = sharedParameters.preferences.safeGetStringSetting("ToolsIconSize");
            JMenu toolsUniqueStyleIconSizeMenu = new JMenu("Icons' Size");
            ButtonGroup iconSizeGroup = new ButtonGroup();
            for (String definedIconSize : iconSizes) {
                JRadioButtonMenuItem toolIconSize = new JRadioButtonMenuItem(definedIconSize);
                if (iconSize.equals(definedIconSize)) {
                    toolIconSize.setSelected(true);
                }
                toolIconSize.addActionListener((e) -> {
                    String chosenOne = definedIconSize;
                    sharedParameters.preferences.safeSetSetting("ToolsIconSize", chosenOne, Preferences.Visibility.GLOBAL);
                    MainTabsStyleHandler.resetMainTabsStylesFromSettings_noUiLock(sharedParameters);
                });
                iconSizeGroup.add(toolIconSize);
                toolsUniqueStyleIconSizeMenu.add(toolIconSize);
            }
            toolsUniqueStyleMenu.add(toolsUniqueStyleIconSizeMenu);

            toolsUniqueStyleMenu.addSeparator();

            for (BurpUITools.MainTabs tool : BurpUITools.MainTabs.values()) {
                if (tool.toString().equalsIgnoreCase("none"))
                    continue;

                // these tabs do not exist in supported Burp versions (2024.2 and later)
                if (tool.equals(BurpUITools.MainTabs.Extender) ||
                        tool.equals(BurpUITools.MainTabs.UserOptions) ||
                        tool.equals(BurpUITools.MainTabs.ProjectOptions)) {
                    continue;
                }

                JCheckBoxMenuItem toolStyleOption = new JCheckBoxMenuItem(tool.toRawString());
                if (sharedParameters.preferences.safeGetBooleanSetting("isUnique_" + tool)) {
                    toolStyleOption.setSelected(true);
                }
                toolStyleOption.addActionListener((e) -> {
                    Boolean currentSetting = sharedParameters.preferences.safeGetBooleanSetting("isUnique_" + tool);
                    if (currentSetting) {
                        sharedParameters.preferences.safeSetSetting("isUnique_" + tool, false, Preferences.Visibility.GLOBAL);
                        MainTabsStyleHandler.unsetMainTabsStyle_noUiLock(sharedParameters, tool);
                    } else {
                        sharedParameters.preferences.safeSetSetting("isUnique_" + tool, true, Preferences.Visibility.GLOBAL);
                        MainTabsStyleHandler.setMainTabsStyle_noUiLock(sharedParameters, tool);
                    }
                });
                toolsUniqueStyleMenu.add(toolStyleOption);
            }
            globalMenu.add(toolsUniqueStyleMenu);

            JCheckBoxMenuItem topMenuScrollableLayout = new JCheckBoxMenuItem("Scrollable Tool Pane");

            if (sharedParameters.preferences.safeGetBooleanSetting("isToolTabPaneScrollable")) {
                topMenuScrollableLayout.setSelected(true);
            }

            topMenuScrollableLayout.addActionListener((e) -> {
                boolean isToolTabPaneScrollable = sharedParameters.preferences.safeGetBooleanSetting("isToolTabPaneScrollable");
                if (isToolTabPaneScrollable) {
                    SwingUtilities.invokeLater(() -> sharedParameters.get_rootTabbedPaneUsingMontoya().setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT));
                    sharedParameters.preferences.safeSetSetting("isToolTabPaneScrollable", false, Preferences.Visibility.GLOBAL);
                } else {
                    SwingUtilities.invokeLater(() -> sharedParameters.get_rootTabbedPaneUsingMontoya().setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT));
                    sharedParameters.preferences.safeSetSetting("isToolTabPaneScrollable", true, Preferences.Visibility.GLOBAL);
                }
            });

            globalMenu.add(topMenuScrollableLayout);


            JCheckBoxMenuItem useLastScreenPositionAndSizeChkBox = new JCheckBoxMenuItem("Use Last Screen Position And Size When Opened");

            if (sharedParameters.preferences.safeGetBooleanSetting("useLastScreenPositionAndSize")) {
                useLastScreenPositionAndSizeChkBox.setSelected(true);
            }

            useLastScreenPositionAndSizeChkBox.addActionListener((e) -> {
                boolean useLastScreenPositionAndSize = sharedParameters.preferences.safeGetBooleanSetting("useLastScreenPositionAndSize");
                if (useLastScreenPositionAndSize) {
                    sharedParameters.preferences.safeSetSetting("useLastScreenPositionAndSize", false, Preferences.Visibility.GLOBAL);
                } else {
                    sharedParameters.preferences.safeSetSetting("useLastScreenPositionAndSize", true, Preferences.Visibility.GLOBAL);
                }
            });

            globalMenu.add(useLastScreenPositionAndSizeChkBox);

            JCheckBoxMenuItem detectOffScreenChkBox = new JCheckBoxMenuItem("Detect Off Screen Window Position");

            if (sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition")) {
                detectOffScreenChkBox.setSelected(true);
            }

            detectOffScreenChkBox.addActionListener((e) -> {
                boolean useLastScreenPositionAndSize = sharedParameters.preferences.safeGetBooleanSetting("detectOffScreenPosition");
                if (useLastScreenPositionAndSize) {
                    sharedParameters.preferences.safeSetSetting("detectOffScreenPosition", false, Preferences.Visibility.GLOBAL);
                } else {
                    sharedParameters.preferences.safeSetSetting("detectOffScreenPosition", true, Preferences.Visibility.GLOBAL);
                }
            });

            globalMenu.add(detectOffScreenChkBox);


            JMenu supportedCapabilitiesMenu = new JMenu("Supported Capabilities");
            // iterate through all capabilities and add them to the menu
            for (var capabilitySetting : sharedParameters.allSettings.capabilitySettingsList) {
                var capability = capabilitySetting.capability;
                JCheckBoxMenuItem capabilityOption = new JCheckBoxMenuItem(capability.name);
                capabilityOption.setToolTipText(capability.description);
                if (capabilitySetting.isEnabled()) {
                    capabilityOption.setSelected(true);
                }
                capabilityOption.addActionListener((e) -> {
                    if (capabilitySetting.isEnabled()) {
                        capabilitySetting.setEnabled(false);
                    } else {
                        capabilitySetting.setEnabled(true);
                    }
                });
                supportedCapabilitiesMenu.add(capabilityOption);

                // PwnFox sub-option: remove or keep the X-PwnFox-Color header (issue #24)
                if (capabilitySetting instanceof PwnFoxSettings) {
                    PwnFoxSettings pwnFoxSettings = (PwnFoxSettings) capabilitySetting;
                    JCheckBoxMenuItem removeHeaderOption = new JCheckBoxMenuItem("PwnFox: Remove the color header");
                    removeHeaderOption.setToolTipText("Removes the X-PwnFox-Color header before the request is sent. Untick it to keep the header for other extensions or tools.");
                    removeHeaderOption.setSelected(pwnFoxSettings.isHeaderRemovalEnabled());
                    removeHeaderOption.addActionListener((e) -> pwnFoxSettings.setHeaderRemovalEnabled(!pwnFoxSettings.isHeaderRemovalEnabled()));
                    supportedCapabilitiesMenu.add(removeHeaderOption);
                }
            }

            globalMenu.add(supportedCapabilitiesMenu);

            // Debug options
            JMenu debugMenu = new JMenu("Debug Settings");
            ButtonGroup debugButtonGroup = new ButtonGroup();

            for (var item : BurpExtensionSharedParameters.DebugLevels.values()) {
                JRadioButtonMenuItem debugOption = new JRadioButtonMenuItem(new AbstractAction(item.getName()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sharedParameters.preferences.safeSetSetting("debugLevel", item.getValue(), Preferences.Visibility.GLOBAL);
                        sharedParameters.debugLevel = item.getValue();
                    }
                });
                if (sharedParameters.debugLevel == item.getValue())
                    debugOption.setSelected(true);

                debugButtonGroup.add(debugOption);
                debugMenu.add(debugOption);
            }

            globalMenu.add(debugMenu);

            add(globalMenu);

            // Project menu
            JMenu projectMenu = new JMenu("Project Settings");

            // Change title button
            JMenuItem changeTitle = new JMenuItem(new AbstractAction("Change Burp Suite Title") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String newTitle = UIHelper.showPlainInputMessage("Change Burp Suite Title String To:", "Change Burp Suite Title", sharedParameters.get_mainFrameUsingMontoya().getTitle(), sharedParameters.get_mainFrameUsingMontoya());
                    if (newTitle != null && !newTitle.trim().isEmpty()) {
                        if (!sharedParameters.get_mainFrameUsingMontoya().getTitle().equals(newTitle)) {
                            BurpTitleAndIcon.setTitle_noUiLock(sharedParameters, newTitle);
                            sharedParameters.preferences.safeSetSetting("BurpTitle", newTitle, Preferences.Visibility.PROJECT);
                        }
                    }
                }
            });
            projectMenu.add(changeTitle);

            // Change title button
            String burpResourceIconName = sharedParameters.preferences.safeGetStringSetting("BurpResourceIconName");
            // the icons are loaded once and cached, so rebuilding this menu stays fast
            java.util.List<ResourceIconCache.NamedIcon> resourceIcons =
                    ResourceIconCache.getIcons(sharedParameters.extensionClass, BURP_ICON_FOLDER, BURP_ICON_MENU_WIDTH);
            if (resourceIcons.isEmpty()) {
                sharedParameters.printDebugMessage("No icon was found in resources");
            }

            JMenu changeBurpIcon = new JMenu("Change Burp Suite Icon");

            ButtonGroup burpIconGroup = new ButtonGroup();
            for (ResourceIconCache.NamedIcon resourceIcon : resourceIcons) {
                String resourcePath = "/icons/" + resourceIcon.fileName();
                JRadioButtonMenuItem burpIconImage = new JRadioButtonMenuItem(resourceIcon.fileName().replaceAll("\\..*$", ""));
                burpIconImage.setIcon(resourceIcon.icon());
                if (resourceIcon.fileName().equalsIgnoreCase(burpResourceIconName)) {
                    burpIconImage.setSelected(true);
                }
                burpIconImage.addActionListener((e) -> {
                    BurpTitleAndIcon.setIcon(sharedParameters, resourcePath, true);
                    sharedParameters.preferences.safeSetSetting("BurpResourceIconName", resourcePath, Preferences.Visibility.PROJECT);
                    sharedParameters.preferences.safeSetSetting("BurpIconCustomPath", "", Preferences.Visibility.PROJECT);
                });
                burpIconGroup.add(burpIconImage);
                changeBurpIcon.add(burpIconImage);
            }

            JRadioButtonMenuItem burpIconImage = new JRadioButtonMenuItem("Custom");
            if (!sharedParameters.preferences.safeGetStringSetting("BurpIconCustomPath").isBlank()) {
                burpIconImage.setSelected(true);
            }

            burpIconImage.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String lastIconPath = sharedParameters.preferences.safeGetStringSetting("LastBurpIconCustomPath");
                    FileFilter imageFilter = new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes());
                    String newIconPath = UIHelper.showFileDialog(lastIconPath, imageFilter, sharedParameters.get_mainFrameUsingMontoya());
                    if (newIconPath != null && !newIconPath.trim().isEmpty()) {
                        BurpTitleAndIcon.setIcon(sharedParameters, newIconPath, false);
                        sharedParameters.preferences.safeSetSetting("BurpResourceIconName", "", Preferences.Visibility.PROJECT);
                        sharedParameters.preferences.safeSetSetting("BurpIconCustomPath", newIconPath, Preferences.Visibility.PROJECT);
                        sharedParameters.preferences.safeSetSetting("LastBurpIconCustomPath", newIconPath, Preferences.Visibility.PROJECT);
                    }
                }
            });
            burpIconGroup.add(burpIconImage);
            changeBurpIcon.add(burpIconImage);

            projectMenu.add(changeBurpIcon);


            // Reset title button
            JMenuItem resetTitle = new JMenuItem(new AbstractAction("Reset Burp Suite Title") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    int response = UIHelper.askConfirmMessage("Sharpener Extension: Reset Title", "Are you sure?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                    if (response == 0) {
                        BurpTitleAndIcon.resetTitle(sharedParameters);
                        sharedParameters.preferences.safeSetSetting("BurpTitle", "", Preferences.Visibility.PROJECT);
                    }
                }
            });
            projectMenu.add(resetTitle);

            // Reset icon button
            JMenuItem resetIcon = new JMenuItem(new AbstractAction("Reset Burp Suite Icon") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    int response = UIHelper.askConfirmMessage("Sharpener Extension: Reset Icon", "Are you sure?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                    if (response == 0) {
                        BurpTitleAndIcon.resetIcon(sharedParameters);
                        sharedParameters.preferences.safeSetSetting("BurpIconCustomPath", "", Preferences.Visibility.PROJECT);
                    }
                }
            });
            projectMenu.add(resetIcon);
            add(projectMenu);

            // Keyboard shortcuts viewer and editor
            JMenuItem keyboardShortcuts = new JMenuItem(new AbstractAction("Keyboard Shortcuts") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    ShortcutsDialog.show(sharedParameters);
                }
            });
            keyboardShortcuts.setToolTipText("Shows all Sharpener shortcuts and allows changing them");
            add(keyboardShortcuts);

            addSeparator();

            JMenuItem unloadExtension = new JMenuItem(new AbstractAction("Unload Extension") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        int response = UIHelper.askConfirmMessage("Sharpener Extension Unload", "Are you sure you want to unload the extension?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                        if (response == 0) {
                            sharedParameters.montoyaApi.extension().unload();
                        }
                    } catch (NoClassDefFoundError | Exception e) {
                        // It seems the extension is dead and we are left with a top menu bar
                    }

                    try {
                        // a private one-shot daemon timer, on purpose not the shared runner:
                        // this cleanup must still fire after unload has stopped the shared timer
                        new Timer("SharpenerTopMenuCleanup", true).schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        SwingUtilities.invokeLater(() -> {
                                            // This is useful when the extension has been unloaded but the menu is still there because of an error
                                            // We should force removing the top menu bar from Burp using all native libraries
                                            JRootPane rootPane = topMenuForExtension.getRootPane();
                                            if (rootPane != null) {
                                                JTabbedPane rootTabbedPane = (JTabbedPane) rootPane.getContentPane().getComponent(0);
                                                JFrame mainFrame = (JFrame) rootTabbedPane.getRootPane().getParent();
                                                JMenuBar mainMenuBar = mainFrame.getJMenuBar();
                                                mainMenuBar.remove(topMenuForExtension);
                                                mainFrame.validate();
                                                mainFrame.repaint();
                                            }
                                        });
                                    }
                                },
                                5000 // 5 seconds-delay to ensure all has been settled!
                        );
                    } catch (Exception e) {

                    }
                }
            });
            add(unloadExtension);

            JMenuItem resetAllSettings = new JMenuItem(new AbstractAction("Remove All Settings & Unload") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    int response = UIHelper.askConfirmMessage("Sharpener Extension: Reset All Settings & Unload", "Are you sure you want to remove all the settings and unload the extension?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                    if (response == 0) {

                        // A bug in resetting settings in BurpExtenderUtilities should be fixed so we will give it another chance instead of using a workaround
                        // sharedParameters.resetAllSettings();
                        sharedParameters.preferences.resetAll();
                        sharedParameters.montoyaApi.extension().unload();
                    }
                }
            });

            add(resetAllSettings);
            addSeparator();

            JCheckBoxMenuItem checkForUpdateOption = new JCheckBoxMenuItem("Check for Update on Start");
            checkForUpdateOption.setToolTipText("Check is done by accessing its GitHub repository");
            if (sharedParameters.preferences.safeGetBooleanSetting("checkForUpdate")) {
                checkForUpdateOption.setSelected(true);
            }

            checkForUpdateOption.addActionListener((e) -> {
                if (sharedParameters.preferences.safeGetBooleanSetting("checkForUpdate")) {
                    sharedParameters.preferences.safeSetSetting("checkForUpdate", false, Preferences.Visibility.GLOBAL);
                } else {
                    sharedParameters.preferences.safeSetSetting("checkForUpdate", true, Preferences.Visibility.GLOBAL);
                    ExtensionMainClass sharpenerBurpExtension = (ExtensionMainClass) sharedParameters.burpExtender;
                    sharpenerBurpExtension.checkForUpdate();
                }
            });
            add(checkForUpdateOption);

            JMenuItem showProjectPage = new JMenuItem(new AbstractAction("Project Page (Opens a Browser)") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    new Thread(() -> {
                        try {
                            Desktop.getDesktop().browse(new URI(sharedParameters.extensionURL));
                        } catch (Exception e) {
                        }
                    }).start();
                }
            });
            showProjectPage.setToolTipText("Will be opened in the default browser");
            add(showProjectPage);

            JMenuItem reportAnIssue = new JMenuItem(new AbstractAction("Report Bug/Feature (Opens a Browser)") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    new Thread(() -> {
                        try {
                            Desktop.getDesktop().browse(new URI(sharedParameters.extensionIssueTracker));
                        } catch (Exception e) {
                        }
                    }).start();
                }
            });
            reportAnIssue.setToolTipText("Will be opened in the default browser");
            add(reportAnIssue);

            addSeparator();

            // the square extension icon, scaled down so the menu item is not distorted
            Image logoImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "/sharpener.png"), 24);
            ImageIcon logoIcon = new ImageIcon(logoImg);
            JMenuItem logoMenu = new JMenuItem("About " + sharedParameters.extensionName, logoIcon);
            logoMenu.setToolTipText("About this extension");
            logoMenu.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String aboutMessage = "Burp Suite " + sharedParameters.extensionName + " - version " + sharedParameters.version +
                            "\n" + sharedParameters.extensionCopyrightMessage + "\n" +
                            "Project link: " + sharedParameters.extensionURL;
                    UIHelper.showMessage(aboutMessage, "About this extension", sharedParameters.get_mainFrameUsingMontoya());
                }
            });
            add(logoMenu);

            // fixing the spacing when an icon is used - this used to work fine with old Java
            for (var item : getMenuComponents()) {
                if (item instanceof JMenuItem) {
                    if (((JMenuItem) item).getIcon() == null) {
                        ((JMenuItem) item).setHorizontalTextPosition(SwingConstants.LEFT);
                    }
                }
            }
        });
    }

    public static void removeTopMenuBarLastResort(ExtensionSharedParameters sharedParameters, boolean repaintUI) {
        if (BurpUITools.isMenuBarLoaded(sharedParameters.extensionName, sharedParameters.get_mainMenuBarUsingMontoya())) {
            // so the menu is there!
            try {
                sharedParameters.printDebugMessage("removing the menu bar the dirty way!");
                BurpUITools.removeMenuBarByName_noUiLock(sharedParameters.extensionName, sharedParameters.get_mainMenuBarUsingMontoya(), repaintUI);
            } catch (Exception e) {
                sharedParameters.printlnError("Error in removing the top menu the dirty way: " + e.getMessage());
            }
        }
    }
}
