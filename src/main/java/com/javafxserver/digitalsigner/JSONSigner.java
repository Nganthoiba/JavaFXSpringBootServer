package com.javafxserver.digitalsigner;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.javafxserver.service.TokenService;

public class JSONSigner {
	public static String signJson(String jsonData, TokenService tokenService) 
			throws Exception {
		
		//Canonicalizing json string
		String canonicalizedJSONString = canonicalizeJson(jsonData);
		
		Map<String, Object> headerMap = new LinkedHashMap<>();
		headerMap.put("alg", "RS256");
		headerMap.put("typ", "JSONSignature");
		//This is usually a simple, unique identifier for the key/certificate inside the token and easily availabe from the Keystore
		//headerMap.put("kid", tokenService.getCertificateAlias()); 
		//But this may vary depending on the token or slot, not always meaningful externally
		
		headerMap.put("kid", getThumbPrint(tokenService.getCertificate()));//Widely Used
		headerMap.put("x5c", List.of(Base64.getEncoder().encodeToString(
			    tokenService.getCertificate().getEncoded()).trim()));
		
		
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
			throws NoSuchAlgorithmException, 
			CertificateEncodingException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digest = messageDigest.digest(certificate.getEncoded());
		StringBuilder stringBuilder = new StringBuilder();
		for(byte b : digest) {
			stringBuilder.append(String.format("%02x", b));
		}
		return stringBuilder.toString();
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
	
	public static PublicKey extractPublicKey(String base64Cert) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(base64Cert);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
        return certificate.getPublicKey();
    }

}
