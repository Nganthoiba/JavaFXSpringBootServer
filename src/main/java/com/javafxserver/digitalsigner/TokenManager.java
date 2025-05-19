package com.javafxserver.digitalsigner;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.javafxserver.service.TokenService;
import com.javafxserver.ui.DigiSignError;
import com.javafxserver.ui.PinPrompt;
import com.javafxserver.config.Config;
import com.javafxserver.exceptions.EmptyPinException;
import com.javafxserver.exceptions.EpassTokenDetectionException;
import com.javafxserver.exceptions.InvalidPinException;
import com.javafxserver.exceptions.TokenErrorTranslator;
import com.javafxserver.exceptions.TokenInitializationFailedException;

public class TokenManager {
	public static TokenService tokenService = new TokenService();
	public static String errorMessage = null;
	public static DigiSignError tokenError = null;
	
	/**
     * This method will initialize token only if it is necessary
     * @throws Exception
     */
	public static void initializeTokenIfNeeded() 
			throws TokenInitializationFailedException, EmptyPinException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, EpassTokenDetectionException, InvalidPinException{
    	
    	if(!isTokenPresent()) {
    		throw new TokenInitializationFailedException("Token is not inserted or not detected, make sure that the usb token is inserted properly");
    	}    	
    	
    	if(!isUSBTokenInitialized()) {
        	if(Config.PIN != null && tokenError != null) {
        		tokenError.displayErrorDialog();
        		throw new TokenInitializationFailedException(tokenError.getTitle()+": "+tokenError.getMessage());            		
        	}
        	
        	String secretPin = PinPrompt.requestUserPinBlocking(pin -> {
                if (Config.PIN != null && !pin.equals(Config.PIN)) {
                    return false;
                }
                return true;
            });        	
        	Config.PIN = secretPin;        	
        }
    	else {
    		System.out.println("Token already initialized.");
    	}
    	
        if(Config.PIN == null) {
        	throw new EmptyPinException("You have not enter secret PIN");
        }
        
        initializeToken(Config.PIN);        
    }
	
	public static void initializeToken(String secretPin) 
			throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, EpassTokenDetectionException, InvalidPinException {
		/**
		 * It is important to validate for the PKCS#11 library file 
		 */
		System.out.println("Initializing token .....");
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
        tokenService.loadToken(configFile, secretPin);
        configFile.delete();
	}
	
	public static void logoutToken() {
		tokenService.cleanup();
	}
	
	public static boolean isTokenPresent() {
		return JnaPkcs11.isTokenPresent();
	}
	
	
	
	public static boolean isUSBTokenInitialized(){
		
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
