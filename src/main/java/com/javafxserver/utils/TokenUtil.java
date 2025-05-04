package com.javafxserver.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Enumeration;

//import sun.security.pkcs11.wrapper.*;

import com.javafxserver.config.Config;
import com.javafxserver.exceptions.SunPKCS11NotFoundException;

import java.security.Provider;
import java.security.Security;

public class TokenUtil {
	//private static PKCS12 pkcs11;
	public static String ERROR = "";
	public static boolean isTokenPresent(Provider pkcs11Provider, char[] pin) {
		try {
            KeyStore ks = KeyStore.getInstance(Config.PKCS11, pkcs11Provider);
            ks.load(null, pin); // Load with PIN to authenticate
            Enumeration<String> aliases = ks.aliases(); // Try listing aliases
            return aliases.hasMoreElements(); // At least one alias means token is usable
        } catch (Exception e) {
        	//ProviderException | IOException | java.security.GeneralSecurityException
            // Could be token not present, removed, invalid PIN, or PKCS11 error
        	ERROR = "Token not present or not accessible: " + e.getMessage();
            System.err.println(ERROR);
            return false;
        }
	}
	
	public static Provider loadPkcs11Provider(String name, String library, int slotIndex) 
			throws SunPKCS11NotFoundException, IOException {
		// Create a temporary configuration file
        Path configFile = Files.createTempFile("pkcs11", ".cfg");
        String config = String.format(
            "name = %s%n" +
            "library = %s%n" +
            "slotListIndex = %d%n",
            name, library, slotIndex
        );
        Files.write(configFile, config.getBytes());

        // Load the SunPKCS11 provider
        Provider provider = Security.getProvider(Config.SUN_PKCS11);
        if (provider == null) {
            throw new SunPKCS11NotFoundException("SunPKCS11 provider not available");
        }

        // Configure the provider using the configuration file
        Provider configured = provider.configure(configFile.toString());

        // Delete the temporary configuration file
        Files.delete(configFile);

        return configured;    
    }
	
	public static Provider loadPkcs11ProviderFromFile(File configFile) throws Exception {
        
        Provider base = Security.getProvider(Config.SUN_PKCS11);
        if (base == null) {
            throw new SunPKCS11NotFoundException("SunPKCS11 not available in current JDK.");
        }

        Provider configured = base.configure(configFile.toString());
        return configured;
    }
	
}
