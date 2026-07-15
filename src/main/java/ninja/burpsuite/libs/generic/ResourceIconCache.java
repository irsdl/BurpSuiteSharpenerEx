// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// Lists and caches the image icons bundled in a resource folder of the extension jar.
// The jar content never changes while Burp runs, so each folder is scanned once and
// every image is loaded and scaled once. Menus used to scan the jar and reload all
// images on the EDT on every open, which made the first paint and every tab
// right-click noticeably slow.
public final class ResourceIconCache {

    // one loaded menu icon: the resource file name (with extension) and the ready icon
    public record NamedIcon(String fileName, ImageIcon icon) {
    }

    private static final ConcurrentHashMap<String, List<NamedIcon>> cache = new ConcurrentHashMap<>();

    private ResourceIconCache() {
    }

    // Returns the icons of a resource folder, scaled to the given width.
    // The first call loads them, later calls return the cached list.
    // Thread safe: a concurrent first call waits for the load instead of repeating it.
    public static List<NamedIcon> getIcons(Class<?> clazz, String resourceFolder, int width) {
        return cache.computeIfAbsent(resourceFolder + "|" + width,
                key -> loadIcons(clazz, resourceFolder, width));
    }

    static List<NamedIcon> loadIcons(Class<?> clazz, String resourceFolder, int width) {
        List<NamedIcon> icons = new ArrayList<>();
        for (String fileName : listResourceFileNames(clazz, resourceFolder)) {
            Image image = ImageHelper.scaleImageToWidth(
                    ImageHelper.loadImageResource(clazz, "/" + resourceFolder + "/" + fileName), width);
            if (image != null) {
                icons.add(new NamedIcon(fileName, new ImageIcon(image)));
            }
        }
        return Collections.unmodifiableList(icons);
    }

    // Lists the file names directly inside a resource folder, sorted by name.
    // Works for the packed extension jar and for a classes/resources directory (tests).
    // Returns an empty list when nothing is found, it never throws.
    static List<String> listResourceFileNames(Class<?> clazz, String resourceFolder) {
        TreeSet<String> names = new TreeSet<>();

        try {
            Enumeration<URL> urls = clazz.getClassLoader().getResources(resourceFolder);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if ("jar".equalsIgnoreCase(url.getProtocol())) {
                    collectFromJar(url, resourceFolder, names);
                } else if ("file".equalsIgnoreCase(url.getProtocol())) {
                    String[] files = new File(url.toURI()).list();
                    if (files != null) {
                        names.addAll(Arrays.asList(files));
                    }
                }
            }
        } catch (Exception e) {
            // fall through to the code source fallback below
        }

        // fallback for a jar without directory entries, where getResources finds nothing
        if (names.isEmpty()) {
            try {
                File source = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (source.isFile()) {
                    try (JarFile jarFile = new JarFile(source)) {
                        collectEntries(jarFile, resourceFolder, names);
                    }
                }
            } catch (Exception e) {
                // no resources could be listed, the caller gets an empty list
            }
        }

        return new ArrayList<>(names);
    }

    private static void collectFromJar(URL folderUrl, String resourceFolder, Set<String> names) throws Exception {
        JarURLConnection connection = (JarURLConnection) folderUrl.openConnection();
        // never close a cached JarFile, it is shared with the class loader
        connection.setUseCaches(false);
        try (JarFile jarFile = connection.getJarFile()) {
            collectEntries(jarFile, resourceFolder, names);
        }
    }

    private static void collectEntries(JarFile jarFile, String resourceFolder, Set<String> names) {
        String prefix = resourceFolder + "/";
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (entryName.startsWith(prefix) && !entryName.endsWith("/")) {
                String fileName = entryName.substring(prefix.length());
                if (!fileName.contains("/")) {
                    names.add(fileName);
                }
            }
        }
    }
}
