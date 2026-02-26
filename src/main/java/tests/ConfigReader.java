package tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    public static void load(String env) {

        // Ruta externa (junto al JAR)
        String externalPath = "configuration/config." + env + ".properties";
        File externalFile = new File(externalPath);

        if (!externalFile.exists()) {
            throw new RuntimeException("No se encontró ningún fichero de configuración para entorno: " + env);
        }

        try (InputStream input = new FileInputStream(externalFile)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el fichero de configuración para entorno: " + env, e);
        }
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static String get(String key, String defaultvalue) {
        String value = properties.getProperty(key);
        if ( value == null)  value = defaultvalue;

        return value;
    }
}

