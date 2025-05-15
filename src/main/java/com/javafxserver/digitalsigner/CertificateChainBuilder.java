package com.javafxserver.digitalsigner;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import com.javafxserver.exceptions.NoCertificateChainException;

public class CertificateChainBuilder {
	public static KeyStore keyStore;
	
	public static List<X509Certificate> buildCertificateChain(
			X509Certificate endEntityCert, 
			X509Certificate intermediateCertificate, 
			X509Certificate rootCACert) 
			throws NoCertificateChainException {
		List<X509Certificate> certChain = new ArrayList<>();
		certChain.add(endEntityCert);
		certChain.add(intermediateCertificate);
		certChain.add(rootCACert);
		return certChain;
	}
	
	/**
     * Orders an unordered list of X509Certificates into a valid certificate chain
     * from leaf (signing certificate) to root.
     */
	public static List<X509Certificate> buildOrderedChain(List<X509Certificate> unordered) throws Exception {
        Map<X500Principal, X509Certificate> subjectMap = new HashMap<>();

        for (X509Certificate cert : unordered) {
            subjectMap.put(cert.getSubjectX500Principal(), cert);
        }

        // Find the leaf certificate (issuer not in subjectMap or self-signed)
        X509Certificate leaf = null;
        for (X509Certificate cert : unordered) {
            if (!subjectMap.containsKey(cert.getIssuerX500Principal()) ||
                cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())) {
                leaf = cert;
                break;
            }
        }

        if (leaf == null) {
            throw new Exception("Leaf certificate not found.");
        }

        List<X509Certificate> ordered = new ArrayList<>();
        ordered.add(leaf);
        X509Certificate current = leaf;

        while (true) {
            X500Principal issuer = current.getIssuerX500Principal();
            if (issuer.equals(current.getSubjectX500Principal())) {
                break; // Reached root
            }

            X509Certificate issuerCert = subjectMap.get(issuer);
            if (issuerCert == null || ordered.contains(issuerCert)) {
                break;
            }

            ordered.add(issuerCert);
            current = issuerCert;
        }

        return ordered;
    }
}
