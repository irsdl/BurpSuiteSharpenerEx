// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;

public class PwnFoxProxyRequestHandler implements ProxyRequestHandler {
    public static final String PWNFOX_COLOR_HEADER = "X-PwnFox-Color";

    ExtensionSharedParameters sharedParameters;
    CapabilitySettings capabilitySettings;

    public PwnFoxProxyRequestHandler(ExtensionSharedParameters sharedParameters, CapabilitySettings capabilitySettings) {
        this.sharedParameters = sharedParameters;
        this.capabilitySettings = capabilitySettings;
    }

    // Highlights the request based on the X-PwnFox-Color header value.
    // The header is kept at this stage, so other extensions can still read it (issue #24).
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        var headerList = interceptedRequest.headers();
        if (headerList != null && capabilitySettings.isEnabled()) {
            for (var item : headerList) {
                if (item.name().equalsIgnoreCase(PWNFOX_COLOR_HEADER)) {
                    var pwnFoxColor = item.value();
                    if (!pwnFoxColor.isEmpty()) {
                        try {
                            interceptedRequest.annotations().setHighlightColor(HighlightColor.highlightColor(pwnFoxColor));
                        } catch (Exception e) {
                            // unknown color value, keep the request without a highlight
                            sharedParameters.printDebugMessage("Unknown PwnFox color value: " + pwnFoxColor);
                        }
                    }
                    break;
                }
            }
        }
        return ProxyRequestReceivedAction.continueWith(interceptedRequest);
    }

    // Removes the header just before the request goes to the server, so it does not leak.
    // The user can turn this off to keep the header for other extensions or tools.
    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        var headerList = interceptedRequest.headers();
        if (headerList != null && capabilitySettings.isEnabled() && isHeaderRemovalEnabled()) {
            for (var item : headerList) {
                if (item.name().equalsIgnoreCase(PWNFOX_COLOR_HEADER)) {
                    return ProxyRequestToBeSentAction.continueWith(interceptedRequest.withRemovedHeader(item.name()));
                }
            }
        }
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

    private boolean isHeaderRemovalEnabled() {
        return sharedParameters.preferences.safeGetSetting(PwnFoxSettings.REMOVE_COLOR_HEADER_SETTING_NAME, true);
    }
}
