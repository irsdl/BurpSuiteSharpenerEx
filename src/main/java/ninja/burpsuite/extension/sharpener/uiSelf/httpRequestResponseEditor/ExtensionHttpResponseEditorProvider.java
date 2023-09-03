package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

public class ExtensionHttpResponseEditorProvider implements HttpResponseEditorProvider {
    ExtensionSharedParameters sharedParameters;
    public ExtensionHttpResponseEditorProvider(ExtensionSharedParameters sharedParameters)
    {
        this.sharedParameters = sharedParameters;
    }



    /**
     * Invoked by Burp when a new HTTP response editor is required from the extension.
     *
     * @param creationContext details about the context that is requiring a response editor
     * @return An instance of {@link ExtensionProvidedHttpResponseEditor}
     */
    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext creationContext) {
        return new ExtensionHttpResponseEditor(sharedParameters, creationContext);
    }
}
