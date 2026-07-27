package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Test: Hemoderivados
 *
 * ── Credenciales ──────────────────────────────────────────────────────────────
 *   username_doctor / password_doctor  (via ClassBaseTest.LoginAsDoctor)
 *
 * ── Paciente ──────────────────────────────────────────────────────────────────
 *   pacqah1NH  (De La Torre Albuquerque, Juan)
 *
 * ── Flujo ─────────────────────────────────────────────────────────────────────
 *   1.  Login médico
 *   2.  Hospitalización → worklist → filtrar y seleccionar paciente
 *   3.  Acciones → Ver historia → nueva pestaña
 *   4.  blood_products-sidebar → Hemoderivados
 *   5.  Nueva solicitud → rellena campos obligatorios → Firmar
 *   6.  Editar solicitud → Firmar
 *   7.  Registrar extracción → Control de seguridad → Registro de extracción → Firmar
 *   8.  Transfundir → Control de seguridad → rellena campos obligatorios → Aceptar
 *
 * ── IDs relevantes ────────────────────────────────────────────────────────────
 *   Sidebar              → blood_products-sidebar
 *   Menú acciones:
 *     Nueva solicitud    → new_request
 *     Editar solicitud   → edit_request
 *     Registrar extrac.  → extraction_register
 *     Transfundir        → transfuse
 *   Formulario solicitud (nueva y editar):
 *     Área               → area           (mat-select, opciones area-0..N)
 *     Prioridad          → priority       (mat-select, opciones priority-0..N)
 *     Fecha esperada     → expectedDate   (input)
 *     Hora               → (XPath por label "Hora") — ID dinámico
 *     Motivo             → codReason      (mat-select, opciones codReason-0..N)
 *     Área destino       → destinationArea(mat-select, opciones destinationArea-0..N)
 *     Producto           → type           (mat-select, opciones type-0..N)
 *     Cantidad           → amount         (input)
 *     Añadir producto    → add-product-button
 *     Firmar             → sign-BloodTransfusionRequestContainer-button
 *     Cancelar           → cancel-BloodTransfusionRequestContainer-button
 *   Modal firma solicitud/extracción:
 *     Contraseña         → password
 *     Firmar             → sign-UserSignComponent-button
 *     Cancelar           → cancel-UserSignComponent-button
 *   Control de seguridad (extracción y transfusión):
 *     Código solicitud   → requestCode
 *     Confirmar código   → confirmRequestCode
 *     NST                → transfusionSecurityNumber
 *     Aceptar            → accept-SecurityControlContainer-button
 *     Cancelar           → cancel-SecurityControlContainer-button
 *   Registro extracción:
 *     Fecha              → date
 *     Hora               → (XPath por label "Hora") — ID dinámico
 *     Firmar             → Firmar-ExtractionLogComponent-button
 *     Cancelar           → cancel-ExtractionLogComponent-button
 *   Formulario transfundir:
 *     Fecha inicio       → startDate
 *     Hora inicio        → (XPath por label "Hora inicio") — ID dinámico
 *     Nº Bolsa           → bagNumber
 *     Confirmar Nº Bolsa → confirmBagNumber
 *     Volumen            → volume
 *     Firmar (header)    → look-notes-button
 *     Aceptar            → accept-TransfuseConstantsContainer-button
 *     Cancelar           → cancel-TransfuseConstantsContainer-button
 *   Fila de la tabla:
 *     Primera fila       → gridId-0
 *     Código solicitud   → grid-gridId-0-request_code (para leerlo)
 *
 * ── Notas ─────────────────────────────────────────────────────────────────────
 *   · El campo fecha (expectedDate, startDate, date) requiere setValueNative()
 *     porque sendKeys concatena al valor existente en Angular.
 *   · El campo Hora tiene ID dinámico (mat-input-N) — se localiza por XPath
 *     buscando el input dentro del mat-form-field que contiene la label "Hora".
 *   · El código de solicitud se lee de la tabla (grid-gridId-0-request_code)
 *     para usarlo en los controles de seguridad de extracción y transfusión.
 */
public class BloodProductsTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // Código de solicitud capturado al crear — se reutiliza en extracción y transfusión
    private String requestCode = null;

    // =========================================================================
    // TC1 — LOGIN
    // =========================================================================
    @Test(priority = 1)
    public void TC1_Login() {
        LoginAsDoctor();

        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("hospitalization-sidebar")));

        Reporter.log("TC1 OK — Login médico completado.");
    }

    // =========================================================================
    // TC2 — HOSPITALIZACIÓN → LISTA DE TRABAJO
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = {"TC1_Login"})
    public void TC2_EnterHospitalizationWorklist() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("initial-loading")));

        localWait.until(d -> {
            d.findElement(By.id("hospitalization-sidebar")).click();
            return !d.findElements(By.id("worklist-sidebar")).isEmpty();
        });

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
    // TC4 — ABRIR HISTORIA → HEMODERIVADOS
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = {"TC3_FilterAndSelectPatient"})
    public void TC4_OpenBloodProducts() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "see_history");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        handleReadOnlyAlert();
        handleVitalAlertsDialog();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("blood_products-sidebar"))).click();

        // Esperar a que la URL cambie a blood-products
        localWait.until(ExpectedConditions.urlContains("blood-products"));

        // Esperar a que la primera fila de la tabla esté en el DOM.
        // El spinner de Angular nunca desaparece en esta pantalla (hay un
        // observable activo permanente). La presencia de gridId-0 indica que
        // la tabla ha terminado de renderizar y el menú Acciones es usable.
        // Si es la primera vez (tabla vacía), esperar al actions-button directamente.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        } catch (TimeoutException ignored) { /* tabla vacía, continuar */ }

        // El drawer lateral del sidebar puede quedar activo con su backdrop —
        // esperar a que desaparezca para que actions-button sea clickable.
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".mat-drawer-backdrop.mat-drawer-shown")));

        localWait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));

        Reporter.log("TC4 OK — Hemoderivados abierto.");
    }

    // =========================================================================
    // TC5 — NUEVA SOLICITUD
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"TC4_OpenBloodProducts"})
    public void TC5_NewRequest() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Nueva solicitud: se abre con click nativo (no JS click) porque el formulario
        // de hemoderivados no responde correctamente al JS click del overlay de Angular.
        // Se reintenta hasta que el botón Firmar del formulario esté presente.
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("new_request"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("sign-BloodTransfusionRequestContainer-button")));

        // Expandir todos los paneles colapsados del acordeón (si los hay)
        expandAllCollapsedPanels();

        // Campos obligatorios sección Solicitud
        clickSelectNative(localWait, "area",     "area-0");
        clickSelectNative(localWait, "priority", "priority-0");

        // Campos obligatorios sección Transfusión
        // La fecha esperada debe ser superior a hoy — usar mañana
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        setValueNative("expectedDate", tomorrow);

        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        fillInputByLabel("Hora", hora);  // label en DOM es "Hora" sin asterisco

        clickSelectNative(localWait, "codReason",       "codReason-0");
        clickSelectNative(localWait, "destinationArea", "destinationArea-0");

        // Añadir un producto (obligatorio para poder firmar)
        clickSelectNative(localWait, "type", "type-0");
        driver.findElement(By.id("amount")).sendKeys("1");
        driver.findElement(By.id("add-product-button")).click();

        // Firmar solicitud
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-BloodTransfusionRequestContainer-button"))).click();

        signWithPassword(localWait, ConfigReader.get("password_doctor"));

        // Esperar a que la lista recargue y leer el código de solicitud
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        requestCode = driver.findElement(By.id("grid-gridId-0-request_code")).getText().trim();

        Reporter.log("TC5 OK — Solicitud creada y firmada. Código: " + requestCode);
    }

    // =========================================================================
    // TC6 — EDITAR SOLICITUD
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = {"TC5_NewRequest"})
    public void TC6_EditRequest() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila que corresponde al requestCode creado en TC5
        selectRowByRequestCode(localWait, requestCode);
        openActionsMenuAndClick(localWait, "edit_request");

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("sign-BloodTransfusionRequestContainer-button")));

        // Firmar directamente (los datos ya están rellenos)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-BloodTransfusionRequestContainer-button"))).click();

        signWithPassword(localWait, ConfigReader.get("password_doctor"));

        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));

        Reporter.log("TC6 OK — Solicitud editada y firmada.");
    }

    // =========================================================================
    // TC7 — REGISTRAR EXTRACCIÓN
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = {"TC6_EditRequest"})
    public void TC7_RegisterExtraction() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        handleSessionLock(ConfigReader.get("password_doctor"));
        selectRowByRequestCode(localWait, requestCode);
        openActionsMenuAndClick(localWait, "extraction_register");

        // Control de seguridad
        fillSecurityControl(localWait, requestCode);

        // Formulario "Registro de extracción" — fecha y hora ya vienen rellenas
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Firmar-ExtractionLogComponent-button")));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("Firmar-ExtractionLogComponent-button"))).click();

        signWithPassword(localWait, ConfigReader.get("password_doctor"));

        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));

        Reporter.log("TC7 OK — Extracción registrada y firmada.");
    }

    // =========================================================================
    // TC8 — TRANSFUNDIR
    // =========================================================================
    @Test(priority = 8, dependsOnMethods = {"TC7_RegisterExtraction"})
    public void TC8_Transfuse() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        handleSessionLock(ConfigReader.get("password_doctor"));
        selectRowByRequestCode(localWait, requestCode);
        openActionsMenuAndClick(localWait, "transfuse");

        // Control de seguridad
        fillSecurityControl(localWait, requestCode);

        // Formulario "Transfundir" — campos obligatorios
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("startDate")));

        // Usar setValueNative para la fecha — sendKeys concatenaría al valor existente
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        setValueNative("startDate", today);

        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        fillInputByLabel("Hora inicio", hora);

        // Nº Bolsa — se usa el código de solicitud como valor de prueba
        setValueNative("bagNumber", requestCode);
        setValueNative("confirmBagNumber", requestCode);
        setValueNative("volume", "100");

        // Firmar — tras la firma el formulario se guarda y cierra automáticamente,
        // no hace falta clicar accept-TransfuseConstantsContainer-button
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("look-notes-button"))).click();

        signWithPassword(localWait, ConfigReader.get("password_doctor"));

        // Esperar a que el formulario cierre (el diálogo desaparece tras la firma)
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("look-notes-button")));

        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));

        Reporter.log("TC8 OK — Transfusión registrada.");
    }


    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * Selecciona la fila de la tabla cuyo código de solicitud coincide con el dado.
     * Necesario porque en cada ejecución se crean nuevas solicitudes y gridId-0
     * puede no corresponder a la solicitud creada en este test.
     *
     * @param localWait    WebDriverWait activo
     * @param code         Código de solicitud a buscar (e.g. "260529000004")
     */
    private void selectRowByRequestCode(WebDriverWait localWait, String code) {
        localWait.until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("[id^='grid-'][id$='-request_code']"));
            for (WebElement cell : rows) {
                if (code.equals(cell.getText().trim())) {
                    // Extraer el índice de fila del ID (grid-gridId-N-request_code)
                    String rowId = cell.getAttribute("id")
                            .replace("-request_code", "")
                            .replace("grid-", "");
                    d.findElement(By.id(rowId)).click();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Abre un mat-select por ID y selecciona una opción por ID usando clicks nativos.
     * Necesario en formularios con cdk-overlay donde arguments[0].click() (JS click)
     * no funciona — es el caso del formulario de hemoderivados.
     *
     * @param localWait  WebDriverWait activo
     * @param selectId   ID del mat-select
     * @param optionId   ID de la opción a seleccionar
     */
    /**
     * Expande todos los mat-expansion-panel que estén colapsados en el DOM,
     * uno a uno con espera entre cada uno para que Angular procese la animación.
     * Robusto ante IDs dinámicos y estructura variable del acordeón.
     */
    private void expandAllCollapsedPanels() {
        List<WebElement> collapsed = driver.findElements(
                By.cssSelector("mat-expansion-panel-header[aria-expanded='false']"));
        for (WebElement header : collapsed) {
            header.click();
            // Esperar a que ese panel quede expandido antes de pasar al siguiente
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    "true".equals(header.getAttribute("aria-expanded")));
        }
    }

    private void clickSelectNative(WebDriverWait localWait, String selectId, String optionId) {
        // 1. Abrir el mat-select con click nativo
        localWait.until(ExpectedConditions.elementToBeClickable(By.id(selectId))).click();

        // 2. Esperar a que el panel esté VISIBLE — la opción debe ser visible en pantalla,
        //    no solo presente en el DOM (las opciones persisten en el DOM entre aperturas).
        localWait.until(ExpectedConditions.visibilityOfElementLocated(By.id(optionId)));

        // 3. Seleccionar la opción con click nativo
        driver.findElement(By.id(optionId)).click();

        // 4. Esperar a que el panel se cierre (opción ya no visible)
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(By.id(optionId)));
    }

    /**
     * Rellena el control de seguridad (modal previo a extracción y transfusión).
     * Los 3 campos reciben el código de solicitud.
     *
     * @param localWait   WebDriverWait activo
     * @param code        Código de solicitud (requestCode)
     */
    private void fillSecurityControl(WebDriverWait localWait, String code) {
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("requestCode")));
        driver.findElement(By.id("requestCode")).sendKeys(code);
        driver.findElement(By.id("confirmRequestCode")).sendKeys(code);
        driver.findElement(By.id("transfusionSecurityNumber")).sendKeys(code);
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SecurityControlContainer-button"))).click();
    }

    /**
     * Rellena el modal de firma con la contraseña dada.
     *
     * @param localWait  WebDriverWait activo
     * @param password   Contraseña del usuario
     */
    private void signWithPassword(WebDriverWait localWait, String password) {
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));
        driver.findElement(By.id("password")).sendKeys(password);
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-UserSignComponent-button"))).click();
        // Esperar a que el modal de firma desaparezca con timeout extendido —
        // el servidor puede tardar en procesar la firma
        new WebDriverWait(driver, LONG_TIMEOUT).until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("sign-UserSignComponent-button")));
    }

    /**
     * Rellena un campo de fecha usando el setter nativo del DOM para evitar
     * que Angular concatene el valor al existente cuando se usa sendKeys.
     *
     * @param fieldId  ID del campo input de fecha
     * @param value    Valor en formato dd/MM/yyyy
     */
    private void setValueNative(String fieldId, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('" + fieldId + "');" +
                        "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(el, '" + value + "');" +
                        "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles: true}));");
    }

    /**
     * Localiza un campo input por el texto de su label dentro de un mat-form-field
     * y le envía el valor. Útil para campos cuyo ID es dinámico (mat-input-N).
     *
     * @param labelText  Texto exacto de la label (e.g. "Hora *", "Hora inicio *")
     * @param value      Valor a introducir
     */
    /**
     * Localiza un input por el texto exacto de su mat-label y le envía el valor.
     * Útil para campos con ID dinámico (mat-input-N).
     * Nota: el asterisco (*) de obligatoriedad no forma parte del mat-label en el DOM.
     *
     * @param labelText  Texto del mat-label sin asterisco (e.g. "Hora", "Hora inicio")
     * @param value      Valor a introducir
     */
    private void fillInputByLabel(String labelText, String value) {
        WebElement input = new WebDriverWait(driver, TIMEOUT).until(
                ExpectedConditions.elementToBeClickable(By.xpath(
                        "//mat-form-field[.//mat-label[normalize-space(.)='" + labelText + "']]//input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", input);
        input.sendKeys(value);
    }
}
