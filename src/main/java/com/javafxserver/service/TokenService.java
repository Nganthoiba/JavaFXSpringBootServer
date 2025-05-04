package com.javafxserver.service;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import com.javafxserver.config.Config;
import com.javafxserver.exceptions.EmptyPinException;
import com.javafxserver.exceptions.EpassTokenDetectionException;
import com.javafxserver.exceptions.InvalidPinException;
import com.javafxserver.exceptions.SunPKCS11NotFoundException;
import com.javafxserver.ui.PasswordDialog;

import com.javafxserver.utils.TokenUtil;
import com.javafxserver.utils.UIUtils;
import com.javafxserver.digitalsigner.CertificateInfo;
import com.javafxserver.digitalsigner.JnaPkcs11Logout;
import com.javafxserver.digitalsigner.TokenDetails;

public class TokenService {
	private Provider pkcs11Provider;
    private KeyStore keyStore;
    private char[] currentPin;
    
	public boolean loadTokenDriver(File dllFile) throws Exception {
        Optional<String> result = new PasswordDialog().showAndWait();       
        
        if(!result.isPresent()) {
        	UIUtils.showAlert("PIN Cancelled", "You have cancelled the PIN entry.");
    		throw new Exception("PIN Entry Cancelled by User.");
        }
        
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            throw new EmptyPinException("PIN cannot be empty.");
        }
        
        String currentPin = result.get().trim();
                
        File configFile = dllFile == null? Config.getConfigFile(): Config.setConfigFile(dllFile.getAbsolutePath());
        return detectToken(configFile, currentPin);        
    }
	
	public boolean detectToken(File configFile, String secretPin) 
			throws EpassTokenDetectionException,  
			InvalidPinException, 
			KeyStoreException,
			NoSuchAlgorithmException, 
			CertificateException, 
			SunPKCS11NotFoundException,			
			IOException, 
			UnrecoverableKeyException{
		
		// Load the PKCS#11 provider using the configuration file (fresh)
        try {
			pkcs11Provider = TokenUtil.loadPkcs11ProviderFromFile(configFile);
			
		} catch (Exception e) {
			throw new EpassTokenDetectionException("Error in loading pkcs11 provider from configuration file: "
			+configFile.getAbsolutePath(), e);
		}
        
        Security.addProvider(pkcs11Provider);
        
        // Load the KeyStore using the PKCS#11 provider and the PIN
        keyStore = KeyStore.getInstance(Config.PKCS11, pkcs11Provider);
        keyStore.load(null, secretPin.toCharArray());

        // Validate by checking alias and accessing key (forces PIN check)
        Enumeration<String> aliases = keyStore.aliases();
        if (!aliases.hasMoreElements()) {
            throw new EpassTokenDetectionException("No certificates found in token.");
        }
        
        String alias = aliases.nextElement();

        // This line is **key** to ensure the PIN is valid
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, secretPin.toCharArray());
        if (privateKey == null) {
			throw new InvalidPinException("Failed to retrieve private key. Invalid PIN or no key found.");
		}
        
        //Here once the PIN is found correct and valid, we can store it in the config        
        Config.PIN = secretPin; // Storing the correct PIN in the config
        
        return true;
	}

    public Provider getPkcs11Provider() {
        return pkcs11Provider;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
    
    public TokenDetails getTokenDetails() {
		TokenDetails tokenDetails = new TokenDetails(pkcs11Provider, keyStore);
		return tokenDetails;
	}
    
    public PrivateKey getPrivateKey(String pin) throws IOException, InvalidPinException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException{
    	Enumeration<String> aliases = keyStore.aliases();
    	String alias = aliases.hasMoreElements() ? aliases.nextElement() : null;
		if (alias == null) {
			throw new IOException("No certificates found in token.");
		}
		
		// Validate the PIN with the token
        if (!TokenUtil.isTokenPresent(pkcs11Provider, pin.toCharArray())) {
            throw new IOException("Invalid token: Token is not present.");
        }
        
		PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, pin.toCharArray());
		if(privateKey == null) {
			throw new InvalidPinException("Failed to retrieve private key. Invalid PIN or no key found.");
		}
		return privateKey;
    }
    
    public PublicKey getPublicKey() throws Exception{
    	Enumeration<String> aliases = keyStore.aliases();
    	String alias = aliases.hasMoreElements() ? aliases.nextElement() : null;
		if (alias == null) {
			throw new IOException("No certificates found in token.");
		}
		X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
		return cert.getPublicKey();
    }
    
    //Method to get certificate details together
    public List<CertificateInfo> getCertificateDetails() throws Exception {
        List<CertificateInfo> certificateInfoList = new ArrayList<>();

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert != null) {
                CertificateInfo info = new CertificateInfo();
                info.setAlias(alias);
                info.setSubjectDN(cert.getSubjectX500Principal().getName());
                info.setIssuerDN(cert.getIssuerX500Principal().getName());
                info.setValidFrom(cert.getNotBefore());
                info.setValidTo(cert.getNotAfter());
                info.setPublicKey(cert.getPublicKey());                
                info.parseSubjectDN();
                info.parseIssuerDN();                
                
                certificateInfoList.add(info);
            }
        }

        return certificateInfoList;
    }  
    
    public void cleanup() {
        try {
            if (pkcs11Provider != null) {
                Security.removeProvider(pkcs11Provider.getName());
              //Logout from the token
               JnaPkcs11Logout.logoutToken(Config.get("library"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pkcs11Provider = null;
            keyStore = null;
            if (currentPin != null) {
                java.util.Arrays.fill(currentPin, '0'); // Clear PIN from memory
                currentPin = null;
            }
            Config.PIN = null; // Clear PIN from config too            
        }
    }
    
    
}
