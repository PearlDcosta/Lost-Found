package com.lostfound.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads resources/config.properties from the classpath exactly once
 * (static initializer) and hands out values by key. This is the ONLY
 * place database/cloud credentials are read from in the whole
 * application — nothing is ever hardcoded in Java source, satisfying
 * the "no hardcoded credentials" requirement.
 *
 * To run the app, the 'resources' folder must be on the classpath,
 * e.g.:
 *   java -cp bin:resources:lib/mysql-connector-j-8.x.x.jar com.lostfound.Main
 * (use ';' instead of ':' as the classpath separator on Windows)
 */
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
        // static utility class, no instances
    }

    /**
     * Returns the value for {@code key}, or throws if it is missing/blank.
     * Use this for required settings like DB credentials — failing fast
     * at startup is preferable to a confusing NullPointerException later.
     */
    public static String get(String key) {
        String value = valueFor(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required config key: '" + key + "' in " + CONFIG_FILE);
        }
        return value.trim();
    }

    /** Returns the value for {@code key}, or {@code defaultValue} if absent. */
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

    /** Environment variables override file values so cloud credentials stay out of source control. */
    private static String valueFor(String key) {
        String environmentKey = "LOSTFOUND_" + key.toUpperCase().replace('.', '_');
        String environmentValue = System.getenv(environmentKey);
        return environmentValue == null ? PROPERTIES.getProperty(key) : environmentValue;
    }
}
