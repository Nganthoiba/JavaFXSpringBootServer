package com.javafxserver.exceptions;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.javafxserver.ui.DigiSignError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenErrorTranslator {
	private static final Logger logger = LoggerFactory.getLogger(TokenErrorTranslator.class);
    public static DigiSignError getFriendlyMessage(Exception ex) {
        String title = "Token Error";
        String message = "An unexpected error occurred while accessing the USB token.";
        String techDetails = ex.getMessage();

        // Handle known exception types
        if (ex instanceof java.security.ProviderException) {
            title = "Token Removed";
            message = "The USB token may have been removed or is not responding. Please reinsert it and try again.";
        } else if (ex instanceof IOException) {
            title = "Access Problem";
            message = "There was a problem accessing the USB token. Please make sure it is properly inserted. "+techDetails;
        } else if (ex instanceof KeyStoreException) {
            title = "Initialization Failed";
            message = "The token could not be initialized. Try reinserting it or restarting the application.";
        } else if (ex instanceof CertificateException) {
            title = "Certificate Missing";
            message = "No valid certificate was found on the token. Please contact support if this continues.";
        } else if (ex instanceof NoSuchAlgorithmException) {
            title = "Verification Failed";
            message = "The system couldn't verify the token. Please ensure it is connected and try again.";
        }
        else {        				// Handle other exceptions
			
			if (techDetails == null) {
				techDetails = "No additional information available.";
			}
			message = "An unexpected error occurred: " + techDetails;
        }

        // Analyze root cause for deeper PKCS#11 errors
        
        /*
         * Throwable cause = ex.getCause();
        if (cause != null) {
            techDetails = cause.getMessage();
            String simpleName = cause.getClass().getSimpleName();
            String msg = cause.getMessage() != null ? cause.getMessage() : "";

            if ("PKCS11Exception".equals(simpleName)) {
                if (msg.contains("CKR_PIN_INCORRECT")) {
                    title = "Incorrect PIN";
                    message = "The PIN you entered is incorrect. Please try again.";
                } else if (msg.contains("CKR_PIN_LOCKED")) {
                    title = "PIN Locked";
                    message = "Too many incorrect PIN attempts. The token is now locked. Contact support to unlock it.";
                } else if (msg.contains("CKR_USER_NOT_LOGGED_IN")) {
                    title = "Login Required";
                    message = "You need to log in to the token with your PIN.";
                } else if (msg.contains("CKR_TOKEN_NOT_PRESENT")) {
                    title = "Token Not Found";
                    message = "The USB token is not detected. Please reinsert it and try again.";
                } else if (msg.contains("CKR_DEVICE_ERROR")) {
                    title = "Device Error";
                    message = "There was a hardware communication issue with the USB token. Try using a different USB port.";
                } else if (msg.contains("CKR_GENERAL_ERROR")) {
                    title = "General Error";
                    message = "A general error occurred while accessing the token. Try reinserting the token or restarting your system.";
                } else if (msg.contains("CKR_SLOT_ID_INVALID")) {
                    title = "Invalid Slot";
                    message = "The system couldn't access the token's slot. Please replug the token.";
                } else {
                    title = "PKCS#11 Error";
                    message = "An error occurred in the token module. Please contact support with the error details.";
                }
            }
        }
		*/
        // Optionally print or log the full stack trace
        //ex.printStackTrace();
        logger.error(title + " : " + ex.getMessage(), ex);
        return new DigiSignError(title, message, techDetails);
    }
}
