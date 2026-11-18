// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.util.jar.JarFile;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Native-launcher test bootstrap: adds packaged classes, never transforms Burp bytecode. */
public final class FullUiBootstrap {
    public static void premain(String unused,Instrumentation instrumentation)throws Exception {
        Path root=Path.of(System.getProperty("sharpener.fullUi.root")).toRealPath();
        if(!Files.readString(root.resolve("session.guard")).equals(System.getProperty("sharpener.fullUi.nonce")))
            throw new IllegalStateException("Invalid full UI bootstrap guard");
        instrumentation.appendToSystemClassLoaderSearch(new JarFile(System.getProperty("sharpener.fullUi.jar")));
        Thread.ofPlatform().daemon().name("SharpenerFullUiStartupGuard").start(()->{
            long deadline=System.nanoTime()+TimeUnit.MINUTES.toNanos(30);
            try {
                while(System.nanoTime()<deadline&&!Files.exists(root.resolve("host.ready"))) {
                    boolean[] activation={false};
                    SwingUtilities.invokeAndWait(()->{
                        ArrayDeque<Component> pending=new ArrayDeque<>();
                        for(Window window:Window.getWindows())if(window.isShowing())pending.add(window);
                        int visited=0;
                        while(!pending.isEmpty()&&visited++<10000) {
                            Component component=pending.removeFirst();
                            if(component instanceof JLabel label&&activationPrompt(label.getText()))activation[0]=true;
                            if(component instanceof javax.swing.text.JTextComponent text&&!text.isEditable()&&activationPrompt(text.getText()))activation[0]=true;
                            if(component instanceof Container container)Collections.addAll(pending,container.getComponents());
                        }
                    });
                    if(activation[0]) {Files.writeString(root.resolve("activation.blocked"),"Activation prompt detected; no new activation is allowed for test runs.");return;}
                    Thread.sleep(250);
                }
            }catch(Throwable failure){try{Files.writeString(root.resolve("activation.blocked"),"Startup guard failed; test cannot proceed.");}catch(Exception ignored){}}
        });
    }
    private static boolean activationPrompt(String text) {
        if(text==null)return false;
        String plain=text.replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").strip().toLowerCase(Locale.ROOT);
        return plain.contains("activate license")||plain.contains("activate licence")||plain.contains("enter your license key")||plain.contains("enter your licence key");
    }
    static void selfCheck() {
        if(!activationPrompt("<html><b>Activate license</b></html>")||!activationPrompt("Enter your licence key")||activationPrompt("Burp Suite")||activationPrompt(null))
            throw new AssertionError("Activation prompt guard failed");
    }
}
