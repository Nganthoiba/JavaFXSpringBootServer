package com.javafxserver.fxcontrollers;

import java.io.File;
import com.javafxserver.config.Config;
import com.javafxserver.exceptions.HandleExceptionMessage;
import com.javafxserver.service.ServerServiceHandler;
import com.javafxserver.utils.LogWriter;
import com.javafxserver.utils.UIUtils;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainViewController {	

    @FXML private TextField epassDriverFilePath;
    @FXML private Button selectFileButton;
    @FXML private Button startServerButton;
    @FXML private Button stopServerButton;
    @FXML private Button launchDemoButton;
    @FXML private Button settingsButton;
    @FXML private TextArea consoleTextArea;
    
    private File selectedDriverFile;
    private HostServices hostServices;
    private Task<Void> serverStartTask;
    

    @FXML
    public void initialize() {
        String libraryPath = Config.getEpassValue("library");
        if (libraryPath != null && !libraryPath.trim().isEmpty()) {            
            selectedDriverFile = new File(libraryPath);
            epassDriverFilePath.setText(libraryPath);
        }
          
        // Open the settings dialog on click
        settingsButton.setOnAction(e -> {
            SettingDialogExample settingDialog = new SettingDialogExample();
            settingDialog.openSettingsDialog(); // Method we created in SettingsDialogExample
        });
    }

    @FXML
    public void chooseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Token Driver");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Driver DLL", "*.dll")
        );
        Stage stage = (Stage) selectFileButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null && file.exists()) {
        	selectedDriverFile = file;
            epassDriverFilePath.setText(file.getAbsolutePath());
            try{
            	Config.setEpassConfigValue("library",file.getAbsolutePath());
            }
            catch(Exception e) {
            	log(e.getMessage());
            }
        }
    }
    

    @FXML	
    public void startServer(ActionEvent eventAction) {
    	log("Trying to start server ...");
    	
    	if (selectedDriverFile == null || !selectedDriverFile.exists()) {
            UIUtils.showAlert("Driver Missing", "Please select the PKCS#11 DLL file.");
            log("Server could not be started due to missing PKCS#11 DLL file");
            return;
        }    	
    	
    	
    	int httpPort = Config.getHttpPort();
    	int httpsPort = Config.getHttpsPort();
    	
    	if(arePortsInUse(httpPort, httpsPort)) {
    		log("Server could not be started due to port(s) already in used.");
    		return;
    	}    	
    	
    	if(ServerServiceHandler.isServerRunning()) {
    		log("Server already tunning");
    		return;
    	}
    	
    	serverStartTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				if (isCancelled()) {
			        return null;
			    }
				ServerServiceHandler.start();	            
				return null;
			}
    		
    	};
    	
    	serverStartTask.setOnSucceeded(event -> {
    		log("Server running at http://localhost:"+httpPort+" and https://localhost:"+httpsPort);
            switchServerButtons(true);
        });
    	
    	serverStartTask.setOnFailed(event -> {
            Throwable e = serverStartTask.getException();
            //handleExceptionMessage(e);
            log("Error: "+ HandleExceptionMessage.getMessage("Failed to start server", e));
            e.printStackTrace();            
            stopServer(null);
            switchServerButtons(false);
            UIUtils.showErrorAlert("Error", e.getMessage());
        }); 
    	
    	new Thread(serverStartTask).start();
    }   
    

    @FXML
    public void stopServer(ActionEvent event) {
    	if (serverStartTask != null && serverStartTask.isRunning()) {
		    serverStartTask.cancel();
		}
    	try {
            ServerServiceHandler.stop();
            switchServerButtons(false);
            log("Server stopped.");
        } catch (Exception e) {
            UIUtils.showErrorAlert("Error", e.getMessage());
            log("An error has occurred: " + e.getMessage());
        }
    }
    
    @FXML
    public void openDemoInBrowser(ActionEvent event) {
    	int httpPort = Config.getHttpPort();
    	String url = "http://localhost:" + httpPort + "/demo";
    	if(hostServices != null) {
    		hostServices.showDocument(url);
    	}
    	else {
    		log("Host services are not initialized");
    	}
        
    }
    
    public void setHostServices(HostServices services) {
    	this.hostServices = services;
    }
    
    private void switchServerButtons(boolean flag) {
    	// If flag = false, then server is down, means, start button should be enabled while the stop button is to be disabled
    	// otherwise if flag = true, the server is up, meaning the start button is to be disabled while the stop button is to be enabled.    	
    	startServerButton.setDisable(flag);
        stopServerButton.setDisable(!flag);
        launchDemoButton.setDisable(!flag);    	
    }
    
    private boolean arePortsInUse(int httpPort, int httpsPort) {
    	boolean httpPortUsed = ServerServiceHandler.isPortInUse(httpPort);
    	boolean httpsPortUsed = ServerServiceHandler.isPortInUse(httpsPort);
    	if(httpPortUsed) {
    		String messageString = "Port "+httpPort+" already in use.";
    		UIUtils.showErrorAlert("Error", messageString);
    		log(messageString);
    		
    	}
    	
    	if(httpsPortUsed) {
    		String messageString = "Port "+httpsPort+" already in use.";
    		UIUtils.showErrorAlert("Error", messageString);
    		log(messageString);
    		
    	}
    	
    	return httpPortUsed||httpsPortUsed;
    }

    private void log(String message) {
        consoleTextArea.appendText(message + "\n");
        LogWriter.writeLog(message);
    }
}
