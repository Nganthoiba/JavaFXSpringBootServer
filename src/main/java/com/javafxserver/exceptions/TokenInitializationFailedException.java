package com.javafxserver.exceptions;

public class TokenInitializationFailedException extends Exception{
	private static final long serialVersionUID = 1L;
	public TokenInitializationFailedException(String message) {
		super(message);
	}

	public TokenInitializationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
