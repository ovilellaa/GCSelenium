package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * NOTA DE MIGRACIÓN (desde CarlosFreire):
 *  - Se ha sustituido la navegación por texto ("Urgencias", "Lista de
 *    trabajo", "Acciones", "Ver historia", "Curso Clínico"/"Evolución") por
 *    los IDs reales ya verificados en EmergencyWLTest.java (mismo proyecto):
 *    "emergency-sidebar", "worklist-sidebar", "actions-button-emergencyGridId",
 *    "see_history", "evolutionary_course-sidebar" — con el texto original
 *    como fallback por si el contexto exacto difiere.
 *  - TC3 (eliminar evolución) y TC4 (navegar a Informe de alta) tenían
 *    asserts degradados a warnings que dejaban "pasar" el test aunque la
 *    acción no se completara; se han restaurado como Assert reales.
 *  - Se ha eliminado un bloque de impresión de depuración dejado en el
 *    código (listado de items del sidebar).
 *  - El resto de selectores específicos del formulario de Evolución/Informe
 *    de alta (campos, diálogos de trazabilidad) no se han podido verificar
 *    contra la app real y quedan marcados con TODO.
 */
public class EvolucionTest extends ClassBaseTest {

    private String patientName = "Carlos Perez";
    private String lastEvolucionId;

    // ══════════════════════════════════════════════════════════════════
    // SETUP — Login y Navegación
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Paso 1: Login como enfermera")
    public void loginEnfermera() {
        LoginAsNurse();
        WaitAMomentPlease(2f);
        Assert.assertTrue(isUserLoggedIn(), "El usuario no está logueado");
        Reporter.log("Login completado — URL: " + driver.getCurrentUrl());
    }

    @Test(priority = 10, dependsOnMethods = "loginEnfermera",
            description = "Paso 2: Navegar a Urgencias y buscar paciente")
    public void navegarUrgenciasYBuscarPaciente() {
        WaitAMomentPlease(1f);

        clickPorIdOTexto("emergency-sidebar", "Urgencias");
        WaitAMomentPlease(1f);
        Reporter.log("Navegando a Urgencias...");

        clickPorIdOTexto("worklist-sidebar", "Lista de trabajo");
        WaitAMomentPlease(2f);
        Reporter.log("En Lista de trabajo");

        BuscarPacienteEnFiltro(patientName);
        WaitAMomentPlease(1f);
        Reporter.log("Paciente buscado: " + patientName);
    }

    @Test(priority = 20, dependsOnMethods = "navegarUrgenciasYBuscarPaciente",
            description = "Paso 3: Abrir historial del paciente")
    public void abrirHistorialPaciente() {
        // Paso 1 — Seleccionar el paciente clickando el radio button de la fila (○)
        // TODO: selección por tag genérico (radio/celda) — módulo específico sin verificar.
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var radios = Array.from(document.querySelectorAll('mat-radio-button, input[type=\"radio\"], .mat-radio-outer-circle'))" +
                            "  .filter(r => r.offsetParent !== null);" +
                            "if (radios[0]) { radios[0].click(); return true; }" +
                            "var celdas = Array.from(document.querySelectorAll('tbody tr td'))" +
                            "  .filter(c => c.offsetParent !== null);" +
                            "if (celdas[0]) { celdas[0].click(); return true; }" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1f);
        Reporter.log("Paciente seleccionado (radio button)");

        // Paso 2 — Click en "Acciones". IDs reales verificados en EmergencyWLTest
        // para la worklist de Urgencias: "actions-button-emergencyGridId"
        // (fallback al genérico "actions-button" y, en último caso, al texto).
        abrirMenuAccionesUrgencias();
        Reporter.log("Menú Acciones abierto");

        // Paso 3 — Click en "Ver historia". ID real verificado en EmergencyWLTest,
        // DeviceRegistrationTest, ScaleRecordTest, etc.: "see_history".
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("see_history"))).click();
        } catch (TimeoutException e) {
            wait.until(d -> {
                Boolean ok = (Boolean) js().executeScript(
                        "var items = Array.from(document.querySelectorAll('button[role=\"menuitem\"], .mat-menu-item, button'));" +
                                "var t = items.find(b => {" +
                                "  var txt = b.textContent.trim().toLowerCase();" +
                                "  return txt === 'ver historia' && b.offsetParent !== null;" +
                                "});" +
                                "if (!t) t = items.find(b => b.textContent.trim().toLowerCase().includes('ver hist') && b.offsetParent !== null);" +
                                "if (t) { t.click(); return true; } return false;");
                return Boolean.TRUE.equals(ok);
            });
        }
        WaitAMomentPlease(1.5f);
        Reporter.log("Click en Ver historia");

        // Paso 4 — Gestionar posibles diálogos de confirmación
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

        // Paso 5 — Si se abre en nueva pestaña, cambiar a ella
        if (TabOpenedCount() > 1) {
            SwitchToTab(GetLastTabOpened());
            WaitAMomentPlease(1f);
        }

        // Paso 6 — Cerrar alertas iniciales. Mismo ID que ClassBaseTest.handleVitalAlertsDialog().
        handleVitalAlertsDialog();

        cerrarAlertaSiExiste();
        WaitAMomentPlease(1f);
        Reporter.log("Historial abierto — URL: " + driver.getCurrentUrl());
    }

    @Test(priority = 30, dependsOnMethods = "abrirHistorialPaciente",
            description = "Paso 4: Navegar a Curso Clínico > Evolución")
    public void navegarAEvolucion() {
        WaitAMomentPlease(1.5f);

        // ID real verificado en EmergencyWLTest para el módulo de Evolución:
        // "evolutionary_course-sidebar". Si no aparece directamente (p.ej.
        // porque en el historial completo está anidado bajo "Curso Clínico"),
        // se recurre a la navegación por texto original.
        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("evolutionary_course-sidebar"))).click();
            Reporter.log("Click en Evolución (evolutionary_course-sidebar)");
        } catch (TimeoutException e) {
            clickElementoTexto("Curso Clínico");
            Reporter.log("Click en Curso Clínico");
            WaitAMomentPlease(1f);

            clickElementoTexto("Evolución");
            Reporter.log("Click en Evolución");
        }
        WaitAMomentPlease(2f);

        Reporter.log("Módulo de Evolución cargado — URL: " + driver.getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════
    // TC1 — CREAR EVOLUCIÓN
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = "navegarAEvolucion",
            description = "TC1: Crear nueva evolución")
    public void tc1_crearEvolucion() {
        long filasBefore = contarFilasEvolucion();
        Reporter.log("Evoluciones antes: " + filasBefore);

        cerrarAlertaSiExiste();
        abrirMenuAccionesEvolucion();
        clickMenuItem("Crear", "Nueva", "Añadir");

        esperarDialogoListo();
        String description = "Evolución de prueba - " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        rellenarEvolucion(description);
        aceptarDialogo();

        long filasAfter = contarFilasEvolucion();
        Reporter.log("Evoluciones después: " + filasAfter);
        Assert.assertTrue(filasAfter > filasBefore, "Debe haberse añadido una evolución");

        lastEvolucionId = obtenerIdUltimaEvolucion();
        Reporter.log("Evolución creada con ID: " + lastEvolucionId);
        Reporter.log("TC1 completado — Evolución creada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC1.1 — EDITAR EVOLUCIÓN
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 50, dependsOnMethods = "tc1_crearEvolucion",
            description = "TC1.1: Editar evolución creada")
    public void tc1_1_editarEvolucion() {
        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1f);

        seleccionarPrimeraFila();
        abrirMenuAccionesEvolucion();
        clickMenuItem("Editar");

        esperarDialogoListo();
        String descriptionEditada = "Evolución editada - " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        rellenarEvolucion(descriptionEditada);
        aceptarDialogo();

        Reporter.log("TC1.1 completado — Evolución editada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC2 — VER TRAZABILIDAD
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 60, dependsOnMethods = "tc1_1_editarEvolucion",
            description = "TC2: Ver trazabilidad de evolución")
    public void tc2_verTrazabilidad() {
        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1f);

        seleccionarPrimeraFila();
        abrirMenuAccionesEvolucion();
        clickMenuItem("Trazabilidad", "Ver trazabilidad", "Historial");

        esperarDialogoListo();
        WaitAMomentPlease(1f);

        long registros = contarRegistrosTrazabilidad();
        Reporter.log("Trazabilidad con " + registros + " registros");
        Assert.assertTrue(registros >= 2, "Debe haber al menos 2 registros (creación y edición)");

        cerrarModalDialogo();
        WaitAMomentPlease(1f);
        Reporter.log("TC2 completado — Trazabilidad verificada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC3 — ELIMINAR EVOLUCIÓN
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 70, dependsOnMethods = "tc2_verTrazabilidad",
            description = "TC3: Eliminar evolución")
    public void tc3_eliminarEvolucion() {
        long filasBefore = contarFilasEvolucion();
        Reporter.log("Evoluciones antes de eliminar: " + filasBefore);

        try { driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE); }
        catch (Exception ignored) {}
        WaitAMomentPlease(1f);

        // Seleccionar la evolución clickando en ella
        boolean seleccionada = (Boolean) wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var filas = Array.from(document.querySelectorAll('.evolution-item, tr.selectable, tr.mat-row, [class*=\"evolution\"]'))" +
                            "  .filter(f => f.offsetParent !== null);" +
                            "if (filas[0]) { filas[0].click(); return true; }" +
                            "var rows = Array.from(document.querySelectorAll('tbody tr, .cdk-row'))" +
                            "  .filter(r => r.offsetParent !== null && r.textContent.trim().length > 0);" +
                            "if (rows[0]) { rows[0].click(); return true; }" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        Assert.assertTrue(seleccionada, "No se pudo seleccionar ninguna evolución para eliminar");
        WaitAMomentPlease(0.5f);
        Reporter.log("Evolución seleccionada");

        abrirMenuAccionesEvolucion();
        boolean clickMenuOk = clickMenuItem("Eliminar", "Borrar", "Quitar");
        Assert.assertTrue(clickMenuOk, "No se encontró la opción de menú para eliminar la evolución");

        confirmarAccion();
        cerrarModalesContinuar();
        WaitAMomentPlease(2f); // Esperar a que se elimine

        long filasAfter = contarFilasEvolucion();
        Reporter.log("Evoluciones después de eliminar: " + filasAfter);
        Assert.assertTrue(filasAfter < filasBefore, "Debe haberse eliminado la evolución");

        Reporter.log("TC3 completado — Evolución eliminada");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC4 — INFORME DE ALTA: NAVEGAR AL FORMULARIO
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 80, dependsOnMethods = "tc3_eliminarEvolucion",
            description = "TC4: Navegar a Informe de alta desde el historial clínico")
    public void tc4_navegarAInformeDeAlta() {
        WaitAMomentPlease(1f);

        // Paso 0 — CERRAR cualquier dropdown abierto
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            WaitAMomentPlease(0.5f);
        } catch (Exception ignored) {}

        // Paso 1 — CLICKEAR LA FLECHA "volver" del header para salir a Historial clínico
        // TODO: localizado por clase de icono (fg-arrow-left) — módulo específico sin verificar.
        try {
            wait.until(d -> {
                Boolean ok = (Boolean) js().executeScript(
                        "var arrows = document.querySelectorAll('i.fg-arrow-left.toolbar-icon');" +
                                "if (arrows.length === 0) return false;" +
                                "var arrow = arrows[0]; " +
                                "if (!arrow || arrow.offsetParent === null) return false;" +
                                "var clickable = arrow.closest('div[class*=\"cursor-pointer\"], div.d-flex, button, a');" +
                                "if (!clickable) clickable = arrow.parentElement;" +
                                "if (clickable && clickable.offsetParent !== null) { " +
                                "  clickable.click(); return true; " +
                                "}" +
                                "return false;");
                return Boolean.TRUE.equals(ok);
            });
            WaitAMomentPlease(2f);
            Reporter.log("Flecha ← clickeada (Historial clínico abierto)");
        } catch (TimeoutException e) {
            Reporter.log("[WARN] No se pudo clickear la flecha del header");
        }

        WaitAMomentPlease(1.5f);

        // Paso 2 — BUSCAR Y CLICKEAR "INFORME DE ALTA"
        // TODO: localizado por texto/xpath — no se conoce el ID real
        // (posible candidato por convención: "discharge_report-sidebar",
        // visto en EmergencyWLTest, pero no verificado en este contexto).
        boolean found = false;
        try {
            List<By> xpaths = Arrays.asList(
                    By.xpath("//*[contains(text(), 'Informe de alta')]"),
                    By.xpath("//div[contains(., 'Informe de alta')]"),
                    By.xpath("//mat-list-item[contains(., 'Informe de alta')]"));

            WebElement element = null;
            for (By xpath : xpaths) {
                try {
                    element = wait.until(ExpectedConditions.visibilityOfElementLocated(xpath));
                    break;
                } catch (TimeoutException e) {
                    Reporter.log("XPath no encontrado: " + xpath);
                }
            }

            if (element != null) {
                Reporter.log("Elemento 'Informe de alta' encontrado");
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                WaitAMomentPlease(0.5f);
                element.click();
                found = true;
                Reporter.log("'Informe de alta' clickeado correctamente");
            } else {
                Reporter.log("[ERROR] No se encontró 'Informe de alta' con ningún xpath");
            }
        } catch (Exception e) {
            Reporter.log("[ERROR] Excepción: " + e.getMessage());
        }

        WaitAMomentPlease(2f);

        // Paso 3 — VERIFICAR QUE EL FORMULARIO ESTÁ CARGADO
        boolean formularioListo;
        try {
            wait.until(d -> {
                Boolean ok = (Boolean) js().executeScript(
                        "var inputs = Array.from(document.querySelectorAll('input, textarea, [contenteditable], mat-select'));" +
                                "var visibleInputs = inputs.filter(i => i.offsetParent !== null);" +
                                "if (visibleInputs.length === 0) return false;" +
                                "var hasHora = inputs.some(i => {" +
                                "  var ph = (i.getAttribute('placeholder') || '').toLowerCase();" +
                                "  return ph.includes('hh:mm') || ph.includes('hora');" +
                                "});" +
                                "return hasHora || visibleInputs.length > 1;");
                return Boolean.TRUE.equals(ok);
            });
            formularioListo = true;
        } catch (TimeoutException e) {
            formularioListo = false;
        }

        Assert.assertTrue(found && formularioListo,
                "No se pudo navegar al formulario de Informe de alta o no cargó correctamente");
        Reporter.log("TC4 — Formulario Informe de alta cargado — URL: " + driver.getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════
    // TC4.1 — INFORME DE ALTA: RELLENAR Y FIRMAR
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 90, dependsOnMethods = "tc4_navegarAInformeDeAlta",
            description = "TC4.1: Rellenar campos y firmar el Informe de alta")
    public void tc4_1_rellenarYFirmarInformeAlta() {
        WaitAMomentPlease(1f);

        // Paso 1 — Rellenar Hora de alta (campo HH:mm)
        try {
            WebElement horaField = (WebElement) js().executeScript(
                    "var inputs = Array.from(document.querySelectorAll('input'));" +
                            "return inputs.find(i => {" +
                            "  var ph = (i.getAttribute('placeholder') || '').toLowerCase();" +
                            "  return ph.includes('hh:mm') || ph.includes('hora');" +
                            "});");
            if (horaField != null) {
                horaField.click();
                horaField.clear();
                horaField.sendKeys("08:00");
                horaField.sendKeys(Keys.TAB);
                WaitAMomentPlease(0.5f);
                Reporter.log("Hora de alta rellenada: 08:00");
            }
        } catch (Exception e) {
            Reporter.log("[WARN] No se pudo rellenar hora: " + e.getMessage());
        }

        // Paso 2 — Seleccionar Destino de alta (primera opción disponible)
        try {
            js().executeScript(
                    "var selects = Array.from(document.querySelectorAll('mat-select'))" +
                            "  .filter(s => s.offsetParent !== null);" +
                            "var destino = selects.find(s => {" +
                            "  var label = s.closest('mat-form-field');" +
                            "  return label && label.textContent.toLowerCase().includes('destino');" +
                            "});" +
                            "if (!destino) destino = selects[0];" +
                            "if (destino) destino.click();");
            WaitAMomentPlease(0.8f);

            js().executeScript(
                    "var opts = Array.from(document.querySelectorAll('mat-option'))" +
                            "  .filter(o => o.offsetParent !== null && !o.classList.contains('mat-option-disabled'));" +
                            "if (opts[0]) opts[0].click();");
            WaitAMomentPlease(0.5f);
            Reporter.log("Destino de alta seleccionado");
        } catch (Exception e) {
            Reporter.log("[WARN] No se pudo seleccionar destino: " + e.getMessage());
        }

        // Paso 3 — Rellenar texto del editor enriquecido
        try {
            WebElement editor = (WebElement) js().executeScript(
                    "return document.querySelector('[contenteditable=\"true\"]," +
                            " .ql-editor, p[data-placeholder], .ProseMirror');");
            if (editor != null) {
                clearAndType(editor, "Informe de alta de prueba - Test automatizado " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), true);
                WaitAMomentPlease(0.5f);
                Reporter.log("Texto del informe rellenado");
            }
        } catch (Exception e) {
            Reporter.log("[WARN] No se pudo rellenar el texto: " + e.getMessage());
        }

        // Paso 4 — Firmar usando Sign() de ClassBaseTest
        WaitAMomentPlease(1f);
        if (!IsDischargeReportSigned()) {
            Reporter.log("Firmando informe de alta...");
            Sign(ConfigReader.get("password_nurse"));
            WaitAMomentPlease(2f);
            Assert.assertTrue(IsDischargeReportSigned(),
                    "El informe de alta debe estar firmado tras Sign()");
            Reporter.log("Informe de alta firmado correctamente");
        } else {
            Reporter.log("El informe ya estaba firmado");
        }

        Reporter.log("TEST COMPLETO INCLUYENDO INFORME DE ALTA FINALIZADO");
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private boolean isUserLoggedIn() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("mat-icon.user-profile-icon")));
            return true;
        } catch (TimeoutException e) { return false; }
    }

    /**
     * Click por ID real conocido, con fallback a texto visible si no aparece.
     */
    private void clickPorIdOTexto(String id, String texto) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id(id))).click();
            return;
        } catch (TimeoutException ignored) {}
        clickElementoTexto(texto);
    }

    /**
     * Abre el menú de Acciones de la worklist de Urgencias. ID verificado en
     * EmergencyWLTest: "actions-button-emergencyGridId". Si el contexto
     * difiere, cae al genérico "actions-button" y, en último caso, al texto.
     */
    private void abrirMenuAccionesUrgencias() {
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("actions-button-emergencyGridId")));
            js().executeScript("arguments[0].click();", btn);
            WaitAMomentPlease(0.8f);
            return;
        } catch (TimeoutException ignored) {}

        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
            js().executeScript("arguments[0].click();", btn);
            WaitAMomentPlease(0.8f);
            return;
        } catch (TimeoutException ignored) {}

        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var btns = Array.from(document.querySelectorAll('button'));" +
                            "var t = btns.find(b => b.textContent.trim().toLowerCase().includes('acciones') && b.offsetParent !== null && !b.disabled);" +
                            "if (t) { t.scrollIntoView({block:'center'}); t.click(); return true; } return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.8f);
    }

    /**
     * Busca un paciente en el campo "Escribe para filtrar sobre la lista"
     * Usa JavaScript para compatibilidad total con Angular Material
     *
     * TODO: "Carlos Perez" hardcodeado — ver nota en DietsTest.buscarYAbrirHistorial.
     */
    private void BuscarPacienteEnFiltro(String nombrePaciente) {
        Reporter.log("Buscando paciente: " + nombrePaciente);

        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var inputs = Array.from(document.querySelectorAll('input'));" +
                            "var campo = inputs.find(i => {" +
                            "  var ph = (i.getAttribute('placeholder') || '').toLowerCase();" +
                            "  return ph.includes('filtrar') || ph.includes('filter') || ph.includes('buscar');" +
                            "});" +
                            "if (!campo) {" +
                            "  campo = inputs.find(i => i.offsetParent !== null && i.type !== 'hidden');" +
                            "}" +
                            "if (campo) { campo.focus(); campo.click(); return true; }" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.3f);

        WebElement campoFiltro = (WebElement) js().executeScript(
                "var inputs = Array.from(document.querySelectorAll('input'));" +
                        "var campo = inputs.find(i => {" +
                        "  var ph = (i.getAttribute('placeholder') || '').toLowerCase();" +
                        "  return ph.includes('filtrar') || ph.includes('filter') || ph.includes('buscar');" +
                        "});" +
                        "if (!campo) campo = inputs.find(i => i.offsetParent !== null && i.type !== 'hidden');" +
                        "return campo;");

        if (campoFiltro == null) throw new RuntimeException("No se encontró el campo de filtro");

        campoFiltro.click();
        campoFiltro.clear();
        campoFiltro.sendKeys(nombrePaciente);
        WaitAMomentPlease(1.5f);
        Reporter.log("Nombre escrito en filtro: " + nombrePaciente);

        wait.until(d -> {
            Long count = (Long) js().executeScript(
                    "return Array.from(document.querySelectorAll('tbody tr, table tr'))" +
                            "  .filter(r => r.offsetParent !== null && r.cells && r.cells.length > 1).length;");
            return count != null && count > 0;
        });

        Boolean clicked = (Boolean) js().executeScript(
                "var filas = Array.from(document.querySelectorAll('tbody tr, table tr'))" +
                        "  .filter(r => r.offsetParent !== null && r.cells && r.cells.length > 1);" +
                        "if (filas[0]) { filas[0].click(); return true; }" +
                        "return false;");

        if (!Boolean.TRUE.equals(clicked)) throw new RuntimeException("No se pudo clickear la fila del paciente");

        WaitAMomentPlease(0.8f);
        Reporter.log("Paciente seleccionado: " + nombrePaciente);
    }

    private void clickElementoTexto(String... textos) {
        for (String texto : textos) {
            try {
                String needle = texto.toLowerCase();
                Boolean ok = (Boolean) js().executeScript(
                        "var needle = '" + needle.replace("'", "\\'") + "';" +
                                "var all = Array.from(document.querySelectorAll('a, button, mat-list-item, span, li, [role=\"tab\"]'));" +
                                "var target = all.find(el => {" +
                                "  var txt = el.textContent.trim().toLowerCase();" +
                                "  return txt === needle && el.offsetParent !== null && !el.disabled;" +
                                "});" +
                                "if (target) { target.scrollIntoView({block:'center'}); target.click(); return true; }" +
                                "return false;");
                if (Boolean.TRUE.equals(ok)) return;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("No se encontró elemento con texto: " + String.join(" o ", textos));
    }

    // TODO: "actions-button" es el ID genérico correcto para el menú Acciones
    // dentro del historial clínico (verificado en toda la app); se mantiene
    // sin cambios porque ya era correcto en el original.
    private void abrirMenuAccionesEvolucion() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        WaitAMomentPlease(0.3f);
        js().executeScript("arguments[0].click();", btn);
        WaitAMomentPlease(0.8f);
    }

    // TODO: ítems de menú específicos de Evolución localizados por texto —
    // no se conoce su ID real. Sustituir cuando se valide contra QA.
    private boolean clickMenuItem(String... opciones) {
        for (String opcion : opciones) {
            try {
                String needle = opcion.toLowerCase();
                wait.until(d -> {
                    Boolean ok = (Boolean) js().executeScript(
                            "var needle = '" + needle.replace("'", "\\'") + "';" +
                                    "var items = Array.from(document.querySelectorAll('button[role=\"menuitem\"], .mat-menu-item, button'));" +
                                    "var t = items.find(b => b.textContent.trim().toLowerCase() === needle && b.offsetParent !== null && !b.disabled);" +
                                    "if (!t) t = items.find(b => b.textContent.trim().toLowerCase().includes(needle) && b.offsetParent !== null && !b.disabled);" +
                                    "if (t) { t.click(); return true; } return false;");
                    return Boolean.TRUE.equals(ok);
                });
                WaitAMomentPlease(0.5f);
                return true;
            } catch (TimeoutException ignored) {}
        }
        Reporter.log("[WARN] No se encontró opción del menú: " + String.join(" o ", opciones));
        return false;
    }

    private void seleccionarPrimeraFila() {
        WaitAMomentPlease(0.5f);
        Boolean ok = (Boolean) js().executeScript(
                "var filas = Array.from(document.querySelectorAll('tr.selectable, tr.mat-row'))" +
                        "  .filter(f => f.offsetParent !== null);" +
                        "if (filas[0]) { filas[0].click(); return true; } return false;");
        if (!Boolean.TRUE.equals(ok))
            throw new RuntimeException("No se encontró fila seleccionable");
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

    // TODO: id "evolution-description" no verificado contra la app real.
    private void rellenarEvolucion(String description) {
        try {
            WebElement descField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("evolution-description")));
            clearAndType(descField, description, true);
            Reporter.log("Descripción rellenada");
        } catch (TimeoutException e) {
            try {
                WebElement alt = driver.findElement(By.xpath("//textarea | //*[@contenteditable='true']"));
                clearAndType(alt, description, true);
                Reporter.log("Descripción rellenada (selector alternativo)");
            } catch (Exception e2) {
                Reporter.log("[WARN] No se pudo rellenar descripción: " + e2.getMessage());
            }
        }
    }

    // TODO: botón Aceptar localizado por texto — no se conoce el ID del
    // componente de este módulo (patrón esperado: accept-{Componente}-button).
    private void aceptarDialogo() {
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var c = document.querySelector('mat-dialog-container');" +
                            "if (!c) return false;" +
                            "var b = Array.from(c.querySelectorAll('button')).find(b => {" +
                            "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                            "  return (t === 'aceptar' || t === 'guardar' || t === 'confirmar') && !b.disabled && b.offsetParent !== null;" +
                            "});" +
                            "if (b) { b.scrollIntoView({block:'center'}); b.click(); return true; }" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1.0f);
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
                                    "  return (t === 'Confirmar' || t === 'Aceptar' || t === 'Sí' || t === 'Eliminar') && b.offsetParent !== null && !b.disabled;" +
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
                                        "  .find(b => b.textContent.trim() === 'Continuar' && !b.disabled && b.offsetParent !== null);" +
                                        "if (b) { b.click(); return true; } return false;"));
                WaitAMomentPlease(0.5f);
            } catch (TimeoutException e) { break; }
        }
    }

    private void cerrarModalDialogo() {
        try {
            Boolean ok = (Boolean) js().executeScript(
                    "var closeBtn = Array.from(document.querySelectorAll('button, [aria-label]'))" +
                            "  .find(b => (b.textContent.trim() === 'Cerrar' || b.getAttribute('aria-label') === 'Cerrar') && b.offsetParent !== null);" +
                            "if (closeBtn) { closeBtn.click(); return true; }" +
                            "return false;");
            if (!Boolean.TRUE.equals(ok))
                driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {}
    }

    private long contarFilasEvolucion() {
        Long count = (Long) js().executeScript(
                "return Array.from(document.querySelectorAll('tr.selectable, tr.mat-row, .evolution-item'))" +
                        "  .filter(f => f.offsetParent !== null).length;");
        return count != null ? count : 0L;
    }

    private String obtenerIdUltimaEvolucion() {
        String id = (String) js().executeScript(
                "var rows = Array.from(document.querySelectorAll('tr.selectable, tr.mat-row, .evolution-item'))" +
                        "  .filter(r => r.offsetParent !== null);" +
                        "if (rows[0]) {" +
                        "  var dataId = rows[0].getAttribute('data-evolution-id');" +
                        "  if (dataId) return dataId;" +
                        "  var idAttr = rows[0].getAttribute('id');" +
                        "  if (idAttr) return idAttr;" +
                        "}" +
                        "return 'UNKNOWN-' + Date.now();");
        return id != null ? id : "UNKNOWN";
    }

    private long contarRegistrosTrazabilidad() {
        Long count = (Long) js().executeScript(
                "var modal = document.querySelector('mat-dialog-container');" +
                        "if (!modal) return 0;" +
                        "return Array.from(modal.querySelectorAll('tr, .traceability-item, [class*=\"trace\"]'))" +
                        "  .filter(r => r.offsetParent !== null).length;");
        return count != null ? count : 0L;
    }
}
