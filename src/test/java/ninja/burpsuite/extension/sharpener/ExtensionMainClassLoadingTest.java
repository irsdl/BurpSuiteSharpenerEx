// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener;

import burp.api.montoya.MontoyaApi;
import ninja.burpsuite.libs.burp.generic.BurpExtensionFeatures;
import ninja.burpsuite.libs.burp.generic.BurpExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.BurpUIDiscovery;
import ninja.burpsuite.libs.generic.DelayedTaskRunner;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtensionMainClassLoadingTest {
    private void prepare(ExtensionSharedParameters parameters, MontoyaApi api) throws Exception {
        parameters.montoyaApi = api;
        parameters.extensionName = "Sharpener";
        parameters.isCompatibleWithCurrentBurpVersion = true;
        parameters.features = new BurpExtensionFeatures();
        parameters.features.hasHttpHandler = true;
        doCallRealMethod().when(parameters).runOptionalFeature(anyString(), any(Runnable.class));
        Field runner = BurpExtensionSharedParameters.class.getField("delayedTasks");
        runner.setAccessible(true);
        runner.set(parameters, mock(DelayedTaskRunner.class));
    }

    private void ready(ExtensionGeneralSettings settings) {
        when(settings.isLoaded()).thenReturn(true);
        settings.capabilitySettingsList = new java.util.ArrayList<>();
    }

    @Test
    void missingUiKeepsApiFeaturesLoaded() {
        BurpUIDiscovery.onEdt(() -> {
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            ExtensionMainClass extension = new ExtensionMainClass();
            try (var construction = mockConstruction(ExtensionSharedParameters.class,
                    (parameters, context) -> prepare(parameters, api));
                 var settings = mockConstruction(ExtensionGeneralSettings.class, (item, context) -> ready(item))) {
                assertDoesNotThrow(() -> extension.initialize(api));
                verify(api.extension(), never()).unload();
                ExtensionSharedParameters parameters = construction.constructed().getFirst();
                assertNotNull(parameters.allSettings);
                extension.extensionUnloaded();
                verify(parameters.allSettings).unloadSettings();
            }

            return null;
        });
    }

    @Test
    void brokenMenuDiscoveryDoesNotAbortSettingsOrLeaveMarkerOnUnload() {
        BurpUIDiscovery.onEdt(() -> {
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            ExtensionMainClass extension = new ExtensionMainClass();
            RuntimeException failure = new IllegalStateException("UI unavailable");
            JRootPane root = new JRootPane();
            try (var construction = mockConstruction(ExtensionSharedParameters.class, (parameters, context) -> {
                prepare(parameters, api);
                JFrame frame = mock(JFrame.class);
                when(frame.getRootPane()).thenReturn(root);
                when(parameters.get_mainFrameUsingMontoya()).thenReturn(frame);
                when(parameters.get_mainMenuBarUsingMontoya()).thenThrow(failure);
            }); var settings = mockConstruction(ExtensionGeneralSettings.class, (item, context) -> ready(item))) {
                assertDoesNotThrow(() -> extension.initialize(api));
                ExtensionSharedParameters parameters = construction.constructed().getFirst();
                verify(parameters).reportCompatibilityIssue("stale menu cleanup", failure);
                verify(api.extension(), never()).unload();
                assertNotNull(parameters.allSettings);
                extension.extensionUnloaded();
                assertNull(root.getClientProperty(ExtensionMainClass.INSTANCE_MARKER_KEY));
            }

            return null;
        });
    }

    @Test
    void failedCoreSettingsStillAbortBeforeHandlersAreRegistered() {
        BurpUIDiscovery.onEdt(() -> {
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            ExtensionMainClass extension = new ExtensionMainClass();
            try (var parameters = mockConstruction(ExtensionSharedParameters.class, (item, context) -> prepare(item, api));
                 var settings = mockConstruction(ExtensionGeneralSettings.class)) {
                assertDoesNotThrow(() -> extension.initialize(api));
                verify(api.extension()).unload();
                verify(api, never()).proxy();
            }

            return null;
        });
    }

    @Test
    void unsupportedMenuApiDoesNotPreventEditorsFromRegistering() {
        BurpUIDiscovery.onEdt(() -> {
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            ExtensionMainClass extension = new ExtensionMainClass();
            try (var parameters = mockConstruction(ExtensionSharedParameters.class, (item, context) -> {
                prepare(item, api);
                item.features.hasTopMenu = true;
                item.features.hasHttpRequestEditor = true;
                item.features.hasHttpResponseEditor = true;
            }); var settings = mockConstruction(ExtensionGeneralSettings.class, (item, context) -> ready(item));
                 var menus = mockConstruction(ninja.burpsuite.extension.sharpener.uiSelf.topMenu.TopMenu.class)) {
                when(api.userInterface().menuBar()).thenThrow(new NoSuchMethodError("changed menu API"));
                assertDoesNotThrow(() -> extension.initialize(api));
                verify(api.userInterface()).registerHttpRequestEditorProvider(any());
                verify(api.userInterface()).registerHttpResponseEditorProvider(any());
                verify(api.extension(), never()).unload();
                extension.extensionUnloaded();
            }

            return null;
        });
    }

    @Test
    void mixedMenuBarAllowsSettingsToLoadAndMarkerToBeClearedOnUnload() {
        BurpUIDiscovery.onEdt(() -> {
            MontoyaApi api = mock(MontoyaApi.class, RETURNS_DEEP_STUBS);
            ExtensionMainClass extension = new ExtensionMainClass();
            JRootPane root = new JRootPane();
            try (var construction = mockConstruction(ExtensionSharedParameters.class, (parameters, context) -> {
                prepare(parameters, api);
                when(parameters.get_isUILoaded()).thenReturn(true);
                JFrame frame = mock(JFrame.class);
                when(frame.getRootPane()).thenReturn(root);
                when(parameters.get_mainFrameUsingMontoya()).thenReturn(frame);
                JMenuBar bar = new JMenuBar();
                bar.add(new JLabel("Burp"));
                bar.add(new JMenu("Help"));
                when(parameters.get_mainMenuBarUsingMontoya()).thenReturn(bar);
            }); var settings = mockConstruction(ExtensionGeneralSettings.class, (item, context) -> ready(item))) {
                // Run only the loading checks so no real Burp handlers or UI are registered.
                ExtensionSharedParameters parameters = new ExtensionSharedParameters(extension, api, "/extension.properties");
                try {
                    Field shared = ExtensionMainClass.class.getDeclaredField("sharedParameters");
                    shared.setAccessible(true);
                    shared.set(extension, parameters);
                    assertTrue(extension.furtherLoadingChecks());
                    assertSame(settings.constructed().getFirst(), parameters.allSettings);
                    assertNotNull(root.getClientProperty(ExtensionMainClass.INSTANCE_MARKER_KEY));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                } finally {
                    extension.extensionUnloaded();
                }
                assertNull(root.getClientProperty(ExtensionMainClass.INSTANCE_MARKER_KEY));
                verify(parameters.allSettings).unloadSettings();
                verify(api.extension(), never()).unload();
            }

            return null;
        });
    }
}
