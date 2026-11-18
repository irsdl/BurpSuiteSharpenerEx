// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.objects;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StandardSettingsCompatibilityTest {
    private static class ChangedUiSettings extends StandardSettings {
        ChangedUiSettings(ExtensionSharedParameters parameters) { super(parameters); }
        public void init() {}
        public Collection<PreferenceObject> definePreferenceObjectCollection() { return List.of(); }
        public void loadSettings() { throw new NoSuchMethodError("optional Burp UI method removed"); }
        public void unloadSettings() { sharedParameters.printlnOutput("cleaned partial hook"); }
    }

    private static class WorkingSettings extends StandardSettings {
        WorkingSettings(ExtensionSharedParameters parameters) { super(parameters); }
        public void init() {}
        public Collection<PreferenceObject> definePreferenceObjectCollection() { return List.of(); }
        public void loadSettings() { sharedParameters.printlnOutput("loaded independent feature"); }
        public void unloadSettings() {}
    }

    @Test void unavailableUiApiCleansPartialHooksAndAllowsOtherFeaturesToLoad() {
        ExtensionSharedParameters parameters = mock(ExtensionSharedParameters.class);
        doCallRealMethod().when(parameters).runOptionalFeature(anyString(), any());
        assertFalse(new ChangedUiSettings(parameters).isLoaded());
        assertTrue(new WorkingSettings(parameters).isLoaded());
        verify(parameters).reportCompatibilityIssue(eq("ChangedUiSettings"), isA(NoSuchMethodError.class));
        verify(parameters).printlnOutput("cleaned partial hook");
        verify(parameters).printlnOutput("loaded independent feature");
    }
}
