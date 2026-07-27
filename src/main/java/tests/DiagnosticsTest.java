package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Test de integración: Diagnósticos
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → Ver historia
 *   3.  Navegar a Diagnósticos
 *   4.  Añadir Dx1 como Principal
 *   5.  Añadir Dx2 como Principal → modal conflicto → Eliminar → continue-button
 *   6.  Añadir Dx3 como Principal → modal conflicto → Secundario → continue-button
 *   7.  Seleccionar el secundario → set_as_main → modal conflicto → Eliminar → continue-button
 *   8.  Eliminar el último diagnóstico principal
 *
 * ── IDs Menú Acciones ───────────────────────────────────────────────────────
 *   actions-button
 *   new_diagnosis / delete_diagnosis / set_as_main
 *
 * ── IDs Modal "Nuevo diagnóstico" ───────────────────────────────────────────
 *   show-catalog-button  (hay 2; el visible es índice 1)
 *   isMain-input         (input del mat-slide-toggle "Principal")
 *   cancel/accept-DiagnosisDialogComponent-button
 *
 * ── IDs Catálogo de diagnósticos ────────────────────────────────────────────
 *   input-default-id                       → campo de búsqueda
 *   tree-checkbox-initial-{ID}-{nivel}     → checkbox árbol izquierdo
 *   angle-right-button                     → mover al árbol derecho
 *   tree-checkbox-final-{ID}-{nivel}       → checkbox árbol derecho (⚠ hay que marcarlo)
 *   cancel/accept-DiagnosesCatalogContainer-button
 *
 *   ⚠ FLUJO CATÁLOGO:
 *     1. sendKeys en input-default-id
 *     2. click tree-checkbox-initial-{ID}
 *     3. click angle-right-button
 *     4. ESPERAR presenceOfElementLocated(tree-checkbox-final-{ID}) — tarda en aparecer
 *     5. click tree-checkbox-final-{ID}  ← marcar el nodo derecho
 *     6. click accept-DiagnosesCatalogContainer-button
 *     7. Tab en el campo diagnosis para cerrar el autocomplete
 *
 * ── IDs Modal conflicto "Diagnóstico principal" ─────────────────────────────
 *   alert-Eliminar / alert-Secundario
 *   ⚠ Después de cualquiera de los dos siempre aparece un modal "Información"
 *     con continue-button que hay que cerrar.
 *
 * ── IDs Modal eliminación diagnóstico ───────────────────────────────────────
 *   continue-button   (aviso informativo previo, si aparece)
 *   alert-confirm / alert-cancel
 *
 * ── IDs Grid diagnósticos ───────────────────────────────────────────────────
 *   grid-gridId-{N}-diagnosis   → columna Diagnóstico (click para seleccionar fila)
 *   grid-gridId-{N}-is-main     → columna Principal ("Sí" / "No")
 *
 * ── Diagnósticos del catálogo ───────────────────────────────────────────────
 *   DX1 búsqueda "Anemia"   inicial D02340-4  final D02340-2  (3A03.0 Aciduria orótica)
 *   DX2 búsqueda "Diabetes" inicial D02902-4  final D02902-2  (5A22.1 Acidosis láctica)
 *   DX3 búsqueda "Anemia"   inicial D00061-4  final D00061-2  (1A36.10 Absceso amebiano)
 */
public class DiagnosticsTest extends ClassBaseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final String DX1_SEARCH       = "Anemia";
    private static final String DX1_INITIAL_NODE = "tree-checkbox-initial-D02340-4";
    private static final String DX1_FINAL_NODE   = "tree-checkbox-final-D02340-2";

    private static final String DX2_SEARCH       = "Diabetes";
    private static final String DX2_INITIAL_NODE = "tree-checkbox-initial-D02902-4";
    private static final String DX2_FINAL_NODE   = "tree-checkbox-final-D02902-2";

    private static final String DX3_SEARCH       = "Anemia";
    private static final String DX3_INITIAL_NODE = "tree-checkbox-initial-D00061-4";
    private static final String DX3_FINAL_NODE   = "tree-checkbox-final-D00061-2";

    // =========================================================================
    // 1. LOGIN
    // =========================================================================
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);
        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_doctor"));
        driver.findElement(By.id("password"))
                .sendKeys(ConfigReader.get("password_doctor"));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("defaultButtonId")));
            jsClick("defaultButtonId");
        } catch (TimeoutException ignored) {}

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));
        Reporter.log("Login completado como médico.");
    }

    // =========================================================================
    // 2. HOSPITALIZACIÓN → LISTA DE TRABAJO
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void EnterHospitalizationWL() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("filter-input")));
        Reporter.log("Lista de trabajo cargada.");
    }

    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(ConfigReader.get("pacqah1NH"));

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("load-all-information")))
                    .click();
        } catch (TimeoutException ignored) {}

        WebElement patientCell = localWait.until(d -> {
            List<WebElement> cells = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-patient']"));
            return cells.isEmpty() ? null : cells.get(0);
        });
        patientCell.click();
        Reporter.log("Paciente seleccionado.");
    }

    @Test(priority = 4, dependsOnMethods = {"FilterAndSelectPatient"})
    public void OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "see_history");
        SelectAccessReason("Guardia");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("diagnostics-sidebar")));
        Reporter.log("Historial clínico abierto.");
    }

    // =========================================================================
    // 3. NAVEGAR A DIAGNÓSTICOS
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterDiagnostics() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Cerrar mat-drawer-backdrop si quedó abierto al cargar la historia
        closeBackdropsIfPresent();

        // diagnostics-sidebar es un <span> dentro de un li — JS click
        jsClick("diagnostics-sidebar");

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button")));
        Reporter.log("Sección Diagnósticos cargada.");
    }

    // =========================================================================
    // 4. AÑADIR DX1 COMO PRINCIPAL
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = {"EnterDiagnostics"})
    public void AddDx1AsPrincipal() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countDiagnosisRows();

        openDiagnosisForm(localWait);
        selectFromCatalog(localWait, DX1_SEARCH, DX1_INITIAL_NODE, DX1_FINAL_NODE);
        activateIsMain();
        acceptDiagnosisForm(localWait);

        // No hay modal de conflicto al añadir el primero
        final int expected = rowsBefore + 1;
        localWait.until(d -> countDiagnosisRows() == expected);
        Reporter.log("Dx1 añadido como Principal. Filas: " + expected);
    }

    // =========================================================================
    // 5. AÑADIR DX2 COMO PRINCIPAL
    //    → modal conflicto → Eliminar → continue-button (informativo)
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = {"AddDx1AsPrincipal"})
    public void AddDx2AsPrincipalThenDeletePrevious() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openDiagnosisForm(localWait);
        selectFromCatalog(localWait, DX2_SEARCH, DX2_INITIAL_NODE, DX2_FINAL_NODE);
        activateIsMain();
        acceptDiagnosisForm(localWait);

        // Modal conflicto → Eliminar el anterior
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-Eliminar"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-Eliminar")));

        // Modal informativo "Información" con continue-button — siempre aparece
        dismissInfoAlert(localWait);

        Reporter.log("Dx2 añadido como Principal. Anterior eliminado.");
    }

    // =========================================================================
    // 6. AÑADIR DX3 COMO PRINCIPAL
    //    → modal conflicto → Secundario → continue-button (informativo)
    // =========================================================================
    @Test(priority = 8, dependsOnMethods = {"AddDx2AsPrincipalThenDeletePrevious"})
    public void AddDx3AsPrincipalThenSetPreviousSecondary() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countDiagnosisRows();

        openDiagnosisForm(localWait);
        selectFromCatalog(localWait, DX3_SEARCH, DX3_INITIAL_NODE, DX3_FINAL_NODE);
        activateIsMain();
        acceptDiagnosisForm(localWait);

        // Modal conflicto → Secundario (el anterior queda como secundario)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-Secundario"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-Secundario")));

        // Modal informativo — siempre aparece después
        dismissInfoAlert(localWait);

        final int expected = rowsBefore + 1;
        localWait.until(d -> countDiagnosisRows() == expected);
        Reporter.log("Dx3 Principal. Dx2 quedó Secundario. Filas: " + expected);
    }

    // =========================================================================
    // 7. SECUNDARIO → ESTABLECER COMO PRINCIPAL
    //    → modal conflicto → Eliminar → continue-button (informativo)
    // =========================================================================
    @Test(priority = 9, dependsOnMethods = {"AddDx3AsPrincipalThenSetPreviousSecondary"})
    public void SetSecondaryAsPrincipalThenDeleteCurrent() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila con is-main = "No"
        selectRowByIsMain(localWait, false);

        // Acciones → Establecer como principal
        openActionsMenuAndClick(localWait, "set_as_main");

        // Modal conflicto → Eliminar el principal actual
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-Eliminar"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-Eliminar")));

        // Modal informativo — siempre aparece después
        dismissInfoAlert(localWait);

        // Verificar que solo queda 1 fila y es principal
        localWait.until(d -> {
            List<WebElement> rows = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-is-main']"));
            return rows.size() == 1 && "Sí".equals(rows.get(0).getText().trim());
        });
        Reporter.log("Secundario establecido como Principal. Principal anterior eliminado.");
    }

    // =========================================================================
    // 8. ELIMINAR EL ÚLTIMO DIAGNÓSTICO PRINCIPAL
    // =========================================================================
    @Test(priority = 10, dependsOnMethods = {"SetSecondaryAsPrincipalThenDeleteCurrent"})
    public void DeleteLastDiagnosis() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countDiagnosisRows();

        // Seleccionar la única fila
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-diagnosis"))).click();

        // Acciones → Eliminar diagnóstico
        openActionsMenuAndClick(localWait, "delete_diagnosis");

        // Aviso informativo previo (si aparece)
        dismissInfoAlert(localWait);

        // Confirmación
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));

        final int expected = rowsBefore - 1;
        localWait.until(d -> countDiagnosisRows() == expected);
        Reporter.log("Último diagnóstico eliminado. Restantes: " + expected);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private void jsClick(String id) {
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('" + id + "').click();");
    }

    /**
     * Cierra cualquier mat-drawer-backdrop o cdk-overlay-backdrop que pueda
     * estar bloqueando interacciones. Se llama antes de navegaciones importantes.
     */
    private void closeBackdropsIfPresent() {
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.mat-drawer-backdrop, .cdk-overlay-backdrop')" +
                        ".forEach(b => b.click());");
    }

    private void openDiagnosisForm(WebDriverWait w) {
        openActionsMenuAndClick(w, "new_diagnosis");
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-DiagnosisDialogComponent-button")));
    }

    /**
     * Selecciona un diagnóstico del catálogo.
     *
     * Flujo:
     *   1. Abre catálogo (show-catalog-button índice 1)
     *   2. Busca en input-default-id
     *   3. Click en checkbox árbol izquierdo (initialNodeId)
     *   4. Click en angle-right-button
     *   5. ESPERA que aparezca el nodo en el árbol derecho (finalNodeId) — puede tardar
     *   6. Click en checkbox árbol derecho (finalNodeId) para marcarlo
     *   7. Acepta el catálogo
     *   8. Tab sobre el input diagnosis para cerrar el autocomplete
     */
    private void selectFromCatalog(WebDriverWait w, String searchTerm,
                                   String initialNodeId, String finalNodeId) {
        // [1] Abrir catálogo — hay 2 botones con id show-catalog-button;
        // el del formulario visible es el índice 1
        w.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            if (btns.size() < 2) return null;
            WebElement btn = btns.get(1);
            return btn.isDisplayed() ? btn : null;
        }).click();

        // [2] Buscar
        WebElement searchInput = w.until(ExpectedConditions.elementToBeClickable(
                By.id("input-default-id")));
        searchInput.clear();
        searchInput.sendKeys(searchTerm);

        // [3] Seleccionar nodo en árbol izquierdo
        w.until(ExpectedConditions.elementToBeClickable(By.id(initialNodeId))).click();

        // [4] Mover al árbol derecho
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("angle-right-button"))).click();

        // [5] Esperar a que el nodo aparezca en el árbol derecho — tarda un tick
        w.until(ExpectedConditions.presenceOfElementLocated(By.id(finalNodeId)));

        // [6] Marcar el nodo en el árbol derecho (viene desmarcado por defecto)
        w.until(ExpectedConditions.elementToBeClickable(By.id(finalNodeId))).click();

        // [7] Aceptar el catálogo
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-DiagnosesCatalogContainer-button"))).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-DiagnosesCatalogContainer-button")));

        // [8] Cerrar el autocomplete con Tab para que Angular lo acepte
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('diagnosis').dispatchEvent(" +
                        "new KeyboardEvent('keydown', {bubbles:true, key:'Tab', keyCode:9}));");

        w.until(d -> {
            List<WebElement> panels = d.findElements(
                    By.cssSelector(".mat-autocomplete-panel"));
            return panels.isEmpty() || !panels.get(0).isDisplayed();
        });
    }

    private void activateIsMain() {
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('isMain-input');" +
                        "if (!el.checked) el.click();");
    }

    private void acceptDiagnosisForm(WebDriverWait w) {
        jsClick("accept-DiagnosisDialogComponent-button");
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-DiagnosisDialogComponent-button")));
    }

    /**
     * Selecciona la fila del grid cuya columna is-main coincide con isPrincipal.
     */
    private void selectRowByIsMain(WebDriverWait w, boolean isPrincipal) {
        String expected = isPrincipal ? "Sí" : "No";
        WebElement cell = w.until(d -> {
            List<WebElement> cells = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-is-main']"));
            return cells.stream()
                    .filter(c -> expected.equals(c.getText().trim()))
                    .findFirst()
                    .orElse(null);
        });
        String idx = cell.getAttribute("id")
                .replace("grid-gridId-", "").replace("-is-main", "");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-" + idx + "-diagnosis"))).click();
    }

    /**
     * Cierra el modal informativo "Información" (continue-button).
     * Aparece siempre después de alert-Eliminar, alert-Secundario y set_as_main.
     * También puede aparecer antes de alert-confirm en delete_diagnosis.
     */
    private void dismissInfoAlert(WebDriverWait w) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")));
            jsClick("continue-button");
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.id("continue-button")));
        } catch (TimeoutException ignored) {}
    }

    private int countDiagnosisRows() {
        return driver.findElements(
                By.cssSelector("[id^='grid-gridId-'][id$='-diagnosis']")).size();
    }
}
