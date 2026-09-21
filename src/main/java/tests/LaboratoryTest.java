package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;

/**
 * Migrado desde "Pruebas Jordi\GCSelenium" (proyecto desconectado de Git).
 *
 * Test de integración: Laboratorio desde Hospitalización → Historial clínico
 *
 * Flujo:
 *   1. Login como médico
 *   2. Hospitalización → Lista de trabajo → filtrar por "Torre" → Juan de la Torre (cama 404)
 *   3. Click derecho → Ver historia → nueva pestaña con historial clínico
 *   4. Mapa de pruebas → Laboratorio
 *   5. Nueva solicitud → BIOMETRIA HEMATICA (BHC) → Inmediata → Aceptar
 *   6. Seleccionar fila → Acciones → Eliminar solicitud → Confirmar
 *   7. Nueva solicitud → FIBRINOGENO → Inmediata → Aceptar
 *   8. Seleccionar fila → Acciones → Abrir informe → Cerrar
 *
 * IDs verificados por Jordi en su propia sesión de exploración:
 *   — Hospitalización WL:            hospitalization-sidebar, worklist-sidebar, filter-input
 *   — Menú contextual:               see_history
 *   — Historial clínico (nueva tab): summary-sidebar, tests_map-sidebar, laboratory-sidebar
 *   — Acciones laboratorio:          actions-button, new_request, delete_request, open_report
 *   — Catálogo:                      accept-TestCatalogLaboratoryContainer-button, angle-right-button
 *   — Árbol nodos (expand):          expanded-node-button (mismo id para todos, filtrar por texto)
 *   — Checkboxes pruebas:            tree-checkbox-initial-LABLAB0025-1 (BIOMETRIA)
 *                                    tree-checkbox-initial-LABLAB0079-1 (FIBRINOGENO)
 *   — Tipo solicitud:                requestTypeId → requestTypeId-0 (Inmediata)
 *   — Guardar solicitud:             accept-LaboratoryDialogContainer-button
 *   — Modal eliminar:                alert-confirm
 *   — Informe:                       close-LaboratoryTestResultContainer-button
 *
 * NOTA DE MIGRACIÓN: el paciente ("Juan de la Torre Albuquerque", cama 404,
 * localizado por texto + scroll) se mantiene igual que en el proyecto
 * original de Jordi en vez de sustituirlo por el paciente estándar de
 * ConfigReader ("pacqah1NH") usado en el resto de tests de Hospitalización —
 * no hay evidencia de que ese paciente estándar tenga solicitudes de
 * laboratorio disponibles en el catálogo, así que se ha preferido no tocar
 * un flujo que Jordi ya verificó funcionando contra la app real.
 *
 * Notas importantes (de la exploración original):
 *   - Al guardar solicitud desde historial clínico NO aparece modal de confirmación
 *     (a diferencia del flujo desde Spotlight).
 *   - El botón "Catálogo" (gc-button) no responde a click() directo de Selenium;
 *     hay que buscarlo por texto con JS.
 *   - El backdrop del catálogo bloquea requestTypeId — hay que cerrarlo con JS primero.
 *   - La nueva pestaña se espera con until(windowHandles > 1) y se cambia a la última.
 */
public class LaboratoryTest extends ClassBaseTest {

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 1 — Login
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 1)
    public void Login() {
        LoginAsDoctor();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("hospitalization-sidebar")));

        Reporter.log("Login completado.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 2 — Hospitalización → Lista de trabajo → filtrar paciente
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void EnterHospitalizationWL() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        w.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        // Esperar a que cargue la lista
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("filter-input")));

        // Scroll progresivo hasta que Juan de la Torre Albuquerque (cama 404) aparezca en el DOM.
        // Timeout de 60s porque puede requerir varias iteraciones de scroll.
        WebDriverWait wScroll = new WebDriverWait(driver, Duration.ofSeconds(60));
        wScroll.until(d -> {
            JavascriptExecutor js = (JavascriptExecutor) d;

            // Comprobar si ya está en el DOM
            java.util.List<WebElement> rows = d.findElements(
                    By.xpath("//*[starts-with(@id,'gridId-') and contains(.,'Torre Albuquerque')]"));
            if (!rows.isEmpty()) return true;

            // Scroll 300px más en el contenedor de la tabla
            js.executeScript(
                    "var c = document.querySelector('.container-component.mat-elevation-z1');" +
                            "if (c) c.scrollTop += 300;"
            );
            return false;
        });

        Reporter.log("Paciente Juan de la Torre Albuquerque encontrado en la lista (cama 404).");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 3 — Click derecho → Ver historia → cambiar a nueva pestaña
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void OpenPatientHistory() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Localizar la fila de Juan de la Torre Albuquerque por su texto
        WebElement patientRow = w.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[starts-with(@id,'gridId-') and contains(.,'Torre Albuquerque')]")));

        // Click derecho para abrir el menú contextual
        new org.openqa.selenium.interactions.Actions(driver)
                .contextClick(patientRow)
                .perform();

        // Ver historia
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("see_history"))).click();

        // Esperar a que se abra la nueva pestaña
        w.until(d -> d.getWindowHandles().size() > 1);

        // Cambiar a la nueva pestaña
        String newTab = new ArrayList<>(driver.getWindowHandles()).getLast();
        driver.switchTo().window(newTab);

        // Esperar a que cargue el historial clínico — el menú viene expandido directamente,
        // el primer elemento siempre visible es summary-sidebar (Resumen)
        WebDriverWait wTab = new WebDriverWait(driver, Duration.ofSeconds(60));
        wTab.until(ExpectedConditions.presenceOfElementLocated(
                By.id("summary-sidebar")));

        Reporter.log("Nueva pestaña con historial clínico abierta.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 4 — Mapa de pruebas → Laboratorio
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 4, dependsOnMethods = {"OpenPatientHistory"})
    public void NavigateToLaboratory() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        // El menú del historial clínico ya viene expandido al abrir la pestaña.
        // Ir directamente a Mapa de pruebas
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("tests_map-sidebar"))).click();

        // Laboratorio
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("laboratory-sidebar"))).click();

        // Esperar a que cargue la tabla de pruebas
        w.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("actions-button")));

        Reporter.log("Sección Laboratorio cargada.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 5 — Crear primera solicitud (BIOMETRIA HEMATICA BHC)
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 5, dependsOnMethods = {"NavigateToLaboratory"})
    public void CreateFirstLabRequest() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Acciones → Nueva solicitud
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("new_request"))).click();

        // Esperar formulario
        w.until(ExpectedConditions.visibilityOfElementLocated(By.id("labCatalogType")));

        // Abrir catálogo
        openCatalog(w);

        // Expandir HEMATOLOGÍA y seleccionar BIOMETRIA HEMATICA (BHC)
        expandTreeNode("HEMATOLOGÍA");
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("tree-checkbox-initial-LABLAB0025-1")));
        jsClick(driver.findElement(By.id("tree-checkbox-initial-LABLAB0025-1")));

        // Mover al panel derecho
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("angle-right-button"))).click();

        // Verificar en panel derecho
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("tree-checkbox-final-LABLAB0025-1")));

        // Aceptar catálogo
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-TestCatalogLaboratoryContainer-button"))).click();

        // Esperar formulario de vuelta y seleccionar tipo Inmediata
        w.until(ExpectedConditions.visibilityOfElementLocated(By.id("requestTypeId")));
        closeBackdropAndClick("requestTypeId");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("requestTypeId-0"))).click();

        // Guardar — en historial clínico NO aparece modal de confirmación
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-LaboratoryDialogContainer-button"))).click();

        // Esperar retorno a la tabla
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-LaboratoryDialogContainer-button")));
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));

        Reporter.log("Primera solicitud creada: BIOMETRIA HEMATICA (BHC) - Inmediata.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 6 — Eliminar la primera solicitud
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 6, dependsOnMethods = {"CreateFirstLabRequest"})
    public void DeleteFirstLabRequest() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Seleccionar la fila de la solicitud recién creada (primera de la lista)
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        jsClick(driver.findElement(By.id("gridId-0")));

        // Acciones → Eliminar solicitud
        jsClick(w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button"))));
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("delete_request"))).click();

        // Confirmar eliminación
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();

        // Esperar a que la fila desaparezca o la tabla se actualice
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));

        Reporter.log("Primera solicitud eliminada correctamente.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 7 — Crear segunda solicitud (FIBRINOGENO) y NO eliminarla
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 7, dependsOnMethods = {"DeleteFirstLabRequest"})
    public void CreateSecondLabRequest() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Acciones → Nueva solicitud
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button"))).click();
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("new_request"))).click();

        // Esperar formulario
        w.until(ExpectedConditions.visibilityOfElementLocated(By.id("labCatalogType")));

        // Abrir catálogo
        openCatalog(w);

        // Expandir COAGULACIÓN y seleccionar FIBRINOGENO
        expandTreeNode("COAGULACIÓN");
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("tree-checkbox-initial-LABLAB0079-1")));
        jsClick(driver.findElement(By.id("tree-checkbox-initial-LABLAB0079-1")));

        // Mover al panel derecho
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("angle-right-button"))).click();

        // Verificar en panel derecho
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("tree-checkbox-final-LABLAB0079-1")));

        // Aceptar catálogo
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-TestCatalogLaboratoryContainer-button"))).click();

        // Esperar formulario y seleccionar tipo Inmediata
        w.until(ExpectedConditions.visibilityOfElementLocated(By.id("requestTypeId")));
        closeBackdropAndClick("requestTypeId");
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("requestTypeId-0"))).click();

        // Guardar
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-LaboratoryDialogContainer-button"))).click();

        // Esperar retorno a la tabla
        w.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("accept-LaboratoryDialogContainer-button")));
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));

        Reporter.log("Segunda solicitud creada: FIBRINOGENO - Inmediata. No se elimina.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 8 — Abrir informe de la segunda solicitud
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 8, dependsOnMethods = {"CreateSecondLabRequest"})
    public void OpenReport() {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Seleccionar la primera fila (solicitud más reciente)
        w.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        jsClick(driver.findElement(By.id("gridId-0")));

        // Acciones → Abrir informe
        jsClick(w.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button"))));
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("open_report"))).click();

        // Esperar modal del informe
        w.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("close-LaboratoryTestResultContainer-button")));

        Reporter.log("Informe abierto correctamente.");

        // Cerrar informe
        w.until(ExpectedConditions.elementToBeClickable(
                By.id("close-LaboratoryTestResultContainer-button"))).click();

        Reporter.log("Test completado con éxito.");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Abre el catálogo de pruebas haciendo clic en el gc-button "Catálogo"
     * visible al lado del label "Tipo" en el panel de Pruebas.
     * Espera con until() a que el gc-button sea visible antes de hacer click.
     */
    private void openCatalog(WebDriverWait w) {
        w.until(d -> {
            Object found = ((JavascriptExecutor) d).executeScript(
                    "var gcBtns = document.querySelectorAll('gc-button');" +
                            "var btn = Array.from(gcBtns).find(function(b) {" +
                            "  var rect = b.getBoundingClientRect();" +
                            "  return b.textContent.trim() === 'Catálogo' && rect.width > 0 && rect.y > 0;" +
                            "});" +
                            "return btn ? 'found' : null;"
            );
            return "found".equals(found);
        });

        ((JavascriptExecutor) driver).executeScript(
                "var gcBtns = document.querySelectorAll('gc-button');" +
                        "var btn = Array.from(gcBtns).find(function(b) {" +
                        "  var rect = b.getBoundingClientRect();" +
                        "  return b.textContent.trim() === 'Catálogo' && rect.width > 0 && rect.y > 0;" +
                        "});" +
                        "if (btn) btn.click();"
        );

        w.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("accept-TestCatalogLaboratoryContainer-button")));
    }

    /**
     * Expande un nodo del árbol del catálogo buscando el expanded-node-button
     * cuyo mat-tree-node padre contenga el texto indicado.
     * Todos los nodos comparten el mismo id "expanded-node-button".
     */
    private void expandTreeNode(String nodeName) {
        ((JavascriptExecutor) driver).executeScript(
                "var btns = Array.from(document.querySelectorAll('#expanded-node-button'));" +
                        "var btn = btns.find(function(b) {" +
                        "  return (b.closest('mat-tree-node') || b.parentElement).textContent.includes('" + nodeName + "');" +
                        "});" +
                        "if (btn) btn.click();"
        );
    }

    /**
     * Cierra cualquier backdrop del CDK overlay y luego hace click en el elemento
     * indicado por id. Necesario para interactuar con mat-select bloqueados por
     * el backdrop transparente del catálogo.
     */
    private void closeBackdropAndClick(String elementId) {
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('.cdk-overlay-backdrop').forEach(function(b) { b.click(); });" +
                        "document.getElementById('" + elementId + "').click();"
        );
    }

    /**
     * Hace clic en un elemento usando JavaScript para evitar
     * interceptación de overlays o backdrops de Angular Material.
     */
    private void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }
}
