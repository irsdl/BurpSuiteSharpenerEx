// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.uiSelf.contextMenu;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenu implements ContextMenuItemsProvider {

    ExtensionSharedParameters sharedParameters;
    public ContextMenu(ExtensionSharedParameters sharedParameters) {
        this.sharedParameters = sharedParameters;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {

        if (event.isFromTool(ToolType.PROXY, ToolType.TARGET, ToolType.REPEATER))
        {
            List<Component> menuItemList = new ArrayList<>();

            JMenuItem retrieveRequestItem = new JMenuItem("Print request");
            JMenuItem retrieveResponseItem = new JMenuItem("Print response");

            HttpRequestResponse requestResponse = event.messageEditorRequestResponse().isPresent() ? event.messageEditorRequestResponse().get().requestResponse() : event.selectedRequestResponses().get(0);

            retrieveRequestItem.addActionListener(l -> sharedParameters.montoyaApi.logging().logToOutput("Request is:\r\n" + requestResponse.request().toString()));
            menuItemList.add(retrieveRequestItem);

            if (requestResponse.response() != null)
            {
                retrieveResponseItem.addActionListener(l -> sharedParameters.montoyaApi.logging().logToOutput("Response is:\r\n" + requestResponse.response().toString()));
                menuItemList.add(retrieveResponseItem);
            }

            return menuItemList;
        }

        return null;
    }
}
