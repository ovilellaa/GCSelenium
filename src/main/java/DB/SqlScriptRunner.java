package DB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ejecuta scripts .sql (leídos como ficheros externos, no del classpath, para
 * poder editarlos sin recompilar) que dejan la BBDD de QA en el estado que
 * necesitan los tests, o que comprueban dicho estado.
 *
 * Convención de carpetas (bajo Configuration/sql/):
 *   - before-suite/  → todos los .sql se ejecutan, en orden alfabético, antes
 *                       de arrancar la suite (por si algo externo ha dejado
 *                       el paciente/dato de pruebas en mal estado).
 *   - after-suite/   → todos los .sql se ejecutan, en orden alfabético, al
 *                       terminar la suite, incluso si algún test ha fallado,
 *                       para devolver la BBDD a su estado inicial.
 *   - on-demand/     → scripts NO ejecutados automáticamente; se invocan por
 *                       nombre desde cualquier test con run()/query()/exists()
 *                       cuando haga falta a mitad de una suite.
 *
 * Parámetros: un script puede contener marcadores "@NOMBRE" (p.ej. "@NHC")
 * que se sustituyen por el valor correspondiente del Map de parámetros antes
 * de ejecutar, escapando las comillas simples del valor (' → ''). Si el
 * marcador va en una columna de texto, el propio script debe llevar las
 * comillas (p.ej. WHERE HISTORYNUMBER = '@NHC'), ver check_patient_exists.sql
 * como ejemplo; en una columna numérica se deja sin comillas y el escapado
 * no afecta, porque un valor numérico nunca contiene comillas.
 *
 * Si no se pasan parámetros (Map vacío, como en before-suite/after-suite), el
 * script se ejecuta tal cual, sin tocar ningún "@algo" — así un script
 * autocontenido con sus propias variables de T-SQL (DECLARE @customerid = ...)
 * funciona sin necesidad de adaptarlo al mecanismo de sustitución.
 */
public final class SqlScriptRunner {

    private static final String BASE_DIR = "Configuration/sql";
    private static final String BEFORE_SUITE_DIR = BASE_DIR + "/before-suite";
    private static final String AFTER_SUITE_DIR = BASE_DIR + "/after-suite";
    private static final String ON_DEMAND_DIR = BASE_DIR + "/on-demand";

    private static final Pattern PARAM_PATTERN = Pattern.compile("@(\\w+)");

    private SqlScriptRunner() {
    }

    /** Llamar desde @BeforeSuite. Ejecuta todos los .sql de before-suite/, en orden alfabético. */
    public static void runBeforeSuiteScripts() {
        runAllScriptsIn(BEFORE_SUITE_DIR);
    }

    /** Llamar desde @AfterSuite. Ejecuta todos los .sql de after-suite/, en orden alfabético. */
    public static void runAfterSuiteScripts() {
        runAllScriptsIn(AFTER_SUITE_DIR);
    }

    /** Ejecuta (INSERT/UPDATE/DELETE/...) un script de on-demand/ sin parámetros. */
    public static void run(String scriptName) {
        run(scriptName, Map.of());
    }

    /** Ejecuta (INSERT/UPDATE/DELETE/...) un script de on-demand/ sustituyendo sus marcadores @NOMBRE. */
    public static void run(String scriptName, Map<String, String> params) {
        execute(new File(ON_DEMAND_DIR, scriptName), params);
    }

    /**
     * Ejecuta un script de on-demand/ que se espera devuelva filas (SELECT) y
     * dice si ha devuelto al menos una. Útil para comprobaciones tipo
     * "¿existe el paciente de pruebas?" antes de decidir si hace falta resetear.
     */
    public static boolean exists(String scriptName, Map<String, String> params) {
        return !query(scriptName, params).isEmpty();
    }

    /** Ejecuta un script de on-demand/ que devuelve filas y las expone como lista de mapas columna→valor. */
    public static List<Map<String, Object>> query(String scriptName, Map<String, String> params) {
        return executeQuery(new File(ON_DEMAND_DIR, scriptName), params);
    }

    // ── Implementación ──────────────────────────────────────────────────────

    private static void runAllScriptsIn(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".sql"));
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            execute(file, Map.of());
        }
    }

    private static void execute(File file, Map<String, String> params) {
        String sql = readAndSubstitute(file, params);
        try (Connection con = DBUtils.getConnection()) {
            for (String batch : splitBatches(sql)) {
                if (batch.isBlank()) {
                    continue;
                }
                try (Statement stmt = con.createStatement()) {
                    stmt.execute(batch);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error ejecutando script SQL: " + file.getPath(), e);
        }
    }

    private static List<Map<String, Object>> executeQuery(File file, Map<String, String> params) {
        String sql = readAndSubstitute(file, params);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = DBUtils.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error ejecutando consulta SQL: " + file.getPath(), e);
        }
        return rows;
    }

    private static String readAndSubstitute(File file, Map<String, String> params) {
        if (!file.exists()) {
            throw new RuntimeException("No se encontró el script SQL: " + file.getPath());
        }

        String sql;
        try {
            sql = Files.readString(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo el script SQL: " + file.getPath(), e);
        }

        if (params.isEmpty()) {
            // Nada que sustituir: el script puede ser autocontenido (con sus
            // propios DECLARE @variable de T-SQL) y no debe tocarse.
            return sql;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        int lastEnd = 0;
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (!params.containsKey(paramName)) {
                // No es necesariamente un marcador nuestro: puede ser una variable
                // nativa de T-SQL (DECLARE @algo) del propio script. Se deja tal cual.
                continue;
            }
            result.append(sql, lastEnd, matcher.start());
            // Se escapan las comillas simples por si el marcador va dentro de un
            // literal de texto en el script (p.ej. '@NHC'); no afecta a valores
            // numéricos, que nunca contienen comillas.
            result.append(params.get(paramName).replace("'", "''"));
            lastEnd = matcher.end();
        }
        result.append(sql, lastEnd, sql.length());
        return result.toString();
    }

    /**
     * Divide el contenido de un script en lotes separados por líneas "GO"
     * (convención de scripts de SQL Server / SSMS), ya que el driver JDBC no
     * entiende GO como sentencia SQL y fallaría si se enviara tal cual.
     */
    private static List<String> splitBatches(String sql) {
        List<String> batches = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\\R")) {
            if (line.trim().equalsIgnoreCase("GO")) {
                batches.add(current.toString());
                current.setLength(0);
            } else {
                current.append(line).append('\n');
            }
        }
        batches.add(current.toString());
        return batches;
    }
}
