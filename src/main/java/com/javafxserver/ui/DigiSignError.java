package com.javafxserver.ui;

import com.javafxserver.utils.UIUtils;

import javafx.application.Platform;

public class DigiSignError {
	private String titleString;
	private String messageString;
	private String causedByString;
	
	public DigiSignError() {
		
	}
	
	public DigiSignError(String title, String message) {
		this.titleString = title;
		this.messageString = message;
	}
	
	public DigiSignError(String title, String message, String causedBy) {
		this.titleString = title;
		this.messageString = message;
		this.causedByString = causedBy;
	}
	
	public void setError(String title, String message) {
		this.titleString = title;
		this.messageString = message;
	}
	
	public void setError(String title, String message, String causedBy) {
		this.titleString = title;
		this.messageString = message;
		this.causedByString = causedBy;
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
	
	public String getCausedBy() {
		return causedByString;
	}
	
	public void setCausedBy(String causedBy) {
		this.causedByString = causedBy;
	}
	
	public void displayErrorDialog() {
		Platform.runLater(() -> UIUtils.showErrorAlert(titleString, messageString));
	}
}
