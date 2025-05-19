package com.javafxserver.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.javafxserver.digitalsigner.TokenManager;
import com.javafxserver.exceptions.TokenErrorTranslator;
import com.javafxserver.ui.DigiSignError;

public class ResponseBuilder {
	public static ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e, HttpStatus status) {
        TokenManager.logoutToken();
        e.printStackTrace();
        
        DigiSignError error = TokenErrorTranslator.getFriendlyMessage(e);
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("message", error.getTitle() + ": " + error.getMessage());
        errorBody.put("techDetails", error.getCausedBy());
        errorBody.put("status", status.value());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody);
    }
    
    public static ResponseEntity<Map<String, Object>> buildResponse(String message, HttpStatus status, String techDetails) {
		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("message", message);
		responseBody.put("status", status.value());
		responseBody.put("techDetails", techDetails);
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseBody);
    }
}
