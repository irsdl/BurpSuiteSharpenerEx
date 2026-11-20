// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.editor.extension.*;
import ninja.burpsuite.extension.sharpener.*;
import ninja.burpsuite.extension.sharpener.uiControllers.subTabs.*;
import ninja.burpsuite.extension.sharpener.uiSelf.contextMenu.ContextMenu;
import ninja.burpsuite.extension.sharpener.uiSelf.httpRequestResponseEditor.ExtensionHttpRequestEditorProvider;
import ninja.burpsuite.libs.burp.generic.*;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.Preferences;
import ninja.burpsuite.libs.generic.WindowsAppUserModelId;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

final class FullUiCases {
    @FunctionalInterface interface Check {void run() throws Exception;}
    final MontoyaApi real;
    final FullUiStorage storage;
    final FullUiReport report;
    final ExtensionMainClass extension=new ExtensionMainClass();
    final Set<String> exercised=new HashSet<>();
    final Map<String,String> manual=new HashMap<>();
    ExtensionSharedParameters p;
    JFrame frame;
    String caseId;
    boolean nativeMode;
    ProxyHighlightingCases traffic;
    final List<Throwable> asynchronous=new java.util.concurrent.CopyOnWriteArrayList<>();
    FullUiCases(MontoyaApi real,FullUiReport report){this.real=real;this.storage=new FullUiStorage(real);this.report=report;}

    void run() throws Exception {
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,failure)->{asynchronous.add(failure);if(previous!=null)previous.uncaughtException(thread,failure);});
        try {
            check("startup",()->{
                extension.initialize(storage.api);
                Field field=ExtensionMainClass.class.getDeclaredField("sharedParameters");field.setAccessible(true);p=(ExtensionSharedParameters)field.get(extension);
                frame=p.get_mainFrameUsingMontoya();
                Ui.require(frame!=null && p.allSettings!=null && p.allSettings.isLoaded(),"Core UI failed to initialize");
                Ui.await(()->p.settingsSurface!=null && p.settingsSurface.component()!=null && Ui.settings(settingsRoot()).size()>20,"Settings surface did not populate");
                Ui.require(p.topMenuBar!=null && Ui.menu(p.topMenuBar).size()>20,"Full Sharpener top menu missing");
                Ui.await(()->p.topMenuBar.isShowing() && p.topMenuBar.getWidth()>0 && p.topMenuBar.getHeight()>0
                        && SwingUtilities.isDescendingFrom(p.topMenuBar,frame.getJMenuBar()),"Sharpener menu caption is not visible in Burp's menu bar");
                Ui.require(p.get_rootTabbedPaneUsingMontoya()!=null,"Main tabs unavailable");
                HttpRequest request=HttpRequest.httpRequestFromUrl("http://127.0.0.1:9/sharpener-full-ui-fixture");
                for(int i=1;i<=3;i++){real.repeater().sendToRepeater(request,"Sharpener-UI-"+i);real.intruder().sendToIntruder(request,"Sharpener-UI-"+i);}
                Ui.await(()->p.get_toolTabbedPane(BurpUITools.MainTabs.Repeater)!=null && p.get_toolTabbedPane(BurpUITools.MainTabs.Intruder)!=null,"Tool panes unavailable");
                Ui.run(()->p.allSettings.subTabsSettings.loadSettings());
                for(var tool:tools())Ui.await(()->handlers(tool).size()>=3,"Missing recognised tab headers in "+tool);
                Ui.require(storage.errors.isEmpty(),"Startup compatibility/errors: "+storage.errors);
            });
            if(p==null || frame==null || p.settingsSurface==null || p.settingsSurface.component()==null)return;
            check("settings-discovery",this::settingsDiscovery);
            check("main-styles",this::mainStyles);
            check("global-options",this::globalOptions);
            check("frame-title",this::frameTitle);
            check("frame-icons",this::frameIcons);
            check("about",()->dialogTop("About Sharpener","About this extension",d->{Ui.require(Ui.find(d,JOptionPane.class).stream().anyMatch(pane->String.valueOf(pane.getMessage()).contains("version")),"Missing about version");Ui.accept(d);}));
            check("shortcuts-dialog",this::shortcuts);
            check("native-form",()->{
                if(p.settingsSurface.isNative()) {
                    nativeMode=true;
                    try { mainStyles();globalOptions();frameTitle();frameIcons();shortcuts(); }
                    finally {nativeMode=false;closeSettings();}
                } else Ui.require(p.topMenuBar!=null,"Older hosts must keep the complete menu");
            });
            closeSettings();
            for(var tool:tools()) {
                String prefix=tool.toString().toLowerCase(Locale.ROOT);
                check(prefix+"-styles",()->subtabStyles(tool));
                check(prefix+"-titles",()->titles(tool));
                check(prefix+"-copy-style",()->copyStyle(tool));
            }
            check("clipboard",this::clipboard);
            check("screenshots",this::screenshots);
            check("mouse-wheel",this::wheel);
            check("context-menu",this::contextMenus);
            check("proxy-highlighting",()->{traffic=new ProxyHighlightingCases(real,p,report);traffic.proxy();});
            check("logger-highlighting",()->{if(traffic==null)throw new Ui.Blocked("Proxy fixtures unavailable");try{traffic.logger();}finally{traffic.close();}});
            check("request-editor",this::requestEditor);
            check("optional-ui",()->{
                Ui.require(!p.features.hasSuiteTab && !p.features.hasHttpResponseEditor,"Optional feature flags changed: add functional cases before enabling placeholders");
                Ui.require(!p.features.hasHttpRequestEditor,"Default request-editor registration changed: expand host registration case");
            });
            check("persistence",()->{
                Ui.run(()->{for(var tool:tools())p.allSettings.subTabsSettings.saveSettings(tool);});
                String json=FullUiReport.JSON.toJson(storage.project);
                var roundTrip=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                Ui.require(roundTrip.size()>0,"No isolated project settings were saved");
                Files.writeString(report.directory.resolve("fixture-preferences.json"),json);
            });
            check("unload-cancel",()->{
                dialogTop("Unload Extension","Sharpener Extension Unload",d->Ui.press(d,"No"));
                dialogTop("Remove All Settings & Unload","Sharpener Extension: Reset All Settings & Unload",d->Ui.press(d,"No"));
                Ui.require(!p.isUnloaded(),"Cancelled unload still unloaded extension");
            });
            manualTop("Project Page (Opens a Browser)","browser-links");manualTop("Report Bug/Feature (Opens a Browser)","browser-links");manualTop("Check for Update on Start","update-network");manual.put("Settings/Check for Update on Start","update-network");
            closeSettings();
            check("settings-navigation",()->{
                if(p.settingsSurface.canOpen())for(var tool:tools()) {
                    closeSettings();var h=handler(tool);var popup=popup(h);
                    SwingUtilities.invokeLater(()->Ui.click(Ui.item(popup,"Sharpener Settings...")));
                    Ui.await(()->{Ui.revealSettings(nativeRoot());return nativeRoot().isShowing();},"Context settings navigation failed");
                    exercised.add(tool+"/Sharpener Settings...");report.controls.put(tool+"/Sharpener Settings...",caseId);
                }
                closeSettings();
            });
            check("menu-coverage",this::coverage);
            check("cleanup",()->{
                nativeMode=p.settingsSurface.isNative();
                openSettings();
                Window settings=p.settingsSurface.isNative()?Ui.edt(()->SwingUtilities.getWindowAncestor(settingsRoot())):null;
                SwingUtilities.invokeLater(()->Ui.click(Ui.setting(settingsRoot(),"Project Settings/Change Burp Suite Title")));
                Ui.await(()->Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog d && d.isShowing() && d.getTitle().equals("Change Burp Suite Title")),"Pending owned dialog not displayed before unload");
                real.extension().unload();Ui.flush();
                Ui.await(p::isUnloaded,"Unload did not stop timers");
                Ui.require(p.backgroundTasks.isStopped(),"Workers survived unload");
                Ui.await(()->p.iconRefreshWindowFocusListener==null,"Icon refresh listener survived unload");
                Ui.require(Ui.edt(()->frame.getRootPane().getClientProperty(ExtensionMainClass.INSTANCE_MARKER_KEY))==null,"Instance marker survived unload");
                Ui.require(Ui.edt(()->!BurpUITools.isMenuBarLoaded("Sharpener",frame.getJMenuBar())),"Extension menu survived unload");
                Ui.await(()->Arrays.stream(Window.getWindows()).noneMatch(w->w instanceof JDialog d && d.isShowing() && d.getTitle().equals("Change Burp Suite Title")),"Owned dialog survived unload");
                if(settings!=null)Ui.require(Ui.edt(settings::isShowing),"Extension cleanup closed Burp's Settings window");
            });
        } finally {
            if(traffic!=null)traffic.close();
            if(p!=null && !p.isUnloaded())extension.extensionUnloaded();
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }
    void check(String id,Check action) {
        caseId=id;int before=asynchronous.size();
        try {action.run();Ui.flush();Ui.require(asynchronous.size()==before,"Uncaught UI exception: "+(asynchronous.size()>before?asynchronous.get(before):"unknown"));report.record(id,"PASS","Assertions passed in the live Burp host.");}
        catch(Ui.Blocked blocked){report.record(id,"BLOCKED",blocked.getMessage());}
        catch(Ui.NotApplicable absent){report.record(id,"NOT_APPLICABLE",absent.getMessage());}
        catch(Throwable failure){
            try(var out=new java.io.PrintWriter(Files.newBufferedWriter(report.directory.resolve(id+"-failure.txt")))){failure.printStackTrace(out);}catch(java.io.IOException ignored){}
            report.record(id,"FAIL",failure.getClass().getSimpleName()+": "+failure.getMessage());
        }
    }
    static List<BurpUITools.MainTabs> tools(){return List.of(BurpUITools.MainTabs.Repeater,BurpUITools.MainTabs.Intruder);}
    List<SubTabsContainerHandler> handlers(BurpUITools.MainTabs tool){var list=p.allSubTabContainerHandlers.get(tool);return list==null?List.of():list.stream().filter(h->h.isValid()&&!h.isDotDotDotTab()).toList();}
    SubTabsContainerHandler handler(BurpUITools.MainTabs tool){selectTool(tool);List<SubTabsContainerHandler> list=Ui.edt(()->handlers(tool));Ui.require(list.size()>=3,"Three fixture tabs needed in "+tool);return list.getFirst();}
    JPopupMenu popup(SubTabsContainerHandler handler) {
        return Ui.edt(()->{try{Method method=SubTabsActions.class.getDeclaredMethod("createPopupMenu",ExtensionSharedParameters.class,SubTabsContainerHandler.class);method.setAccessible(true);return (JPopupMenu)method.invoke(null,p,handler);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}});
    }
    void click(Component root,String path,String prefix) {
        Ui.run(()->{JMenuItem item=Ui.item(root,path);String actual=Ui.menu(root).entrySet().stream().filter(e->e.getValue()==item).findFirst().orElseThrow().getKey();Ui.click(item);exercised.add(prefix+actual);report.controls.put(prefix+actual,caseId);});Ui.flush();
    }
    Component nativeRoot(){return p.settingsSurface.component();}
    Component settingsRoot(){return nativeMode && p.settingsSurface.isNative()?nativeRoot():p.topMenuBar;}
    String controlPrefix(){return nativeMode?"Settings/":"Top/";}
    void openSettings() throws Exception {
        if(!p.settingsSurface.isNative() || Ui.edt(()->nativeRoot().isShowing()))return;
        SwingUtilities.invokeLater(()->{
            if(p.settingsSurface.canOpen())p.settingsSurface.open();
            else {
                var candidates=Ui.menu(frame.getJMenuBar()).entrySet().stream().filter(e->e.getKey().endsWith("Settings")||e.getKey().endsWith("Settings...")).toList();
                Ui.require(candidates.size()==1,"Open Burp Settings manually: navigation API is unavailable");Ui.click(candidates.getFirst().getValue());
            }
        });
        Ui.await(()->{Ui.revealSettings(nativeRoot());return nativeRoot().isShowing();},"Native Sharpener panel not discovered in actual Burp Settings");
    }
    void closeSettings() {
        if(p.settingsSurface.isNative())Ui.run(()->{Window settings=SwingUtilities.getWindowAncestor(nativeRoot());
            if(settings!=null && settings!=frame)settings.dispatchEvent(new WindowEvent(settings,WindowEvent.WINDOW_CLOSING));});
    }
    void settingsDiscovery() throws Exception {
        openSettings();
        if(p.settingsSurface.isNative()) {
            Ui.require(p.topMenuBar!=null,"Native Settings must not replace the full top menu");
            Ui.require(Ui.edt(()->nativeRoot().isShowing()),"Panel is not actually displayed");
            Ui.require(Ui.edt(()->Ui.find(nativeRoot(),JTextComponent.class).stream().anyMatch(l->l.getText().contains("this project"))),"Missing project scope help");
            closeSettings();openSettings();
            p.preferences.set("detectOffScreenPosition",false);Ui.flush();
            Ui.require(!Ui.edt(()->Ui.setting(nativeRoot(),"Global Settings/Detect Off Screen Window Position").isSelected()),"External preference change not refreshed");
            p.preferences.set("detectOffScreenPosition",true);Ui.flush();
            Ui.run(()->{
                Ui.find(nativeRoot(),JTabbedPane.class).getFirst().setSelectedIndex(0);
                var source=nativeRoot();var picture=new java.awt.image.BufferedImage(source.getWidth(),source.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D g=picture.createGraphics();try{source.printAll(g);}finally{g.dispose();}
                try{ImageIO.write(picture,"png",report.directory.resolve("native-settings.png").toFile());}catch(java.io.IOException e){throw new IllegalStateException(e);}
            });
            closeSettings();
        } else Ui.require(p.topMenuBar!=null,"Fallback host has no real menu");
    }
    void top(String path) {
        try{if(nativeMode)openSettings();else closeSettings();}catch(Exception e){throw new IllegalStateException(e);}
        Ui.run(()->{Ui.click(Ui.setting(settingsRoot(),path));exercised.add(controlPrefix()+path);report.controls.put(controlPrefix()+path,caseId);});Ui.flush();
    }
    void tab(SubTabsContainerHandler handler,String path){click(popup(handler),path,handler.currentToolTab+"/");}
    void dialogTop(String path,String title,java.util.function.Consumer<JDialog> respond)throws Exception {
        if(nativeMode)openSettings();else closeSettings();
        Ui.run(()->Ui.setting(settingsRoot(),path).reveal());Ui.flush();
        Ui.dialog(nativeMode?SwingUtilities.getWindowAncestor(nativeRoot()):frame,()->{var item=Ui.setting(settingsRoot(),path);exercised.add(controlPrefix()+path);Ui.click(item);},title,respond);report.controls.put(controlPrefix()+path,caseId);
    }
    void dialogTab(SubTabsContainerHandler handler,String path,String title,java.util.function.Consumer<JDialog> respond)throws Exception {
        JPopupMenu popup=popup(handler);String actual=Ui.edt(()->Ui.menu(popup).entrySet().stream().filter(e->e.getValue()==Ui.item(popup,path)).findFirst().orElseThrow().getKey());
        Ui.dialog(frame,()->{exercised.add(handler.currentToolTab+"/"+actual);Ui.click(Ui.item(popup,path));},title,respond);report.controls.put(handler.currentToolTab+"/"+actual,caseId);
    }

    List<String> topPaths(String prefix){return Ui.edt(()->Ui.settings(settingsRoot()).keySet().stream().filter(path->path.startsWith(prefix)).toList());}
    void mainStyles() throws Exception {
        String style="Global Settings/Tools' Template And Style/";
        top(style+"Enable All");
        for(String path:topPaths(style+"Icons' Theme/")) {
            if(path.endsWith("custom directory")) {
                Path fixtures=Files.createDirectories(report.directory.resolve("theme"));
                var icon=ImageIO.read(ExtensionMainClass.class.getResource("/sharpener.png"));
                for(var tool:BurpUITools.MainTabs.values())ImageIO.write(icon,"png",fixtures.resolve(tool+".png").toFile());
                dialogTop(path,"@file",d->{var chooser=Ui.find(d,JFileChooser.class).getFirst();chooser.setSelectedFile(fixtures.toFile());chooser.approveSelection();});
                Ui.require("custom".equals(p.preferences.safeGetStringSetting("ToolsThemeName")),"Custom theme not selected");
            } else {top(path);String name=path.substring(path.lastIndexOf('/')+1);Ui.require((name.equals("none")?"":name).equals(p.preferences.safeGetStringSetting("ToolsThemeName")),"Theme preference did not change");}
        }
        for(String path:topPaths(style+"Icons' Size/")){top(path);Ui.require(path.endsWith("/"+p.preferences.safeGetStringSetting("ToolsIconSize")),"Icon size did not change");}
        top(style+"Disable All");
        for(String path:topPaths(style))if(!path.substring(style.length()).contains("/")&&!path.endsWith("All")) {
            boolean before=Ui.edt(()->Ui.setting(settingsRoot(),path).isSelected());top(path);
            Ui.require(Ui.edt(()->Ui.setting(settingsRoot(),path).isSelected())!=before,"Tool checkbox did not toggle: "+path);top(path);
        }
        top(style+"Icons' Theme/@irsdl");top(style+"Icons' Size/16");
        top(style+"Proxy");
        Ui.await(()->{var tabs=p.get_rootTabbedPaneUsingMontoya();for(int i=0;i<tabs.getTabCount();i++)if("Proxy".equals(tabs.getTitleAt(i)) && tabs.getTabComponentAt(i) instanceof JComponent header)return header.getClientProperty("Sharpener.mainTab.icon")!=null;return false;},"Main Proxy header did not receive its Sharpener icon");
        top(style+"Proxy");
    }
    void globalOptions() {
        Map<String,String> options=Map.of("Scrollable Tool Pane","isToolTabPaneScrollable","Use Last Screen Position And Size When Opened","useLastScreenPositionAndSize","Detect Off Screen Window Position","detectOffScreenPosition");
        for(var entry:options.entrySet()) {
            boolean before=p.preferences.safeGetBooleanSetting(entry.getValue());top("Global Settings/"+entry.getKey());
            Ui.require(p.preferences.safeGetBooleanSetting(entry.getValue())!=before,"Global preference did not toggle");top("Global Settings/"+entry.getKey());
        }
        for(String path:topPaths("Global Settings/Supported Capabilities/")){top(path);top(path);}
        for(String path:topPaths("Global Settings/Debug Settings/"))top(path);
        top("Global Settings/Debug Settings/Disabled");
    }
    void frameTitle() throws Exception {
        String original=Ui.edt(frame::getTitle);
        dialogTop("Project Settings/Change Burp Suite Title","Change Burp Suite Title",d->{Ui.text(d,"Sharpener full UI fixture");Ui.accept(d);});
        Ui.await(()->frame.getTitle().equals("Sharpener full UI fixture"),"Title not applied; saved="+p.preferences.safeGetStringSetting("BurpTitle")+", frame="+Ui.edt(frame::getTitle));
        dialogTop("Project Settings/Change Burp Suite Title","Change Burp Suite Title",d->d.dispose());
        Ui.require(Ui.edt(frame::getTitle).equals("Sharpener full UI fixture"),"Cancelled title changed the window");
        dialogTop("Project Settings/Reset Burp Suite Title","Sharpener Extension: Reset Title",d->Ui.press(d,"Yes"));
        Ui.require(Ui.edt(frame::getTitle).equals(original),"Original title not restored");
    }
    void frameIcons() throws Exception {
        for(String path:topPaths("Project Settings/Change Burp Suite Icon/")) {
            if(path.endsWith("/Custom"))continue;
            top(path);Ui.await(()->frame.getIconImages().size()==6,"Application icon not installed");
            Ui.require(!p.preferences.safeGetStringSetting("BurpResourceIconName").isBlank(),"Bundled icon not saved");
        }
        Path custom=report.directory.resolve("custom-icon.png");
        ImageIO.write(ImageIO.read(ExtensionMainClass.class.getResource("/sharpener.png")),"png",custom.toFile());
        dialogTop("Project Settings/Change Burp Suite Icon/Custom","@file",d->{var chooser=Ui.find(d,JFileChooser.class).getFirst();chooser.setSelectedFile(custom.toFile());chooser.approveSelection();});
        Ui.await(()->p.preferences.safeGetStringSetting("BurpIconCustomPath").equals(custom.toString()),"Custom application icon not saved");
        dialogTop("Project Settings/Change Burp Suite Icon/Custom","@file",d->Ui.find(d,JFileChooser.class).getFirst().cancelSelection());
        if(WindowsAppUserModelId.isWindows())Ui.await(()->WindowsAppUserModelId.getTaskbarProperties(frame).iconResource()!=null,"Windows taskbar resource not installed");
        dialogTop("Project Settings/Reset Burp Suite Icon","Sharpener Extension: Reset Icon",d->Ui.press(d,"Yes"));
        Ui.await(()->p.originalWindowTaskbarProperties.isEmpty()&&p.iconRefreshWindowFocusListener==null,"Icon state not restored");
    }
    void shortcuts() throws Exception {
        dialogTop("Keyboard Shortcuts","Sharpener Keyboard Shortcuts",d->{var table=Ui.find(d,JTable.class).getFirst();Ui.require(table.getRowCount()>=10,"Missing shortcut rows");Ui.press(d,"Save");});
        dialogTop("Keyboard Shortcuts","Sharpener Keyboard Shortcuts",d->Ui.press(d,"Close"));
        // Reset's confirmation and reopened dialog run inside nested Swing event loops.
        SwingUtilities.invokeLater(()->Ui.click(Ui.setting(settingsRoot(),"Keyboard Shortcuts")));
        Ui.await(()->Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Sharpener Keyboard Shortcuts")),"Shortcut dialog did not open");
        SwingUtilities.invokeLater(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Sharpener Keyboard Shortcuts"))Ui.press(d,"Reset to Defaults");});
        Ui.await(()->Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Reset Shortcuts")),"Shortcut reset confirmation missing");
        Ui.run(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Reset Shortcuts"))Ui.press(d,UIManager.getString("OptionPane.yesButtonText"));});
        Ui.await(()->Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Sharpener Keyboard Shortcuts")),"Shortcuts did not reopen after reset");
        Ui.run(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Sharpener Keyboard Shortcuts"))Ui.press(d,"Close");});
    }
    void subtabStyles(BurpUITools.MainTabs tool)throws Exception {
        var h=handler(tool);
        for(String path:Ui.edt(()->new ArrayList<>(Ui.menu(popup(h)).keySet()))) {
            if(path.startsWith("Predefined Styles/")){tab(h,path);Ui.require(h.hasIcon(),"Preset failed to add icon: "+path);}
            else if(path.startsWith("Custom Style/Font Name/")){tab(h,path);Ui.require(h.getFontName().equals(path.substring(path.lastIndexOf('/')+1)),"Font did not change");}
            else if(path.startsWith("Custom Style/Font Size/")){tab(h,path);Ui.require(h.getFontSize()==Float.parseFloat(path.substring(path.lastIndexOf('/')+1)),"Font size did not change");}
            else if(path.startsWith("Custom Style/Icon/")){tab(h,path);Ui.require(h.hasIcon()!=path.endsWith("/None"),"Tab icon did not change");}
        }
        boolean bold=h.isBold(),italic=h.isItalic();tab(h,"Custom Style/Bold");tab(h,"Custom Style/Italic");
        Ui.require(h.isBold()!=bold&&h.isItalic()!=italic,"Bold/italic did not toggle");
        dialogTab(h,"Custom Style/Set Foreground Color","Choose a Color",d->{Ui.find(d,JColorChooser.class).getFirst().setColor(Color.MAGENTA);Ui.accept(d);});
        Ui.require(h.getColor().equals(Color.MAGENTA),"Foreground color did not apply");
        tab(h,"Reset to Default");Ui.require(!h.hasIcon(),"Reset did not remove owned tab icon");
    }
    String pathContaining(SubTabsContainerHandler h,String text){return Ui.edt(()->Ui.menu(popup(h)).keySet().stream().filter(path->path.contains(text)).findFirst().orElseThrow(()->new AssertionError("Missing "+text)));}
    void titles(BurpUITools.MainTabs tool)throws Exception {
        var h=handler(tool);
        Ui.run(()->{int i=0;for(var item:handlers(tool))item.setTabTitle("Sharpener-UI-"+(++i),true);});
        dialogTab(h,"Rename Title","Rename Title",d->{Ui.text(d,"Sharpener-UI-renamed");Ui.accept(d);});
        Ui.require(h.getTabTitle().equals("Sharpener-UI-renamed"),"Rename failed");
        dialogTab(h,pathContaining(h,"Search by RegEx"),"Search in titles and jump to tab",d->{Ui.text(d,"Sharpener-UI");Ui.accept(d);});
        tab(h,pathContaining(h,"/Next "));tab(h,pathContaining(h,"/Previous "));
        dialogTab(h,"Match/Replace Titles (Use RegEx)","Title Match and Replace (RegEx)",d->{var fields=Ui.find(d,JTextField.class);Ui.require(fields.size()==2,"Missing regex inputs");fields.get(0).setText("renamed");fields.get(1).setText("changed");Ui.accept(d);});
        Ui.require(h.getTabTitle().contains("changed"),"Regex replacement failed");
        String history=Ui.edt(()->Ui.menu(popup(h)).keySet().stream().filter(path->path.startsWith("Previous Titles/Set/")).findFirst().orElseThrow());tab(h,history);
        for(String action:List.of("First Tab","Last Tab","Previous Tab","Next Tab","Back","Forward")) {
            String path=pathContaining(h,"/"+action+" ");tab(h,path);
        }
        var pane=p.get_toolTabbedPane(tool);
        tab(h,pathContaining(h,"/First Tab "));Ui.require(pane.getSelectedIndex()==0,"First-tab navigation failed");
        tab(h,"Previous Titles/Clear History");
        dialogTab(h,"Keyboard Shortcuts","Sharpener Keyboard Shortcuts",d->Ui.press(d,"Close"));
    }
    void copyStyle(BurpUITools.MainTabs tool)throws Exception {
        var h=handler(tool);var other=Ui.edt(()->handlers(tool).get(1));
        tab(h,"Predefined Styles/High - Confirmed");tab(h,"Copy Style");tab(other,"Paste Style");
        Ui.require(h.getColor().equals(other.getColor())&&other.hasIcon(),"Paste style did not transfer color/icon");
        dialogTab(h,"Find/Paste Style (Use RegEx in Title)","Search in titles and replace their style",d->{Ui.text(d,"Sharpener-UI");Ui.accept(d);});
        dialogTab(h,"Paste Style For All Visible Tabs","Sharpener Extension: Changing all visible tabs' styles",d->Ui.press(d,"Yes"));
        Ui.require(Ui.edt(()->handlers(tool).stream().allMatch(item->item.getColor().equals(h.getColor()))),"Batch paste missed fixture tabs");
    }

    void clipboard() throws Exception {
        if(!Boolean.getBoolean("sharpener.fullUi.clipboard"))throw new Ui.Blocked("Clipboard tests require -PfullUiClipboard=true. The default does not overwrite the desktop clipboard.");
        Clipboard clipboard=Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents=clipboard.getContents(null);
        if(contents!=null && Arrays.stream(contents.getTransferDataFlavors()).anyMatch(flavor->!flavor.equals(DataFlavor.stringFlavor)&&!flavor.isMimeTypeEqual("text/plain")))
            throw new Ui.Blocked("Clipboard contains non-plain-text formats; copy plain fixture text before opting in, so rich user data is preserved.");
        String original=contents!=null&&contents.isDataFlavorSupported(DataFlavor.stringFlavor)?(String)contents.getTransferData(DataFlavor.stringFlavor):"";
        try {
            for(var tool:tools()) {
                var h=handler(tool);tab(h,"Copy Title");
                Ui.require(((String)clipboard.getData(DataFlavor.stringFlavor)).contains(h.getTabTitle()),"Title copy failed");
                clipboard.setContents(new StringSelection("Sharpener-UI-pasted"),null);tab(h,"Paste Title");
                Ui.await(()->h.getTabTitle().startsWith("Sharpener-UI-pasted"),"Title paste failed");
                String history=Ui.edt(()->Ui.menu(popup(h)).keySet().stream().filter(path->path.startsWith("Previous Titles/Copy/")).findFirst().orElseThrow());tab(h,history);
                tab(h,"Capture Screenshot/Clipboard");
                long deadline=System.nanoTime()+10_000_000_000L;
                while(!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)&&System.nanoTime()<deadline)Thread.sleep(30);
                Ui.require(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor),"Screenshot clipboard image missing");
            }
        } finally {clipboard.setContents(new StringSelection(original),null);}
    }
    void screenshots()throws Exception {
        Path directory=Files.createDirectories(report.directory.resolve("fixture-screenshots"));
        long savedBefore=storage.output.stream().filter(line->line.contains("Image file saved successfully: ")).count();
        for(var tool:tools()) {
            var h=handler(tool);selectTool(tool);Ui.run(()->h.parentTabbedPane.setSelectedIndex(h.getTabIndex()));
            Ui.await(()->h.parentTabbedPane.getSelectedComponent().getWidth()>0,"Selected fixture tab has no layout");
            dialogTab(h,"Capture Screenshot/File","@file",d->{var chooser=Ui.find(d,JFileChooser.class).getFirst();chooser.setSelectedFile(directory.toFile());chooser.approveSelection();});
        }
        Ui.await(()->storage.output.stream().filter(line->line.contains("Image file saved successfully: ")).count()>=savedBefore+2,"Screenshot workers did not finish writing PNGs");
        try(var files=Files.list(directory)){for(Path path:files.toList())Ui.require(ImageIO.read(path.toFile())!=null,"Invalid screenshot PNG");}
        var h=handler(BurpUITools.MainTabs.Repeater);dialogTab(h,"Capture Screenshot/File","@file",d->Ui.find(d,JFileChooser.class).getFirst().cancelSelection());
    }
    void selectTool(BurpUITools.MainTabs tool) {
        Ui.run(()->{var main=p.get_rootTabbedPaneUsingMontoya();Component content=BurpUIDiscovery.toolContent(main,tool);Ui.require(content!=null,"Missing main tool "+tool);main.setSelectedComponent(content);});Ui.flush();
    }
    void wheel() {
        for(var tool:tools()) {
            var h=handler(tool);String path=pathContaining(h,"Activate Mouse Wheel");boolean before=p.preferences.safeGetBooleanSetting("mouseWheelToScroll_"+tool);
            if(before)tab(h,path);tab(h,path);
            Ui.require(p.preferences.safeGetBooleanSetting("mouseWheelToScroll_"+tool),"Wheel option did not enable");
            Ui.run(()->{
                var pane=p.get_toolTabbedPane(tool);pane.setSelectedIndex(0);
                MouseWheelEvent event=new MouseWheelEvent(pane,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,2,2,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,1);
                for(var listener:pane.getMouseWheelListeners())listener.mouseWheelMoved(event);
                Ui.require(pane.getSelectedIndex()!=0,"Wheel event did not select another tab");
            });
            tab(h,path);Ui.require(!p.preferences.safeGetBooleanSetting("mouseWheelToScroll_"+tool),"Wheel option did not disable");
        }
    }
    void contextMenus() {
        ContextMenu provider=new ContextMenu(p);
        HttpRequest request=HttpRequest.httpRequestFromUrl("http://127.0.0.1:9/full-ui-context");
        HttpRequestResponse sample=HttpRequestResponse.httpRequestResponse(request,HttpResponse.httpResponse("HTTP/1.1 200 OK\r\nContent-Length: 7\r\n\r\nfixture"));
        ContextMenuEvent event=contextEvent(List.of(sample));
        Ui.run(()->{var items=provider.provideMenuItems(event);Ui.require(items.size()==2,"Request/response context actions missing");for(Component item:items){Ui.click((JMenuItem)item);report.controls.put("Context/"+((JMenuItem)item).getText(),caseId);}});
        Ui.require(storage.output.stream().anyMatch(s->s.contains("full-ui-context")),"Request print action produced no output");
        Ui.require(storage.output.stream().anyMatch(s->s.contains("fixture")),"Response print action produced no output");
        Ui.run(()->{var empty=provider.provideMenuItems(contextEvent(List.of()));Ui.require(empty==null||empty.isEmpty(),"Empty selection should have no actions");});
    }
    ContextMenuEvent contextEvent(List<HttpRequestResponse> selected) {
        return FullUiStorage.proxy(ContextMenuEvent.class,(object,method,args)->switch(method.getName()){
            case "isFromTool"->Arrays.asList((ToolType[])args[0]).contains(ToolType.REPEATER);
            case "selectedRequestResponses"->selected;case "messageEditorRequestResponse"->Optional.empty();case "selectedIssues"->List.of();
            default->throw new UnsupportedOperationException(method.getName());
        });
    }
    void requestEditor() {
        if(!p.features.hasHttpRequestEditor)throw new Ui.NotApplicable("Request editor is disabled by extension.properties and has no current user-facing UI. Enabling the feature activates these functional assertions.");
        Ui.run(()->{
            for(EditorMode mode:EditorMode.values()) {
                EditorCreationContext context=FullUiStorage.proxy(EditorCreationContext.class,(object,method,args)->{
                    if(method.getName().equals("editorMode"))return mode;throw new UnsupportedOperationException(method.getName());
                });
                var editor=new ExtensionHttpRequestEditorProvider(p).provideHttpRequestEditor(context);
                HttpRequest request=HttpRequest.httpRequestFromUrl("http://127.0.0.1:9/full-ui-editor");
                editor.setRequestResponse(HttpRequestResponse.httpRequestResponse(request,null));
                Ui.require(editor.getRequest().toString().equals(request.toString()),"Untouched request editor changed content");
                var text=Ui.find(editor.uiComponent(),JTextComponent.class).stream().filter(t->t.getText().contains("full-ui-editor")).findFirst().orElseThrow();
                Ui.require(text.isEditable()==(mode!=EditorMode.READ_ONLY),"Editor read-only mode mismatch");
                if(mode!=EditorMode.READ_ONLY){
                    String before=text.getText();text.setCaretPosition(text.getDocument().getLength());text.replaceSelection("full-ui-changed");String changed=text.getText();
                    Action undo=text.getActionMap().get("Undo"),redo=text.getActionMap().get("Redo");
                    Ui.require(undo!=null&&redo!=null,"Editor undo/redo actions missing");
                    undo.actionPerformed(new ActionEvent(text,ActionEvent.ACTION_PERFORMED,"Undo"));Ui.require(text.getText().equals(before),"Editor undo failed");
                    redo.actionPerformed(new ActionEvent(text,ActionEvent.ACTION_PERFORMED,"Redo"));Ui.require(text.getText().equals(changed),"Editor redo failed");
                    Ui.require(editor.getRequest().toString().contains("full-ui-changed"),"Edited text not returned");
                }
                Ui.require(editor.caption().equals("Sharpener"),"Editor caption missing");
            }
        });
    }
    void manualTop(String path,String id){manual.put("Top/"+path,id);report.controls.putIfAbsent("Top/"+path,"MANUAL: "+id);}
    static String coveragePath(String path) {
        return path.replace("/Find Title (Click > Use RegEx)/","/Find Title/")
                .replace("/Find Title (Click > Next, Right-Click > Prev)/","/Find Title/");
    }
    void coverage() {
        Map<String,Ui.Setting> observed=new TreeMap<>(Ui.edt(()->{Map<String,Ui.Setting> all=new TreeMap<>();
            Ui.settings(p.topMenuBar).forEach((path,item)->all.put("Top/"+path,item));
            if(p.settingsSurface.isNative())Ui.settings(nativeRoot()).forEach((path,item)->all.put("Settings/"+path,item));return all;}));
        for(var tool:tools())Ui.edt(()->{Ui.menu(popup(handler(tool))).forEach((path,item)->observed.put(tool+"/"+path,new Ui.Setting(item,-1)));return null;});
        List<String> missing=new ArrayList<>();
        Set<String> completed=exercised.stream().map(FullUiCases::coveragePath).collect(java.util.stream.Collectors.toSet());
        for(var entry:observed.entrySet()) {
            String path=entry.getKey();
            if(completed.contains(coveragePath(path)))continue;
            String manualCase=manual.get(path);
            if(manualCase==null && (path.contains("/Copy Title")||path.contains("/Paste Title")||path.contains("/Previous Titles/Copy/")||path.endsWith("/Capture Screenshot/Clipboard")))manualCase="clipboard";
            if(manualCase!=null)report.controls.put(path,"PENDING: "+manualCase);
            else if(!entry.getValue().isEnabled())report.controls.put(path,"DISABLED: fixture/state dependent; inspect underlying case");
            else {missing.add(path);report.controls.put(path,"UNCOVERED");}
        }
        Ui.require(missing.isEmpty(),"UI elements need test cases: "+missing);
    }
}
