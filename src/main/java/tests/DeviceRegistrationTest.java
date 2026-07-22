package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Registros → Registro de dispositivos
 *
 * Flujo completo:
 *   1.  Login como enfermera (username_nurse / password_nurse)
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → seleccionar
 *   3.  Acciones → Ver historia → nueva pestaña
 *   4.  Registros → Registro de dispositivos
 *   ─── Bloque Vía ───────────────────────────────────────────────────────────
 *   10. Añadir vía        (Periférico, Antebrazo derecho)
 *   11. Modificar vía     (Arterial, Antebrazo izquierdo)
 *   12. Retirar vía       (motivo: Fin de tratamiento)
 *   13. Añadir vía final  (PICC, Mano derecha)
 *   ─── Bloque Sondaje ──────────────────────────────────────────────────────
 *   20. Añadir sondaje        (Foley 2v, líquido: 500)
 *   21. Modificar sondaje     (Foley 3v, líquido: 800)
 *   22. Retirar sondaje       (motivo: Fin de tratamiento)
 *   23. Añadir sondaje final  (Nelatón, líquido: 500)
 *   ─── Bloque Drenaje ──────────────────────────────────────────────────────
 *   30. Añadir drenaje        (Drenaje Blake, Abdomen, líquido: 200)
 *   31. Modificar drenaje     (Penrose, Flanco derecho)
 *   32. Retirar drenaje       (motivo: Fin de tratamiento)
 *   33. Añadir drenaje final  (Sonda pleural, Abdomen, líquido: 200)
 *
 * IDs clave verificados en la app:
 *   Navegación:
 *     - hospitalization-sidebar, worklist-sidebar
 *     - filter-input, grid-gridId-{N}-patient, actions-button, see_history
 *     - registers-sidebar, device_registration-sidebar
 *   Sección dispositivos:
 *     - actions-button-add-devices             → botón "Añadir" con desplegable
 *     - add_intake / add_probing / add_drainage → ítems del menú añadir
 *     - modify / withdraw                      → ítems del menú editar/retirar
 *     - add-devices-{N}                        → filas del grid
 *     - grid-add-devices-{N}-device            → columna Dispositivo ("Vía","Sondaje","Drenaje")
 *     - grid-add-devices-{N}-removedDate       → columna fecha retirada ("-" = activo)
 *   Modal dispositivo (Vía / Sondaje / Drenaje):
 *     - type           → mat-select Tipo (type-0..N, distintos por dispositivo)
 *     - location       → Localización (Vía y Drenaje)
 *     - fluidAmount    → Cantidad de líquido (Sondaje y Drenaje)
 *     - number / other → campos opcionales
 *     - accept-DeviceComponent-button / cancel-DeviceComponent-button
 *   Modal retirada:
 *     - reason         → mat-select Motivo (reason-0..9)
 *     - observations   → textarea
 *     - accept-WithdrawDeviceDialogComponent-button
 *     - cancel-WithdrawDeviceDialogComponent-button
 *
 * Tipos Vía     → type-0 Arterial | type-4 Periférico | type-5 PICC
 * Tipos Sondaje → type-2 Foley 2v | type-3 Foley 3v   | type-8 Nelatón
 * Tipos Drenaje → type-1 Drenaje(Blake) | type-5 Penrose | type-6 Sonda pleural
 * Motivos retirada → reason-4 Fin de tratamiento
 */
public class DeviceRegistrationTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // ── Vía ──────────────────────────────────────────────────────────────────
    private static final String VIA_TYPE_CREATE    = "type-4"; // Periférico
    private static final String VIA_TYPE_FINAL     = "type-5"; // PICC
    private static final String VIA_LOCATION       = "Antebrazo derecho";
    private static final String VIA_LOCATION_MOD   = "Antebrazo izquierdo";
    private static final String VIA_LOCATION_FINAL = "Mano derecha";

    // ── Sondaje ───────────────────────────────────────────────────────────────
    private static final String SONDAJE_TYPE_CREATE = "type-2"; // Foley 2v
    private static final String SONDAJE_TYPE_FINAL  = "type-8"; // Nelatón
    private static final String SONDAJE_FLUID       = "500";
    private static final String SONDAJE_FLUID_MOD   = "800";

    // ── Drenaje ───────────────────────────────────────────────────────────────
    private static final String DRENAJE_TYPE_CREATE = "type-1"; // Drenaje (Blake)
    private static final String DRENAJE_TYPE_FINAL  = "type-6"; // Sonda pleural
    private static final String DRENAJE_LOCATION    = "Abdomen";
    private static final String DRENAJE_LOCATION_MOD= "Flanco derecho";
    private static final String DRENAJE_FLUID       = "200";
    private static final String DRENAJE_NUMBER      = "1";

    // ── Motivo retirada ───────────────────────────────────────────────────────
    private static final String WITHDRAW_REASON = "reason-4"; // Fin de tratamiento

    // =========================================================================
    // 1. LOGIN COMO ENFERMERA
    // =========================================================================
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

        try {
            WebElement centerBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("defaultButtonId")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerBtn);
        } catch (TimeoutException e) {
            // Sin pantalla de selección de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado como enfermera.");
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

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("filter-input")));

        Reporter.log("Lista de trabajo de Hospitalización cargada.");
    }

    // =========================================================================
    // 3. BUSCAR Y SELECCIONAR PACIENTE
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        String patientNH = ConfigReader.get("pacqah1NH");

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(patientNH);

        // Si hay botón "Cargar todo", pulsarlo para asegurar que el paciente es visible
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("load-all-information")))
                    .click();
        } catch (TimeoutException e) {
            // El paciente ya era visible sin cargar todo
        }

        // El grid filtra dinámicamente: buscar la primera celda -patient visible
        // que coincida con el NH del paciente
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='grid-gridId-'][id$='-patient']")));

        // Seleccionar la celda del paciente (primera fila tras el filtro = índice 0)
        WebElement patientCell = localWait.until(d -> {
            List<WebElement> cells = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-patient']"));
            return cells.isEmpty() ? null : cells.get(0);
        });

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", patientCell);

        Reporter.log("Paciente NH=" + patientNH + " seleccionado.");
    }

    // =========================================================================
    // 4. VER HISTORIA → NUEVA PESTAÑA
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = {"FilterAndSelectPatient"})
    public void OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Abrir menú Acciones → Ver historia
        openActionsMenuAndClick(localWait, "see_history");

        SelectAccessReason("Guardia");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        // Esperar a que la historia esté completamente cargada
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("registers-sidebar")));

        Reporter.log("Historial clínico abierto en nueva pestaña.");
    }

    // =========================================================================
    // 5. NAVEGAR A REGISTRO DE DISPOSITIVOS
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void NavigateToDeviceRegistry() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("registers-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("device_registration-sidebar"))).click();

        // Esperar a que el botón Añadir esté disponible — indica carga completa
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-add-devices")));

        Reporter.log("Sección Registro de dispositivos cargada.");
    }

    // =========================================================================
    // BLOQUE VÍA: añadir → modificar → retirar → añadir final
    // =========================================================================

    @Test(priority = 10, dependsOnMethods = {"NavigateToDeviceRegistry"})
    public void AddVia() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countActiveRows();

        openDeviceMenuAndClick(localWait, "add_intake");
        waitForModalToOpen(localWait);

        selectOption(localWait, "type", VIA_TYPE_CREATE);
        fillDeviceField(localWait, "location", VIA_LOCATION);

        acceptModal(localWait);
        dismissInfoAlert(localWait);

        localWait.until(d -> countActiveRows() == rowsBefore + 1);

        Reporter.log("Vía añadida. Tipo: Periférico | Loc: " + VIA_LOCATION);
    }

    @Test(priority = 11, dependsOnMethods = {"AddVia"})
    public void ModifyVia() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        clickLastActiveRowOfType(localWait, "Vía");
        openDeviceMenuAndClick(localWait, "modify");
        waitForModalToOpen(localWait);

        // El campo "type" es disabled en el modal de modificación — no se puede cambiar.
        // Solo se modifican los campos editables: location y fluidAmount.
        fillDeviceField(localWait, "location", VIA_LOCATION_MOD);

        acceptModal(localWait);

        Reporter.log("Vía modificada. Loc: " + VIA_LOCATION_MOD);
    }

    @Test(priority = 12, dependsOnMethods = {"ModifyVia"})
    public void RetireVia() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int activeRowsBefore = countActiveRows();

        clickLastActiveRowOfType(localWait, "Vía");
        withdrawDevice(localWait);

        localWait.until(d -> countActiveRows() == activeRowsBefore - 1);

        Reporter.log("Vía retirada correctamente.");
    }

    @Test(priority = 13, dependsOnMethods = {"RetireVia"})
    public void AddFinalVia() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countActiveRows();

        openDeviceMenuAndClick(localWait, "add_intake");
        waitForModalToOpen(localWait);

        selectOption(localWait, "type", VIA_TYPE_FINAL);
        fillDeviceField(localWait, "location", VIA_LOCATION_FINAL);

        acceptModal(localWait);
        dismissInfoAlert(localWait);

        localWait.until(d -> countActiveRows() == rowsBefore + 1);

        Reporter.log("Vía final añadida. Tipo: PICC | Loc: " + VIA_LOCATION_FINAL);
    }

    // =========================================================================
    // BLOQUE SONDAJE: añadir → modificar → retirar → añadir final
    // =========================================================================

    @Test(priority = 20, dependsOnMethods = {"AddFinalVia"})
    public void AddSondaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countActiveRows();

        openDeviceMenuAndClick(localWait, "add_probing");
        waitForModalToOpen(localWait);

        selectOption(localWait, "type", SONDAJE_TYPE_CREATE);
        fillDeviceField(localWait, "fluidAmount", SONDAJE_FLUID);

        acceptModal(localWait);
        dismissInfoAlert(localWait);

        localWait.until(d -> countActiveRows() == rowsBefore + 1);

        Reporter.log("Sondaje añadido. Tipo: Foley 2v | Líquido: " + SONDAJE_FLUID);
    }

    @Test(priority = 21, dependsOnMethods = {"AddSondaje"})
    public void ModifySondaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        clickLastActiveRowOfType(localWait, "Sondaje");
        openDeviceMenuAndClick(localWait, "modify");
        waitForModalToOpen(localWait);

        // El campo "type" es disabled en el modal de modificación — no se puede cambiar.
        fillDeviceField(localWait, "fluidAmount", SONDAJE_FLUID_MOD);

        acceptModal(localWait);

        Reporter.log("Sondaje modificado. Líquido: " + SONDAJE_FLUID_MOD);
    }

    @Test(priority = 22, dependsOnMethods = {"ModifySondaje"})
    public void RetireSondaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int activeRowsBefore = countActiveRows();

        clickLastActiveRowOfType(localWait, "Sondaje");
        withdrawDevice(localWait);

        localWait.until(d -> countActiveRows() == activeRowsBefore - 1);

        Reporter.log("Sondaje retirado correctamente.");
    }

    @Test(priority = 23, dependsOnMethods = {"RetireSondaje"})
    public void AddFinalSondaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countActiveRows();

        openDeviceMenuAndClick(localWait, "add_probing");
        waitForModalToOpen(localWait);

        selectOption(localWait, "type", SONDAJE_TYPE_FINAL);
        fillDeviceField(localWait, "fluidAmount", SONDAJE_FLUID);

        acceptModal(localWait);
        dismissInfoAlert(localWait);

        localWait.until(d -> countActiveRows() == rowsBefore + 1);

        Reporter.log("Sondaje final añadido. Tipo: Nelatón | Líquido: " + SONDAJE_FLUID);
    }

    // =========================================================================
    // BLOQUE DRENAJE: añadir → modificar → retirar → añadir final
    // =========================================================================

    @Test(priority = 30, dependsOnMethods = {"AddFinalSondaje"})
    public void AddDrenaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countActiveRows();

        openDeviceMenuAndClick(localWait, "add_drainage");
        waitForModalToOpen(localWait);

        selectOption(localWait, "type", DRENAJE_TYPE_CREATE);
        fillDeviceField(localWait, "number", DRENAJE_NUMBER);
        fillDeviceField(localWait, "location", DRENAJE_LOCATION);
        fillDeviceField(localWait, "fluidAmount", DRENAJE_FLUID);

        acceptModal(localWait);
        dismissInfoAlert(localWait);

        localWait.until(d -> countActiveRows() == rowsBefore + 1);

        Reporter.log("Drenaje añadido. Tipo: Drenaje (Blake) | Loc: " + DRENAJE_LOCATION);
    }

    @Test(priority = 31, dependsOnMethods = {"AddDrenaje"})
    public void ModifyDrenaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        clickLastActiveRowOfType(localWait, "Drenaje");
        openDeviceMenuAndClick(localWait, "modify");
        waitForModalToOpen(localWait);

        // El campo "type" es disabled en el modal de modificación — no se puede cambiar.
        fillDeviceField(localWait, "number", DRENAJE_NUMBER);
        fillDeviceField(localWait, "location", DRENAJE_LOCATION_MOD);

        acceptModal(localWait);

        Reporter.log("Drenaje modificado. Loc: " + DRENAJE_LOCATION_MOD);
    }

    @Test(priority = 32, dependsOnMethods = {"ModifyDrenaje"})
    public void RetireDrenaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int activeRowsBefore = countActiveRows();

        clickLastActiveRowOfType(localWait, "Drenaje");
        withdrawDevice(localWait);

        localWait.until(d -> countActiveRows() == activeRowsBefore - 1);

        Reporter.log("Drenaje retirado correctamente.");
    }

    @Test(priority = 33, dependsOnMethods = {"RetireDrenaje"})
    public void AddFinalDrenaje() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = countActiveRows();

        openDeviceMenuAndClick(localWait, "add_drainage");
        waitForModalToOpen(localWait);

        selectOption(localWait, "type", DRENAJE_TYPE_FINAL);
        fillDeviceField(localWait, "number", DRENAJE_NUMBER);
        fillDeviceField(localWait, "location", DRENAJE_LOCATION);
        fillDeviceField(localWait, "fluidAmount", DRENAJE_FLUID);

        acceptModal(localWait);
        dismissInfoAlert(localWait);

        localWait.until(d -> countActiveRows() == rowsBefore + 1);

        Reporter.log("Drenaje final añadido. Tipo: Sonda pleural | Loc: " + DRENAJE_LOCATION);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Abre el menú "Añadir" de dispositivos (actions-button-add-devices)
     * y hace JS click en el ítem indicado.
     * Usa presenceOfElementLocated para el ítem porque el overlay cdk puede
     * hacer que visibilityOf falle mientras el panel se anima.
     */
    private void openDeviceMenuAndClick(WebDriverWait localWait, String menuItemId) {
        // Cerrar el mat-drawer-backdrop si está abierto — impide hacer click
        // en actions-button-add-devices con "element click intercepted"
        ((JavascriptExecutor) driver).executeScript(
                "var b = document.querySelector('.mat-drawer-backdrop');" +
                        "if(b) b.click();");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-add-devices"))).click();

        WebElement menuItem = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(menuItemId)));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuItem);
    }

    /**
     * Espera a que el modal de dispositivo esté completamente renderizado.
     * Llamar SIEMPRE después de openDeviceMenuAndClick() y ANTES de rellenar campos.
     * La espera de elementToBeClickable en "type" garantiza que Angular ha terminado
     * de inicializar el formulario reactivo.
     */
    private void waitForModalToOpen(WebDriverWait localWait) {
        // Esperar a que el contenedor del modal esté en DOM...
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.tagName("mat-dialog-container")));
        // ...y a que el campo accept esté listo, lo que garantiza que Angular
        // ha terminado de inicializar el formulario.
        // No se usa elementToBeClickable en "type" porque en el modal de
        // modificación ese mat-select está disabled (aria-disabled=true).
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-DeviceComponent-button")));
    }

    /**
     * Acepta el modal de dispositivo via JS click y espera a que se cierre.
     * NOTA: NO gestiona el aviso "Ya existe un catéter con este tipo" aquí.
     * Ese aviso aparece DESPUÉS de que el modal ya desapareció del DOM (asíncrono
     * en Angular), por lo que debe gestionarse con dismissInfoAlert() en el
     * método llamador, tras esta llamada.
     */
    private void acceptModal(WebDriverWait localWait) {
        // Cerrar el mat-drawer-backdrop si está presente — interfiere con el modal
        // porque Angular lo renderiza encima de mat-dialog-container al navegar
        // por el sidebar, y puede hacer que invisibilityOf(mat-dialog-container)
        // nunca se cumpla al confundir los overlays del CDK.
        ((JavascriptExecutor) driver).executeScript(
                "var b = document.querySelector('.mat-drawer-backdrop');" +
                        "if(b) b.click();");

        WebElement btn = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-DeviceComponent-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        // Esperar a que el botón Accept desaparezca del DOM — más fiable que
        // esperar invisibilityOf(mat-dialog-container) porque el backdrop del
        // drawer puede mantener el overlay activo brevemente tras el cierre.
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-DeviceComponent-button")));
    }

    /**
     * Gestiona el aviso informativo "Ya existe un catéter con este tipo" que
     * puede aparecer DESPUÉS de que el modal del formulario ya se cerró.
     * El aviso es asíncrono en Angular: llega entre 100-500ms tras el cierre
     * del formulario, por lo que handleValidationAlert() (que usa un shortWait
     * de 3s ejecutado ANTES del invisibilityOf) no lo captura.
     *
     * Estrategia: esperar con presenceOfElementLocated hasta 5s. Si el aviso
     * no aparece, continuar silenciosamente. Si aparece, hacer JS click en
     * continue-button y esperar a que desaparezca.
     */
    private void dismissInfoAlert(WebDriverWait localWait) {
        try {
            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement continueBtn = alertWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("continue-button")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn);
            alertWait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("continue-button")));
            Reporter.log("Aviso informativo 'Ya existe un catéter' gestionado.");
        } catch (TimeoutException e) {
            // Sin aviso — normal cuando no hay duplicado del tipo
        }
    }

    /**
     * Rellena un campo de texto del modal usando nativeSetter + dispatchEvent.
     * Necesario para campos Angular reactivos donde .sendKeys() no dispara
     * el change detection correctamente.
     */
    private void fillDeviceField(WebDriverWait localWait, String fieldId, String value) {
        WebElement field = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(fieldId)));

        ((JavascriptExecutor) driver).executeScript(
                "var setter = Object.getOwnPropertyDescriptor(" +
                        "  window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(arguments[0], arguments[1]);" +
                        "arguments[0].dispatchEvent(new Event('input',  {bubbles:true}));" +
                        "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                field, value);
    }

    /**
     * Selecciona la última fila ACTIVA del tipo de dispositivo indicado.
     * "Activa" = columna removedDate contiene "-" (no tiene fecha de retirada).
     * Iterar sobre todas las filas y quedarse con la última que cumpla ambas
     * condiciones garantiza seleccionar siempre el dispositivo más reciente
     * del tipo dado, aunque haya varios del mismo tipo en el historial.
     *
     * IDs del grid:
     *   - Fila:          add-devices-{N}
     *   - Dispositivo:   grid-add-devices-{N}-device      → "Vía" / "Sondaje" / "Drenaje"
     *   - Fecha retirada: grid-add-devices-{N}-removedDate → "-" si activo
     */
    private void clickLastActiveRowOfType(WebDriverWait localWait, String deviceType) {
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='add-devices-']")));

        List<WebElement> rows = driver.findElements(By.cssSelector("[id^='add-devices-']"));

        WebElement targetRow = null;
        for (WebElement row : rows) {
            String rowId        = row.getAttribute("id");
            String deviceCellId = "grid-" + rowId + "-device";
            String removedCellId= "grid-" + rowId + "-removedDate";

            List<WebElement> deviceCells  = driver.findElements(By.id(deviceCellId));
            List<WebElement> removedCells = driver.findElements(By.id(removedCellId));

            boolean matchesType = !deviceCells.isEmpty()
                    && deviceType.equals(deviceCells.get(0).getText().trim());
            boolean isActive    = !removedCells.isEmpty()
                    && "-".equals(removedCells.get(0).getText().trim());

            if (matchesType && isActive) {
                targetRow = row;
                break; // Las filas están ordenadas DESC por fecha — la primera
                // coincidencia activa es siempre la más reciente.
            }
        }

        if (targetRow == null) {
            throw new NoSuchElementException(
                    "No se encontró ninguna fila activa de tipo '" + deviceType + "'.");
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", targetRow);
    }

    /**
     * Devuelve el número de filas ACTIVAS en la tabla de dispositivos.
     * Se usa para verificar que añadir/retirar cambia el conteo correctamente.
     */
    private int countActiveRows() {
        List<WebElement> rows = driver.findElements(By.cssSelector("[id^='add-devices-']"));
        int active = 0;
        for (WebElement row : rows) {
            String cellId = "grid-" + row.getAttribute("id") + "-removedDate";
            List<WebElement> cells = driver.findElements(By.id(cellId));
            if (!cells.isEmpty() && "-".equals(cells.get(0).getText().trim())) {
                active++;
            }
        }
        return active;
    }

    /**
     * Retira el dispositivo actualmente seleccionado.
     * Flujo: menú Acciones → "Retirar" → seleccionar motivo → Aceptar.
     * Usa selectOption de ClassBaseTest para el mat-select de motivo.
     * handleDeleteConfirmation() gestiona el posible diálogo adicional de confirmación.
     */
    private void withdrawDevice(WebDriverWait localWait) {
        openDeviceMenuAndClick(localWait, "withdraw");

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Usar selectOption de ClassBaseTest: click en el mat-select + JS click en opción
        selectOption(localWait, "reason", WITHDRAW_REASON);

        WebElement acceptBtn = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-WithdrawDeviceDialogComponent-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        handleDeleteConfirmation(localWait);
    }

    /**
     * Gestiona el diálogo de confirmación adicional que puede aparecer
     * tras aceptar la retirada (ej. "¿Confirmar eliminación?").
     */
    private void handleDeleteConfirmation(WebDriverWait localWait) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement confirmBtn = shortWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("alert-confirm")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
            shortWait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("alert-confirm")));
        } catch (TimeoutException e) {
            // Sin diálogo adicional de confirmación
        }
    }
}
