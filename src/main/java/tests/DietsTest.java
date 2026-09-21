package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test: Circuito Completo de Dietas
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Buscar paciente y abrir historial
 *   3.  Navegar a Tratamientos > Dietas
 *   TC1: Crear dieta (Ayuno) y eliminarla en el mismo test
 *   TC2: Crear una nueva dieta (Ayuno)
 *   TC3: Finalizar la dieta del TC2
 *
 * NOTA DE MIGRACIÓN (desde CarlosFreire): los selectores de menú, campos de
 * formulario y filas de grid de este módulo son específicos de la pantalla
 * de Dietas y no se han podido verificar contra la app real (localizan por
 * texto visible en vez de ID estable). Se han sustituido por IDs ya
 * verificados en el resto del proyecto SOLO los diálogos genéricos
 * compartidos por toda la app (alertas vitales, "Continuar", confirmación
 * de eliminación). El resto queda marcado con TODO para endurecer cuando
 * se valide este test contra el entorno real.
 */
public class DietsTest extends ClassBaseTest {

    // ══════════════════════════════════════════════════════════════════
    // SETUP — Login y navegación hasta Dietas
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
        // del proyecto, en vez del nombre hardcodeado del original.
        String nh = ConfigReader.get("pacqah1NH");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("search-action"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("spotlight-search")))
                .sendKeys(nh);

        wait.until(ExpectedConditions.elementToBeClickable(By.id("spotlight-list-item-0-0"))).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("quickAction-PATIENT_SEE_HISTORY"))).click();

        // Modal de motivo de acceso a historia ajena (paciente no asignado al
        // usuario en sesión) — heredado de ClassBaseTest, mismo que usan todos
        // los tests de DonovanSaucedo. El original de CarlosFreire no lo
        // gestionaba porque probó con un paciente ya asignado a él; con el
        // paciente estándar de configuración (pacqah1NH) sí aparece.
        SelectAccessReason("Guardia");

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
        Reporter.log("Foco en nueva pestaña: " + driver.getCurrentUrl());

        // Heredado de ClassBaseTest — mismo ID (accept-AlertsContainer-button)
        // que usaba aquí el try/catch original.
        handleVitalAlertsDialog();

        WaitAMomentPlease(1.0f);
        Reporter.log("Historial abierto");
    }

    @Test(priority = 20, dependsOnMethods = "buscarYAbrirHistorial")
    public void navegarADietas() {
        // Verificado contra QA (2026-09-18): el sidebar expone el ID real
        // "treatments-sidebar" para "Tratamientos". El submenú "Dietas" que
        // aparece tras expandirlo (distinto del módulo de nivel superior
        // "Dietas y recomendaciones", id
        // diets_and_recommendations-sidebar) no tiene ID propio confirmado,
        // así que se sigue localizando por texto exacto.
        wait.until(ExpectedConditions.elementToBeClickable(By.id("treatments-sidebar"))).click();
        Reporter.log("Click en Tratamientos");

        Boolean dietasClickado = wait.until(d -> (Boolean) js().executeScript(
                "var all = Array.from(document.querySelectorAll('a, button, mat-list-item, [role=\"tab\"], span, li'));" +
                        "var target = all.find(el => {" +
                        "  var txt = el.textContent.trim().toLowerCase();" +
                        "  return txt === 'dietas' && el.offsetParent !== null;" +
                        "});" +
                        "if (target) { target.click(); return true; } return false;"
        ));
        if (Boolean.TRUE.equals(dietasClickado)) Reporter.log("Click en Dietas");

        WaitAMomentPlease(2.0f);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//button[@id='actions-button'] | //*[contains(text(),'Dietas')]")));

        Reporter.log("Módulo Dietas cargado — URL: " + driver.getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════
    // TC1 — CREAR DIETA (AYUNO) Y ELIMINARLA EN EL MISMO TEST
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 30, dependsOnMethods = "navegarADietas")
    public void tc1_crearYEliminarDieta() {
        // — CREAR ——————————————————————————————————————————————————————
        long antes = contarFilasDieta();
        Reporter.log("Dietas antes de crear: " + antes);

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir nutrición oral");

        esperarDialogoListo();
        seleccionarTipoDieta("Ayuno");
        aceptarDialogo();

        long[] trasCrear = {contarFilasDieta()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasCrear[0] = contarFilasDieta();
            return trasCrear[0] > antes;
        });
        Reporter.log("Dietas tras crear: " + trasCrear[0]);
        Assert.assertTrue(trasCrear[0] > antes, "Debe haberse creado la dieta");
        Reporter.log("Dieta creada");

        // — ELIMINAR ———————————————————————————————————————————————————
        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarFilaSinFechaFin();
        abrirMenuAcciones();
        clickMenuItem("Eliminar dieta");

        WaitAMomentPlease(1.0f);
        cerrarDialogoInformativo();
        confirmarEliminacion();
        WaitAMomentPlease(2.0f);
        cerrarModalesContinuar();

        long[] trasEliminar = {contarFilasDieta()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            trasEliminar[0] = contarFilasDieta();
            return trasEliminar[0] < trasCrear[0];
        });
        Reporter.log("Dietas tras eliminar: " + trasEliminar[0]);
        Assert.assertTrue(trasEliminar[0] < trasCrear[0], "Debe haberse eliminado la dieta");
        Reporter.log("TC1 completado — Dieta creada y eliminada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC2 — CREAR NUEVA DIETA (AYUNO)
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = "tc1_crearYEliminarDieta")
    public void tc2_crearDieta() {
        long antes = contarFilasDieta();
        Reporter.log("Dietas antes: " + antes);

        cerrarAlertaSiExiste();
        abrirMenuAcciones();
        clickMenuItem("Añadir nutrición oral");

        esperarDialogoListo();
        seleccionarTipoDieta("Ayuno");
        aceptarDialogo();

        long[] despues = {contarFilasDieta()};
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            despues[0] = contarFilasDieta();
            return despues[0] > antes;
        });
        Reporter.log("Dietas después: " + despues[0]);
        Assert.assertTrue(despues[0] > antes, "Debe haberse creado la dieta");
        Reporter.log("TC2 completado — Dieta AYUNO creada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC3 — FINALIZAR LA DIETA DEL TC2
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 50, dependsOnMethods = "tc2_crearDieta")
    public void tc3_finalizarDieta() {
        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
        WaitAMomentPlease(1.0f);

        seleccionarFilaSinFechaFin();
        abrirMenuAcciones();
        clickMenuItem("Finalizar dieta");

        WaitAMomentPlease(1.0f);
        cerrarDialogoInformativo();

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> (Boolean) js().executeScript(
                    "var btn = Array.from(document.querySelectorAll('button')).find(b => {" +
                            "  var t = b.textContent.replace(/\\s+/g,' ').trim();" +
                            "  return (t === 'Confirmar' || t === 'Aceptar' || t === 'Sí') && b.offsetParent !== null && !b.disabled;" +
                            "});" +
                            "if (btn) { btn.click(); return true; } return false;"));
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException ignored) {}

        WaitAMomentPlease(2.0f);
        cerrarModalesContinuar();

        // Verificar que la dieta ahora tiene fecha fin (ya no muestra '-')
        Boolean hayFilaFinalizada = (Boolean) js().executeScript(
                "var filas = Array.from(document.querySelectorAll('mat-row, tr'))" +
                        "  .filter(f => f.offsetParent !== null && f.textContent.trim().length > 5);" +
                        "return filas.some(f => {" +
                        "  var celdas = Array.from(f.querySelectorAll('mat-cell, td'));" +
                        "  return celdas.some(c => c.textContent.trim() !== '-' && c.textContent.trim() !== '' && /\\d/.test(c.textContent));" +
                        "});");
        Assert.assertTrue(Boolean.TRUE.equals(hayFilaFinalizada),
                "Debe existir al menos una dieta con fecha fin tras finalizar");

        Reporter.log("CIRCUITO COMPLETADO EXITOSAMENTE");
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private void jsClick(WebElement el) {
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        WaitAMomentPlease(0.3f);
        js().executeScript("arguments[0].click();", el);
    }

    private void abrirMenuAcciones() {
        WebElement btn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        jsClick(btn);
        WaitAMomentPlease(0.8f);
    }

    // TODO: ítems de menú específicos de Dietas ("Añadir nutrición oral",
    // "Eliminar dieta", "Finalizar dieta") localizados por texto — no se
    // conoce su ID real. Sustituir por ID cuando se valide contra QA.
    private void clickMenuItem(String texto) {
        String needle = texto.toLowerCase();
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle = '" + needle.replace("'", "\\'") + "';" +
                            "var items = Array.from(document.querySelectorAll('button[role=\"menuitem\"], .mat-menu-item, button'));" +
                            "var target = items.find(b => b.textContent.trim().toLowerCase() === needle && b.offsetParent !== null && !b.disabled);" +
                            "if (target) { target.click(); return true; }" +
                            "target = items.find(b => b.textContent.trim().toLowerCase().includes(needle) && b.offsetParent !== null && !b.disabled);" +
                            "if (target) { target.click(); return true; } return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
    }

    /**
     * Selecciona la primera fila de dieta activa (sin fecha fin).
     * La celda de fecha fin muestra "-" cuando la dieta está activa.
     *
     * TODO: grid localizado por tag genérico (mat-row/tr). El resto del
     * proyecto usa IDs estables tipo "gridId-{N}" en las filas — verificar
     * si este grid los expone antes de endurecer este helper.
     */
    private void seleccionarFilaSinFechaFin() {
        WaitAMomentPlease(0.5f);
        Boolean encontrada = (Boolean) js().executeScript(
                "var filas = Array.from(document.querySelectorAll('mat-row, tr'))" +
                        "  .filter(f => f.offsetParent !== null && f.textContent.trim().length > 5);" +
                        "var target = filas.find(f => {" +
                        "  var celdas = Array.from(f.querySelectorAll('mat-cell, td'));" +
                        "  return celdas.some(c => c.textContent.trim() === '-' || c.textContent.trim() === '');" +
                        "});" +
                        "if (target) { target.click(); return true; } return false;");
        if (!Boolean.TRUE.equals(encontrada)) {
            throw new RuntimeException(
                    "No se encontró ninguna dieta activa (sin fecha fin)");
        }
        Reporter.log("Fila de dieta activa (sin fecha fin) seleccionada");
        WaitAMomentPlease(0.5f);
    }

    // TODO: dropdown de tipo de dieta localizado por texto de opción —
    // no se conoce el ID del mat-select ni de las mat-option de este módulo.
    private void seleccionarTipoDieta(String tipoDieta) {
        Reporter.log("Seleccionando dieta: " + tipoDieta);

        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> (Boolean) js().executeScript(
                    "var dialog = document.querySelector('mat-dialog-container');" +
                            "if (!dialog) return false;" +
                            "var candidates = Array.from(dialog.querySelectorAll(" +
                            "  'mat-select, [role=\"combobox\"], [role=\"listbox\"], .mat-select-trigger, gc-select, [class*=\"select\"]'" +
                            ")).filter(el => el.offsetParent !== null);" +
                            "if (candidates.length > 0) { candidates[0].click(); return true; }" +
                            "var all = Array.from(dialog.querySelectorAll('*'))" +
                            "  .filter(el => el.offsetParent !== null && el.children.length < 3" +
                            "    && (el.textContent.trim() === 'Dieta' || el.placeholder === 'Dieta'));" +
                            "if (all.length > 0) { all[0].click(); return true; }" +
                            "return false;"));
            WaitAMomentPlease(0.8f);
        } catch (TimeoutException e) {
            Reporter.log("[WARN] No se encontró trigger estándar — el dropdown puede estar ya abierto");
        }

        String needleLower = tipoDieta.toLowerCase();
        Boolean ok = (Boolean) new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> (Boolean) js().executeScript(
                        "var needle = '" + needleLower.replace("'", "\\'") + "';" +
                                "var opts = Array.from(document.querySelectorAll('mat-option, .mat-option'));" +
                                "var target = opts.find(o => o.textContent.trim().toLowerCase() === needle && o.offsetParent !== null);" +
                                "if (!target) target = opts.find(o => o.textContent.trim().toLowerCase().includes(needle) && o.offsetParent !== null);" +
                                "if (!target) {" +
                                "  var overlay = document.querySelector('.cdk-overlay-container, .cdk-overlay-pane, [class*=\"dropdown\"], [class*=\"panel\"]');" +
                                "  if (overlay) {" +
                                "    var items = Array.from(overlay.querySelectorAll('li, div, span, a'))" +
                                "      .filter(el => el.offsetParent !== null && el.children.length === 0);" +
                                "    target = items.find(el => el.textContent.trim().toLowerCase() === needle);" +
                                "    if (!target) target = items.find(el => el.textContent.trim().toLowerCase().includes(needle));" +
                                "  }" +
                                "}" +
                                "if (target) { target.scrollIntoView({block:'center'}); target.click(); return true; }" +
                                "return false;"));

        if (Boolean.TRUE.equals(ok)) {
            Reporter.log("Tipo de dieta seleccionado: " + tipoDieta);
        } else {
            throw new RuntimeException("No se encontró la opción '" + tipoDieta + "' en el dropdown");
        }
        WaitAMomentPlease(0.5f);
    }

    private void esperarDialogoListo() {
        new WebDriverWait(driver, Duration.ofSeconds(40))
                .until(ExpectedConditions.presenceOfElementLocated(By.tagName("mat-dialog-container")));
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

    // TODO: botón Aceptar del formulario de Dietas localizado por texto —
    // el resto del proyecto usa el patrón "accept-{Componente}Container-button"
    // (p.ej. accept-ScaleRegisterContainer-button). Sustituir cuando se
    // confirme el nombre real del componente de este diálogo.
    private void aceptarDialogo() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
                Boolean ok = (Boolean) js().executeScript(
                        "var c = document.querySelector('mat-dialog-container');" +
                                "if (!c) return false;" +
                                "var b = Array.from(c.querySelectorAll('button')).find(b => {" +
                                "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                                "  return (t === 'aceptar' || t === 'guardar' || t === 'confirmar' || t === 'ok')" +
                                "    && !b.disabled && b.offsetParent !== null;" +
                                "});" +
                                "if (b) { b.scrollIntoView({block:'center'}); b.click(); return true; } return false;");
                return Boolean.TRUE.equals(ok);
            });
            WaitAMomentPlease(0.5f);
            cerrarModalesContinuar();
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                    d.findElements(By.tagName("mat-dialog-container")).isEmpty());
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException e) {
            Reporter.log("[WARN] Timeout en aceptar diálogo — cerrando con Escape");
            try {
                driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
                WaitAMomentPlease(1.0f);
            } catch (Exception ignored) {}
        }
    }

    private void cerrarAlertaSiExiste() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> (Boolean) js().executeScript(
                    "var btns = Array.from(document.querySelectorAll('button'));" +
                            "var b = btns.find(b => b.textContent.trim() === 'Aceptar' && b.offsetParent !== null);" +
                            "if (b) { b.click(); return true; } return false;"));
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException ignored) {}
    }

    /**
     * Cierra el diálogo informativo pulsando "Continuar".
     * Intenta primero el ID "continue-button" (mismo usado por
     * ClassBaseTest.handleReadOnlyAlert/handleValidationAlert en toda la
     * app) y solo si no aparece recurre a la búsqueda por texto original.
     */
    private void cerrarDialogoInformativo() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                    .click();
            WaitAMomentPlease(0.8f);
            return;
        } catch (TimeoutException ignored) {}

        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> (Boolean) js().executeScript(
                    "var btns = Array.from(document.querySelectorAll('button'));" +
                            "var b = btns.find(b => b.textContent.trim().toLowerCase() === 'continuar' && b.offsetParent !== null);" +
                            "if (b) { b.click(); return true; } return false;"));
            WaitAMomentPlease(0.8f);
        } catch (TimeoutException ignored) {}
    }

    /**
     * Confirma una eliminación. Intenta primero el ID "alert-confirm"
     * (mismo usado en ScaleRecordTest.DeleteScale y
     * DeviceRegistrationTest.handleDeleteConfirmation para el mismo tipo
     * de diálogo de confirmación de borrado) y solo si no aparece recurre
     * a la búsqueda por texto original.
     */
    private void confirmarEliminacion() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.elementToBeClickable(By.id("alert-confirm")))
                    .click();
            WaitAMomentPlease(0.5f);
            return;
        } catch (TimeoutException ignored) {}

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> (Boolean) js().executeScript(
                    "var btn = Array.from(document.querySelectorAll('button')).find(b => {" +
                            "  var t = b.textContent.replace(/\\s+/g,' ').trim();" +
                            "  return (t === 'Confirmar' || t === 'Aceptar' || t === 'Sí') && b.offsetParent !== null && !b.disabled;" +
                            "});" +
                            "if (btn) { btn.click(); return true; } return false;"));
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException ignored) {}
    }

    /**
     * Cierra en cadena los diálogos informativos "Continuar" que puedan
     * aparecer tras una acción. Intenta el ID "continue-button" en cada
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
                new WebDriverWait(driver, Duration.ofSeconds(2)).until(d -> (Boolean) js().executeScript(
                        "var b = Array.from(document.querySelectorAll('button'))" +
                                "  .find(b => b.textContent.trim() === 'Continuar' && !b.disabled);" +
                                "if (b) { b.click(); return true; } return false;"));
                WaitAMomentPlease(0.5f);
            } catch (TimeoutException e) {
                break;
            }
        }
    }

    private long contarFilasDieta() {
        Long count = (Long) js().executeScript(
                "return Array.from(document.querySelectorAll('mat-row, tr'))" +
                        "  .filter(f => f.offsetParent !== null && f.textContent.trim().length > 5).length;");
        return count != null ? count : 0L;
    }
}
