package com.javafxserver.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.javafxserver.digitalsigner.TokenManager;

@Controller
public class PagesController {
	@GetMapping("/testResponseBody")
	@ResponseBody
	public String test() {
		return "This is a test for string in response body";
	}
    
	@GetMapping("/index")	
	public String index(Model model) {
		model.addAttribute("message", "Hello! this is from Spring Boot and Thymeleaf");
		return "index";
	}
	
	@GetMapping("/")
	public String demo(Model model) {		
		model.addAttribute("title", "Demo");
		model.addAttribute("heading", "Demostration Page For Digital Signer");
		return "demo";
	}
	
	@GetMapping("/esignPDF")
	public String esignPDF(Model model) {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		
		model.addAttribute("title", "Esign PDF");
        //model.addAttribute("content", "esignPDF"); // Dynamically selects the child template
        
        //return "baseLayout"; // Parent template
		return "esignPDF";
	}
	
	@GetMapping("/esignJSON")
	public String esignJSON() {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		return "esignJSON";
	}
	
	@GetMapping("/esignJSON-JWS")
	public String esignJSONJWS() {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		return "esignJSON-JWS";
	}
	
	@GetMapping("/esignXML")
	public String esignXML() {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		return "esignXML";
	}
}
