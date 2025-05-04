package com.javafxserver.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;

public class PasswordDialog extends Dialog<String> {
	private PasswordField passwordField;

	public PasswordDialog() {
		setTitle("Password Dialog");
		setHeaderText("Please enter your secret PIN:");

		passwordField = new PasswordField();
		passwordField.setPromptText("PIN");

		VBox container = new VBox();
        container.setPadding(new Insets(10)); // Add padding around the PasswordField
        container.setSpacing(10); // Add spacing between elements if needed
        container.setStyle("-fx-alignment: center;"); // Center the PasswordField
        
        container.getChildren().add(passwordField);
        
		getDialogPane().setContent(container);

		ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

		setResultConverter(dialogButton -> {
			if (dialogButton == okButtonType) {
				return passwordField.getText();
			}
			return null;
		});
	}
	
}
