package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

public class ExtensionHttpRequestEditorProvider implements HttpRequestEditorProvider {
    ExtensionSharedParameters sharedParameters;
    public ExtensionHttpRequestEditorProvider(ExtensionSharedParameters sharedParameters)
    {
        this.sharedParameters = sharedParameters;
    }

    /**
     * Invoked by Burp when a new HTTP request editor is required from the extension.
     *
     * @param creationContext details about the context that is requiring a request editor
     * @return An instance of {@link ExtensionProvidedHttpRequestEditor}
     */
    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        return new ExtensionHttpRequestEditor(sharedParameters, creationContext);
    }
}
