// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import burp.api.montoya.core.*;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.http.*;
import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;
import ninja.burpsuite.extension.sharpener.capabilities.implementations.*;
import ninja.burpsuite.extension.sharpener.capabilities.objects.CapabilitySettings;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;

/** Developer-only measurements against real host message/ByteArray/action implementations. No traffic is sent. */
final class ManualHighlighterPerformanceCases {
    final ExtensionSharedParameters parameters;
    final FullUiReport report;
    final ManualHighlighterSettings settings;
    final com.sun.management.ThreadMXBean allocations;
    final List<Map<String,Object>> rows=new ArrayList<>();
    static volatile Object apiResult;
    Constructor<?> baseline;
    ManualHighlighterPerformanceCases(ExtensionSharedParameters parameters,FullUiReport report) {
        this.parameters=parameters;this.report=report;
        settings=(ManualHighlighterSettings)parameters.allSettings.capabilitySettingsList.stream().filter(s->s instanceof ManualHighlighterSettings).findFirst().orElseThrow();
        var bean=ManagementFactory.getThreadMXBean();
        allocations=bean instanceof com.sun.management.ThreadMXBean allocationBean&&allocationBean.isThreadAllocatedMemorySupported()?allocationBean:null;
    }
    void run()throws Exception {
        String supplied=System.getProperty("sharpener.fullUi.highlighterBaseline");
        if(supplied==null)throw new Ui.NotApplicable("Measurements require an explicitly reviewed baseline jar and SHA-256; no performance pass inferred.");
        Path path=Path.of(supplied);
        String hash=FullUiReport.sha256(path);
        Ui.require(hash.equalsIgnoreCase(System.getProperty("sharpener.fullUi.highlighterBaselineSha256")),"Baseline changed since launcher check");
        loadBaseline(path);
        if(allocations==null)throw new Ui.Blocked("This host JVM cannot measure per-thread allocations");
        if(!allocations.isThreadAllocatedMemoryEnabled())allocations.setThreadAllocatedMemoryEnabled(true);
        boolean enabled=settings.isEnabled(),limited=settings.isLoggerInspectionLimited();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("baseline_jar_sha256",hash);result.put("candidate_jar_sha256",report.jarHash);result.put("host",report.host);
        result.put("scope","Same host/JVM; actual host HTTP messages, ByteArray slicing and actions; callback interfaces are developer proxies. Synthetic fixtures only, no network. Latencies include toByteArray, slice, scan, annotations and continueWith. Worker setup/warmup excluded. Concurrent cells use four threads; GC deltas are process-wide. Max scanner read does not prove toByteArray internals are bounded.");
        try {
            settings.setEnabled(true);settings.setLoggerInspectionLimited(false);
            parity();result.put("non_ascii_host_parity","PASS: literal byte scanner matches unchanged host String conversion with NUL, Latin-1 and UTF-8 surrounding bytes");
            result.put("host_api_components",apiComponents());
            for(int size:new int[]{1024,65536,1048576,10485760})for(String marker:List.of("absent","early","late"))for(String stage:List.of("request","response","proxy-rewrite")) {
                byte[] raw=fixture(size,marker,stage);
                // Alternate order to reduce systematic baseline-first JVM warmup bias.
                for(int workers:new int[]{1,4})for(String version:(size==65536||size==10485760?List.of("limited","unlimited","baseline"):List.of("baseline","limited","unlimited")))
                    measure(raw,marker,stage,version,workers);
            }
            for(String fast:List.of("disabled","cached"))for(String version:List.of("baseline","limited","unlimited"))for(int workers:new int[]{1,4})
                measure(fixture(10485760,"absent","request"),fast,"request",version,workers);
            result.put("result","PASS");
        } finally {
            settings.setEnabled(enabled);settings.setLoggerInspectionLimited(limited);
            result.put("cells",rows);
            Files.writeString(report.directory.resolve("manual-highlighter-performance.json"),FullUiReport.JSON.toJson(result));
        }
    }
    void loadBaseline(Path path)throws Exception {
        String name=ManualHighlighterRequestResponseHandler.class.getName();Map<String,byte[]> classes=new HashMap<>();
        try(JarFile jar=new JarFile(path.toFile())) {
            for(var entry:Collections.list(jar.entries()))if(entry.getName().equals(name.replace('.','/')+".class")||entry.getName().startsWith(name.replace('.','/')+"$"))
                try(var input=jar.getInputStream(entry)){classes.put(entry.getName().replace('/','.').replaceAll("\\.class$",""),input.readAllBytes());}
        }
        ClassLoader loader=new ClassLoader(getClass().getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String key,boolean resolve)throws ClassNotFoundException {
                if(!classes.containsKey(key))return super.loadClass(key,resolve);
                Class<?> type=findLoadedClass(key);if(type==null){byte[] bytes=classes.get(key);type=defineClass(key,bytes,0,bytes.length);}if(resolve)resolveClass(type);return type;
            }
        };
        baseline=loader.loadClass(name).getConstructor(ExtensionSharedParameters.class,CapabilitySettings.class);
    }
    Object handler(String version)throws Exception {return version.equals("baseline")?baseline.newInstance(parameters,settings):new ManualHighlighterRequestResponseHandler(parameters,settings);}
    List<Map<String,Object>> apiComponents() {
        List<Map<String,Object>> components=new ArrayList<>();
        for(int size:new int[]{1024,65536,1048576,10485760})for(String stage:List.of("request","response")) {
            byte[] raw=fixture(size,"absent",stage);
            var request=stage.equals("request")?HttpRequest.httpRequest(HttpService.httpService("127.0.0.1",9,false),ByteArray.byteArray(raw)):null;
            var response=stage.equals("response")?HttpResponse.httpResponse(ByteArray.byteArray(raw)):null;
            java.util.function.Supplier<ByteArray> bytes=()->request==null?response.toByteArray():request.toByteArray();
            ByteArray whole=bytes.get();
            for(String operation:List.of("toByteArray","boundedSliceAndGetBytes")) {
                java.util.function.Supplier<Object> action=operation.equals("toByteArray")?()->bytes.get():()->whole.subArray(0,Math.min(whole.length(),8207)).getBytes();
                try {
                    for(int warmup=0;warmup<5;warmup++)apiResult=action.get();
                    long[] nanos=new long[16];long before=allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());
                    for(int i=0;i<nanos.length;i++){long at=System.nanoTime();apiResult=action.get();nanos[i]=System.nanoTime()-at;}
                    long allocated=allocations.getThreadAllocatedBytes(Thread.currentThread().threadId())-before;Arrays.sort(nanos);
                    components.add(Map.of("message_bytes",size,"stage",stage,"operation",operation,"samples",nanos.length,"p50_ns",percentile(nanos,.50),"p95_ns",percentile(nanos,.95),"allocated_bytes_per_call",allocated/(double)nanos.length));
                } finally {apiResult=null;}
            }
        }
        return components;
    }
    void parity()throws Exception {
        for(String stage:List.of("request","response","proxy-rewrite"))for(String text:List.of("\u00ff\0tempcolorRED\u0080permcolorblue","\u00c3\u00a9permcolorPINK\u00ff","tempcolorinvalid\0permcolormagenta","plain\u00ff\0")) {
            byte[] raw=(stage.equals("response")?"HTTP/1.1 200 OK\r\n\r\n":"POST / HTTP/1.1\r\nHost: fixture\r\n\r\n").concat(text).getBytes(StandardCharsets.ISO_8859_1);
            Probe original=new Probe(raw,stage,false),candidate=new Probe(raw,stage,false);
            Object before=original.call(handler("baseline")),after=candidate.call(handler("unlimited"));
            Ui.require(original.annotations.highlightColor()==candidate.annotations.highlightColor(),"Host String/byte colour interpretation mismatch: "+stage);
            if(stage.equals("proxy-rewrite"))Ui.require(Arrays.equals(((ProxyRequestReceivedAction)before).request().toByteArray().getBytes(),((ProxyRequestReceivedAction)after).request().toByteArray().getBytes()),"Proxy protocol parity mismatch");
        }
    }
    static byte[] fixture(int size,String placement,String stage) {
        byte[] raw=new byte[size];Arrays.fill(raw,(byte)'x');
        byte[] prefix=(stage.equals("response")?"HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n\r\n":"POST / HTTP/1.1\r\nHost: fixture\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(prefix,0,raw,0,prefix.length);
        if(!placement.equals("absent")) {
            byte[] marker=(stage.equals("response")?"permcolorblue":"tempcolorred").getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(marker,0,raw,placement.equals("early")?prefix.length+3:raw.length-marker.length,marker.length);
        }
        return raw;
    }
    record Samples(long[] nanos,long allocated,int maxRead,long strings,long arrays) {}
    void measure(byte[] raw,String placement,String stage,String version,int workers)throws Exception {
        settings.setEnabled(!placement.equals("disabled"));settings.setLoggerInspectionLimited(version.equals("limited"));
        int iterations=placement.equals("cached")?16:placement.equals("disabled")?200:Math.max(4,Math.min(200,2_621_440/raw.length));
        Object target=handler(version);
        ExecutorService executor=Executors.newFixedThreadPool(workers);
        try {
            CountDownLatch ready=new CountDownLatch(workers),start=new CountDownLatch(1);
            List<Future<Samples>> futures=new ArrayList<>();
            for(int worker=0;worker<workers;worker++) {int workerId=worker;futures.add(executor.submit(()->{
                Probe probe=new Probe(raw,stage,placement.equals("cached"));probe.id=workerId*100+1;
                for(int warmup=0;warmup<3;warmup++){if(placement.equals("cached"))seed(target,probe.id);probe.call(target);}
                if(placement.equals("cached"))for(int i=0;i<iterations;i++)seed(target,workerId*100+i+1);
                probe.reset();long[] nanos=new long[iterations];ready.countDown();start.await();
                long before=allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());
                for(int i=0;i<iterations;i++) {
                    if(placement.equals("cached"))probe.id=workerId*100+i+1;
                    long at=System.nanoTime();probe.call(target);nanos[i]=System.nanoTime()-at;
                }
                long allocated=allocations.getThreadAllocatedBytes(Thread.currentThread().threadId())-before;
                HighlightColor expected=placement.equals("disabled")||placement.equals("absent")||(placement.equals("late")&&version.equals("limited")&&!stage.equals("proxy-rewrite")&&raw.length>65536)
                        ?HighlightColor.NONE:stage.equals("response")?HighlightColor.BLUE:HighlightColor.RED;
                Ui.require(probe.annotations.highlightColor()==expected,"Measured callback colour mismatch: "+version+" "+stage+" "+placement);
                if(placement.equals("disabled")||placement.equals("cached"))Ui.require(probe.arrays==0&&probe.strings==0,"Fast path read message content");
                if(!version.equals("baseline")&&!stage.equals("proxy-rewrite"))Ui.require(probe.strings==0,"General callback decoded a String");
                if(version.equals("limited")&&!stage.equals("proxy-rewrite"))Ui.require(probe.maxRead<=65536,"Scanner exceeded cutoff");
                return new Samples(nanos,allocated,probe.maxRead,probe.strings,probe.arrays);
            }));}
            if(!ready.await(60,TimeUnit.SECONDS))throw new IllegalStateException("Measurement workers did not start");
            long collections=gcCount(),gcMillis=gcMillis(),at=System.nanoTime();start.countDown();
            List<Samples> samples=new ArrayList<>();for(var future:futures)samples.add(future.get(90,TimeUnit.SECONDS));
            long elapsed=System.nanoTime()-at;
            long[] nanos=samples.stream().flatMapToLong(sample->Arrays.stream(sample.nanos())).sorted().toArray();
            Map<String,Object> row=new LinkedHashMap<>();row.put("version",version);row.put("stage",stage);row.put("placement",placement);row.put("message_bytes",raw.length);row.put("workers",workers);row.put("samples",nanos.length);
            row.put("p50_ns",percentile(nanos,.50));row.put("p95_ns",percentile(nanos,.95));row.put("p99_ns",percentile(nanos,.99));row.put("max_ns",nanos[nanos.length-1]);
            long allocated=samples.stream().mapToLong(Samples::allocated).sum();row.put("allocated_bytes_per_callback",allocated/(double)nanos.length);row.put("allocated_bytes_per_second",allocated/(elapsed/1e9));
            row.put("callbacks_per_second",nanos.length/(elapsed/1e9));row.put("gc_collections",gcCount()-collections);row.put("gc_millis",gcMillis()-gcMillis);
            row.put("max_scanner_read_exclusive",samples.stream().mapToInt(Samples::maxRead).max().orElse(0));row.put("message_string_calls",samples.stream().mapToLong(Samples::strings).sum());row.put("message_to_byte_array_calls",samples.stream().mapToLong(Samples::arrays).sum());
            row.put("message_bytes_materialized_as_string",samples.stream().anyMatch(sample->sample.strings()>0)?raw.length:0);
            rows.add(row);
        } finally {executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);}
    }
    static long percentile(long[] sorted,double fraction){return sorted[(int)Math.ceil(sorted.length*fraction)-1];}
    static long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean->Math.max(0,bean.getCollectionCount())).sum();}
    static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean->Math.max(0,bean.getCollectionTime())).sum();}
    static void seed(Object handler,int id)throws Exception {
        Field field=handler.getClass().getDeclaredField("pendingProxyColors");field.setAccessible(true);Object cache=field.get(handler);
        Method remember=cache.getClass().getDeclaredMethod("remember",int.class,HighlightColor.class);remember.setAccessible(true);remember.invoke(cache,id,HighlightColor.RED);
    }
    static final class Probe {
        final Annotations annotations=Annotations.annotations();
        final String stage;
        final Object message;
        int id=1,maxRead;
        long strings,arrays;
        Probe(byte[] raw,String stage,boolean proxy) {
            this.stage=stage;
            HttpService service=HttpService.httpService("127.0.0.1",9,false);
            var request=stage.equals("response")?null:HttpRequest.httpRequest(service,ByteArray.byteArray(raw));
            var response=stage.equals("response")?HttpResponse.httpResponse(ByteArray.byteArray(raw)):null;
            ToolSource source=(ToolSource)Proxy.newProxyInstance(ToolSource.class.getClassLoader(),new Class<?>[]{ToolSource.class},(p,m,args)->m.getName().equals("isFromTool")&&proxy
                    &&(args[0] instanceof ToolType[] types?Arrays.asList(types).contains(ToolType.PROXY):args[0]==ToolType.PROXY));
            Class<?> type=stage.equals("response")?HttpResponseReceived.class:stage.equals("proxy-rewrite")?InterceptedRequest.class:HttpRequestToBeSent.class;
            message=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,args)->switch(m.getName()) {
                case "annotations"->annotations;
                case "messageId"->id;
                case "toolSource"->source;
                case "httpService"->service;
                case "toString"->{strings++;yield response==null?request.toString():response.toString();}
                case "toByteArray"->{
                    arrays++;ByteArray bytes=response==null?request.toByteArray():response.toByteArray();
                    yield Proxy.newProxyInstance(ByteArray.class.getClassLoader(),new Class<?>[]{ByteArray.class},(p2,m2,args2)->{
                        if(m2.getName().equals("subArray"))maxRead=Math.max(maxRead,(int)args2[1]);
                        return m2.invoke(bytes,args2);
                    });
                }
                default->{
                    try {yield (response==null?HttpRequest.class:HttpResponse.class).getMethod(m.getName(),m.getParameterTypes()).invoke(response==null?request:response,args);}
                    catch(InvocationTargetException failure){throw failure.getCause();}
                }
            });
        }
        Object call(Object handler) {
            return switch(stage) {
                case "response"->((HttpHandler)handler).handleHttpResponseReceived((HttpResponseReceived)message);
                case "proxy-rewrite"->((ProxyRequestHandler)handler).handleRequestReceived((InterceptedRequest)message);
                default->((HttpHandler)handler).handleHttpRequestToBeSent((HttpRequestToBeSent)message);
            };
        }
        void reset(){strings=arrays=0;maxRead=0;}
    }
}
