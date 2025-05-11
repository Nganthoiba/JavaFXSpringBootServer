package com.javafxserver.exceptions;

import java.io.IOException;

public class NoCertificateChainException extends IOException {
	private static final long serialVersionUID = 1L;
	public NoCertificateChainException(String message) {
		super(message);
	}

	public NoCertificateChainException(String message, Throwable cause) {
		super(message, cause);
	}
}
