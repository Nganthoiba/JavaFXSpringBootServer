package com.javafxserver.digitalsigner;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class RootCALoader {
	public static X509Certificate loadRootCA() throws Exception {
        // Load the certificate from resources
		
		//rootCA.cer is the root CA certificate for e-Mudra Sub CA for Class 3 individual 2022
        try (InputStream is = RootCALoader.class.getClassLoader().getResourceAsStream("certs/rootCA.cer")) {
            if (is == null) {
                throw new FileNotFoundException("Root CA certificate not found in resources!");
            }
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(is);
        }
    }
}
