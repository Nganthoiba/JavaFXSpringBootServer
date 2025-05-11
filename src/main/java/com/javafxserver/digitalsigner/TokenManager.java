package com.javafxserver.digitalsigner;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.javafxserver.service.TokenService;
import com.javafxserver.ui.DigiSignError;

import com.javafxserver.config.Config;
import com.javafxserver.exceptions.TokenErrorTranslator;

public class TokenManager {
	public static TokenService tokenService = new TokenService();
	public static String errorMessage = null;
	public static DigiSignError tokenError = null;
	
	
	public static void initializeToken(String secretPin) 
			throws Exception {
		/**
		 * It is important to validate for the PKCS#11 library file 
		 */
		String pkcs11LibPath = Config.getEpassConfig().get("library").toString();
		System.out.println("PKCS11 Library Path: " + pkcs11LibPath);
		
		File pkcs11LibFile = new File(pkcs11LibPath);
		if(pkcs11LibFile.exists()) {
			System.out.println("PKCS11 Library file exists.");
		} else {
			System.out.println("PKCS11 Library file does not exist.");
			throw new IOException("PKCS#11 library file not found at: " + pkcs11LibPath);
		}
		
		File configFile = Config.createTemporaryPKCS11Config();
        tokenService.detectToken(configFile, secretPin);
        configFile.delete();
	}
	
	public static void logoutToken() {
		tokenService.cleanup();
	}
	
	public static boolean isTokenPresent() {
		return JnaPkcs11.isTokenPresent();
	}
	
	public static boolean isUSBTokenInitialized() throws Exception {
		
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
            	tokenError = new DigiSignError("Certificate Missing", 
                        "Token present but no certificates found. Try reinserting the token.");
                return false;
            }

        } 
		/*
		catch (java.security.ProviderException ex) {
        	tokenError = new DigiSignError("Token Removed", 
                    "The ePass2003 token device may have been removed or is not responding. Please insert properly.");
        	ex.printStackTrace();
        	return false;
        } 
		catch(IOException ioException) {
			tokenError = new DigiSignError("USB Token Unaccessible", 
					"There was a problem accessing the USB token. Please check if it's properly inserted.");
			ioException.printStackTrace();
        	tokenService.cleanup();
        	Config.PIN = null;
			return false;
		}
		catch(KeyStoreException keyStoreException) {
			tokenError = new DigiSignError("Token Access Error", 
			        "The token could not be initialized. Try reinserting it and restarting the application.");
		    keyStoreException.printStackTrace();
		    return false;
		}
		catch(CertificateException certificateException) {
			tokenError = new DigiSignError("Certificate Issue", 
			        "The token doesn't seem to contain a valid certificate. Contact support if the issue persists.");
		    certificateException.printStackTrace();
			return false;
		}
		catch(NoSuchAlgorithmException noSuchAlgorithmException) {
			tokenError = new DigiSignError("Token Verification Failed", 
			        "We couldn’t verify the token. Please check the connection or reinsert the token.");
		    noSuchAlgorithmException.printStackTrace();
		    return false;
		}
		*/
		
		catch (Exception ex) {
			tokenError = TokenErrorTranslator.getFriendlyMessage(ex);
        	//ex.printStackTrace();
        	tokenService.setPkcs11Provider(null); 
        	tokenService.setKeyStore(null);
        	return false;
        }
		return true;
	}
}
