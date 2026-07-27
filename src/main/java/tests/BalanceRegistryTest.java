package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * Test: Registro de Balances
 *
 * ── Credenciales ──────────────────────────────────────────────────────────────
 *   username_nurse / password_nurse  (via ClassBaseTest.LoginAsNurse)
 *
 * ── Paciente ──────────────────────────────────────────────────────────────────
 *   pacqah1NH  (De La Torre Albuquerque, Juan)
 *
 * ── Flujo ─────────────────────────────────────────────────────────────────────
 *   1.  Login enfermera
 *   2.  Hospitalización → worklist → filtrar y seleccionar paciente
 *   3.  Acciones → Ver historia → nueva pestaña
 *   4.  Registro de balances
 *   5.  Nueva entrada  → Ingesta oral  → Volumen 250 ml  → Aceptar
 *   6.  Nueva salida   → Diuresis      → Volumen 150 ml  → Aceptar
 *   7.  Modificar entrada: abrir celda → cambiar volumen a 300 → Aceptar
 *   8.  Modificar salida:  abrir celda → cambiar volumen a 200 → Aceptar
 *   9.  Eliminar entrada:  abrir celda → Opciones → Eliminar → Confirmar
 *   10. Eliminar salida:   abrir celda → Opciones → Eliminar → Confirmar
 *   11. Nueva entrada  → Ingesta oral  → Volumen 250 ml  → Aceptar
 *   12. Nueva salida   → Diuresis      → Volumen 150 ml  → Aceptar
 *
 * ── IDs relevantes ────────────────────────────────────────────────────────────
 *   Sidebar              → registry_of_balances-sidebar
 *   Acciones             → actions-button  (via openActionsMenuAndClick)
 *   Nueva entrada        → new_entry   (abre submenú)
 *   Nueva salida         → new_output  (abre submenú)
 *   Ingesta oral         → id="0"  (en submenú de entrada)
 *   Diuresis             → id="0"  (en submenú de salida)
 *   Formulario:
 *     Volumen            → volume
 *     Aceptar            → accept-BalanceEntryDialogComponent-button
 *     Cancelar           → cancel-BalanceEntryDialogComponent-button
 *   Formulario modificar:
 *     Opciones           → menu-option-button
 *     Eliminar registro  → delete_registry
 *   Confirmación:
 *     Confirmar          → alert-confirm
 *   Aviso duplicado      → continue-button  (puede aparecer al crear entrada/salida)
 *   Celda con dato:
 *     Las celdas con registros contienen un span.tooltip-value.
 *     La tabla usa clase cdk-column-{hora} donde {hora} es la hora del registro
 *     en formato 24h (0-23) — permite localizar exactamente la celda creada.
 */
public class BalanceRegistryTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // Hora en la que se crean los registros en este test (se captura antes de crearlos).
    // La tabla usa cdk-column-{hora} como clase CSS en las celdas, donde {hora} es la
    // hora del registro en formato 24h (0-23). Capturar antes de crear garantiza que
    // aunque el test tarde y cambie de hora, siempre se abre la celda correcta.
    private String recordHour = null;

    // =========================================================================
    // TC1 — LOGIN
    // =========================================================================
    @Test(priority = 1)
    public void TC1_Login() {
        // LoginAsNurse incluye GotoToUrl() y la gestión de la pantalla de
        // selección de centro (si el usuario tiene más de uno)
        LoginAsNurse();

        // Esperar a que la pantalla principal esté lista antes de continuar.
        // Login() no incluye esta espera para no acoplar el ClassBaseTest a
        // un módulo concreto, así que la hacemos aquí.
        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("hospitalization-sidebar")));

        Reporter.log("TC1 OK — Login enfermera completado.");
    }

    // =========================================================================
    // TC2 — HOSPITALIZACIÓN → LISTA DE TRABAJO
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = {"TC1_Login"})
    public void TC2_EnterHospitalizationWorklist() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("filter-input")));

        Reporter.log("TC2 OK — Lista de hospitalización cargada.");
    }

    // =========================================================================
    // TC3 — FILTRAR Y SELECCIONAR PACIENTE
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = {"TC2_EnterHospitalizationWorklist"})
    public void TC3_FilterAndSelectPatient() {
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

        Reporter.log("TC3 OK — Paciente seleccionado.");
    }

    // =========================================================================
    // TC4 — ABRIR HISTORIA → REGISTRO DE BALANCES
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = {"TC3_FilterAndSelectPatient"})
    public void TC4_OpenBalanceRegistry() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Acciones → Ver historia (via ClassBaseTest para evitar overlay)
        openActionsMenuAndClick(localWait, "see_history");

        // Cambiar a la nueva pestaña
        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        // Modales opcionales al abrir la historia (via ClassBaseTest)
        handleReadOnlyAlert();
        handleVitalAlertsDialog();

        // Navegar a Registro de balances
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("registry_of_balances-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));

        Reporter.log("TC4 OK — Registro de balances abierto.");
    }

    // =========================================================================
    // TC5 — NUEVA ENTRADA (Ingesta oral, 250 ml)
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"TC4_OpenBalanceRegistry"})
    public void TC5_NewEntry() {
        // Capturar la hora ANTES de crear para poder localizar la celda después
        recordHour = String.valueOf(LocalTime.now().getHour());
        addEntry("0", "250");
        Reporter.log("TC5 OK — Nueva entrada (Ingesta oral 250 ml) registrada. Hora: " + recordHour);
    }

    // =========================================================================
    // TC6 — NUEVA SALIDA (Diuresis, 150 ml)
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = {"TC5_NewEntry"})
    public void TC6_NewOutput() {
        addOutput("0", "150");
        Reporter.log("TC6 OK — Nueva salida (Diuresis 150 ml) registrada.");
    }

    // =========================================================================
    // TC7 — MODIFICAR ENTRADA (cambiar volumen a 300 ml)
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = {"TC6_NewOutput"})
    public void TC7_ModifyEntry() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openRecordCell(localWait, "Ingesta oral");

        WebElement volume = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("volume")));
        volume.clear();
        volume.sendKeys("300");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-BalanceEntryDialogComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));

        Reporter.log("TC7 OK — Entrada modificada a 300 ml.");
    }

    // =========================================================================
    // TC8 — MODIFICAR SALIDA (cambiar volumen a 200 ml)
    // =========================================================================
    @Test(priority = 8, dependsOnMethods = {"TC7_ModifyEntry"})
    public void TC8_ModifyOutput() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openRecordCell(localWait, "Diuresis");

        WebElement volume = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("volume")));
        volume.clear();
        volume.sendKeys("200");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-BalanceEntryDialogComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));

        Reporter.log("TC8 OK — Salida modificada a 200 ml.");
    }

    // =========================================================================
    // TC9 — ELIMINAR ENTRADA
    // =========================================================================
    @Test(priority = 9, dependsOnMethods = {"TC8_ModifyOutput"})
    public void TC9_DeleteEntry() {
        deleteRecord(new WebDriverWait(driver, TIMEOUT), "Ingesta oral");
        Reporter.log("TC9 OK — Entrada eliminada.");
    }

    // =========================================================================
    // TC10 — ELIMINAR SALIDA
    // =========================================================================
    @Test(priority = 10, dependsOnMethods = {"TC9_DeleteEntry"})
    public void TC10_DeleteOutput() {
        deleteRecord(new WebDriverWait(driver, TIMEOUT), "Diuresis");
        Reporter.log("TC10 OK — Salida eliminada.");
    }

    // =========================================================================
    // TC11 — NUEVA ENTRADA (segunda vez)
    // =========================================================================
    @Test(priority = 11, dependsOnMethods = {"TC10_DeleteOutput"})
    public void TC11_NewEntryAgain() {
        // Actualizar la hora por si ha cambiado desde TC5
        recordHour = String.valueOf(LocalTime.now().getHour());
        addEntry("0", "250");
        Reporter.log("TC11 OK — Nueva entrada (Ingesta oral 250 ml) registrada de nuevo. Hora: " + recordHour);
    }

    // =========================================================================
    // TC12 — NUEVA SALIDA (segunda vez)
    // =========================================================================
    @Test(priority = 12, dependsOnMethods = {"TC11_NewEntryAgain"})
    public void TC12_NewOutputAgain() {
        addOutput("0", "150");
        Reporter.log("TC12 OK — Nueva salida (Diuresis 150 ml) registrada de nuevo.");
    }


    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Crea una nueva entrada de balance.
     * @param typeId  ID del subtipo en el submenú ("0" = Ingesta oral)
     * @param volume  Volumen en ml
     */
    private void addEntry(String typeId, String volume) {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_entry");
        localWait.until(ExpectedConditions.elementToBeClickable(By.id(typeId))).click();

        // Modal opcional "Ya se ha registrado una entrada de este tipo en esta hora"
        handleValidationAlert();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));

        WebElement vol = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("volume")));
        vol.clear();
        vol.sendKeys(volume);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-BalanceEntryDialogComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));
    }

    /**
     * Crea una nueva salida de balance.
     * @param typeId  ID del subtipo en el submenú ("0" = Diuresis)
     * @param volume  Volumen en ml
     */
    private void addOutput(String typeId, String volume) {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_output");
        localWait.until(ExpectedConditions.elementToBeClickable(By.id(typeId))).click();

        // Modal opcional "Ya se ha registrado una salida de este tipo en esta hora"
        handleValidationAlert();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));

        WebElement vol = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("volume")));
        vol.clear();
        vol.sendKeys(volume);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-BalanceEntryDialogComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));
    }

    /**
     * Abre el diálogo "Modificar volumen" clicando la celda del registro creado
     * en esta ejecución.
     *
     * La tabla usa clase CSS "cdk-column-{hora}" en cada celda, donde {hora}
     * corresponde a la hora del registro en formato 24h (0-23). Se localiza la
     * fila por su label y dentro de ella la celda cuya clase incluye
     * "cdk-column-{recordHour}" — así se abre exactamente el registro de esta
     * ejecución, sin importar cuántos registros previos haya a otras horas.
     *
     * @param localWait  WebDriverWait activo
     * @param rowLabel   Texto del label de fila ("Ingesta oral", "Diuresis")
     */
    private void openRecordCell(WebDriverWait localWait, String rowLabel) {
        final String hour = recordHour;

        // Esperar a que la celda de la hora correcta tenga datos (tooltip-value)
        localWait.until(d ->
                (Boolean) ((JavascriptExecutor) d).executeScript(
                        "var rows = document.querySelectorAll('tr');" +
                                "for (var r of rows) {" +
                                "    var first = r.querySelector('td');" +
                                "    if (first && first.textContent.includes('" + rowLabel + "')) {" +
                                "        var cell = r.querySelector('td.cdk-column-" + hour + "');" +
                                "        if (cell && cell.querySelector('.tooltip-value')) return true;" +
                                "    }" +
                                "}" +
                                "return false;"));

        // Clicar la celda
        ((JavascriptExecutor) driver).executeScript(
                "var rows = document.querySelectorAll('tr');" +
                        "for (var r of rows) {" +
                        "    var first = r.querySelector('td');" +
                        "    if (first && first.textContent.includes('" + rowLabel + "')) {" +
                        "        var cell = r.querySelector('td.cdk-column-" + hour + "');" +
                        "        if (cell) { cell.click(); return; }" +
                        "    }" +
                        "}");

        // Esperar a que abra el diálogo
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-BalanceEntryDialogComponent-button")));
    }

    /**
     * Elimina un registro: abre su celda → Opciones → Eliminar → Confirmar.
     *
     * @param localWait  WebDriverWait activo
     * @param rowLabel   Texto del label de fila ("Ingesta oral", "Diuresis")
     */
    private void deleteRecord(WebDriverWait localWait, String rowLabel) {
        openRecordCell(localWait, rowLabel);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("menu-option-button"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("delete_registry"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));
    }
}
