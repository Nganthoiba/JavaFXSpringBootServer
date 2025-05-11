package com.javafxserver.digitalsigner;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.tsp.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class TSAClient {
	private final static String TSA_URL = "http://timestamp.digicert.com";
	//http://sha256timestamp.ws.symantec.com/sha256/timestamp
	//http://timestamp.comodoca.com/rfc3161
	//http://timestamp.globalsign.com/scripts/timstamp.dll
	//http://timestamp.digicert.com
	//http://timestamp.sectigo.com
	//http://tsa.startssl.com/rfc3161
	//http://tsa.safecreative.org
	//http://ca.signfiles.com/TSAServer.aspx
	
	public static TimeStampToken getTimeStampToken(byte[] dataToSign) 
			throws MalformedURLException, IOException, NoSuchAlgorithmException, TSPException, URISyntaxException{
        // Create a digest of the data
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(dataToSign);

        // Create a TimeStampRequest
        TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
        tsqGenerator.setCertReq(true);
        TimeStampRequest request = tsqGenerator.generate(
                //new ASN1ObjectIdentifier("1.3.14.3.2.26"), // OID for SHA-256
        		new ASN1ObjectIdentifier("2.16.840.1.101.3.4.2.1"),
                hash
        );

        // Send the request to the TSA server
        byte[] requestBytes = request.getEncoded();
        
        HttpURLConnection connection = (HttpURLConnection) new URI(TSA_URL).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/timestamp-query");
        connection.setRequestProperty("Accept", "application/timestamp-reply");
        connection.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));
        connection.setRequestProperty("User-Agent", "Java TSA Client");
        connection.setConnectTimeout(60000); //60 seconds
        connection.setReadTimeout(60000);
        
        connection.getOutputStream().write(requestBytes);

        // Read the TSA response
        try (InputStream inputStream = connection.getInputStream()) {
            TimeStampResponse response = new TimeStampResponse(inputStream);
            response.validate(request);

            // Return the TimeStampToken
            return response.getTimeStampToken();
        }
    }
	
	/**
	 * Explanation

	1. Hashing the Data: The data to be signed is hashed using SHA-256.
	2. TimeStampRequest: A request is created with the hash and sent to the TSA server.
	3. TimeStampResponse: The response is validated, and the TimeStampToken is extracted.
	 */
}
