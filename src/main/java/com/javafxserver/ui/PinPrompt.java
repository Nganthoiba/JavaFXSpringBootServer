package com.javafxserver.ui;

import java.util.Optional;

import javafx.application.Platform;
import java.util.function.Function;

import com.javafxserver.utils.LogWriter;
import com.javafxserver.utils.UIUtils;

public class PinPrompt {
	private static final int MAX_RETRIES = 3;
	
	/**
     * Requests user PIN via dialog with optional validation.
     *
     * @param validator a function that returns true for a valid PIN
     * @return the valid user-entered PIN
     * @throws RuntimeException if interrupted or maximum retries exceeded
     */
	public static String requestUserPinBlocking(Function<String, Boolean> validator) {
	    final String[] pin = {""};
	    final boolean[] isValid = {false};
	    final boolean[] isCancelled = {false};
//	    final int[] attempts = {0};
	
//	    while (attempts[0] < MAX_RETRIES) {
	        final Object lock = new Object();
	        
	        Platform.runLater(() -> {
//	        	int remainingAttempts = MAX_RETRIES - attempts[0];
	            PasswordDialog dialog = new PasswordDialog();
	            dialog.setTitle("Enter secret PIN:");
//	            dialog.setHeaderText("You have " + remainingAttempts + " attempt(s) remaining.");
	            Optional<String> result = dialog.showAndWait();
	
	            synchronized (lock) {
	            	/*
	                result.ifPresent(input -> {
	                	
	                });
	                */
	            	if(result.isPresent()) {
	            		String input = result.get();
	            		if(input.isEmpty()) {
	                		UIUtils.showAlert("Empty PIN", "PIN cannot be empty.");
	                		isValid[0] = false;
	                	}
	                	else if (validator.apply(input)) {
	                        pin[0] = input;
	                        isValid[0] = true;
	                    } else {
//	                        UIUtils.showAlert("Invalid PIN", 
//	                        		"Invalid PIN entered.\nYou have " + (MAX_RETRIES - (attempts[0] + 1))+" attempt(s) left");
	                    	UIUtils.showAlert("Invalid PIN", 
	                        		"Invalid PIN entered.");
	                        //System.out.println("");
	                        LogWriter.writeLog("Invalid PIN entered.");
	                    }
	            	}
	            	else {
	            		UIUtils.showAlert("PIN Cancelled", "You have cancelled the PIN entry.");
	            		LogWriter.writeLog("PIN Entry Cancelled by User.");	            		
	            		isCancelled[0] = true;
	            	}
	                lock.notify();
	            }
	        });
	
	        synchronized (lock) {
	            try {
	                lock.wait(); // Wait for dialog to close
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                throw new RuntimeException("PIN dialog was interrupted", e);
	            }
	        }
	
	        if (isValid[0]) {
	            return pin[0];
	        }
	        
	        if(isCancelled[0]) {
	        	throw new RuntimeException("PIN entry cancelled by user.");
	        }
//	        attempts[0]++;
//	    }
	    // 🛠 FIX: Show alert on JavaFX thread
	    Platform.runLater(() -> UIUtils.showAlert("Access Denied", "Maximum PIN attempts exceeded. Please restart the process."));
	    throw new RuntimeException("Maximum PIN attempts exceeded");
	}


    /**
     * Shortcut: Requests PIN with no validation
     */
    public static String requestUserPinBlocking() {
        return requestUserPinBlocking(p -> true);
    }
}
