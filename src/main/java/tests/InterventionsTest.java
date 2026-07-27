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
 * Test de integración: Intervenciones Quirúrgicas
 *
 * Estrategia de independencia (Opción B):
 *   - TC2 y TC3 dependen solo de TC1.
 *   - Si se lanza el suite completo: TC1 → TC2 → TC3 (por priority).
 *   - Si se lanza solo TC3: TC1 → TC3. TC3 detecta si hay propuesta
 *     en el grid y si no la crea automáticamente.
 *
 * Flujo:
 *   TC1 — Login + navegar a Intervenciones del pacienteQX
 *   TC2 — Crear propuesta → verde → eliminar → recrear → verde
 *   TC3 — Preanestesia: indicación APTO + firma médico → verde
 *
 * ── Paciente ─────────────────────────────────────────────────────────────────
 *   pacienteQX → ConfigReader.get("pacqah1NH")
 *
 * ── IDs navegación ───────────────────────────────────────────────────────────
 *   hospitalization-sidebar / worklist-sidebar / filter-input
 *   grid-gridId-{N}-patient  actions-button → see_history
 *   interventions-sidebar    actions-button-interventions
 *
 * ── IDs Submenús (doble JS click en el mismo script) ─────────────────────────
 *   intervention_proposal → new_proposal / delete_proposal
 *   pre_anesthesia → pre-anesthesia_of_the_intervention
 *
 * ── IDs Formulario propuesta ──────────────────────────────────────────────────
 *   speciality (autocomplete) → speciality-0 = Dermatologia
 *   interventionType (mat-select) → interventionType-0 = Cirugía ambulatoria
 *   intervention-proposal-intervention-priority → -2 = Urgente
 *   show-catalog-button (primer visible con texto) → catálogo diagnóstico
 *     input-default-id, tree-checkbox-initial-D00296-3,
 *     angle-right-button, tree-checkbox-final-D00296-2,
 *     accept-DiagnosesCatalogContainer-button
 *   grid-diagnoses-0-code → diagnóstico añadido automáticamente al grid
 *   surgeryDurationTime → execCommand "1h"
 *   predictedTechniques → textarea nativeSetter
 *   Campo "Hora" (sin ID, label = "Hora") → execCommand "23:59"
 *   accept-InterventionProposalContainer-button
 *
 * ── IDs Grid intervenciones ───────────────────────────────────────────────────
 *   grid-interventions-0-code     → la nueva propuesta aparece SIEMPRE en idx 0
 *   grid-interventions-0-in_prop  → círculo Prop.Int
 *   grid-interventions-0-prea     → círculo PreA
 *   Verde = mat-icon con clases "text-success" y "fg-solid"
 *
 * ── IDs Eliminación propuesta ─────────────────────────────────────────────────
 *   cause (mat-select) → cause-7 = Solicitud errónea
 *   accept-SurgicalDocumentsDeleteComponent-button
 *
 * ── IDs Preanestesia ──────────────────────────────────────────────────────────
 *   state (mat-select) → state-0 = APTO
 *   img[src*="shield"] → escudo naranja, click abre modal de firma
 *   Modal firma: password / sign-UserSignComponent-button
 *   accept-PreanesthesiaContainer-button → guarda el formulario tras firmar
 */
public class InterventionsTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // Datos de la propuesta
    private static final String SPECIALITY     = "Dermatologia";
    private static final String DIAG_SEARCH    = "1C10.3";
    private static final String DIAG_INITIAL   = "tree-checkbox-initial-D00296-3";
    private static final String DIAG_FINAL     = "tree-checkbox-final-D00296-2";
    private static final String DURATION       = "1h";
    private static final String PROCEDURE_TEXT = "Procedimiento previsto de prueba automatizado";
    private static final String HORA_PREVISTA  = "23:59";

    // =========================================================================
    // TC1 — LOGIN + NAVEGAR A INTERVENCIONES
    // =========================================================================
    @Test(priority = 1)
    public void TC1_LoginAndNavigate() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_doctor"));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("password"))).sendKeys(ConfigReader.get("password_doctor"));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("defaultButtonId")))
                    .click();
        } catch (TimeoutException ignored) {}

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("filter-input")));

        String pacienteQX = ConfigReader.get("pacqah1NH");
        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(pacienteQX);

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.id("load-all-information"))).click();
        } catch (TimeoutException ignored) {}

        WebElement patientCell = localWait.until(d -> {
            List<WebElement> cells = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-patient']"));
            return cells.isEmpty() ? null : cells.get(0);
        });
        patientCell.click();

        openActionsMenuAndClick(localWait, "see_history");
        SelectAccessReason("Guardia");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());
        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        // Cerrar backdrops y navegar a Intervenciones
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.mat-drawer-backdrop,.cdk-overlay-backdrop')" +
                        ".forEach(b=>b.click());");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("interventions-sidebar")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('interventions-sidebar').click();");

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button-interventions")));

        Reporter.log("TC1 OK — Intervenciones cargado. Paciente: " + pacienteQX);
    }

    // =========================================================================
    // TC2 — CREAR PROPUESTA → VERDE → ELIMINAR → RECREAR → VERDE
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC2_CreateAndDeleteProposal() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // ── Primera creación ──────────────────────────────────────────────────
        createProposal(localWait);
        assertCircleGreen(localWait, "grid-interventions-0-in_prop",
                "TC2 — Prop.Int debe ser verde tras crear la propuesta");

        // ── Eliminar ──────────────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("intervention_proposal")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('intervention_proposal').click();" +
                        "document.getElementById('delete_proposal').click();");

        // Modal "Datos quirúrgicos" — cause-7 = Solicitud errónea
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SurgicalDocumentsDeleteComponent-button")));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cause"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cause-7"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SurgicalDocumentsDeleteComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-SurgicalDocumentsDeleteComponent-button")));

        Reporter.log("TC2 — Propuesta creada, verde verificado y eliminada.");

        // ── Recrear la misma propuesta ────────────────────────────────────────
        createProposal(localWait);
        assertCircleGreen(localWait, "grid-interventions-0-in_prop",
                "TC2 — Prop.Int debe ser verde en la propuesta recreada");

        Reporter.log("TC2 OK — Propuesta recreada y verde verificado.");
    }

    // =========================================================================
    // TC3 — PREANESTESIA: INDICACIÓN APTO + FIRMA MÉDICO → VERDE
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC3_Preanesthesia() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Si se lanza TC3 solo (sin TC2), la propuesta puede no existir
        if (driver.findElements(By.id("grid-interventions-0-code")).isEmpty()) {
            Reporter.log("INFO — No hay propuesta en el grid. Creando...");
            createProposal(localWait);
        }

        // Seleccionar la fila 0 (la nueva propuesta siempre aparece en idx 0)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir preanestesia ────────────────────────────────────────────────
        // pre_anesthesia es submenu trigger — doble JS click en el mismo script
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("pre_anesthesia")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('pre_anesthesia').click();" +
                        "document.getElementById('pre-anesthesia_of_the_intervention').click();");

        // El formulario puede tener secciones colapsadas (mat-expansion-panel).
        // Si state no es visible, expandir "Datos de la anestesia"
        localWait.until(d -> {
            WebElement stateEl = d.findElement(By.id("state"));
            if (stateEl.isDisplayed()) return true;
            // Expandir el panel que contiene state
            ((JavascriptExecutor) driver).executeScript(
                    "var panels = document.querySelectorAll('mat-expansion-panel-header');" +
                            "Array.from(panels).forEach(function(p) {" +
                            "    if (p.textContent.includes('Datos de la anestesia') || " +
                            "        p.textContent.includes('anestesia') || " +
                            "        p.textContent.includes('Indicaci')) {" +
                            "        var expanded = p.getAttribute('aria-expanded');" +
                            "        if (expanded === 'false') p.click();" +
                            "    }" +
                            "});");
            return null;
        });

        // ── Indicación → APTO ─────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("state"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("state-0"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Firmar con el escudo ──────────────────────────────────────────────
        // La firma guarda y cierra el formulario automáticamente — no hay que
        // pulsar Aceptar después
        localWait.until(d ->
                d.findElements(By.cssSelector("img[src*='shield']")).stream()
                        .filter(WebElement::isDisplayed).findFirst().orElse(null)
        ).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("password"))).sendKeys("123456");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-UserSignComponent-button"))).click();

        // Esperar que el formulario se cierre (la firma guarda automáticamente)
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("sign-UserSignComponent-button")));
        localWait.until(d -> d.findElements(
                By.cssSelector("mat-dialog-container")).isEmpty());

        // ── Verificar círculo PreA verde ──────────────────────────────────────
        assertCircleGreen(localWait, "grid-interventions-0-prea",
                "TC3 — PreA debe ser verde tras firmar");

        Reporter.log("TC3 OK — Preanestesia firmada (APTO), PreA verde verificado.");
    }


    // =========================================================================
    // TC4 — ANESTESIA: FECHAS/HORAS/TIPO + FIRMA → VERDE
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC4_Anesthesia() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Si no hay propuesta, crearla
        if (driver.findElements(By.id("grid-interventions-0-code")).isEmpty()) {
            Reporter.log("INFO — No hay propuesta. Creando...");
            createProposal(localWait);
        }

        // Seleccionar la fila 0
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir anestesia ───────────────────────────────────────────────────
        // anesthesia es ítem directo (no submenú)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("anesthesia"))).click();

        // Esperar formulario y expandir todas las secciones colapsadas
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("anesthesiaStartTime")));
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // ── Fechas inicio y fin con nativeSetter ──────────────────────────────
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        fillDateById("anesthesiaStartTime", today);
        fillDateById("endTimeAnesthesia", today);

        // ── Horas y duración por label con execCommand ────────────────────────
        // Horas anteriores a la actual: 09:00 inicio, 09:15 fin
        fillInputByLabel(localWait, "Hora inicio *", "09:00");
        fillInputByLabel(localWait, "Hora fin *",    "09:15");
        fillInputByLabel(localWait, "Duración",       "00:15");

        // ── Tipo de anestesia → General ───────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("anesthesiaType"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("anesthesiaType-2"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Firmar ────────────────────────────────────────────────────────────
        // La firma guarda automáticamente — el formulario se cierra solo
        // y el botón Aceptar pasa a ser Cerrar (cancel-AnesthesiaReportContainer-button)
        localWait.until(d ->
                d.findElements(By.cssSelector("img[src*='shield']")).stream()
                        .filter(WebElement::isDisplayed).findFirst().orElse(null)
        ).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("password"))).sendKeys("123456");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-UserSignComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("sign-UserSignComponent-button")));

        // Cerrar el formulario ya firmado
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AnesthesiaReportContainer-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AnesthesiaReportContainer-button")));

        // ── Verificar círculo Anes verde ──────────────────────────────────────
        assertCircleGreen(localWait, "grid-interventions-0-anes",
                "TC4 — Anes debe ser verde tras firmar");

        Reporter.log("TC4 OK — Anestesia firmada, Anes verde verificado.");
    }

    // =========================================================================
    // TC5 — SEGURIDAD QX: PRIMERA PREGUNTA SÍ + GUARDAR BORRADOR (NARANJA)
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC5_SecurityQx() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Si no hay propuesta, crearla
        if (driver.findElements(By.id("grid-interventions-0-code")).isEmpty()) {
            Reporter.log("INFO — No hay propuesta. Creando...");
            createProposal(localWait);
        }

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir Seguridad Qx ────────────────────────────────────────────────
        // security_qx es ítem directo (no submenú)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("security_qx"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-SurgicalSafetyContainer-button")));

        // ── Primera pregunta → Sí ─────────────────────────────────────────────
        // IDs de los botones: {orden}-true-button / {orden}-false-button / {orden}-not-applicable-button
        // La primera pregunta tiene prefijo "10-"
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("10-true-button"))).click();

        // ── Aceptar (guardar como borrador, sin firmar) ───────────────────────
        // El borrador se muestra en naranja (text-warning fg-solid)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SurgicalSafetyContainer-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-SurgicalSafetyContainer-button")));

        // Seg.Qx es borrador → naranja (text-warning), no verde — no se verifica verde
        Reporter.log("TC5 OK — Seguridad Qx guardada como borrador (naranja esperado).");
    }

    // =========================================================================
    // TC6 — INFORME QX: CIRUJANO + PROCEDIMIENTO + DX POSTOP + FIRMA → VERDE
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC6_QxReport() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        if (driver.findElements(By.id("grid-interventions-0-code")).isEmpty()) {
            Reporter.log("INFO — No hay propuesta. Creando...");
            createProposal(localWait);
        }

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir Informe Qx ──────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("qx_report"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("user-autocomplete-default-id")));

        // ── Cirujano (autocomplete) ───────────────────────────────────────────
        // user-autocomplete-default-id → opción user-autocomplete-default-id-0
        WebElement cirujanoInput = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("user-autocomplete-default-id")));
        cirujanoInput.clear();
        cirujanoInput.sendKeys("Doctor");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("user-autocomplete-default-id-0"))).click();
        localWait.until(d -> d.findElements(
                        By.cssSelector(".mat-autocomplete-panel")).stream()
                .noneMatch(WebElement::isDisplayed));

        // ── Procedimiento desde catálogo OMC ─────────────────────────────────
        // Primer show-catalog-button visible con texto "Catálogo" = procedimiento
        // Catálogo: "Catálogo de procedimientos OMC"
        //   Buscar "Queloide" → expandir nodo padre (G0) → seleccionar tree-checkbox-initial-131062640-1
        //   Mover con angle-right-button → seleccionar tree-checkbox-final-131062640-1
        //   Aceptar: accept-OMCProceduresCatalogContainer-button
        //   El procedimiento se añade automáticamente al grid omc
        localWait.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            WebElement first = btns.stream()
                    .filter(b -> b.isDisplayed() && "Catálogo".equals(b.getText().trim()))
                    .findFirst().orElse(null);
            if (first == null) return null;
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", first);
            return true;
        });

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("input-default-id"))).sendKeys("Queloide");

        // Expandir el nodo padre G0
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("expanded-node-button"))).click();

        // Seleccionar el nodo hoja en árbol izquierdo
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("tree-checkbox-initial-131062640-1"))).click();

        // Mover al árbol derecho
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("angle-right-button"))).click();

        // Marcar nodo en árbol derecho
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("tree-checkbox-final-131062640-1"))).click();

        // Aceptar catálogo OMC
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-OMCProceduresCatalogContainer-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-OMCProceduresCatalogContainer-button")));

        // Cerrar cualquier dropdown que se abra tras aceptar el catálogo
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(b=>b.click());");

        // Verificar que el procedimiento se añadió al grid omc
        localWait.until(d -> {
            WebElement grid = d.findElement(By.id("omc"));
            return grid.getText().contains("131062");
        });

        // ── Diagnóstico postoperatorio desde catálogo ─────────────────────────
        // Segundo show-catalog-button visible con texto "Catálogo" = diagnóstico
        // Mismo catálogo de diagnósticos CIE que en la propuesta: 1C10.3
        localWait.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            List<WebElement> visible = btns.stream()
                    .filter(b -> b.isDisplayed() && "Catálogo".equals(b.getText().trim()))
                    .collect(java.util.stream.Collectors.toList());
            if (visible.size() < 2) return null;
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();", visible.get(1));
            return true;
        });

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("input-default-id"))).sendKeys(DIAG_SEARCH);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id(DIAG_INITIAL))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("angle-right-button"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id(DIAG_FINAL))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-DiagnosesCatalogContainer-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-DiagnosesCatalogContainer-button")));

        // Verificar diagnóstico en grid
        localWait.until(d -> {
            WebElement grid = d.findElement(By.id("diagnoses"));
            return grid.getText().contains("1C10.3");
        });

        // ── Firmar ────────────────────────────────────────────────────────────
        // La firma guarda y cierra automáticamente
        localWait.until(d ->
                d.findElements(By.cssSelector("img[src*='shield']")).stream()
                        .filter(WebElement::isDisplayed).findFirst().orElse(null)
        ).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("password"))).sendKeys("123456");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-UserSignComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("sign-UserSignComponent-button")));

        // Cerrar dropdown si se abre tras la firma
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(b=>b.click());");

        // Cerrar formulario (el botón Cancelar pasa a ser Cerrar tras firmar)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-SurgicalReportContainer-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-SurgicalReportContainer-button")));

        // ── Verificar círculo I.Qx verde ──────────────────────────────────────
        assertCircleGreen(localWait, "grid-interventions-0-qx_r",
                "TC6 — I.Qx debe ser verde tras firmar");

        Reporter.log("TC6 OK — Informe Qx firmado, I.Qx verde verificado.");
    }

    // =========================================================================
    // TC7 — REGISTRO QX: CAMPOS OBLIGATORIOS + FECHAS/HORAS → VERDE
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC7_QxRegister() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        if (driver.findElements(By.id("grid-interventions-0-code")).isEmpty()) {
            Reporter.log("INFO — No hay propuesta. Creando...");
            createProposal(localWait);
        }

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir Registro Qx ─────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("qx_register"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-SurgicalRecordComponent-button")));

        // ── Expandir todas las secciones colapsadas ───────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // ── Esperar a que el campo Servicio cargue antes de rellenar los dependientes
        // interventionType y surgicalNumberSurgical se resetean si se eligen antes
        // de que el servicio haya cargado su valor
        localWait.until(d -> {
            String svcVal = (String) ((JavascriptExecutor) driver).executeScript(
                    "var el = document.getElementById('service');" +
                            "return el ? el.value : '';");
            return svcVal != null && !svcVal.trim().isEmpty();
        });

        // ── Tipo de intervención * (autocomplete) → Programada ────────────────
        // Abrir el panel con click en .mat-select-arrow-wrapper (sin sendKeys)
        // interventionType-0 = Programada
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('interventionType')" +
                        ".closest('mat-form-field')" +
                        ".querySelector('.mat-select-arrow-wrapper').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("interventionType-0"))).click();
        localWait.until(d -> !d.findElement(By.id("interventionType"))
                .getAttribute("value").trim().isEmpty());

        // ── Quirófano * (mat-select) → QX-1 ──────────────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('surgicalNumberSurgical').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("surgicalNumberSurgical-0"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Requiere anestesia (mat-select) → Sí ─────────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('needsAnesthesistSurgical').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("needsAnesthesistSurgical-0"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Fechas (nativeSetter) ─────────────────────────────────────────────
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        fillDateById("operatingRoomEntryDate",     today);
        fillDateById("interventionStartDate",      today);
        fillDateById("interventionEndDate",        today);
        fillDateById("operatingRoomDepartureDate", today);

        // ── Horas (execCommand) — anteriores a la hora actual ─────────────────
        fillTimeById("operatingRoomEntryTime",     "08:00");
        fillTimeById("interventionStartTime",      "08:05");
        fillTimeById("interventionEndTime",        "09:00");
        fillTimeById("operatingRoomDepartureTime", "09:05");

        // ── Aceptar ───────────────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SurgicalRecordComponent-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-SurgicalRecordComponent-button")));

        // ── Verificar círculo R.Qx verde ──────────────────────────────────────
        assertCircleGreen(localWait, "grid-interventions-0-qx_re",
                "TC7 — R.Qx debe ser verde");

        Reporter.log("TC7 OK — Registro Qx guardado, R.Qx verde verificado.");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Crea la propuesta de intervención completa.
     * La nueva propuesta siempre aparece en el índice 0 del grid.
     */
    private void createProposal(WebDriverWait w) {
        // Abrir formulario — doble JS click en el mismo script
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("intervention_proposal")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('intervention_proposal').click();" +
                        "document.getElementById('new_proposal').click();");

        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-InterventionProposalContainer-button")));

        // Servicio → Dermatologia
        WebElement specialityInput = w.until(
                ExpectedConditions.elementToBeClickable(By.id("speciality")));
        specialityInput.click();
        specialityInput.sendKeys(SPECIALITY);
        // Esperar que speciality-0 sea exactamente SPECIALITY — el primer resultado
        // puede variar según el servicio por defecto del médico (ej. "Admisión")
        w.until(d -> {
            List<WebElement> opts = d.findElements(By.id("speciality-0"));
            if (opts.isEmpty() || !opts.get(0).isDisplayed()) return null;
            if (!opts.get(0).getText().trim().equals(SPECIALITY)) return null;
            return opts.get(0);
        }).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(By.id("speciality-0")));

        // Tipo → Cirugía ambulatoria
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("interventionType"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("interventionType-0"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Prioridad → Urgente
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("intervention-proposal-intervention-priority"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("intervention-proposal-intervention-priority-2"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Diagnóstico desde catálogo (primer show-catalog-button visible con texto)
        w.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            WebElement visible = btns.stream()
                    .filter(b -> b.isDisplayed() && !b.getText().trim().isEmpty())
                    .findFirst().orElse(null);
            if (visible == null) return null;
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", visible);
            return true;
        });

        w.until(ExpectedConditions.elementToBeClickable(
                By.id("input-default-id"))).sendKeys(DIAG_SEARCH);
        w.until(ExpectedConditions.elementToBeClickable(By.id(DIAG_INITIAL))).click();
        w.until(ExpectedConditions.elementToBeClickable(By.id("angle-right-button"))).click();
        w.until(ExpectedConditions.presenceOfElementLocated(By.id(DIAG_FINAL))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-DiagnosesCatalogContainer-button"))).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-DiagnosesCatalogContainer-button")));
        // El catálogo añade el diagnóstico directamente al grid
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("grid-diagnoses-0-code")));

        // Duración → "1h" (execCommand — nativeSetter no funciona aquí)
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("surgeryDurationTime")));
        ((JavascriptExecutor) driver).executeScript(
                "var f = document.getElementById('surgeryDurationTime');" +
                        "f.focus(); f.select();" +
                        "document.execCommand('selectAll');" +
                        "document.execCommand('delete');" +
                        "document.execCommand('insertText', false, '" + DURATION + "');" +
                        "f.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "f.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "f.blur();");

        // Procedimiento previsto (textarea, nativeSetter)
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("predictedTechniques")));
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('predictedTechniques');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLTextAreaElement.prototype, 'value').set;" +
                        "setter.call(el, '" + PROCEDURE_TEXT + "');" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));");

        // Hora prevista (input sin ID fijo, label = "Hora", execCommand)
        w.until(d -> {
            Boolean done = (Boolean) ((JavascriptExecutor) driver).executeScript(
                    "var ffs = Array.from(document.querySelectorAll(" +
                            "    'mat-dialog-container mat-form-field'));" +
                            "var ff = ffs.find(function(f){" +
                            "    var l = f.querySelector('label');" +
                            "    return l && l.textContent.trim() === 'Hora';" +
                            "});" +
                            "if (!ff) return false;" +
                            "var inp = ff.querySelector('input');" +
                            "if (!inp) return false;" +
                            "inp.focus(); inp.select();" +
                            "document.execCommand('selectAll');" +
                            "document.execCommand('delete');" +
                            "document.execCommand('insertText', false, '" + HORA_PREVISTA + "');" +
                            "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                            "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                            "inp.blur();" +
                            "return true;");
            return Boolean.TRUE.equals(done);
        });

        // Aceptar y esperar proceso
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-InterventionProposalContainer-button"))).click();
        waitForHeavyProcess(w);
    }

    /**
     * Espera a que el HeavyProcessLoader finalice y todos los dialogs se cierren.
     */
    private void waitForHeavyProcess(WebDriverWait w) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.id("close-HeavyProcessLoaderDialogComponent-button")));
            new WebDriverWait(driver, LONG_TIMEOUT)
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.id("close-HeavyProcessLoaderDialogComponent-button")));
        } catch (TimeoutException ignored) {}
        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(d -> d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());
    }

    /**
     * Rellena un input de hora por ID con execCommand.
     * Necesario para campos de hora con validadores de formato.
     */
    private void fillTimeById(String fieldId, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var f = document.getElementById('" + fieldId + "');" +
                        "if (!f) return;" +
                        "f.focus(); f.select();" +
                        "document.execCommand('selectAll');" +
                        "document.execCommand('delete');" +
                        "document.execCommand('insertText', false, '" + value + "');" +
                        "f.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "f.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "f.blur();");
    }

    /**
     * Rellena un input de fecha por ID con nativeSetter.
     */
    private void fillDateById(String fieldId, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('" + fieldId + "');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(el, '" + value + "');" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));");
    }

    /**
     * Rellena un input localizado por el texto exacto de su label.
     * Usa execCommand para que Angular detecte el cambio correctamente.
     */
    private void fillInputByLabel(WebDriverWait w, String labelText, String value) {
        w.until(d -> {
            Boolean done = (Boolean) ((JavascriptExecutor) driver).executeScript(
                    "var ffs = Array.from(document.querySelectorAll(" +
                            "    'mat-dialog-container mat-form-field'));" +
                            "var ff = ffs.find(function(f){" +
                            "    var l = f.querySelector('label');" +
                            "    return l && l.textContent.trim() === '" +
                            labelText.replace("'", "\'") + "';" +
                            "});" +
                            "if (!ff) return false;" +
                            "var inp = ff.querySelector('input');" +
                            "if (!inp) return false;" +
                            "inp.focus(); inp.select();" +
                            "document.execCommand('selectAll');" +
                            "document.execCommand('delete');" +
                            "document.execCommand('insertText', false, '" + value + "');" +
                            "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                            "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                            "inp.blur();" +
                            "return true;");
            return Boolean.TRUE.equals(done);
        });
    }

    /**
     * Verifica que el círculo de estado de una columna del grid es verde.
     * Verde = mat-icon con clases "text-success" y "fg-solid".
     */
    private void assertCircleGreen(WebDriverWait w, String cellId, String message) {
        w.until(d -> {
            List<WebElement> cells = d.findElements(By.id(cellId));
            if (cells.isEmpty()) return false;
            return cells.get(0).findElements(By.cssSelector("mat-icon")).stream()
                    .anyMatch(icon -> {
                        String cls = icon.getAttribute("class");
                        return cls.contains("text-success") && cls.contains("fg-solid");
                    });
        });
        Assert.assertTrue(true, message);
    }
}
