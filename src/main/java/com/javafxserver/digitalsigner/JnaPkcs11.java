package com.javafxserver.digitalsigner;

import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.NativeLongByReference;

import com.javafxserver.config.Config;

public class JnaPkcs11 {
	public interface PKCS11Library extends Library {
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
        	//No token found just halt further execution
        	return;
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
    
    /**
	 * To check whether an ePass2003 token is inserted or removed without needing the PIN, the correct and efficient way is to use 
	 * the PKCS#11 function C_GetSlotList() with tokenPresent = true.

		This does not require a PIN or a login — it just checks which slots have a token present.
	 */
	
    public interface PKCS11 extends Library {
        int C_Initialize(Pointer pInitArgs);
        int C_Finalize(Pointer pReserved);
        int C_GetSlotList(boolean tokenPresent, long[] pSlotList, NativeLongByReference pulCount);
    }
	
    public static boolean isTokenPresent() {
        Map<String, Object> epassConfig = Config.getEpassConfig();
        PKCS11 pkcs11 = Native.load(epassConfig.get("library").toString(), PKCS11.class);

        try {
        	
            int rv = pkcs11.C_Initialize(null);
            if (rv != 0) {
                //System.out.println("C_Initialize failed: " + rv);
                //return false;
            }

            NativeLongByReference slotCountRef = new NativeLongByReference();
            rv = pkcs11.C_GetSlotList(true, null, slotCountRef);
            if (rv != 0) {
                System.out.println("C_GetSlotList (count) failed: " + rv);
                return false;
            }

            long slotCount = slotCountRef.getValue().longValue();
            if (slotCount > 0) {
                //System.out.println("Token is inserted.");
                return true;
            } else {
                //System.out.println("Token is NOT inserted.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pkcs11.C_Finalize(null);
        }
        return false;
    }
}
