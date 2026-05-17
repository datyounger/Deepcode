package com.deepcode.util;

import java.io.*;
import java.util.Properties;

/**
 * Application configuration manager.
 * Stores settings like API keys, crawl intervals, etc.
 */
public class Config {
    private static Config instance;
    private Properties properties;
    private static final String CONFIG_FILE = "deepcode.properties";

    private Config() {
        properties = new Properties();
        loadConfig();
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading config: " + e.getMessage());
            }
        } else {
            // Set defaults
            setDefaults();
            saveConfig();
        }
    }

    private void setDefaults() {
        properties.setProperty("gemini.api.key", "");
        properties.setProperty("crawl.interval.hours", "24");
        properties.setProperty("crawl.max.submissions", "50");
        properties.setProperty("db.path", "deepcode.db");
        properties.setProperty("gemini.model", "gemini-2.0-flash");
        properties.setProperty("gemini.api.url", "https://generativelanguage.googleapis.com/v1beta/models/");
    }

    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "DeepCode Configuration");
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    public String get(String key) {
        return properties.getProperty(key, "");
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }

    public String getGeminiApiKey() {
        return get("gemini.api.key");
    }

    public void setGeminiApiKey(String apiKey) {
        set("gemini.api.key", apiKey);
    }

    public int getCrawlIntervalHours() {
        return getInt("crawl.interval.hours", 24);
    }

    public int getMaxSubmissions() {
        return getInt("crawl.max.submissions", 50);
    }

    public String getDbPath() {
        return get("db.path", "deepcode.db");
    }

    public String getGeminiModel() {
        return get("gemini.model", "gemini-2.0-flash");
    }

    public String getGeminiApiUrl() {
        return get("gemini.api.url", "https://generativelanguage.googleapis.com/v1beta/models/");
    }
}
