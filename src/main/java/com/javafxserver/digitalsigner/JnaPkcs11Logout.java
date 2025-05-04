package com.javafxserver.digitalsigner;
import com.sun.jna.Native;
import com.sun.jna.ptr.NativeLongByReference;
public class JnaPkcs11Logout {
	public interface PKCS11Library extends com.sun.jna.Library {
        long CKF_SERIAL_SESSION = 0x00000004;
        long CKF_RW_SESSION = 0x00000002;
        
        /**
         * Those CKF_SERIAL_SESSION & CKF_RW_SESSION are PKCS#11 session flags, 
         * used when opening a session with a token (like a smart card or USB token) 
         * via the PKCS#11 API.

		   They define how the session behaves — whether it's read-only or read/write, 
		   and whether it's serial (single-threaded) or not.
		   
		   CKF_SERIAL_SESSION = 0x00000004
			This tells the token you want a serial (single-threaded) session.
			
			This flag is mandatory for most tokens.
			
			It ensures the session is not shared between threads (the opposite would be CKF_PARALLEL_SESSION,
			 which is rarely used).
			 
			 CKF_RW_SESSION = 0x00000002
			This flag requests a read/write session.
			
			Without it, the session is read-only — meaning you can’t modify token objects 
			(like certificates or keys), or perform operations like login/logout on some tokens.
			
			Required for actions like logging out or signing with a private key.
         * */

        int C_Initialize(Object args);
        int C_Finalize(Object args);
        int C_GetSlotList(boolean tokenPresent, long[] slotList, NativeLongByReference count);
        int C_OpenSession(long slotID, long flags, Object application, Object notify, NativeLongByReference session);
        int C_Login(long session, long userType, byte[] pin, long pinLen);
        int C_Logout(long session);
        int C_CloseSession(long session);
    }

    public static void logoutToken(String libraryPath) throws Exception {
        PKCS11Library pkcs11 = Native.load(libraryPath, PKCS11Library.class);

        pkcs11.C_Initialize(null);

        NativeLongByReference slotCountRef = new NativeLongByReference();
        pkcs11.C_GetSlotList(true, null, slotCountRef);
        long[] slots = new long[slotCountRef.getValue().intValue()];
        pkcs11.C_GetSlotList(true, slots, slotCountRef);

        if (slots.length == 0) {
            throw new Exception("No token found.");
        }

        NativeLongByReference sessionRef = new NativeLongByReference();
        int openResult = pkcs11.C_OpenSession(
            slots[0],
            PKCS11Library.CKF_SERIAL_SESSION | PKCS11Library.CKF_RW_SESSION,
            null,
            null,
            sessionRef
        );

        if (openResult != 0) throw new Exception("Open session failed: " + openResult);

        long session = sessionRef.getValue().longValue();
        pkcs11.C_Logout(session);
        pkcs11.C_CloseSession(session);
        pkcs11.C_Finalize(null);

        System.out.println("Token session logged out successfully.");
    }
}
