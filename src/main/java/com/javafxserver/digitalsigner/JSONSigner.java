package com.javafxserver.digitalsigner;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.javafxserver.exceptions.InvalidPinException;
import com.javafxserver.service.TokenService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;

public class JSONSigner {
	public static String signJson(String jsonData, TokenService tokenService) 
			throws Exception {
		
		//Get chain of X509 certificates
		List<X509Certificate> certChain = tokenService.getX509CertificateChain();
		
		// Validate the certificate chain
		if(certChain == null || certChain.isEmpty()) {
			throw new IllegalArgumentException("Certificate chain is empty or null");
		}
		
		// Now generate a list of base64 encoded certificates in the chain
		List<String> x5cList = new ArrayList<>();
		for(X509Certificate cert : certChain) {
			String x5cCert = Base64.getEncoder().encodeToString(cert.getEncoded()).trim();			
			x5cList.add(x5cCert);
		}
		
		//Canonicalizing json string
		String canonicalizedJSONString = canonicalizeJson(jsonData);
		
		Map<String, Object> headerMap = new LinkedHashMap<>();
		headerMap.put("alg", "RS256");
		headerMap.put("typ", "JSON-Web-Signature");
		//This is usually a simple, unique identifier for the key/certificate inside the token and easily availabe from the Keystore
		
		//But this may vary depending on the token or slot, not always meaningful externally
		
		headerMap.put("kid", getThumbPrint(tokenService.getFirstX509Certificate()));//Widely Used
		
		//Adding the x5c list (certificate chain) to the header
		headerMap.put("x5c", x5cList);		
		
		PrivateKey privateKey = tokenService.getPrivateKey();
		Signature signature = Signature.getInstance("SHA256withRSA", tokenService.getPkcs11Provider());
		signature.initSign(privateKey);
		signature.update(canonicalizedJSONString.getBytes(StandardCharsets.UTF_8));
		
		byte[] signedData = signature.sign();
		
		// Generate Base64-encoded signature
		String base64Signature = Base64.getEncoder().encodeToString(signedData);
		
		//Now embed signature in json response
		Map<String, Object> signedJsonMap = new LinkedHashMap<>();
		signedJsonMap.put("header", headerMap);
		signedJsonMap.put("payload", new ObjectMapper().readTree(canonicalizedJSONString));
		signedJsonMap.put("signature", base64Signature);
		
		return new ObjectMapper().writeValueAsString(signedJsonMap);
	}
	
	public static String canonicalizeJson(String jsonData) throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true); // Enforce key ordering
        return mapper.writeValueAsString(mapper.readTree(jsonData)); // Normalize JSON structure
    }
	
	//Get the thumb print of the X509 Certificate
	//This method is to generate Certificate Thumbprint (SHA-1 or SHA-256)
	//This is a widely-used and unique fingerprint derived from the certificate and considered Unique, verifiable, standard practice.
	public static String getThumbPrint(X509Certificate certificate) 
			throws NoSuchAlgorithmException, CertificateEncodingException 
			 {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digest;
		try {
			digest = messageDigest.digest(certificate.getEncoded());
			StringBuilder stringBuilder = new StringBuilder();
			for(byte b : digest) {
				stringBuilder.append(String.format("%02x", b));
			}
			return stringBuilder.toString();
		} catch (CertificateEncodingException e) {
			throw new CertificateEncodingException("Unable to get thumbprint: " + e.getMessage(), e);			
		}
		
	}
	
	
	/*------------------------------------------------VERIFICATION PART------------------------------------------------*/	
	/**
	 * 
	 * @param jsonData
	 * @param signature
	 * @param publicKey
	 * @return
	 * @throws Exception
	 */
	
	public static boolean verifySignature(String jsonData, String signature, PublicKey publicKey) throws Exception {
	    String canonicalizedJson = canonicalizeJson(jsonData);
	    byte[] decodedSignature = Base64.getDecoder().decode(signature);

	    Signature verifier = Signature.getInstance("SHA256withRSA");
	    verifier.initVerify(publicKey);
	    verifier.update(canonicalizedJson.getBytes(StandardCharsets.UTF_8));

	    return verifier.verify(decodedSignature);
	}
	
	public static boolean verifySignedJson(String signedJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(signedJson);

        // Extract components
        String signature = rootNode.get("signature").asText();
        String canonicalizedJson = canonicalizeJson(rootNode.get("payload").toString());

        // Retrieve public key from certificate
        String certBase64 = rootNode.get("header").get("x5c").get(0).asText();
        PublicKey publicKey = extractPublicKey(certBase64);

        // Perform signature verification
        return verifySignature(canonicalizedJson, signature, publicKey);
    }
	
	public static PublicKey extractPublicKey(String base64Cert) throws CertificateException{
        byte[] certBytes = Base64.getDecoder().decode(base64Cert);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
        return certificate.getPublicKey();
    }
	
	
	// CODE FOR GENERATING JSON WEB SIGNATURE (JWS) - OPTIONAL
	public static String generateJWS(String jsonData, TokenService tokenService) 
			throws JOSEException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateEncodingException, UnrecoverableKeyException, InvalidPinException {
		PrivateKey privateKey = tokenService.getPrivateKey();
		
		//Get chain of X509 certificates
		List<X509Certificate> certChain = tokenService.getX509CertificateChain();
		
		// Validate the certificate chain
		if(certChain == null || certChain.isEmpty()) {
			throw new IllegalArgumentException("Certificate chain is empty or null");
		}
		
		
		X509Certificate certificate = certChain.get(0); //tokenService.getFirstX509Certificate();		
		String kid= getThumbPrint(certificate);
		
		
		// Now generate a list of base64 encoded certificates in the chain
		List<com.nimbusds.jose.util.Base64> x5cList = new ArrayList<>();
		for(X509Certificate cert : certChain) {
			String x5cCert = Base64.getEncoder().encodeToString(cert.getEncoded()).trim();
			com.nimbusds.jose.util.Base64 x5cBase64String = new com.nimbusds.jose.util.Base64(x5cCert);
			x5cList.add(x5cBase64String);
		}		
				
		//Canonicalizing json string
		String payloadJSONString = canonicalizeJson(jsonData);
		
		// Create JWS header
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JOSE)
            .keyID(kid)
            .x509CertChain(x5cList)
            .build();
        
        // Create payload
        Payload payload = new Payload(payloadJSONString);
        
        // Create JWS object
        JWSObject jwsObject = new JWSObject(header, payload);
        /*
        // Sign it
        RSASSASigner signer = new RSASSASigner(privateKey);
        jwsObject.sign(signer);
        */
        
        PKCS11RSASSASigner signer = new PKCS11RSASSASigner(privateKey, tokenService.getPkcs11Provider());
        jwsObject.sign(signer);
        
        
        // Output compact serialization
        String jwsCompact = jwsObject.serialize();
        System.out.println("Compact JWS: " + jwsCompact);
        
        return jwsCompact;		
	}
	
	
	/**
	 * Verifies the JWS signature.
	 * @param jwsString
	 * @return
	 * @throws JOSEException
	 * @throws ParseException
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 */
	public static boolean verifyJWS(String jwsString) 
			throws JOSEException, ParseException, KeyStoreException, IOException, CertificateException {		
		try {		
			// Parse and verify it
	        JWSObject parsedJWS = JWSObject.parse(jwsString);
	        JWSHeader headerJws = parsedJWS.getHeader();
	        List<com.nimbusds.jose.util.Base64> certChainList = headerJws.getX509CertChain();
	        if(certChainList == null || certChainList.isEmpty()) {
				throw new IllegalArgumentException("x5c parameter is missing in the JWS header");
			}
	        
	        // Extract the first certificate from the chain which is nothing but the user's certificate (public key)
	        String x5cBase64 = certChainList.get(0).toString();
	        
	        // Extract the public key from the certificate
	        PublicKey publicKey = extractPublicKey(x5cBase64);
	        
	        
	        RSASSAVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
	        
	        boolean isSignatureValid = parsedJWS.verify(verifier);
	        
	        if (isSignatureValid) {
	            System.out.println("Signature verified!");
	            System.out.println("Payload: " + parsedJWS.getPayload().toString());
	        } else {
	            System.out.println("Signature verification failed.");
	        }		
			
			return isSignatureValid;
		}
		catch (ParseException e) {
			throw new ParseException("Error parsing JWS: " + e.getMessage(), e.getErrorOffset());			
		}
	}
	
	// Method to verify the JWS Signature and extract the headers
	public static Map<String, Object> verifyJWSAndExtractHeaders(String jwsString) 
			throws JOSEException, ParseException, KeyStoreException, IOException, CertificateException {
		JWSObject parsedJWS = JWSObject.parse(jwsString);
		JWSHeader header = parsedJWS.getHeader();
		
		Map<String, Object> headers = new LinkedHashMap<>();
		headers.put("alg", header.getAlgorithm().getName());
		headers.put("typ", header.getType());
		headers.put("kid", header.getKeyID());
		
		List<com.nimbusds.jose.util.Base64> certChain = header.getX509CertChain();
		if (certChain != null && !certChain.isEmpty()) {
		    List<String> certList = new ArrayList<>();
		    for (com.nimbusds.jose.util.Base64 cert : certChain) {
		        certList.add(cert.toString()); // Ensures proper string representation
		    }
		    headers.put("x5c", certList);
		}
		
		return headers;
	}
	
	// Method to verify the JWS Signature and extract the payload
	public static Payload verifyJWSAndExtractPayload(String jwsString) 
			throws JOSEException, ParseException, KeyStoreException, IOException, CertificateException {
		JWSObject parsedJWS = JWSObject.parse(jwsString);
		return parsedJWS.getPayload();
	}

}
