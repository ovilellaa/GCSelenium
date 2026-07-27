package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente
 *                      → Ver historia (nueva pestaña) → Antecedentes
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → Ver historia
 *   3.  Antecedentes
 *   4.  Otros antecedentes de interés: Añadir → Editar → Eliminar
 *   5.  Enfermedades crónicas: Añadir → Editar → Eliminar
 *   6.  Hábitos tóxicos: seleccionar Tabaco, Alcohol y Drogas → eliminar con ×
 *   7.  Medicación crónica (pestaña): Añadir → Editar → Eliminar
 *
 * ── IDs Otros antecedentes de interés ──────────────────────────────────────
 *   background-actions-button
 *   add-background / edit-background / delete-background
 *   div.angular-editor-textarea  (contenteditable, sin ID propio)
 *   cancel/accept-WYSIWYGEditorDialogComponent-button
 *
 * ── IDs Enfermedades crónicas ───────────────────────────────────────────────
 *   actions-button-diseases_and_risk_factors
 *   add-chronic-disease / edit-chronic-disease / delete-chronic-disease
 *   illness  (mat-select) → illness-0..7  | isActive → isActive-0 (Sí) / isActive-1 (No)
 *   observations (textarea — nativeSetter)
 *   cancel/accept-ChronicDiseaseComponent-button
 *   Grid: grid-diseases_and_risk_factors-{N}-disease
 *
 *   ⚠ IMPORTANTE — selectOption del padre usa JS click, lo que NO cierra el panel
 *   del mat-select en Angular Material; se usa selectOptionNative (click nativo)
 *   para todos los selects de esta pantalla.
 *   ⚠ illness está DISABLED al editar — solo se cambian observations.
 *   ⚠ Puede haber filas preexistentes → se usa el último índice dinámico.
 *
 * ── IDs Hábitos tóxicos ─────────────────────────────────────────────────────
 *   tobacco / alcohol / drug  (mat-select)
 *   tobacco-N / alcohol-N / drug-N  (mat-option)
 *   show-detail-tobacco-button-suffix / show-detail-alcohol-button-suffix
 *     / show-detail-drugs-button-suffix  (mat-icon fg-times → JS click)
 *
 * ── IDs Medicación crónica ──────────────────────────────────────────────────
 *   mat-tab-label-0-1
 *   actions-button-chronic_medication
 *   add-chronic-medication / edit-chronic-medication / delete-chronic-medication
 *   startingDate / indication (textarea → nativeSetter) / reason / observations
 *   cancel/accept-ChronicMedicationDialogContainer-button
 *   Grid: grid-chronic_medication-{N}-indication
 */
public class AntecedentsTest extends ClassBaseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // ── Enfermedades crónicas ─────────────────────────────────────────────
    // illness-2 = Cancer (usamos índice que no exista ya para el paciente)
    private static final String DISEASE_ILLNESS_ID   = "illness-2";
    private static final String DISEASE_IS_ACTIVE_ID = "isActive-0"; // Sí
    private static final String DISEASE_OBSERVATIONS = "Observaciones de prueba";
    private static final String DISEASE_OBS_EDIT     = "Observaciones editadas por test";

    // ── Hábitos tóxicos ───────────────────────────────────────────────────
    private static final String TOBACCO_OPTION = "tobacco-1"; // Habitual
    private static final String ALCOHOL_OPTION = "alcohol-3"; // Ligero
    private static final String DRUG_OPTION    = "drug-2";    // No Adicto

    // ── Medicación crónica ────────────────────────────────────────────────
    private static final String CHRONIC_MED_TEXT      = "Enalapril 10mg/24h vía oral";
    private static final String CHRONIC_MED_TEXT_EDIT = "Enalapril 20mg/24h vía oral (editado)";

    // Textos antecedentes
    private static final String BACKGROUND_TEXT_CREATE = "Antecedente de prueba creado en test automático";
    private static final String BACKGROUND_TEXT_EDIT   = "Antecedente editado por test automático";

    // =========================================================================
    // 1. LOGIN
    // =========================================================================
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);
        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_doctor"));
        driver.findElement(By.id("password")).sendKeys(ConfigReader.get("password_doctor"));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        try {
            WebElement centerBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("defaultButtonId")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerBtn);
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
        Reporter.log("Lista de trabajo de Hospitalización cargada.");
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
                By.id("background-sidebar")));
        Reporter.log("Historial clínico abierto en nueva pestaña.");
    }

    // =========================================================================
    // 3. ANTECEDENTES
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterAntecedents() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("background-sidebar"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("background-actions-button")));
        Reporter.log("Sección Antecedentes cargada.");
    }

    // =========================================================================
    // 4A. OTROS ANTECEDENTES DE INTERÉS — Añadir
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = {"EnterAntecedents"})
    public void AddBackground() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClickCustom(localWait, "background-actions-button", "add-background");

        WebElement editor = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("mat-dialog-container div.angular-editor-textarea")));
        editor.click();
        editor.sendKeys(BACKGROUND_TEXT_CREATE);

        jsClick("accept-WYSIWYGEditorDialogComponent-button");
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-WYSIWYGEditorDialogComponent-button")));

        Reporter.log("Antecedente añadido: " + BACKGROUND_TEXT_CREATE);
    }

    // =========================================================================
    // 4B. OTROS ANTECEDENTES DE INTERÉS — Editar
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = {"AddBackground"})
    public void EditBackground() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClickCustom(localWait, "background-actions-button", "edit-background");

        WebElement editor = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("mat-dialog-container div.angular-editor-textarea")));
        editor.click();
        editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        editor.sendKeys(BACKGROUND_TEXT_EDIT);

        jsClick("accept-WYSIWYGEditorDialogComponent-button");
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-WYSIWYGEditorDialogComponent-button")));

        Reporter.log("Antecedente editado: " + BACKGROUND_TEXT_EDIT);
    }

    // =========================================================================
    // 4C. OTROS ANTECEDENTES DE INTERÉS — Eliminar
    // =========================================================================
    @Test(priority = 8, dependsOnMethods = {"EditBackground"})
    public void DeleteBackground() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClickCustom(localWait, "background-actions-button", "delete-background");
        handleDeleteConfirmation(localWait);
        Reporter.log("Antecedente de interés eliminado.");
    }

    // =========================================================================
    // 5A. ENFERMEDADES CRÓNICAS — Añadir
    // =========================================================================
    @Test(priority = 9, dependsOnMethods = {"DeleteBackground"})
    public void AddChronicDisease() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countDiseaseRows();

        openActionsMenuAndClickCustom(localWait, "actions-button-diseases_and_risk_factors",
                "add-chronic-disease");
        waitForChronicDiseaseModal(localWait);

        // ── Click nativo en mat-select: cierra el panel correctamente ────────
        // selectOption del padre usa JS click → el panel no se cierra → se usa
        // selectOptionNative que hace click nativo en la opción.
        selectOptionNative(localWait, "illness",   DISEASE_ILLNESS_ID);
        selectOptionNative(localWait, "isActive",  DISEASE_IS_ACTIVE_ID);

        // Observaciones: nativeSetter para Angular Reactive Forms
        fillTextareaNative("observations", DISEASE_OBSERVATIONS);

        acceptChronicDiseaseModal(localWait);

        // Esperar la nueva fila en el grid
        final int expectedRows = rowsBefore + 1;
        localWait.until(d -> countDiseaseRows() == expectedRows);
        Reporter.log("Enfermedad crónica añadida. Filas: " + expectedRows);
    }

    // =========================================================================
    // 5B. ENFERMEDADES CRÓNICAS — Editar (última fila = la recién añadida)
    // =========================================================================
    @Test(priority = 10, dependsOnMethods = {"AddChronicDisease"})
    public void EditChronicDisease() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int lastIdx = countDiseaseRows() - 1;

        // Click nativo en la fila (evita element click intercepted)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-diseases_and_risk_factors-" + lastIdx + "-disease"))).click();

        openActionsMenuAndClickCustom(localWait, "actions-button-diseases_and_risk_factors",
                "edit-chronic-disease");
        waitForChronicDiseaseModal(localWait);

        // illness está DISABLED al editar — solo se cambian observations
        fillTextareaNative("observations", DISEASE_OBS_EDIT);

        acceptChronicDiseaseModal(localWait);
        Reporter.log("Enfermedad crónica editada: " + DISEASE_OBS_EDIT);
    }

    // =========================================================================
    // 5C. ENFERMEDADES CRÓNICAS — Eliminar (última fila)
    // =========================================================================
    @Test(priority = 11, dependsOnMethods = {"EditChronicDisease"})
    public void DeleteChronicDisease() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int lastIdx    = countDiseaseRows() - 1;
        int rowsBefore = lastIdx + 1;

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-diseases_and_risk_factors-" + lastIdx + "-disease"))).click();

        openActionsMenuAndClickCustom(localWait, "actions-button-diseases_and_risk_factors",
                "delete-chronic-disease");
        handleDeleteConfirmation(localWait);

        final int expectedRows = rowsBefore - 1;
        localWait.until(d -> countDiseaseRows() == expectedRows);
        Reporter.log("Enfermedad crónica eliminada. Filas: " + expectedRows);
    }

    // =========================================================================
    // 6A. HÁBITOS TÓXICOS — Seleccionar opciones
    // =========================================================================
    @Test(priority = 12, dependsOnMethods = {"DeleteChronicDisease"})
    public void SelectToxicHabits() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        selectOptionNative(localWait, "tobacco", TOBACCO_OPTION);
        Reporter.log("Tabaco → Habitual");

        selectOptionNative(localWait, "alcohol", ALCOHOL_OPTION);
        Reporter.log("Alcohol → Ligero");

        selectOptionNative(localWait, "drug", DRUG_OPTION);
        Reporter.log("Drogas → No Adicto");

        // Confirmar que el select de tabaco tiene texto
        localWait.until(d ->
                !d.findElement(By.id("tobacco")).getText().trim().isEmpty());
        Reporter.log("Hábitos tóxicos seleccionados.");
    }

    // =========================================================================
    // 6B. HÁBITOS TÓXICOS — Eliminar con × (mat-icon fg-times)
    // =========================================================================
    @Test(priority = 13, dependsOnMethods = {"SelectToxicHabits"})
    public void DeleteToxicHabits() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        clickHabitDeleteIcon(localWait, "show-detail-tobacco-button-suffix");
        Reporter.log("Tabaco eliminado con ×");

        clickHabitDeleteIcon(localWait, "show-detail-alcohol-button-suffix");
        Reporter.log("Alcohol eliminado con ×");

        clickHabitDeleteIcon(localWait, "show-detail-drugs-button-suffix");
        Reporter.log("Drogas eliminadas con ×");

        // Confirmar que el select de tabaco queda vacío
        localWait.until(d ->
                d.findElement(By.id("tobacco")).getText().trim().isEmpty());
        Reporter.log("Todos los hábitos tóxicos eliminados.");
    }

    // =========================================================================
    // 7. MEDICACIÓN — Pestaña Medicación crónica
    // =========================================================================
    @Test(priority = 14, dependsOnMethods = {"DeleteToxicHabits"})
    public void SwitchToChronicMedicationTab() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("mat-tab-label-0-1"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button-chronic_medication")));
        Reporter.log("Pestaña Medicación crónica activa.");
    }

    // =========================================================================
    // 7A. MEDICACIÓN CRÓNICA — Añadir
    // =========================================================================
    @Test(priority = 15, dependsOnMethods = {"SwitchToChronicMedicationTab"})
    public void AddChronicMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countChronicMedRows();

        openActionsMenuAndClickCustom(localWait, "actions-button-chronic_medication",
                "add-chronic-medication");
        waitForChronicMedModal(localWait);

        fillTextareaNative("indication", CHRONIC_MED_TEXT);

        acceptChronicMedModal(localWait);

        final int expectedRows = rowsBefore + 1;
        localWait.until(d -> countChronicMedRows() == expectedRows);
        Reporter.log("Medicación crónica añadida: " + CHRONIC_MED_TEXT);
    }

    // =========================================================================
    // 7B. MEDICACIÓN CRÓNICA — Editar (última fila)
    // =========================================================================
    @Test(priority = 16, dependsOnMethods = {"AddChronicMedication"})
    public void EditChronicMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int lastIdx = countChronicMedRows() - 1;
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-chronic_medication-" + lastIdx + "-indication"))).click();

        openActionsMenuAndClickCustom(localWait, "actions-button-chronic_medication",
                "edit-chronic-medication");
        waitForChronicMedModal(localWait);

        fillTextareaNative("indication", CHRONIC_MED_TEXT_EDIT);

        acceptChronicMedModal(localWait);
        Reporter.log("Medicación crónica editada: " + CHRONIC_MED_TEXT_EDIT);
    }

    // =========================================================================
    // 7C. MEDICACIÓN CRÓNICA — Eliminar (última fila)
    // =========================================================================
    @Test(priority = 17, dependsOnMethods = {"EditChronicMedication"})
    public void DeleteChronicMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int lastIdx    = countChronicMedRows() - 1;
        int rowsBefore = lastIdx + 1;

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-chronic_medication-" + lastIdx + "-indication"))).click();

        openActionsMenuAndClickCustom(localWait, "actions-button-chronic_medication",
                "delete-chronic-medication");
        handleDeleteConfirmation(localWait);

        final int expectedRows = rowsBefore - 1;
        localWait.until(d -> countChronicMedRows() == expectedRows);
        Reporter.log("Medicación crónica eliminada. Filas: " + expectedRows);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Abre un menú de acciones con ID de botón personalizado y hace click nativo en el ítem.
     * Complementa openActionsMenuAndClick del padre (que siempre usa "actions-button").
     *
     * Se usa click nativo (no JS) en el ítem porque algunos ítems son mat-menu-item con
     * submenú (aria-haspopup="menu", clase mat-menu-item-submenu-trigger): el JS click
     * abre el submenú sin cerrarlo, dejando el overlay activo y bloqueando clicks
     * posteriores con "element click intercepted". El click nativo de Selenium navega
     * directamente al destino (modal o acción) y cierra el menú correctamente.
     */
    private void openActionsMenuAndClickCustom(WebDriverWait w, String buttonId, String itemId) {
        WebElement btn = w.until(ExpectedConditions.elementToBeClickable(By.id(buttonId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        // Click nativo — cierra el overlay del menú (incluyendo submenús trigger)
        w.until(ExpectedConditions.elementToBeClickable(By.id(itemId))).click();
    }

    /**
     * Selecciona una opción de un mat-select usando CLICK NATIVO en la opción.
     *
     * El método selectOption del padre usa JS click en la opción, lo que NO cierra
     * el panel del mat-select en Angular Material: el cdk-overlay-backdrop transparente
     * permanece activo y bloquea clicks posteriores con "element click intercepted".
     * El click nativo de Selenium sí cierra el panel correctamente.
     */
    private void selectOptionNative(WebDriverWait w, String selectId, String optionId) {
        w.until(ExpectedConditions.elementToBeClickable(By.id(selectId))).click();

        // presenceOfElementLocated: la opción puede no ser "visible" para Selenium
        // cuando hay un backdrop de modal encima, pero sí es clickable con click nativo.
        WebElement option = w.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        option.click(); // click nativo — cierra el panel y confirma la selección

        // Esperar a que el panel del select se cierre
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".mat-select-panel")));
    }

    /** JS click por ID. */
    private void jsClick(String id) {
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('" + id + "').click();");
    }

    /**
     * Rellena un textarea Angular usando nativeSetter + eventos input/change.
     * sendKeys no siempre sincroniza el valor con el FormControl Angular Reactive.
     */
    private void fillTextareaNative(String fieldId, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('" + fieldId + "');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLTextAreaElement.prototype, 'value').set;" +
                        "setter.call(el, '" + value.replace("'", "\\'") + "');" +
                        "el.dispatchEvent(new Event('input',  {bubbles: true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles: true}));");
    }

    /**
     * Espera y acepta el diálogo de confirmación de eliminación.
     * Prueba primero continue-button y luego accept-ConfirmDialogComponent-button.
     */
    private void handleDeleteConfirmation(WebDriverWait w) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")));
            jsClick("continue-button");
            return;
        } catch (TimeoutException ignored) {}

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.id("accept-ConfirmDialogComponent-button")));
            jsClick("accept-ConfirmDialogComponent-button");
        } catch (TimeoutException ignored) {}
    }

    private void waitForChronicDiseaseModal(WebDriverWait w) {
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-ChronicDiseaseComponent-button")));
    }

    /**
     * Acepta el modal de enfermedad crónica y espera a que el overlay desaparezca
     * completamente antes de continuar (evita "element click intercepted" en la fila).
     */
    private void acceptChronicDiseaseModal(WebDriverWait w) {
        jsClick("accept-ChronicDiseaseComponent-button");
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-ChronicDiseaseComponent-button")));
        // Esperar a que el dialog desaparezca
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("mat-dialog-container")));
        // El menú de Acciones queda abierto tras cerrar el modal (el submenu trigger
        // add-chronic-disease lo deja activo). Cerrar cualquier mat-menu-panel visible.
        closeOpenMenuIfPresent();
    }

    private void waitForChronicMedModal(WebDriverWait w) {
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-ChronicMedicationDialogContainer-button")));
    }

    private void acceptChronicMedModal(WebDriverWait w) {
        jsClick("accept-ChronicMedicationDialogContainer-button");
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-ChronicMedicationDialogContainer-button")));
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("mat-dialog-container")));
    }

    /** Cuenta filas en el grid de enfermedades crónicas. */
    private int countDiseaseRows() {
        return driver.findElements(
                By.cssSelector(
                        "[id^='grid-diseases_and_risk_factors-'][id$='-disease']")).size();
    }

    /** Cuenta filas en el grid de medicación crónica. */
    private int countChronicMedRows() {
        return driver.findElements(
                By.cssSelector("[id^='grid-chronic_medication-'][id$='-indication']")).size();
    }

    /**
     * Hace click en el mat-icon × de un hábito tóxico (id: show-detail-{habit}-button-suffix).
     * El mat-icon tiene clase "fg-times reset" y se activa con JS click.
     */
    private void clickHabitDeleteIcon(WebDriverWait w, String iconId) {
        WebElement icon = w.until(ExpectedConditions.elementToBeClickable(By.id(iconId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", icon);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }
    /**
     * Cierra cualquier mat-menu panel que haya quedado abierto haciendo click en
     * el backdrop transparente del overlay. Es necesario después de acciones que
     * abren un modal desde un submenu trigger (add-chronic-disease), donde el menú
     * padre permanece activo tras cerrarse el modal.
     */
    private void closeOpenMenuIfPresent() {
        try {
            List<WebElement> backdrops = new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(d -> {
                        List<WebElement> b = d.findElements(
                                By.cssSelector(".cdk-overlay-backdrop"));
                        return b.isEmpty() ? null : b;
                    });
            if (backdrops != null) {
                // Click en el backdrop para cerrar el menú
                ((JavascriptExecutor) driver).executeScript(
                        "var b = document.querySelector('.cdk-overlay-backdrop');" +
                                "if (b) b.click();");
                // Esperar a que el menú desaparezca
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.invisibilityOfElementLocated(
                                By.cssSelector(".mat-menu-panel")));
            }
        } catch (TimeoutException ignored) {
            // No había menú abierto
        }
    }

}
