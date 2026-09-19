package com.lostfound.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;













public class ConfigLoader {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Could not find '" + CONFIG_FILE + "' on the classpath. " +
                        "Make sure the 'resources' folder is included when you run the app " +
                        "(see the classpath example in ConfigLoader's Javadoc).");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read '" + CONFIG_FILE + "': " + e.getMessage(), e);
        }
    }

    private ConfigLoader() {
        
    }

    




    public static String get(String key) {
        String value = valueFor(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required config key: '" + key + "' in " + CONFIG_FILE);
        }
        return value.trim();
    }

    
    public static String get(String key, String defaultValue) {
        String value = valueFor(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    public static int getInt(String key, int defaultValue) {
        String value = valueFor(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    
    private static String valueFor(String key) {
        String environmentKey = "LOSTFOUND_" + key.toUpperCase().replace('.', '_');
        String environmentValue = System.getenv(environmentKey);
        return environmentValue == null ? PROPERTIES.getProperty(key) : environmentValue;
    }
}
