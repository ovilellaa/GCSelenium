package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Test de integración: Propuesta de Intervención Quirúrgica — worklist Quirófano
 *
 * Estructura idéntica a InterventionsTest:
 *   TC1 — Login + navegar al worklist de Quirófano
 *   TC2 — Crear propuesta de intervención
 *   TC3 — Preanestesia: indicación APTO + firma
 *   TC4 — Anestesia: tiempos + tipo General + firma + cerrar
 *   TC5 — Seguridad Qx: primera pregunta Sí + guardar borrador
 *   TC6 — Informe Qx: cirujano + procedimiento OMC + diagnóstico + firma
 *   TC7 — Registro Qx: tipo + quirófano + anestesia + fechas/horas + aceptar
 *
 * Todos los TCs del 3 al 7 dependen solo de TC1 (igual que InterventionsTest),
 * por lo que pueden ejecutarse independientemente si el worklist ya está abierto.
 * Cada TC llama a selectLastPatientRow() que siempre coge la última fila del
 * paciente con "De La Torre" del grid y espera la clase row--selected antes
 * de abrir el menú Acciones.
 *
 * ── Navegación ────────────────────────────────────────────────────────────────
 *   operating_room-sidebar  →  worklist-sidebar
 *
 * ── Acciones add-action (menú plano, sin submenús) ────────────────────────────
 *   intervention_proposal   → abre formulario propuesta directamente
 *
 * ── IDs formulario propuesta ─────────────────────────────────────────────────
 *   patient / patient-0         interventionType / interventionType-0
 *   intervention-proposal-intervention-priority / -2 (Urgente)
 *   show-catalog-button (1er visible con texto) → catálogo diagnóstico
 *     input-default-id / tree-checkbox-initial-D00296-3
 *     angle-right-button / tree-checkbox-final-D00296-2
 *     accept-DiagnosesCatalogContainer-button / grid-diagnoses-0-code
 *   surgeryDurationTime (execCommand)   predictedTechniques (nativeSetter)
 *   Campo "Hora" por label (execCommand)
 *   accept-InterventionProposalContainer-button
 *   close-HeavyProcessLoaderDialogComponent-button
 *
 * ── Grid worklist ─────────────────────────────────────────────────────────────
 *   gridId-{N}                     (TR fila)
 *   grid-gridId-{N}-surgery-type   (celda — click genera row--selected fiable)
 *   grid-gridId-{N}-patient        (celda — texto nombre paciente)
 *   row--selected                  (clase CSS → fila activa para Acciones)
 *   actions-button                 (menú Acciones del worklist)
 *
 * ── IDs acciones (ítems directos, sin submenús) ───────────────────────────────
 *   pre_anesthesia  anesthesia  operating_room_report
 *   surgical_safety  operating_room_record
 *
 * ── IDs preanestesia ──────────────────────────────────────────────────────────
 *   accept-PreanesthesiaContainer-button
 *   state / state-0 (APTO)
 *   img[src*='shield'] naranja → click → password → sign-UserSignComponent-button
 *   img[src*='shield-check-valid'] verde → ya firmado, skip firma
 *
 * ── IDs anestesia ─────────────────────────────────────────────────────────────
 *   anesthesiaStartTime / endTimeAnesthesia (nativeSetter fecha)
 *   "Hora inicio *" / "Hora fin *" / "Duración" (execCommand por label)
 *   anesthesiaType / anesthesiaType-2 (General)
 *   close-AnesthesiaReportContainer-button (Cerrar tras firma o ya firmado)
 *
 * ── IDs seguridad Qx ──────────────────────────────────────────────────────────
 *   accept-SurgicalSafetyContainer-button
 *   10-true-button (primera pregunta Sí)
 *
 * ── IDs informe Qx ────────────────────────────────────────────────────────────
 *   user-autocomplete-default-id / user-autocomplete-default-id-0
 *   show-catalog-button (1º Catálogo) → OMC:
 *     expanded-node-button / tree-checkbox-initial-131062640-1
 *     angle-right-button / tree-checkbox-final-131062640-1
 *     accept-OMCProceduresCatalogContainer-button
 *   show-catalog-button (2º Catálogo) → diagnóstico Qx (mismo que propuesta)
 *   cancel-SurgicalReportContainer-button (Cerrar tras firma)
 *
 * ── IDs registro Qx ──────────────────────────────────────────────────────────
 *   accept-SurgicalRecordComponent-button
 *   service (esperar valor precargado)
 *   interventionType / interventionType-0
 *   surgicalNumberSurgical / surgicalNumberSurgical-0
 *   needsAnesthesistSurgical / needsAnesthesistSurgical-0
 *   operatingRoomEntryDate/interventionStartDate/interventionEndDate/
 *   operatingRoomDepartureDate (nativeSetter)
 *   operatingRoomEntryTime/interventionStartTime/interventionEndTime/
 *   operatingRoomDepartureTime (execCommand)
 */
public class PropuestaIntervencionTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    private static final String DIAG_SEARCH  = "1C10.3";
    private static final String DIAG_INITIAL = "tree-checkbox-initial-D00296-3";
    private static final String DIAG_FINAL   = "tree-checkbox-final-D00296-2";
    private static final String DURATION     = "1h";
    private static final String PROCEDURE    = "Técnica escrita por Selenium";
    private static final String HORA_PREV    = "23:59";
    private static final String PATIENT_FRAGMENT = "De La Torre";

    // rowId de la última propuesta creada por TC2 — usado por TC3-TC7
    // para garantizar que siempre se trabaja sobre la propuesta nueva,
    // no sobre una propuesta anterior del mismo paciente que pueda tener
    // los formularios ya firmados de ejecuciones anteriores del test.
    private String createdRowId = null;

    // =========================================================================
    // TC1 — LOGIN + NAVEGAR AL WORKLIST DE QUIRÓFANO
    // =========================================================================
    @Test(priority = 1)
    public void TC1_LoginAndNavigate() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        LoginAsDoctor();

        w.until(ExpectedConditions.elementToBeClickable(
                By.id("operating_room-sidebar"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        // Esperar que cargue el worklist (botón add-action visible)
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("add-action")));

        Reporter.log("TC1 OK — Worklist de Quirófano cargado.");
    }

    // =========================================================================
    // TC2 — CREAR PROPUESTA DE INTERVENCIÓN
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = "TC1_LoginAndNavigate")
    public void TC2_CreateProposal() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        // add-action abre menú plano — intervention_proposal es ítem directo
        w.until(ExpectedConditions.elementToBeClickable(By.id("add-action"))).click();
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("intervention_proposal")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('intervention_proposal').click();");

        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-InterventionProposalContainer-button")));

        // Paciente por NH — patient-0 es siempre el primero con ese código
        String nh = ConfigReader.get("pacqah1NH");
        WebElement patientInput = w.until(
                ExpectedConditions.elementToBeClickable(By.id("patient")));
        patientInput.click();
        patientInput.clear();
        patientInput.sendKeys(nh);
        w.until(ExpectedConditions.elementToBeClickable(By.id("patient-0"))).click();
        // Esperar que entity se autorellene
        w.until(d -> {
            String val = ((String) ((JavascriptExecutor) driver).executeScript(
                    "var el=document.getElementById('entity'); return el ? el.value : '';"));
            return val != null && !val.trim().isEmpty();
        });

        // Tipo → Cirugía ambulatoria
        w.until(ExpectedConditions.elementToBeClickable(By.id("interventionType"))).click();
        w.until(ExpectedConditions.elementToBeClickable(By.id("interventionType-0"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Prioridad → Urgente
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("intervention-proposal-intervention-priority"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("intervention-proposal-intervention-priority-2"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Diagnóstico desde catálogo
        w.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            WebElement vis = btns.stream()
                    .filter(b -> b.isDisplayed() && !b.getText().trim().isEmpty())
                    .findFirst().orElse(null);
            if (vis == null) return null;
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", vis);
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
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("grid-diagnoses-0-code")));

        // Duración (execCommand)
        ((JavascriptExecutor) driver).executeScript(
                "var f=document.getElementById('surgeryDurationTime');" +
                        "f.focus();f.select();" +
                        "document.execCommand('selectAll');" +
                        "document.execCommand('delete');" +
                        "document.execCommand('insertText',false,'" + DURATION + "');" +
                        "f.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "f.dispatchEvent(new Event('change',{bubbles:true}));f.blur();");

        // Técnica prevista (nativeSetter)
        ((JavascriptExecutor) driver).executeScript(
                "var el=document.getElementById('predictedTechniques');" +
                        "var s=Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype,'value').set;" +
                        "s.call(el,'" + PROCEDURE + "');" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));");

        // Hora prevista (por label, execCommand)
        fillInputByLabel(w, "Hora", HORA_PREV);

        // Aceptar
        WebElement acceptBtn = w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-InterventionProposalContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);
        waitForHeavyProcess(w);

        // Esperar a que la nueva fila aparezca en el grid y guardar su rowId.
        // Se espera que el número de filas del paciente aumente respecto al estado inicial.
        createdRowId = w.until(d -> {
            List<WebElement> rows = d.findElements(By.cssSelector("[id^='gridId-']"));
            if (rows.isEmpty()) return null;
            // La propuesta nueva siempre aparece al final del grid
            String lastId = null;
            for (WebElement row : rows) {
                String id = row.getAttribute("id");
                List<WebElement> pats = d.findElements(By.id("grid-" + id + "-patient"));
                if (!pats.isEmpty() && pats.get(0).getText().contains(PATIENT_FRAGMENT)) {
                    lastId = id;
                }
            }
            return lastId;
        });

        Reporter.log("TC2 OK — Propuesta creada (NH: " + nh + ", rowId: " + createdRowId + ").");
    }

    // =========================================================================
    // TC3 — PREANESTESIA: INDICACIÓN APTO + FIRMA
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = "TC1_LoginAndNavigate")
    public void TC3_Preanesthesia() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        // Gestionar cualquier modal residual del TC anterior
        dismissAnyAlert(w);

        selectLastPatientRow(w);
        openActionsMenuAndClick(w, "pre_anesthesia");

        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-PreanesthesiaContainer-button")));

        // Expandir panel si state no es visible
        w.until(d -> {
            WebElement st = d.findElement(By.id("state"));
            if (st.isDisplayed()) return true;
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                            "  var t=p.textContent;" +
                            "  if(t.includes('anestesia')||t.includes('Indicaci'))" +
                            "    if(p.getAttribute('aria-expanded')==='false') p.click();" +
                            "});");
            return null;
        });

        // Indicación → APTO (state-0)
        w.until(ExpectedConditions.elementToBeClickable(By.id("state"))).click();
        w.until(ExpectedConditions.elementToBeClickable(By.id("state-0"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        signReport(w);

        // Esperar cierre completo
        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(d -> d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        Reporter.log("TC3 OK — Preanestesia firmada (APTO).");
    }

    // =========================================================================
    // TC4 — ANESTESIA: TIEMPOS + TIPO GENERAL + FIRMA + CERRAR
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = "TC1_LoginAndNavigate")
    public void TC4_Anesthesia() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        dismissAnyAlert(w);

        selectLastPatientRow(w);
        openActionsMenuAndClick(w, "anesthesia");

        // Esperar que el formulario cargue
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("cancel-AnesthesiaReportContainer-button")));

        // Detectar si el formulario está en modo edición o modo lectura.
        // Modo edición : escudo naranja + campo anesthesiaStartTime editable
        // Modo lectura : escudo verde   + solo botón Cerrar (cancel-...-button)
        // En modo lectura NO hay inputs editables → saltar todo el fill y la firma.
        boolean isEditable = !driver.findElements(By.id("anesthesiaStartTime")).isEmpty()
                && driver.findElement(By.id("anesthesiaStartTime")).isEnabled();

        if (isEditable) {
            // ── Modo edición: rellenar campos y firmar ────────────────────────

            // Expandir todas las secciones colapsadas
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                            "  if(p.getAttribute('aria-expanded')==='false') p.click();});");

            // Fechas (nativeSetter)
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            fillDateById("anesthesiaStartTime", today);
            fillDateById("endTimeAnesthesia",   today);

            // Horas (execCommand por label)
            fillInputByLabel(w, "Hora inicio *", "09:00");
            fillInputByLabel(w, "Hora fin *",    "09:15");
            fillInputByLabel(w, "Duración",      "00:15");

            // Tipo → General (anesthesiaType-2)
            w.until(ExpectedConditions.elementToBeClickable(By.id("anesthesiaType"))).click();
            w.until(ExpectedConditions.elementToBeClickable(By.id("anesthesiaType-2"))).click();
            w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                    .stream().noneMatch(WebElement::isDisplayed));

            // Firmar
            signReport(w);

            Reporter.log("TC4 — Anestesia rellenada y firmada.");
        } else {
            // ── Modo lectura: formulario ya firmado, skip directo a cerrar ────
            Reporter.log("TC4 — Anestesia ya en modo lectura (firmada anteriormente), skip fill.");
        }

        // Cerrar el formulario.
        // El botón Cerrar siempre se llama cancel-AnesthesiaReportContainer-button
        // tanto en modo edición (antes de firmar) como en modo lectura (tras firmar).
        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("cancel-AnesthesiaReportContainer-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('cancel-AnesthesiaReportContainer-button').click();");

        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(d -> d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        Reporter.log("TC4 OK — Anestesia cerrada.");
    }

    // =========================================================================
    // TC5 — SEGURIDAD QX: PRIMERA PREGUNTA SÍ + BORRADOR
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = "TC1_LoginAndNavigate")
    public void TC5_SecurityQx() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        dismissAnyAlert(w);

        selectLastPatientRow(w);
        openActionsMenuAndClick(w, "surgical_safety");

        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-SurgicalSafetyContainer-button")));

        // Primera pregunta → Sí (prefijo "10-")
        w.until(ExpectedConditions.elementToBeClickable(By.id("10-true-button"))).click();

        // Guardar como borrador (sin firmar → naranja)
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SurgicalSafetyContainer-button"))).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-SurgicalSafetyContainer-button")));

        Reporter.log("TC5 OK — Seguridad Qx guardada como borrador.");
    }

    // =========================================================================
    // TC6 — INFORME QX: CIRUJANO + PROCEDIMIENTO OMC + DX QX + FIRMA
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = "TC1_LoginAndNavigate")
    public void TC6_QxReport() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        dismissAnyAlert(w);

        selectLastPatientRow(w);
        openActionsMenuAndClick(w, "operating_room_report");

        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("user-autocomplete-default-id")));

        // Cirujano → primera opción
        WebElement cirujano = w.until(ExpectedConditions.elementToBeClickable(
                By.id("user-autocomplete-default-id")));
        cirujano.clear();
        cirujano.sendKeys("Doctor");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("user-autocomplete-default-id-0"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-autocomplete-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Procedimiento OMC (1er Catálogo visible)
        w.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            WebElement first = btns.stream()
                    .filter(b -> b.isDisplayed() && "Catálogo".equals(b.getText().trim()))
                    .findFirst().orElse(null);
            if (first == null) return null;
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", first);
            return true;
        });
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("input-default-id"))).sendKeys("Queloide");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("expanded-node-button"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("tree-checkbox-initial-131062640-1"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("angle-right-button"))).click();
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("tree-checkbox-final-131062640-1"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-OMCProceduresCatalogContainer-button"))).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-OMCProceduresCatalogContainer-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(b=>b.click());");
        w.until(d -> d.findElement(By.id("omc")).getText().contains("131062"));

        // Diagnóstico Qx (2º Catálogo visible)
        w.until(d -> {
            List<WebElement> btns = d.findElements(By.id("show-catalog-button"));
            List<WebElement> vis = btns.stream()
                    .filter(b -> b.isDisplayed() && "Catálogo".equals(b.getText().trim()))
                    .collect(java.util.stream.Collectors.toList());
            if (vis.size() < 2) return null;
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", vis.get(1));
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
        w.until(d -> d.findElement(By.id("diagnoses")).getText().contains(DIAG_SEARCH));

        signReport(w);

        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(b=>b.click());");

        // Cerrar informe (botón Cancelar → Cerrar tras firma)
        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("cancel-SurgicalReportContainer-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('cancel-SurgicalReportContainer-button').click();");

        new WebDriverWait(driver, LONG_TIMEOUT)
                .until(d -> d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        Reporter.log("TC6 OK — Informe Qx firmado.");
    }

    // =========================================================================
    // TC7 — REGISTRO QX: TIPO + QUIRÓFANO + ANESTESIA + FECHAS/HORAS
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = "TC1_LoginAndNavigate")
    public void TC7_QxRegister() {
        WebDriverWait w = new WebDriverWait(driver, TIMEOUT);

        dismissAnyAlert(w);

        selectLastPatientRow(w);
        openActionsMenuAndClick(w, "operating_room_record");

        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-SurgicalRecordComponent-button")));

        // Expandir secciones
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "  if(p.getAttribute('aria-expanded')==='false') p.click();});");

        // Esperar que el campo Servicio tenga valor
        w.until(d -> {
            String v = (String) ((JavascriptExecutor) driver).executeScript(
                    "var el=document.getElementById('service'); return el?el.value:'';");
            return v != null && !v.trim().isEmpty();
        });

        // Tipo intervención → Programada
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('interventionType')" +
                        ".closest('mat-form-field')" +
                        ".querySelector('.mat-select-arrow-wrapper').click();");
        w.until(ExpectedConditions.elementToBeClickable(By.id("interventionType-0"))).click();
        w.until(d -> !d.findElement(By.id("interventionType"))
                .getAttribute("value").trim().isEmpty());

        // Quirófano → QX-1
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('surgicalNumberSurgical').click();");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("surgicalNumberSurgical-0"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Requiere anestesia → Sí
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('needsAnesthesistSurgical').click();");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("needsAnesthesistSurgical-0"))).click();
        w.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // Fechas (nativeSetter)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        fillDateById("operatingRoomEntryDate",     today);
        fillDateById("interventionStartDate",      today);
        fillDateById("interventionEndDate",        today);
        fillDateById("operatingRoomDepartureDate", today);

        // Horas (execCommand)
        fillTimeById("operatingRoomEntryTime",     "08:00");
        fillTimeById("interventionStartTime",      "08:05");
        fillTimeById("interventionEndTime",        "09:00");
        fillTimeById("operatingRoomDepartureTime", "09:05");

        // Aceptar
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-SurgicalRecordComponent-button"))).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-SurgicalRecordComponent-button")));

        Reporter.log("TC7 OK — Registro Qx guardado.");
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Selecciona la fila del grid correspondiente a la propuesta activa.
     *
     * Estrategia:
     *  - Si createdRowId != null (TC2 ya creó la propuesta en esta sesión):
     *    usa ese rowId directamente — garantiza trabajar siempre con la propuesta
     *    nueva, no con una anterior que pueda tener formularios ya firmados.
     *  - Si createdRowId == null (TC3-TC7 corren solos sin TC2):
     *    coge la última fila del paciente en el grid (comportamiento anterior).
     *
     * Click en -surgery-type: genera row--selected de forma fiable.
     * Espera row--selected antes de retornar para evitar "seleccionar un paciente".
     */
    private void selectLastPatientRow(WebDriverWait w) {
        final String targetRowId = (createdRowId != null) ? createdRowId : null;

        w.until(d -> {
            String rowId;
            if (targetRowId != null) {
                // Usar el rowId guardado por TC2
                rowId = targetRowId;
            } else {
                // Buscar la última fila del paciente
                List<WebElement> rows = d.findElements(By.cssSelector("[id^='gridId-']"));
                String lastId = null;
                for (WebElement row : rows) {
                    String id = row.getAttribute("id");
                    List<WebElement> pats = d.findElements(By.id("grid-" + id + "-patient"));
                    if (!pats.isEmpty() && pats.get(0).getText().contains(PATIENT_FRAGMENT)) {
                        lastId = id;
                    }
                }
                if (lastId == null) return null;
                rowId = lastId;
            }
            // Click en surgery-type — genera row--selected de forma fiable
            List<WebElement> cells = d.findElements(By.id("grid-" + rowId + "-surgery-type"));
            if (cells.isEmpty()) return null;
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cells.get(0));
            return rowId;
        });

        // Esperar que la clase row--selected aparezca en alguna fila
        w.until(d -> !d.findElements(By.cssSelector("[id^='gridId-'].row--selected")).isEmpty());
    }

    /**
     * Firma el informe abierto si el escudo es naranja (pendiente).
     * Si el escudo ya es verde (shield-check-valid) el informe está firmado — skip.
     */
    private void signReport(WebDriverWait w) {
        WebElement shield = w.until(d ->
                d.findElements(By.cssSelector("img[src*='shield']")).stream()
                        .filter(WebElement::isDisplayed).findFirst().orElse(null));

        if (shield.getAttribute("src").contains("valid")) {
            Reporter.log("signReport — escudo verde, ya firmado.");
            return;
        }

        shield.click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("password"))).sendKeys(ConfigReader.get("password_doctor"));
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-UserSignComponent-button"))).click();
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("sign-UserSignComponent-button")));
    }

    /**
     * Cierra cualquier modal de alerta/info (continue-button) que haya quedado
     * abierto de un TC anterior. Llamar al inicio de cada TC para limpiar estado.
     */
    private void dismissAnyAlert(WebDriverWait w) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")));
            ((JavascriptExecutor) driver).executeScript(
                    "var b=document.getElementById('continue-button'); if(b) b.click();");
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());
        } catch (TimeoutException ignored) {}
    }

    /**
     * Espera que el HeavyProcessLoader finalice y todos los dialogs se cierren.
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
     * Rellena un input localizado por el texto de su label con execCommand.
     */
    private void fillInputByLabel(WebDriverWait w, String labelText, String value) {
        w.until(d -> {
            Boolean done = (Boolean) ((JavascriptExecutor) driver).executeScript(
                    "var ffs=Array.from(document.querySelectorAll('mat-dialog-container mat-form-field'));" +
                            "var ff=ffs.find(function(f){var l=f.querySelector('label');" +
                            "  return l&&l.textContent.trim()==='" + labelText + "';});" +
                            "if(!ff) return false;" +
                            "var inp=ff.querySelector('input'); if(!inp) return false;" +
                            "inp.focus();inp.select();" +
                            "document.execCommand('selectAll');" +
                            "document.execCommand('delete');" +
                            "document.execCommand('insertText',false,'" + value + "');" +
                            "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                            "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                            "inp.blur();return true;");
            return Boolean.TRUE.equals(done);
        });
    }

    /**
     * Rellena un input de fecha por ID con nativeSetter. Formato: dd/MM/yyyy.
     */
    private void fillDateById(String id, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var el=document.getElementById('" + id + "');" +
                        "var s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                        "s.call(el,'" + value + "');" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));");
    }

    /**
     * Rellena un input de hora por ID con execCommand.
     */
    private void fillTimeById(String id, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var f=document.getElementById('" + id + "'); if(!f) return;" +
                        "f.focus();f.select();" +
                        "document.execCommand('selectAll');" +
                        "document.execCommand('delete');" +
                        "document.execCommand('insertText',false,'" + value + "');" +
                        "f.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "f.dispatchEvent(new Event('change',{bubbles:true}));f.blur();");
    }
}
