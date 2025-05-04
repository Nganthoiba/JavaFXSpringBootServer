package com.javafxserver.digitalsigner;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.javafxserver.service.TokenService;
import com.javafxserver.ui.Error;

import com.javafxserver.config.Config;
import com.javafxserver.exceptions.EpassTokenDetectionException;
import com.javafxserver.exceptions.InvalidPinException;
import com.javafxserver.exceptions.SunPKCS11NotFoundException;

public class TokenManager {
	public static TokenService tokenService = new TokenService();
	public static String errorMessage = null;
	public static Error tokenError = null;
	
	
	public static void initializeToken(String secretPin) 
			throws UnrecoverableKeyException, 
			KeyStoreException, 
			NoSuchAlgorithmException, 
			CertificateException, 
			EpassTokenDetectionException, 
			InvalidPinException, 
			SunPKCS11NotFoundException, 
			IOException {
		tokenService.cleanup();
        tokenService.detectToken(Config.createTemporaryPKCS11Config(), secretPin);
	}
	
	public static boolean isUSBTokenPresent() {
		if(tokenService.getPkcs11Provider() == null) {
			return false;
		}
		if(tokenService.getKeyStore() == null) {
			return false;
		}
		
		try {
            KeyStore ks = KeyStore.getInstance(Config.PKCS11, tokenService.getPkcs11Provider());
            ks.load(null, null); // no PIN needed for checking presence

            if (!ks.aliases().hasMoreElements()) {            	
            	tokenError = new Error("Certificate Missing", 
                        "Token present but no certificates found. Try reinserting the token.");
                return false;
            }

        } catch (java.security.ProviderException ex) {
        	tokenError = new Error("Token Removed", 
                    "The ePass2003 token device may have been removed. Please insert properly. "+ex.getMessage());
        	 return false;
        } catch (Exception ex) {
        	tokenService.setPkcs11Provider(null);        
        	tokenError = new Error("Load failed", 
                    "Unable to load usb token, please inject it properly: " + ex.getMessage());
        	return false;
        }
		return true;
	}
}
