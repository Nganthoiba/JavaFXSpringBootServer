package com.javafxserver.exceptions;

public class SunPKCS11NotFoundException extends Exception {
	private static final long serialVersionUID = 1L;
	public SunPKCS11NotFoundException(String message) {
		super(message);
	}

	public SunPKCS11NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
