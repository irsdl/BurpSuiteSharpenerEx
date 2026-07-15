// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import java.awt.*;

public class ExtensionHttpResponseEditor implements ExtensionProvidedHttpResponseEditor {
    private ExtensionSharedParameters sharedParameters;

    public ExtensionHttpResponseEditor(ExtensionSharedParameters sharedParameters, EditorCreationContext creationContext) {
        this.sharedParameters = sharedParameters;
    }

    /**
     * @return An instance of {@link HttpResponse} derived from the content of the HTTP response editor.
     */
    @Override
    public HttpResponse getResponse() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param requestResponse
     */
    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {

    }

    /**
     * {@inheritDoc}
     *
     * @param requestResponse
     */
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String caption() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component uiComponent() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Selection selectedData() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModified() {
        return false;
    }
}
