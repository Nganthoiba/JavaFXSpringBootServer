package com.javafxserver.digitalsigner;

import java.security.cert.X509Certificate;
import java.util.List;

import com.javafxserver.exceptions.NoCertificateChainException;

public class CertificateChainValidator {
	public static boolean validateCertificateChain(List<X509Certificate> certChain) {
		try {
			if (certChain == null || certChain.isEmpty()) {
				throw new NoCertificateChainException("Certificate chain is empty or null.");
			}

			for (int i = 0; i < certChain.size() - 1; i++) {
				X509Certificate currentCert = certChain.get(i);
				X509Certificate nextCert = certChain.get(i + 1);

				// Check if the issuer of the current certificate matches the subject of the next certificate
				if (!currentCert.getIssuerX500Principal().equals(nextCert.getSubjectX500Principal())) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
