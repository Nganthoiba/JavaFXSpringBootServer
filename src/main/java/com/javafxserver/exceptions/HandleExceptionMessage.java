package com.javafxserver.exceptions;

public class HandleExceptionMessage {
	public static String getMessage(String message, Throwable throwable) {
		
		String className = throwable.getClass().getSimpleName();
		String causeMessage = throwable.getMessage();

			
		//checking the cause of the exception
		if(throwable instanceof java.security.UnrecoverableKeyException) {
			return("Error: "+className+", "+message);
		}			
		if(throwable instanceof java.security.KeyStoreException) {
			return("Error: "+className+", "+message+ " Please check if the PKCS#11 driver is installed and the path is correct or the device is plugged in.");
		}
		else if(throwable instanceof java.security.ProviderException) {
			return("Error: "+className+", "+message+ " Please check if the PKCS#11 driver is installed and the path is correct or the device is plugged in.");
		}
		else if(causeMessage.contains("CKR_PIN_INCORRECT")) {
			return("Error: "+className+", "+causeMessage+ " You may have entered incorrect PIN, please check.");
		}
		
		if(message.contains("PKCS11 not found")) {
			return("Error: "+message+ " Please check if the PKCS#11 driver is installed and the path is correct or the device is plugged in.");
		}
		return("An error occurred: " + className + " - " + message);
	    
	}
	
	public static String getMessage(Exception e) {
    	Throwable cause = e;
    	while (cause.getCause() != null) {
			cause = cause.getCause();
		}
	    
		if (e.getMessage() != null) {
			String message = e.getMessage();			
			return getMessage(message, cause);
		}
		return("An unknown error occurred.");
	}
}
