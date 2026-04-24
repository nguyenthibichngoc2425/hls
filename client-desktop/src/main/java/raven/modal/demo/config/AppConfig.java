package raven.modal.demo.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();
    private static final String DEFAULT_API_BASE_URL = "http://172.22.64.1:8080/api";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream classpathStream = openFromClasspath()) {
            if (classpathStream != null) {
                props.load(classpathStream);
                return;
            }
        } catch (IOException ignored) {
            // Try filesystem fallback below.
        }

        try (InputStream fallbackStream = openFromFileSystem()) {
            if (fallbackStream != null) {
                props.load(fallbackStream);
                return;
            }
        } catch (IOException ignored) {
            // Use defaults below.
        }

        props.setProperty("app.api-base-url", DEFAULT_API_BASE_URL);
    }

    private static InputStream openFromClasspath() {
        ClassLoader classLoader = AppConfig.class.getClassLoader();
        InputStream stream = classLoader.getResourceAsStream("app.properties");
        if (stream != null) {
            return stream;
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader.getResourceAsStream("app.properties");
        }
        return null;
    }

    private static InputStream openFromFileSystem() throws IOException {
        Path[] candidates = new Path[] {
                Path.of("src", "main", "resources", "app.properties"),
                Path.of("client-desktop", "src", "main", "resources", "app.properties")
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.newInputStream(candidate);
            }
        }
        return null;
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String getAPIBaseUrl() {
        String value = get("app.api-base-url");
        if (value == null || value.isBlank()) {
            return DEFAULT_API_BASE_URL;
        }
        return value;
    }

}
