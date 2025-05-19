package com.javafxserver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Config {
    public static final String PKCS11 = "PKCS11";
    public static final String SUN_PKCS11 = "SunPKCS11";

    public static final String HOME_PATH = System.getProperty("user.home");
    public static final String APP_PATH = HOME_PATH + File.separator + "DigiSignServerApp";
    public static final String CONFIG_FILE_PATH = APP_PATH + File.separator + "config.json";

    public static final String STORAGE_PATH = APP_PATH + File.separator + "storage";
    public static final String SIGNED_PATH = STORAGE_PATH + File.separator + "signed";
    public static final String PUBLIC_PATH = System.getProperty("user.dir") + File.separator + "public";

    public static final int DEFAULT_HTTP_PORT = 8090;
    public static final int DEFAULT_HTTPS_PORT = 8443;
    public static String PIN = null; // This will be set when the user enters the correct PIN

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Object> load() {
        try {
            Path path = Paths.get(CONFIG_FILE_PATH);
            if (!Files.exists(path)) {
                createDefaultConfig();
            }
            String json = Files.readString(path);
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void saveConfig(Map<String, Object> config) {
        try {
            Files.createDirectories(Paths.get(APP_PATH));
            try (Writer writer = new FileWriter(CONFIG_FILE_PATH)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig() throws IOException {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("http_port", DEFAULT_HTTP_PORT);
        defaultConfig.put("https_port", DEFAULT_HTTPS_PORT);
        defaultConfig.put("cors_origins", new ArrayList<String>());
        defaultConfig.put("epass_config", new HashMap<String, Object>());
        saveConfig(defaultConfig);
    }

    public static int getHttpPort() {
        Map<String, Object> config = load();
        return ((Number) config.getOrDefault("http_port", DEFAULT_HTTP_PORT)).intValue();
    }

    public static int getHttpsPort() {
        Map<String, Object> config = load();
        return ((Number) config.getOrDefault("https_port", DEFAULT_HTTPS_PORT)).intValue();
    }

    public static List<String> getCorsOrigins() {
        Map<String, Object> config = load();
        Object obj = config.get("cors_origins");
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>();
    }

    public static void setHttpPort(int port) {
        Map<String, Object> config = load();
        config.put("http_port", port);
        saveConfig(config);
    }

    public static void setHttpsPort(int port) {
        Map<String, Object> config = load();
        config.put("https_port", port);
        saveConfig(config);
    }

    public static void setCorsOrigins(List<String> origins) {
        Map<String, Object> config = load();
        config.put("cors_origins", origins);
        saveConfig(config);
    }

    public static void addCorsOrigin(String origin) {
        List<String> origins = getCorsOrigins();
        if (!origins.contains(origin)) {
            origins.add(origin);
            setCorsOrigins(origins);
        }
    }

    public static void removeCorsOrigin(String origin) {
        List<String> origins = getCorsOrigins();
        if (origins.contains(origin)) {
            origins.remove(origin);
            setCorsOrigins(origins);
        }
    }

    public static Map<String, Object> getEpassConfig() {
        Map<String, Object> config = load();
        Object obj = config.get("epass_config");
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    public static String getEpassValue(String key) {
        Object val = getEpassConfig().get(key);
        return val != null ? val.toString() : null;
    }

    public static void setEpassConfigValue(String key, Object value) {
        Map<String, Object> config = load();
        Map<String, Object> epass = getEpassConfig();
        epass.put(key, value);
        config.put("epass_config", epass);
        saveConfig(config);
    }

    public static File createTemporaryPKCS11Config() throws IOException {
        Map<String, Object> epassConfig = getEpassConfig();

        File tempFile = File.createTempFile("epass_config", ".cfg");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("name = " + epassConfig.getOrDefault("name", "ePass2003") + "\n");
            writer.write("library = " + epassConfig.get("library") + "\n");
            writer.write("slotListIndex = " + epassConfig.getOrDefault("slotListIndex", 0) + "\n");
            writer.write("attributes = " + epassConfig.getOrDefault("attributes", "compatibility") + "\n");
        }
        return tempFile;
    }

    public static Object get(String key) {
        Map<String, Object> config = load();
        return config.get(key);
    }

    public static void set(String key, Object value) {
        Map<String, Object> config = load();
        config.put(key, value);
        saveConfig(config);
    }
} 
