package com.javafxserver.web.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSigner;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javafxserver.config.Config;
import com.javafxserver.ui.DigiSignError;
import com.javafxserver.ui.PinPrompt;
import com.javafxserver.utils.ConvertFile;
import com.javafxserver.web.request.VerifyJsonRequest;
import com.javafxserver.digitalsigner.Coordinate;
import com.javafxserver.digitalsigner.Epass2003PDFSigner;
import com.javafxserver.digitalsigner.JSONSigner;
import com.javafxserver.digitalsigner.PDFSigner;
import com.javafxserver.digitalsigner.Rectangle;
import com.javafxserver.digitalsigner.SignatureDetail;
import com.javafxserver.digitalsigner.TokenManager;
import com.javafxserver.exceptions.EmptyPinException;
import com.javafxserver.exceptions.HandleExceptionMessage;
import com.javafxserver.exceptions.TokenErrorTranslator;


@RestController
public class AppController {
	
	@PostMapping("/api/simpleEssignPDF")
	public ResponseEntity<Map<String, String>> simpleEssignPDF(
			@RequestParam("pdf_file") MultipartFile multipartFile
			) {
		File tempFile = null;
		try {
			if (multipartFile.isEmpty()) {
				throw new IllegalArgumentException("No file is uploaded");
			}
			initializeTokenIfNeeded(); 
			tempFile = ConvertFile.toFile(multipartFile);
			
			File outputDir = new File(Config.APP_PATH + File.separator + "signedFiles");
			if (!outputDir.exists()) {
			    outputDir.mkdirs(); // Create the directory if it does not exist
			}
			File outputFile = new File(outputDir, "_signed.pdf");
			
			//tempFile.getAbsolutePath().replace(".pdf", "_signed.pdf");
			String pkcs11LibraryPathString = Config.getEpassConfig().get("library")+"";
			
			Epass2003PDFSigner.signPDF(tempFile, outputFile, pkcs11LibraryPathString, Config.PIN);
			
		} catch (IllegalArgumentException | 
				IndexOutOfBoundsException | 
				IOException | 
				URISyntaxException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
		} finally {
			if (tempFile != null && tempFile.exists()) {
				tempFile.delete();
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Document Esigned Successfully"));
	}

    @PostMapping("/api/esignPDF")
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
            initializeTokenIfNeeded(); 
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
        	return buildErrorResponse(e, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
        	return buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    @PostMapping("/api/esignJSON")
    public ResponseEntity<String> esignJson(@RequestBody String jsonData) {
    	try {
    		initializeTokenIfNeeded();            
            String signedJson = JSONSigner.signJson(jsonData, TokenManager.tokenService); // Your signing logic
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
    
    @PostMapping("/api/verifyJSON")
    public ResponseEntity<Map<String, String>> verifySignedJson(@RequestBody VerifyJsonRequest signedJsonData){
    	String payload = signedJsonData.payload;
    	String signatureString = signedJsonData.signature;
    	
    	try {
    		initializeTokenIfNeeded();
            boolean verifyFlag = JSONSigner.verifySignature(payload, signatureString, TokenManager.tokenService.getPublicKey());
            return verifyFlag 
            	    ? ResponseEntity.ok(Map.of("message", "Valid JSON data.")) 
            	    : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid JSON data."));

            
    	}
    	catch(Exception e) {
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
    	}
    	
    }
    
    @PostMapping("/api/verifyWholeJSON")
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
    
    
    @GetMapping("/api/test")    
    public Map<String, String> testApi() {
    	Map<String, String> responseMap = new HashMap<>();
    	responseMap.put("message", "Hello, World!");
        return responseMap;
    }
    
    @PostMapping("/api/test")
    public ResponseEntity<String> testApi(@RequestBody String jsonData) {
    	return ResponseEntity.ok(jsonData);
    }
    
    /**
     * This method will initialize token only if it is necessary
     * @throws Exception
     */
    private void initializeTokenIfNeeded() throws Exception{
    	/*
    	if(!TokenManager.isTokenPresent()) {
    		throw new TokenInitializationFailedException("Token is not inserted or not detected, make sure that the usb token is inserted properly");
    	}
    	*/
    	
    	if(!TokenManager.isUSBTokenInitialized()) {
        	if(Config.PIN != null && TokenManager.tokenError != null) {
        		TokenManager.tokenError.displayErrorDialog();
        		throw new Exception(TokenManager.tokenError.getTitle()+": "+TokenManager.tokenError.getMessage());            		
        	}
        	
        	String secretPin = PinPrompt.requestUserPinBlocking(pin -> {
                if (Config.PIN != null && !pin.equals(Config.PIN)) {
                    return false;
                }
                return true;
            });        	
        	Config.PIN = secretPin;        	
        }
        
        if(Config.PIN == null) {
        	throw new EmptyPinException("You have not enter secret PIN");
        }
        
        TokenManager.initializeToken(Config.PIN);        
    }
    
    private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e, HttpStatus status) {
        TokenManager.logoutToken();
        e.printStackTrace();
        
        DigiSignError error = TokenErrorTranslator.getFriendlyMessage(e);
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("message", error.getTitle() + ": " + error.getMessage());
        //errorBody.put("message", HandleExceptionMessage.getMessage(e));
        errorBody.put("status", status.value());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody);
    }
}
