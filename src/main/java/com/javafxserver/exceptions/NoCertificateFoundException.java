package com.javafxserver.exceptions;

public class NoCertificateFoundException extends Exception {
	private static final long serialVersionUID = 1L;
	public NoCertificateFoundException(String message) {
		super(message);
	}

	public NoCertificateFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
