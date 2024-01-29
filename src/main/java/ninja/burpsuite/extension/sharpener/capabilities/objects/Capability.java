package ninja.burpsuite.extension.sharpener.capabilities.objects;

import ninja.burpsuite.extension.sharpener.ExtensionSharedParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Capability implements Serializable {
    public String name = ""; // this is used when displaying the capability in the UI
    public String description = ""; // this is used when displaying the tooltip for the capability
    public String settingName = ""; // this is used when storing the setting in the preferences
    public List<CapabilityGroup> capabilityGroupList = new ArrayList<>();
    public String implementationClassName = ""; // this is used when loading the capability
    public int order = 100000; // this is used when sorting the capabilities
    public boolean enabledByDefault = false; // this is used when sorting the capabilities
    // constructor for the capability
    public Capability(String name, String description, String settingName, List<CapabilityGroup> capabilityGroupList, String implementationClassName) {
        this(name, description, settingName, capabilityGroupList, implementationClassName, 100000, false);
    }

    public Capability(String name, String description, String settingName, List<CapabilityGroup> capabilityGroupList, String implementationClassName, int order, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.settingName = settingName;
        this.capabilityGroupList = capabilityGroupList;
        this.implementationClassName = implementationClassName;
        this.order = order;
        this.enabledByDefault = enabledByDefault;
    }

    // create the class object for the implemented capability using implementationClassName and reflection
    // It accepts ExtensionSharedParameters and CapabilitySettings as parameters and returns a dynamic object
    public Object createCapabilityObject(ExtensionSharedParameters sharedParameters, CapabilitySettings capabilitySettings) {
        try {
            Class<?> clazz = Class.forName(implementationClassName);
            return clazz.getConstructor(ExtensionSharedParameters.class, CapabilitySettings.class).newInstance(sharedParameters, capabilitySettings);
        } catch (Exception e) {
            sharedParameters.printException(e, "Error in creating capability object for " + name);
        }
        return null;
    }


}
