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
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.internal.*;
import burp.api.montoya.proxy.http.*;
import burp.api.montoya.proxy.websocket.*;
import burp.api.montoya.websocket.Direction;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualHighlighterTest {
    ManualHighlighterRequestResponseHandler handler;
    ManualHighlighterSettings settings;
    Annotations annotations;

    @BeforeEach void setup() {
        var parameters=mock(ExtensionSharedParameters.class);
        parameters.burpSupportedColorNames="red|orange|yellow|green|cyan|blue|pink|magenta|gray";
        settings=mock(ManualHighlighterSettings.class);when(settings.isEnabled()).thenReturn(true);when(settings.isLoggerInspectionLimited()).thenReturn(true);
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
    void headerView(HttpRequest request, String headers) {
        var view=mock(HttpRequest.class);
        when(view.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes(headers));
        when(request.withBody("")).thenReturn(view);
    }
    void requestMetadata(HttpRequest request, String head) {
        var headers=new java.util.ArrayList<burp.api.montoya.http.message.HttpHeader>();
        for(String line:head.substring(head.indexOf("\r\n")+2).split("\r\n")) {
            int colon=line.indexOf(':');if(colon<0)continue;
            var header=mock(burp.api.montoya.http.message.HttpHeader.class);
            when(header.name()).thenReturn(line.substring(0,colon));when(header.value()).thenReturn(line.substring(colon+1).trim());headers.add(header);
        }
        when(request.headers()).thenReturn(headers);
        boolean transferEncoded=headers.stream().anyMatch(h->h.name().equalsIgnoreCase("Transfer-Encoding"));
        when(request.hasHeader("Transfer-Encoding")).thenReturn(transferEncoded);
        when(request.bodyOffset()).thenReturn(head.getBytes(StandardCharsets.ISO_8859_1).length);
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
        var request=mock(HttpRequestToBeSent.class);headerView(request,"GET /"+marker+" HTTP/1.1\r\n\r\n");when(request.annotations()).thenReturn(annotations);
        var result=handler.handleHttpRequestToBeSent(request);assertSame(request,result.request());assertSame(annotations,result.annotations());verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void loggerResponseUsesPermanentMarker() {
        var response=mock(HttpResponseReceived.class);when(response.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes("permcolorblue"));when(response.annotations()).thenReturn(annotations);
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
        when(outgoing.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes("GET //permcolorblue HTTP/1.1\r\n\r\n"));
        headerView(outgoing,"GET //permcolorblue HTTP/1.1\r\n\r\n");
        handler.handleHttpRequestToBeSent(outgoing);verify(loggerAnnotations).setHighlightColor(HighlightColor.RED);
        verify(loggerAnnotations,never()).setHighlightColor(HighlightColor.BLUE);
        verify(outgoing,never()).toByteArray();
        verify(outgoing,never()).withBody(anyString());
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
        when(outgoing.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes(""));
        headerView(outgoing,"");
        handler.handleHttpRequestToBeSent(outgoing);verifyNoInteractions(loggerAnnotations);
        when(source.isFromTool(ToolType.PROXY)).thenReturn(true);handler.handleHttpRequestToBeSent(outgoing);verify(loggerAnnotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void disabledLoggerDoesNotApplyPreviouslyStrippedMarker() {
        var incoming=request("GET /tempcolorred HTTP/1.1\r\n\r\n");when(incoming.messageId()).thenReturn(42);handler.handleRequestReceived(incoming);
        when(settings.isEnabled()).thenReturn(false);var outgoing=mock(HttpRequestToBeSent.class);var source=mock(ToolSource.class);var loggerAnnotations=mock(Annotations.class);
        when(source.isFromTool(ToolType.PROXY)).thenReturn(true);when(outgoing.toolSource()).thenReturn(source);when(outgoing.messageId()).thenReturn(42);when(outgoing.annotations()).thenReturn(loggerAnnotations);
        handler.handleHttpRequestToBeSent(outgoing);verifyNoInteractions(loggerAnnotations);
        when(settings.isEnabled()).thenReturn(true);
        when(outgoing.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes(""));
        headerView(outgoing,"");
        handler.handleHttpRequestToBeSent(outgoing);verifyNoInteractions(loggerAnnotations);
    }
    @Test void disabledHttpCallbacksNeverAccessBytesOrStrings() {
        when(settings.isEnabled()).thenReturn(false);
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);
        var response=mock(HttpResponseReceived.class);when(response.annotations()).thenReturn(annotations);
        when(request.toString()).thenThrow(new AssertionError("Disabled request decoded"));
        when(response.toString()).thenThrow(new AssertionError("Disabled response decoded"));
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        assertSame(response,handler.handleHttpResponseReceived(response).response());
        verify(request,never()).toByteArray();verify(response,never()).toByteArray();
        verify(request,never()).withBody(anyString());verify(request,never()).body();
        verify(settings,never()).isLoggerInspectionLimited();verifyNoInteractions(annotations);
    }
    @ParameterizedTest @ValueSource(strings={"", "Content-Type: application/octet-stream\r\n", "Content-Type: multipart/form-data; boundary=fixture\r\n", "Content-Length: 1\r\n", "Content-Length: -1\r\n", "Content-Length: 0\r\nContent-Length: 1\r\n", "Transfer-Encoding: chunked\r\n"})
    void urlAndHeaderMarkersSurviveLargeBodiesWithoutRewritingOrDecoding(String headers) {
        String raw="POST /tempcolorred HTTP/1.1\r\nHost: fixture\r\nX-Color: permcolorblue\r\n"+headers+"\r\n\u00ff\0"+"x".repeat(2_000_000)+"tempcolorgreen";
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);
        headerView(request,raw.substring(0,raw.indexOf("\r\n\r\n")+4));
        requestMetadata(request,raw.substring(0,raw.indexOf("\r\n\r\n")+4));
        when(request.toString()).thenThrow(new AssertionError("General request decoded"));
        var original=new ManualHighlightScannerTest.Bytes(raw.getBytes(StandardCharsets.ISO_8859_1),0);
        if(headers.equals("Content-Length: 1\r\n"))when(request.toByteArray()).thenReturn(original.array());
        else when(request.toByteArray()).thenThrow(new AssertionError("Rejected declaration copied original request"));
        when(request.body()).thenThrow(new AssertionError("Original body copied"));
        var result=handler.handleHttpRequestToBeSent(request);assertSame(request,result.request());assertSame(annotations,result.annotations());
        verify(annotations).setHighlightColor(HighlightColor.RED);verify(request,times(headers.equals("Content-Length: 1\r\n")?1:0)).toByteArray();verify(request,never()).body();
        assertEquals(0,original.maxRead,"Oversized actual request body must not be scanned");
        verify(request,times(1)).withBody("");
        verify(settings,times(1)).isEnabled();verify(settings,times(1)).isLoggerInspectionLimited();
    }
    @Test void oversizedHeaderAndResponseBudgetUseRawLengthRatherThanContentLength() {
        String raw="HTTP/1.1 200 OK\r\nContent-Length: 0\r\nX-Padding: "+"x".repeat(65536)+"permcolorblue\r\n\r\n";
        var source=new ManualHighlightScannerTest.Bytes(raw.getBytes(StandardCharsets.ISO_8859_1),65536);
        var response=mock(HttpResponseReceived.class);when(response.toByteArray()).thenReturn(source.array());when(response.annotations()).thenReturn(annotations);
        when(response.toString()).thenThrow(new AssertionError("General response decoded"));
        assertSame(response,handler.handleHttpResponseReceived(response).response());verifyNoInteractions(annotations);
        when(settings.isLoggerInspectionLimited()).thenReturn(false);
        when(response.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes(raw));
        assertSame(response,handler.handleHttpResponseReceived(response).response());verify(annotations).setHighlightColor(HighlightColor.BLUE);
    }
    @ParameterizedTest @ValueSource(ints={65535,65536,65537})
    void actualMessageLengthDeterminesExclusiveInspectionEnd(int length) {
        byte[] raw=new byte[length];java.util.Arrays.fill(raw,(byte)'x');
        byte[] marker="permcolorred".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(marker,0,raw,raw.length-marker.length,marker.length);
        var source=new ManualHighlightScannerTest.Bytes(raw,Math.min(length,65536));
        var request=mock(HttpResponseReceived.class);when(request.annotations()).thenReturn(annotations);when(request.toByteArray()).thenReturn(source.array());
        assertSame(request,handler.handleHttpResponseReceived(request).response());
        if(length<=65536)verify(annotations).setHighlightColor(HighlightColor.RED);else verifyNoInteractions(annotations);
        clearInvocations(annotations);when(settings.isLoggerInspectionLimited()).thenReturn(false);
        when(request.toByteArray()).thenReturn(new ManualHighlightScannerTest.Bytes(raw,length).array());
        handler.handleHttpResponseReceived(request);verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @Test void noBodyHeaderPermanentMarkerIsInspected() {
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);
        headerView(request,"GET / HTTP/1.1\r\nX-Color: permcolorblue\r\n\r\n");
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());verify(annotations).setHighlightColor(HighlightColor.BLUE);
    }
    @ParameterizedTest @ValueSource(ints={12,1024,65536,2_000_000})
    void missingLengthUsesHeadersAndUnlimitedRestoresCompleteDetection(int bodySize) {
        String head="POST / HTTP/1.1\r\nHost: fixture\r\nX-Color: permcolorblue\r\n\r\n";
        String raw=head+"x".repeat(bodySize-12)+"tempcolorred";
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);
        headerView(request,head);requestMetadata(request,head);
        when(request.toByteArray()).thenThrow(new AssertionError("Limited callback copied original request"));
        when(request.body()).thenThrow(new AssertionError("Limited callback copied original body"));
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        verify(annotations).setHighlightColor(HighlightColor.BLUE);verify(request,never()).body();verify(request,never()).toByteArray();
        clearInvocations(annotations);
        when(settings.isLoggerInspectionLimited()).thenReturn(false);
        doReturn(ManualHighlightScannerTest.bytes(raw)).when(request).toByteArray();
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());verify(annotations).setHighlightColor(HighlightColor.RED);
    }
    @ParameterizedTest @ValueSource(strings={"0","1","12","00012"," 12 "})
    void smallDeclaredLengthIsOnlyAHintAndActualBytesDetermineBodyHighlight(String declared) {
        String head="POST / HTTP/1.1\r\nHost: fixture\r\nContent-Length: "+declared+"\r\nX-Color: permcolorblue\r\n\r\n";
        String raw=head+"padding that is longer than the declaration-tempcolorred";
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);requestMetadata(request,head);
        when(request.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes(raw));
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        verify(annotations).setHighlightColor(HighlightColor.RED);verify(request,times(1)).toByteArray();
        verify(request,never()).withBody(anyString());verify(request,never()).body();
    }
    @ParameterizedTest @ValueSource(ints={65535,65536,65537})
    void actualWholeRequestSizeControlsAdmissionAfterAnUnderstatedLength(int size) {
        String head="POST / HTTP/1.1\r\nContent-Length: 1\r\nX-Color: permcolorblue\r\n\r\n";
        String raw=head+"x".repeat(size-head.length()-12)+"tempcolorred";
        var actual=new ManualHighlightScannerTest.Bytes(raw.getBytes(StandardCharsets.ISO_8859_1),size<=65536?size:0);
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);requestMetadata(request,head);headerView(request,head);
        when(request.toByteArray()).thenReturn(actual.array());
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        verify(annotations).setHighlightColor(size<=65536?HighlightColor.RED:HighlightColor.BLUE);
        verify(request,times(1)).toByteArray();verify(request,times(size<=65536?0:1)).withBody("");
        if(size>65536)assertEquals(0,actual.maxRead);
    }
    @ParameterizedTest @ValueSource(strings={"", "-1", "+1", "one", "1, 1", "999999999999999999999999999999", "65536", "65537"})
    void invalidOrOversizedDeclaredWholeSizeNeverCopiesTheOriginal(String declared) {
        String head="POST /tempcolorred HTTP/1.1\r\nContent-Length: "+declared+"\r\n\r\n";
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);requestMetadata(request,head);headerView(request,head);
        when(request.toByteArray()).thenThrow(new AssertionError("Rejected hint copied request"));
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        verify(annotations).setHighlightColor(HighlightColor.RED);verify(request,never()).toByteArray();verify(request,never()).body();
    }
    @ParameterizedTest @ValueSource(strings={"Content-Length: 12\r\nContent-Length: 12\r\n", "Content-Length: 12\r\nTransfer-Encoding: chunked\r\n", "Transfer-Encoding: chunked\r\n", ""})
    void absentAmbiguousOrTransferEncodedLengthsUseHeaderFallback(String fields) {
        String head="POST /permcolorblue HTTP/1.1\r\n"+fields+"\r\n";
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);requestMetadata(request,head);headerView(request,head);
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        verify(annotations).setHighlightColor(HighlightColor.BLUE);verify(request,never()).toByteArray();verify(request,never()).body();
    }
    @Test void oversizedHeaderOffsetRejectsEvenAZeroDeclaredBodyWithoutCopyingOriginal() {
        String head="POST /permcolorblue HTTP/1.1\r\nContent-Length: 0\r\n\r\n";
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);requestMetadata(request,head);headerView(request,head);
        when(request.bodyOffset()).thenReturn(65537);
        handler.handleHttpRequestToBeSent(request);verify(annotations).setHighlightColor(HighlightColor.BLUE);verify(request,never()).toByteArray();
    }
    @ParameterizedTest @ValueSource(ints={65535,65536,65537})
    void requestHeaderViewStillHasAnExclusiveByteBudget(int length) {
        byte[] head=new byte[length];java.util.Arrays.fill(head,(byte)'x');
        byte[] marker="tempcolorred".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(marker,0,head,length-marker.length,marker.length);
        var bytes=new ManualHighlightScannerTest.Bytes(head,Math.min(length,65536));
        var view=mock(HttpRequest.class);when(view.toByteArray()).thenReturn(bytes.array());
        var request=mock(HttpRequestToBeSent.class);when(request.withBody("")).thenReturn(view);when(request.annotations()).thenReturn(annotations);
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        if(length<=65536)verify(annotations).setHighlightColor(HighlightColor.RED);else verifyNoInteractions(annotations);
        assertTrue(bytes.maxRead<=65536);verify(request,never()).toByteArray();verify(request,never()).body();
    }
    @Test void malformedRequestCannotFallBackToFullMaterializationInLimitedMode() {
        var request=mock(HttpRequestToBeSent.class);when(request.annotations()).thenReturn(annotations);
        when(request.withBody("")).thenThrow(new MalformedRequestException("Synthetic malformed request"));
        assertSame(request,handler.handleHttpRequestToBeSent(request).request());
        verifyNoInteractions(annotations);verify(request,never()).toByteArray();verify(request,never()).body();
    }
    @Test void generalResponsesIgnoreTemporaryAndPreserveExistingAnnotationWhenNoPermanentMatches() {
        var response=mock(HttpResponseReceived.class);when(response.toByteArray()).thenReturn(ManualHighlightScannerTest.bytes("tempcolorred\0permcolorinvalid"));when(response.annotations()).thenReturn(annotations);
        when(annotations.highlightColor()).thenReturn(HighlightColor.YELLOW);
        assertSame(annotations,handler.handleHttpResponseReceived(response).annotations());
        verify(annotations,never()).setHighlightColor(any());assertEquals(HighlightColor.YELLOW,annotations.highlightColor());
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void proxyRemovalRemainsCompleteBeyondLoggerBudget(boolean limited) {
        when(settings.isLoggerInspectionLimited()).thenReturn(limited);
        String body="x".repeat(100_000)+"tempcolorblue-permcolorgreen-tempcolorred";
        var incoming=request("POST /tempcolorred HTTP/1.1\r\nHost: fixture\r\nContent-Length: "+body.length()+"\r\n\r\n"+body);
        String forwarded=handler.handleRequestReceived(incoming).request().toString();
        assertFalse(forwarded.contains("tempcolor"));assertTrue(forwarded.endsWith("-permcolorgreen-"));
        verify(annotations).setHighlightColor(HighlightColor.RED);
        verify(settings,never()).isLoggerInspectionLimited();
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
