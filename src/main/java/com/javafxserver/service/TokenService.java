package com.javafxserver.service;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import com.javafxserver.config.Config;
import com.javafxserver.exceptions.EpassTokenDetectionException;
import com.javafxserver.exceptions.InvalidPinException;
import com.javafxserver.exceptions.SunPKCS11NotFoundException;
import com.javafxserver.utils.TokenUtil;
import com.javafxserver.digitalsigner.CertificateInfo;
import com.javafxserver.digitalsigner.JnaPkcs11;
import com.javafxserver.digitalsigner.TokenDetails;

public class TokenService {
	private Provider pkcs11Provider;
    private KeyStore keyStore;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private char[] currentPin;    
	
	public boolean detectToken(File configFile, String secretPin) 
			throws KeyStoreException, 
			EpassTokenDetectionException, 
			NoSuchAlgorithmException, 
			CertificateException, 
			IOException, 
			UnrecoverableKeyException, 
			InvalidPinException 
	{
		
		// Load the PKCS#11 provider using the configuration file (fresh)		
        try {
			pkcs11Provider = TokenUtil.loadPkcs11ProviderFromFile(configFile);
		} catch (SunPKCS11NotFoundException e) {
			System.out.println("Error loading PKCS#11 provider: " + e.getMessage());
			throw new EpassTokenDetectionException("Failed to get provider- "+e.getMessage(), e);
		}
        catch(ProviderException e) {
			System.out.println("Error initializing PKCS#11 provider: " + e.getMessage());
			throw new EpassTokenDetectionException("Failed to initialize provider- "+e.getMessage(), e);
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
        
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
        publicKey = cert.getPublicKey();

        // This line is **key** to ensure the PIN is valid
        privateKey = (PrivateKey) keyStore.getKey(alias, secretPin.toCharArray());
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
    
    public void setPkcs11Provider(Provider provider) {
        this.pkcs11Provider = provider;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
    
    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }
    
    public TokenDetails getTokenDetails() {
		TokenDetails tokenDetails = new TokenDetails(pkcs11Provider, keyStore);
		return tokenDetails;
	}
    
    public PrivateKey getPrivateKey() {
    	return privateKey;
    }
    
    public PrivateKey getPrivateKey(String pin) throws IOException, InvalidPinException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException{
    	    	
    	Enumeration<String> aliases = keyStore.aliases();
    	String alias = aliases.hasMoreElements() ? aliases.nextElement() : null;
    	
		if (alias == null) {
			throw new IOException("No certificates found in token.");
		}
		
		privateKey = (PrivateKey) keyStore.getKey(alias, pin.toCharArray());
		if(privateKey == null) {
			throw new InvalidPinException("Failed to retrieve private key. Invalid PIN or no key found.");
		}
		return privateKey;
    }
    
    public PublicKey getPublicKey() throws KeyStoreException, IOException{
    	
    	if(publicKey != null) {
    		return publicKey;
    	}
    	
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
                CertificateInfo info = new CertificateInfo(alias, cert);                
                certificateInfoList.add(info);
            }
        }

        return certificateInfoList;
    }
    
    public String getCertificateAlias() throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;  // First alias that has a private key
            }
        }
        throw new Exception("No alias with a private key found.");
    }
    
    public X509Certificate getCertificate() throws Exception {
        if (keyStore == null) {
            throw new IllegalStateException("KeyStore is not initialized. Call detectToken() first.");
        }

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    return (X509Certificate) cert;
                }
            }
        }

        throw new Exception("No valid X.509 certificate found in the token.");
    }
    
    
    public void cleanup() {
        try {
            if (pkcs11Provider != null) {
            	Map<String, Object> epassConfig = Config.getEpassConfig();
            	
                Security.removeProvider(pkcs11Provider.getName());
                //Logout from the token
                JnaPkcs11.logoutToken(epassConfig.get("library").toString());
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
