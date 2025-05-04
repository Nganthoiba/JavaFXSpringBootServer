package com.javafxserver.exceptions;

public class FileConversionException extends Exception{
	private static final long serialVersionUID = 1L;
	public FileConversionException(String message) {
		super(message);
	}

	public FileConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}
