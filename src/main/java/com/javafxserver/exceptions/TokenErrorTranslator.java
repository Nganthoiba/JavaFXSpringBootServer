package com.javafxserver.exceptions;

import com.javafxserver.ui.DigiSignError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class TokenErrorTranslator {
    private static final Logger logger = LoggerFactory.getLogger(TokenErrorTranslator.class);

    public static DigiSignError getFriendlyMessage(Exception ex) {
        String title = "Token Error";
        String message = "An unexpected error occurred while accessing the USB token.";
        String techDetails = ex.getMessage() != null ? ex.getMessage() : "No technical details available.";

        if (ex instanceof ProviderException) {
            title = "Token Removed";
            message = "The USB token may have been removed or is not responding. Please reinsert it and try again.";
        } else if (ex instanceof DigitalSigningException) {
            title = "Signing Problem";
            message = "An error occurred during the signing operation. Please ensure a valid ePass USB token is inserted.";
        } else if (ex instanceof IOException) {
            String[] resolved = analyzeRootCause(ex);
            title = resolved[0];
            message = resolved[1];
            if(resolved[2] != null && !resolved[2].isEmpty()) {
				techDetails = resolved[2];
			}
        } else if (ex instanceof KeyStoreException) {
            title = "Initialization Failed";
            message = "The token could not be initialized. Try reinserting it or restarting the application.";
        } else if (ex instanceof CertificateException) {
            title = "Certificate Missing";
            message = "No valid certificate was found on the token. Please contact support.";
        } else if (ex instanceof NoSuchAlgorithmException) {
            title = "Verification Failed";
            message = "The system couldn't verify the token. Ensure it is properly connected.";
        } else {
            message = "An unexpected error occurred: " + techDetails;
        }

        logger.error("{}: {}", title, ex.getMessage(), ex);
        return new DigiSignError(title, message, techDetails);
    }

    private static String[] analyzeRootCause(Exception ex) {
        String title = "Token Error";
        String message = "An unexpected error occurred. Please contact support.";
        String techDetails = ex.getMessage();

        Throwable cause = ex.getCause();
        while (cause != null) {
            String msg = cause.getMessage() != null ? cause.getMessage() : "";
            techDetails = msg;
            String deeper = getDeeperMessage(cause);
            if(!deeper.isEmpty()) {
            	if (deeper.contains("CKR_PIN_INCORRECT")) {
                    title = "Incorrect PIN";
                    message = "The PIN you entered is incorrect. Please try again.";
                } else if (deeper.contains("CKR_PIN_EXPIRED")) {
                    title = "PIN Expired";
                    message = "The PIN has expired. Please contact support to reset it.";
                } else if (deeper.contains("CKR_PIN_LOCKED")) {
                    title = "PIN Locked";
                    message = "Too many incorrect PIN attempts. The token is now locked.";
                } else if (deeper.contains("CKR_KEY_HANDLE_INVALID")) {
                    title = "Invalid Key Handle";
                    message = "The key handle is invalid. Please reinsert the token.";
                } else if (deeper.contains("CKR_KEY_NOT_FOUND")) {
                    title = "Key Not Found";
                    message = "No key found on the token. Please ensure it is inserted properly.";
                } else if (deeper.contains("CKR_KEY_SIZE_RANGE")) {
                    title = "Key Size Error";
                    message = "The key size is invalid. Contact support.";
                }else if (msg.contains("CKR_SESSION_CLOSED")) {
                    title = "Session Closed";
                    message = "The session with the token was closed. Please reinsert the token.";
                    break;
                } else if (msg.contains("CKR_USER_NOT_LOGGED_IN")) {
                    title = "Login Required";
                    message = "You need to log in to the token with your PIN.";
                    break;
                } else if (msg.contains("CKR_TOKEN_NOT_PRESENT")) {
                    title = "Token Not Found";
                    message = "The USB token is not detected. Please reinsert it.";
                    break;
                } else if (msg.contains("CKR_DEVICE_ERROR")) {
                    title = "Device Error";
                    message = "There was a hardware issue with the token. Try a different USB port.";
                    break;
                } else if (msg.contains("CKR_GENERAL_ERROR")) {
                    title = "General Error";
                    message = "A general error occurred with the token. Please try again.";
                    break;
                } else if (msg.contains("CKR_SLOT_ID_INVALID")) {
                    title = "Invalid Slot";
                    message = "The token slot is invalid. Try reinserting the token.";
                    break;
                } else {
                    title = "Key Error";
                    message = "A key error occurred. Contact support.";
                }            	
            	break;
            }
            cause = cause.getCause();
        }

        return new String[]{title, message, techDetails};
    }

    private static String getDeeperMessage(Throwable throwable) {
        Throwable cause = throwable.getCause();
        return (cause != null && cause.getMessage() != null) ? cause.getMessage() : "";
    }
}
