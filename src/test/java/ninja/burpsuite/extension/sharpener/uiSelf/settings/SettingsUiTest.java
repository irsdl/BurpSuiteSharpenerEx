// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.extension.sharpener.uiSelf.settings;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import ninja.burpsuite.extension.sharpener.*;
import ninja.burpsuite.extension.sharpener.capabilities.implementations.*;
import ninja.burpsuite.extension.sharpener.uiControllers.burpFrame.BurpFrameSettings;
import ninja.burpsuite.extension.sharpener.uiControllers.mainTabs.MainTabsSettings;
import ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu;
import ninja.burpsuite.libs.burp.generic.*;
import ninja.burpsuite.libs.generic.BackgroundTaskRunner;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.*;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.nameManager.NameManager;
import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsUiTest {
    private ExtensionSharedParameters p;
    private MontoyaApi api;
    private InMemoryPersistence store;
    private SharpenerSettingsActions actions;
    private final List<AutoCloseable> owned = new ArrayList<>();
    @BeforeEach void setup() {
        BurpUIDiscovery.onEdt(() -> {
            p = mock(ExtensionSharedParameters.class);
            api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS); p.montoyaApi = api;
            p.debugLevel = 0; p.extensionName = "Sharpener"; p.extensionClass = ExtensionMainClass.class;
            p.burpExtender = mock(ExtensionMainClass.class);
            try { var field = BurpExtensionSharedParameters.class.getField("backgroundTasks"); field.setAccessible(true); field.set(p, mock(BackgroundTaskRunner.class)); }
            catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            store = new InMemoryPersistence();
            p.preferences = spy(new ExtendedPreferences(store.montoyaApi(), new DefaultGsonProvider()));
            // Each fixture models its own extension classloader and the unchanged canonical keys.
            Set<String> keys = new HashSet<>(List.of("checkForUpdate", "debugLevel", "ToolsThemeName", "ToolsThemeCustomPath", "ToolsIconSize", "isToolTabPaneScrollable", "BurpTitle", "BurpResourceIconName", "BurpIconCustomPath", "LastBurpIconCustomPath", "useLastScreenPositionAndSize", "detectOffScreenPosition", "lastApplicationPosition", "lastApplicationSize", "customShortcutsV2", "pwnFoxSupportCapability", "pwnFoxRemoveColorHeader", "manualHighlightCapability"));
            for (var tool : BurpUITools.MainTabs.values()) keys.add("isUnique_" + tool);
            for (String key : keys) if (NameManager.isReserved(key)) NameManager.release(key);
            new MainTabsSettings(p); new BurpFrameSettings(p);
            p.preferences.register("checkForUpdate", boolean.class, false);
            p.preferences.register("debugLevel", int.class, 0);
            p.allSettings = mock(ExtensionGeneralSettings.class);
            p.allSettings.capabilitySettingsList = new ArrayList<>(List.of(new ManualHighlighterSettings(p), new PwnFoxSettings(p)));
            actions = new SharpenerSettingsActions(p);
            when(p.settingsActions()).thenReturn(actions);
            Registration menuHandle = mock(Registration.class); when(menuHandle.isRegistered()).thenReturn(true);
            when(api.userInterface().menuBar().registerMenu(any(JMenu.class))).thenReturn(menuHandle);
            clearInvocations(p.preferences, p.burpExtender, p.backgroundTasks);
            return null;
        });
    }
    @AfterEach void cleanup() {
        BurpUIDiscovery.onEdt(() -> {
            for (AutoCloseable resource : owned) try { resource.close(); } catch (Exception e) { throw new AssertionError(e); }
            if (actions != null) actions.close();
            if (p != null) for (String key : p.preferences.getRegisteredSettings().keySet()) if (NameManager.isReserved(key)) NameManager.release(key);
            return null;
        });
    }
    private AbstractButton button(Component root, String path) {
        if (root instanceof AbstractButton b && path.equals(b.getName())) return b;
        Component[] children = root instanceof JMenu m ? m.getMenuComponents() : root instanceof Container c ? c.getComponents() : new Component[0];
        for (Component child : children) { AbstractButton found = button(child, path); if (found != null) return found; }
        return null;
    }
    private <T> List<T> find(Component root, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(root)) found.add(type.cast(root));
        if (root instanceof Container c) for (Component child : c.getComponents()) found.addAll(find(child, type));
        return found;
    }
    private void flush() { BurpUIDiscovery.onEdt(() -> null); }
    private void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container c) layout(c);
    }
    @Test void formUsesAvailableWidthAndCompactSelectorsInLightAndDarkThemes() {
        BurpUIDiscovery.onEdt(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                for (boolean dark : List.of(false, true)) {
                    UIManager.setLookAndFeel(dark ? new com.formdev.flatlaf.FlatDarkLaf() : new com.formdev.flatlaf.FlatLightLaf());
                    for (int width : List.of(460, 740)) {
                        JPanel root = new JPanel(); SettingsPanelView view = new SettingsPanelView(p, root);
                        try {
                            root.setSize(width, 820);
                            for (int pass = 0; pass < 5; pass++) layout(root);
                            assertEquals(4, find(root, JComboBox.class).size(), "Use selectors for themes, icon size, debug and project icon");
                            assertTrue(find(root, JRadioButton.class).isEmpty(), "The settings form must not reproduce long menu choice lists");
                            JTabbedPane tabs = find(root, JTabbedPane.class).getFirst();
                            assertEquals(2, tabs.getTabCount()); assertTrue(tabs.getWidth() >= width - 48);
                            JComboBox<?> theme = find(root, JComboBox.class).stream().filter(c -> STYLE_THEME.equals(c.getName())).findFirst().orElseThrow();
                            assertTrue(theme.getWidth() >= 170, "Use the settings width for readable selectors: " + theme.getWidth());
                            assertNull(button(root, "Unload Extension")); assertNull(button(root, "About Sharpener"));
                            for (int page = 0; page < 2; page++) {
                                tabs.setSelectedIndex(page); for (int pass = 0; pass < 3; pass++) layout(root);
                                if (page == 1) {
                                    for (JTextField field : find(tabs.getSelectedComponent(), JTextField.class)) {
                                        Rectangle bounds = SwingUtilities.convertRectangle(field.getParent(), field.getBounds(), root);
                                        assertTrue(bounds.y < 500 && bounds.height >= 18 && bounds.x + bounds.width <= width, "Project fields must be visible without giant empty regions: " + bounds);
                                    }
                                }
                                var image = new java.awt.image.BufferedImage(width, 820, java.awt.image.BufferedImage.TYPE_INT_RGB);
                                Graphics2D graphics = image.createGraphics(); try { root.printAll(graphics); } finally { graphics.dispose(); }
                                var file = java.nio.file.Path.of("build/reports/settings-preview", (dark ? "dark" : "light") + "-" + width + "-" + (page == 0 ? "global" : "project") + ".png");
                                java.nio.file.Files.createDirectories(file.getParent()); javax.imageio.ImageIO.write(image, "png", file.toFile());
                            }
                        } finally { view.close(); }
                    }
                }
            } catch (Exception e) { throw new AssertionError(e); }
            finally { try { UIManager.setLookAndFeel(original); } catch (Exception e) { throw new AssertionError(e); } }
            return null;
        });
    }
    private static final String STYLE_THEME = "Global Settings/Tools' Template And Style/Icons' Theme/";
    @Test void renderingAndRefreshAreReadOnlyAndKeepSerializedValues() {
        BurpUIDiscovery.onEdt(() -> {
            p.preferences.set("ToolsThemeName", "hacker"); p.preferences.set("BurpTitle", "Project fixture");
            Map<String,String> globals = Map.copyOf(store.globalStore), projects = Map.copyOf(store.projectStore);
            clearInvocations(p.preferences);
            SharpenerSettingsPanel panel = new SharpenerSettingsPanel(p); owned.add(panel);
            JComponent first = panel.uiComponent();
            TopMenu menu = new TopMenu(p); owned.add(menu);
            assertTrue(Arrays.stream(menu.getMenuComponents()).anyMatch(c -> c instanceof JMenuItem item && "Report Bug/Feature (Opens a Browser)".equals(item.getText())), "Preserve the original issue action caption and location");
            for (int i = 0; i < 4; i++) menu.updateTopMenuBar();
            assertSame(first, panel.uiComponent());
            assertEquals("hacker", p.preferences.safeGetStringSetting("ToolsThemeName"));
            assertTrue(find(first, JComboBox.class).stream().anyMatch(combo -> String.valueOf(combo.getSelectedItem()).equals("Hacker")));
            assertEquals(globals, store.globalStore); assertEquals(projects, store.projectStore);
            verify(p.preferences, never()).safeSetSetting(anyString(), any(), any());
            verifyNoInteractions(p.burpExtender, p.backgroundTasks);
            return null;
        });
    }
    @Test void nativeAndMenuWriteIdenticalKeysScopesAndApplyImmediately() {
        BurpUIDiscovery.onEdt(() -> {
            JPanel content = new JPanel(); SettingsPanelView panel = new SettingsPanelView(p, content); owned.add(panel);
            TopMenu menu = new TopMenu(p); owned.add(menu);
            for (Component view : List.of(content, menu)) {
                p.preferences.set("detectOffScreenPosition", true);
                // Deliberately stale view: the explicit selected value, not the cached preference, wins.
                button(view, "Global Settings/Detect Off Screen Window Position").setSelected(true);
                button(view, "Global Settings/Detect Off Screen Window Position").doClick(0);
                assertEquals("false", store.globalStore.get("detectOffScreenPosition"));
                button(view, "Global Settings/Supported Capabilities/PwnFox: Remove the color header").doClick(0);
                assertEquals(String.valueOf(p.preferences.safeGetBooleanSetting("pwnFoxRemoveColorHeader")), store.globalStore.get("pwnFoxRemoveColorHeader"));
            }
            JFrame frame = mock(JFrame.class); when(p.get_mainFrameUsingMontoya()).thenReturn(frame); when(frame.getTitle()).thenReturn("Original");
            actions.setTitle("Custom"); assertEquals("\"Custom\"", store.projectStore.get("BurpTitle")); assertFalse(store.globalStore.containsKey("BurpTitle"));
            actions.setTitle("  "); actions.setTitle(null); assertEquals("\"Custom\"", store.projectStore.get("BurpTitle"));
            return null;
        });
        flush(); BurpUIDiscovery.onEdt(() -> { verify(p.get_mainFrameUsingMontoya()).setTitle("Custom"); return null; });
    }
    @Test void preferenceNotificationsRefreshBothSurfacesAndDisposalRemovesListeners() {
        final JPanel content = new JPanel();
        final TopMenu[] menu = new TopMenu[1];
        BurpUIDiscovery.onEdt(() -> {
            owned.add(new SettingsPanelView(p, content)); menu[0] = new TopMenu(p); owned.add(menu[0]);
            p.preferences.set("checkForUpdate", true); return null;
        }); flush();
        BurpUIDiscovery.onEdt(() -> {
            assertTrue(button(content, "Check for Update on Start").isSelected());
            assertTrue(button(menu[0], "Check for Update on Start").isSelected());
            verifyNoInteractions(p.burpExtender);
            actions.close(); actions.close();
            verify(p.preferences, times(1)).removeSettingListener(any());
            return null;
        });
    }
    @Test void updateOptInOnlyChecksOnUserEnableAndCancelledSelectionsPreserveState() {
        BurpUIDiscovery.onEdt(() -> {
            actions.setCustomTheme(null); actions.setCustomTheme(""); actions.setIcon(null, false); actions.setIcon(" ", false);
            assertEquals("@irsdl", p.preferences.safeGetStringSetting("ToolsThemeName"));
            verify(p.preferences, never()).safeSetSetting(anyString(), any(), any());
            actions.setUpdateCheck(true); actions.setUpdateCheck(true); actions.setUpdateCheck(false);
            verify((ExtensionMainClass)p.burpExtender, times(1)).checkForUpdate();
            return null;
        });
    }
    @Test void nativeSuccessKeepsFullMenuAndNavigationFailureRetainsBoth() {
        BurpUIDiscovery.onEdt(() -> {
            Registration registration = mock(Registration.class); when(registration.isRegistered()).thenReturn(true);
            when(api.userInterface().registerSettingsPanel(any())).thenReturn(registration);
            SettingsSurface surface = new SettingsSurface(p); owned.add(surface);
            surface.register(); surface.register();
            assertTrue(surface.isNative()); assertNotNull(p.topMenuBar); assertTrue(surface.canOpen());
            var ui = api.userInterface(); doThrow(new AbstractMethodError("No navigation implementation")).when(ui).openSettingsWindow();
            surface.open(); assertFalse(surface.canOpen()); assertTrue(surface.isNative());
            verify(api.userInterface().menuBar(), times(1)).registerMenu(any(JMenu.class));
            surface.close(); surface.close(); verify(registration, times(1)).deregister();
            return null;
        });
    }
    @Test void missingClassesMethodsAndRegistrationFailureCreateRealFallbackMenu() {
        BurpUIDiscovery.onEdt(() -> {
            for (Throwable failure : List.of(new NoClassDefFoundError("SettingsPanel"), new NoSuchMethodError("registerSettingsPanel"), new AbstractMethodError("old host"), new IllegalStateException("host refused registration"))) {
                SettingsSurface surface = new SettingsSurface(p);
                Registration registration = mock(Registration.class); when(registration.isRegistered()).thenReturn(true);
                when(api.userInterface().menuBar().registerMenu(any(JMenu.class))).thenReturn(registration);
                surface.register(ignored -> { if (failure instanceof Error e) throw e; throw (RuntimeException)failure; });
                assertFalse(surface.isNative()); assertFalse(surface.canOpen()); assertNotNull(p.topMenuBar);
                assertNotNull(button(p.topMenuBar, "Keyboard Shortcuts"));
                button(p.topMenuBar, "Global Settings/Detect Off Screen Window Position").doClick(0);
                surface.close(); assertNull(p.topMenuBar); verify(registration).deregister();
            }
            return null;
        });
    }
    @Test void partialNativeRegistrationIsRemovedWithoutRemovingTheMenu() {
        BurpUIDiscovery.onEdt(() -> {
            Registration partial = mock(Registration.class);
            when(api.userInterface().registerSettingsPanel(any())).thenReturn(partial);
            Registration fallback = mock(Registration.class); when(fallback.isRegistered()).thenReturn(true);
            when(api.userInterface().menuBar().registerMenu(any(JMenu.class))).thenReturn(fallback);
            SettingsSurface surface = new SettingsSurface(p); owned.add(surface); surface.register();
            var order = inOrder(partial, api.userInterface().menuBar());
            order.verify(api.userInterface().menuBar()).registerMenu(any(JMenu.class)); order.verify(partial).isRegistered(); order.verify(partial).deregister();
            assertNotNull(p.topMenuBar); surface.close();
            when(api.userInterface().menuBar().registerMenu(any(JMenu.class))).thenThrow(new NoSuchMethodError("menu"));
            SettingsSurface neither = new SettingsSurface(p); owned.add(neither);
            assertDoesNotThrow(() -> neither.register()); assertNull(p.topMenuBar);
            verify(api.extension(), never()).unload();
            return null;
        });
    }
    @Test void noNavigationSupportIsIndependentAndUnloadCannotReviveUi() {
        BurpUIDiscovery.onEdt(() -> {
            Registration registration = mock(Registration.class); AutoCloseable cleanup = mock(AutoCloseable.class);
            SettingsSurface surface = new SettingsSurface(p); owned.add(surface);
            surface.register(ignored -> new SettingsSurface.NativeSurface(registration, new JPanel(), cleanup, false, () -> fail("navigation unavailable")));
            assertTrue(surface.isNative()); assertFalse(surface.canOpen()); surface.open(); surface.close();
            surface.register(ignored -> { fail("must not register after disposal"); return null; });
            verify(registration).deregister(); assertFalse(surface.isNative());
            return null;
        });
    }
    @Test void existingGlobalProjectJsonReloadsAndResetUsesTheOriginalDefaults() {
        BurpUIDiscovery.onEdt(() -> {
            p.preferences.set("ToolsThemeName", "gradient"); p.preferences.set("BurpTitle", "Project title");
            p.preferences.set("pwnFoxRemoveColorHeader", false);
            assertEquals("\"gradient\"", store.globalStore.get("ToolsThemeName"));
            assertEquals("\"Project title\"", store.projectStore.get("BurpTitle"));
            for (String key : List.of("ToolsThemeName", "BurpTitle", "pwnFoxRemoveColorHeader")) NameManager.release(key);
            ExtendedPreferences reload = new ExtendedPreferences(store.montoyaApi(), new DefaultGsonProvider());
            reload.register("ToolsThemeName", String.class, "@irsdl", Preferences.Visibility.GLOBAL);
            reload.register("BurpTitle", String.class, "", Preferences.Visibility.PROJECT);
            reload.register("pwnFoxRemoveColorHeader", boolean.class, true, Preferences.Visibility.GLOBAL);
            assertEquals("gradient", reload.get("ToolsThemeName")); assertEquals("Project title", reload.get("BurpTitle"));
            assertEquals(false, reload.get("pwnFoxRemoveColorHeader"));
            reload.resetAll();
            assertEquals("\"@irsdl\"", store.globalStore.get("ToolsThemeName"));
            assertEquals("\"\"", store.projectStore.get("BurpTitle"));
            assertEquals("true", store.globalStore.get("pwnFoxRemoveColorHeader"));
            return null;
        });
    }
    @Test void closingOneControllerLeavesOtherListenersAliveAndDiscardsQueuedImages() throws Exception {
        int[] notifications = {0};
        org.mockito.ArgumentCaptor<BackgroundTaskRunner.Task> task = org.mockito.ArgumentCaptor.forClass(BackgroundTaskRunner.Task.class);
        BurpUIDiscovery.onEdt(() -> {
            SharpenerSettingsActions other = new SharpenerSettingsActions(p); owned.add(other);
            other.addListener(() -> notifications[0]++);
            verify(p.backgroundTasks).submit(eq("settings images"), task.capture());
            actions.close(); actions.close();
            p.preferences.set("detectOffScreenPosition", false);
            return null;
        });
        flush(); assertEquals(1, notifications[0]);
        BurpUIDiscovery.onEdt(() -> { try { owned.getFirst().close(); } catch (Exception e) { throw new AssertionError(e); } return null; });
        task.getValue().run(); flush(); assertEquals(1, notifications[0]);
    }
    @Test void coreLoadsWhenOptionalSettingsPanelClassIsAbsent() throws Exception {
        // Load the actual integration boundary in a host-like loader, hiding the optional API.
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("burp.api.montoya.ui.settings.")) throw new ClassNotFoundException(name);
                if (name.startsWith("ninja.burpsuite.extension.sharpener.uiSelf.settings.SettingsSurface") ||
                        name.endsWith("NativeSettingsIntegration") || name.endsWith("SharpenerSettingsPanel")) {
                    Class<?> found = findLoadedClass(name);
                    if (found == null) try (var input = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = Objects.requireNonNull(input).readAllBytes(); found = defineClass(name, bytes, 0, bytes.length);
                    } catch (java.io.IOException e) { throw new ClassNotFoundException(name, e); }
                    if (resolve) resolveClass(found); return found;
                }
                return super.loadClass(name, resolve);
            }
        };
        BurpUIDiscovery.onEdt(() -> {
            Registration registration = mock(Registration.class); when(registration.isRegistered()).thenReturn(true);
            when(api.userInterface().menuBar().registerMenu(any(JMenu.class))).thenReturn(registration);
            try {
                Class<?> type = loader.loadClass(SettingsSurface.class.getName());
                Object surface = type.getConstructor(ExtensionSharedParameters.class).newInstance(p);
                type.getMethod("register").invoke(surface);
                assertEquals(false, type.getMethod("isNative").invoke(surface)); assertNotNull(p.topMenuBar);
                type.getMethod("close").invoke(surface); verify(registration).deregister();
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            return null;
        });
    }
    @Test void failedPartialCleanupNeverCreatesDuplicateUiAndRetriesOwnedHandleOnUnload() {
        BurpUIDiscovery.onEdt(() -> {
            Registration partial = mock(Registration.class);
            doThrow(new IllegalStateException("host busy")).doNothing().when(partial).deregister();
            when(api.userInterface().registerSettingsPanel(any())).thenReturn(partial);
            SettingsSurface surface = new SettingsSurface(p); owned.add(surface);
            surface.register(); surface.register();
            assertFalse(surface.isNative()); assertNotNull(p.topMenuBar);
            verify(api.userInterface().menuBar(), times(1)).registerMenu(any(JMenu.class));
            surface.close(); verify(partial, times(2)).deregister();
            return null;
        });
    }
    @Test void unloadDuringHostRegistrationDisposesTheLateHandle() {
        BurpUIDiscovery.onEdt(() -> {
            Registration registration = mock(Registration.class); AutoCloseable controls = mock(AutoCloseable.class);
            SettingsSurface surface = new SettingsSurface(p); owned.add(surface);
            surface.register(ignored -> {
                surface.close(); // Simulate a nested host event loop unloading while registerSettingsPanel runs.
                return new SettingsSurface.NativeSurface(registration, new JPanel(), controls, true, () -> fail("dead navigation"));
            });
            assertFalse(surface.isNative()); assertNull(surface.component()); verify(registration).deregister();
            try { verify(controls).close(); } catch (Exception e) { throw new AssertionError(e); }
            verify(api.userInterface().menuBar(), times(1)).registerMenu(any(JMenu.class));
            return null;
        });
    }
}
