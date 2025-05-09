package com.javafxserver.digitalsigner.JWS;

import java.security.PrivateKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

public class JWSJsonSigner {
	public static String signJSON(String jsonDataString, PrivateKey privateKey) throws JOSEException {
		// Create header with RSA SHA-256 algorithm
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JOSE).build();
        
        //INitializing payload with the original json data (string)
        Payload payload = new Payload(jsonDataString);
        
        // Create the JWS object
        JWSObject jwsObject = new JWSObject(header, payload);
        
        // Sign it with the RSA private key
        RSASSASigner signer = new RSASSASigner(privateKey);        
        jwsObject.sign(signer);
        
        // Output compact serialization (3-part string)
        return jwsObject.serialize();
	}
}
