// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;

public class PwnFoxProxyRequestHandler implements ProxyRequestHandler {
    ExtensionSharedParameters sharedParameters;
    CapabilitySettings capabilitySettings;

    public PwnFoxProxyRequestHandler(ExtensionSharedParameters sharedParameters, CapabilitySettings capabilitySettings) {
        this.sharedParameters = sharedParameters;
        this.capabilitySettings = capabilitySettings;
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        var headerList = interceptedRequest.headers();
        if (headerList != null) {
            if (capabilitySettings.isEnabled()) {
                for (var item : headerList) {
                    if (item.name().equalsIgnoreCase("x-pwnfox-color")) {
                        var pwnFoxColor = item.value();
                        if (!pwnFoxColor.isEmpty()) {
                            interceptedRequest.annotations().setHighlightColor(HighlightColor.highlightColor(pwnFoxColor));
                        }
                        return ProxyRequestReceivedAction.continueWith(interceptedRequest.withRemovedHeader("X-PwnFox-Color"));
                    }
                }
            }
        }
        return ProxyRequestReceivedAction.continueWith(interceptedRequest);
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

}
