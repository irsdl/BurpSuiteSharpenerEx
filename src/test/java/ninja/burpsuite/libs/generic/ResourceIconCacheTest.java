// Copyright (C) 2021-2026 Soroush Dalili (@irsdl)
// Released under AGPL v3.0 with additional terms, see the LICENSE and NOTICE files
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Performance regression tests for the icon cache. The menus used to rescan the
// extension jar and reload every icon image on the EDT each time they were opened.
// The cache loads a folder once and returns the same list afterwards.
class ResourceIconCacheTest {

    @Test
    void listsTheBundledSubTabIcons() {
        List<String> names = ResourceIconCache.listResourceFileNames(ResourceIconCache.class, "subtabicons");
        assertFalse(names.isEmpty());
        // a couple of icons the tab menu relies on
        assertTrue(names.contains("alert.png"));
        assertTrue(names.contains("high.png"));
        // only file names, no paths
        for (String name : names) {
            assertFalse(name.contains("/"));
        }
    }

    @Test
    void listsTheBundledBurpIcons() {
        List<String> names = ResourceIconCache.listResourceFileNames(ResourceIconCache.class, "icons");
        assertFalse(names.isEmpty());
    }

    @Test
    void unknownFolderGivesAnEmptyListWithoutThrowing() {
        assertTrue(ResourceIconCache.listResourceFileNames(ResourceIconCache.class, "no-such-folder").isEmpty());
    }

    @Test
    void loadIconsReturnsOneReadyIconPerImageFile() {
        List<ResourceIconCache.NamedIcon> icons = ResourceIconCache.loadIcons(ResourceIconCache.class, "icons", 16);
        assertEquals(ResourceIconCache.listResourceFileNames(ResourceIconCache.class, "icons").size(), icons.size());
        for (ResourceIconCache.NamedIcon icon : icons) {
            assertNotNull(icon.icon());
            assertEquals(16, icon.icon().getIconWidth());
        }
    }

    @Test
    void getIconsCachesTheListSoTheJarIsScannedOnlyOnce() {
        List<ResourceIconCache.NamedIcon> first = ResourceIconCache.getIcons(ResourceIconCache.class, "subtabicons", 32);
        List<ResourceIconCache.NamedIcon> second = ResourceIconCache.getIcons(ResourceIconCache.class, "subtabicons", 32);
        assertFalse(first.isEmpty());
        assertSame(first, second);
    }

    @Test
    void cachedListCannotBeModifiedByCallers() {
        List<ResourceIconCache.NamedIcon> icons = ResourceIconCache.getIcons(ResourceIconCache.class, "icons", 16);
        assertThrows(UnsupportedOperationException.class, icons::clear);
    }
}
