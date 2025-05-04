package com.javafxserver.service;

import java.io.IOException;

public class ServerServiceHandler {
	public static ServerService serverService = new ServerService();
	
	public static void start() throws Exception {
		if(serverService.isServerRunning()) {    		
    		throw new Exception("Server already active and running...");
    	}
		serverService.start();		
	}
	
	public static void stop() throws Exception {
		if(serverService.isServerRunning())
    	{
			serverService.stop();
    	}
	}
	
	public static boolean isServerRunning() {
		return serverService.isServerRunning();
	}
	
	// Check if the port is in used/occupied already by other resources
 	public static boolean isPortInUse(int port) {
 		try (var socket = new java.net.ServerSocket(port)) {
			return false;
		} catch (IOException e) {
			return true;
		}
 	}
}
