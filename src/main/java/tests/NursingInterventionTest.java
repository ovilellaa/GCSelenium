package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Test: Registro de Enfermería → Previo Intervención
 *
 * ── Credenciales ─────────────────────────────────────────────────────────────
 *   username_nurse / password_nurse (enfermera)
 *
 * ── Paciente ──────────────────────────────────────────────────────────────────
 *   pacqah1NH (mismo que InterventionsTest)
 *
 * ── Submenú ───────────────────────────────────────────────────────────────────
 *   nursing_record (trigger) → nursing_record_pre (Previo intervención)
 *   Doble JS click en el mismo script
 *
 * ── Formulario ────────────────────────────────────────────────────────────────
 *   Secciones colapsadas — expandir todas antes de rellenar
 *
 *   arrivalDate     → fecha de hoy (nativeSetter)
 *
 *   Grupos Sí/No (mat-button-toggle-group) — 13 grupos en total:
 *   Índice | Label
 *     0    | Acude con acompañante
 *     1    | Analítica
 *     2    | Pulsera de identificación
 *     3    | P. Cruzados
 *     4    | Consentimiento informado
 *     5    | ECG
 *     6    | RX
 *     7    | Ayunas
 *     8    | Control analítico
 *     9    | Premedicación
 *    10    | Administrado
 *    11    | Profilaxis con antibióticos
 *    12    | Administrado
 *
 *   Click correcto: mat-button-toggle[id="true"] button (dentro de cada grupo)
 *   NO usar #true-button directamente — todos tienen el mismo ID
 *
 *   supplementaryTests → textarea nativeSetter
 *
 *   nurse → disabled (se auto-rellena con la sesión actual)
 *
 *   Aceptar: accept-NursingRecordPreDialog-button
 *   Cancelar: cancel-NursingRecordPreDialog-button
 */
public class NursingInterventionTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // =========================================================================
    // TC1 — LOGIN ENFERMERA + NAVEGAR A INTERVENCIONES
    // =========================================================================
    @Test(priority = 1)
    public void TC1_LoginAndNavigate() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_nurse"));
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("password"))).sendKeys(ConfigReader.get("password_nurse"));
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

        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.mat-drawer-backdrop,.cdk-overlay-backdrop')" +
                        ".forEach(b=>b.click());");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("interventions-sidebar")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('interventions-sidebar').click();");

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button-interventions")));

        Reporter.log("TC1 OK — Intervenciones cargado con enfermera. Paciente: " + pacienteQX);
    }

    // =========================================================================
    // TC2 — REGISTRO DE ENFERMERÍA → PREVIO INTERVENCIÓN
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC2_PreInterventionNursing() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la primera fila del grid (índice 0 = última propuesta creada)
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir Registro enfermería → Previo intervención ───────────────────
        // nursing_record es submenú trigger — doble JS click en el mismo script
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        // Esperar que nursing_record esté presente antes del doble click
        // El hijo (nursing_record_pre) aparece tras el primer click en nursing_record
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("nursing_record")));
        localWait.until(d -> {
            Object exists = ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('nursing_record_pre') !== null;");
            if (!Boolean.TRUE.equals(exists)) {
                ((JavascriptExecutor) driver).executeScript(
                        "var n = document.getElementById('nursing_record');" +
                                "if (n) n.click();");
                return null;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('nursing_record_pre').click();");
            return true;
        });

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-NursingRecordPreDialog-button")));

        // ── Expandir todas las secciones colapsadas ───────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // ── Fecha de llegada ──────────────────────────────────────────────────
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('arrivalDate');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(el, '" + today + "');" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));");

        // ── Todos los grupos Sí/No → Sí ──────────────────────────────────────
        // Hay 13 grupos mat-button-toggle-group.
        // El click correcto es mat-button-toggle[id="true"] button dentro de cada grupo
        // (NO usar #true-button directamente — todos comparten ese ID)
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-button-toggle-group').forEach(function(g){" +
                        "    var btn = g.querySelector(\"mat-button-toggle[id='true'] button\");" +
                        "    if (btn) btn.click();" +
                        "});");

        // Verificar que todos los toggles quedaron seleccionados


        // ── Pruebas complementarias (textarea) ───────────────────────────────
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("supplementaryTests")));
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('supplementaryTests');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLTextAreaElement.prototype, 'value').set;" +
                        "setter.call(el, 'Pruebas complementarias de prueba automatizado');" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));");

        // ── Aceptar ───────────────────────────────────────────────────────────
        // nurse está disabled — se auto-rellena con la sesión actual de la enfermera
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-NursingRecordPreDialog-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-NursingRecordPreDialog-button")));

        // ── Verificar datos guardados: reabrir y comprobar arrivalDate ─────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        // Esperar que nursing_record esté presente antes del doble click
        // El hijo (nursing_record_pre) aparece tras el primer click en nursing_record
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("nursing_record")));
        localWait.until(d -> {
            Object exists = ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('nursing_record_pre') !== null;");
            if (!Boolean.TRUE.equals(exists)) {
                ((JavascriptExecutor) driver).executeScript(
                        "var n = document.getElementById('nursing_record');" +
                                "if (n) n.click();");
                return null;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('nursing_record_pre').click();");
            return true;
        });

        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("arrivalDate")));
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // Verificar fecha de llegada guardada
        localWait.until(d -> {
            String val = (String) ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('arrivalDate').value;");
            return val != null && !val.trim().isEmpty();
        });
        String savedDate = (String) ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('arrivalDate').value;");
        org.testng.Assert.assertFalse(savedDate.isEmpty(),
                "TC2 — arrivalDate debe estar guardado");

        // Verificar primer toggle = Sí
        String firstToggleChecked = (String) ((JavascriptExecutor) driver).executeScript(
                "var g = document.querySelectorAll('mat-button-toggle-group')[0];" +
                        "var c = g ? g.querySelector('.mat-button-toggle-checked') : null;" +
                        "return c ? c.id : '';");
        org.testng.Assert.assertEquals(firstToggleChecked, "true",
                "TC2 — Primer toggle debe ser Sí");

        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('accept-NursingRecordPreDialog-button').click();");
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-NursingRecordPreDialog-button")));

        Reporter.log("TC2 OK — Previo Intervención guardado. arrivalDate=" + savedDate);
    }
    // =========================================================================
    // TC3 — REGISTRO INTRAOPERATORIO
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC3_IntraoperativeNursing() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir Registro intraoperatorio ────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        // Esperar que el submenú hijo esté disponible antes del doble click
        // Esperar que nursing_record esté presente antes del doble click
        // El hijo (nursing_record_intra) aparece tras el primer click en nursing_record
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("nursing_record")));
        localWait.until(d -> {
            Object exists = ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('nursing_record_intra') !== null;");
            if (!Boolean.TRUE.equals(exists)) {
                ((JavascriptExecutor) driver).executeScript(
                        "var n = document.getElementById('nursing_record');" +
                                "if (n) n.click();");
                return null;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('nursing_record_intra').click();");
            return true;
        });

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-NursingRecordIntraDialog-button")));

        // ── Expandir todas las secciones ──────────────────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // ── Pintado (mat-select) → Alcohol yodado ─────────────────────────────
        // skinAntisepsisType-0 = Alcohol yodado
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('skinAntisepsisType').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("skinAntisepsisType-0"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Tipo de anestesia (mat-select) → General ──────────────────────────
        // anesthesiaType-1 = General
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('anesthesiaType').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("anesthesiaType-1"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Posición del paciente (mat-select) → Decúbito supino ─────────────
        // customerPosition-3 = Decúbito supino
        // JS click para evitar intercepción de tooltip
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('customerPosition').click();");
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("customerPosition-3")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('customerPosition-3').click();");
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Grado de asepsia (mat-select) → Limpia ───────────────────────────
        // gradeAsepsis-1 = Limpia
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('gradeAsepsis').click();");
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("gradeAsepsis-1")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('gradeAsepsis-1').click();");
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Todos los grupos Sí/No → Sí ──────────────────────────────────────
        // 13 grupos — click en mat-button-toggle[id="true"] button de cada uno
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-button-toggle-group').forEach(function(g){" +
                        "    var btn = g.querySelector(\"mat-button-toggle[id='true'] button\");" +
                        "    if (btn) btn.click();" +
                        "});");

        // ── Aceptar ───────────────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-NursingRecordIntraDialog-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-NursingRecordIntraDialog-button")));

        // ── Verificar datos guardados: reabrir y comprobar anesthesiaType ─────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(b=>b.click());");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        // Esperar que nursing_record esté presente antes del doble click
        // El hijo (nursing_record_intra) aparece tras el primer click en nursing_record
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("nursing_record")));
        localWait.until(d -> {
            Object exists = ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('nursing_record_intra') !== null;");
            if (!Boolean.TRUE.equals(exists)) {
                ((JavascriptExecutor) driver).executeScript(
                        "var n = document.getElementById('nursing_record');" +
                                "if (n) n.click();");
                return null;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('nursing_record_intra').click();");
            return true;
        });

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("anesthesiaType")));
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // Verificar tipo de anestesia guardado
        localWait.until(d -> {
            String val = (String) ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('anesthesiaType').textContent.trim();");
            return val != null && !val.trim().isEmpty();
        });
        String savedAnesthesia = (String) ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('anesthesiaType').textContent.trim();");
        org.testng.Assert.assertEquals(savedAnesthesia, "General",
                "TC3 — Tipo de anestesia debe ser General");

        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('accept-NursingRecordIntraDialog-button').click();");
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-NursingRecordIntraDialog-button")));

        Reporter.log("TC3 OK — Registro Intraoperatorio guardado. anesthesiaType=" + savedAnesthesia);
    }

    // =========================================================================
    // TC4 — URPA
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = {"TC1_LoginAndNavigate"})
    public void TC4_Urpa() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();

        // ── Abrir URPA ────────────────────────────────────────────────────────
        // nursing_record_post = URPA
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        // Esperar que nursing_record esté presente antes del doble click
        // El hijo (nursing_record_post) aparece tras el primer click en nursing_record
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("nursing_record")));
        localWait.until(d -> {
            Object exists = ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('nursing_record_post') !== null;");
            if (!Boolean.TRUE.equals(exists)) {
                ((JavascriptExecutor) driver).executeScript(
                        "var n = document.getElementById('nursing_record');" +
                                "if (n) n.click();");
                return null;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('nursing_record_post').click();");
            return true;
        });

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-NursingRecordPostDialog-button")));

        // ── Expandir secciones ────────────────────────────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // ── Fecha inicio y fin (nativeSetter) ─────────────────────────────────
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        ((JavascriptExecutor) driver).executeScript(
                "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLInputElement.prototype,'value').set;" +
                        "['startDate','endDate'].forEach(function(id){" +
                        "    var el = document.getElementById(id);" +
                        "    setter.call(el,'" + today + "');" +
                        "    el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "    el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "});");

        // ── Hora inicio y Hora fin por label (nativeSetter) ───────────────────
        // Las horas tienen IDs dinámicos (mat-input-XXX) — se localizan por label
        // IMPORTANTE: usar nativeSetter, no execCommand (execCommand no funciona en estos inputs)
        fillInputByLabelNative(localWait, "Hora inicio", "09:30");
        fillInputByLabelNative(localWait, "Hora fin",    "10:00");
        // duration es campo calculado (disabled) — no rellenar

        // ── Destino (mat-select) → Domicilio ──────────────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('destination').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("destination-0"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Estado de conciencia (mat-select) → Consciente ────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('awareness').click();");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("awareness-2"))).click();
        localWait.until(d -> d.findElements(By.cssSelector(".mat-select-panel"))
                .stream().noneMatch(WebElement::isDisplayed));

        // ── Todos los grupos Sí/No → Sí ──────────────────────────────────────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-button-toggle-group').forEach(function(g){" +
                        "    var btn = g.querySelector(\"mat-button-toggle[id='true'] button\");" +
                        "    if (btn) btn.click();" +
                        "});");



        // ── Aceptar ───────────────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-NursingRecordPostDialog-button"))).click();
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-NursingRecordPostDialog-button")));

        // ── Verificar datos guardados: reabrir y comprobar startDate ──────────
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(b=>b.click());");
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-interventions-0-code"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button-interventions"))).click();
        // Esperar que nursing_record esté presente antes del doble click
        // El hijo (nursing_record_post) aparece tras el primer click en nursing_record
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("nursing_record")));
        localWait.until(d -> {
            Object exists = ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('nursing_record_post') !== null;");
            if (!Boolean.TRUE.equals(exists)) {
                ((JavascriptExecutor) driver).executeScript(
                        "var n = document.getElementById('nursing_record');" +
                                "if (n) n.click();");
                return null;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('nursing_record_post').click();");
            return true;
        });

        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("startDate")));
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('mat-expansion-panel-header').forEach(function(p){" +
                        "    if (p.getAttribute('aria-expanded') === 'false') p.click();" +
                        "});");

        // Verificar fecha de inicio guardada
        localWait.until(d -> {
            String val = (String) ((JavascriptExecutor) driver).executeScript(
                    "return document.getElementById('startDate').value;");
            return val != null && !val.trim().isEmpty();
        });
        String savedStartDate = (String) ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('startDate').value;");
        org.testng.Assert.assertFalse(savedStartDate.isEmpty(),
                "TC4 — startDate debe estar guardado");

        // Verificar estado de conciencia guardado
        String savedAwareness = (String) ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('awareness').textContent.trim();");
        org.testng.Assert.assertEquals(savedAwareness, "Consciente",
                "TC4 — Estado de conciencia debe ser Consciente");

        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('accept-NursingRecordPostDialog-button').click();");
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-NursingRecordPostDialog-button")));

        Reporter.log("TC4 OK — URPA guardado. startDate=" + savedStartDate + ", awareness=" + savedAwareness);
    }

    /**
     * Rellena un input localizado por label usando nativeSetter.
     * Para inputs de hora con IDs dinámicos (mat-input-XXX) que no aceptan execCommand.
     */
    private void fillInputByLabelNative(WebDriverWait w, String labelText, String value) {
        w.until(d -> {
            Boolean done = (Boolean) ((JavascriptExecutor) driver).executeScript(
                    "var ffs = Array.from(document.querySelectorAll(" +
                            "    'mat-dialog-container mat-form-field'));" +
                            "var ff = ffs.find(function(f){" +
                            "    var l = f.querySelector('label');" +
                            "    return l && l.textContent.trim() === '" + labelText + "';" +
                            "});" +
                            "if (!ff) return false;" +
                            "var inp = ff.querySelector('input');" +
                            "if (!inp) return false;" +
                            "var setter = Object.getOwnPropertyDescriptor(" +
                            "    window.HTMLInputElement.prototype,'value').set;" +
                            "setter.call(inp, '" + value + "');" +
                            "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                            "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                            "inp.blur();" +
                            "return true;");
            return Boolean.TRUE.equals(done);
        });
    }

}
