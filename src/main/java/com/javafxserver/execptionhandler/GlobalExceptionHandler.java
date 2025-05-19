package com.javafxserver.execptionhandler;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.javafxserver.utils.LogWriter;

@ControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        //ex.printStackTrace(); // Logs to console
        LogWriter.writeLog("Error: " + ex.getMessage()); // Logs to file
        System.out.println("Error: " + ex.getMessage());
        return "error"; // Show custom error.html
    }
}
