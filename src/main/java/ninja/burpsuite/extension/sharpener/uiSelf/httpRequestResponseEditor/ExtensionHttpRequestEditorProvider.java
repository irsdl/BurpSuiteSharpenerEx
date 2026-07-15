// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor;

import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

public class ExtensionHttpRequestEditorProvider implements HttpRequestEditorProvider {
    ExtensionSharedParameters sharedParameters;

    public ExtensionHttpRequestEditorProvider(ExtensionSharedParameters sharedParameters) {
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
