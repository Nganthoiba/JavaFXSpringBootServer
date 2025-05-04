package com.javafxserver.digitalsigner;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import com.javafxserver.utils.TokenUtil;
public class TokenDetails {
	private Provider pkcs11Provider;
    private KeyStore keyStore;
    
    public TokenDetails(Provider pkcs11Provider, KeyStore keyStore) {
        this.pkcs11Provider = pkcs11Provider;
        this.keyStore = keyStore;
    }
    
    public Provider getPkcs11Provider() {
        return pkcs11Provider;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
    
    public void clearTokenDetails() {
		this.pkcs11Provider = null;
		this.keyStore = null;
	}
    
    public PrivateKey getPrivateKey(String pin) throws Exception{
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

}
