// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.mainTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.ImageHelper;
import ninja.burpsuite.libs.generic.MouseEventForwarder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


public class MainTabsStyleHandler {
    // client property keys used to keep the original Burp tab look, so unload can restore it
    static final String ORIGINAL_LAYOUT_KEY = "Sharpener.mainTab.originalLayout";
    static final String ORIGINAL_FONT_KEY = "Sharpener.mainTab.originalFont";
    static final String ORIGINAL_BORDER_KEY = "Sharpener.mainTab.originalBorder";
    static final String ORIGINAL_OPAQUE_KEY = "Sharpener.mainTab.originalOpaque";

    public static void setMainTabsStyle_noUiLock(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs toolName) {
        SwingUtilities.invokeLater(() -> {

            sharedParameters.printDebugMessage("setToolTabStyle for " + toolName);
            String themeName = sharedParameters.preferences.safeGetStringSetting("ToolsThemeName");
            String themeCustomPath = sharedParameters.preferences.safeGetStringSetting("ToolsThemeCustomPath");
            String iconSizeStr = sharedParameters.preferences.safeGetSetting("ToolsIconSize", "16");
            int iconSize = Integer.parseInt(iconSizeStr);

            JTabbedPane tabbedPane = sharedParameters.get_rootTabbedPaneUsingMontoya();
            if (tabbedPane == null)
                return;

            for (Component component : tabbedPane.getComponents()) {
                int componentIndex = tabbedPane.indexOfComponent(component);
                if (componentIndex == -1) {
                    continue;
                }

                String componentTitle = tabbedPane.getTitleAt(componentIndex);
                if (componentTitle != null && componentTitle.equalsIgnoreCase(toolName.toString())) {
                    JComponent tabComponent = (JComponent) tabbedPane.getTabComponentAt(componentIndex);
                    if (tabComponent.getComponent(0) instanceof JTextField jTextField) {
                        saveOriginalTabLook(tabComponent, jTextField);
                        // the tab component swallows clicks on its free space (above and below the
                        // title, or next to the icon), so they must be forwarded to the tabbed pane
                        addTabClickForwarder(tabComponent, tabbedPane);

                        jTextField.setFont(jTextField.getFont().deriveFont(Font.BOLD));
                        jTextField.setOpaque(false);
                        jTextField.setBorder(BorderFactory.createEmptyBorder());
                        try {
                            Image myImg;
                            if (!themeName.equalsIgnoreCase("custom")) {
                                myImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "/themes/" + themeName + "/" + toolName + ".png"), iconSize);
                                if (myImg == null) {
                                    // is this an extension?
                                    myImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "/themes/extensions/" + toolName + ".png"), iconSize);
                                }
                            } else {
                                // custom path!
                                myImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageFile(themeCustomPath + "/" + toolName + ".png"), iconSize);
                                if (myImg == null) {
                                    sharedParameters.printlnError("'" + themeCustomPath + "/" + toolName + ".png' could not be loaded or did not exist.");
                                }
                            }
                            JLabel jLabel;
                            if (myImg != null) {
                                ImageIcon imgIcon = new ImageIcon(myImg);
                                jLabel = new JLabel(imgIcon);
                            } else {
                                jLabel = new JLabel();
                            }
                            jLabel.setOpaque(false);
                            jLabel.setBorder(BorderFactory.createEmptyBorder());
                            // clicks on the icon must still reach the tabbed pane so the tab can be selected
                            jLabel.addMouseListener(new MouseEventForwarder(tabbedPane));

                            // a tight layout keeps a small gap between the icon and the title
                            // and does not make the tab taller than a normal Burp tab
                            tabComponent.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 0));
                            tabComponent.remove(0);
                            tabComponent.add(jLabel);
                            tabComponent.add(jTextField);
                        } catch (Exception e) {
                            sharedParameters.montoyaApi.logging().logToError(e);
                        }

                    }
                    break;
                }
            }
            tabbedPane.revalidate();
            tabbedPane.repaint();
        });


    }

    public static void resetMainTabsStylesFromSettings_noUiLock(ExtensionSharedParameters sharedParameters) {
        sharedParameters.printDebugMessage("resetToolTabStylesFromSettings");
        //unsetAllToolTabStyles(sharedParameters);
        //setToolTabStylesFromSettings(sharedParameters);
        // instead of two commented lines above, we want to reset it more smoothly
        SwingUtilities.invokeLater(() -> {

            sharedParameters.printDebugMessage("setToolTabStylesFromSettings");
            if (sharedParameters.preferences.safeGetBooleanSetting("isToolTabPaneScrollable")) {
                sharedParameters.get_rootTabbedPaneUsingMontoya().setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            } else {
                sharedParameters.get_rootTabbedPaneUsingMontoya().setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
            }

            for (BurpUITools.MainTabs tool : BurpUITools.MainTabs.values()) {
                if (sharedParameters.preferences.safeGetBooleanSetting("isUnique_" + tool)) {
                    MainTabsStyleHandler.unsetMainTabsStyle_noUiLock(sharedParameters, tool);
                    MainTabsStyleHandler.setMainTabsStyle_noUiLock(sharedParameters, tool);
                }
            }
        });
    }

    public static void unsetAllMainTabsStyles(ExtensionSharedParameters sharedParameters) {
        sharedParameters.printDebugMessage("unsetAllToolTabStyles");
        Runnable unsetAll = () -> {
            if (!sharedParameters.isSubTabScrollSupportedByDefault) {
                if (sharedParameters.preferences.safeGetBooleanSetting("isToolTabPaneScrollable")) {
                    sharedParameters.get_rootTabbedPaneUsingMontoya().setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
                }
            }

            for (BurpUITools.MainTabs tool : BurpUITools.MainTabs.values()) {
                MainTabsStyleHandler.unsetMainTabsStyle(sharedParameters, tool);
            }
        };

        // UI changes must run on the UI thread; unload can be called from another thread
        if (SwingUtilities.isEventDispatchThread()) {
            unsetAll.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(unsetAll);
            } catch (Exception e) {
                sharedParameters.printDebugMessage("unsetAllMainTabsStyles could not run on the UI thread: " + e.getMessage());
            }
        }
    }

    //_withUiLock
    public static void unsetMainTabsStyle_noUiLock(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs toolName) {
        SwingUtilities.invokeLater(() -> {
            unsetMainTabsStyle(sharedParameters, toolName);
        });
    }

    public static void unsetMainTabsStyle(ExtensionSharedParameters sharedParameters, BurpUITools.MainTabs toolName) {
        JTabbedPane tabbedPane = sharedParameters.get_rootTabbedPaneUsingMontoya();
        if (tabbedPane == null)
            return;

        for (Component component : tabbedPane.getComponents()) {
            int componentIndex = tabbedPane.indexOfComponent(component);
            if (componentIndex == -1) {
                continue;
            }

            String componentTitle = tabbedPane.getTitleAt(componentIndex);
            if (componentTitle == null)
                continue;

            if (componentTitle.equalsIgnoreCase(toolName.toString())) {
                JComponent tabComponent = (JComponent) tabbedPane.getTabComponentAt(componentIndex);
                removeTabClickForwarder(tabComponent);
                if (tabComponent.getComponent(0) instanceof JLabel) {
                    tabComponent.remove(0);
                    JTextField jTextField = (JTextField) tabComponent.getComponent(0);
                    restoreOriginalTabLook(tabComponent, jTextField);
                    tabbedPane.revalidate();
                    tabbedPane.repaint();
                }
                break;
            }
        }
    }

    private static void addTabClickForwarder(JComponent tabComponent, JTabbedPane tabbedPane) {
        for (var listener : tabComponent.getMouseListeners()) {
            if (listener instanceof MouseEventForwarder)
                return;
        }
        tabComponent.addMouseListener(new MouseEventForwarder(tabbedPane));
    }

    private static void removeTabClickForwarder(JComponent tabComponent) {
        for (var listener : tabComponent.getMouseListeners()) {
            if (listener instanceof MouseEventForwarder)
                tabComponent.removeMouseListener(listener);
        }
    }

    private static void saveOriginalTabLook(JComponent tabComponent, JTextField jTextField) {
        if (tabComponent.getClientProperty(ORIGINAL_LAYOUT_KEY) == null) {
            tabComponent.putClientProperty(ORIGINAL_LAYOUT_KEY, tabComponent.getLayout());
        }
        if (jTextField.getClientProperty(ORIGINAL_FONT_KEY) == null) {
            jTextField.putClientProperty(ORIGINAL_FONT_KEY, jTextField.getFont());
            jTextField.putClientProperty(ORIGINAL_BORDER_KEY, jTextField.getBorder());
            jTextField.putClientProperty(ORIGINAL_OPAQUE_KEY, jTextField.isOpaque());
        }
    }

    private static void restoreOriginalTabLook(JComponent tabComponent, JTextField jTextField) {
        LayoutManager originalLayout = (LayoutManager) tabComponent.getClientProperty(ORIGINAL_LAYOUT_KEY);
        if (originalLayout != null) {
            tabComponent.setLayout(originalLayout);
            tabComponent.putClientProperty(ORIGINAL_LAYOUT_KEY, null);
        }

        Font originalFont = (Font) jTextField.getClientProperty(ORIGINAL_FONT_KEY);
        if (originalFont != null) {
            jTextField.setFont(originalFont);
            jTextField.setBorder((Border) jTextField.getClientProperty(ORIGINAL_BORDER_KEY));
            jTextField.setOpaque(Boolean.TRUE.equals(jTextField.getClientProperty(ORIGINAL_OPAQUE_KEY)));
            jTextField.putClientProperty(ORIGINAL_FONT_KEY, null);
            jTextField.putClientProperty(ORIGINAL_BORDER_KEY, null);
            jTextField.putClientProperty(ORIGINAL_OPAQUE_KEY, null);
        } else {
            // fallback for a tab styled before the original look was saved
            jTextField.setFont(jTextField.getFont().deriveFont(Font.PLAIN));
            jTextField.setOpaque(false);
            jTextField.setBorder(BorderFactory.createEmptyBorder());
        }
    }
}
