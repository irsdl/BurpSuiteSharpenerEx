// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiControllers.subTabs;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObject;
import ninja.burpsuite.extension.sharpener.objects.TabFeaturesObjectStyle;
import ninja.burpsuite.libs.burp.generic.BurpUITools;
import ninja.burpsuite.libs.generic.ImageHelper;
import ninja.burpsuite.libs.generic.MouseEventForwarder;
import ninja.burpsuite.libs.generic.uiObjFinder.UIWalker;
import ninja.burpsuite.libs.generic.uiObjFinder.UiSpecObject;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public final class SubTabsContainerHandler {
    public JTabbedPane parentTabbedPane;
    public Container currentTabContainer;
    public JComponent currentTabTextField;
    public JComponent currentTabIcon;
    public JComponent currentTabCloseButton;
    public JComponent currentTabGroupButton;
    public ArrayList<Integer> tabIndexHistory = new ArrayList<>();
    public BurpUITools.MainTabs currentToolTab;

    private final SubTabsContainerHandler instance;
    private final ExtensionSharedParameters sharedParameters;
    private ArrayList<String> cachedTabTitles;
    private boolean titleEditInProgress = false;
    private String beforeManualEditTabTitle = "";
    private Color originalTabColor;
    private PropertyChangeListener subTabPropertyChangeListener;
    private ComponentListener subTabComponentWatcher;
    private boolean isFromSetColor = false;
    private MouseEventForwarder subTabContainerClickForwarder;
    private LayoutManager originalTabLayout;
    private ArrayList<String> titleHistory = new ArrayList<>();
    private boolean _isVisible = true;
    private boolean _hasChanges = false;

    public SubTabsContainerHandler(ExtensionSharedParameters sharedParameters, JTabbedPane tabbedPane, int tabIndex, boolean forComparison) {
        this.instance = this;
        this.sharedParameters = sharedParameters;
        this.parentTabbedPane = tabbedPane;

        if (tabbedPane.getTabCount() <= tabIndex)
            return;

        Component currentTabTemp = tabbedPane.getTabComponentAt(tabIndex);
        if (!(currentTabTemp instanceof Container)) return; // this is not a container, so it is not useful for us

        // to find whether this subtab is in repeater or intruder:
        String toolTabName = "";
        Component _parentTabbedPane = tabbedPane.getParent();
        if (_parentTabbedPane instanceof JTabbedPane currentParentTabbedPane) {
            toolTabName = currentParentTabbedPane.getTitleAt(currentParentTabbedPane.indexOfComponent(tabbedPane));

        } else if (_parentTabbedPane instanceof JPanel && _parentTabbedPane.getParent() instanceof JTabbedPane currentParentTabbedPane) {
            toolTabName = currentParentTabbedPane.getTitleAt(currentParentTabbedPane.indexOfComponent(tabbedPane.getParent()));
        } else if (_parentTabbedPane instanceof JPanel currentParentTabbedPane) {
            // it's being detached! who does that?! :p
            toolTabName = ((JFrame) currentParentTabbedPane.getRootPane().getParent()).getTitle().replace("Burp ", "");
        }

        currentToolTab = BurpUITools.getMainTabsObjFromString(toolTabName);

        if (currentToolTab == BurpUITools.MainTabs.None) {
            // this is the new changes introduce by burp 2022.1, so we need this code now
            int currentTabToolIndex = sharedParameters.get_rootTabbedPaneUsingMontoya().indexOfComponent(tabbedPane.getParent());
            toolTabName = sharedParameters.get_rootTabbedPaneUsingMontoya().getTitleAt(currentTabToolIndex);
        }

        currentToolTab = BurpUITools.getMainTabsObjFromString(toolTabName);
        this.currentTabContainer = (Container) currentTabTemp;

        // remember Burp's own tab layout so it can be restored after an icon is removed. The icon
        // needs a different layout, and rebuilding a generic one afterwards made the tab narrower
        // than a plain Burp tab. Restoring the captured layout keeps the tab its normal size.
        if (!sharedParameters.isTabTextColorSetByBackground) {
            LayoutManager currentLayout = currentTabContainer.getLayout();
            // Burp lays out its tab with its own custom layout manager (a burp.* class). A standard
            // AWT or Swing layout here (FlowLayout, BorderLayout) means the tab was already changed by
            // this extension (possibly an older damaged version), so it is not captured as the original.
            if (currentLayout != null) {
                String layoutClass = currentLayout.getClass().getName();
                if (!layoutClass.startsWith("java.awt.") && !layoutClass.startsWith("javax.swing.")) {
                    originalTabLayout = currentLayout;
                }
            }
        }

        UiSpecObject textFieldTabTitleUSO = new UiSpecObject(JTextField.class);
        currentTabTextField = (JComponent) UIWalker.findUIObjectInSubComponents(currentTabContainer, 1, textFieldTabTitleUSO);

        if (currentTabTextField == null) {
            sharedParameters.printlnError("An error has occurred when reading a specific tab. A restart might be needed.");
            return;
        }

        UiSpecObject closeButtonUSO = new UiSpecObject(JComponent.class);
        closeButtonUSO.set_isPartialName(true);
        closeButtonUSO.set_isCaseSensitiveName(false);
        closeButtonUSO.set_name("close");
        currentTabCloseButton = (JComponent) UIWalker.findUIObjectInSubComponents(currentTabContainer, 1, closeButtonUSO);

        UiSpecObject groupButtonUSO = new UiSpecObject(JComponent.class);
        groupButtonUSO.set_isPartialName(true);
        groupButtonUSO.set_isCaseSensitiveName(false);
        groupButtonUSO.set_name("group");
        currentTabGroupButton = (JComponent) UIWalker.findUIObjectInSubComponents(currentTabContainer, 1, groupButtonUSO);

        // to keep history of previous titles
        if (titleHistory.size() == 0)
            addTitleHistory(getTabTitle(), true);

        if (tabIndexHistory.size() == 0)
            tabIndexHistory.add(tabIndex);

        if (!forComparison)
            addSubTabWatcher();

        setHasChanges(false); // init mode
    }

    public boolean addSubTabWatcher() {
        if (!isValid())
            return false;
        // this.currentTabLabel.getPropertyChangeListeners().length is 2 by default in this case ... Burp Suite may change this and break my extension :s
        if (subTabPropertyChangeListener == null && this.currentTabTextField.getPropertyChangeListeners().length < 3) {
            subTabPropertyChangeListener = evt -> {
                if (evt.getPropertyName().equalsIgnoreCase("editable")) {
                    if (evt.getSource().getClass().equals(currentTabTextField.getClass())) {
                        if (!titleEditInProgress) {
                            if ((boolean) evt.getNewValue()) {
                                titleEditInProgress = true;
                                beforeManualEditTabTitle = getTabTitle();
                                originalTabColor = getColor();
                            }
                        } else {
                            if (!((boolean) evt.getNewValue())) {
                                titleEditInProgress = false;
                                sharedParameters.delayedTasks.schedule(
                                        new java.util.TimerTask() {
                                            @Override
                                            public void run() {
                                                if (sharedParameters.isUnloaded())
                                                    return;
                                                setColor(originalTabColor, false);
                                                if (!beforeManualEditTabTitle.equals(getTabTitle())) {
                                                    addTitleHistory(beforeManualEditTabTitle, true);
                                                    // title has changed manually
                                                    sharedParameters.allSettings.subTabsSettings.saveSettings(instance);
                                                }
                                                sharedParameters.allSettings.subTabsSettings.loadSettings();
                                            }
                                        },
                                        500
                                );
                            }
                        }
                    }

                } else if (evt.getPropertyName().equalsIgnoreCase("disabledTextColor")) {
                    if (!sharedParameters.isTabTextColorSetByBackground) {
                        // Burp 2026+ sets the tab colour through the tabbed pane foreground,
                        // so the title field disabledTextColor is no longer used for colouring
                        return;
                    }
                    boolean isFromSetToDefault = false;
                    Color newColor = (Color) evt.getNewValue();

                    if (newColor != null && isSetToDefaultColour(newColor)) {
                        isFromSetToDefault = true;
                    }

                    loadDefaultSetting();

                    if (!isFromSetColor && !isFromSetToDefault) {
                        if (newColor != null && newColor.equals(sharedParameters.defaultTabFeaturesObjectStyle.getColor())) {
                            // we have a case for auto tab colour change which we want to avoid
                            setColor((Color) evt.getOldValue(), false);
                        }
                    } else if (newColor == null || isFromSetToDefault) {
                        setColor(sharedParameters.defaultTabFeaturesObjectStyle.getColor(), false);
                    }
                    isFromSetColor = false;
                }
            };
            this.currentTabTextField.addPropertyChangeListener(subTabPropertyChangeListener);
            subTabComponentWatcher = new ComponentListener() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // Do nothing
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    // Do nothing
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    if (sharedParameters.isTabGroupSupportedByDefault) {
                        setVisibleIcon(true, false);
                    }
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    if (sharedParameters.isTabGroupSupportedByDefault) {
                        setVisibleIcon(false, false);
                    }
                }
            };
            this.currentTabTextField.addComponentListener(subTabComponentWatcher);
        } else if (this.currentTabTextField.getPropertyChangeListeners().length == 3) {
            subTabPropertyChangeListener = this.currentTabTextField.getPropertyChangeListeners()[2];
        }
        // on new Burp the tab container swallows clicks on its free space, so a click on the
        // padding between the title and the close button never reaches the tabbed pane. This
        // forwarder makes the whole tab clickable, so middle-click opens the menu and a left
        // click on the free space selects the tab, matching a plain Burp tab.
        if (currentTabContainer instanceof JComponent) {
            addContainerClickForwarder((JComponent) currentTabContainer);
        }
        return true;
    }

    public void removeSubTabWatcher() {
        if (subTabPropertyChangeListener != null) {
            this.currentTabTextField.removePropertyChangeListener(subTabPropertyChangeListener);
        }
        // the component listener added by addSubTabWatcher must be removed too, otherwise it stays on the tab after unload
        if (subTabComponentWatcher != null) {
            this.currentTabTextField.removeComponentListener(subTabComponentWatcher);
        }
        if (currentTabContainer instanceof JComponent) {
            removeContainerClickForwarder((JComponent) currentTabContainer);
        }
    }

    public TabFeaturesObject getTabFeaturesObject() {
        return new TabFeaturesObject(getTabIndex(), getTabTitle(), getTitleHistory(), getFontName(), getFontSize(), isBold(), isItalic(), getVisibleCloseButton(), getColor(), getIconString(), getIconSize());
    }

    public TabFeaturesObjectStyle getTabFeaturesObjectStyle() {
        return getTabFeaturesObject().getStyle();
    }

    public void updateByTabFeaturesObject(TabFeaturesObject tabFeaturesObject, boolean keepHistory, boolean ignoreHasChanges) {
        this.setTabTitle(tabFeaturesObject.getTitle(), ignoreHasChanges);
        if (keepHistory) {
            this.setTitleHistory(tabFeaturesObject.getTitleHistory());
        }


        this.updateByTabFeaturesObjectStyle(tabFeaturesObject.getStyle(), ignoreHasChanges);
    }

    public void updateByTabFeaturesObjectStyle(TabFeaturesObjectStyle tabFeaturesObjectStyle, boolean ignoreHasChanges) {
        this.setIcon(tabFeaturesObjectStyle.get_IconResourceString(), tabFeaturesObjectStyle.iconSize, ignoreHasChanges);
        this.setFontName(tabFeaturesObjectStyle.fontName, ignoreHasChanges);
        this.setFontSize(tabFeaturesObjectStyle.fontSize, ignoreHasChanges);
        this.setBold(tabFeaturesObjectStyle.isBold, ignoreHasChanges);
        this.setItalic(tabFeaturesObjectStyle.isItalic, ignoreHasChanges);
        this.setVisibleCloseButton(tabFeaturesObjectStyle.isCloseButtonVisible, ignoreHasChanges);
        this.setColor(tabFeaturesObjectStyle.getColor(), ignoreHasChanges);
    }

    public boolean isValid() {
        boolean result = parentTabbedPane != null && getTabIndex() != -1 && currentTabContainer != null && currentTabTextField != null;

        return result;
    }

    private void loadDefaultSetting() {
        // To set the defaultSubTabObject parameter which keeps default settings of a normal tab
        if (sharedParameters.defaultTabFeaturesObjectStyle == null) {
            var defFont = UIManager.getDefaults().getFont("TabbedPane.font");
            Color defColor;
            if (sharedParameters.isTabTextColorSetByBackground) {
                defColor = UIManager.getDefaults().getColor("TabbedPane.foreground");
            } else {
                // Burp 2026+ paints an unset tab with the tabbed pane default foreground colour
                if (parentTabbedPane != null && parentTabbedPane.getForeground() != null) {
                    defColor = parentTabbedPane.getForeground();
                } else {
                    defColor = UIManager.getDefaults().getColor("TabbedPane.foreground");
                }
            }
            sharedParameters.defaultTabFeaturesObjectStyle = new TabFeaturesObjectStyle("Default", defFont.getFontName(),
                    defFont.getSize(), defFont.isBold(), defFont.isItalic(), true, defColor,
                    "", 0);
        }
    }

    public boolean isDefaultColour(Color color) {
        if (color == null)
            return false;

        // the current look and feel default colour also counts as a default colour
        if (sharedParameters.defaultTabFeaturesObjectStyle != null
                && color.equals(sharedParameters.defaultTabFeaturesObjectStyle.getColor()))
            return true;

        String rgbHex = Integer.toHexString(color.getRGB()).substring(2);
        if (!sharedParameters.isDarkMode) {
            // light mode workaround, the second check covers the reset marker colour
            return rgbHex.equals("000000") || isSetToDefaultColour(color);
        } else {
            // dark mode workaround, the second check covers the reset marker colour
            return rgbHex.equals("bbbbbb") || isSetToDefaultColour(color);
        }
    }

    public boolean isSetToDefaultColour(Color color) {
        if (!sharedParameters.isDarkMode) {
            return Integer.toHexString(color.getRGB()).substring(2).equals("010101");
        } else {
            return Integer.toHexString(color.getRGB()).substring(2).equals("bcbcbc");
        }
    }

    public boolean isDotDotDotTab() {
        if (sharedParameters.isTabGroupSupportedByDefault) {
            // in this version dotdotdot tab has been removed!
            return false;
        } else {
            return parentTabbedPane.getTabComponentAt(parentTabbedPane.getTabCount() - 1).equals(currentTabContainer);
        }
    }

    public boolean isWebSocketTab() {
        if (parentTabbedPane.getComponentAt(getTabIndex()) == null)
            return false;

        return ((JComponent) parentTabbedPane.getComponentAt(getTabIndex())).getComponents().length < 2;
    }

    public boolean isDefault() {
        boolean result = false;

        if (isValid()) {
            if (sharedParameters.defaultTabFeaturesObjectStyle == null) {
                loadDefaultSetting();
            }

            if (isDefaultColour(getColor())) {
                // this is useful when user has changed dark <-> light mode; so we can still detect a default colour!
                if ((getTabIndex() == parentTabbedPane.getTabCount() - 1 && !sharedParameters.isTabGroupSupportedByDefault)
                        || sharedParameters.defaultTabFeaturesObjectStyle.equalsIgnoreColor(getTabFeaturesObjectStyle())) {
                    result = true;
                }
            } else {
                if ((getTabIndex() == parentTabbedPane.getTabCount() - 1 && !sharedParameters.isTabGroupSupportedByDefault)
                        || sharedParameters.defaultTabFeaturesObjectStyle.equals(getTabFeaturesObjectStyle())) {
                    result = true;
                }
            }
        }
        return result;
    }

    public void setToDefault(boolean ignoreHasChanges) {
        setToDefault(ignoreHasChanges, true);
    }

    public void setToDefault(boolean ignoreHasChanges, boolean useResetMarkerColour) {
        if (isValid()) {
            loadDefaultSetting();
            TabFeaturesObjectStyle tfosDefault = sharedParameters.defaultTabFeaturesObjectStyle;
            if (tfosDefault != null) {
                // a copy is used so the shared default style object is never changed
                TabFeaturesObjectStyle tfosToApply = tfosDefault.duplicate();
                // the reset marker workaround is only needed on old Burp. Burp 2026+ sets the colour
                // through the tabbed pane foreground, which applies the default colour directly.
                if (useResetMarkerColour && sharedParameters.isTabTextColorSetByBackground) {
                    // in order to set the right colour when reset to default is used, we need to use a special colour to detect this event
                    // this is because Burp does use the default colour when an item is changed - we have a workaround for that but
                    // the workaround stops reset to default to change the colour as well, so we need another workaround!!!
                    // the sub-tab watcher detects this marker colour and then applies the real default colour
                    if (!sharedParameters.isDarkMode) {
                        // light mode workaround
                        tfosToApply.setColor(Color.decode("#010101"));
                    } else {
                        // dark mode workaround
                        tfosToApply.setColor(Color.decode("#bcbcbc"));
                    }
                }
                removeIcon(ignoreHasChanges);
                updateByTabFeaturesObjectStyle(tfosToApply, ignoreHasChanges);
            }
        }
    }

    public boolean isCurrentTitleUnique(boolean isCaseSensitive) {
        boolean result = true;
        String currentTabTitle = getTabTitle();


        if (cachedTabTitles == null || !titleHistory.get(titleHistory.size() - 1).equals(currentTabTitle)) {
            refreshLocalTitleCache(isCaseSensitive);
            addTitleHistory(currentTabTitle, true);
        }

        if (!isCaseSensitive) {
            currentTabTitle = currentTabTitle.toLowerCase();
        }
        if (Collections.frequency(cachedTabTitles, currentTabTitle) > 1)
            result = false;

        return result;
    }

    public boolean isNewTitleUnique(String newTitle, boolean isCaseSensitive) {
        boolean result = true;

        if (cachedTabTitles == null || !titleHistory.get(titleHistory.size() - 1).equals(getTabTitle())) {
            cachedTabTitles = new ArrayList<>();
            int maxIndex = parentTabbedPane.getTabCount() - 1;
            if (sharedParameters.isTabGroupSupportedByDefault)
                maxIndex += 1;

            for (int index = 0; index < maxIndex; index++) {
                if (parentTabbedPane.getTitleAt(index) != null) {
                    if (isCaseSensitive) {
                        cachedTabTitles.add(parentTabbedPane.getTitleAt(index).trim());
                    } else {
                        cachedTabTitles.add(parentTabbedPane.getTitleAt(index).trim().toLowerCase());
                    }
                }
            }
        }

        if (!isCaseSensitive) {
            newTitle = newTitle.toLowerCase();
        }

        if (Collections.frequency(cachedTabTitles, newTitle) > 0)
            result = false;

        return result;
    }

    public int getTabIndex() {
        int subTabIndex = parentTabbedPane.indexOfTabComponent(currentTabContainer);

        if (isDotDotDotTab()) {
            subTabIndex = parentTabbedPane.getTabCount() - 1;
        }

        if (tabIndexHistory.size() == 0 || (subTabIndex != tabIndexHistory.get(tabIndexHistory.size() - 1) && !sharedParameters.isTabGroupSupportedByDefault)) {
            tabIndexHistory.add(subTabIndex);
        }

        return subTabIndex;
    }

    public String[] getTitleHistory() {
        if (titleHistory == null) {
            titleHistory = new ArrayList<>();
        }

        if (titleHistory.size() == 0)
            titleHistory.add(getTabTitle());

        String[] result = titleHistory.toArray(new String[titleHistory.size()]);

        return result;
    }


    public void setTitleHistory(String[] titles) {
        if (titles == null || titles.length == 0)
            titles = new String[]{getTabTitle()};

        titleHistory = new ArrayList<>(Arrays.asList(titles));
    }

    public void addTitleHistory(String title, boolean shouldUpdateSharedParameters) {
        title = title.trim();

        titleHistory.remove(title);

        titleHistory.add(title);

        if (shouldUpdateSharedParameters) {
            ArrayList<SubTabsContainerHandler> subTabsContainerHandlers = sharedParameters.allSubTabContainerHandlers.get(currentToolTab);
            int currentIndex = subTabsContainerHandlers.indexOf(instance);
            if (currentIndex >= 0)
                subTabsContainerHandlers.get(currentIndex).addTitleHistory(title, false);
        }
    }

    public void makeUniqueTitle() {
        String title = getTabTitle().trim();
        if (!isCurrentTitleUnique(false)) {
            // We need to rename its title to become unique
            int i = 0;
            String newTitle = "";
            while (newTitle.isEmpty() || !isNewTitleUnique(newTitle, false)) {
                // we need to add a number to the title to make it a unique title
                i++;
                newTitle = "#" + i + " " + title;
            }

            TabFeaturesObject originalFO = sharedParameters.supportedTools_SubTabs.get(currentToolTab).get(title.toLowerCase().trim());
            if (originalFO != null) {
                // the original item has special style, so we need to copy it
                originalFO.setTitle(newTitle); // we will fix the supportedTools_SubTabs parameter in saveSettings()
                updateByTabFeaturesObject(originalFO, false, true);
            } else {
                // the original item has no style
                setTabTitle(newTitle, false, true);
            }
        }
    }

    public String getLowercaseTrimmedTabTitle() {
        return getTabTitle().toLowerCase().trim();
    }

    public String getTabTitle() {
        String title = "";
        if (getTabIndex() != -1)
            title = parentTabbedPane.getTitleAt(getTabIndex());
        if (title == null || title.isBlank()) {
            title = "";
        }
        return title;
    }

    public void setTabTitle(String title, boolean keepHistory, boolean ignoreHasChanges) {
        if (isValid() && !title.isBlank() && !getTabTitle().equals(title.trim())) {
            if (!ignoreHasChanges)
                setHasChanges(true);
            title = StringUtils.abbreviate(title.trim(), 100);
            if (keepHistory) {
                addTitleHistory(title, true);
            }
            parentTabbedPane.setTitleAt(getTabIndex(), title);
            refreshLocalTitleCache(false);
        }
    }

    public void setTabTitle(String title, boolean ignoreHasChanges) {
        setTabTitle(title, ignoreHasChanges, true);
    }

    public void refreshLocalTitleCache(boolean isCaseSensitive) {
        cachedTabTitles = new ArrayList<>();
        int maxIndex = parentTabbedPane.getTabCount() - 1;
        if (sharedParameters.isTabGroupSupportedByDefault)
            maxIndex += 1;
        for (int index = 0; index < maxIndex; index++) {
            if (parentTabbedPane.getTitleAt(index) != null) {
                if (isCaseSensitive) {
                    cachedTabTitles.add(parentTabbedPane.getTitleAt(index).trim());
                } else {
                    cachedTabTitles.add(parentTabbedPane.getTitleAt(index).toLowerCase().trim());
                }
            }
        }
    }

    public void setFont(Font newFont, boolean ignoreHasChanges) {
        if (isValid() && !getFont().equals(newFont)) {
            if (!ignoreHasChanges)
                setHasChanges(true);
            currentTabTextField.setFont(newFont);
        }
    }

    public Font getFont() {
        return currentTabTextField.getFont();
    }

    public void setFontName(String name, boolean ignoreHasChanges) {
        setFont(new Font(name, getFont().getStyle(), getFont().getSize()), ignoreHasChanges);
    }

    public String getFontName() {
        return getFont().getFamily();
    }

    public void setFontSize(float size, boolean ignoreHasChanges) {
        setFont(getFont().deriveFont(size), ignoreHasChanges);
        if (hasIcon() && getIconSize() != size) {
            setIcon(getIconString(), (int) size, ignoreHasChanges);
        }
    }

    public float getFontSize() {
        return getFont().getSize();
    }

    public void toggleBold(boolean ignoreHasChanges) {
        setFont(getFont().deriveFont(getFont().getStyle() ^ Font.BOLD), ignoreHasChanges);
    }

    public void setBold(boolean shouldBeBold, boolean ignoreHasChanges) {
        if (shouldBeBold && !isBold()) {
            toggleBold(ignoreHasChanges);
        } else if (!shouldBeBold && isBold()) {
            toggleBold(ignoreHasChanges);
        }
    }

    public boolean isBold() {
        return getFont().isBold();
    }

    public void toggleItalic(boolean ignoreHasChanges) {
        setFont(getFont().deriveFont(getFont().getStyle() ^ Font.ITALIC), ignoreHasChanges);
    }

    public void setItalic(boolean shouldBeItalic, boolean ignoreHasChanges) {
        if (shouldBeItalic && !isItalic()) {
            toggleItalic(ignoreHasChanges);
        } else if (!shouldBeItalic && isItalic()) {
            toggleItalic(ignoreHasChanges);
        }
    }

    public boolean isItalic() {
        return getFont().isItalic();
    }

    public Color getColor() {
        if (!sharedParameters.isTabTextColorSetByBackground && isValid()) {
            // Burp 2026+ paints the tab title using the tabbed pane per-tab foreground colour
            Color tabForeground = parentTabbedPane.getForegroundAt(getTabIndex());
            if (tabForeground != null)
                return tabForeground;
            // no explicit colour means the tab uses the tabbed pane default colour
            return parentTabbedPane.getForeground();
        }
        return currentTabTextField.getForeground();
    }

    public String getColorCode() {
        Color color = getColor();
        int rgb = (color == null) ? 0 : color.getRGB();
        return String.format("#%06x", rgb & 0xFFFFFF);
    }

    public void setColor(Color color, boolean ignoreHasChanges) {
        if (!isValid() || color == null)
            return;

        if (sharedParameters.isTabTextColorSetByBackground) {
            if (!getColor().equals(color)) {
                isFromSetColor = true;
                if (!ignoreHasChanges)
                    setHasChanges(true);
                // old Burp used this colour as the tab title text colour
                parentTabbedPane.setBackgroundAt(getTabIndex(), color);
            }
        } else {
            // Burp 2026+ paints the tab title using the tabbed pane per-tab foreground colour.
            // Setting the title text field colour no longer changes what is painted.
            int index = getTabIndex();
            Color currentTabForeground = parentTabbedPane.getForegroundAt(index);
            if (currentTabForeground == null || !currentTabForeground.equals(color)) {
                if (!ignoreHasChanges)
                    setHasChanges(true);
                parentTabbedPane.setForegroundAt(index, color);
            }
            resetTabBackground();
        }
    }

    private void resetTabBackground() {
        Color currentTabBackground = parentTabbedPane.getBackgroundAt(getTabIndex());
        // a null colour makes the tab use the default tabbed pane background again
        if (currentTabBackground != null && !currentTabBackground.equals(parentTabbedPane.getBackground())) {
            parentTabbedPane.setBackgroundAt(getTabIndex(), null);
        }
    }

    public void showCloseButton(boolean ignoreHasChanges) {
        if (!sharedParameters.isTabTextColorSetByBackground)
            // new Burp shows the close button only on the selected tab, so it is left to Burp
            return;
        if (isValid() && currentTabCloseButton != null && !currentTabCloseButton.isVisible()) {
            if (!ignoreHasChanges)
                setHasChanges(true);
            currentTabCloseButton.setVisible(true);
            parentTabbedPane.revalidate();
            parentTabbedPane.repaint();
        }
    }

    public void hideCloseButton(boolean ignoreHasChanges) {
        if (!sharedParameters.isTabTextColorSetByBackground)
            // new Burp shows the close button only on the selected tab, so it is left to Burp
            return;
        if (isValid() && currentTabCloseButton != null && currentTabCloseButton.isVisible()) {
            if (!ignoreHasChanges)
                setHasChanges(true);
            currentTabCloseButton.setVisible(false);
            parentTabbedPane.revalidate();
            parentTabbedPane.repaint();
        }
    }

    public void setIcon(String iconString, int iconSize, boolean ignoreHasChanges) {
        if (isValid() && iconString != null && !iconString.isBlank() && iconSize > 0 && (!getIconString().equals(iconString) || iconSize != getIconSize())) {
            if (!ignoreHasChanges)
                setHasChanges(true);

            // search the subtab icon to ensure it is valid and get its icon to pass to setIconAt
            Image myImg = ImageHelper.scaleImageToWidth(ImageHelper.loadImageResource(sharedParameters.extensionClass, "subtabicons/" + iconString + ".png"), iconSize);

            if (myImg != null) {
                JComponent tabComponent = (JComponent) parentTabbedPane.getTabComponentAt(getTabIndex());

                if (tabComponent.getComponent(0) instanceof JLabel) {
                    // we already have an icon so we remove it!
                    tabComponent.remove(0);
                }

                if (tabComponent.getComponent(0) instanceof JTextField) {
                    // No icon has been added
                    try {
                        JLabel jLabel = new JLabel(new ImageIcon(myImg));
                        jLabel.setName(iconString + ":" + iconSize);
                        jLabel.setOpaque(false);
                        jLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                        // clicks on the icon must still reach the tabbed pane so the tab can be selected
                        jLabel.addMouseListener(new MouseEventForwarder(parentTabbedPane));
                        if (sharedParameters.isTabTextColorSetByBackground) {
                            // old Burp: the original layout only manages the title, so a generic layout is needed
                            tabComponent.setLayout(new FlowLayout(FlowLayout.CENTER));
                            tabComponent.setSize(tabComponent.getComponent(1).getWidth() + jLabel.getWidth(), tabComponent.getHeight());
                            tabComponent.add(jLabel, 0);
                        } else {
                            // new Burp: a BorderLayout would stretch the title across the tab and leave a
                            // gap between the icon and the text, so a tight FlowLayout is used instead.
                            // the whole-tab click forwarder is already added by addSubTabWatcher.
                            tabComponent.setLayout(new FlowLayout(FlowLayout.LEADING, 4, 0));
                            tabComponent.add(jLabel, 0);
                        }
                        if (!ignoreHasChanges) {
                            parentTabbedPane.revalidate();
                            parentTabbedPane.repaint();
                        }
                    } catch (Exception err) {
                        sharedParameters.montoyaApi.logging().logToError(err);
                    }
                }

                if (!isTitleVisible()) {
                    setVisibleIcon(false, true);
                }
            }
        }
    }

    public void setVisibleIcon(boolean state, boolean ignoreHasChanges) {
        if (isValid()) {
            JComponent tabComponent = (JComponent) parentTabbedPane.getTabComponentAt(getTabIndex());

            if (tabComponent.getComponent(0) instanceof JLabel) {
                tabComponent.getComponent(0).setVisible(state);
                if (!ignoreHasChanges) {
                    parentTabbedPane.revalidate();
                    parentTabbedPane.repaint();
                }
            }
        }
    }

    public void removeIcon(boolean ignoreHasChanges) {
        if (isValid()) {
            JComponent tabComponent = (JComponent) parentTabbedPane.getTabComponentAt(getTabIndex());
            if (tabComponent.getComponent(0) instanceof JLabel) {
                // we have an icon set
                tabComponent.remove(0);
                if (!ignoreHasChanges) {
                    parentTabbedPane.revalidate();
                    parentTabbedPane.repaint();
                }
            }
            // the whole-tab click forwarder stays on the tab (it is removed on unload by removeSubTabWatcher)
            // heals tabs which lost their original layout to an older version of this extension,
            // and restores the normal Burp BorderLayout after the tight icon FlowLayout is removed
            repairTabComponentLayout(tabComponent);
        }
    }

    private void addContainerClickForwarder(JComponent tabComponent) {
        if (sharedParameters.isTabTextColorSetByBackground || subTabContainerClickForwarder != null)
            return;

        subTabContainerClickForwarder = new MouseEventForwarder(parentTabbedPane);
        tabComponent.addMouseListener(subTabContainerClickForwarder);
    }

    private void removeContainerClickForwarder(JComponent tabComponent) {
        if (subTabContainerClickForwarder == null)
            return;

        tabComponent.removeMouseListener(subTabContainerClickForwarder);
        subTabContainerClickForwarder = null;
    }

    private void repairTabComponentLayout(JComponent tabComponent) {
        // only new Burp is handled; its tab component uses Burp's own custom layout
        if (sharedParameters.isTabTextColorSetByBackground)
            return;

        // preferred path: restore Burp's own captured tab layout, so the tab keeps the same size
        // and look as a plain Burp tab after an icon is removed
        if (originalTabLayout != null) {
            if (tabComponent.getLayout() != originalTabLayout) {
                tabComponent.setLayout(originalTabLayout);
                tabComponent.revalidate();
                tabComponent.repaint();
            }
            return;
        }

        // fallback for tabs whose original layout was not captured (for example a tab damaged by an
        // older extension version): rebuild a generic BorderLayout so the tab is at least usable
        if (tabComponent.getLayout() instanceof BorderLayout)
            return;

        tabComponent.setLayout(new BorderLayout(10, 0));
        boolean sideUsed = false;
        for (Component tabComponentChild : tabComponent.getComponents()) {
            if (tabComponentChild == currentTabTextField) {
                tabComponent.add(tabComponentChild, BorderLayout.CENTER);
            } else if (tabComponentChild instanceof JLabel) {
                tabComponent.add(tabComponentChild, BorderLayout.LINE_START);
            } else if (!sideUsed) {
                // best effort for other items such as the close button
                tabComponent.add(tabComponentChild, BorderLayout.LINE_END);
                sideUsed = true;
            }
        }
        tabComponent.revalidate();
        tabComponent.repaint();
    }

    public String getIconString() {
        String _iconString = "";
        if (hasIcon() && isValid()) {
            JComponent tabComponent = (JComponent) parentTabbedPane.getTabComponentAt(getTabIndex());
            if (tabComponent.getComponent(0) instanceof JLabel) {
                // we have an icon set
                String tempName = tabComponent.getComponent(0).getName();
                if (!tempName.isBlank() && tempName.contains(":"))
                    _iconString = tempName.split(":")[0];
            }
        }

        return _iconString;
    }

    public int getIconSize() {
        int _iconSize = 0;
        if (hasIcon() && isValid()) {
            JComponent tabComponent = (JComponent) parentTabbedPane.getTabComponentAt(getTabIndex());
            if (tabComponent.getComponent(0) instanceof JLabel) {
                // we have an icon set
                String tempName = tabComponent.getComponent(0).getName();
                if (!tempName.isBlank() && tempName.contains(":"))
                    _iconSize = Integer.parseInt(tempName.split(":")[1]);
            }
        }
        return _iconSize;
    }

    public boolean hasIcon() {
        boolean result = false;
        if (isValid()) {
            JComponent tabComponent = (JComponent) parentTabbedPane.getTabComponentAt(getTabIndex());
            if (tabComponent.getComponent(0) instanceof JLabel) {
                // we have an icon set
                result = true;
            }
        }
        return result;
    }

    public void setVisibleCloseButton(boolean isVisible, boolean ignoreHasChanges) {
        if (isVisible) {
            showCloseButton(ignoreHasChanges);
        } else {
            hideCloseButton(ignoreHasChanges);
        }
    }

    public boolean getVisibleCloseButton() {
        if (!sharedParameters.isTabTextColorSetByBackground)
            // new Burp toggles the close button per selection, so a stable default is reported
            // instead of the live state to keep styles from changing as tabs are selected
            return true;

        if (!isValid()) {
            return true;
        }

        if (currentTabCloseButton == null || currentTabCloseButton.getParent() == null)
            return false;

        return currentTabCloseButton.isVisible();
    }

    public boolean getVisible() {
        if (isDotDotDotTab())
            return true;
        return _isVisible;
    }

    public void setVisible(boolean visible) {
        if (visible != getVisible() && !isDotDotDotTab() && isValid()) {
            if (!visible) {
                originalTabColor = getColor();
                currentTabContainer.setPreferredSize(new Dimension(0, getCurrentDimension().height));
            } else {
                currentTabContainer.setPreferredSize(null);
                setColor(originalTabColor, true);
            }
            parentTabbedPane.setEnabledAt(getTabIndex(), visible);
            currentTabContainer.repaint();
            currentTabContainer.revalidate();
            _isVisible = visible;
            setHasChanges(false);
        }
    }

    public boolean isTitleVisible() {
        boolean result = false;
        if (currentTabTextField != null) {
            result = currentTabTextField.isVisible();
        }
        return result;
    }

    public Dimension getCurrentDimension() {
        return currentTabContainer.getPreferredSize();
    }

    public boolean getHasChanges() {
        if (!getVisible())
            setHasChanges(false);
        return _hasChanges;
    }

    public void setHasChanges(boolean hasChanges) {
        this._hasChanges = hasChanges;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (isValid()) {
            if (o instanceof SubTabsContainerHandler temp) {
                if (temp.currentTabContainer != null)
                    result = temp.currentTabContainer.equals(this.currentTabContainer);
            } else if (o instanceof Container temp) {
                result = temp.equals(this.currentTabContainer);
            }
        } else {
            if (o instanceof SubTabsContainerHandler temp) {
                if (temp.tabIndexHistory.size() != 0 && this.tabIndexHistory.size() != 0)
                    result = temp.tabIndexHistory.get(temp.tabIndexHistory.size() - 1).equals(this.tabIndexHistory.get(this.tabIndexHistory.size() - 1));
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        // follows the same rule as equals: the tab container when valid, otherwise the last known tab index
        // these objects are kept in lists, not in hash based collections
        if (isValid()) {
            return java.util.Objects.hashCode(currentTabContainer);
        } else if (!tabIndexHistory.isEmpty()) {
            return tabIndexHistory.get(tabIndexHistory.size() - 1).hashCode();
        }
        return 0;
    }

    public boolean isNormalTab() {
        boolean result = isValid() && currentTabCloseButton != null;
        return result;
    }

    public boolean isGroupContainerTab() {
        boolean result = isValid() && currentTabGroupButton != null;
        return result;
    }

    public int getGroupCount() {
        int result = -1;

        if (isGroupContainerTab()) {
            UiSpecObject groupCountLabelUSO = new UiSpecObject(JLabel.class);
            groupCountLabelUSO.set_isPartialName(true);
            groupCountLabelUSO.set_isCaseSensitiveName(false);
            groupCountLabelUSO.set_name("group");
            var currentTabGroupCountLabel = (JLabel) UIWalker.findUIObjectInSubComponents(currentTabContainer, 1, groupCountLabelUSO);
            if (currentTabGroupCountLabel != null) {
                try {
                    result = Integer.parseInt(currentTabGroupCountLabel.getText());
                } catch (Exception e) {
                    // Label was not numerical
                    result = 0;
                }
            }
        }
        return result;
    }

}
