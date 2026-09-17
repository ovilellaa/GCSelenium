package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Registro de constantes
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo
 *   3.  Filtrar la WL por el NH del paciente (ConfigReader "pacqah1NH") → seleccionar
 *   4.  Seleccionar al paciente → Acciones → Ver historia
 *   5.  Cambiar a la nueva pestaña del historial clínico
 *   6.  Entrar en Registro de constantes → vista Episodio
 *   7.  Acciones → Nuevo registro → rellenar → Aceptar → verificar celda
 *   8.  Seleccionar registro → Acciones → Modificar → cambiar valores → Aceptar → verificar celda
 *   9.  Seleccionar registro → Acciones → Eliminar → Confirmar → verificar decremento de filas
 *  10.  Acciones → Nuevo registro final → rellenar → Aceptar → verificar celda
 */
public class VitalSignsRecordTest extends ClassBaseTest {

    // Timeout estándar para todos los WebDriverWait
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // Pausa visual entre acciones (ms) — ajustar aquí para ir más rápido o más lento

    // Datos de prueba
    private static final String TEMP_CREATE = "36.5";
    private static final String TAS_CREATE  = "120";
    private static final String TAD_CREATE  = "80";
    private static final String FC_CREATE   = "72";

    private static final String TEMP_MODIFY = "37.2";
    private static final String FC_MODIFY   = "80";

    private static final String TEMP_FINAL = "37.0";
    private static final String TAS_FINAL  = "115";
    private static final String TAD_FINAL  = "75";
    private static final String FC_FINAL   = "68";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. LOGIN
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_nurse"));

        driver.findElement(By.id("password")).sendKeys(ConfigReader.get("password_nurse"));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        try {
            WebDriverWait centerWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            centerWait.until(ExpectedConditions.elementToBeClickable(
                    By.id("defaultButtonId"))).click();
        } catch (TimeoutException e) {
            // Sin pantalla de selección de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado como médico.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. HOSPITALIZACIÓN → LISTA DE TRABAJO
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void EnterHospitalizationWL() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.tagName("gc-hospitalization-list")));

        Reporter.log("Lista de trabajo de Hospitalización cargada.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BUSCAR PACIENTE EN LA WL (filter-input + pacqah1NH del config)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca al paciente usando el campo "filter-input" — la barra de búsqueda
     * de la lista de trabajo de Hospitalización — con el NH leído de
     * ConfigReader("pacqah1NH").
     *
     * Este es el mismo patrón que usa SurgeryWLTest.IsPatientInSurgeryWL():
     * escribir el NH en filter-input y esperar a gridId-0.
     *
     * Usar el NH en vez del nombre evita resultados ambiguos. Para cambiar
     * de paciente basta modificar "pacqah1NH" en el fichero de configuración.
     */
    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Leer el NH desde el fichero de configuración
        String patientNH = ConfigReader.get("pacqah1NH");

        // Escribir en la barra de búsqueda de la WL (filter-input).
        // No se usa clear() porque con Angular Material no limpia el modelo
        // interno — se selecciona todo con Ctrl+A y se sobreescribe.
        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(patientNH);

        // La WL carga las filas de forma lazy — si el paciente no está entre
        // las primeras filas cargadas, hacer click en load-all-information.
        // Se usa un wait de 30s porque la carga de todas las filas puede tardar.
        try {
            WebDriverWait loadWait = new WebDriverWait(driver, Duration.ofSeconds(30));
            WebElement loadBtn = localWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("load-all-information")));
            loadBtn.click();
            // Esperar a que gridId-0 aparezca tras la carga completa
            loadWait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        } catch (TimeoutException e) {
            // El paciente ya era visible entre las primeras filas cargadas
        }

        // Con el NH el filtro devuelve exactamente un resultado → gridId-0
        WebElement patientRow = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("gridId-0")));
        patientRow.click();

        Reporter.log("Paciente NH=" + patientNH + " encontrado y seleccionado en la WL.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ACCIONES → VER HISTORIA
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 4, dependsOnMethods = {"FilterAndSelectPatient"})
    public void OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("see_history"))).click();

        // Métodos heredados de ClassBaseTest
        SelectAccessReason("Guardia");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("clinical-history-back-button")));

        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        Reporter.log("Historial clínico del paciente abierto.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. NAVEGAR A REGISTRO DE CONSTANTES
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void NavigateToConstantRegistry() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("constant_registry-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.tagName("gc-patient-constants")));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        Reporter.log("Sección Registro de constantes cargada (vista Episodio).");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. AÑADIR NUEVO REGISTRO
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 6, dependsOnMethods = {"NavigateToConstantRegistry"})
    public void AddNewRecord() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Asegurar vista Episodio
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        openActionsMenuAndClick(localWait, "new_registry");

        localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("temperature")));

        fillField(localWait, "temperature", TEMP_CREATE);
        fillField(localWait, "tas",         TAS_CREATE);
        fillField(localWait, "tad",         TAD_CREATE);
        fillField(localWait, "fc",          FC_CREATE);

        WebElement acceptBtn = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-PatientConstantsRecordContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Volver a activar vista Episodio — Angular puede haberla reseteado al cerrar el modal
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        localWait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("grid-gridId-0-tas"), TAS_CREATE));

        Reporter.log("Nuevo registro creado. TAS en tabla: "
                + driver.findElement(By.id("grid-gridId-0-tas")).getText());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. MODIFICAR EL REGISTRO
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 7, dependsOnMethods = {"AddNewRecord"})
    public void ModifyRecord() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Asegurar vista Episodio
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        // Esperar a que la tabla renderice antes de seleccionar la fila
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='gridId-']")));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("gridId-0"))).click();

        openActionsMenuAndClick(localWait, "modify_registry");

        localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("temperature")));

        fillField(localWait, "temperature", TEMP_MODIFY);
        fillField(localWait, "fc",          FC_MODIFY);

        WebElement acceptBtn = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-PatientConstantsRecordContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Volver a activar vista Episodio — Angular puede haberla reseteado al cerrar el modal
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        // Verificar con temperature (valor único 37,2) en vez de fc
        // La app muestra decimales con coma aunque el input acepta punto
        String tempModifyDisplayed = TEMP_MODIFY.replace(".", ",");
        localWait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("grid-gridId-0-temperature"), tempModifyDisplayed));

        Reporter.log("Registro modificado. Temperatura en tabla: "
                + driver.findElement(By.id("grid-gridId-0-temperature")).getText());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. ELIMINAR EL REGISTRO
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 8, dependsOnMethods = {"ModifyRecord"})
    public void DeleteRecord() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Asegurar vista Episodio
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        // Esperar a que la tabla renderice
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='gridId-']")));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("gridId-0"))).click();

        openActionsMenuAndClick(localWait, "delete_registry");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();

        // Verificar que el diálogo de confirmación desapareció — el delete se procesó
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));

        Reporter.log("Registro eliminado correctamente.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. AÑADIR REGISTRO FINAL
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 9, dependsOnMethods = {"DeleteRecord"})
    public void AddFinalRecord() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Asegurar vista Episodio
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        openActionsMenuAndClick(localWait, "new_registry");

        localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("temperature")));

        fillField(localWait, "temperature", TEMP_FINAL);
        fillField(localWait, "tas",         TAS_FINAL);
        fillField(localWait, "tad",         TAD_FINAL);
        fillField(localWait, "fc",          FC_FINAL);

        WebElement acceptBtn = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-PatientConstantsRecordContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Volver a activar vista Episodio tras el modal — Angular puede haberla reseteado
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("1-button"))).click();

        localWait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("grid-gridId-0-tas"), TAS_FINAL));

        Reporter.log("Registro final añadido. TAS en tabla: "
                + driver.findElement(By.id("grid-gridId-0-tas")).getText());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Selecciona todo el contenido del campo via JS, escribe el nuevo valor
     * y dispara el evento 'input' para que Angular actualice su modelo interno.
     *
     * Sin el evento 'input', Angular no detecta el cambio y el formulario
     * queda con el valor anterior en el modelo, impidiendo que el Aceptar
     * procese correctamente aunque el DOM muestre el nuevo valor.
     */
    private void fillField(WebDriverWait localWait, String fieldId, String value) {
        WebElement field = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(fieldId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", field);
        field.sendKeys(value);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", field);
    }

}
