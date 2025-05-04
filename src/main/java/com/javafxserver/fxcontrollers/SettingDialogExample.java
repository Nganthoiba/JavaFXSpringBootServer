package com.javafxserver.fxcontrollers;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.javafxserver.config.Config;
import com.javafxserver.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingDialogExample extends Application {

    private TextField httpPortField;
    private TextField httpsPortField;
    private ListView<String> originListView;
    
    @Override
    public void start(Stage primaryStage) {
        Button settingsButton = new Button("Settings");
        settingsButton.setOnAction(e -> openSettingsDialog());

        VBox root = new VBox(10, settingsButton);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 300, 200));
        primaryStage.setTitle("Mini Server App");
        primaryStage.show();
    }

    public void openSettingsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Server Settings");

        httpPortField = new TextField();
        httpsPortField = new TextField();
        originListView = new ListView<>();
        originListView.setPrefHeight(100);

        Button addOriginBtn = new Button("+ Add");
        Button removeOriginBtn = new Button("- Remove");
        TextField newOriginField = new TextField();

        addOriginBtn.setOnAction(e -> {
            String origin = newOriginField.getText();
            if (!origin.isEmpty() && !originListView.getItems().contains(origin)) {
                originListView.getItems().add(origin);
                newOriginField.clear();
            }
        });

        removeOriginBtn.setOnAction(e -> {
            String selected = originListView.getSelectionModel().getSelectedItem();
            originListView.getItems().remove(selected);
        });

        HBox originControls = new HBox(5, newOriginField, addOriginBtn, removeOriginBtn);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");

        saveBtn.setOnAction(event -> {        	
            try {
				saveSettings();
				UIUtils.showAlert("Setting saved", "Your settings have been saved, you have to restart the server "+
				"for the changes to take effect.");
				dialog.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				UIUtils.showErrorAlert("Error Saving", "Error: "+e.getMessage());
				e.printStackTrace();
			}
            
        });
        cancelBtn.setOnAction(e -> dialog.close());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);

        grid.add(new Label("HTTP Port:"), 0, 0);
        grid.add(httpPortField, 1, 0);
        grid.add(new Label("HTTPS Port:"), 0, 1);
        grid.add(httpsPortField, 1, 1);
        grid.add(new Label("Allowed Origins:"), 0, 2);
        grid.add(originListView, 1, 2);
        grid.add(originControls, 1, 3);

        HBox buttons = new HBox(10, saveBtn, cancelBtn);
        buttons.setPadding(new Insets(10));

        VBox dialogLayout = new VBox(10, grid, buttons);
        Scene scene = new Scene(dialogLayout);
        dialog.setScene(scene);

        loadSettings();

        dialog.showAndWait();
    }

    private void loadSettings() {        
        try {
        	httpPortField.setText(String.valueOf(Config.getHttpPort()));
        	httpsPortField.setText(String.valueOf(Config.getHttpsPort()));
        	originListView.getItems().setAll(Config.getCorsOrigins());
        	
        	System.out.println(String.valueOf(Config.getHttpPort()));
        	System.out.println(String.valueOf(Config.getHttpsPort()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    private void saveSettings() throws Exception{        
        try {            
        	Config.setHttpPort(Integer.parseInt(httpPortField.getText()));
        	Config.setHttpsPort(Integer.parseInt(httpsPortField.getText()));
        	
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid port numbers entered");
        }
        
        List<String> allowedOriginStrings = new ArrayList<>(originListView.getItems());
        
        try {
        	Config.setCorsOrigins(allowedOriginStrings);
        } catch (Exception e) {
        	//showAlert("Unable to set allowed origins.");
        	throw new Exception("Unable to save allowed origins.", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
