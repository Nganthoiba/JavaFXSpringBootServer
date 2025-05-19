package com.javafxserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javafxserver.exceptions.EpassTokenDetectionException;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import javax.smartcardio.*;

public class USBTokenDetector {
    static class TokenEntry {
        public String name;
        public String vid_pid;
        public List<String> match;
        public String windows;
        public String linux;
    }

    static class TokenConfig {
        public List<TokenEntry> tokens;
    }

    private static final String CONFIG_FILE = "token-drivers.json";

    public static String detectTokenLibrary() throws IOException, URISyntaxException, EpassTokenDetectionException {
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("Detected OS: " + os);

        List<String> tokenOutput;

        if (os.contains("win")) {
            tokenOutput = getWindowsSmartCardReaderInfo();
        } else if (os.contains("nux") || os.contains("nix")) {
            tokenOutput = getLinuxUsbList();
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        List<TokenEntry> tokens = loadConfig();
        System.out.println("No. of USB tokens detected: " + tokenOutput.size());

        for (String line : tokenOutput) {
            System.out.println("Detected USB token: " + line);
            for (TokenEntry token : tokens) {
                for (String keyword : token.match) {
                    if (line.toLowerCase().contains(keyword.toLowerCase())) {
                        System.out.println("Matched with: " + token.name);
                        return os.contains("win") ? token.windows : token.linux;
                    }
                }
            }
        }

        throw new EpassTokenDetectionException("No known USB token detected.");
    }

    private static List<TokenEntry> loadConfig() throws IOException {
        InputStream inputStream = USBTokenDetector.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (inputStream == null) {
            throw new FileNotFoundException(CONFIG_FILE + " not found in classpath.");
        }

        ObjectMapper mapper = new ObjectMapper();
        TokenConfig config = mapper.readValue(inputStream, TokenConfig.class);
        return config.tokens;
    }

    private static List<String> getWindowsSmartCardReaderInfo() throws IOException {        
    	
    	ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                "Get-CimInstance -ClassName Win32_PnPEntity | Where-Object { $_.PNPClass -eq 'SmartCardReader' } | Select-Object -ExpandProperty Name");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<String> output = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) output.add(line.trim());
        }

        return output;
            	
    }

    private static List<String> getLinuxUsbList() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", "lsusb");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        List<String> output = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) output.add(line.trim());
        }

        return output;
    }
    
    public static void detectUSBTokens() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();

            System.out.println("Available Smart Card Readers:");
            for (CardTerminal terminal : terminals) {
                System.out.println("Reader: " + terminal.getName());
                if (terminal.isCardPresent()) {
                    Card card = terminal.connect("*");
                    ATR atr = card.getATR();
                    System.out.println("Token inserted. ATR: " + bytesToHex(atr.getBytes()));
                    card.disconnect(false);
                } else {
                    System.out.println("No card inserted.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
