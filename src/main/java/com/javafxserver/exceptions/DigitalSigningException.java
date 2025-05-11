package com.javafxserver.exceptions;

import java.io.IOException;

public class DigitalSigningException extends IOException {
	private static final long serialVersionUID = 1L;
	public DigitalSigningException(String message) {
		super("DigitalSigningException: " + message);
	}

	public DigitalSigningException(String message, Throwable cause) {
		super("DigitalSigningException: " + message, cause);
	}
}
