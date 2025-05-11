package com.javafxserver.exceptions;

import java.io.IOException;

public class NoCertificateFoundException extends IOException {
	private static final long serialVersionUID = 1L;
	public NoCertificateFoundException(String message) {
		super(message);
	}

	public NoCertificateFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
