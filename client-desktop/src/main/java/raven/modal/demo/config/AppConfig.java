package raven.modal.demo.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("app.properties")) {

            if (in == null) {
                throw new RuntimeException("Cannot find app.properties in classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load app.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String getAPIBaseUrl() {
        return get("app.api-base-url");
    }

}
