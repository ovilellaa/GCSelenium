package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Test de integración: Validación Farmacia
 *
 * Flujo:
 *   TC1 — Login médico → Farmacia → Validación → seleccionar primer paciente
 *          → esperar carga de medicamentos → seleccionar primer medicamento
 *   TC2 — Marcar como válido → verificar status=1 en grid
 *   TC3 — Marcar como no válido → rellenar motivo + observaciones → confirmar
 *          → verificar status=2 en grid
 *   TC4 — Modificar medicamento (habrá cambiado de posición, buscarlo por status=2)
 *          → cambiar dosis → aceptar → verificar cambio
 *
 * ── Ordenación del grid ───────────────────────────────────────────────────────
 *   La validación farmacia ordena: pendientes primero (status=0), luego
 *   válidos (status=1) y finalmente no válidos (status=2). Tras cada cambio
 *   de estado el medicamento se desplaza en el grid, por lo que se busca
 *   siempre por status en lugar de por posición fija.
 *
 * ── IDs navegación ────────────────────────────────────────────────────────────
 *   pharmacy-sidebar        → módulo Farmacia (menú principal)
 *   validation-sidebar      → submenú Validación
 *
 * ── IDs lista de pacientes ────────────────────────────────────────────────────
 *   cdk-drop-list-0         → contenedor mat-list de pacientes (sin IDs en items)
 *   Selector primer item    → #cdk-drop-list-0 mat-list-item:first-child
 *
 * ── IDs grid de medicamentos ──────────────────────────────────────────────────
 *   gridId-{N}                           → fila N
 *   grid-gridId-{N}-drug                 → celda medicamento
 *   grid-gridId-{N}-validation-status    → estado (0=pend, 1=válido, 2=no válido)
 *   actions-button                       → menú Acciones
 *
 * ── IDs menú Acciones ─────────────────────────────────────────────────────────
 *   mark-as-valid           → Marcar como válido
 *   mark-as-not-valid       → Marcar como no válido
 *   modify-prescription     → Modificar medicamento
 *
 * ── IDs diálogo "Marcar como no válido" ──────────────────────────────────────
 *   reason                  → dropdown Motivo; opciones reason-0..reason-6
 *     reason-0  Compra especial
 *     reason-1  Error en prescripcion
 *     reason-2  Falta autorizacion por costo
 *     reason-3  Otros
 *     reason-4  Px alergico
 *     reason-5  Px en procedimiento quirurgico
 *     reason-6  Px en transfucion sanguinea
 *   comment                                               → textarea Observaciones
 *   Marcar como no válido-MarkAsInvalidDialogComponent-button → confirmar
 *   Cancelar-MarkAsInvalidDialogComponent-button          → cancelar
 *
 * ── IDs formulario Modificar (PrescriptionContainer) ─────────────────────────
 *   visibleQuantity                              → campo cantidad
 *   accept_dialog-PrescriptionContainer-button  → Aceptar
 *   cancel_dialog-PrescriptionContainer-button  → Cancelar
 */
public class PharmacyValidationTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // Valores para no-validar y modificar
    private static final String INVALID_REASON_ID  = "reason-3";   // "Otros"
    private static final String INVALID_COMMENT    = "Justificante de prueba automatizada Selenium";
    private static final String NEW_QUANTITY       = "3";

    // Status codes del grid
    private static final String STATUS_PENDING  = "0";
    private static final String STATUS_VALID    = "1";
    private static final String STATUS_INVALID  = "2";

    // =========================================================================
    // TC1 — LOGIN + NAVEGAR A VALIDACIÓN + SELECCIONAR PRIMER PACIENTE
    // =========================================================================
    @Test(priority = 1)
    public void TC1_LoginAndSelectFirstPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        LoginAsDoctor();

        // ── Farmacia → Validación ─────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("pharmacy-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("validation-sidebar"))).click();

        // ── Esperar que cargue la lista de pacientes ──────────────────────────
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("cdk-drop-list-0")));

        // ── Seleccionar el primer paciente de la lista ────────────────────────
        // Los mat-list-item no tienen IDs propios, se accede por posición CSS
        WebElement firstPatient = localWait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#cdk-drop-list-0 mat-list-item:first-child")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstPatient);

        Reporter.log("TC1 — Primer paciente seleccionado.");

        // ── Esperar que carguen los medicamentos en el panel derecho ──────────
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.tagName("gc-prescriptions")));
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("gridId-0")));

        Reporter.log("TC1 OK — Medicamentos cargados. Primer medicamento: " +
                driver.findElement(By.id("grid-gridId-0-drug")).getText());
    }

    // =========================================================================
    // TC2 — MARCAR PRIMER MEDICAMENTO COMO VÁLIDO
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = "TC1_LoginAndSelectFirstPatient")
    public void TC2_MarkFirstMedicationAsValid() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la primera fila pendiente (status=0)
        String rowId = findRowByStatus(localWait, STATUS_PENDING);
        Assert.assertNotNull(rowId, "No hay ningún medicamento pendiente en el grid.");

        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('grid-" + rowId + "-drug').click();");

        // Acciones → Marcar como válido
        openActionsMenuAndClick(localWait, "mark-as-valid");

        // Esperar que el grid se reordene (el medicamento pasa a status=1)
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                findRowByStatus(d, STATUS_VALID) != null);

        String validRowId = findRowByStatus(localWait, STATUS_VALID);
        Assert.assertNotNull(validRowId, "Ningún medicamento tiene status válido tras marcar.");

        Reporter.log("TC2 OK — Medicamento marcado como válido. Fila: " + validRowId);
    }

    // =========================================================================
    // TC3 — MARCAR EL MEDICAMENTO VÁLIDO COMO NO VÁLIDO
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = "TC2_MarkFirstMedicationAsValid")
    public void TC3_MarkMedicationAsInvalid() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Buscar la fila con status=1 (válido) y seleccionarla
        String validRowId = findRowByStatus(localWait, STATUS_VALID);
        Assert.assertNotNull(validRowId, "No hay medicamento válido para marcar como no válido.");

        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('grid-" + validRowId + "-drug').click();");

        // Acciones → Marcar como no válido
        openActionsMenuAndClick(localWait, "mark-as-not-valid");

        // ── Diálogo "Marcar como no válido" ───────────────────────────────────
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("reason")));

        // Seleccionar motivo: "Otros" (reason-3)
        WebElement reasonField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("reason")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reasonField);

        WebElement reasonOption = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(INVALID_REASON_ID)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reasonOption);

        // Rellenar observaciones
        WebElement commentField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("comment")));
        commentField.sendKeys(INVALID_COMMENT);

        // Confirmar
        WebElement confirmBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Marcar como no válido-MarkAsInvalidDialogComponent-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);

        // Esperar cierre del diálogo
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        // Verificar que el medicamento pasó a status=2
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                findRowByStatus(d, STATUS_INVALID) != null);

        String invalidRowId = findRowByStatus(localWait, STATUS_INVALID);
        Assert.assertNotNull(invalidRowId, "Ningún medicamento tiene status no válido tras marcar.");

        Reporter.log("TC3 OK — Medicamento marcado como no válido. Fila: " + invalidRowId +
                " | Motivo: Otros | Observaciones: " + INVALID_COMMENT);
    }

    // =========================================================================
    // TC4 — MODIFICAR EL MEDICAMENTO NO VÁLIDO (cambiar cantidad/dosis)
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = "TC3_MarkMedicationAsInvalid")
    public void TC4_ModifyInvalidMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Buscar la fila con status=2 (no válido) — habrá cambiado de posición
        String invalidRowId = findRowByStatus(localWait, STATUS_INVALID);
        Assert.assertNotNull(invalidRowId, "No hay medicamento no válido para modificar.");

        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('grid-" + invalidRowId + "-drug').click();");

        // Acciones → Modificar medicamento
        openActionsMenuAndClick(localWait, "modify-prescription");

        // ── Formulario "Actualizar medicamento del tratamiento" ───────────────
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("visibleQuantity")));

        // Cambiar la cantidad con nativeSetter (Angular no detecta .value = x)
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('visibleQuantity');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(el, '" + NEW_QUANTITY + "');" +
                        "el.dispatchEvent(new Event('input',  {bubbles: true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles: true}));");

        // Aceptar (JS click — evita overlay intercepted)
        WebElement acceptBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept_dialog-PrescriptionContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        // Esperar cierre del diálogo
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        // Verificar que la dosis modificada aparece en el grid.
        // La celda grid-{rowId}-dose muestra "N TABLETA"; la celda
        // grid-{rowId}-amount muestra solo el número.
        // El medicamento puede quedarse en la misma posición o reordenarse,
        // por eso verificamos buscando en TODAS las celdas -amount del grid.
        final String expectedDose = NEW_QUANTITY + " TABLETA";
        new WebDriverWait(driver, LONG_TIMEOUT).until(d -> {
            List<WebElement> doseCells = d.findElements(By.cssSelector("td[id$='-dose']"));
            for (WebElement cell : doseCells) {
                try {
                    if (expectedDose.equalsIgnoreCase(cell.getText().trim())) return true;
                } catch (StaleElementReferenceException ignored) {}
            }
            return false;
        });

        Reporter.log("TC4 OK — Medicamento modificado. Dosis esperada: " + expectedDose + ".");
    }

    // =========================================================================
    // HELPER — Busca la primera fila del grid con un status concreto
    // =========================================================================
    /**
     * Recorre todas las filas [id^="gridId-"] y devuelve el id de la primera
     * cuya celda [id$="-validation-status"] coincida con el status buscado.
     * Devuelve null si no encuentra ninguna.
     *
     * Códigos de status:
     *   "0" → Pendiente  (amarillo)
     *   "1" → Válido     (verde)
     *   "2" → No válido  (rojo)
     */
    private String findRowByStatus(WebDriver d, String targetStatus) {
        List<WebElement> rows = d.findElements(By.cssSelector("[id^='gridId-']"));
        for (WebElement row : rows) {
            try {
                String rowId  = row.getAttribute("id");
                String cellId = "grid-" + rowId + "-validation-status";
                List<WebElement> cells = d.findElements(By.id(cellId));
                if (!cells.isEmpty() && targetStatus.equals(cells.get(0).getText().trim())) {
                    return rowId;
                }
            } catch (StaleElementReferenceException ignored) {}
        }
        return null;
    }

    // Sobrecarga que acepta WebDriverWait (usa el driver interno)
    private String findRowByStatus(WebDriverWait wait, String targetStatus) {
        return findRowByStatus(driver, targetStatus);
    }
}
