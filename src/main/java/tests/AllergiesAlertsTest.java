package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Alergias y alertas
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → Ver historia
 *   3.  Alergias y alertas
 *   4.  Crear 4 alertas:
 *         - Alergia alimentaria  (subcategoría: Gluten)
 *         - Otros                (subcategoría libre: texto)
 *         - Alergia medicamentos (medicamento: Amoxicilina)
 *         - Alergia medicamentos (grupo terapeutico: configurable via "alert.drug_group_search")
 *       Todas con nivel Relevante
 *   5.  Editar cada alerta (solo cambia fecha de inicio)
 *   6.  Eliminar cada alerta
 *
 * Notas del formulario "Nueva alerta":
 *   - vitalImportanceId  → mat-select Nivel (Relevante = vitalImportanceId-1)
 *   - category           → input autocomplete Categoría
 *   - subCategoryQuery   → input autocomplete Subcategoría
 *   - add-text-button    → confirma la subcategoría como chip (OBLIGATORIO antes de Aceptar)
 *   - startingDate       → Fecha inicio (datepicker, execCommand para limpiar)
 *   - accept-AllergiesAlertDialogComponent-button → Aceptar
 *   - cancel-AllergiesAlertDialogComponent-button → Cancelar
 *
 *   Fila del grid: grid-gridId-{N}-category (columna de categoría, usada para seleccionar)
 *   Acciones: new_alert, edit_alert, delete_alert
 *   Confirmación de eliminación: continue-DeleteAlertDialogComponent-button
 */
public class AllergiesAlertsTest extends ClassBaseTest {

    private static final Duration TIMEOUT    = Duration.ofSeconds(15);
    private static final String   LEVEL_ID   = "vitalImportanceId-1"; // Relevante
    private static final String   START_DATE = "01/01/2024";
    private static final String   EDIT_DATE  = "15/05/2025";
    private static final String   END_DATE   = "31/12/2026";

    // Indice de la primera alerta creada en este test.
    // Se calcula contando las filas del grid ANTES de crear la primera alerta,
    // de modo que editar/eliminar siempre apunta a las alertas correctas aunque
    // ya existan otras alertas previas en el grid.
    private int firstCreatedAlertIndex = 0;

    // Grupo terapeutico para la cuarta alerta (alergia a medicamentos).
    // El codigo puede variar segun el entorno:
    //   QA:  Y22A (si existe en la base de datos del entorno)
    //   Otro: Y71A (p.ej. "Y71a - agua de mar", disponible en nuestro entorno)
    // Se lee de config con fallback al valor por defecto del entorno actual.
    private static final String DRUG_GROUP_SEARCH =
            ConfigReader.get("alert.drug_group_search", "Y71A");

    // -------------------------------------------------------------------------
    // 1. LOGIN
    // -------------------------------------------------------------------------
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
            WebElement centerBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("defaultButtonId")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerBtn);
        } catch (TimeoutException e) {
            // Sin pantalla de seleccion de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado como medico.");
    }

    // -------------------------------------------------------------------------
    // 2. HOSPITALIZACION → LISTA DE TRABAJO → VER HISTORIA
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

    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        String patientNH = ConfigReader.get("pacqah1NH");

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(patientNH);

        try {
            WebElement loadBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("load-all-information")));
            loadBtn.click();
        } catch (TimeoutException e) {
            // El paciente ya era visible
        }

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-patient"))).click();

        Reporter.log("Paciente NH=" + patientNH + " seleccionado.");
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
                By.id("allergies_and_alerts-sidebar")));

        Reporter.log("Historial clinico abierto en nueva pestana.");
    }

    // -------------------------------------------------------------------------
    // 3. ALERGIAS Y ALERTAS
    // -------------------------------------------------------------------------
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterAllergiesAlerts() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("allergies_and_alerts-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button")));

        Reporter.log("Seccion Alergias y alertas cargada.");
    }

    // -------------------------------------------------------------------------
    // 4. CREAR 4 ALERTAS
    // -------------------------------------------------------------------------
    @Test(priority = 6, dependsOnMethods = {"EnterAllergiesAlerts"})
    public void CreateAlertAlimentaria() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Contar alertas existentes ANTES de crear para saber desde qué índice
        // editar y eliminar, evitando tocar alertas previas del paciente.
        firstCreatedAlertIndex = driver.findElements(
                By.cssSelector("[id$='-category'][id*='grid-gridId']")).size();
        Reporter.log("Alertas previas en el grid: " + firstCreatedAlertIndex);

        openActionsMenuAndClick(localWait, "new_alert");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AllergiesAlertDialogComponent-button")));

        fillAlertForm(localWait, "category-1", "Gluten", "subCategoryQuery-0", START_DATE);

        handleValidationAlert();

        Reporter.log("Alerta 1 creada: Alergia alimentaria.");
    }

    @Test(priority = 7, dependsOnMethods = {"CreateAlertAlimentaria"})
    public void CreateAlertOtros() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_alert");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AllergiesAlertDialogComponent-button")));

        fillAlertForm(localWait, "category-9", "Polen", "subCategoryQuery-0", START_DATE);

        handleValidationAlert();

        Reporter.log("Alerta 2 creada: Otros.");
    }

    @Test(priority = 8, dependsOnMethods = {"CreateAlertOtros"})
    public void CreateAlertMedicamentoAmoxicilina() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_alert");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AllergiesAlertDialogComponent-button")));

        fillAlertForm(localWait, "category-4", "Amoxicilina", "subCategoryQuery-0", START_DATE);

        handleValidationAlert();

        Reporter.log("Alerta 3 creada: Alergia medicamentos - Amoxicilina.");
    }

    @Test(priority = 9, dependsOnMethods = {"CreateAlertMedicamentoAmoxicilina"})
    public void CreateAlertMedicamentoY71A() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_alert");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AllergiesAlertDialogComponent-button")));

        fillAlertFormWithDrugProperty(localWait, "category-4", "drugProperty-1",
                DRUG_GROUP_SEARCH, "subCategoryQuery-0", START_DATE);

        handleValidationAlert();

        Reporter.log("Alerta 4 creada: Alergia medicamentos - " + DRUG_GROUP_SEARCH + " (Grupo terapeutico).");
    }

    // -------------------------------------------------------------------------
    // 5. EDITAR LAS 4 ALERTAS (solo cambia fecha de inicio)
    // -------------------------------------------------------------------------
    @Test(priority = 10, dependsOnMethods = {"CreateAlertMedicamentoY71A"})
    public void EditAlert1() {
        editAlert(firstCreatedAlertIndex, EDIT_DATE, END_DATE);
        Reporter.log("Alerta 1 editada.");
    }

    @Test(priority = 11, dependsOnMethods = {"EditAlert1"})
    public void EditAlert2() {
        editAlert(firstCreatedAlertIndex + 1, EDIT_DATE, END_DATE);
        Reporter.log("Alerta 2 editada.");
    }

    @Test(priority = 12, dependsOnMethods = {"EditAlert2"})
    public void EditAlert3() {
        editAlert(firstCreatedAlertIndex + 2, EDIT_DATE, END_DATE);
        Reporter.log("Alerta 3 editada.");
    }

    @Test(priority = 13, dependsOnMethods = {"EditAlert3"})
    public void EditAlert4() {
        editAlert(firstCreatedAlertIndex + 3, EDIT_DATE, END_DATE);
        Reporter.log("Alerta 4 editada.");
    }

    // -------------------------------------------------------------------------
    // 6. ELIMINAR LAS 4 ALERTAS
    // Siempre se elimina la fila 0 — tras eliminar la primera, la siguiente
    // ocupa su lugar.
    // -------------------------------------------------------------------------
    // Al eliminar, las filas del grid se desplazan hacia arriba.
    // Siempre se elimina el indice firstCreatedAlertIndex — tras cada eliminacion
    // la siguiente alerta creada ocupa ese mismo indice.
    @Test(priority = 14, dependsOnMethods = {"EditAlert4"})
    public void DeleteAlert1() {
        deleteAlert(firstCreatedAlertIndex);
        Reporter.log("Alerta 1 eliminada.");
    }

    @Test(priority = 15, dependsOnMethods = {"DeleteAlert1"})
    public void DeleteAlert2() {
        deleteAlert(firstCreatedAlertIndex);
        Reporter.log("Alerta 2 eliminada.");
    }

    @Test(priority = 16, dependsOnMethods = {"DeleteAlert2"})
    public void DeleteAlert3() {
        deleteAlert(firstCreatedAlertIndex);
        Reporter.log("Alerta 3 eliminada.");
    }

    @Test(priority = 17, dependsOnMethods = {"DeleteAlert3"})
    public void DeleteAlert4() {
        deleteAlert(firstCreatedAlertIndex);
        Reporter.log("Alerta 4 eliminada.");
    }

    // =========================================================================
    // METODOS AUXILIARES
    // =========================================================================

    /**
     * Rellena y guarda el formulario de nueva alerta.
     *
     * Pasos:
     *   1. Nivel: Relevante (mat-select vitalImportanceId)
     *   2. Categoría: autocomplete → opción por categoryOptionId
     *   3. Subcategoría: escribe searchText → selecciona primera opción →
     *      pulsa "Añadir" (OBLIGATORIO para confirmar el chip antes de Aceptar)
     *   4. Fecha inicio: limpia con execCommand + sendKeys
     *   5. Aceptar
     *
     * @param localWait        WebDriverWait activo
     * @param categoryOptionId ID de la opción de categoría (p.ej. "category-1")
     * @param subSearchText    Texto a buscar en subcategoría (p.ej. "Gluten")
     * @param subOptionId      ID de la opción de subcategoría a seleccionar
     * @param startDate        Fecha de inicio en formato dd/MM/yyyy
     */
    private void fillAlertForm(WebDriverWait localWait, String categoryOptionId,
                               String subSearchText, String subOptionId, String startDate) {
        // Esperar a que el formulario este completamente listo (sin paneles de opciones abiertos)
        localWait.until(d -> d.findElements(By.cssSelector("mat-option[id^='category-']")).isEmpty());

        // Nivel: Relevante
        selectWhenReady(localWait, "vitalImportanceId", LEVEL_ID);

        // Categoria: autocomplete
        WebElement catField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("category")));
        catField.click();
        WebElement catOption = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(categoryOptionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", catOption);
        // Esperar a que el panel del autocomplete de categoria se cierre
        localWait.until(d -> d.findElements(By.cssSelector("mat-option[id^='category-']")).isEmpty());

        // Subcategoria: escribir + seleccionar opcion + Añadir (obligatorio)
        WebElement subField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("subCategoryQuery")));
        subField.click();
        subField.sendKeys(subSearchText);

        try {
            WebElement subOption = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id(subOptionId)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", subOption);
        } catch (TimeoutException e) {
            // Si no hay opcion en el autocomplete (texto libre), se usa el texto tal cual
            Reporter.log("Subcategoria sin opcion autocomplete para '" + subSearchText + "' — usando texto libre.");
        }

        // Añadir confirma el chip de subcategoria (requisito del formulario)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("add-text-button"))).click();

        // Fecha de inicio
        fillDateField(localWait, "startingDate", startDate);

        // Aceptar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-AllergiesAlertDialogComponent-button"))).click();

        // Esperar a que el formulario se cierre
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AllergiesAlertDialogComponent-button")));
    }

    /**
     * Variante de fillAlertForm para alertas de medicamentos donde hay que cambiar
     * la "Propiedad de busqueda" (drugProperty mat-select) antes de buscar la subcategoria.
     *
     * Para "Alergia o intolerancia a medicamentos" el campo Principio activo solo acepta
     * valores de la base de datos. Si el medicamento no se encuentra con "Principios activos"
     * (por defecto), hay que cambiar drugProperty a "Grupo terapeutico" (drugProperty-1)
     * y buscar por el codigo del grupo (p.ej. "Y71A" en nuestro entorno, "Y22A" en QA).
     * El valor se configura en config.properties con la clave "alert.drug_group_search".
     *
     * @param localWait        WebDriverWait activo
     * @param categoryOptionId ID de la opcion de categoria
     * @param drugPropertyId   ID de la opcion de propiedad de busqueda (p.ej. "drugProperty-1")
     * @param subSearchText    Texto a buscar en el campo Principio activo
     * @param subOptionId      ID de la opcion de subcategoria a seleccionar
     * @param startDate        Fecha de inicio en formato dd/MM/yyyy
     */
    private void fillAlertFormWithDrugProperty(WebDriverWait localWait, String categoryOptionId,
                                               String drugPropertyId, String subSearchText,
                                               String subOptionId, String startDate) {
        // Esperar a que no haya mat-option de category del formulario anterior
        localWait.until(d -> d.findElements(
                By.cssSelector("mat-option[id^='category-']")).isEmpty());

        // Nivel: Relevante
        selectWhenReady(localWait, "vitalImportanceId", LEVEL_ID);

        // Categoria: autocomplete
        WebElement catField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("category")));
        catField.click();
        WebElement catOption = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(categoryOptionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", catOption);
        localWait.until(d -> d.findElements(
                By.cssSelector("mat-option[id^='category-']")).isEmpty());

        // Cambiar propiedad de busqueda antes de buscar el principio activo
        // El mat-select drugProperty aparece cuando la categoria es "Alergia a medicamentos"
        selectWhenReady(localWait, "drugProperty", drugPropertyId);

        // Principio activo / Grupo terapeutico: escribir + seleccionar opcion + Añadir
        WebElement subField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("subCategoryQuery")));
        subField.click();
        subField.sendKeys(subSearchText);

        WebElement subOption = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(subOptionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", subOption);

        // Añadir confirma la subcategoria en el grid interno
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("add-text-button"))).click();

        // Fecha de inicio
        fillDateField(localWait, "startingDate", startDate);

        // Aceptar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-AllergiesAlertDialogComponent-button"))).click();

        // Esperar cierre del formulario
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AllergiesAlertDialogComponent-button")));
    }

    /**
     * Selecciona una fila del grid por indice, abre Editar, cambia la fecha de inicio
     * y añade la fecha de fin (que no se rellena al crear la alerta).
     *
     * @param rowIndex   Indice de fila (0-based)
     * @param newDate    Nueva fecha de inicio en formato dd/MM/yyyy
     * @param newEndDate Fecha de fin a añadir en formato dd/MM/yyyy
     */
    private void editAlert(int rowIndex, String newDate, String newEndDate) {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila
        WebElement row = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-" + rowIndex + "-category")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", row);

        // Acciones → Editar alerta
        openActionsMenuAndClick(localWait, "edit_alert");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AllergiesAlertDialogComponent-button")));

        // Cambiar fecha de inicio
        fillDateField(localWait, "startingDate", newDate);

        // Añadir fecha de fin (no se pone al crear, se añade en la edicion)
        fillDateField(localWait, "endingDate", newEndDate);

        // Aceptar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-AllergiesAlertDialogComponent-button"))).click();

        // Esperar cierre del formulario
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AllergiesAlertDialogComponent-button")));
    }

    /**
     * Selecciona la fila 0 del grid, abre Eliminar y confirma.
     * Siempre elimina la primera fila — tras cada eliminacion la siguiente
     * sube a la posicion 0.
     */
    private void deleteAlert(int rowIndex) {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        WebElement row = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-" + rowIndex + "-category")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", row);

        // Acciones → Eliminar alerta
        openActionsMenuAndClick(localWait, "delete_alert");

        // Confirmar eliminacion — dialogo "¿Desea eliminar el registro?"
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();

        // Esperar a que el dialogo de confirmacion desaparezca
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));
    }

    /**
     * Abre un mat-select y selecciona la opcion indicada.
     *
     * A diferencia del formulario de heridas, el formulario de alerta mezcla
     * mat-select (vitalImportanceId) con autocompletes (category, subCategory).
     * La condicion isEmpty() de mat-option no es fiable aqui porque los autocompletes
     * pueden dejar opciones en el DOM de forma independiente al mat-select.
     *
     * Se usa elementToBeClickable en el select (garantiza que no hay overlay bloqueante)
     * y JS click en la opcion (evita el bloqueo del cdk-overlay-pane).
     */
    private void selectWhenReady(WebDriverWait localWait, String selectId, String optionId) {
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id(selectId))).click();

        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        // Esperar a que el panel del select se cierre
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id(optionId)));
    }

    /**
     * Limpia el campo de fecha y escribe el nuevo valor.
     *
     * Estrategia: triple-click selecciona todo el contenido del input, luego
     * sendKeys sobreescribe directamente. Es la unica tecnica que funciona de
     * forma fiable tanto en el formulario de nueva alerta como en el de edicion,
     * donde execCommand('selectAll') no siempre resetea el modelo Angular.
     */
    /**
     * Limpia el campo de fecha con CTRL+A + DELETE via sendKeys nativo de Selenium
     * y escribe el nuevo valor. Es el unico metodo que el datepicker de Angular
     * Material reconoce para limpiar el valor existente sin concatenar:
     *   - execCommand: no actualiza el modelo interno de Angular -> campo invalido
     *   - nativeSetter: Angular ignora el cambio -> valor no se guarda
     *   - CTRL+A + DELETE via sendKeys: el datepicker lo procesa como input real
     */
    private void fillDateField(WebDriverWait localWait, String fieldId, String date) {
        WebElement field = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(fieldId)));
        field.click();
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        field.sendKeys(Keys.DELETE);
        field.sendKeys(date);
        field.sendKeys(Keys.TAB);
    }
}
