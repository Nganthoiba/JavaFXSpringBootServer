package com.javafxserver.ui;

import com.javafxserver.utils.UIUtils;

import javafx.application.Platform;

public class Error {
	private String titleString;
	private String messageString;
	
	public Error() {
		
	}
	
	public Error(String title, String message) {
		this.titleString = title;
		this.messageString = message;
	}
	
	public void setError(String title, String message) {
		this.titleString = title;
		this.messageString = message;
	}
	
	public void setTitle(String title) {
		this.titleString = title;
	}
	
	public String getTitle() {
		return titleString;
	}
	
	public void setMesage(String message) {
		this.messageString = message;
	}
	
	public String getMessage() {
		return messageString;
	}
	
	public void displayErrorDialog() {
		Platform.runLater(() -> UIUtils.showErrorAlert(titleString, messageString));
	}
}
