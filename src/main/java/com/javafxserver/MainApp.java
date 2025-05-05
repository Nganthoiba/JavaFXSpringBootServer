package com.javafxserver;

import com.javafxserver.fxcontrollers.MainViewController;
import com.javafxserver.service.ServerServiceHandler;
import com.javafxserver.utils.LogWriter;
import com.javafxserver.utils.UIUtils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application{
		
	@Override
    public void init() {
		//Config.createAppPath();
    }
	
	private void loadCss(Scene scene) {
		String css = getClass().getResource("/styles/application.css").toExternalForm();
		scene.getStylesheets().add(css);
	}
	

    @Override
    public void start(Stage primaryStage) {
    	try {
	        // Initialize your JavaFX UI here	        
	        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/javafxserver/fxml/MainView.fxml"));
	        Parent root = loader.load();	
	        Scene scene = new Scene(root);
	        this.loadCss(scene);
	        
	        // Get the controller instance
	        MainViewController controller = loader.getController();

	        // Pass HostServices to the controller
	        controller.setHostServices(getHostServices());
	        
	        primaryStage.setScene(scene);
	        primaryStage.setTitle("Digital Signature Mini Server Application");
	        primaryStage.show();
	        System.out.println("DigiSignServer started...");
    	}
    	catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
    		UIUtils.showErrorAlert("Error", "An error has occured. "+e.getMessage());
		}
    }

    @Override
    public void stop() throws Exception{
    	super.stop();        
    	if(ServerServiceHandler.isServerRunning())
    	{
    		ServerServiceHandler.stop();
    		LogWriter.writeLog("Server stopped");
    	}
        System.out.println("Application is closed");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
