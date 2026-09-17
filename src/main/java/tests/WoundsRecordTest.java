package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Registros → Registro de heridas
 *
 * Flujo:
 *   1.  Login como enfermera
 *   2.  Hospitalización → Lista de trabajo
 *   3.  Filtrar la WL por el NH del paciente → seleccionar fila
 *   4.  Acciones → Ver historia → cambiar a la nueva pestaña
 *   5.  Gestionar modales de alertas/solo-lectura
 *   6.  Registros → Registro de heridas
 *   7.  Acciones → Nueva herida → rellenar formulario → Aceptar → verificar fila en grid
 *   8.  Seleccionar herida → Acciones → Editar → Nueva hoja (empeoramiento) → Aceptar
 */
public class WoundsRecordTest extends ClassBaseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // --- Datos de prueba: hoja inicial (evolucion favorable) -----------------
    private static final String APPEARANCE_DATE      = "14/05/2026";
    private static final String APPEARANCE_TIME      = "10:00";
    private static final String WOUND_TYPE_ID        = "type-3";          // Herida quirurgica
    private static final String WOUND_STATE_ID       = "state-1";         // Evolucion favorable
    private static final String LOCATION_SEARCH      = "Abd";
    private static final String LOCATION_OPTION_ID   = "location-0";      // Tronco anterior abdominal epigastrio
    private static final String WOUND_BED_SEARCH     = "Gran";
    private static final String WOUND_BED_OPTION_ID  = "woundBedState-0"; // Tejidos de granulacion
    private static final String EXUDATE_SEARCH       = "Seroso";
    private static final String EXUDATE_OPTION_ID    = "exudated-0";      // Seroso

    // --- Datos de prueba: segunda hoja (empeoramiento) -----------------------
    private static final String WOUND_STATE_EDIT_ID = "state-0"; // Empeoramiento

    // -------------------------------------------------------------------------
    // 1. LOGIN
    // -------------------------------------------------------------------------
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_nurse"));

        driver.findElement(By.id("password"))
                .sendKeys(ConfigReader.get("password_nurse"));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        // La pantalla de seleccion de centro puede aparecer o no segun el entorno.
        // Click directo falla con "element not interactable" en Angular Material;
        // se usa JS click igual que en el resto del proyecto.
        try {
            WebElement centerBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("defaultButtonId")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerBtn);
        } catch (TimeoutException e) {
            // Sin pantalla de seleccion de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado como enfermera.");
    }

    // -------------------------------------------------------------------------
    // 2. HOSPITALIZACION → LISTA DE TRABAJO
    // -------------------------------------------------------------------------
    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void EnterHospitalizationWL() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("filter-input")));

        Reporter.log("Lista de trabajo de Hospitalizacion cargada.");
    }

    // -------------------------------------------------------------------------
    // 3. FILTRAR Y SELECCIONAR PACIENTE
    // -------------------------------------------------------------------------
    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        String patientNH = ConfigReader.get("pacqah1NH");

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(patientNH);

        // Si hay filas pendientes de carga, forzar la carga total
        try {
            WebElement loadBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("load-all-information")));
            loadBtn.click();
        } catch (TimeoutException e) {
            // El paciente ya era visible sin necesidad de cargar todo
        }

        // Con el NH el filtro devuelve exactamente un resultado
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-patient"))).click();

        Reporter.log("Paciente NH=" + patientNH + " seleccionado en la WL.");
    }

    // -------------------------------------------------------------------------
    // 4. ACCIONES → VER HISTORIA → CAMBIAR PESTANA
    // -------------------------------------------------------------------------
    @Test(priority = 4, dependsOnMethods = {"FilterAndSelectPatient"})
    public void OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "see_history");

        // Modal de acceso a historia ajena (paciente no asignado)
        SelectAccessReason("Guardia");

        // Cambiar a la nueva pestana del historial clinico
        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("registers-sidebar")));

        Reporter.log("Historial clinico del paciente abierto en nueva pestana.");
    }

    // -------------------------------------------------------------------------
    // 5. REGISTROS → REGISTRO DE HERIDAS
    // -------------------------------------------------------------------------
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterWoundsRecord() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("registers-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("wounds_record-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button")));

        Reporter.log("Seccion Registro de heridas cargada.");
    }

    // -------------------------------------------------------------------------
    // 6. NUEVA HERIDA → HOJA INICIAL (EVOLUCION FAVORABLE)
    // -------------------------------------------------------------------------
    @Test(priority = 6, dependsOnMethods = {"EnterWoundsRecord"})
    public void CreateWound() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new");

        // Esperar a que el formulario este completamente cargado
        // (select-default-id clickable confirma que el panel esta listo)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-WoundRecordEvolutionContainer-button")));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("select-default-id")));

        // Fecha de evolucion
        selectWhenOptionReady(localWait, "select-default-id", "select-default-id-0");

        // Tipo: Herida quirurgica
        selectWhenOptionReady(localWait, "type", WOUND_TYPE_ID);

        // Fecha y hora de aparicion
        fillDateField(localWait, "appearanceDate", APPEARANCE_DATE);
        // El campo hora tiene id dinamico (mat-input-N, variable entre sesiones).
        // Se localiza de forma estable por su mat-label "Hora".
        WebElement horaField = localWait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//mat-label[normalize-space()='Hora']/ancestor::mat-form-field//input")));
        horaField.click();
        horaField.sendKeys(APPEARANCE_TIME);

        // Evolucion favorable
        selectWhenOptionReady(localWait, "state", WOUND_STATE_ID);

        // Ubicacion (obligatorio)
        fillAutocomplete(localWait, "location", LOCATION_SEARCH, LOCATION_OPTION_ID);

        // Lecho de herida: tejidos de granulacion (buen estado)
        fillAutocomplete(localWait, "woundBedState", WOUND_BED_SEARCH, WOUND_BED_OPTION_ID);

        // Exudado: seroso (buen estado)
        fillAutocomplete(localWait, "exudated", EXUDATE_SEARCH, EXUDATE_OPTION_ID);

        saveWoundForm(localWait);

        // Esperar a que el formulario se cierre antes de interactuar con el grid
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-WoundRecordEvolutionContainer-button")));

        // Verificar que la herida aparece en el grid buscando por su estado conocido
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("(//td[contains(@id,'grid-gridId') and contains(@id,'-state') " +
                        "and normalize-space(.)='Evolución favorable'])[last()]")));

        Reporter.log("Nueva herida creada con evolucion favorable y visible en el grid.");
    }

    // -------------------------------------------------------------------------
    // 7. EDITAR HERIDA → NUEVA HOJA (EMPEORAMIENTO)
    // -------------------------------------------------------------------------
    @Test(priority = 7, dependsOnMethods = {"CreateWound"})
    public void EditWoundAddNewPage() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Cerrar cualquier formulario de herida que pudiera estar abierto antes
        // de intentar seleccionar una fila del grid. Si el formulario esta abierto
        // y se intenta editar otra herida, la app muestra un modal de confirmacion
        // que bloquea el flujo. Cancelar primero evita ese modal por completo.
        try {
            WebElement cancelBtn = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.id("cancel-WoundRecordEvolutionContainer-button")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cancelBtn);
            localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("cancel-WoundRecordEvolutionContainer-button")));
        } catch (TimeoutException e) {
            // No habia formulario abierto — continuar directamente
        }

        // La herida creada en CreateWound (Evolucion favorable) se añade al final
        // del grid (orden ascendente por fecha). Se localiza por su columna de estado
        // en lugar de por indice de fila, para que el test funcione aunque ya haya
        // heridas previas en el grid.
        WebElement woundRow = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("(//td[contains(@id,'grid-gridId') and contains(@id,'-state') " +
                        "and normalize-space(.)='Evolución favorable'])[last()]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", woundRow);

        // Acciones → Editar
        openActionsMenuAndClick(localWait, "edit");

        // Esperar a que el formulario este completamente cargado
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-WoundRecordEvolutionContainer-button")));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("select-default-id")));

        // Boton "Nueva" — anade una nueva hoja de evolucion a la herida.
        // JS click para evitar el bloqueo del cdk-overlay-pane mientras se anima.
        WebElement nuevaBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("(//*[@id='first-right-button'])[1]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nuevaBtn);

        // Esperar a que la nueva hoja este completamente cargada
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-WoundRecordEvolutionContainer-button")));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("select-default-id")));

        // Fecha de evolucion de la nueva hoja
        selectWhenOptionReady(localWait, "select-default-id", "select-default-id-0");

        // Empeoramiento
        selectWhenOptionReady(localWait, "state", WOUND_STATE_EDIT_ID);

        // La ubicacion, lecho de herida y exudado vienen pre-rellenados de la hoja
        // anterior como chips. Son campos opcionales — no se modifican para evitar
        // que el panel de sugerencias del mat-chip-input quede abierto y bloquee
        // el isEmpty() de saveWoundForm.
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("location")));

        saveWoundForm(localWait);

        Reporter.log("Nueva hoja de evolucion con empeoramiento guardada correctamente.");
    }

    // =========================================================================
    // METODOS AUXILIARES
    // =========================================================================

    /**
     * Abre un mat-select y selecciona la opcion indicada de forma sincronizada.
     *
     * Sincronizacion entre selects consecutivos:
     *   Cuando un mat-select se abre, Angular renderiza las mat-option en el
     *   cdk-overlay-container. Cuando se cierra, las elimina del DOM.
     *   Esperar a que no haya ninguna mat-option en el DOM antes de abrir el
     *   siguiente select garantiza que el panel anterior se cerro completamente,
     *   eliminando la race condition entre selects consecutivos.
     *
     *   Esta condicion es independiente del numero de backdrops, que varia segun
     *   como se abrio el formulario (JS click vs click nativo).
     *
     * El click nativo es necesario para que Angular abra el panel de opciones.
     * JS click no dispara los eventos internos del cdk-select y el panel no abre.
     * El click en la opcion usa JS para evitar el bloqueo del cdk-overlay-pane.
     */
    private void selectWhenOptionReady(WebDriverWait localWait, String selectId, String optionId) {
        // Esperar a que no haya mat-option en el DOM (ningun select previo abierto)
        localWait.until(d -> d.findElements(By.tagName("mat-option")).isEmpty());

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id(selectId))).click();

        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        // Esperar a que el panel se cierre (mat-option desaparecen del DOM)
        // antes de retornar, evitando la race condition con el siguiente select.
        localWait.until(d -> d.findElements(By.tagName("mat-option")).isEmpty());
    }

    /**
     * Limpia el campo de fecha con execCommand selectAll+delete (unico metodo que el
     * datepicker de Angular Material acepta sin duplicar el valor), escribe la fecha
     * nueva y dispara Tab para que el datepicker la valide internamente.
     *
     * native setter + sendKeys duplica el valor porque el datepicker mantiene
     * un modelo interno que no se resetea con la asignacion directa de .value.
     * execCommand opera sobre la seleccion activa del DOM que Angular si entiende.
     */
    private void fillDateField(WebDriverWait localWait, String fieldId, String date) {
        WebElement field = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(fieldId)));
        field.click();
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].focus();" +
                        "document.execCommand('selectAll');" +
                        "document.execCommand('delete');", field);
        field.sendKeys(date);
        field.sendKeys(Keys.TAB);
    }

    /**
     * Escribe en el campo de autocomplete y selecciona la opcion via JS click.
     *
     * Algunos campos (woundBedState, exudated) son mat-chip-input: el metodo
     * arguments[0].select() no funciona en ellos. Se usa click() + sendKeys()
     * directamente, que es lo que Angular escucha para filtrar las opciones.
     *
     * Si la opcion no aparece en 5s el metodo continua sin fallar (campos opcionales).
     */
    private void fillAutocomplete(WebDriverWait localWait, String fieldId,
                                  String searchText, String optionId) {
        try {
            WebElement field = localWait.until(
                    ExpectedConditions.elementToBeClickable(By.id(fieldId)));
            field.click();
            // Limpiar el campo antes de escribir para evitar concatenacion con
            // texto previo. Ctrl+A + Delete funciona en mat-chip-input y mat-input.
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            field.sendKeys(searchText);

            // Al filtrar por texto, Angular asigna indices 0,1,2... a las opciones
            // visibles independientemente de su posicion en la lista completa.
            // Por tanto optionId debe ser el indice relativo al filtro (siempre 0
            // cuando el texto de busqueda devuelve una sola coincidencia).
            WebElement option = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        } catch (TimeoutException e) {
            Reporter.log("Opcion de autocomplete no encontrada para " + fieldId + " — se omite.");
        }
    }

    /**
     * Guarda el formulario via JS click en Aceptar.
     *
     * Antes de hacer click espera a que no haya mat-option en el DOM — garantiza
     * que el ultimo select cerro completamente y no hay overlay bloqueante.
     *
     * Tras el click gestiona el aviso "continue-button" si aparece
     * (p.ej. "espere 1 minuto entre operaciones" o aviso de validacion).
     * Usa JS click en continue-button para evitar StaleElementReferenceException
     * si el elemento desaparece justo al hacer click nativo.
     */
    private void saveWoundForm(WebDriverWait localWait) {
        // Esperar a que no haya ningun panel de opciones abierto antes de guardar
        localWait.until(d -> d.findElements(By.tagName("mat-option")).isEmpty());

        WebElement acceptBtn = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-WoundRecordEvolutionContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        try {
            WebElement continueBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.id("continue-button")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn);
        } catch (TimeoutException e) {
            // Sin aviso — el formulario se guardo directamente
        }
    }
}
