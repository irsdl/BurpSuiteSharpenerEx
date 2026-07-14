// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.internal.MontoyaObjectFactory;
import burp.api.montoya.internal.ObjectFactoryLocator;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.libs.burp.generic.ExtendedPreferences;
import ninja.burpsuite.libs.objects.PreferenceObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// These tests cover the PwnFox Highlighter behavior around the X-PwnFox-Color header.
// The header must stay visible to other extensions at the received stage and must only
// be removed just before the request is sent, unless the user keeps it (issue #24).
public class PwnFoxProxyRequestHandlerTest {

    private ExtensionSharedParameters sharedParameters;
    private PwnFoxSettings pwnFoxSettings;
    private PwnFoxProxyRequestHandler handler;
    private InterceptedRequest interceptedRequest;
    private Annotations annotations;

    @BeforeEach
    void setUp() {
        sharedParameters = mock(ExtensionSharedParameters.class);
        sharedParameters.preferences = mock(ExtendedPreferences.class);
        setCapabilityEnabled(true);
        setHeaderRemoval(true);

        pwnFoxSettings = new PwnFoxSettings(sharedParameters);
        handler = new PwnFoxProxyRequestHandler(sharedParameters, pwnFoxSettings);

        interceptedRequest = mock(InterceptedRequest.class);
        annotations = mock(Annotations.class);
        when(interceptedRequest.annotations()).thenReturn(annotations);

        // the Montoya action factories need the Burp runtime, so a mocked factory is injected
        MontoyaObjectFactory factory = mock(MontoyaObjectFactory.class);
        when(factory.requestInitialInterceptResultFollowUserRules(any(HttpRequest.class))).thenAnswer(invocation -> {
            ProxyRequestReceivedAction action = mock(ProxyRequestReceivedAction.class);
            when(action.request()).thenReturn(invocation.getArgument(0));
            return action;
        });
        when(factory.requestFinalInterceptResultContinueWith(any(HttpRequest.class))).thenAnswer(invocation -> {
            ProxyRequestToBeSentAction action = mock(ProxyRequestToBeSentAction.class);
            when(action.request()).thenReturn(invocation.getArgument(0));
            return action;
        });
        when(factory.highlightColor("red")).thenReturn(HighlightColor.RED);
        when(factory.highlightColor("blue")).thenReturn(HighlightColor.BLUE);
        when(factory.highlightColor("not-a-color")).thenThrow(new IllegalArgumentException("Unknown color"));
        ObjectFactoryLocator.FACTORY = factory;
    }

    @AfterEach
    void tearDown() {
        ObjectFactoryLocator.FACTORY = null;
    }

    private void setCapabilityEnabled(boolean enabled) {
        when(sharedParameters.preferences.safeGetSetting("pwnFoxSupportCapability", true)).thenReturn(enabled);
    }

    private void setHeaderRemoval(boolean enabled) {
        when(sharedParameters.preferences.safeGetSetting(PwnFoxSettings.REMOVE_COLOR_HEADER_SETTING_NAME, true)).thenReturn(enabled);
    }

    private void setHeaders(String name, String value) {
        HttpHeader header = mock(HttpHeader.class);
        when(header.name()).thenReturn(name);
        when(header.value()).thenReturn(value);
        when(interceptedRequest.headers()).thenReturn(List.of(header));
    }

    @Test
    void requestReceivedHighlightsAndKeepsHeader() {
        setHeaders("X-PwnFox-Color", "red");

        var action = handler.handleRequestReceived(interceptedRequest);

        verify(annotations).setHighlightColor(HighlightColor.RED);
        verify(interceptedRequest, never()).withRemovedHeader(anyString());
        assertSame(interceptedRequest, action.request());
    }

    @Test
    void requestReceivedMatchesHeaderNameCaseInsensitively() {
        setHeaders("x-pwnfox-color", "blue");

        handler.handleRequestReceived(interceptedRequest);

        verify(annotations).setHighlightColor(HighlightColor.BLUE);
    }

    @Test
    void requestReceivedDoesNothingWhenCapabilityDisabled() {
        setCapabilityEnabled(false);
        setHeaders("X-PwnFox-Color", "red");

        var action = handler.handleRequestReceived(interceptedRequest);

        verify(annotations, never()).setHighlightColor(any(HighlightColor.class));
        assertSame(interceptedRequest, action.request());
    }

    @Test
    void requestReceivedIgnoresEmptyColorValue() {
        setHeaders("X-PwnFox-Color", "");

        handler.handleRequestReceived(interceptedRequest);

        verify(annotations, never()).setHighlightColor(any(HighlightColor.class));
    }

    @Test
    void requestReceivedSurvivesUnknownColorValue() {
        setHeaders("X-PwnFox-Color", "not-a-color");

        var action = assertDoesNotThrow(() -> handler.handleRequestReceived(interceptedRequest));

        assertSame(interceptedRequest, action.request());
    }

    @Test
    void requestToBeSentRemovesHeaderByDefault() {
        setHeaders("X-PwnFox-Color", "red");
        HttpRequest strippedRequest = mock(HttpRequest.class);
        when(interceptedRequest.withRemovedHeader("X-PwnFox-Color")).thenReturn(strippedRequest);

        var action = handler.handleRequestToBeSent(interceptedRequest);

        assertSame(strippedRequest, action.request());
    }

    @Test
    void requestToBeSentKeepsHeaderWhenRemovalIsOff() {
        setHeaderRemoval(false);
        setHeaders("X-PwnFox-Color", "red");

        var action = handler.handleRequestToBeSent(interceptedRequest);

        verify(interceptedRequest, never()).withRemovedHeader(anyString());
        assertSame(interceptedRequest, action.request());
    }

    @Test
    void requestToBeSentKeepsHeaderWhenCapabilityDisabled() {
        setCapabilityEnabled(false);
        setHeaders("X-PwnFox-Color", "red");

        var action = handler.handleRequestToBeSent(interceptedRequest);

        verify(interceptedRequest, never()).withRemovedHeader(anyString());
        assertSame(interceptedRequest, action.request());
    }

    @Test
    void requestToBeSentLeavesRequestWithoutHeaderUntouched() {
        HttpHeader header = mock(HttpHeader.class);
        when(header.name()).thenReturn("Host");
        when(interceptedRequest.headers()).thenReturn(List.of(header));

        var action = handler.handleRequestToBeSent(interceptedRequest);

        verify(interceptedRequest, never()).withRemovedHeader(anyString());
        assertSame(interceptedRequest, action.request());
    }

    @Test
    void removalSettingIsRegisteredGlobalWithDefaultTrue() {
        PreferenceObject preferenceObject = pwnFoxSettings.definePreferenceObjectCollection().iterator().next();

        assertEquals(PwnFoxSettings.REMOVE_COLOR_HEADER_SETTING_NAME, preferenceObject.settingName);
        assertEquals(boolean.class, preferenceObject.type);
        assertEquals(true, preferenceObject.defaultValue);
        assertEquals(Preferences.Visibility.GLOBAL, preferenceObject.visibility);
    }

    @Test
    void headerRemovalAccessorsUsePreferences() {
        assertTrue(pwnFoxSettings.isHeaderRemovalEnabled());

        pwnFoxSettings.setHeaderRemovalEnabled(false);

        verify(sharedParameters.preferences).safeSetSetting(PwnFoxSettings.REMOVE_COLOR_HEADER_SETTING_NAME, false, Preferences.Visibility.GLOBAL);
    }
}
