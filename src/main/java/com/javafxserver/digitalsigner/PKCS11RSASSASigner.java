package com.javafxserver.digitalsigner;

import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.util.Collections;
import java.util.Set;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;

public class PKCS11RSASSASigner implements JWSSigner{

	private final PrivateKey privateKey;
    private final Provider provider;
    
    public PKCS11RSASSASigner(PrivateKey privateKey, Provider provider) {
		this.privateKey = privateKey;
		this.provider = provider;
	}

	@Override
	public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
		try {
			String algorithm = resolveSignatureAlgorithm(header.getAlgorithm());
            Signature signature = Signature.getInstance(algorithm, provider);
			signature.initSign(privateKey);
			signature.update(signingInput);
			byte[] signatureBytes = signature.sign();
			return Base64URL.encode(signatureBytes);
		}catch(Exception e) {
			throw new JOSEException("Signing with PKCS#11 key failed", e);
		}
	}
	
	@Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return Collections.singleton(JWSAlgorithm.RS256); // Add others if needed
    }

    @Override
    public JCAContext getJCAContext() {
        return new JCAContext();
    }
	
	private String resolveSignatureAlgorithm(Algorithm alg) throws JOSEException {
        if (JWSAlgorithm.RS256.equals(alg)) return "SHA256withRSA";
        if (JWSAlgorithm.RS384.equals(alg)) return "SHA384withRSA";
        if (JWSAlgorithm.RS512.equals(alg)) return "SHA512withRSA";
        throw new JOSEException("Unsupported algorithm: " + alg);
    }
}
