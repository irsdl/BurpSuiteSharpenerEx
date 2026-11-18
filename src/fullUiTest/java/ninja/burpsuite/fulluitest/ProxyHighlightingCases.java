// Burp Suite Extension Name: Sharpener
// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.core.Registration;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.proxy.http.*;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import com.google.gson.*;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import ninja.burpsuite.libs.burp.generic.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/** Explicit full-suite only: bounded HTTP fixtures sent exclusively over loopback. */
final class ProxyHighlightingCases implements AutoCloseable {
    record Wire(String target,String headers,String body,int contentLength) {}
    final MontoyaApi api;
    final ExtensionSharedParameters parameters;
    final FullUiReport report;
    final ServerSocket origin;
    final Thread server;
    final Map<String,Wire> received=new ConcurrentHashMap<>();
    final Map<String,String> replies=new ConcurrentHashMap<>();
    final Map<String,String> ids=new LinkedHashMap<>();
    final Map<HighlightColor,Integer> palette=new EnumMap<>(HighlightColor.class);
    final Map<String,Object> evidence=new LinkedHashMap<>();
    final Map<String,Map<String,Object>> messageIds=new ConcurrentHashMap<>();
    final List<Registration> observers=new ArrayList<>();
    final CapabilitySettings setting;
    final boolean initiallyEnabled;
    String originalOptions;
    boolean userOptions;
    boolean originallyIntercepting, closed, proxyPassed;
    int proxyPort;
    volatile Throwable serverFailure;

    ProxyHighlightingCases(MontoyaApi api,ExtensionSharedParameters parameters,FullUiReport report)throws Exception {
        this.api=api;this.parameters=parameters;this.report=report;
        setting=parameters.allSettings.capabilitySettingsList.stream().filter(c->c.capability.settingName.equals("manualHighlightCapability")).findFirst().orElseThrow();
        initiallyEnabled=setting.isEnabled();
        origin=new ServerSocket(0,16,InetAddress.getByName("127.0.0.1"));
        server=Thread.ofPlatform().daemon().name("SharpenerLocalHighlightFixture").start(this::serve);
    }
    void proxy()throws Exception {
        originalOptions=api.burpSuite().exportProjectOptionsAsJson();originallyIntercepting=api.proxy().isInterceptEnabled();
        JsonObject config=JsonParser.parseString(originalOptions).getAsJsonObject();
        JsonObject proxy=listenerOptions(config,0);
        if(proxy==null){userOptions=true;originalOptions=api.burpSuite().exportUserOptionsAsJson();config=JsonParser.parseString(originalOptions).getAsJsonObject();proxy=listenerOptions(config,0);}
        if(proxy==null){List<String> keys=new ArrayList<>();optionKeys(config,"",keys,0);Files.writeString(report.directory.resolve("fixture-config-keys.json"),FullUiReport.JSON.toJson(keys));throw new AssertionError("Proxy listener configuration schema unavailable; see fixture-config-keys.json");}
        try(ServerSocket reservation=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){proxyPort=reservation.getLocalPort();}
        JsonObject listener=new JsonObject();listener.addProperty("listener_port",proxyPort);listener.addProperty("listen_mode","loopback_only");listener.addProperty("running",true);listener.addProperty("certificate_mode","per_host");
        JsonArray listeners=new JsonArray();listeners.add(listener);proxy.add("request_listeners",listeners);
        if(userOptions)api.burpSuite().importUserOptionsFromJson(config.toString());else api.burpSuite().importProjectOptionsFromJson(config.toString());api.proxy().disableIntercept();
        Ui.run(()->setting.setEnabled(true));
        observers.add(api.proxy().registerRequestHandler(new ProxyRequestHandler(){
            public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest request){observe(request.headerValue("X-Sharpener-Test"),"proxy",request.messageId(),request.annotations().highlightColor());return ProxyRequestReceivedAction.continueWith(request,request.annotations());}
            public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest request){return ProxyRequestToBeSentAction.continueWith(request,request.annotations());}
        }));
        observers.add(api.http().registerHttpHandler(new HttpHandler(){
            public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request){observe(request.headerValue("X-Sharpener-Test"),"http",request.messageId(),request.annotations().highlightColor());return RequestToBeSentAction.continueWith(request,request.annotations());}
            public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response){return ResponseReceivedAction.continueWith(response,response.annotations());}
        }));
        await(()->{try(Socket socket=new Socket()){socket.connect(new InetSocketAddress("127.0.0.1",proxyPort),200);return true;}catch(IOException waiting){return false;}},"Loopback Proxy listener did not start");
        exchange("control","","plain","",HighlightColor.NONE);
        exchange("temporary","TempColorRED","start tempcolorBLUE middle TEMPCOLORRED end","",HighlightColor.RED);
        Wire temporary=received.get(ids.get("temporary"));
        Ui.require(!temporary.target().toLowerCase(Locale.ROOT).contains("tempcolor")&&temporary.body().equals("start  middle  end"),"Proxy did not remove all temporary markers from URL/body");
        Ui.require(temporary.contentLength()==temporary.body().getBytes(StandardCharsets.ISO_8859_1).length,"Rewritten Content-Length does not match origin body");
        exchange("permanent","permcolorBLUE","plain","",HighlightColor.BLUE);
        Ui.require(received.get(ids.get("permanent")).target().contains("permcolorBLUE"),"Permanent request marker was removed");
        exchange("response","","plain","reply PERMCOLORGREEN",HighlightColor.GREEN);
        exchange("unknown","tempcolorinvalid/permcolorinvalid","plain","",HighlightColor.NONE);
        Ui.run(()->setting.setEnabled(false));
        exchange("disabled","tempcolorred","plain","permcolorblue",HighlightColor.NONE);
        Ui.require(received.get(ids.get("disabled")).target().contains("tempcolorred"),"Disabled highlighter changed request");
        Ui.run(()->setting.setEnabled(true));
        select(BurpUITools.MainTabs.Proxy);
        Ui.run(()->{
            Component content=BurpUIDiscovery.toolContent(parameters.get_rootTabbedPaneUsingMontoya(),BurpUITools.MainTabs.Proxy);
            for(JTabbedPane tabs:Ui.find(content,JTabbedPane.class))for(int i=0;i<tabs.getTabCount();i++)if(tabs.getTitleAt(i).equalsIgnoreCase("HTTP history"))tabs.setSelectedIndex(i);
        });
        for(var fixture:Map.of("control",HighlightColor.NONE,"temporary",HighlightColor.RED,"permanent",HighlightColor.BLUE,"response",HighlightColor.GREEN).entrySet())palette.put(fixture.getValue(),rowBackground(BurpUITools.MainTabs.Proxy,ids.get(fixture.getKey())));
        Ui.require(!palette.get(HighlightColor.RED).equals(palette.get(HighlightColor.NONE))&&!palette.get(HighlightColor.BLUE).equals(palette.get(HighlightColor.NONE)),"Proxy rows have annotations but no rendered highlight colour");
        evidence.put("proxy_rendered_palette",palette);proxyPassed=true;save();
    }
    void logger()throws Exception {
        if(!proxyPassed)throw new Ui.Blocked("Proxy fixture case must pass before Logger colour comparisons");
        select(BurpUITools.MainTabs.Logger);
        for(var fixture:Map.of("control",HighlightColor.NONE,"temporary",HighlightColor.RED,"permanent",HighlightColor.BLUE,"response",HighlightColor.GREEN).entrySet()) {
            int actual=rowBackground(BurpUITools.MainTabs.Logger,ids.get(fixture.getKey()));
            evidence.put("logger_"+fixture.getKey(),actual);
        }
        String id=id("other-tool");
        var request=HttpRequest.httpRequest(HttpService.httpService("127.0.0.1",origin.getLocalPort(),false),"GET /"+id+"/tempcolorRED HTTP/1.1\r\nHost: 127.0.0.1:"+origin.getLocalPort()+"\r\nX-Sharpener-Test: "+id+"\r\nConnection: close\r\n\r\n");
        api.http().sendRequest(request);
        await(()->received.containsKey(id),"Montoya HTTP fixture did not reach loopback origin");
        Ui.require(received.get(id).target().contains("tempcolorRED"),"Logger annotation rewrote non-Proxy traffic");
        evidence.put("logger_other_tool",rowBackground(BurpUITools.MainTabs.Logger,id));
        save();
        for(var fixture:Map.of("control",HighlightColor.NONE,"temporary",HighlightColor.RED,"permanent",HighlightColor.BLUE,"response",HighlightColor.GREEN).entrySet())
            Ui.require(Objects.equals(evidence.get("logger_"+fixture.getKey()),palette.get(fixture.getValue())),"Logger row colour differs from Proxy "+fixture.getValue()+" for "+fixture.getKey());
        Ui.require(Objects.equals(evidence.get("logger_other_tool"),palette.get(HighlightColor.RED)),"Other-tool Logger marker did not change rendered colour");
    }
    String id(String name){return "sharpener-highlight-"+name+"-"+UUID.randomUUID().toString().substring(0,8);}
    void observe(String id,String stage,int number,HighlightColor color){if(id!=null&&id.startsWith("sharpener-highlight-")){var values=messageIds.computeIfAbsent(id,ignored->new ConcurrentHashMap<>());values.put(stage+"_id",number);values.put(stage+"_color",color.name());}}
    static JsonObject listenerOptions(JsonElement element,int depth) {
        if(depth>16)return null;
        if(element.isJsonObject()) {
            JsonObject object=element.getAsJsonObject();if(object.has("request_listeners"))return object;
            for(var child:object.entrySet()){JsonObject found=listenerOptions(child.getValue(),depth+1);if(found!=null)return found;}
        }else if(element.isJsonArray())for(JsonElement child:element.getAsJsonArray()){JsonObject found=listenerOptions(child,depth+1);if(found!=null)return found;}
        return null;
    }
    static void optionKeys(JsonElement element,String path,List<String> keys,int depth) {
        if(depth>6||keys.size()>2000)return;
        if(element.isJsonObject())for(var child:element.getAsJsonObject().entrySet()){String name=path+"/"+child.getKey();keys.add(name);optionKeys(child.getValue(),name,keys,depth+1);}
    }
    void exchange(String name,String marker,String body,String reply,HighlightColor expected)throws Exception {
        String id=id(name);ids.put(name,id);replies.put(id,reply);
        byte[] payload=body.getBytes(StandardCharsets.ISO_8859_1);
        String target="/"+id+"/"+marker;
        String raw="POST http://127.0.0.1:"+origin.getLocalPort()+target+" HTTP/1.1\r\nHost: 127.0.0.1:"+origin.getLocalPort()+"\r\nX-Sharpener-Test: "+id+"\r\nContent-Length: "+payload.length+"\r\nConnection: close\r\n\r\n"+body;
        try(Socket socket=new Socket("127.0.0.1",proxyPort)){socket.setSoTimeout(10000);socket.getOutputStream().write(raw.getBytes(StandardCharsets.ISO_8859_1));socket.getOutputStream().flush();String response=new String(socket.getInputStream().readNBytes(65536),StandardCharsets.ISO_8859_1);Ui.require(response.contains("200 OK"),"Proxy fixture did not get successful origin response");}
        await(()->received.containsKey(id)&&history(id)!=null&&history(id).response()!=null,"Proxy history did not capture fixture "+name);
        ProxyHttpRequestResponse message=history(id);
        Ui.require(message.annotations().highlightColor()==expected,"Proxy "+name+" expected "+expected+" but got "+message.annotations().highlightColor());
        if(!reply.isEmpty())Ui.require(message.response().bodyToString().equals(reply),"Response marker/body changed");
        evidence.put(name,Map.of("highlight",expected.name(),"wire_body",received.get(id).body(),"content_length",received.get(id).contentLength()));
    }
    ProxyHttpRequestResponse history(String id){var matches=api.proxy().history(message->id.equals(message.request().headerValue("X-Sharpener-Test")));return matches.isEmpty()?null:matches.getFirst();}
    void select(BurpUITools.MainTabs tool){Ui.run(()->{var tabs=parameters.get_rootTabbedPaneUsingMontoya();Component content=BurpUIDiscovery.toolContent(tabs,tool);Ui.require(content!=null,"Tool unavailable: "+tool);tabs.setSelectedComponent(content);});Ui.flush();}
    int rowBackground(BurpUITools.MainTabs tool,String id)throws Exception {
        select(tool);int[] background={0};
        await(()->Ui.edt(()->{
            Component content=BurpUIDiscovery.toolContent(parameters.get_rootTabbedPaneUsingMontoya(),tool);
            for(JTable table:Ui.find(content,JTable.class))for(int row=0;row<Math.min(table.getRowCount(),200);row++)for(int col=0;col<table.getColumnCount();col++) {
                Object value=table.getValueAt(row,col);
                if(value!=null&&String.valueOf(value).contains(id)) {
                    table.clearSelection();table.scrollRectToVisible(table.getCellRect(row,col,true));
                    background[0]=table.prepareRenderer(table.getCellRenderer(row,col),row,col).getBackground().getRGB();return true;
                }
            }
            return false;
        }),"No visible "+tool+" table row for local fixture");
        return background[0];
    }
    void await(java.util.function.BooleanSupplier condition,String message)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline){if(serverFailure!=null)throw new IOException("Loopback origin failed",serverFailure);if(condition.getAsBoolean())return;Thread.sleep(40);}
        throw new AssertionError(message);
    }
    static String line(InputStream input)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();int value;
        while((value=input.read())!=-1&&value!='\n'){if(bytes.size()>16384)throw new IOException("Fixture header limit exceeded");if(value!='\r')bytes.write(value);}
        return bytes.toString(StandardCharsets.ISO_8859_1);
    }
    void serve() {
        try {
            while(!origin.isClosed())try(Socket socket=origin.accept()) {
                socket.setSoTimeout(5000);InputStream input=socket.getInputStream();String first=line(input);if(first.isBlank())continue;
                StringBuilder headers=new StringBuilder();String id="";int length=0;
                for(int count=0;count<100;count++){String header=line(input);if(header.isEmpty())break;headers.append(header).append('\n');int colon=header.indexOf(':');if(colon>0){String key=header.substring(0,colon);String value=header.substring(colon+1).strip();if(key.equalsIgnoreCase("Content-Length"))length=Integer.parseInt(value);if(key.equalsIgnoreCase("X-Sharpener-Test"))id=value;}}
                if(length<0||length>65536)throw new IOException("Fixture body limit exceeded");byte[] body=input.readNBytes(length);if(body.length!=length)throw new EOFException("Origin body shorter than Content-Length");
                received.put(id,new Wire(first,headers.toString(),new String(body,StandardCharsets.ISO_8859_1),length));
                byte[] reply=replies.getOrDefault(id,"").getBytes(StandardCharsets.ISO_8859_1);
                OutputStream output=socket.getOutputStream();output.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "+reply.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));output.write(reply);output.flush();
            }
        }catch(Throwable failure){if(!origin.isClosed())serverFailure=failure;}
    }
    void save()throws IOException {evidence.put("message_ids",messageIds);Files.writeString(report.directory.resolve("fixture-highlighting.json"),FullUiReport.JSON.toJson(evidence));}
    @Override public void close()throws Exception {
        if(closed)return;closed=true;origin.close();server.join(2000);
        for(Registration observer:observers)observer.deregister();
        Ui.run(()->setting.setEnabled(initiallyEnabled));
        if(originalOptions!=null){if(userOptions)api.burpSuite().importUserOptionsFromJson(originalOptions);else api.burpSuite().importProjectOptionsFromJson(originalOptions);if(originallyIntercepting)api.proxy().enableIntercept();else api.proxy().disableIntercept();}
    }
}
