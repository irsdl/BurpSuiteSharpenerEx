// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import ninja.burpsuite.libs.burp.generic.BurpUIDiscovery;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

final class Ui {
    static final class Blocked extends RuntimeException {Blocked(String message){super(message);}}
    static final class NotApplicable extends RuntimeException {NotApplicable(String message){super(message);}}
    static <T>T edt(Supplier<T> action){return BurpUIDiscovery.onEdt(action);}
    static void run(Runnable action){edt(()->{action.run();return null;});}
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void await(BooleanSupplier condition,String message)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline){if(edt(condition::getAsBoolean))return;Thread.sleep(30);}
        throw new AssertionError(message);
    }
    static void flush(){run(()->{});run(()->{});run(()->{});}
    static List<Component> children(Component root){return root instanceof JMenu m?List.of(m.getMenuComponents()):root instanceof Container c?List.of(c.getComponents()):List.of();}
    static <T>List<T> find(Component root,Class<T> type) {
        List<T> found=new ArrayList<>();ArrayDeque<Component> queue=new ArrayDeque<>();queue.add(root);
        int count=0;
        while(!queue.isEmpty() && ++count<20000){Component c=queue.removeFirst();if(type.isInstance(c))found.add(type.cast(c));queue.addAll(children(c));}
        return found;
    }
    static LinkedHashMap<String,JMenuItem> menu(Component root) {
        LinkedHashMap<String,JMenuItem> found=new LinkedHashMap<>();walkMenu(root,"",found);return found;
    }
    private static void walkMenu(Component root,String prefix,Map<String,JMenuItem> found) {
        for(Component child:children(root))if(child instanceof JMenuItem item) {
            String text=item.getText()==null?"":item.getText();
            String path=prefix.isEmpty()?text:prefix+"/"+text;
            if(item instanceof JMenu)walkMenu(item,path,found);else if(item.getActionListeners().length>0)found.put(path,item);
        }
    }
    static JMenuItem item(Component root,String path) {
        var matches=menu(root).entrySet().stream().filter(e->e.getKey().equals(path)||e.getKey().startsWith(path+" (")||e.getKey().startsWith(path+" [")).toList();
        require(matches.size()==1,"Expected unique menu control: "+path+", found "+matches.size());
        return matches.getFirst().getValue();
    }
    static void click(JMenuItem item){require(item.isEnabled(),"Disabled menu item: "+item.getText());item.doClick(0);}
    static boolean owned(Window window,Window parent){for(Window p=window.getOwner();p!=null;p=p.getOwner())if(p==parent)return true;return false;}
    static void dialog(JFrame parent,Runnable action,String title,Consumer<JDialog> respond)throws Exception {
        CompletableFuture<Void> complete=new CompletableFuture<>();
        SwingUtilities.invokeLater(()->{try{action.run();complete.complete(null);}catch(Throwable failure){complete.completeExceptionally(failure);}});
        try {
            await(()->Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog d&&d.isShowing()&&owned(d,parent)&&matches(d,title)),"Dialog did not appear: "+title);
            run(()->{
                JDialog dialog=Arrays.stream(Window.getWindows()).filter(w->w instanceof JDialog d&&d.isShowing()&&owned(d,parent)&&matches(d,title)).map(w->(JDialog)w).findFirst().orElseThrow();
                require(dialog.getOwner()!=null,"Unowned dialog: "+title);respond.accept(dialog);
            });
            complete.get(10,TimeUnit.SECONDS);
        } finally {
            run(()->{for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&owned(dialog,parent)&&matches(dialog,title))dialog.dispose();});
        }
        flush();
    }
    private static boolean matches(JDialog dialog,String title){return title.equals("@file")?!find(dialog,JFileChooser.class).isEmpty():dialog.getTitle().equals(title);}
    static void press(JDialog dialog,String label) {
        var button=find(dialog,JButton.class).stream().filter(b->label.equals(b.getText())).findFirst().orElseThrow(()->new AssertionError("Missing dialog button "+label));
        button.doClick(0);
    }
    static void accept(JDialog dialog){
        String ok=UIManager.getString("OptionPane.okButtonText");
        var buttons=find(dialog,JButton.class).stream().filter(b->Objects.equals(b.getText(),ok)).toList();
        JButton button=buttons.size()==1?buttons.getFirst():dialog.getRootPane().getDefaultButton();
        require(button!=null,"No accept dialog button");
        try {
            JButton defaultButton=dialog.getRootPane().getDefaultButton();
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("sharpener.fullUi.root"),"dialog-actions.log"),
                    dialog.getTitle()+": default="+(defaultButton==null?"none":defaultButton.getText())+", chosen="+button.getText()+"\n",
                    java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
        }catch(java.io.IOException ignored){}
        SwingUtilities.invokeLater(()->button.doClick(0));
    }
    static void text(JDialog dialog,String value){var fields=find(dialog,JTextField.class).stream().filter(JTextField::isEditable).toList();require(!fields.isEmpty(),"No dialog input");fields.getFirst().setText(value);}
}
