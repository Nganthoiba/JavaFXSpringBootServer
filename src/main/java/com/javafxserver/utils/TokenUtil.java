package com.javafxserver.utils;

import java.io.File;
//import sun.security.pkcs11.wrapper.*;
import com.javafxserver.config.Config;
import com.javafxserver.exceptions.SunPKCS11NotFoundException;

import java.security.Provider;
import java.security.Security;

public class TokenUtil {
	//private static PKCS12 pkcs11;
	public static String ERROR = "";
	
	
	public static Provider loadPkcs11ProviderFromFile(File configFile) throws SunPKCS11NotFoundException{
        
        Provider base = Security.getProvider(Config.SUN_PKCS11);
        if (base == null) {
        	// SunPKCS11 is not available in current JDK.
        	ERROR = Config.SUN_PKCS11 + " not available in current JDK.";
        	System.out.println(ERROR);
            throw new SunPKCS11NotFoundException(ERROR);
        }

        Provider configured = base.configure(configFile.toString());
        return configured;
    }
	
}
