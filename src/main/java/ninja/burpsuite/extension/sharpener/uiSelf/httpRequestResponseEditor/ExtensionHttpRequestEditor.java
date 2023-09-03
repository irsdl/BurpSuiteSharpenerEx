package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import java.awt.*;

public class ExtensionHttpRequestEditor implements ExtensionProvidedHttpRequestEditor {
    ExtensionSharedParameters sharedParameters;
    private HttpRequestResponse requestResponse;
    private final SharpenerMessageTabPanelGUI requestEditor;

    ExtensionHttpRequestEditor(ExtensionSharedParameters sharedParameters, EditorCreationContext creationContext)
    {
        this.sharedParameters = sharedParameters;

        if (creationContext.editorMode() == EditorMode.READ_ONLY)
        {
            requestEditor = new SharpenerMessageTabPanelGUI(sharedParameters, true);
        }
        else {
            requestEditor = new SharpenerMessageTabPanelGUI(sharedParameters, false);
        }

    }
    /**
     * @return An instance of {@link HttpRequest} derived from the content of the HTTP request editor.
     */
    @Override
    public HttpRequest getRequest() {
        HttpRequest request;

        if (requestEditor.isModified())
        {
            request = requestResponse.request().withAddedHeader("test1","demo1");
        }
        else
        {
            request = requestResponse.request().withAddedHeader("test2","demo2");;
        }

        return request;
    }

    /**
     * {@inheritDoc}
     *
     * @param httpRequestResponse
     */
    @Override
    public void setRequestResponse(HttpRequestResponse httpRequestResponse) {
        this.requestResponse = httpRequestResponse;
        //requestEditor.setContents();
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
