import org.testng.TestNG;
import java.util.Collections;

public class TestLauncher {
    public static void main(String[] args) {
        // Valor por defecto
        String env = "qa";

        // Buscar argumento -env=xxx
        for (String arg : args) {
            if (arg.startsWith("-env=")) {
                env = arg.substring("-env=".length());
            }
        }

        // Guardar el entorno en una variable de sistema
        System.setProperty("env", env);

        // Lanzar TestNG con tu suite
        TestNG testng = new TestNG();
        testng.setTestSuites(Collections.singletonList("testng.xml"));
        testng.run();
    }
}
