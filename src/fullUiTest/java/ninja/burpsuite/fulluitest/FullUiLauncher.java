// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import com.google.gson.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/** Owns only a newly launched process and disposable paths; never attaches to user projects. */
public final class FullUiLauncher {
    public static void main(String[] args) throws Exception {
        if(args.length==4&&args[0].equals("--review")&&args[2].equals("--evidence")) {FullUiReview.run(Path.of(args[1]),Path.of(args[3]));return;}
        if (Arrays.asList(args).contains("--plan")) {
            FullUiReport.selfCheck();
            FullUiReport.catalog().forEach(c -> System.out.println(c.id()+" ["+c.mode()+"] "+c.name()));
            System.out.println("Harness checks passed. No Burp or GUI was launched.");
            return;
        }
        Map<String,String> options = new HashMap<>();
        for (int i=0;i<args.length;i++) {
            if (args[i].equals("--automated-only") || args[i].equals("--clipboard")) options.put(args[i],"true");
            else { if(i+1==args.length) throw new IllegalArgumentException("Missing value: "+args[i]); options.put(args[i],args[++i]); }
        }
        Path host=requiredFile(options,"--burp-jar"), java=requiredFile(options,"--java"), extension=requiredFile(options,"--extension-jar"), harness=requiredFile(options,"--harness-jar");
        int timeout=Integer.parseInt(options.getOrDefault("--timeout","600"));
        if(timeout<30 || timeout>1800)throw new IllegalArgumentException("Timeout must be 30..1800 seconds");
        String edition=options.getOrDefault("--edition","auto").toLowerCase(Locale.ROOT);
        if(!Set.of("auto","community","professional").contains(edition))throw new IllegalArgumentException("Edition must be auto, community or professional");
        Path output=Path.of(Objects.requireNonNull(options.get("--output"))).toAbsolutePath();
        Files.createDirectories(output);
        Path run=Files.createTempDirectory(output,"run-");
        Files.createDirectory(run.resolve("jvm-temp"));
        String nonce=UUID.randomUUID().toString();
        Files.writeString(run.resolve("session.guard"),nonce);
        Files.writeString(run.resolve("project-config.json"),"{\"project_options\":{}}");
        String main;
        try(JarFile jar=new JarFile(host.toFile())) { main=jar.getManifest().getMainAttributes().getValue("Main-Class"); }
        if(main==null || !main.startsWith("burp."))throw new IllegalArgumentException("Expected an official executable Burp jar");
        List<String> vm=new ArrayList<>(List.of("-Djava.awt.headless=false",
                "-Djava.io.tmpdir="+run.resolve("jvm-temp"),
                "-Djava.util.prefs.PreferencesFactory="+IsolatedPreferencesFactory.class.getName(),
                "-Dsharpener.fullUi.root="+run,"-Dsharpener.fullUi.nonce="+nonce,
                "-Dsharpener.fullUi.jar="+extension,"-Dsharpener.fullUi.clipboard="+options.containsKey("--clipboard"),
                "-Dsharpener.fullUi.edition="+edition));
        List<String> command=new ArrayList<>();
        if(options.containsKey("--native-launcher")) {
            Path launcher=requiredFile(options,"--native-launcher");command.add(launcher.toString());
            vm.forEach(value->command.add("-J"+value));
            Path runtime=launcher.getParent().resolve(".install4j/i4jruntime.jar");
            if(!Files.isRegularFile(runtime))throw new IllegalArgumentException("Expected an install4j launcher alongside .install4j/i4jruntime.jar");
            command.add("-J-javaagent:"+harness);
        } else {command.add(java.toString());command.addAll(vm);command.add("-javaagent:"+harness);command.addAll(List.of("-jar",host.toString()));}
        // Community uses a temporary project; it cannot open/create Pro project files.
        if(!edition.equals("community"))command.add("--project-file="+run.resolve("test.burp"));
        command.addAll(List.of("--disable-auto-update","--data-dir="+run.resolve("burp-data"),
                "--config-file="+run.resolve("project-config.json"),
                "--developer-extension-class-name="+FullUiExtension.class.getName()));
        Path report=run.resolve("results.json");
        System.out.println("Launching disposable Burp UI validation. Report directory: "+run);
        byte[] seed=IsolatedPreferencesFactory.captureBurpPreferences(edition);
        Process child=new ProcessBuilder(command).redirectOutput(run.resolve("burp.stdout.log").toFile()).redirectError(run.resolve("burp.stderr.log").toFile()).start();
        try {
            Files.writeString(run.resolve("host.pid"),String.valueOf(child.pid()));
            try(var input=new java.io.DataOutputStream(child.getOutputStream())){input.writeInt(seed.length);input.write(seed);input.flush();}
            Arrays.fill(seed,(byte)0);
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(timeout);
            while(child.isAlive() && !Files.exists(report) && !Files.exists(run.resolve("activation.blocked")) && System.nanoTime()<deadline)Thread.sleep(200);
            if(!Files.exists(report)) {
                Files.writeString(run.resolve("launcher.blocked"),"Startup timeout or host exit before completion");
                FullUiReport blocked=new FullUiReport(run,FullUiReport.sha256(extension));
                blocked.record("startup","BLOCKED",Files.exists(run.resolve("activation.blocked"))
                        ?"Existing activation was not available or startup guard failed. Test stopped; never activate a disposable test profile."
                        :"Burp did not produce results: check JRE/license/startup compatibility in the private logs. Timeout/process exit is never a pass.");
                blocked.complete();
            }
        } catch(Exception failure) {
            if(!Files.exists(report)) {
                Files.writeString(run.resolve("launcher.blocked"),"Launcher/bootstrap precondition failure");
                FullUiReport blocked=new FullUiReport(run,FullUiReport.sha256(extension));
                blocked.record("startup","BLOCKED","Launcher/bootstrap precondition failed: "+failure.getClass().getSimpleName()+". Inspect private host logs; no pass is inferred.");blocked.complete();
            }
            throw failure;
        } finally {
            Arrays.fill(seed,(byte)0);
            var descendants=child.descendants().toList();
            if(child.isAlive() && !child.waitFor(3,TimeUnit.SECONDS)) { child.destroy(); if(!child.waitFor(10,TimeUnit.SECONDS)) {child.destroyForcibly();child.waitFor(10,TimeUnit.SECONDS);} }
            for(ProcessHandle descendant:descendants)if(descendant.isAlive())descendant.destroy();
        }
        JsonObject result=JsonParser.parseString(Files.readString(report)).getAsJsonObject();
        System.out.println("Full UI result: "+result.get("overall").getAsString()+"; "+report);
        boolean automatedPass=result.get("automated_passed").getAsBoolean();
        if(!automatedPass || (!options.containsKey("--automated-only") && !result.get("overall").getAsString().equals("PASS")))
            throw new IllegalStateException("Full UI validation is failed or incomplete. Read results.json and results.html; manual/blocked cases are not passes.");
        if(!result.get("overall").getAsString().equals("PASS"))System.out.println("Automated-only mode: manual full-suite checks remain pending.");
    }
    private static Path requiredFile(Map<String,String> options,String key) {
        Path path=Path.of(Objects.requireNonNull(options.get(key),"Missing "+key)).toAbsolutePath();
        if(!Files.isRegularFile(path))throw new IllegalArgumentException("Not a file for "+key);
        return path;
    }
}
