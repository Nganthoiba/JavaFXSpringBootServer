package com.javafxserver.digitalsigner.JWS;

import java.security.PublicKey;
import java.text.ParseException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;

public class JWSJsonVerifier {
	public static boolean verifyJWS(String jwsString, PublicKey publicKey) 
			throws ParseException, JOSEException {		
		//Parse the compacted jws string
		JWSObject jwsObject = JWSObject.parse(jwsString);
		
		//Create a verifier
		JWSVerifier jwsVerifier = new RSASSAVerifier((java.security.interfaces.RSAPublicKey) publicKey);
		
		//Verify the signature
		return jwsObject.verify(jwsVerifier);
	}
	
	public static String retrievePayload(String jwsString) throws ParseException {
		//Parse the compacted jws string
		JWSObject jwsObject = JWSObject.parse(jwsString);
		return jwsObject.getPayload().toString();
	}
}
