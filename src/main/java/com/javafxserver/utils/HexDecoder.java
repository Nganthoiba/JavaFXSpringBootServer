package com.javafxserver.utils;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.util.encoders.Hex;


public class HexDecoder {
	public static String decodeHexToString(String hex) {
		byte[] decodedBytes = Hex.decode(hex);
		return new String(decodedBytes, StandardCharsets.UTF_8).trim();
 	}
}
