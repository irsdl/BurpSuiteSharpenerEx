// Burp Suite Sharpener
// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)
// Project link: https://github.com/irsdl/BurpSuiteSharpenerEx

package ninja.burpsuite.libs.generic;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.awt.*;

/**
 * Reads and writes the Windows AppUserModelID property of a window.
 * <p>
 * When Burp Suite is started from its native launcher (install4j exe), the launcher
 * sets a process level AppUserModelID. The Windows taskbar then shows the launcher
 * icon for that ID and ignores the icon set on the window itself. Giving the window
 * its own AppUserModelID breaks that link, so the taskbar uses the window icon again.
 * <p>
 * This class only uses core JNA. Burp Suite bundles the same JNA classes, so at
 * runtime the parent class loader normally provides them.
 */
public class WindowsAppUserModelId {

    // PKEY_AppUserModel_ID from the Windows SDK (propkey.h)
    static final String PKEY_APP_USER_MODEL_ID_FMTID = "9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3";
    static final int PKEY_APP_USER_MODEL_ID_PID = 5;

    // IID_IPropertyStore
    static final String IID_IPROPERTY_STORE = "886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99";

    private static final short VT_EMPTY = 0;
    private static final short VT_LPWSTR = 31;

    // IPropertyStore vtable slots: 0-2 IUnknown, 3 GetCount, 4 GetAt, 5 GetValue, 6 SetValue, 7 Commit
    private static final int VTBL_RELEASE = 2;
    private static final int VTBL_GET_VALUE = 5;
    private static final int VTBL_SET_VALUE = 6;
    private static final int VTBL_COMMIT = 7;

    // COINIT_APARTMENTTHREADED
    private static final int COINIT_APARTMENTTHREADED = 2;

    private interface Shell32 extends StdCallLibrary {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);

        int SHGetPropertyStoreForWindow(Pointer hwnd, Pointer riid, PointerByReference ppv);
    }

    private interface Ole32 extends StdCallLibrary {
        Ole32 INSTANCE = Native.load("ole32", Ole32.class);

        int CoInitializeEx(Pointer pvReserved, int dwCoInit);

        void CoUninitialize();

        int PropVariantClear(Pointer pvar);
    }

    public static boolean isWindows() {
        return isWindowsOsName(System.getProperty("os.name"));
    }

    static boolean isWindowsOsName(String osName) {
        return osName != null && osName.toLowerCase().startsWith("windows");
    }

    /**
     * Returns the AppUserModelID stored on the window, or null when the window has none.
     * Returns null on any failure. Must be called on a displayable window.
     */
    public static String getAppUserModelId(Window window) {
        if (!isWindows() || window == null || !window.isDisplayable()) {
            return null;
        }

        Pointer hwnd = Native.getWindowPointer(window);
        if (hwnd == null) {
            return null;
        }

        boolean mustUninitializeCom = initCom();
        try {
            Pointer store = openPropertyStore(hwnd);
            if (store == null) {
                return null;
            }
            try {
                Memory propertyKey = buildAppUserModelIdPropertyKey();
                Memory propVariant = emptyPropVariant();
                int hr = invokeVtblFunction(store, VTBL_GET_VALUE, new Object[]{store, propertyKey, propVariant});
                if (hr != 0) {
                    return null;
                }
                try {
                    if (propVariant.getShort(0) == VT_LPWSTR) {
                        Pointer stringPointer = propVariant.getPointer(8);
                        if (stringPointer != null) {
                            return stringPointer.getWideString(0);
                        }
                    }
                    return null;
                } finally {
                    Ole32.INSTANCE.PropVariantClear(propVariant);
                }
            } finally {
                invokeVtblFunction(store, VTBL_RELEASE, new Object[]{store});
            }
        } finally {
            if (mustUninitializeCom) {
                Ole32.INSTANCE.CoUninitialize();
            }
        }
    }

    /**
     * Sets the AppUserModelID of the window. A null or empty appId removes the
     * window property, which restores the default taskbar behaviour.
     * Returns true on success. Must be called on a displayable window.
     */
    public static boolean setAppUserModelId(Window window, String appId) {
        if (!isWindows() || window == null || !window.isDisplayable()) {
            return false;
        }

        Pointer hwnd = Native.getWindowPointer(window);
        if (hwnd == null) {
            return false;
        }

        boolean mustUninitializeCom = initCom();
        try {
            Pointer store = openPropertyStore(hwnd);
            if (store == null) {
                return false;
            }
            try {
                Memory propertyKey = buildAppUserModelIdPropertyKey();
                Memory propVariant = emptyPropVariant();

                // The value memory must stay referenced until SetValue has copied it.
                Memory value = null;
                if (appId != null && !appId.isEmpty()) {
                    value = new Memory((appId.length() + 1L) * 2); // UTF-16 characters plus null terminator
                    value.setWideString(0, appId);
                    propVariant.setShort(0, VT_LPWSTR);
                    propVariant.setPointer(8, value);
                } else {
                    propVariant.setShort(0, VT_EMPTY); // VT_EMPTY removes the window property
                }

                int hr = invokeVtblFunction(store, VTBL_SET_VALUE, new Object[]{store, propertyKey, propVariant});
                java.lang.ref.Reference.reachabilityFence(value); // SetValue has copied the string by now
                if (hr != 0) {
                    return false;
                }
                invokeVtblFunction(store, VTBL_COMMIT, new Object[]{store});
                return true;
            } finally {
                invokeVtblFunction(store, VTBL_RELEASE, new Object[]{store});
            }
        } finally {
            if (mustUninitializeCom) {
                Ole32.INSTANCE.CoUninitialize();
            }
        }
    }

    private static boolean initCom() {
        // 0 = S_OK, 1 = S_FALSE (already initialized): both need CoUninitialize.
        // Other results (for example RPC_E_CHANGED_MODE) mean COM is already usable
        // in another mode, so we continue without calling CoUninitialize.
        int hr = Ole32.INSTANCE.CoInitializeEx(null, COINIT_APARTMENTTHREADED);
        return hr == 0 || hr == 1;
    }

    private static Pointer openPropertyStore(Pointer hwnd) {
        Memory iid = new Memory(16);
        iid.write(0, guidToBytes(IID_IPROPERTY_STORE), 0, 16);
        PointerByReference ppv = new PointerByReference();
        int hr = Shell32.INSTANCE.SHGetPropertyStoreForWindow(hwnd, iid, ppv);
        if (hr != 0) {
            return null;
        }
        return ppv.getValue();
    }

    // PROPERTYKEY is a GUID followed by a 4 byte property id
    private static Memory buildAppUserModelIdPropertyKey() {
        Memory memory = new Memory(20);
        memory.write(0, guidToBytes(PKEY_APP_USER_MODEL_ID_FMTID), 0, 16);
        memory.setInt(16, PKEY_APP_USER_MODEL_ID_PID);
        return memory;
    }

    // PROPVARIANT: WORD vt at offset 0, three reserved WORDs, value union at offset 8
    private static Memory emptyPropVariant() {
        Memory memory = new Memory(24);
        memory.clear();
        return memory;
    }

    private static int invokeVtblFunction(Pointer comObject, int slot, Object[] args) {
        Pointer vtbl = comObject.getPointer(0);
        Pointer functionPointer = vtbl.getPointer((long) slot * Native.POINTER_SIZE);
        Function function = Function.getFunction(functionPointer, Function.ALT_CONVENTION);
        return function.invokeInt(args);
    }

    /**
     * Converts a GUID string to its 16 byte memory layout:
     * Data1 (int), Data2 (short) and Data3 (short) are little endian,
     * Data4 (8 bytes) is kept in written order.
     */
    static byte[] guidToBytes(String guid) {
        String hex = guid.replace("-", "");
        if (hex.length() != 32) {
            throw new IllegalArgumentException("Invalid GUID: " + guid);
        }
        byte[] bytes = new byte[16];
        long data1 = Long.parseLong(hex.substring(0, 8), 16);
        int data2 = Integer.parseInt(hex.substring(8, 12), 16);
        int data3 = Integer.parseInt(hex.substring(12, 16), 16);
        bytes[0] = (byte) data1;
        bytes[1] = (byte) (data1 >>> 8);
        bytes[2] = (byte) (data1 >>> 16);
        bytes[3] = (byte) (data1 >>> 24);
        bytes[4] = (byte) data2;
        bytes[5] = (byte) (data2 >>> 8);
        bytes[6] = (byte) data3;
        bytes[7] = (byte) (data3 >>> 8);
        for (int i = 0; i < 8; i++) {
            bytes[8 + i] = (byte) Integer.parseInt(hex.substring(16 + i * 2, 18 + i * 2), 16);
        }
        return bytes;
    }
}
