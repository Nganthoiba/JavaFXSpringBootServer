package com.javafxserver.web.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javafxserver.config.Config;
import com.javafxserver.utils.ConvertFile;
import com.javafxserver.utils.ResponseBuilder;
import com.javafxserver.web.request.VerifyJsonRequest;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.Payload;
import com.javafxserver.digitalsigner.Coordinate;
import com.javafxserver.digitalsigner.JSONSigner;
import com.javafxserver.digitalsigner.PDFSigner;
import com.javafxserver.digitalsigner.Rectangle;
import com.javafxserver.digitalsigner.SignatureDetail;
import com.javafxserver.digitalsigner.TokenManager;
import com.javafxserver.digitalsigner.XMLSigner;
import com.javafxserver.exceptions.HandleExceptionMessage;

@RestController
@RequestMapping("/api")
public class AppController {

    @PostMapping("/esignPDF")
    public ResponseEntity<?> digitallySignPdf(
            @RequestParam("pdf_file") MultipartFile multipartFile,
            @RequestParam("x") float x,
            @RequestParam("y") float y,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "rectangle_height", required = false) Integer rect_height,
            @RequestParam(value = "rectangle_width", required = false) Integer rect_width
    		) {

        byte[] signedPdfBytes = null;
        File tempFile = null;
        try {
            if (multipartFile.isEmpty()) {
                throw new IllegalArgumentException("No file is uploaded");
            }
            TokenManager.initializeTokenIfNeeded(); 
            SignatureDetail signDetail = new SignatureDetail();
            signDetail.coordinate = new Coordinate(x, y);
            signDetail.location = location==null?"": location;
            signDetail.reason = reason==null?"": reason;
            
            if(pageNo != null) {
            	signDetail.pageNumber = pageNo;
            }
            if(rect_height != null && rect_width != null) {
            	signDetail.rectangle = new Rectangle(rect_height, rect_width);
            }            
            
            tempFile = ConvertFile.toFile(multipartFile);
            signedPdfBytes = PDFSigner.signPDF(tempFile, Config.PIN, TokenManager.tokenService, signDetail);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "signed_doc.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(signedPdfBytes);

        } catch (IllegalArgumentException | 
        		IndexOutOfBoundsException | 
        		IOException | 
        		URISyntaxException e) {
        	return ResponseBuilder.buildErrorResponse(e, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
        	return ResponseBuilder.buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    @PostMapping("/esignJSON")
    public ResponseEntity<String> esignJson(@RequestBody String jsonData) {
    	try {
    		TokenManager.initializeTokenIfNeeded();            
            String signedJson = JSONSigner.signJson(jsonData, TokenManager.tokenService);
            return ResponseEntity.ok(signedJson);
        } 
    	catch(IllegalArgumentException | 
        		IndexOutOfBoundsException | 
        		IOException | 
        		URISyntaxException e) {
    		String messageString = HandleExceptionMessage.getMessage(e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + messageString + "\"}");
    		
    	}
    	catch (Exception e) {
        	TokenManager.logoutToken();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * This end point will verify the signed JSON data
     * 
     * @param signedJsonData
     * @return
     */
    
    @PostMapping("/verifyJSON")
    public ResponseEntity<Map<String, String>> verifySignedJson(@RequestBody VerifyJsonRequest signedJsonData){
    	String payload = signedJsonData.payload;
    	String signatureString = signedJsonData.signature;
    	
    	try {
    		TokenManager.initializeTokenIfNeeded();
            boolean verifyFlag = JSONSigner.verifySignature(payload, signatureString, TokenManager.tokenService.getPublicKey());
            return verifyFlag 
            	    ? ResponseEntity.ok(Map.of("message", "Valid JSON data.")) 
            	    : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid JSON data."));

            
    	}
    	catch(Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
    	}
    	
    }
    
    @PostMapping("/verifyWholeJSON")
    public ResponseEntity<Map<String, String>> verifyWholeSignedJson(@RequestBody String signedJsonString){
    	try {
    		ObjectMapper mapper = new ObjectMapper();
    		JsonNode rootNode = mapper.readTree(signedJsonString);
    		
    		//Extract component
    		String signatureString = rootNode.get("signature").asText();
    		String payloadString = JSONSigner.canonicalizeJson(rootNode.get("payload").toString());
    		
    		// Retrieve public key from embedded certificate
    		String Base64encodedCertificate = rootNode.get("header").get("x5c").get(0).asText();
    		PublicKey publicKey = JSONSigner.extractPublicKey(Base64encodedCertificate);
    		
    		boolean verifyFlag = JSONSigner.verifySignature(payloadString, signatureString, publicKey);
    		return verifyFlag 
            	    ? ResponseEntity.ok(Map.of("message", "Signature verified. JSON data is found to be true.")) 
            	    : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Signature could not be verified. JSON data may not be correct"));
    		
    	}
    	catch(Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
    	}
    }
    
    @PostMapping("/esignXMLfile")
    public ResponseEntity<?> signXmlAndDownload(@RequestParam("xml_file") MultipartFile multipartFile) throws Exception {
    	byte[] signedXMLBytes = null;
    	File tempXMLFile = null;
		try {
			if (multipartFile.isEmpty()) {
				throw new IllegalArgumentException("No file is uploaded");
			}
			
			TokenManager.initializeTokenIfNeeded(); 
			tempXMLFile = ConvertFile.toFile(multipartFile);
			
			signedXMLBytes = XMLSigner.signXML(tempXMLFile, TokenManager.tokenService);
			
			// Return response with download headers
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
	        headers.setContentDisposition(ContentDisposition.attachment().filename("signed_output.xml").build());
	        headers.setContentLength(signedXMLBytes.length);

	        return new ResponseEntity<>(signedXMLBytes, headers, HttpStatus.OK);
			
		} catch (IllegalArgumentException | 
				IndexOutOfBoundsException | 
				IOException | 
				URISyntaxException e) {
			return ResponseBuilder.buildErrorResponse(e, HttpStatus.BAD_REQUEST);

		} catch (Exception e) {
			return ResponseBuilder.buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			if (tempXMLFile != null && tempXMLFile.exists()) {
				tempXMLFile.delete();
			}
		}
        
    }
    
    
    @PostMapping("/esignXMLdata")
    public ResponseEntity<?> signXmlAndDownload(@RequestBody String xmlData) {
        try {
            if (xmlData == null || xmlData.trim().isEmpty()) {
                throw new IllegalArgumentException("No XML data is provided");
            }

            TokenManager.initializeTokenIfNeeded();
            
            byte[] signedXMLByteData = XMLSigner.signXML(xmlData, TokenManager.tokenService);

            // Option 1: Return as Base64 (safer for transport)
            String signedXMLBase64 = Base64.getEncoder().encodeToString(signedXMLByteData);

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("message", "XML data signed successfully.");
            responseMap.put("signedXMLBase64", signedXMLBase64);
            TokenManager.logoutToken();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseMap);

        } catch (IllegalArgumentException e) {
            return ResponseBuilder.buildErrorResponse(e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ResponseBuilder.buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/getJWS")
    public ResponseEntity<?> getJWS(@RequestBody String jsonData) {
		try {
			if (jsonData == null || jsonData.trim().isEmpty()) {
				throw new IllegalArgumentException("No JSON data is provided");
			}

			TokenManager.initializeTokenIfNeeded();
			
			String jws = JSONSigner.generateJWS(jsonData, TokenManager.tokenService);

			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("message", "JWS generated successfully.");
			responseMap.put("jws", jws);
			
			TokenManager.logoutToken();
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(responseMap);

		}
		catch(JOSEException e) {
			e.printStackTrace();
			return ResponseBuilder.buildResponse("Unable to generate JSON Web Signature (JWS) string", HttpStatus.BAD_REQUEST, e.getMessage());
		}
		catch (IllegalArgumentException e) {
			return ResponseBuilder.buildErrorResponse(e, HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return ResponseBuilder.buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
    /**
     * This end point will verify the JWS data and responds with the header and payload if
     * jws is valid otherwise it will respond with an error message
     * 
     * @param jwsString
     * @return
     * 
     */
    @PostMapping("/verifyJWS")
    public ResponseEntity<?> verifyJWS(@RequestParam("jws") String jwsString) {
    	try {
    		if (jwsString == null || jwsString.trim().isEmpty()) {
    			throw new IllegalArgumentException("No JWS data is provided");
    		}
    		
    		boolean isValid = JSONSigner.verifyJWS(jwsString);
    		if(!isValid) {
    			throw new IllegalArgumentException("JWS is not a valid JWS string");
    		}
    		
    		//Get header and payload from the jws    		
    		Map<String, Object> headers = JSONSigner.verifyJWSAndExtractHeaders(jwsString); 
    		Payload payload = JSONSigner.verifyJWSAndExtractPayload(jwsString);
    		
    		//Canonicalizing json string
    		String canonicalizedJSONString = JSONSigner.canonicalizeJson(payload.toString());   		    		
    		
    		Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("message", "JWS string is valid.");
			responseMap.put("header", headers);
			responseMap.put("payload", new ObjectMapper().readTree(canonicalizedJSONString));
			
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(responseMap);
    	}
    	catch(ParseException e) {
            return ResponseBuilder.buildResponse("The JWS string is not valid", HttpStatus.BAD_REQUEST, e.getMessage());
    	}
    	catch(IllegalArgumentException e) {
			return ResponseBuilder.buildErrorResponse(e, HttpStatus.BAD_REQUEST);
		}
		catch(Exception e) {
			return ResponseBuilder.buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
		}
    } 
   
}
