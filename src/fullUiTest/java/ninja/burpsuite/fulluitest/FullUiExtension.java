// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import burp.api.montoya.*;
import ninja.burpsuite.extension.sharpener.*;
import java.nio.file.*;

/** Developer entry point, inert unless launched by the guarded full-test runner. */
public final class FullUiExtension implements BurpExtension {
    @Override public void initialize(MontoyaApi api) {
        String root=System.getProperty("sharpener.fullUi.root"), nonce=System.getProperty("sharpener.fullUi.nonce");
        if(root==null || nonce==null) {api.logging().logToError("Full UI harness is inert outside its disposable test runner.");return;}
        Thread.ofPlatform().daemon().name("SharpenerFullUiTests").start(()->{
            FullUiReport report=null;
            try {
                Path directory=Path.of(root).toRealPath();
                if(!Files.readString(directory.resolve("session.guard")).equals(nonce))throw new IllegalStateException("Invalid full UI session guard");
                Files.writeString(directory.resolve("host.ready"),"Developer extension loaded");
                Path jar=Path.of(System.getProperty("sharpener.fullUi.jar")).toRealPath();
                Path loaded=Path.of(ExtensionMainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
                if(!jar.equals(loaded))throw new IllegalStateException("Sharpener classes were not loaded from the requested packaged jar");
                report=new FullUiReport(directory,FullUiReport.sha256(jar));
                report.host.put("burp_build",String.valueOf(api.burpSuite().version().buildNumber()));
                report.host.put("edition",api.burpSuite().version().edition().name());
                String expected=System.getProperty("sharpener.fullUi.edition","auto");
                report.host.put("requested_edition",expected);
                if(!matchesEdition(expected,api.burpSuite().version().edition().name()))
                    throw new IllegalStateException("Host edition did not match requested "+expected);
                report.host.put("os",System.getProperty("os.name"));
                report.host.put("jre",System.getProperty("java.runtime.version"));
                String os=System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
                report.host.put("os_family",os.startsWith("windows")?"windows":os.contains("mac")?"macos":os.contains("linux")?"linux":"unsupported");
                new FullUiCases(api,report).run();
            } catch(Throwable failure) {
                if(report!=null)report.record("startup","FAIL",failure.getClass().getSimpleName()+": "+failure.getMessage());
                else api.logging().logToError("Full UI session could not start: "+failure.getClass().getSimpleName());
            } finally {
                try {if(report!=null)report.complete();}catch(Exception failure){api.logging().logToError("Could not write full UI report");}
                // This API belongs only to the process created by FullUiLauncher.
                try {api.burpSuite().shutdown();}catch(Exception ignored){}
            }
        });
    }
    static boolean matchesEdition(String requested,String actual) {
        return requested.equals("auto")||actual.replaceFirst("_EDITION$","").equalsIgnoreCase(requested);
    }
}
