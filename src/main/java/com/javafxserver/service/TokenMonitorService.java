/**
 * This class is to check if the USB epass token device is injected in the PC, or the PC is able to access the device
 */
package com.javafxserver.service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.javafxserver.config.Config;
import com.javafxserver.digitalsigner.JnaPkcs11;
import com.javafxserver.digitalsigner.TokenManager;
import com.javafxserver.exceptions.HandleExceptionMessage;


public class TokenMonitorService {
	private ScheduledExecutorService tokenMonitor;
	private Provider provider;
	public static boolean tokenPresent = false;
	
	public TokenMonitorService(Provider provider) {
		this.provider = provider;
	}
	
	public interface TokenLostCallback {
        void onTokenLost(String title, String message);
    }
	
	public void startMonitoring(TokenLostCallback callback) {
		stopMonitoring(); // in case if it's already running

        tokenMonitor = Executors.newSingleThreadScheduledExecutor();
        tokenMonitor.scheduleAtFixedRate(() -> {
        	
        	try {
	            if(isTokenPresent()) {
	            	// Device is present
					if(tokenPresent == false) {
						//If there was no token device before, then it is inserted now
						/*
						callback.onTokenLost("Token Inserted", 
							"The ePass2003 token USB device is detected.");
						*/
						// Token is already present, do nothing
		            	tokenPresent = true;
		            	System.out.println("Token is present");
					}
				}
	            else {
	            	if(tokenPresent == true) {
	            		//Token was present before, but now it is removed
						// Token is removed
	            		/*
						callback.onTokenLost("epass2003 USB not detected?", 
							"The ePass2003 token USB device is not detected.");
							*/
						//necessary to clean up the session
						TokenManager.logoutToken();
						System.out.println("Token is no longer present");
						tokenPresent = false;
					}
	            }
        	}
        	catch(ProviderException e) {
        		//e.printStackTrace();
        		// Actual usb epass2003 device is removed here
        		callback.onTokenLost("epass2003 USB Removed", 
						e.getMessage() + " (ePass2003 token device has been removed)");
        		TokenManager.logoutToken();
        	}
        	catch(KeyStoreException e) {
        		// Handle the exception
				callback.onTokenLost(e.getMessage(), HandleExceptionMessage.getMessage(e));
				//e.printStackTrace();
        	}
            catch(Exception e) {
            	//e.printStackTrace();
            	String exMessage = HandleExceptionMessage.getMessage(e);
            	System.out.println("Error checking token presence: " + exMessage);
            	// Handle the exception
				callback.onTokenLost(e.getMessage(), exMessage);
            }
        	
        }, 4, 4, TimeUnit.SECONDS);
	}
	
	
	private boolean isTokenPresent() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{
		
		if(!JnaPkcs11.isTokenPresent()) {
			// Device is not present
			//System.out.println("Token is not inserted");
			return false;
		}
		
		provider = TokenManager.tokenService.getPkcs11Provider();
		if(provider == null) {
			// Provider is not available
			//System.out.println("Provider is not available");
			return false;
		}
		
		if (!provider.getName().toLowerCase().contains("SunPKCS11".toLowerCase())) {
		    // Provider is not the expected one
		    System.out.println("Provider is not the expected one: " + provider.getName());
		    return false;
		}
		
		KeyStore ks = KeyStore.getInstance(Config.PKCS11, this.provider);
        ks.load(null, null); // no PIN needed for checking presence

        if (!ks.aliases().hasMoreElements()) {
        	System.out.println("Token present but no certificates found. Try reinserting the token.");
            return false;
		}			
		 
		// Token is present
		return true;
	}
	
	public void stopMonitoring() {
		if(tokenMonitor != null) {
			if(!tokenMonitor.isShutdown() || !tokenMonitor.isTerminated()) {
				tokenMonitor.shutdown();
			}			
		}
	}
	
	
	
	
}
