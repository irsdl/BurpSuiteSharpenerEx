// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic.uiObjFinder;

import javax.swing.*;
import java.awt.*;

public class UIWalker {
    public static JComponent getCurrentJComponent(Component rootUIObject) {
        JComponent rootUIJComponent = null;
        if (rootUIObject instanceof JComponent) {
            rootUIJComponent = (JComponent) rootUIObject;
        } else if (rootUIObject.getComponentAt(0, 0) instanceof JComponent) {
            rootUIJComponent = (JComponent) rootUIObject.getComponentAt(0, 0);
        }
        return rootUIJComponent;
    }

    public static Component findUIObjectInSubComponents(Component rootUIObject, int maxDepth, UiSpecObject uiSpecObject) {
        return findUIObjectInSubComponentsWithExclusions(rootUIObject, maxDepth, uiSpecObject, null);
    }

    public static Component findUIObjectInSubComponentsWithExclusions(Component rootUIObject, int maxDepth, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        JComponent rootUIJComponent = getCurrentJComponent(rootUIObject);

        if (rootUIJComponent != null) {
            if (uiSpecObject.isCompatible(rootUIJComponent) && !containsComponent(arrayOfExcludedComponents, rootUIJComponent)) {
                foundObject = rootUIJComponent;
            } else {
                foundObject = findUIObjectInSubComponentsWithExclusions(rootUIJComponent, maxDepth, 0, uiSpecObject, arrayOfExcludedComponents);
            }
        }

        return foundObject;
    }

    private static Component findUIObjectInSubComponentsWithExclusions(JComponent rootUIJComponent, int maxDepth, int currentDepth, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        for (Component subComponent : rootUIJComponent.getComponents()) {
            if (uiSpecObject.isCompatible(subComponent) && !containsComponent(arrayOfExcludedComponents, subComponent)) {
                foundObject = subComponent;
                break;
            } else if (currentDepth < maxDepth && subComponent instanceof JComponent) {
                foundObject = findUIObjectInSubComponentsWithExclusions((JComponent) subComponent, maxDepth, currentDepth + 1, uiSpecObject, arrayOfExcludedComponents);
                if (foundObject != null)
                    break;
            }
        }
        return foundObject;
    }

    public static Component findUIObjectInParentComponents(Component rootUIObject, int maxDepth, UiSpecObject uiSpecObject) {
        return findUIObjectInParentComponentsWithExclusions(rootUIObject, maxDepth, uiSpecObject, null);
    }

    public static Component findUIObjectInParentComponentsWithExclusions(Component rootUIObject, int maxDepth, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        JComponent rootUIJComponent = getCurrentJComponent(rootUIObject);

        if (rootUIJComponent != null) {
            if (uiSpecObject.isCompatible(rootUIJComponent) && !containsComponent(arrayOfExcludedComponents, rootUIJComponent)) {
                foundObject = rootUIJComponent;
            } else {
                foundObject = findUIObjectInParentComponentsWithExclusions(rootUIJComponent, maxDepth, 0, uiSpecObject, arrayOfExcludedComponents);
            }
        }

        return foundObject;
    }

    private static Component findUIObjectInParentComponentsWithExclusions(JComponent rootUIJComponent, int maxDepth, int currentDepth, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        if (rootUIJComponent.getParent() instanceof JComponent parentComponent) {
            if (uiSpecObject.isCompatible(parentComponent) && !containsComponent(arrayOfExcludedComponents, parentComponent)) {
                foundObject = parentComponent;
            } else if (currentDepth < maxDepth && parentComponent instanceof JComponent) {
                foundObject = findUIObjectInParentComponentsWithExclusions(parentComponent, maxDepth, currentDepth + 1, uiSpecObject, arrayOfExcludedComponents);
            }
        }
        return foundObject;
    }

    public static Component findUIObjectInNeighbourComponents(Component rootUIObject, UiSpecObject uiSpecObject) {
        return findUIObjectInNeighbourComponentsWithExclusions(rootUIObject, uiSpecObject, null);
    }

    public static Component findUIObjectInNeighbourComponentsWithExclusions(Component rootUIObject, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        JComponent rootUIJComponent = getCurrentJComponent(rootUIObject);

        if (rootUIJComponent != null) {
            if (uiSpecObject.isCompatible(rootUIJComponent) && !containsComponent(arrayOfExcludedComponents, rootUIJComponent)) {
                foundObject = rootUIJComponent;
            } else {
                foundObject = findUIObjectInNeighbourComponentsWithExclusions(rootUIJComponent, uiSpecObject, arrayOfExcludedComponents);
            }
        }

        return foundObject;
    }

    private static Component findUIObjectInNeighbourComponentsWithExclusions(JComponent rootUIJComponent, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        if (rootUIJComponent.getParent() instanceof JComponent parentComponent) {
            foundObject = findUIObjectInSubComponentsWithExclusions(parentComponent, 1, uiSpecObject, arrayOfExcludedComponents);
        }
        return foundObject;
    }

    public static Component findUIObjectInComponents(Component[] arrayOfComponents, UiSpecObject uiSpecObject) {
        return findUIObjectInComponentsWithExclusions(arrayOfComponents, uiSpecObject, null);
    }

    public static Component findUIObjectInComponentsWithExclusions(Component[] arrayOfComponents, UiSpecObject uiSpecObject, Component[] arrayOfExcludedComponents) {
        Component foundObject = null;
        for(Component currentComponent:arrayOfComponents){
            if (uiSpecObject.isCompatible(currentComponent) && !containsComponent(arrayOfExcludedComponents, currentComponent)) {
                foundObject = currentComponent;
                break;
            }
        }
        return foundObject;
    }

    public static boolean containsComponent(Component[] components, Component target) {
        if(components != null && target != null){
            for (Component comp : components) {
                if (comp == target) {
                    return true;
                }
            }
        }
        return false;
    }
}
