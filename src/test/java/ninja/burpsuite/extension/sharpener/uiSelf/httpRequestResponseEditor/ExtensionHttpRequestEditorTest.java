// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

// The request editor is a disabled placeholder (hasHttpRequestEditor is false),
// but its getRequest() used to append demo headers to the request. If the feature
// flag is ever enabled, an untouched editor must hand the original request back
// unchanged so nothing extra can ever reach the target server.
public class ExtensionHttpRequestEditorTest {

    @Test
    void untouchedEditorReturnsTheOriginalRequestWithoutAddingAnything() {
        ExtensionSharedParameters sharedParameters = mock(ExtensionSharedParameters.class);
        EditorCreationContext creationContext = mock(EditorCreationContext.class);
        ExtensionHttpRequestEditor editor = new ExtensionHttpRequestEditor(sharedParameters, creationContext);

        HttpRequestResponse requestResponse = mock(HttpRequestResponse.class);
        HttpRequest originalRequest = mock(HttpRequest.class);
        when(requestResponse.request()).thenReturn(originalRequest);

        editor.setRequestResponse(requestResponse);

        assertSame(originalRequest, editor.getRequest());
        verify(originalRequest, never()).withAddedHeader(anyString(), anyString());
    }
}
