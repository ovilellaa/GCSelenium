package tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    public static void load(String env) {
        String filePath = "src/test/resources/config." + env + ".properties";
        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar el fichero: " + filePath, e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}

