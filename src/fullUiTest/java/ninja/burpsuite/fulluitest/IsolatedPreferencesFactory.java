// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)
package ninja.burpsuite.fulluitest;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.prefs.*;

/** Test JVM only: memory-only preferences, seeded privately through the child stdin pipe. */
public final class IsolatedPreferencesFactory implements PreferencesFactory {
    static final int MAX_SEED=8*1024*1024;
    private final Node user=new Node(null,""),system=new Node(null,"");
    public IsolatedPreferencesFactory() {
        if(System.getProperty("sharpener.fullUi.root")==null)throw new IllegalStateException("Preference isolation requires the full UI test runner");
        try {
            DataInputStream input=new DataInputStream(System.in);int length=input.readInt();
            if(length<0||length>MAX_SEED)throw new IOException("Invalid full UI preference seed size");
            byte[] seed=input.readNBytes(length);if(seed.length!=length)throw new EOFException("Incomplete preference seed");
            restore(user,JsonParser.parseString(new String(seed,StandardCharsets.UTF_8)).getAsJsonObject());
            Arrays.fill(seed,(byte)0);
        }catch(IOException failure){throw new IllegalStateException("Cannot receive private test preference seed",failure);}
    }
    public Preferences userRoot(){return user;}
    public Preferences systemRoot(){return system;}

    static byte[] captureBurpPreferences(String edition) throws BackingStoreException {
        return captureBurpPreferences(Preferences.userRoot(),edition.equals("community"));
    }
    private static byte[] captureBurpPreferences(Preferences source) throws BackingStoreException {
        return captureBurpPreferences(source,false);
    }
    private static byte[] captureBurpPreferences(Preferences source,boolean community) throws BackingStoreException {
        JsonObject root=new JsonObject(), children=new JsonObject();root.add("values",new JsonObject());root.add("children",children);
        if(source.nodeExists("burp")) {
            // Activation responses use opaque root keys, not names beginning with
            // "license". Preserve existing local cache entries unchanged in memory.
            // Do not copy user projects,
            // extension settings, history, scripts or account/session preferences.
            Preferences burp=source.node("burp");JsonObject node=new JsonObject(),values=new JsonObject();
            Set<String> startup=Set.of("eulapro","eulacommunity","burp.eula","use_community_edition",
                    "suite.new_project_default_config_option","burp.suite.new_project_default_config_option");
            boolean licensePresent=false, activationPresent=false;
            for(String key:burp.keys())if(key.matches("(?i)licen[cs]e[0-9]*")||isActivationCacheKey(key)||startup.contains(key)) {
                if(community&&(key.matches("(?i)licen[cs]e[0-9]*")||isActivationCacheKey(key)))continue;
                String value=burp.get(key,null);if(value!=null)values.addProperty(key,value);
                if(value!=null&&!value.isBlank()) {
                    licensePresent|=key.matches("(?i)licen[cs]e[0-9]*");
                    activationPresent|=isActivationCacheKey(key);
                }
            }
            if(!community&&licensePresent&&!activationPresent)
                throw new IllegalStateException("Existing local activation cache is unavailable. Full UI tests must not request a new activation.");
            if(community)values.addProperty("use_community_edition","true");
            node.add("values",values);node.add("children",new JsonObject());children.add("burp",node);
        }
        byte[] seed=root.toString().getBytes(StandardCharsets.UTF_8);
        if(seed.length>MAX_SEED)throw new IllegalStateException("Burp preferences exceed isolated test seed limit");
        return seed;
    }
    private static boolean isActivationCacheKey(String key) {
        if(!key.matches("[A-Za-z0-9+/]{20,86}={0,2}"))return false;
        try {
            byte[] decoded=Base64.getDecoder().decode(key);
            return (decoded.length==16||decoded.length==20||decoded.length==32) && Base64.getEncoder().encodeToString(decoded).equals(key);
        }catch(IllegalArgumentException invalid){return false;}
    }
    private static JsonObject capture(Preferences source,int depth)throws BackingStoreException {
        if(depth>16)throw new IllegalStateException("Burp preference tree is too deep");
        JsonObject node=new JsonObject(),values=new JsonObject(),children=new JsonObject();
        for(String key:source.keys()){String value=source.get(key,null);if(value!=null)values.addProperty(key,filter(value));}
        for(String child:source.childrenNames())children.add(child,capture(source.node(child),depth+1));
        node.add("values",values);node.add("children",children);return node;
    }
    private static void restore(Node node,JsonObject seed) {
        seed.getAsJsonObject("values").entrySet().forEach(entry->node.values.put(entry.getKey(),entry.getValue().getAsString()));
        seed.getAsJsonObject("children").entrySet().forEach(entry->restore((Node)node.node(entry.getKey()),entry.getValue().getAsJsonObject()));
    }
    static String filter(String input) {
        if(input==null||!(input.stripLeading().startsWith("{")||input.stripLeading().startsWith("[")))return input;
        try {JsonElement json=JsonParser.parseString(input);filterJson(json);return json.toString();}catch(JsonParseException invalid){return input;}
    }
    private static void filterJson(JsonElement value) {
        if(value.isJsonObject())for(var entry:value.getAsJsonObject().entrySet()) {
            if(entry.getKey().toLowerCase(Locale.ROOT).contains("extension")&&entry.getValue().isJsonArray())entry.setValue(new JsonArray());
            else filterJson(entry.getValue());
        } else if(value.isJsonArray())for(JsonElement element:value.getAsJsonArray())filterJson(element);
    }
    static void selfCheck() throws Exception {
        Node source=new Node(null,"");source.node("burp").put("fixture","before");
        Node isolated=new Node(null,"");restore(isolated,capture(source,0));
        isolated.node("burp").put("fixture","after");isolated.node("test-only").put("new","value");
        if(!source.node("burp").get("fixture","").equals("before")||source.nodeExists("test-only"))throw new AssertionError("Preference writes escaped isolation");
        Node profile=new Node(null,"");Preferences burp=profile.node("burp");
        String cacheKey=Base64.getEncoder().encodeToString(new byte[16]);
        burp.put("license1","synthetic license");burp.put(cacheKey,"synthetic cached activation");
        burp.put("eulapro","previously accepted");burp.put("suite.recentProjects0","private project");
        burp.node("extensions").put("fixture","private extension settings");
        byte[] seed=captureBurpPreferences(profile);
        try {
            Node seeded=new Node(null,"");restore(seeded,JsonParser.parseString(new String(seed,StandardCharsets.UTF_8)).getAsJsonObject());
            Preferences result=seeded.node("burp");
            if(!result.get(cacheKey,"").equals("synthetic cached activation")||!result.get("license1","").equals("synthetic license")||
                    result.get("suite.recentProjects0",null)!=null||result.nodeExists("extensions"))throw new AssertionError("Activation cache seed or profile isolation failed");
            result.put(cacheKey,"test write");
            if(!burp.get(cacheKey,"").equals("synthetic cached activation"))throw new AssertionError("Activation cache write escaped isolation");
        }finally{Arrays.fill(seed,(byte)0);}
        byte[] communitySeed=captureBurpPreferences(profile,true);
        try {
            Node seeded=new Node(null,"");restore(seeded,JsonParser.parseString(new String(communitySeed,StandardCharsets.UTF_8)).getAsJsonObject());
            Preferences result=seeded.node("burp");
            if(result.get("license1",null)!=null||result.get(cacheKey,null)!=null||!result.getBoolean("use_community_edition",false))
                throw new AssertionError("Community test imported Pro licensing or failed edition selection");
        }finally{Arrays.fill(communitySeed,(byte)0);}
        burp.remove(cacheKey);
        try{captureBurpPreferences(profile);throw new AssertionError("Unactivated profile was accepted");}
        catch(IllegalStateException expected){if(!expected.getMessage().contains("activation cache"))throw expected;}
    }
    private static final class Node extends AbstractPreferences {
        final Map<String,String> values=new HashMap<>();final Map<String,Node> nodes=new HashMap<>();
        Node(Node parent,String name){super(parent,name);}
        protected void putSpi(String key,String value){values.put(key,value);}
        protected String getSpi(String key){return values.get(key);}
        protected void removeSpi(String key){values.remove(key);}
        protected void removeNodeSpi(){values.clear();if(parent() instanceof Node parent)parent.nodes.remove(name());}
        protected String[] keysSpi(){return values.keySet().toArray(String[]::new);}
        protected String[] childrenNamesSpi(){return nodes.keySet().toArray(String[]::new);}
        protected AbstractPreferences childSpi(String name){return nodes.computeIfAbsent(name,key->new Node(this,key));}
        protected void syncSpi(){}
        protected void flushSpi(){}
    }
}
