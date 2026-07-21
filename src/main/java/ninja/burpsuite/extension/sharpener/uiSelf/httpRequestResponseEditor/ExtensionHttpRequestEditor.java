// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.text.JTextComponent;
import java.awt.*;

public class ExtensionHttpRequestEditor implements ExtensionProvidedHttpRequestEditor {
    ExtensionSharedParameters sharedParameters;
    private HttpRequestResponse requestResponse;
    private final SharpenerMessageTabPanelGUI requestEditor;
    private final EditorCreationContext creationContext;

    ExtensionHttpRequestEditor(ExtensionSharedParameters sharedParameters, EditorCreationContext creationContext) {
        this.sharedParameters = sharedParameters;
        this.creationContext = creationContext;

        // RSyntaxTextArea registers a shared keymap that overrides Burp's own text shortcuts, so it is removed
        JTextComponent.removeKeymap("RTextAreaKeymap");
        if (creationContext.editorMode() == EditorMode.READ_ONLY) {
            requestEditor = new SharpenerMessageTabPanelGUI(sharedParameters, true);
        } else {
            requestEditor = new SharpenerMessageTabPanelGUI(sharedParameters, false);
        }

    }

    /**
     * @return An instance of {@link HttpRequest} derived from the content of the HTTP request editor.
     */
    @Override
    public HttpRequest getRequest() {
        // an untouched editor must hand the original request back unchanged;
        // an edited one returns exactly the editor content, nothing is ever added
        if (requestEditor.isModified()) {
            return HttpRequest.httpRequest(requestResponse.request().httpService(),
                    requestEditor.sharpenerMessageEditor_RTextScrollPane.getTextArea().getText());
        }
        return requestResponse.request();
    }

    /**
     * {@inheritDoc}
     *
     * @param httpRequestResponse
     */
    @Override
    public void setRequestResponse(HttpRequestResponse httpRequestResponse) {
        this.requestResponse = httpRequestResponse;
        requestEditor.sharpenerMessageEditor_RTextScrollPane.getTextArea().setText(httpRequestResponse.request().toString());
    }

    /**
     * {@inheritDoc}
     *
     * @param httpRequestResponse
     */
    @Override
    public boolean isEnabledFor(HttpRequestResponse httpRequestResponse) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String caption() {
        return sharedParameters.extensionName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component uiComponent() {
        return requestEditor.getMainPanel();
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
        return requestEditor.isModified();
    }
}
