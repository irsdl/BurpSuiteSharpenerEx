// Burp Suite Extension Name: Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Released initially as open source by MDSec - https://www.mdsec.co.uk
// Project link: https://github.com/mdsecresearch/BurpSuiteSharpener

package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.http.*;
import burp.api.montoya.proxy.websocket.*;
import burp.api.montoya.websocket.Direction;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManualHighlighterRequestResponseHandler implements ProxyRequestHandler, ProxyResponseHandler, ProxyWebSocketCreationHandler, ProxyMessageHandler {
    ExtensionSharedParameters sharedParameters;
    CapabilitySettings capabilitySettings;

    // the shortest color is red and the longest is magenta in Burp Suite HighlightColor
    String highlightPatternToBeRemovedStr = "";
    String highlightPatternStayStr = "";
    Pattern highlightPatternToBeRemoved;
    Pattern highlightPatternStayPattern;

    public ManualHighlighterRequestResponseHandler(ExtensionSharedParameters sharedParameters, CapabilitySettings capabilitySettings) {
        this.sharedParameters = sharedParameters;
        this.capabilitySettings = capabilitySettings;

        highlightPatternToBeRemovedStr = "tempcolor("+sharedParameters.burpSupportedColorNames+")";
        highlightPatternStayStr = "permcolor("+sharedParameters.burpSupportedColorNames+")";
        highlightPatternToBeRemoved = Pattern.compile(highlightPatternToBeRemovedStr, Pattern.CASE_INSENSITIVE);
        highlightPatternStayPattern = Pattern.compile(highlightPatternStayStr, Pattern.CASE_INSENSITIVE);
    }

    // REQUEST
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        if (capabilitySettings.isEnabled()) {
            var requestString = interceptedRequest.toString();
            //sharedParameters.montoyaApi.utilities().byteUtils().convertToString(interceptedRequest.toByteArray().getBytes());

            Matcher highlightMatcherToBeRemoved = highlightPatternToBeRemoved.matcher(requestString);
            Matcher highlightMatcherStay = highlightPatternStayPattern.matcher(requestString);

            if (highlightMatcherToBeRemoved.find()) {
                String color = highlightMatcherToBeRemoved.group(1);

                if (!color.isEmpty() && HighlightColor.highlightColor(color.toLowerCase()) != HighlightColor.NONE) {
                    // Remove the header from the request
                    // no need to escape it as group(0) should be just letters
                    requestString = requestString.replaceAll(highlightMatcherToBeRemoved.group(0), "");

                    // Highlighting color
                    interceptedRequest.annotations().setHighlightColor(HighlightColor.highlightColor(color));

                    HttpRequest httpRequest;
                    HttpService httpService = interceptedRequest.httpService();

                    // the following code has been disabled as Burp Suite automatically decide about the HTTP version
                    /*
                    if(interceptedRequest.httpVersion().equalsIgnoreCase("HTTP/2")){
                        String[] headerBody = HTTPMessageHelper.getHeaderAndBody(requestString);
                        String[] justHeaders = headerBody[0].split("\r?\n");

                        List<HttpHeader> headersList = Arrays.stream(justHeaders)
                                .skip(1)
                                .map(HttpHeader::httpHeader)
                                .collect(Collectors.toList());

                        httpRequest = HttpRequest.http2Request(httpService, headersList, headerBody[1]).withMethod(interceptedRequest.method()).withPath(interceptedRequest.path().replaceAll(highlightMatcherToBeRemoved.group(0),""));
                    }else{
                        httpRequest = HttpRequest.httpRequest(httpService, requestString);
                    }
                    */
                    httpRequest = HttpRequest.httpRequest(httpService, requestString);
                    if (interceptedRequest.contains(highlightPatternToBeRemoved)) {
                        // if the value was in the body of the request, content-length should be updated
                        //httpRequest = httpRequest.withUpdatedHeader("content-length", String.valueOf(httpRequest.body().length()));
                        // the following method can update the content-length header, see https://github.com/PortSwigger/burp-extensions-montoya-api/issues/83
                        httpRequest = httpRequest.withBody(httpRequest.body());
                        //TODO: we need to fix the chunked messages length in the body of the HTTP request
                        // However, it is rare to see chunked request messages from the browsers.
                    }

                    return ProxyRequestReceivedAction.continueWith(httpRequest);
                }
            } else if (highlightMatcherStay.find()) {
                String color = highlightMatcherStay.group(1);

                if (!color.isEmpty() && HighlightColor.highlightColor(color.toLowerCase()) != HighlightColor.NONE) {
                    // Highlighting color
                    interceptedRequest.annotations().setHighlightColor(HighlightColor.highlightColor(color));
                }
            }
        }
        return ProxyRequestReceivedAction.continueWith(interceptedRequest);
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

    // RESPONSE

    /**
     * This method is invoked when an HTTP response is received in the Proxy.
     *
     * @param interceptedResponse An {@link InterceptedResponse} object
     *                            that extensions can use to query and update details of the response, and
     *                            control whether the response should be intercepted and displayed to the
     *                            user for manual review or modification.
     * @return The {@link ProxyResponseReceivedAction} containing the required action, HTTP response and annotations to be passed through.
     */
    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        if (capabilitySettings.isEnabled()) {
            var responseString = interceptedResponse.toString();
                    //sharedParameters.montoyaApi.utilities().byteUtils().convertToString(interceptedResponse.toByteArray().getBytes());
            Matcher highlightMatcherStay = highlightPatternStayPattern.matcher(responseString);
            if (highlightMatcherStay.find()) {
                String color = highlightMatcherStay.group(1);
                if (!color.isEmpty() && HighlightColor.highlightColor(color.toLowerCase()) != HighlightColor.NONE) {
                    // Highlighting color
                    interceptedResponse.annotations().setHighlightColor(HighlightColor.highlightColor(color));
                }
            }
        }
        return ProxyResponseReceivedAction.continueWith(interceptedResponse);
    }

    /**
     * This method is invoked when an HTTP response has been processed by the
     * Proxy before it is returned to the client.
     *
     * @param interceptedResponse An {@link InterceptedResponse} object
     *                            that extensions can use to query and update details of the response.
     * @return The {@link ProxyResponseToBeSentAction} containing the required action, HTTP response and annotations to be passed through.
     */
    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }

    // WEBSOCKET

    /**
     * Invoked by Burp when a WebSocket is being created by the Proxy tool.<br>
     * <b>Note</b> that the client side of the connection will not be upgraded until after this method completes.
     *
     * @param webSocketCreation {@link ProxyWebSocketCreation} containing information about the proxy websocket that is being created
     */
    @Override
    public void handleWebSocketCreation(ProxyWebSocketCreation webSocketCreation) {
        webSocketCreation.proxyWebSocket().registerProxyMessageHandler(this);
    }

    /**
     * Invoked when a text message is received from either the client or server.
     * This gives the extension the ability to modify the message before it is
     * processed by Burp.
     *
     * @param interceptedTextMessage Intercepted text WebSocket message.
     * @return The {@link TextMessageReceivedAction} containing the required action and text message to be passed through.
     */
    @Override
    public TextMessageReceivedAction handleTextMessageReceived(InterceptedTextMessage interceptedTextMessage) {
        if (capabilitySettings.isEnabled()) {
            Matcher highlightMatcherToBeRemoved = highlightPatternToBeRemoved.matcher(interceptedTextMessage.payload());
            Matcher highlightMatcherStay = highlightPatternStayPattern.matcher(interceptedTextMessage.payload());
            String finalPayload = interceptedTextMessage.payload();

            if (highlightMatcherToBeRemoved.find() && interceptedTextMessage.direction() == Direction.CLIENT_TO_SERVER) {
                String color = highlightMatcherToBeRemoved.group(1);

                if (!color.isEmpty() && HighlightColor.highlightColor(color.toLowerCase()) != HighlightColor.NONE) {
                    // Remove the header from the request
                    // no need to escape it as group(0) should be just letters
                    finalPayload = finalPayload.replaceAll(highlightMatcherToBeRemoved.group(0), "");
                    // Highlighting color
                    interceptedTextMessage.annotations().setHighlightColor(HighlightColor.highlightColor(color));
                }
            } else if (highlightMatcherStay.find()) {
                String color = highlightMatcherStay.group(1);
                if (!color.isEmpty() && HighlightColor.highlightColor(color.toLowerCase()) != HighlightColor.NONE) {
                    // Highlighting color
                    interceptedTextMessage.annotations().setHighlightColor(HighlightColor.highlightColor(color));
                }
            }
            String finalPayloadFinal = finalPayload;
            InterceptedTextMessage textMessage = new InterceptedTextMessage() {
                @Override
                public String payload() {
                    return finalPayloadFinal;
                }

                @Override
                public Direction direction() {
                    return interceptedTextMessage.direction();
                }

                @Override
                public Annotations annotations() {
                    return interceptedTextMessage.annotations();
                }
            };
            return TextMessageReceivedAction.continueWith(textMessage);
        }

        return TextMessageReceivedAction.continueWith(interceptedTextMessage);
    }

    /**
     * Invoked when a text message is about to be sent to either the client or server.
     * This gives the extension the ability to modify the message before it is
     * sent.
     *
     * @param interceptedTextMessage Intercepted text WebSocket message.
     * @return The {@link TextMessageReceivedAction} containing the required action and text message to be passed through.
     */
    @Override
    public TextMessageToBeSentAction handleTextMessageToBeSent(InterceptedTextMessage interceptedTextMessage) {
        return TextMessageToBeSentAction.continueWith(interceptedTextMessage);
    }

    /**
     * Invoked when a binary message is received from either the client or server.
     * This gives the extension the ability to modify the message before it is
     * processed by Burp.
     *
     * @param interceptedBinaryMessage Intercepted binary WebSocket message.
     * @return The {@link BinaryMessageReceivedAction} containing the required action and binary message to be passed through.
     */
    @Override
    public BinaryMessageReceivedAction handleBinaryMessageReceived(InterceptedBinaryMessage interceptedBinaryMessage) {
        return BinaryMessageReceivedAction.continueWith(interceptedBinaryMessage);
    }

    /**
     * Invoked when a binary message is about to be sent to either the client or server.
     * This gives the extension the ability to modify the message before it is
     * sent.
     *
     * @param interceptedBinaryMessage Intercepted binary WebSocket message.
     * @return The {@link BinaryMessageReceivedAction} containing the required action and binary message to be passed through.
     */
    @Override
    public BinaryMessageToBeSentAction handleBinaryMessageToBeSent(InterceptedBinaryMessage interceptedBinaryMessage) {
        return BinaryMessageToBeSentAction.continueWith(interceptedBinaryMessage);
    }


}
