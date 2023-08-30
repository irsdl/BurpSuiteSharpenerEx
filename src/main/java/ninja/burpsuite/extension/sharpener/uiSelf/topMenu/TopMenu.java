// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiSelf.topMenu;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionMainClass;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsStyleHandler;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpTitleAndIcon;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.ImageHelper;
import ninja.burpsuite.libs.generic.UIHelper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;


public class TopMenu extends javax.swing.JMenu {
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
                        MainTabsStyleHandler.setMainTabsStyle(sharedParameters, tool);
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
                        MainTabsStyleHandler.unsetMainTabsStyle(sharedParameters, tool);
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
                    MainTabsStyleHandler.resetMainTabsStylesFromSettings(sharedParameters);
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
                MainTabsStyleHandler.resetMainTabsStylesFromSettings(sharedParameters);
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
                    MainTabsStyleHandler.resetMainTabsStylesFromSettings(sharedParameters);
                });
                iconSizeGroup.add(toolIconSize);
                toolsUniqueStyleIconSizeMenu.add(toolIconSize);
            }
            toolsUniqueStyleMenu.add(toolsUniqueStyleIconSizeMenu);

            toolsUniqueStyleMenu.addSeparator();

            for (BurpUITools.MainTabs tool : BurpUITools.MainTabs.values()) {
                if (tool.toString().equalsIgnoreCase("none"))
                    continue;

                if(sharedParameters.burpMajorVersion >= 2023 && (
                        tool.equals(BurpUITools.MainTabs.Extender) ||
                                tool.equals(BurpUITools.MainTabs.UserOptions) ||
                                tool.equals(BurpUITools.MainTabs.ProjectOptions)
                )){
                    continue;
                }else if(sharedParameters.burpMajorVersion < 2023 && (
                        tool.equals(BurpUITools.MainTabs.Extensions)
                )){
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
                        MainTabsStyleHandler.unsetMainTabsStyle(sharedParameters, tool);
                    } else {
                        sharedParameters.preferences.safeSetSetting("isUnique_" + tool, true, Preferences.Visibility.GLOBAL);
                        MainTabsStyleHandler.setMainTabsStyle(sharedParameters, tool);
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

            JCheckBoxMenuItem pwnFoxSupportCapability = new JCheckBoxMenuItem("PwnFox Highlighter");
            pwnFoxSupportCapability.setToolTipText("Useful when PwnFox extension is enabled in Firefox");
            if (sharedParameters.preferences.safeGetBooleanSetting("pwnFoxSupportCapability")) {
                pwnFoxSupportCapability.setSelected(true);
            }
            pwnFoxSupportCapability.addActionListener((e) -> {
                if (sharedParameters.preferences.safeGetBooleanSetting("pwnFoxSupportCapability")) {
                    sharedParameters.preferences.safeSetSetting("pwnFoxSupportCapability", false, Preferences.Visibility.GLOBAL);
                } else {
                    sharedParameters.preferences.safeSetSetting("pwnFoxSupportCapability", true, Preferences.Visibility.GLOBAL);
                }
            });
            supportedCapabilitiesMenu.add(pwnFoxSupportCapability);

            globalMenu.add(supportedCapabilitiesMenu);

            // Debug options
            JMenu debugMenu = new JMenu("Debug Settings");
            ButtonGroup debugButtonGroup = new ButtonGroup();

            for(var item : BurpExtensionSharedParameters.DebugLevels.values()){
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
                            BurpTitleAndIcon.setTitle(sharedParameters, newTitle);
                            sharedParameters.preferences.safeSetSetting("BurpTitle", newTitle, Preferences.Visibility.PROJECT);
                        }
                    }
                }
            });
            projectMenu.add(changeTitle);

            // Change title button
            String burpResourceIconName = sharedParameters.preferences.safeGetStringSetting("BurpResourceIconName");
            Resource[] resourceIcons = new Resource[]{};

            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(sharedParameters.extensionClass.getClassLoader());
                resourceIcons = resolver.getResources("classpath:icons/*.*");

            } catch (IOException e) {
                sharedParameters.printDebugMessage("No icon was found in resources");
            }

            JMenu changeBurpIcon = new JMenu("Change Burp Suite Icon");

            ButtonGroup burpIconGroup = new ButtonGroup();
            for (Resource resourceIcon : resourceIcons) {
                String resourcePath = "/icons/" + resourceIcon.getFilename();
                JRadioButtonMenuItem burpIconImage = new JRadioButtonMenuItem(resourceIcon.getFilename().replaceAll("\\..*$", ""));
                burpIconImage.setIcon(new ImageIcon(ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, resourcePath), 16)));
                if (resourceIcon.getFilename().equalsIgnoreCase(burpResourceIconName)) {
                    burpIconImage.setSelected(true);
                }
                burpIconImage.addActionListener((e) -> {
                    BurpTitleAndIcon.setIcon(sharedParameters, resourcePath, 48, true);
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
                        BurpTitleAndIcon.setIcon(sharedParameters, newIconPath, 48, false);
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
                        new Timer().schedule(
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

            JMenuItem reloadAllSettings = new JMenuItem(new AbstractAction("Reload All Settings") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    new Thread(() -> {
                        MainTabsStyleHandler.resetMainTabsStylesFromSettings(sharedParameters);
                        ExtensionMainClass sharpenerBurpExtension = (ExtensionMainClass) sharedParameters.burpExtender;
                        sharpenerBurpExtension.load(true);

                    }).start();
                }
            });
            add(reloadAllSettings);

            JMenuItem resetAllSettings = new JMenuItem(new AbstractAction("Remove All Settings & Unload") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    int response = UIHelper.askConfirmMessage("Sharpener Extension: Reset All Settings & Unload", "Are you sure you want to remove all the settings and unload the extension?", new String[]{"Yes", "No"}, sharedParameters.get_mainFrameUsingMontoya());
                    if (response == 0) {

                        // A bug in resetting settings in BurpExtenderUtilities should be fixed so we will give it another chance instead of using a workaround
                        // sharedParameters.resetAllSettings();
                        sharedParameters.preferences.resetAllSettings();
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

            Image logoImg;
            if (sharedParameters.isDarkMode) {
                logoImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "/sharpener_rotated_small.png"), 59);
            } else {
                logoImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "/sharpener_rotated_small.png"), 59);
            }
            ImageIcon logoIcon = new ImageIcon(logoImg);
            JMenuItem logoMenu = new JMenuItem("About " + sharedParameters.extensionName,logoIcon);
            logoMenu.setPreferredSize(new Dimension(100, 46));

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
                BurpUITools.removeMenuBarByName(sharedParameters.extensionName, sharedParameters.get_mainMenuBarUsingMontoya(), repaintUI);
            } catch (Exception e) {
                sharedParameters.printlnError("Error in removing the top menu the dirty way: " + e.getMessage());
            }
        }
    }
}
