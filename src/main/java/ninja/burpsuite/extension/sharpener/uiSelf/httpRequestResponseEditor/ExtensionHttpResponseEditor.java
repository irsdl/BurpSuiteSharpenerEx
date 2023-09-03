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
    private HttpRequestResponse requestResponse;
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
