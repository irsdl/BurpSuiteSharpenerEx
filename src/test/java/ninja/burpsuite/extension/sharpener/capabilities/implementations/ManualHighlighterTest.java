// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx
package ninja.burpsuite.extension.sharpener.capabilities.implementations;

import burp.api.montoya.core.*;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.internal.*;
import burp.api.montoya.proxy.http.*;
import burp.api.montoya.proxy.websocket.*;
import burp.api.montoya.websocket.Direction;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualHighlighterTest {
    ManualHighlighterRequestResponseHandler handler;
    CapabilitySettings settings;
    Annotations annotations;

    @BeforeEach void setup() {
        var parameters=mock(ExtensionSharedParameters.class);
        parameters.burpSupportedColorNames="red|orange|yellow|green|cyan|blue|pink|magenta|gray";
        settings=mock(CapabilitySettings.class);when(settings.isEnabled()).thenReturn(true);
        annotations=mock(Annotations.class);
        handler=new ManualHighlighterRequestResponseHandler(parameters,settings);
        ObjectFactoryLocator.FACTORY=mock(MontoyaObjectFactory.class,invocation->{
            Object[] args=invocation.getArguments();Class<?> result=invocation.getMethod().getReturnType();
            if(result.getSimpleName().endsWith("Action"))return mock(result,call->switch(call.getMethod().getName()) {
                case "request","response","textMessage"->args[0];
                case "payload"->args[0] instanceof burp.api.montoya.websocket.TextMessage text?text.payload():args[0];
                case "annotations"->args.length>1?args[1]:annotations;
                default->RETURNS_DEFAULTS.answer(call);
            });
            if(invocation.getMethod().getName().equals("httpRequest")&&args.length==2&&args[1] instanceof String raw) {
                HttpRequest request=mock(HttpRequest.class);when(request.toString()).thenReturn(raw);
                ByteArray body=mock(ByteArray.class);when(body.getBytes()).thenReturn(raw.substring(raw.indexOf("\r\n\r\n")+4).getBytes(StandardCharsets.ISO_8859_1));
                when(request.body()).thenReturn(body);when(request.withBody(body)).thenReturn(request);return request;
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }
    @AfterEach void teardown(){ObjectFactoryLocator.FACTORY=null;}
    InterceptedRequest request(String raw) {
        var request=mock(InterceptedRequest.class);when(request.toString()).thenReturn(raw);when(request.annotations()).thenReturn(annotations);
        when(request.httpService()).thenReturn(mock(HttpService.class));return request;
    }

    @Test void proxyRemovesEveryRecognisedTemporaryMarkerIgnoringCase() {
        var request=request("POST /TempColorRED HTTP/1.1\r\nHost: fixture\r\n\r\nprefix-tempcolorblue-TemPcoLorRed-suffix-tempcolorinvalid");
        var result=handler.handleRequestReceived(request);
        assertEquals("POST / HTTP/1.1\r\nHost: fixture\r\n\r\nprefix---suffix-tempcolorinvalid",result.request().toString());
        assertSame(annotations,result.annotations());verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @ParameterizedTest @ValueSource(strings={"red","ORANGE","Yellow","green","CYAN","blue","Pink","MAGENTA","gray"})
    void permanentRequestMarkerHighlightsWithoutChangingTraffic(String color) {
        var request=request("GET /permcolor"+color+" HTTP/1.1\r\n\r\n");
        var result=handler.handleRequestReceived(request);
        assertSame(request,result.request());assertSame(annotations,result.annotations());
        verify(annotations).setHighlightColor(HighlightColor.valueOf(color.toUpperCase(Locale.ROOT)));
    }
    @Test void temporaryMarkerWinsOverPermanentInSameRequest() {
        handler.handleRequestReceived(request("GET /permcolorblue/tempcolorred HTTP/1.1\r\n\r\n"));
        verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void disabledCapabilityPreservesMarkersAndExistingAnnotations() {
        when(settings.isEnabled()).thenReturn(false);var request=request("GET /tempcolorred HTTP/1.1\r\n\r\n");
        assertSame(request,handler.handleRequestReceived(request).request());verifyNoInteractions(annotations);
    }
    @Test void unknownMarkersDoNotChangeColour() {
        handler.handleRequestReceived(request("GET /tempcolorinvalid/permcolorinvalid HTTP/1.1\r\n\r\n"));verifyNoInteractions(annotations);
    }
    @Test void proxyResponsePermanentMarkerHighlightsAndRetainsResponse() {
        var response=mock(InterceptedResponse.class);when(response.toString()).thenReturn("HTTP/1.1 200 OK\r\n\r\nPermColorGREEN");when(response.annotations()).thenReturn(annotations);
        var result=handler.handleResponseReceived(response);assertSame(response,result.response());assertSame(annotations,result.annotations());verify(annotations).setHighlightColor(HighlightColor.GREEN);
    }
    @Test void temporaryResponseMarkerDoesNotTriggerHighlighting() {
        var response=mock(InterceptedResponse.class);when(response.toString()).thenReturn("HTTP/1.1 200 OK\r\n\r\ntempcolorred");when(response.annotations()).thenReturn(annotations);
        handler.handleResponseReceived(response);verifyNoInteractions(annotations);
    }
    @ParameterizedTest @ValueSource(strings={"tempcolorred","permcolorred"})
    void loggerRequestAnnotationDoesNotRewriteOtherToolTraffic(String marker) {
        var request=mock(HttpRequestToBeSent.class);when(request.toString()).thenReturn(marker);when(request.annotations()).thenReturn(annotations);
        var result=handler.handleHttpRequestToBeSent(request);assertSame(request,result.request());assertSame(annotations,result.annotations());verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void loggerResponseUsesPermanentMarker() {
        var response=mock(HttpResponseReceived.class);when(response.toString()).thenReturn("permcolorblue");when(response.annotations()).thenReturn(annotations);
        var result=handler.handleHttpResponseReceived(response);assertSame(response,result.response());assertSame(annotations,result.annotations());verify(annotations).setHighlightColor(HighlightColor.BLUE);
    }
    @Test void disabledLoggerCapabilityPreservesAnnotations() {
        when(settings.isEnabled()).thenReturn(false);var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);
        assertSame(annotations,handler.handleHttpRequestToBeSent(request).annotations());verifyNoInteractions(annotations);
    }
    @Test void markerParsingIsIndependentOfSystemLocale() {
        Locale previous=Locale.getDefault();try{Locale.setDefault(Locale.forLanguageTag("tr-TR"));handler.handleRequestReceived(request("GET /PERMCOLORPINK HTTP/1.1\r\n\r\n"));verify(annotations).setHighlightColor(HighlightColor.PINK);}finally{Locale.setDefault(previous);}
    }
    @Test void strippedProxyMarkerReachesSeparateLoggerAnnotations() {
        var incoming=request("GET /tempcolorred/permcolorblue HTTP/1.1\r\n\r\n");when(incoming.messageId()).thenReturn(42);
        handler.handleRequestReceived(incoming);
        var outgoing=mock(HttpRequestToBeSent.class);var source=mock(ToolSource.class);var loggerAnnotations=mock(Annotations.class);
        when(source.isFromTool(ToolType.PROXY)).thenReturn(true);when(outgoing.toolSource()).thenReturn(source);when(outgoing.messageId()).thenReturn(42);when(outgoing.annotations()).thenReturn(loggerAnnotations);
        when(outgoing.toString()).thenReturn("GET //permcolorblue HTTP/1.1\r\n\r\n");
        handler.handleHttpRequestToBeSent(outgoing);verify(loggerAnnotations).setHighlightColor(HighlightColor.RED);
        verify(loggerAnnotations,never()).setHighlightColor(HighlightColor.BLUE);
        clearInvocations(loggerAnnotations);handler.handleHttpRequestToBeSent(outgoing);verify(loggerAnnotations).setHighlightColor(HighlightColor.BLUE);
    }
    @Test void pendingColoursAreBoundedAndConsumedOnce() {
        var pending=new ManualHighlighterRequestResponseHandler.PendingProxyColors(2);
        pending.remember(1,HighlightColor.RED);pending.remember(2,HighlightColor.BLUE);pending.remember(3,HighlightColor.GREEN);
        assertNull(pending.take(1));assertEquals(HighlightColor.BLUE,pending.take(2));assertNull(pending.take(2));assertEquals(HighlightColor.GREEN,pending.take(3));
    }
    @Test void otherToolsCannotConsumeAProxiedRequestsPendingColour() {
        var incoming=request("GET /tempcolorred HTTP/1.1\r\n\r\n");when(incoming.messageId()).thenReturn(42);handler.handleRequestReceived(incoming);
        var outgoing=mock(HttpRequestToBeSent.class);var source=mock(ToolSource.class);var loggerAnnotations=mock(Annotations.class);
        when(outgoing.toolSource()).thenReturn(source);when(outgoing.messageId()).thenReturn(42);when(outgoing.annotations()).thenReturn(loggerAnnotations);
        handler.handleHttpRequestToBeSent(outgoing);verifyNoInteractions(loggerAnnotations);
        when(source.isFromTool(ToolType.PROXY)).thenReturn(true);handler.handleHttpRequestToBeSent(outgoing);verify(loggerAnnotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void disabledLoggerDoesNotApplyPreviouslyStrippedMarker() {
        var incoming=request("GET /tempcolorred HTTP/1.1\r\n\r\n");when(incoming.messageId()).thenReturn(42);handler.handleRequestReceived(incoming);
        when(settings.isEnabled()).thenReturn(false);var outgoing=mock(HttpRequestToBeSent.class);var source=mock(ToolSource.class);var loggerAnnotations=mock(Annotations.class);
        when(source.isFromTool(ToolType.PROXY)).thenReturn(true);when(outgoing.toolSource()).thenReturn(source);when(outgoing.messageId()).thenReturn(42);when(outgoing.annotations()).thenReturn(loggerAnnotations);
        handler.handleHttpRequestToBeSent(outgoing);verifyNoInteractions(loggerAnnotations);
    }
    @Test void webSocketClientTemporaryMarkersAreRemovedAndAnnotated() {
        var message=mock(InterceptedTextMessage.class);when(message.payload()).thenReturn("TempColorRED-a-tempcolorBLUE");when(message.annotations()).thenReturn(annotations);when(message.direction()).thenReturn(Direction.CLIENT_TO_SERVER);
        assertEquals("-a-",handler.handleTextMessageReceived(message).payload());verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void webSocketServerTemporaryMarkerIsRetained() {
        var message=mock(InterceptedTextMessage.class);when(message.payload()).thenReturn("tempcolorred");when(message.annotations()).thenReturn(annotations);when(message.direction()).thenReturn(Direction.SERVER_TO_CLIENT);
        assertEquals("tempcolorred",handler.handleTextMessageReceived(message).payload());verifyNoInteractions(annotations);
    }
}
