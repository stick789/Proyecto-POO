package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties props = new Properties();

    private static InputStream findConfigStream() {
        InputStream input = ConfigLoader.class.getResourceAsStream("/config.properties");
        if (input == null) {
            input = ConfigLoader.class.getResourceAsStream("/properties/config.properties");
        }
        if (input == null) {
            try {
                input = ConfigLoader.class.getModule().getResourceAsStream("config.properties");
            } catch (IOException ignored) {
                input = null;
            }
        }
        if (input == null) {
            try {
                input = ConfigLoader.class.getModule().getResourceAsStream("properties/config.properties");
            } catch (IOException ignored) {
                input = null;
            }
        }
        return input;
    }
    
    static {
        try (InputStream input = findConfigStream()) {
            if (input == null) {
                throw new RuntimeException("No se encontró config.properties (ni properties/config.properties) en el classpath");
            }
            props.load(input);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error cargando config.properties", e);
        }
    }
    
    public static String get(String key) {
        return props.getProperty(key);
    }
}