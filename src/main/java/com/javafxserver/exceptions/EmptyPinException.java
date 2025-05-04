package com.javafxserver.exceptions;

public class EmptyPinException extends Exception {
	private static final long serialVersionUID = 1L;
	public EmptyPinException(String message) {
		super(message);
	}

	public EmptyPinException(String message, Throwable cause) {
		super(message, cause);
	}
}
