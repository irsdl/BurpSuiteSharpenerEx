// Released under AGPL see LICENSE for more information
// Developed by Soroush Dalili (@irsdl)

package ninja.burpsuite.libs.generic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowsAppUserModelIdTest {

    @Test
    void guidToBytesUsesWindowsGuidMemoryLayout() {
        // IID_IPropertyStore: Data1, Data2 and Data3 are little endian, Data4 is not
        byte[] expected = new byte[]{
                (byte) 0xEB, (byte) 0x8E, (byte) 0x6D, (byte) 0x88,
                (byte) 0xF2, (byte) 0x8C,
                (byte) 0x46, (byte) 0x44,
                (byte) 0x8D, (byte) 0x02, (byte) 0xCD, (byte) 0xBA, (byte) 0x1D, (byte) 0xBD, (byte) 0xCF, (byte) 0x99
        };
        assertArrayEquals(expected, WindowsAppUserModelId.guidToBytes(WindowsAppUserModelId.IID_IPROPERTY_STORE));
    }

    @Test
    void guidToBytesMatchesAppUserModelIdPropertyKey() {
        // PKEY_AppUserModel_ID fmtid from the Windows SDK (propkey.h)
        byte[] expected = new byte[]{
                (byte) 0x55, (byte) 0x28, (byte) 0x4C, (byte) 0x9F,
                (byte) 0x79, (byte) 0x9F,
                (byte) 0x39, (byte) 0x4B,
                (byte) 0xA8, (byte) 0xD0, (byte) 0xE1, (byte) 0xD4, (byte) 0x2D, (byte) 0xE1, (byte) 0xD5, (byte) 0xF3
        };
        assertArrayEquals(expected, WindowsAppUserModelId.guidToBytes(WindowsAppUserModelId.PKEY_APP_USER_MODEL_ID_FMTID));
    }

    @Test
    void appUserModelIdPropertyIdIsFive() {
        // The property id of PKEY_AppUserModel_ID is 5 in the Windows SDK
        assertEquals(5, WindowsAppUserModelId.PKEY_APP_USER_MODEL_ID_PID);
    }

    @Test
    void guidToBytesRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> WindowsAppUserModelId.guidToBytes("not-a-guid"));
    }

    @Test
    void isWindowsOsNameOnlyMatchesWindows() {
        assertTrue(WindowsAppUserModelId.isWindowsOsName("Windows 11"));
        assertTrue(WindowsAppUserModelId.isWindowsOsName("Windows Server 2022"));
        assertFalse(WindowsAppUserModelId.isWindowsOsName("Linux"));
        assertFalse(WindowsAppUserModelId.isWindowsOsName("Mac OS X"));
        assertFalse(WindowsAppUserModelId.isWindowsOsName("Darwin")); // must not match the "win" inside "Darwin"
        assertFalse(WindowsAppUserModelId.isWindowsOsName(null));
    }

    @Test
    void getAndSetRejectNullOrNotDisplayableWindows() {
        // Null windows must fail safely on every OS without touching native code
        assertNull(WindowsAppUserModelId.getAppUserModelId(null));
        assertFalse(WindowsAppUserModelId.setAppUserModelId(null, "some.id"));
    }
}
