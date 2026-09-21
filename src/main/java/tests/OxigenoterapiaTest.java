package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Flujo:
 *   1.  Login como médico
 *   2.  Buscar paciente y abrir historial
 *   3.  Navegar a Tratamientos > Oxigenoterapia
 *   TC1: Añadir oxigenoterapia (campos obligatorios), modificarla y cerrarla
 *   TC2: Añadir VM (campos obligatorios), modificarla y cerrarla
 *
 * NOTA DE MIGRACIÓN (desde CarlosFreire): abrirAcciones() ya probaba
 * correctamente el ID real "actions-button" antes de caer a texto — no se
 * ha tocado. Se ha sustituido por ID verificado solo el resto de diálogos
 * genéricos (alertas vitales, "Continuar", confirmación). Los selectores
 * de campos/menús específicos de Oxigenoterapia/VM quedan marcados con TODO.
 */
public class OxigenoterapiaTest extends ClassBaseTest {

    // ══════════════════════════════════════════════════════════════════
    // SETUP
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 1)
    public void loginMedico() {
        LoginAsDoctor();
        wait.until(d -> !d.getCurrentUrl().contains("/login"));
        Reporter.log("Login completado — URL: " + driver.getCurrentUrl());
    }

    @Test(priority = 10, dependsOnMethods = "loginMedico")
    public void buscarYAbrirHistorial() {
        // Paciente leído de ConfigReader ("pacqah1NH"), igual que en el resto
        // del proyecto — ver nota en DietsTest.buscarYAbrirHistorial.
        wait.until(ExpectedConditions.elementToBeClickable(By.id("search-action"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("spotlight-search")))
                .sendKeys(ConfigReader.get("pacqah1NH"));

        wait.until(ExpectedConditions.elementToBeClickable(By.id("spotlight-list-item-0-0"))).click();

        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("quickAction-PATIENT_SEE_HISTORY"))).click();

        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("dialog-patient-episodes-panel-0"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("accept-PatientEpisodesContainer-button"))).click();
        } catch (Exception ignored) {}

        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("accept-FileHistoryAccess-button"))).click();
        } catch (Exception ignored) {}

        WaitAMomentPlease(1.5f);
        SwitchToTab(GetLastTabOpened());
        Reporter.log("Foco en nueva pestaña — URL: " + driver.getCurrentUrl());

        // Heredado de ClassBaseTest — mismo ID (accept-AlertsContainer-button).
        handleVitalAlertsDialog();

        WaitAMomentPlease(1.0f);
        Reporter.log("Historial abierto");
    }

    @Test(priority = 20, dependsOnMethods = "buscarYAbrirHistorial")
    public void navegarAOxigenoterapia() {
        // TODO: navegación por texto visible — ver nota en DietsTest.navegarADietas.
        WaitAMomentPlease(1.5f);

        Boolean tratamientosOk = (Boolean) js().executeScript(
                "var all = Array.from(document.querySelectorAll(" +
                        "  'a, button, mat-list-item, [role=\"tab\"], span, li'));" +
                        "var t = all.find(el => el.textContent.trim().toLowerCase() === 'tratamientos'" +
                        "  && el.offsetParent !== null);" +
                        "if (t) { t.click(); return true; } return false;");
        if (Boolean.TRUE.equals(tratamientosOk))
            Reporter.log("Click en 'Tratamientos'");

        WaitAMomentPlease(1.0f);

        Boolean oxOk = (Boolean) js().executeScript(
                "var all = Array.from(document.querySelectorAll(" +
                        "  'a, button, mat-list-item, [role=\"tab\"], span, li'));" +
                        "var t = all.find(el => el.textContent.trim().toLowerCase() === 'oxigenoterapia'" +
                        "  && el.offsetParent !== null);" +
                        "if (t) { t.click(); return true; } return false;");
        if (Boolean.TRUE.equals(oxOk))
            Reporter.log("Click en 'Oxigenoterapia'");

        WaitAMomentPlease(2.0f);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//*[contains(@class,'actions') or contains(text(),'Acciones')" +
                        " or contains(text(),'oxigenoterapia') or contains(text(),'Oxigenoterapia')]")));

        Reporter.log("Módulo Oxigenoterapia cargado — URL: " + driver.getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════
    // TC1 — AÑADIR OXIGENOTERAPIA, MODIFICARLA Y CERRARLA
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 30, dependsOnMethods = "navegarAOxigenoterapia")
    public void tc1_añadirModificarYCerrarOxigenoterapia() {
        long antes = contarFilas();
        Reporter.log("Registros antes: " + antes);

        // ── AÑADIR ────────────────────────────────────────────────
        abrirAcciones();
        clickMenuItem("Añadir oxigenoterapia");

        esperarDialogoListo();
        rellenarFormularioOxigenoterapia();
        aceptarDialogo();

        long[] trasCrear = {contarFilas()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasCrear[0] = contarFilas();
            return trasCrear[0] > antes;
        });
        Reporter.log("Registros tras añadir: " + trasCrear[0]);
        Assert.assertTrue(trasCrear[0] > antes, "Debe haberse añadido la oxigenoterapia");
        Reporter.log("Oxigenoterapia añadida");

        // ── MODIFICAR ─────────────────────────────────────────────
        WaitAMomentPlease(1.0f);
        seleccionarPrimeraFilaDeEstado("Activo");
        abrirAcciones();
        clickMenuItem("Modificar oxigenoterapia");

        esperarDialogoListo();
        modificarCampoObservaciones("Modificado por test automático");
        aceptarDialogo();
        WaitAMomentPlease(1.5f);
        Reporter.log("Oxigenoterapia modificada");

        // ── CERRAR ────────────────────────────────────────────────
        WaitAMomentPlease(1.0f);
        seleccionarPrimeraFilaDeEstado("Activo");
        abrirAcciones();
        clickMenuItem("Cerrar oxigenoterapia/VM");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);
        Reporter.log("Oxigenoterapia cerrada");

        Reporter.log("TC1 completado — Oxigenoterapia añadida, modificada y cerrada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC2 — AÑADIR VM, MODIFICARLA Y CERRARLA
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = "tc1_añadirModificarYCerrarOxigenoterapia")
    public void tc2_añadirModificarYCerrarVM() {
        long antes = contarFilas();
        Reporter.log("Registros antes: " + antes);

        // ── AÑADIR ────────────────────────────────────────────────
        abrirAcciones();
        clickMenuItem("Añadir VM");

        esperarDialogoListo();
        rellenarFormularioVM();
        aceptarDialogo();

        long[] trasCrear = {contarFilas()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasCrear[0] = contarFilas();
            return trasCrear[0] > antes;
        });
        Reporter.log("Registros tras añadir: " + trasCrear[0]);
        Assert.assertTrue(trasCrear[0] > antes, "Debe haberse añadido la VM");
        Reporter.log("VM añadida");

        // ── MODIFICAR ─────────────────────────────────────────────
        WaitAMomentPlease(1.0f);
        seleccionarFila("Activo", "vmi");   // Seleccionar fila VM activa (contiene VMI/VMNI)
        abrirAcciones();
        clickMenuItem("Modificar VM");

        esperarDialogoListo();
        modificarCampoObservaciones("Modificado por test automático");
        aceptarDialogo();
        WaitAMomentPlease(1.5f);
        Reporter.log("VM modificada");

        // ── CERRAR ────────────────────────────────────────────────
        WaitAMomentPlease(1.0f);
        seleccionarFila("Activo", "vmi");   // Seleccionar fila VM activa
        abrirAcciones();
        clickMenuItem("Cerrar oxigenoterapia/VM");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);
        Reporter.log("VM cerrada");

        Reporter.log("CIRCUITO OXIGENOTERAPIA + VM COMPLETADO EXITOSAMENTE");
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS DE FORMULARIO
    // ══════════════════════════════════════════════════════════════════

    /**
     * Rellena los campos obligatorios (*) del formulario "Nueva oxigenoterapia":
     *   - Dispositivo *  → MAT-SELECT: primera opción disponible
     *   - Fecha inicio * → INPUT datepicker: fecha de hoy (dd/MM/yyyy)
     *   - Hora inicio *  → INPUT texto HH:mm → "09:00"
     *   - Duración *     → MAT-SELECT: primera opción disponible
     *
     * TODO: campos localizados por posición/label de texto — módulo
     * específico sin verificar contra la app real.
     */
    private void rellenarFormularioOxigenoterapia() {
        Reporter.log("Rellenando formulario de oxigenoterapia (campos obligatorios)...");
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        seleccionarPrimerMatSelect();           // Dispositivo *
        rellenarInputPorLabel("Fecha inicio", fechaHoy, false);   // Fecha inicio *
        rellenarInputPorLabel("Hora inicio",  "09:00",  false);   // Hora inicio *
        seleccionarUltimoMatSelectVacio();      // Duración *

        WaitAMomentPlease(0.5f);
        Reporter.log("Formulario de oxigenoterapia rellenado");
    }

    /**
     * Rellena los campos obligatorios (*) del formulario "Nueva VM":
     *   - Tipo *       → primer MAT-SELECT  (p.ej. VMI)
     *   - Modo *       → segundo MAT-SELECT (p.ej. primera opción)
     *   - Dispositivo *→ tercer MAT-SELECT  (p.ej. Mascarilla...)
     *
     * No hay fecha, hora ni duración en este formulario.
     */
    private void rellenarFormularioVM() {
        Reporter.log("Rellenando formulario VM (campos obligatorios: Tipo, Modo, Dispositivo)...");
        seleccionarTodosMatSelectsVacios();
        WaitAMomentPlease(0.5f);
        Reporter.log("Formulario VM rellenado");
    }

    /**
     * Selecciona la primera opción en cada MAT-SELECT vacío del diálogo VM.
     * Itera hasta 5 veces; en cada iteración abre el primer select vacío
     * y elige la primera opción disponible.
     */
    private void seleccionarTodosMatSelectsVacios() {
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            try {
                Boolean abierto = (Boolean) js().executeScript(
                        "var dialog = document.querySelector('mat-dialog-container');" +
                                "if (!dialog) return false;" +
                                "var sels = Array.from(dialog.querySelectorAll('mat-select'))" +
                                "  .filter(function(s) {" +
                                "    return s.offsetParent !== null" +
                                "      && !s.querySelector('.mat-select-value-text');" +
                                "  });" +
                                "if (sels.length === 0) return false;" +
                                "sels[0].scrollIntoView({block:'center'});" +
                                "sels[0].click();" +
                                "return true;");

                if (!Boolean.TRUE.equals(abierto)) {
                    Reporter.log("No hay más MAT-SELECTs vacíos tras " + idx + " iteraciones");
                    break;
                }
                WaitAMomentPlease(0.8f);

                new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                        (Boolean) js().executeScript(
                                "var opts = Array.from(document.querySelectorAll('mat-option'))" +
                                        "  .filter(function(o) { return o.offsetParent !== null; });" +
                                        "if (opts.length === 0) return false;" +
                                        "opts[0].scrollIntoView({block:'center'});" +
                                        "opts[0].click();" +
                                        "return true;"));

                Reporter.log("MAT-SELECT " + (idx + 1) + " rellenado");
                WaitAMomentPlease(0.5f);

            } catch (Exception e) {
                Reporter.log("[WARN] seleccionarTodosMatSelectsVacios iter " + idx + ": " + e.getMessage());
                break;
            }
        }
    }

    /** Abre el primer MAT-SELECT del diálogo y elige la primera opción. */
    private void seleccionarPrimerMatSelect() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                    (Boolean) js().executeScript(
                            "var dialog = document.querySelector('mat-dialog-container');" +
                                    "if (!dialog) return false;" +
                                    "var sel = Array.from(dialog.querySelectorAll('mat-select'))" +
                                    "  .find(s => s.offsetParent !== null);" +
                                    "if (sel) { sel.scrollIntoView({block:'center'}); sel.click(); return true; }" +
                                    "return false;"));
            WaitAMomentPlease(0.8f);

            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                    (Boolean) js().executeScript(
                            "var opts = Array.from(document.querySelectorAll(" +
                                    "  'mat-option')).filter(o => o.offsetParent !== null);" +
                                    "if (opts[0]) { opts[0].scrollIntoView({block:'center'}); opts[0].click(); return true; }" +
                                    "return false;"));

            Reporter.log("Primer MAT-SELECT rellenado");
            WaitAMomentPlease(0.5f);
        } catch (Exception e) {
            Reporter.log("[WARN] seleccionarPrimerMatSelect: " + e.getMessage());
        }
    }

    /**
     * Abre el último MAT-SELECT del diálogo que aún no tenga valor
     * (corresponde a "Duración *") y elige la primera opción.
     */
    private void seleccionarUltimoMatSelectVacio() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                    (Boolean) js().executeScript(
                            "var dialog = document.querySelector('mat-dialog-container');" +
                                    "if (!dialog) return false;" +
                                    "var sels = Array.from(dialog.querySelectorAll('mat-select'))" +
                                    "  .filter(s => s.offsetParent !== null &&" +
                                    "    !s.querySelector('.mat-select-value-text'));" +
                                    "if (!sels.length) {" +
                                    "  var all = Array.from(dialog.querySelectorAll('mat-select'))" +
                                    "    .filter(s => s.offsetParent !== null);" +
                                    "  sels = [all[all.length - 1]];" +
                                    "}" +
                                    "var sel = sels[sels.length - 1];" +
                                    "if (sel) { sel.scrollIntoView({block:'center'}); sel.click(); return true; }" +
                                    "return false;"));
            WaitAMomentPlease(0.8f);

            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                    (Boolean) js().executeScript(
                            "var opts = Array.from(document.querySelectorAll(" +
                                    "  'mat-option')).filter(o => o.offsetParent !== null);" +
                                    "if (opts[0]) { opts[0].scrollIntoView({block:'center'}); opts[0].click(); return true; }" +
                                    "return false;"));

            Reporter.log("MAT-SELECT Duración rellenado");
            WaitAMomentPlease(0.5f);
        } catch (Exception e) {
            Reporter.log("[WARN] seleccionarUltimoMatSelectVacio: " + e.getMessage());
        }
    }

    /**
     * Rellena un INPUT del diálogo buscando la mat-label o label más cercana
     * que contenga {@code labelTexto}.
     *
     * @param labelTexto  Texto de la etiqueta (p.ej. "Fecha inicio", "Hora inicio")
     * @param valor       Valor a escribir
     * @param esDatepicker Si true, usa TAB final para confirmar datepicker Angular
     */
    private void rellenarInputPorLabel(String labelTexto, String valor, boolean esDatepicker) {
        try {
            WebElement input = (WebElement) js().executeScript(
                    "var lbl = '" + labelTexto.toLowerCase().replace("'", "\'") + "';" +
                            "var dialog = document.querySelector('mat-dialog-container');" +
                            "if (!dialog) return null;" +
                            "var fields = Array.from(dialog.querySelectorAll('mat-form-field'));" +
                            "var field = fields.find(f => {" +
                            "  var el = f.querySelector('mat-label, label');" +
                            "  return el && el.textContent.trim().toLowerCase().includes(lbl);" +
                            "});" +
                            "if (!field) return null;" +
                            "return field.querySelector('input');");

            if (input == null) {
                Reporter.log("[WARN] No se encontró input para label '" + labelTexto + "'");
                return;
            }

            js().executeScript("arguments[0].scrollIntoView({block:'center'});", input);
            input.click();
            input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            input.sendKeys(valor);
            input.sendKeys(Keys.TAB);

            String valorFinal = (String) js().executeScript("return arguments[0].value;", input);
            Reporter.log("'" + labelTexto + "' rellenado: " + valorFinal);
            WaitAMomentPlease(0.4f);
        } catch (Exception e) {
            Reporter.log("[WARN] rellenarInputPorLabel('" + labelTexto + "'): " + e.getMessage());
        }
    }

    /**
     * Escribe en el campo Observaciones del diálogo activo (para la operación Modificar).
     */
    private void modificarCampoObservaciones(String texto) {
        try {
            Boolean ok = (Boolean) js().executeScript(
                    "var dialog = document.querySelector('mat-dialog-container');" +
                            "if (!dialog) return false;" +
                            "var area = dialog.querySelector('textarea');" +
                            "if (!area) area = dialog.querySelector('[contenteditable=\"true\"]');" +
                            "if (area) { area.scrollIntoView({block:'center'}); area.focus(); area.click(); return true; }" +
                            "return false;");

            if (Boolean.TRUE.equals(ok)) {
                WebElement area;
                try {
                    area = driver.findElement(By.xpath("//mat-dialog-container//textarea"));
                } catch (NoSuchElementException e) {
                    area = driver.findElement(By.xpath(
                            "//mat-dialog-container//*[@contenteditable='true']"));
                }
                area.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                area.sendKeys(texto);
                Reporter.log("Observaciones actualizadas");
            } else {
                Reporter.log("[WARN] No se encontró el campo Observaciones — se continúa sin modificar");
            }
            WaitAMomentPlease(0.3f);
        } catch (Exception e) {
            Reporter.log("[WARN] modificarCampoObservaciones: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS DE NAVEGACIÓN Y UI
    // ══════════════════════════════════════════════════════════════════

    /**
     * Abre el menú "Acciones" del módulo de Oxigenoterapia. Ya probaba
     * correctamente el ID real "actions-button" antes de caer a texto —
     * se mantiene sin cambios.
     */
    private void abrirAcciones() {
        try {
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
            js().executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            WaitAMomentPlease(0.3f);
            js().executeScript("arguments[0].click();", btn);
            WaitAMomentPlease(0.8f);
            Reporter.log("Menú Acciones abierto (via #actions-button)");
            return;
        } catch (Exception ignored) {}

        wait.until(d -> (Boolean) js().executeScript(
                "var btns = Array.from(document.querySelectorAll('button'))" +
                        "  .filter(b => b.offsetParent !== null && !b.disabled);" +
                        "var t = btns.find(b => b.textContent.trim().toLowerCase().includes('acciones'));" +
                        "if (t) { t.scrollIntoView({block:'center'}); t.click(); return true; }" +
                        "return false;"));
        WaitAMomentPlease(0.8f);
        Reporter.log("Menú Acciones abierto (via texto)");
    }

    // TODO: ítems de menú específicos de Oxigenoterapia/VM localizados por
    // texto — no se conoce su ID real. Sustituir cuando se valide contra QA.
    private void clickMenuItem(String texto) {
        String needle = texto.toLowerCase().replace("'", "\\'");
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle = '" + needle + "';" +
                            "var items = Array.from(document.querySelectorAll(" +
                            "  'button[role=\"menuitem\"], .mat-menu-item, button'));" +
                            "var t = items.find(b => b.textContent.trim().toLowerCase() === needle" +
                            "  && b.offsetParent !== null && !b.disabled);" +
                            "if (!t) t = items.find(b => b.textContent.trim().toLowerCase().includes(needle)" +
                            "  && b.offsetParent !== null && !b.disabled);" +
                            "if (t) { t.click(); return true; } return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
        Reporter.log("Opción de menú '" + texto + "' clickada");
    }

    /**
     * Selecciona la primera fila cuyo estado sea el indicado (p.ej. "Activo").
     */
    private void seleccionarPrimeraFilaDeEstado(String estado) {
        seleccionarFila(estado, null);
    }

    /**
     * Selecciona la primera fila que coincida con estado Y tipo (columna Tipo).
     * Si tipo es null, solo filtra por estado.
     */
    private void seleccionarFila(String estado, String tipo) {
        WaitAMomentPlease(0.5f);
        String estadoNeedle = estado != null ? estado.toLowerCase() : "";
        String tipoNeedle   = tipo   != null ? tipo.toLowerCase()   : "";

        Boolean ok = (Boolean) js().executeScript(
                "var estadoN = '" + estadoNeedle + "';" +
                        "var tipoN   = '" + tipoNeedle   + "';" +
                        "var filas = Array.from(document.querySelectorAll('tr.selectable, tr.mat-row, mat-row'))" +
                        "  .filter(f => f.offsetParent !== null);" +
                        "var t = filas.find(f => {" +
                        "  var txt = f.textContent.toLowerCase();" +
                        "  var estadoOk = !estadoN || txt.includes(estadoN);" +
                        "  var tipoOk   = !tipoN   || txt.includes(tipoN);" +
                        "  return estadoOk && tipoOk;" +
                        "});" +
                        "if (!t) t = filas[0];" +
                        "if (t) { t.scrollIntoView({block:'center'}); t.click(); return true; }" +
                        "return false;");
        if (!Boolean.TRUE.equals(ok))
            throw new RuntimeException("No se encontró ninguna fila en la tabla de oxigenoterapias");
        Reporter.log("Fila seleccionada" + (tipo != null ? " (tipo=" + tipo + ")" : ""));
        WaitAMomentPlease(0.5f);
    }

    private void esperarDialogoListo() {
        new WebDriverWait(driver, Duration.ofSeconds(40))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.tagName("mat-dialog-container")));
        new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> {
            Boolean ready = (Boolean) js().executeScript(
                    "var s = document.querySelector('gc-dialog-skeleton');" +
                            "if (!s) return true;" +
                            "var l = s.querySelector('.skeleton');" +
                            "return !l || l.offsetParent === null;");
            return Boolean.TRUE.equals(ready);
        });
        WaitAMomentPlease(0.8f);
    }

    // TODO: botón Aceptar localizado por texto — no se conoce el ID del
    // componente de este módulo.
    private void aceptarDialogo() {
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var c = document.querySelector('mat-dialog-container');" +
                            "if (!c) return false;" +
                            "var b = Array.from(c.querySelectorAll('button')).find(b => {" +
                            "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                            "  return (t === 'aceptar' || t === 'guardar' || t === 'confirmar');" +
                            "});" +
                            "if (b) { b.scrollIntoView({block:'center'}); b.click(); return true; }" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1.0f);

        // Detectar modal de validación (campos obligatorios sin rellenar)
        Boolean hayModalValidacion = (Boolean) js().executeScript(
                "var modals = Array.from(document.querySelectorAll('mat-dialog-container'));" +
                        "return modals.some(m => m.innerText.includes('campos obligatorios') " +
                        "  || m.innerText.includes('completa todos'));");
        if (Boolean.TRUE.equals(hayModalValidacion)) {
            cerrarUnContinuar();
            WaitAMomentPlease(0.5f);
            throw new RuntimeException(
                    "[FAIL] Formulario con campos obligatorios sin rellenar. Revisar rellenarFormulario*().");
        }

        cerrarModalesContinuar();
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d ->
                d.findElements(By.tagName("mat-dialog-container")).isEmpty());
        WaitAMomentPlease(0.5f);
    }

    /**
     * Confirma una acción destructiva. Intenta primero el ID "alert-confirm"
     * (mismo usado en ScaleRecordTest.DeleteScale y
     * DeviceRegistrationTest.handleDeleteConfirmation) y cae al texto si no aparece.
     */
    private void confirmarAccion() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(By.id("alert-confirm")))
                    .click();
            WaitAMomentPlease(0.5f);
            return;
        } catch (TimeoutException ignored) {}

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    (Boolean) js().executeScript(
                            "var btn = Array.from(document.querySelectorAll('button')).find(b => {" +
                                    "  var t = b.textContent.replace(/\\s+/g,' ').trim();" +
                                    "  return (t === 'Confirmar' || t === 'Aceptar' || t === 'Sí')" +
                                    "    && b.offsetParent !== null && !b.disabled;" +
                                    "});" +
                                    "if (btn) { btn.click(); return true; } return false;"));
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException ignored) {}
    }

    /**
     * Cierra un único diálogo "Continuar". Intenta primero el ID
     * "continue-button" y cae al texto si no aparece.
     */
    private void cerrarUnContinuar() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                    .click();
            return;
        } catch (TimeoutException ignored) {}

        js().executeScript(
                "var b = Array.from(document.querySelectorAll('button'))" +
                        "  .find(b => b.textContent.trim() === 'Continuar' && b.offsetParent !== null);" +
                        "if (b) b.click();");
    }

    /**
     * Cierra en cadena los diálogos informativos "Continuar". Intenta el ID
     * "continue-button" (usado en toda la app, ver ClassBaseTest) en cada
     * iteración y cae al texto original si no aparece.
     */
    private void cerrarModalesContinuar() {
        for (int i = 0; i < 5; i++) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(2))
                        .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                        .click();
                WaitAMomentPlease(0.5f);
                continue;
            } catch (TimeoutException ignored) {}

            try {
                new WebDriverWait(driver, Duration.ofSeconds(2)).until(d ->
                        (Boolean) js().executeScript(
                                "var b = Array.from(document.querySelectorAll('button'))" +
                                        "  .find(b => b.textContent.trim() === 'Continuar' && !b.disabled);" +
                                        "if (b) { b.click(); return true; } return false;"));
                WaitAMomentPlease(0.5f);
            } catch (TimeoutException e) {
                break;
            }
        }
    }

    /** Cuenta filas visibles (selectable o mat-row) en la vista actual. */
    private long contarFilas() {
        Long count = (Long) js().executeScript(
                "return Array.from(document.querySelectorAll(" +
                        "  'tr.selectable, tr.mat-row, mat-row'))" +
                        "  .filter(f => f.offsetParent !== null).length;");
        return count != null ? count : 0L;
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }
}
