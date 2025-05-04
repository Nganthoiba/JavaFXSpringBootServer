package com.javafxserver.exceptions;

public class EpassTokenDetectionException extends Exception{
	private static final long serialVersionUID = 1L;
	public EpassTokenDetectionException(String message) {
		super(message);
	}
	
	public EpassTokenDetectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
