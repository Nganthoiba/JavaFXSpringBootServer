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
    
	@GetMapping("/")	
	public String index(Model model) {
		//model.addAttribute("message", "Hello! this is from Spring Boot and Thymeleaf");
		//return "index";
		return "demo";
	}
	
	@GetMapping("/demo")
	public String demo() {		
		return "demo";
	}
	
	@GetMapping("/esignPDF")
	public String esignPDF() {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		return "esignPDF";
	}
	
	@GetMapping("/esignJSON")
	public String esignJSON() {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		return "esignJSON";
	}
	
	@GetMapping("/esignXML")
	public String esignXML() {
		// Logout token forcefully if there is already a session
		TokenManager.logoutToken();
		return "esignXML";
	}
}
