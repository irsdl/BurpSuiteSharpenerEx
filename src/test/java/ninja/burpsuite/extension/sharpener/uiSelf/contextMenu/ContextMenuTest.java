// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.uiSelf.contextMenu;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextMenuTest {
    private ContextMenuEvent event(List<HttpRequestResponse> selected) {
        ContextMenuEvent event = mock(ContextMenuEvent.class);
        when(event.isFromTool(ToolType.PROXY, ToolType.TARGET, ToolType.REPEATER)).thenReturn(true);
        when(event.messageEditorRequestResponse()).thenReturn(Optional.empty());
        when(event.selectedRequestResponses()).thenReturn(selected);
        return event;
    }

    @Test void emptySelectionDoesNotCreateActionsOrThrow() {
        var menu = new ContextMenu(mock(ExtensionSharedParameters.class));
        assertTrue(menu.provideMenuItems(event(List.of())).isEmpty());
    }

    @Test void absentRequestDoesNotCreateAnUnusablePrintAction() {
        var menu = new ContextMenu(mock(ExtensionSharedParameters.class));
        assertTrue(menu.provideMenuItems(event(List.of(mock(HttpRequestResponse.class)))).isEmpty());
    }

    @Test void requestWithoutResponseOffersOnlyRequestAction() {
        var menu = new ContextMenu(mock(ExtensionSharedParameters.class));
        HttpRequestResponse message = mock(HttpRequestResponse.class);
        when(message.request()).thenReturn(mock(HttpRequest.class));
        assertEquals(1, menu.provideMenuItems(event(List.of(message))).size());
    }
}
