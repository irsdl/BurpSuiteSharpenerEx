// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

final class FullUiReport {
    record Case(String id,String mode,String name) {}
    record Result(String id,String status,String evidence) {}
    static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    final Path directory;
    final String jarHash;
    final Map<String,Object> host=new LinkedHashMap<>();
    final Map<String,Result> results=new LinkedHashMap<>();
    final Map<String,String> controls=new TreeMap<>();
    FullUiReport(Path directory,String jarHash){this.directory=directory;this.jarHash=jarHash;}
    static List<Case> catalog() {
        try(var stream=FullUiReport.class.getResourceAsStream("/ui-cases.json")) {
            if(stream==null)throw new IllegalStateException("Missing case catalog");
            return List.of(JSON.fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Case[].class));
        } catch(IOException failure){throw new UncheckedIOException(failure);}
    }
    static void selfCheck() {
        FullUiBootstrap.selfCheck();
        if(!FullUiExtension.matchesEdition("community","COMMUNITY_EDITION")||!FullUiExtension.matchesEdition("professional","PROFESSIONAL")||
                FullUiExtension.matchesEdition("community","PROFESSIONAL"))throw new AssertionError("Edition validation failed");
        String before="Repeater/Find Title (Click > Use RegEx)/Search by RegEx";
        String after="Repeater/Find Title (Click > Next, Right-Click > Prev)/Search by RegEx";
        if(!FullUiCases.coveragePath(before).equals(FullUiCases.coveragePath(after))||
                FullUiCases.coveragePath(before).equals(FullUiCases.coveragePath(after+" new action")))
            throw new AssertionError("Dynamic caption coverage lost action identity");
        try {IsolatedPreferencesFactory.selfCheck();}catch(Exception failure){throw new IllegalStateException("Preference isolation check failed",failure);}
        Set<String> names=new HashSet<>();
        for(Case c:catalog()) {
            if(!names.add(c.id()) || c.name().isBlank() || !Set.of("automated","manual").contains(c.mode()))
                throw new IllegalStateException("Invalid full UI case catalog");
        }
        if(!names.containsAll(List.of("startup","menu-coverage","cleanup","native-desktop","host-persistence-reload")))
            throw new IllegalStateException("Missing full UI coverage gate");
        if(!escape("<script>\"&'").equals("&lt;script&gt;&quot;&amp;&#39;"))throw new IllegalStateException("Unsafe HTML report encoding");
        String filtered=IsolatedPreferencesFactory.filter("{\"extender\":{\"extensions\":[{\"path\":\"ordinary-extension.jar\"}]},\"unchanged\":17}");
        var config=JsonParser.parseString(filtered).getAsJsonObject();
        if(config.getAsJsonObject("extender").getAsJsonArray("extensions").size()!=0 || config.get("unchanged").getAsInt()!=17)
            throw new IllegalStateException("Full UI ordinary-extension isolation failed");
    }
    synchronized void record(String id,String status,String evidence) {
        if(catalog().stream().noneMatch(c->c.id().equals(id)))throw new IllegalArgumentException("Unknown case "+id);
        if(!Set.of("PASS","FAIL","BLOCKED","MANUAL_REQUIRED","NOT_APPLICABLE").contains(status))throw new IllegalArgumentException(status);
        results.put(id,new Result(id,status,evidence));
        try {Files.writeString(directory.resolve("progress.json"),JSON.toJson(results),StandardCharsets.UTF_8);}catch(IOException ignored){}
    }
    synchronized void complete() throws IOException {
        if(Files.exists(directory.resolve("launcher.blocked")))
            record("startup","BLOCKED","Launcher timeout/startup failure; late host completion cannot override this result.");
        for(Case c:catalog())results.putIfAbsent(c.id(),new Result(c.id(),c.mode().equals("manual")?"MANUAL_REQUIRED":"BLOCKED","Not executed; see case description and host logs."));
        boolean automated=catalog().stream().filter(c->c.mode().equals("automated")).allMatch(c->Set.of("PASS","NOT_APPLICABLE").contains(results.get(c.id()).status()));
        String overall=results.values().stream().anyMatch(r->r.status().equals("FAIL"))?"FAIL":
                results.values().stream().allMatch(r->Set.of("PASS","NOT_APPLICABLE").contains(r.status()))?"PASS":"INCOMPLETE";
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("schema_version",1);report.put("tested_utc",Instant.now().toString());report.put("jar_sha256",jarHash);
        report.put("catalog_sha256",catalogFingerprint());
        report.put("host",host);report.put("overall",overall);report.put("automated_passed",automated);
        report.put("scope","Real host Swing actions with real Montoya APIs except isolated Sharpener preference stores and synthetic context/editor events. Physical desktop and host persistence checks remain explicit manual cases.");
        report.put("cases",catalog().stream().map(c->Map.of("id",c.id(),"name",c.name(),"mode",c.mode(),"status",results.get(c.id()).status(),"evidence",results.get(c.id()).evidence())).toList());
        report.put("controls",controls);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("results.html"),html(overall),StandardCharsets.UTF_8);
        Map<String,Object> checklist=new LinkedHashMap<>();
        checklist.put("jar_sha256",jarHash);checklist.put("catalog_sha256",catalogFingerprint());checklist.put("host",host);checklist.put("reviewer","");checklist.put("tested_utc","");
        checklist.put("cases",catalog().stream().filter(c->c.mode().equals("manual")).map(c->Map.of("id",c.id(),"name",c.name(),"status","MANUAL_REQUIRED","evidence","")).toList());
        Files.writeString(directory.resolve("manual-checklist.json"),JSON.toJson(checklist),StandardCharsets.UTF_8);
        Path staged=Files.createTempFile(directory,"results-",".tmp");
        try {
            Files.writeString(staged,JSON.toJson(report),StandardCharsets.UTF_8);
            Files.move(staged,directory.resolve("results.json"),StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(staged);}
    }
    private String html(String overall) {
        StringBuilder out=new StringBuilder("<!doctype html><meta charset='utf-8'><title>Sharpener full UI tests</title><style>body{font:15px system-ui;margin:2rem}table{border-collapse:collapse;width:100%}td,th{border:1px solid #aaa;padding:.5rem;text-align:left}code{overflow-wrap:anywhere}</style><h1>Sharpener full UI tests: "+escape(overall)+"</h1><p>Jar SHA-256: <code>"+jarHash+"</code></p><p>Manual or blocked checks are not passes. This report does not certify untested OS/channel combinations.</p><table><tr><th>Case<th>Status<th>Evidence");
        for(Case c:catalog()) {Result r=results.get(c.id());out.append("<tr><td>").append(escape(c.id()+": "+c.name())).append("<td>").append(escape(r.status())).append("<td>").append(escape(r.evidence()));}
        out.append("</table><h2>Observed menu controls</h2><table><tr><th>Path<th>Coverage");
        controls.forEach((path,status)->out.append("<tr><td>").append(escape(path)).append("<td>").append(escape(status)));
        return out.append("</table>").toString();
    }
    static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    static String catalogFingerprint() {
        try(var stream=FullUiReport.class.getResourceAsStream("/ui-cases.json")) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Objects.requireNonNull(stream).readAllBytes()));
        } catch(Exception failure){throw new IllegalStateException("Cannot fingerprint full UI catalog",failure);}
    }
    static String sha256(Path file)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=Files.newInputStream(file)){byte[] buffer=new byte[65536];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);}
        return HexFormat.of().formatHex(digest.digest());
    }
}
