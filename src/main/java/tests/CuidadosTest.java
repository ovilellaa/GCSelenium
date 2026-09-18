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
 * Test: Circuito Completo de Cuidados
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Buscar paciente y abrir historial
 *   3.  Navegar a Tratamientos > Cuidados
 *   TC1: Añadir protocolo y eliminarlo
 *   TC2: Añadir un cuidado y eliminarlo
 *   TC3: Añadir cuidado planificado (Indefinido) y eliminarlo
 *   TC4: Añadir cuidado planificado (Planificado) y eliminarlo
 *   TC5: Añadir cuidado planificado, finalizarlo y eliminarlo
 *   TC6: Añadir un cuidado, mover su pauta y eliminarlo
 *
 *
 * NOTA sobre aceptarDialogo():
 *   Tras pulsar Aceptar, la app puede abrir un modal informativo de validación
 *   ("Por favor completa todos los campos...") que también es un mat-dialog-container.
 *   El método cierra ese modal con "Continuar" y luego espera a que TODOS los
 *   mat-dialog-container desaparezcan.
 *
 * NOTA DE MIGRACIÓN (desde CarlosFreire): rellenarCuidadoPlanificado() y
 * seleccionarHoraDestinoMoverPauta() ya usan IDs reales verificados en la app
 * ("care", "careType", "careDate", "hour", "schedule", "length", "careHour")
 * y no se han tocado. El resto de navegación/menús sigue localizando por
 * texto visible — se ha sustituido por ID solo en los diálogos genéricos
 * compartidos por toda la app (alertas vitales, "Continuar", confirmación).
 * Lo específico del módulo queda marcado con TODO.
 */
public class CuidadosTest extends ClassBaseTest {

    // ══════════════════════════════════════════════════════════════════
    // SETUP — Login y navegación hasta Cuidados
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 1)
    public void loginMedico() {
        LoginAsDoctor();
        wait.until(d -> !d.getCurrentUrl().contains("/login"));
        Reporter.log("Login completado — URL: " + driver.getCurrentUrl());
    }

    @Test(priority = 10, dependsOnMethods = "loginMedico")
    public void buscarYAbrirHistorial() {
        // TODO: "Carlos Perez" hardcodeado — ver nota en DietsTest.buscarYAbrirHistorial.
        wait.until(ExpectedConditions.elementToBeClickable(By.id("search-action"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("spotlight-search")))
                .sendKeys("Carlos Perez");

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
    public void navegarACuidados() {
        // TODO: navegación por texto visible — ver nota en DietsTest.navegarADietas.
        WaitAMomentPlease(1.5f);

        Boolean tratamientosClickado = (Boolean) js().executeScript(
                "var all = Array.from(document.querySelectorAll(" +
                        "  'a, button, mat-list-item, [role=\"tab\"], span, li'));" +
                        "var target = all.find(el => {" +
                        "  var txt = el.textContent.trim().toLowerCase();" +
                        "  return txt === 'tratamientos' && el.offsetParent !== null;" +
                        "});" +
                        "if (target) { target.click(); return true; } return false;");
        if (Boolean.TRUE.equals(tratamientosClickado))
            Reporter.log("Click en Tratamientos");

        WaitAMomentPlease(1.0f);

        Boolean cuidadosClickado = (Boolean) js().executeScript(
                "var all = Array.from(document.querySelectorAll(" +
                        "  'a, button, mat-list-item, [role=\"tab\"], span, li'));" +
                        "var target = all.find(el => {" +
                        "  var txt = el.textContent.trim().toLowerCase();" +
                        "  return txt === 'cuidados' && el.offsetParent !== null;" +
                        "});" +
                        "if (target) { target.click(); return true; } return false;");
        if (Boolean.TRUE.equals(cuidadosClickado))
            Reporter.log("Click en Cuidados");

        WaitAMomentPlease(2.0f);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//button[@id='actions-button'] | //*[contains(text(),'Planificación')]")));

        Reporter.log("Módulo Cuidados cargado — URL: " + driver.getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════
    // TC1 — AÑADIR PROTOCOLO Y ELIMINARLO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 30, dependsOnMethods = "navegarACuidados")
    public void tc1_añadirYEliminarProtocolo() {
        long antes = contarFilasCuidado();
        Reporter.log("Cuidados antes: " + antes);

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir protocolo");

        esperarDialogoListo();
        seleccionarPrimerProtocolo();
        seleccionarTodosCheckboxProtocolo();
        aceptarDialogo();

        long[] trasAnadir = {contarFilasCuidado()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasAnadir[0] = contarFilasCuidado();
            return trasAnadir[0] > antes;
        });
        Reporter.log("Cuidados tras añadir: " + trasAnadir[0]);
        Assert.assertTrue(trasAnadir[0] > antes, "Deben haberse añadido cuidados del protocolo");
        Reporter.log("Protocolo añadido");

        WaitAMomentPlease(1.0f);
        int intentos = 0;
        while (contarFilasCuidado() > antes && intentos < 20) {
            try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
            catch (Exception ignored) {}
            WaitAMomentPlease(0.5f);
            seleccionarPrimeraFilaSelectable();
            abrirMenuAcciones();
            clickMenuItem("Eliminar pauta");
            confirmarAccion();
            cerrarModalesContinuar();
            WaitAMomentPlease(1.0f);
            intentos++;
        }

        long trasEliminar = contarFilasCuidado();
        Reporter.log("Cuidados tras eliminar: " + trasEliminar);
        Assert.assertTrue(trasEliminar <= antes, "Deben haberse eliminado todos los cuidados");
        Reporter.log("TC1 completado — Protocolo añadido y eliminado");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC2 — AÑADIR UN CUIDADO Y ELIMINARLO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = "tc1_añadirYEliminarProtocolo")
    public void tc2_añadirYEliminarCuidado() {
        long antes = contarFilasCuidado();
        Reporter.log("Cuidados antes: " + antes);

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir cuidados");

        esperarDialogoListo();
        seleccionarPrimerItemListaCuidados();
        aceptarDialogo();

        long[] despues = {contarFilasCuidado()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            despues[0] = contarFilasCuidado();
            return despues[0] > antes;
        });
        Reporter.log("Cuidados tras añadir: " + despues[0]);
        Assert.assertTrue(despues[0] > antes, "Debe haberse añadido el cuidado");
        Reporter.log("Cuidado añadido");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Eliminar pauta");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);

        long[] trasEliminar = {contarFilasCuidado()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasEliminar[0] = contarFilasCuidado();
            return trasEliminar[0] < despues[0];
        });
        Reporter.log("Cuidados tras eliminar: " + trasEliminar[0]);
        Assert.assertTrue(trasEliminar[0] < despues[0], "Debe haberse eliminado el cuidado");
        Reporter.log("TC2 completado — Cuidado añadido y eliminado");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC3 — AÑADIR CUIDADO PLANIFICADO (INDEFINIDO) Y ELIMINARLO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 50, dependsOnMethods = "tc2_añadirYEliminarCuidado")
    public void tc3_añadirYEliminarCuidadoPlanificadoIndefinido() {
        long antes = contarFilasCuidado();
        Reporter.log("Cuidados antes: " + antes);

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir cuidado planificado");

        esperarDialogoListo();
        rellenarCuidadoPlanificado("Indefinido");
        aceptarDialogo();

        // El cuidado puede ser de hoy o mañana según la fecha; esperamos aumento de fila
        long[] despues = {contarFilasCuidadoTotal()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            despues[0] = contarFilasCuidadoTotal();
            return despues[0] > antes;
        });
        Reporter.log("Cuidados tras añadir: " + despues[0]);
        Assert.assertTrue(despues[0] > antes, "Debe haberse añadido el cuidado planificado Indefinido");
        Reporter.log("Cuidado planificado Indefinido añadido");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Eliminar pauta");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);

        long[] trasEliminar = {contarFilasCuidadoTotal()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasEliminar[0] = contarFilasCuidadoTotal();
            return trasEliminar[0] < despues[0];
        });
        Reporter.log("Cuidados tras eliminar: " + trasEliminar[0]);
        Assert.assertTrue(trasEliminar[0] < despues[0], "Debe haberse eliminado el cuidado");
        Reporter.log("TC3 completado — Cuidado planificado Indefinido añadido y eliminado");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC4 — AÑADIR CUIDADO PLANIFICADO (PLANIFICADO) Y ELIMINARLO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 60, dependsOnMethods = "tc3_añadirYEliminarCuidadoPlanificadoIndefinido")
    public void tc4_añadirYEliminarCuidadoPlanificadoPlanificado() {
        long antes = contarFilasCuidadoTotal();
        Reporter.log("Cuidados antes: " + antes);

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir cuidado planificado");

        esperarDialogoListo();
        rellenarCuidadoPlanificado("Planificado");
        aceptarDialogo();

        long[] despues = {contarFilasCuidadoTotal()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            despues[0] = contarFilasCuidadoTotal();
            return despues[0] > antes;
        });
        Reporter.log("Cuidados tras añadir: " + despues[0]);
        Assert.assertTrue(despues[0] > antes, "Debe haberse añadido el cuidado planificado Planificado");
        Reporter.log("Cuidado planificado Planificado añadido");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Eliminar pauta");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);

        long[] trasEliminar = {contarFilasCuidadoTotal()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasEliminar[0] = contarFilasCuidadoTotal();
            return trasEliminar[0] < despues[0];
        });
        Reporter.log("Cuidados tras eliminar: " + trasEliminar[0]);
        Assert.assertTrue(trasEliminar[0] < despues[0], "Debe haberse eliminado el cuidado");
        Reporter.log("TC4 completado — Cuidado planificado Planificado añadido y eliminado");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC5 — AÑADIR CUIDADO PLANIFICADO, FINALIZARLO Y ELIMINARLO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 70, dependsOnMethods = "tc4_añadirYEliminarCuidadoPlanificadoPlanificado")
    public void tc5_añadirFinalizarYEliminarCuidadoPlanificado() {
        long antes = contarFilasCuidadoTotal();

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir cuidado planificado");

        esperarDialogoListo();
        rellenarCuidadoPlanificado("Indefinido");
        aceptarDialogo();

        long[] trasAnadir = {contarFilasCuidadoTotal()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasAnadir[0] = contarFilasCuidadoTotal();
            return trasAnadir[0] > antes;
        });
        Reporter.log("Cuidados tras añadir: " + trasAnadir[0]);
        Assert.assertTrue(trasAnadir[0] > antes, "Debe haberse añadido el cuidado");
        Reporter.log("Cuidado planificado añadido");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Finalizar pauta");
        WaitAMomentPlease(0.5f);
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);
        Reporter.log("Cuidado finalizado");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Eliminar pauta");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);

        long[] trasEliminar = {contarFilasCuidadoTotal()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasEliminar[0] = contarFilasCuidadoTotal();
            return trasEliminar[0] < trasAnadir[0];
        });
        Reporter.log("Cuidados tras eliminar: " + trasEliminar[0]);
        Assert.assertTrue(trasEliminar[0] < trasAnadir[0], "Debe haberse eliminado el cuidado");
        Reporter.log("TC5 completado — Cuidado añadido, finalizado y eliminado");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC6 — AÑADIR CUIDADO, MOVER PAUTA Y ELIMINARLO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 80, dependsOnMethods = "tc5_añadirFinalizarYEliminarCuidadoPlanificado")
    public void tc6_añadirMoverPautaYEliminarCuidado() {
        long antes = contarFilasCuidado();

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir cuidados");

        esperarDialogoListo();
        seleccionarPrimerItemListaCuidados();
        aceptarDialogo();

        long[] trasAnadir = {contarFilasCuidado()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasAnadir[0] = contarFilasCuidado();
            return trasAnadir[0] > antes;
        });
        Assert.assertTrue(trasAnadir[0] > antes, "Debe haberse añadido el cuidado");
        Reporter.log("Cuidado añadido");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Mover Pauta");

        esperarDialogoListo();
        seleccionarHoraDestinoMoverPauta("11:00");
        aceptarDialogo();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);
        Reporter.log("Pauta movida a las 11:00");

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarPrimeraFilaSelectable();
        abrirMenuAcciones();
        clickMenuItem("Eliminar pauta");
        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(1.5f);

        long[] trasEliminar = {contarFilasCuidado()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasEliminar[0] = contarFilasCuidado();
            return trasEliminar[0] < trasAnadir[0];
        });
        Assert.assertTrue(trasEliminar[0] < trasAnadir[0], "Debe haberse eliminado el cuidado");

        Reporter.log("CIRCUITO DE CUIDADOS COMPLETADO EXITOSAMENTE");
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private void abrirMenuAcciones() {
        WebElement btn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        WaitAMomentPlease(0.3f);
        js().executeScript("arguments[0].click();", btn);
        WaitAMomentPlease(0.8f);
    }

    // TODO: ítems de menú específicos de Cuidados localizados por texto —
    // no se conoce su ID real. Sustituir cuando se valide contra QA.
    private void clickMenuItem(String texto) {
        String needle = texto.toLowerCase();
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle = '" + needle.replace("'", "\\'") + "';" +
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
    }

    private void seleccionarPrimeraFilaSelectable() {
        WaitAMomentPlease(0.5f);
        Boolean ok = (Boolean) js().executeScript(
                "var filas = Array.from(document.querySelectorAll('tr.selectable'))" +
                        "  .filter(f => f.offsetParent !== null);" +
                        "if (filas[0]) { filas[0].click(); return true; } return false;");
        if (!Boolean.TRUE.equals(ok))
            throw new RuntimeException("No se encontró ninguna fila selectable en la tabla de cuidados");
        Reporter.log("Primera fila seleccionada");
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

    /**
     * Pulsa Aceptar en el diálogo activo y espera a que desaparezca.
     *
     * Flujo real de la app:
     *   1. Click en "Aceptar"
     *   2. Si hay campos inválidos → app muestra modal informativo con "Continuar"
     *      → lo cerramos y lanzamos RuntimeException para que el test falle limpiamente
     *   3. Si todo OK → el diálogo principal se cierra solo
     *
     * TODO: el botón "Aceptar" del propio formulario se sigue localizando por
     * texto (no se conoce el ID del componente de este módulo).
     */
    private void aceptarDialogo() {
        // Paso 1 — click en Aceptar
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var c = document.querySelector('mat-dialog-container');" +
                            "if (!c) return false;" +
                            "var b = Array.from(c.querySelectorAll('button')).find(b => {" +
                            "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                            "  return (t === 'aceptar' || t === 'guardar' || t === 'confirmar')" +
                            "    && !b.disabled && b.offsetParent !== null;" +
                            "});" +
                            "if (b) { b.scrollIntoView({block:'center'}); b.click(); return true; }" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1.0f);

        // Paso 2 — si aparece modal de validación ("Por favor completa todos los campos")
        //          lo detectamos por su texto y lanzamos excepción
        Boolean hayModalValidacion = (Boolean) js().executeScript(
                "var modals = Array.from(document.querySelectorAll('mat-dialog-container'));" +
                        "return modals.some(m => m.innerText.includes('campos obligatorios') " +
                        "  || m.innerText.includes('completa todos'));");
        if (Boolean.TRUE.equals(hayModalValidacion)) {
            cerrarUnContinuar();
            WaitAMomentPlease(0.5f);
            throw new RuntimeException(
                    "[FAIL] El formulario tiene campos obligatorios sin rellenar. " +
                            "Revisar rellenarCuidadoPlanificado().");
        }

        // Paso 3 — cerrar posibles modales "Continuar" de confirmación y
        //          esperar a que todos los mat-dialog-container desaparezcan
        cerrarModalesContinuar();
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d ->
                d.findElements(By.tagName("mat-dialog-container")).isEmpty());
        WaitAMomentPlease(0.5f);
    }

    // ─── Diálogo "Añadir protocolo" ───────────────────────────────────

    // TODO: selects/checkboxes del diálogo "Añadir protocolo" localizados
    // por posición/tag genérico, no por ID — módulo específico sin verificar.
    private void seleccionarPrimerProtocolo() {
        wait.until(d -> (Boolean) js().executeScript(
                "var dialog = document.querySelector('mat-dialog-container');" +
                        "if (!dialog) return false;" +
                        "var sel = dialog.querySelector('mat-select, .mat-select-trigger');" +
                        "if (sel) { sel.click(); return true; } return false;"));
        WaitAMomentPlease(0.8f);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> (Boolean) js().executeScript(
                        "var opts = Array.from(document.querySelectorAll(" +
                                "  'mat-option, .mat-option')).filter(o => o.offsetParent !== null);" +
                                "if (opts[0]) { opts[0].scrollIntoView({block:'center'}); opts[0].click(); return true; }" +
                                "return false;"));

        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                (Boolean) js().executeScript(
                        "var dialog = document.querySelector('mat-dialog-container');" +
                                "if (!dialog) return false;" +
                                "return dialog.querySelectorAll('mat-checkbox').length > 1;"));
        Reporter.log("Protocolo seleccionado");
        WaitAMomentPlease(0.5f);
    }

    private void seleccionarTodosCheckboxProtocolo() {
        js().executeScript(
                "var dialog = document.querySelector('mat-dialog-container');" +
                        "if (dialog) { var cb = dialog.querySelector('mat-checkbox'); if (cb) cb.click(); }");
        WaitAMomentPlease(0.5f);
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                (Boolean) js().executeScript(
                        "var dialog = document.querySelector('mat-dialog-container');" +
                                "if (!dialog) return false;" +
                                "return dialog.querySelector('mat-checkbox.mat-checkbox-checked') !== null;"));
        Reporter.log("Todos los cuidados del protocolo seleccionados");
    }

    // ─── Diálogo "Añadir cuidados" (lista con checkboxes) ─────────────

    // TODO: lista de cuidados localizada por tag genérico — módulo específico sin verificar.
    private void seleccionarPrimerItemListaCuidados() {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                (Boolean) js().executeScript(
                        "var dialog = document.querySelector('mat-dialog-container');" +
                                "if (!dialog) return false;" +
                                "return dialog.querySelectorAll('tr, mat-row, li').length > 0;"));

        Boolean ok = (Boolean) js().executeScript(
                "var dialog = document.querySelector('mat-dialog-container');" +
                        "if (!dialog) return false;" +
                        "var filas = Array.from(dialog.querySelectorAll('tr.selectable, tr.mat-row, mat-row'))" +
                        "  .filter(f => f.offsetParent !== null);" +
                        "if (filas.length > 0) { filas[0].click(); return true; }" +
                        "var checks = Array.from(dialog.querySelectorAll('mat-checkbox'))" +
                        "  .filter(c => c.offsetParent !== null);" +
                        "var target = checks[1] || checks[0];" +
                        "if (target) { target.click(); return true; }" +
                        "return false;");

        if (!Boolean.TRUE.equals(ok))
            throw new RuntimeException("No se encontró ningún cuidado en 'Añadir cuidados'");
        Reporter.log("Primer cuidado de la lista seleccionado");
        WaitAMomentPlease(0.5f);
    }

    // ─── Diálogo "Añadir cuidado planificado" ─────────────────────────

    /**
     * Rellena completamente el diálogo usando los IDs reales de la app:
     *
     *   #care      → INPUT autocomplete: escribe "Aislamiento" y elige la 1ª opción
     *   #careType  → MAT-SELECT: elige el tipo recibido por parámetro
     *   #careDate  → INPUT datepicker: triple click (selección nativa) + fecha dd/MM/yyyy + TAB
     *               NOTA: CTRL+A no funciona en mat-datepicker de Angular; triple click es
     *               la forma correcta de seleccionar todo el contenido del campo.
     *   #hour      → MAT-SELECT: elige "09:00" (o la primera disponible)
     *   #schedule  → INPUT: escribe "1d" (formato válido para Pauta)
     *   #length    → INPUT (Duración):
     *               - Tipo "Indefinido" → disabled, se autocalcula sólo
     *               - Tipo "Planificado" → habilitado y OBLIGATORIO, escribe "1d"
     *
     * @param tipoCuidado "Indefinido" o "Planificado"
     */
    private void rellenarCuidadoPlanificado(String tipoCuidado) {
        Reporter.log("Rellenando cuidado planificado (tipo=" + tipoCuidado + ")");

        // 1. Campo "Cuidado" — INPUT autocomplete id="care"
        WebElement inputCare = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("care")));
        inputCare.sendKeys("Aislamiento");
        WaitAMomentPlease(0.8f);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> (Boolean) js().executeScript(
                        "var opts = Array.from(document.querySelectorAll(" +
                                "  'mat-option, .mat-option')).filter(o => o.offsetParent !== null);" +
                                "if (opts[0]) { opts[0].scrollIntoView({block:'center'}); opts[0].click(); return true; }" +
                                "return false;"));
        Reporter.log("Cuidado seleccionado en autocomplete");
        WaitAMomentPlease(0.5f);

        // 2. Campo "Tipo de cuidado" — MAT-SELECT id="careType"
        WebElement selectTipo = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("careType")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", selectTipo);
        selectTipo.click();
        WaitAMomentPlease(0.8f);

        String needleTipo = tipoCuidado.toLowerCase();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> (Boolean) js().executeScript(
                        "var needle = '" + needleTipo + "';" +
                                "var opts = Array.from(document.querySelectorAll(" +
                                "  'mat-option, .mat-option')).filter(o => o.offsetParent !== null);" +
                                "var t = opts.find(o => o.textContent.trim().toLowerCase() === needle);" +
                                "if (!t) t = opts.find(o => o.textContent.trim().toLowerCase().includes(needle));" +
                                "if (t) { t.scrollIntoView({block:'center'}); t.click(); return true; }" +
                                "return false;"));
        Reporter.log("Tipo de cuidado seleccionado: " + tipoCuidado);
        WaitAMomentPlease(0.5f);

        // 3. Campo "Fecha" — INPUT datepicker id="careDate"
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        WebElement inputFecha = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("careDate")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", inputFecha);
        inputFecha.click();
        inputFecha.sendKeys(Keys.chord(Keys.CONTROL, "a")); // seleccionar todo
        inputFecha.sendKeys(fechaHoy);                      // sobreescribir
        inputFecha.sendKeys(Keys.TAB);                      // confirmar con Angular
        WaitAMomentPlease(0.5f);
        String fechaValor = (String) js().executeScript("return document.getElementById('careDate').value;");
        Reporter.log("Fecha rellenada: " + fechaValor);

        // 4. Campo "Hora" — MAT-SELECT id="hour"
        WebElement selectHora = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("hour")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", selectHora);
        selectHora.click();
        WaitAMomentPlease(0.8f);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> (Boolean) js().executeScript(
                        "var opts = Array.from(document.querySelectorAll(" +
                                "  'mat-option, .mat-option')).filter(o => o.offsetParent !== null);" +
                                "var t = opts.find(o => o.textContent.trim() === '09:00');" +
                                "if (!t) t = opts[0];" +
                                "if (t) { t.scrollIntoView({block:'center'}); t.click(); return true; }" +
                                "return false;"));
        Reporter.log("Hora seleccionada");
        WaitAMomentPlease(0.5f);

        // 5. Campo "Pauta" — INPUT id="schedule"  formato: "1d"
        WebElement inputSchedule = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("schedule")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", inputSchedule);
        inputSchedule.click();
        inputSchedule.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        inputSchedule.sendKeys("1d");
        inputSchedule.sendKeys(Keys.TAB);
        Reporter.log("Pauta rellenada: 1d");
        WaitAMomentPlease(0.5f);

        // 6. Campo "Duración" — INPUT id="length"
        //    Indefinido  → disabled, se autocalcula. No tocar.
        //    Planificado → habilitado y OBLIGATORIO. Mismo formato que Pauta.
        try {
            Boolean lengthDisabled = (Boolean) js().executeScript(
                    "return document.getElementById('length').disabled;");
            if (!Boolean.TRUE.equals(lengthDisabled)) {
                WebElement inputLength = driver.findElement(By.id("length"));
                js().executeScript("arguments[0].scrollIntoView({block:'center'});", inputLength);
                inputLength.click();
                inputLength.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                inputLength.sendKeys("1d");
                inputLength.sendKeys(Keys.TAB);
                Reporter.log("Duración rellenada: 1d");
            } else {
                Reporter.log("Duración deshabilitada — se autocalcula");
            }
        } catch (Exception e) {
            Reporter.log("[WARN] No se pudo rellenar Duración: " + e.getMessage());
        }
        WaitAMomentPlease(0.5f);
    }

    // ─── Diálogo "Mover Pauta" ─────────────────────────────────────────

    /**
     * Selecciona la hora destino en el diálogo "Mover Pauta".
     *
     * IDs reales del diálogo (inspeccionados en la app):
     *   #scheduleHourOrigen → MAT-SELECT hora origen (ya preseleccionada, no tocar)
     *   #careDate           → INPUT fecha destino (ya tiene hoy por defecto)
     *   #careHour           → MAT-SELECT hora destino (OBLIGATORIO, vacío al abrir)
     */
    private void seleccionarHoraDestinoMoverPauta(String hora) {
        Reporter.log("Seleccionando hora destino: " + hora);
        try {
            WebElement selectHoraDestino = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("careHour")));
            js().executeScript("arguments[0].scrollIntoView({block:'center'});", selectHoraDestino);
            selectHoraDestino.click();
            WaitAMomentPlease(0.8f);

            String prefix = hora.split(":")[0];
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> (Boolean) js().executeScript(
                            "var needle = '" + hora + "'; var prefix = '" + prefix + "';" +
                                    "var opts = Array.from(document.querySelectorAll(" +
                                    "  'mat-option, .mat-option')).filter(o => o.offsetParent !== null);" +
                                    "var t = opts.find(o => o.textContent.trim() === needle);" +
                                    "if (!t) t = opts.find(o => o.textContent.trim().startsWith(prefix));" +
                                    "if (!t) t = opts[0];" +
                                    "if (t) { t.scrollIntoView({block:'center'}); t.click(); return true; }" +
                                    "return false;"));
            Reporter.log("Hora destino seleccionada: " + hora);
        } catch (Exception e) {
            Reporter.log("[WARN] Error seleccionando hora destino: " + e.getMessage());
        }
        WaitAMomentPlease(0.5f);
    }

    // ─── Helpers genéricos ─────────────────────────────────────────────

    /**
     * Confirma una acción destructiva (eliminar/finalizar pauta). Intenta
     * primero el ID "alert-confirm" (mismo usado en ScaleRecordTest.DeleteScale
     * y DeviceRegistrationTest.handleDeleteConfirmation) y cae al texto
     * original si no aparece.
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

    private void cerrarAlertaSiExiste() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d ->
                    (Boolean) js().executeScript(
                            "var b = Array.from(document.querySelectorAll('button'))" +
                                    "  .find(b => b.textContent.trim() === 'Aceptar' && b.offsetParent !== null);" +
                                    "if (b) { b.click(); return true; } return false;"));
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException ignored) {}
    }

    /**
     * Cierra un único diálogo "Continuar" (usado tras el modal de validación).
     * Intenta primero el ID "continue-button" y cae al texto si no aparece.
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

    /**
     * Cuenta las filas visibles en la vista "Planificación (Hoy)".
     * Solo muestra cuidados con pauta para HOY.
     */
    private long contarFilasCuidado() {
        Long count = (Long) js().executeScript(
                "return Array.from(document.querySelectorAll('tr.selectable'))" +
                        "  .filter(f => f.offsetParent !== null).length;");
        return count != null ? count : 0L;
    }

    /**
     * Cuenta las filas en cualquier vista del módulo de cuidados.
     * Usado en TC3/TC4/TC5 donde la fecha del cuidado puede ser mañana
     * y no aparece en la vista "Hoy". Si la vista de hoy está vacía,
     * navega un día adelante para contar y vuelve a la vista de hoy.
     *
     * TODO (bug potencial detectado en revisión): el intento de "volver a
     * hoy" (líneas de más abajo) no verifica que el click realmente tenga
     * efecto — si el botón "chevron_left" no aparece o el click falla
     * silenciosamente, la vista puede quedar en "mañana" para los pasos
     * siguientes del test (seleccionarPrimeraFilaSelectable, etc.). No se
     * ha corregido sin poder validarlo contra la app real.
     */
    private long contarFilasCuidadoTotal() {
        // Primero contamos en la vista actual (hoy)
        Long countHoy = (Long) js().executeScript(
                "return Array.from(document.querySelectorAll('tr.selectable'))" +
                        "  .filter(f => f.offsetParent !== null).length;");
        long hoy = countHoy != null ? countHoy : 0L;

        if (hoy > 0) return hoy;

        // Si 0 en vista hoy, avanzar al día siguiente para verificar si está ahí
        Boolean avanzado = (Boolean) js().executeScript(
                "var btn = document.getElementById('down-arrow-button');" +
                        "if (!btn) {" +
                        "  btn = Array.from(document.querySelectorAll('button'))" +
                        "    .find(b => b.querySelector('mat-icon') && " +
                        "              b.querySelector('mat-icon').textContent.trim() === 'chevron_right');" +
                        "}" +
                        "if (btn) { btn.click(); return true; } return false;");

        if (Boolean.TRUE.equals(avanzado)) {
            WaitAMomentPlease(1.0f);
            Long countManana = (Long) js().executeScript(
                    "return Array.from(document.querySelectorAll('tr.selectable'))" +
                            "  .filter(f => f.offsetParent !== null).length;");
            long manana = countManana != null ? countManana : 0L;

            // Volver a hoy
            js().executeScript(
                    "var btns = Array.from(document.querySelectorAll('button'))" +
                            "  .filter(b => b.querySelector('mat-icon') && " +
                            "              b.querySelector('mat-icon').textContent.trim() === 'chevron_left');" +
                            "if (btns[0]) btns[0].click();");
            WaitAMomentPlease(0.8f);

            return hoy + manana;
        }
        return hoy;
    }
}
