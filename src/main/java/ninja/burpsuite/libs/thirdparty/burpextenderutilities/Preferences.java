// Vendored from Burp-Montoya-Utilities by Corey Arthur (@CoreyD97)
// https://github.com/CoreyD97/Burp-Montoya-Utilities (commit b7faf563)
// Copyright (C) Corey Arthur
// Released under AGPL v3.0, see the LICENSE file; not covered by the
// additional terms in the NOTICE file
// Modifications (repackaging) Copyright (C) 2026 Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.thirdparty.burpextenderutilities;

import burp.api.montoya.MontoyaApi;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.nameManager.NameManager;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.typeadapter.AtomicIntegerTypeAdapter;
import ninja.burpsuite.libs.thirdparty.burpextenderutilities.typeadapter.ByteArrayToBase64TypeAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Preferences {

    public enum Visibility {GLOBAL, PROJECT, VOLATILE}

    private ILogProvider logProvider;

    public ILogProvider getLogProvider() { return logProvider; }
    public void setLogProvider(ILogProvider logProvider) { this.logProvider = logProvider; }

    private final IGsonProvider gsonProvider;

    public IGsonProvider getGsonProvider() { return gsonProvider; }
    private final MontoyaApi montoya;
    private final HashMap<String, Object> preferences;
    private final HashMap<String, Object> preferenceDefaults;
    private final HashMap<String, Type> preferenceTypes;
    private final HashMap<String, Visibility> preferenceVisibilities;
    private final ArrayList<PreferenceListener> preferenceListeners;

    public Preferences(final MontoyaApi montoyaApi, final IGsonProvider gsonProvider, final ILogProvider logProvider){
        this(montoyaApi, gsonProvider);
        this.logProvider = logProvider;
    }

    public Preferences(final MontoyaApi montoyaApi, final IGsonProvider gsonProvider){
        this.montoya = montoyaApi;
        this.gsonProvider = gsonProvider;
        this.preferenceDefaults = new HashMap<>();
        this.preferences = new HashMap<>();
        this.preferenceTypes = new HashMap<>();
        this.preferenceVisibilities = new HashMap<>();
        this.preferenceListeners = new ArrayList<>();
        registerRequiredTypeAdapters();
    }

    private void registerRequiredTypeAdapters(){
        this.gsonProvider.registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter());
        this.gsonProvider.registerTypeAdapter(byte[].class, new ByteArrayToBase64TypeAdapter());
    }

    /**
    * @deprecated use {@link #register} instead.
    */
    @Deprecated
    public void registerSetting(String settingName, Type type){
        registerSetting(settingName, type, null, Visibility.GLOBAL);
    }

    /**
     * @deprecated use {@link #register} instead.
     */
    @Deprecated
    public void registerSetting(String settingName, Type type, Object defaultValue){
        registerSetting(settingName, type, defaultValue, Visibility.GLOBAL);
    }

    /**
     * @deprecated use {@link #register} instead.
     */
    @Deprecated
    public void registerSetting(String settingName, Type type, Visibility visibility){
        registerSetting(settingName, type, null, visibility);
    }

    /**
     * @deprecated use {@link #register} instead.
     */
    @Deprecated
    public void registerSetting(String settingName, Type type, Object defaultValue, Visibility visibility){
        register(settingName, type, defaultValue, visibility);
    }

    public void register(String settingName, Type type){
        register(settingName, type, null, Visibility.GLOBAL);
    }

    public void register(String settingName, Type type, Object defaultValue){
        register(settingName, type, defaultValue, Visibility.GLOBAL);
    }

    public void register(String settingName, Type type, Visibility visibility){
        register(settingName, type, null, visibility);
    }

    public void register(String settingName, Type type, Object defaultValue, Visibility visibility){
        register(settingName, type, defaultValue, visibility, true);
    }

    public void register(String settingName, Type type, Object defaultValue, Visibility visibility, Boolean persistDefault){
        NameManager.reserve(settingName);

        this.preferenceVisibilities.put(settingName, visibility);
        this.preferenceTypes.put(settingName, type);
        this.preferenceDefaults.put(settingName, defaultValue);

        Object previousValue;
        switch(visibility){
            case PROJECT -> previousValue = getProjectSettingFromBurp(settingName, type);
            case GLOBAL -> previousValue = getGlobalSettingFromBurp(settingName, type);
            default -> previousValue = defaultValue;
        }

        if(previousValue != null){
            this.preferences.put(settingName, previousValue);
        }else{
            if(persistDefault) reset(settingName);
            else               this.preferences.put(settingName, cloneDefault(settingName));
        }

        logOutput(String.format("Registered setting: [Key=%s, Scope=%s, Type=%s, Default=%s, Value=%s, Persisted=%s]",
                settingName, visibility, type, defaultValue, this.preferences.get(settingName), persistDefault));
    }

    public void unregister(String settingName){
        assertThisManages(settingName);

        unpersist(settingName);

        preferences.remove(settingName);
        preferenceDefaults.remove(settingName);
        preferenceVisibilities.remove(settingName);
        preferenceTypes.remove(settingName);

        NameManager.release(settingName);

        for (PreferenceListener preferenceListener : this.preferenceListeners) {
            preferenceListener.onPreferenceSet(this, settingName, get(settingName));
        }
    }

    public void unpersist(String settingName) {
        assertThisManages(settingName);

        Visibility visibility = this.preferenceVisibilities.get(settingName);
        switch(visibility){
            case PROJECT -> delProjectSettingFromBurp(settingName);
            case GLOBAL  -> delGlobalSettingFromBurp(settingName);
        }

        logOutput(String.format("Unpersisted setting: [Key=%s]",
          settingName));
    }

    public void repersist(String settingName){
        assertThisManages(settingName);

        Object previousValue = this.preferences.get(settingName);
        Visibility visibility = this.preferenceVisibilities.get(settingName);
        switch(visibility){
        case PROJECT -> setProjectSetting(settingName, previousValue);
        case GLOBAL  -> setGlobalSetting(settingName, previousValue);
        }

        logOutput(String.format("Repersisted setting: [Key=%s, Value=%s]",
          settingName, this.preferences.get(settingName)));
    }

    public void setDefault(String settingName, Object newDefaultValue){
        assertThisManages(settingName);
        this.preferenceDefaults.put(settingName, newDefaultValue);
    }

    private void setGlobalSetting(String settingName, Object value) {
        Type type = this.preferenceTypes.get(settingName);
        Object currentValue = this.preferences.get(settingName);
        //String currentValueJson = gsonProvider.getGson().toJson(currentValue, type);
        String newValueJson = gsonProvider.getGson().toJson(value, type);
        //Temporarily removed. Not saving preferences for instance variables.
//        if(newValueJson != null && newValueJson.equals(currentValueJson)) return;

        this.montoya.persistence().preferences().setString(settingName, newValueJson);
        this.preferences.put(settingName, value);
    }

    private void setProjectSetting(String settingName, Object value) {
        Type type = this.preferenceTypes.get(settingName);
        Object currentValue = this.preferences.get(settingName);
        //String currentValueJson = gsonProvider.getGson().toJson(currentValue, type);
        String newValueJson = gsonProvider.getGson().toJson(value, type);
        //Temporarily removed. Not saving preferences for instance variables.
//        if(newValueJson != null && newValueJson.equals(currentValueJson)) return;
        this.montoya.persistence().extensionData().setString(settingName, newValueJson);
        this.preferences.put(settingName, value);
    }

    private Object getGlobalSettingFromBurp(String settingName, Type settingType) {
        String storedValue = getGlobalSettingJson(settingName);
        if(storedValue == null) return null;

//        logOutput(String.format("Value %s loaded for global setting \"%s\". Trying to deserialize.", storedValue, settingName));
        try {
            return gsonProvider.getGson().fromJson(storedValue, settingType);
        }catch (Exception e){
            logError("Could not deserialize stored setting \"" + settingName
                    + "\". This may be due to a change in stored types. Falling back to default.");
            logError("Value: " + storedValue);
            return null;
        }
    }

    private Object getProjectSettingFromBurp(String settingName, Type settingType) {
        String storedValue = getProjectSettingJson(settingName);
        if(storedValue == null) return null;

        try {
            return gsonProvider.getGson().fromJson(storedValue, settingType);
        }catch (Exception e){
            logError("Could not deserialize stored setting \"" + settingName
                    + "\". This may be due to a change in stored types. Falling back to default.");
            logError("Value: " + storedValue);
            return null;
        }
    }

    private void delProjectSettingFromBurp(String settingName){
        montoya.persistence().extensionData().deleteString(settingName);
    }

    private void delGlobalSettingFromBurp(String settingName){
        montoya.persistence().preferences().deleteString(settingName);
    }

    public HashMap<String, Visibility> getRegisteredSettings(){
        return this.preferenceVisibilities;
    }

    /**
     * @deprecated use {@link #get} instead.
     */
    @Deprecated
    public <T> T getSetting(String settingName) {
        return get(settingName);
    }

    public <T> T get(String settingName){
        assertThisManages(settingName);
        Object value = this.preferences.get(settingName);

        return (T) value;
    }

    /**
     * @deprecated use {@link #set} instead.
     */
    @Deprecated
    public void setSetting(String settingName, Object value){
        set(settingName, value, this);
    }

    /**
     * @deprecated use {@link #set} instead.
     */
    @Deprecated
    public void setSetting(String settingName, Object value, Object eventSource){
        set(settingName, value, eventSource);
    }

    public void set(String settingName, Object value){
        set(settingName, value, this);
    }

    public void set(String settingName, Object value, Object eventSource){
        assertThisManages(settingName);
        Visibility visibility = this.preferenceVisibilities.get(settingName);
        switch (visibility) {
            case VOLATILE: {
                this.preferences.put(settingName, value);
                break;
            }
            case PROJECT: {
                this.setProjectSetting(settingName, value);
                break;
            }
            case GLOBAL: {
                this.setGlobalSetting(settingName, value);
                break;
            }
        }

        for (PreferenceListener preferenceListener : this.preferenceListeners) {
            preferenceListener.onPreferenceSet(eventSource, settingName, value);
        }
    }

    /**
     * @deprecated use {@link #getType} instead.
     */
    @Deprecated
    public Type getSettingType(String settingName) {
        return getType(settingName);
    }

    public Type getType(String settingName) {
        assertThisManages(settingName);
        return this.preferenceTypes.get(settingName);
    }

    private String getGlobalSettingJson(String settingName) {
        return this.montoya.persistence().preferences().getString(settingName);
    }

    private String getProjectSettingJson(String settingName) {
        return this.montoya.persistence().extensionData().getString(settingName);
    }

    public void addSettingListener(PreferenceListener preferenceListener){
        this.preferenceListeners.add(preferenceListener);
    }

    public void removeSettingListener(PreferenceListener preferenceListener){
        this.preferenceListeners.remove(preferenceListener);
    }

    /**
     * @deprecated use {@link #reset} instead.
     */
    @Deprecated
    public void resetSetting(String settingName) {
        reset(settingName);
    }

    public void reset(String settingName){
        assertThisManages(settingName);

        Object newInstance = cloneDefault(settingName);
        this.set(settingName, newInstance);

        for (PreferenceListener preferenceListener : this.preferenceListeners) {
            preferenceListener.onPreferenceSet(this, settingName, get(settingName));
        }
    }

    /**
     * @deprecated use {@link #reset} instead.
     */
    @Deprecated
    public void resetSettings(Set<String> keys){
        reset(keys);
    }

    public void reset(Set<String> keys){
        for (String key : keys) {
            reset(key);
        }
    }

    /**
     * @deprecated use {@link #resetAll()} instead.
     */
    @Deprecated
    public void resetAllSettings(){
        resetAll();
    }

    public void resetAll(){
        HashMap<String, Preferences.Visibility> registeredSettings = getRegisteredSettings();
        reset(registeredSettings.keySet());
    }

    void logOutput(String message){
        if(this.logProvider != null)
            logProvider.logOutput(message);
    }

    void logError(String errorMessage){
        if(this.logProvider != null)
            logProvider.logError(errorMessage);
    }

    private Object cloneSetting(String settingName){
        assertThisManages(settingName);
        Type type  = preferenceTypes.get(settingName);
        Object src = preferences.get(settingName);

        return GsonUtilities.clone(src, type, gsonProvider.getGson());
    }

    private Object cloneDefault(String settingName){
        assertThisManages(settingName);
        Type type  = preferenceTypes.get(settingName);
        Object src = preferenceDefaults.get(settingName);

        return GsonUtilities.clone(src, type, gsonProvider.getGson());
    }

    private void assertThisManages(String settingName){
        if(preferenceVisibilities.get(settingName) == null){
            String msg = "Setting " + settingName +
              " is not managed by this Preferences instance.\n" +
              "If no other Preferences instance is managing this setting, try registering it first.";
            throw new UnmanagedSettingException(msg);
        }
    }
}
