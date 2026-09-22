import org.testng.TestNG;

import java.awt.Desktop;
import java.io.File;
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

        openReport();
    }

    /**
     * Abre el informe de TestNG en el navegador por defecto al terminar la
     * suite, para que quien lanzó el jar lo vea sin tener que ir a buscarlo
     * a mano en test-output/. Si falla (sin entorno gráfico, p. ej. en un
     * pipeline CI) se ignora silenciosamente — no debe hacer fallar la suite.
     */
    private static void openReport() {
        try {
            File report = new File("test-output/emailable-report.html");
            if (report.exists() && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(report);
            }
        } catch (Exception e) {
            System.out.println("No se pudo abrir el informe automáticamente: " + e.getMessage());
        }
    }
}
