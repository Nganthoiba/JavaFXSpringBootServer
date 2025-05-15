package com.javafxserver.digitalsigner;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class CALoader {
	public static X509Certificate loadRootCA() throws Exception {
        // Load the certificate from resources		
		
        try (InputStream is = CALoader.class.getClassLoader().getResourceAsStream("certs/CCAIndia2022.cer")) {
            if (is == null) {
                throw new FileNotFoundException("Root CA certificate not found in resources!");
            }
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(is);
        }
    }
	
	public static X509Certificate loadCert(String path) throws Exception {
	    try (InputStream is = CALoader.class.getClassLoader().getResourceAsStream(path)) {
	        if (is == null) {
	            throw new FileNotFoundException("Certificate not found: " + path);
	        }
	        CertificateFactory cf = CertificateFactory.getInstance("X.509");
	        return (X509Certificate) cf.generateCertificate(is);
	    }
	}

	public static boolean containsCertificate(List<X509Certificate> chain, X509Certificate certToCheck) {
	    for (X509Certificate cert : chain) {
	        if (cert.getSubjectX500Principal().equals(certToCheck.getSubjectX500Principal())) {
	            return true;
	        }
	    }
	    return false;
	}
}
