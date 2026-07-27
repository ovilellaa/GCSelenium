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
 * Test de integración: CRUD Medicación + Trazabilidad
 *
 * Flujo:
 *   TC1 — Login médico → Hospitalización → paciente 0039530 → Ver historia
 *          → nueva pestaña → Tratamientos → Medicación
 *   TC2 — Agregar medicamento: Amoxiclav (326051), Bucal topica, C/1hr, todos los días
 *   TC3 — Modificar medicamento: cambiar cantidad a 2
 *   TC4 — Eliminar medicamento: confirmar → gestionar aviso de negocio si aparece
 *   TC5 — Añadir nuevo medicamento (mismo fármaco, mismos parámetros)
 *   TC6 — Ver trazabilidad → cerrar
 *
 * ── Paciente ──────────────────────────────────────────────────────────────────
 *   ConfigReader.get("pacqah1NH")  →  0039530  (De La Torre Albuquerque, Juan)
 *
 * ── IDs navegación ────────────────────────────────────────────────────────────
 *   hospitalization-sidebar · worklist-sidebar · filter-input
 *   grid-gridId-{N}-patient · actions-button · see_history
 *   treatments-sidebar · medication-sidebar
 *
 * ── IDs lista medicación ──────────────────────────────────────────────────────
 *   grid-gridId-0-drug          → celda del medicamento activo (fila 0)
 *   actions-button              → menú Acciones
 *   add_drug                    → Agregar medicamento
 *   update_drug                 → Modificar medicamento
 *   delete_drug                 → Eliminar medicamento
 *   see_traceability            → Ver trazabilidad
 *
 * ── IDs formulario Agregar / Modificar (PrescriptionContainer) ───────────────
 *   drugConfig                  → autocomplete medicamento;  opción: drugConfig-0
 *   visibleQuantity             → cantidad
 *   intake                      → autocomplete vía;          opción: intake-0
 *   usualPattern                → autocomplete intervalo;    opción: usualPattern-0
 *   days-0 … days-6             → chips días L–D (por defecto todos marcados)
 *   accept_dialog-PrescriptionContainer-button  → Aceptar
 *   cancel_dialog-PrescriptionContainer-button  → Cancelar
 *
 * ── IDs modal "fármaco ya existe en tratamiento" ──────────────────────────────
 *   alert-confirm               → Confirmar (añadir de todos modos)
 *   alert-cancel                → Cancelar
 *   Aparece en mat-dialog-container[1] sobre el formulario de prescripción.
 *   Texto: "El fármaco seleccionado ya está en el tratamiento.
 *           ¿Quieres añadirlo de todos modos?"
 *
 * ── IDs diálogo Eliminar ──────────────────────────────────────────────────────
 *   alert-confirm               → Confirmar eliminación
 *   alert-cancel                → Cancelar eliminación
 *   continue-button             → Continuar (aviso de negocio: esperar 1 min)
 *
 * ── IDs diálogo Trazabilidad ──────────────────────────────────────────────────
 *   close-SeeTraceabilityDialogComponent-button → Cerrar
 */
public class AddMedicationTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    // ── Datos del medicamento ─────────────────────────────────────────────────
    private static final String DRUG_CODE          = "326051";
    private static final String DRUG_OPTION_ID     = "drugConfig-0";
    private static final String VIA_SEARCH         = "bucal";
    private static final String VIA_OPTION_ID      = "intake-0";
    private static final String INTERVAL_SEARCH    = "c/1";
    private static final String INTERVAL_OPTION_ID = "usualPattern-0";
    private static final String NEW_QUANTITY       = "2";

    // =========================================================================
    // TC1 — LOGIN + NAVEGAR A MEDICACIÓN
    // =========================================================================
    @Test(priority = 1)
    public void TC1_LoginAndNavigateToMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        LoginAsDoctor();

        // ── Hospitalización → Lista de trabajo ────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("filter-input")));

        // ── Filtrar por código de paciente ────────────────────────────────────
        String pacienteNH = ConfigReader.get("pacqah1NH");
        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(pacienteNH);

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.id("load-all-information"))).click();
        } catch (TimeoutException ignored) {}

        // ── Click en fila del paciente (JS para evitar overlay) ───────────────
        WebElement patientCell = localWait.until(d -> {
            List<WebElement> cells = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-patient']"));
            return cells.isEmpty() ? null : cells.get(0);
        });
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", patientCell);

        // ── Acciones → Ver historia ───────────────────────────────────────────
        openActionsMenuAndClick(localWait, "see_history");
        SelectAccessReason("Guardia");

        // ── Cambiar a la nueva pestaña ────────────────────────────────────────
        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        // ── Cerrar TODOS los backdrops activos ───────────────────────────────
        // Después de handleReadOnlyAlert puede quedar un mat-drawer-backdrop
        // con clase mat-drawer-shown que bloquea los clicks sobre el sidebar.
        // Se hace click en todos (mat-drawer-backdrop Y cdk-overlay-backdrop)
        // y luego se espera que ambos desaparezcan del DOM visible.
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll(" +
                        "'.mat-drawer-backdrop,.cdk-overlay-backdrop'" +
                        ").forEach(b => b.click());");

        new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector(".mat-drawer-backdrop.mat-drawer-shown")));

        // ── Tratamientos → Medicación (JS click directo por ID) ───────────────
        // Se usa presenceOfElementLocated porque elementToBeClickable puede
        // fallar si algún overlay residual sigue en el DOM un instante más.
        // El JS click no requiere que Selenium considere el elemento "clickable".
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("treatments-sidebar")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('treatments-sidebar').click();");

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("medication-sidebar")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('medication-sidebar').click();");

        // Esperar que gc-medication esté en el DOM (aparece antes que actions-button)
        new WebDriverWait(driver, LONG_TIMEOUT).until(
                ExpectedConditions.presenceOfElementLocated(By.tagName("gc-medication")));

        // Esperar que el botón Acciones sea clickable
        localWait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));

        Reporter.log("TC1 OK — Módulo Medicación cargado. Paciente: " + pacienteNH);
    }

    // =========================================================================
    // TC2 — AGREGAR MEDICAMENTO
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = "TC1_LoginAndNavigateToMedication")
    public void TC2_AddMedication() {
        openActionsMenuAndClick(new WebDriverWait(driver, TIMEOUT), "add_drug");
        fillAndSaveDrugForm();

        // Verificar que el medicamento aparece en la lista
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);
        WebElement addedRow = localWait.until(d -> {
            for (WebElement cell : d.findElements(By.cssSelector("td, mat-cell"))) {
                try {
                    if (cell.getText().toUpperCase().contains("AMOXICLAV")) return cell;
                } catch (StaleElementReferenceException ignored) {}
            }
            return null;
        });
        Assert.assertNotNull(addedRow, "AMOXICLAV no aparece en la lista tras agregar.");
        Reporter.log("TC2 OK — Medicamento AMOXICLAV agregado correctamente.");
    }

    // =========================================================================
    // TC3 — MODIFICAR MEDICAMENTO
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = "TC2_AddMedication")
    public void TC3_UpdateMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila activa y abrir Modificar
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('grid-gridId-0-drug').click();");
        openActionsMenuAndClick(localWait, "update_drug");

        // Esperar que se abra el formulario de actualización
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("visibleQuantity")));

        // Cambiar cantidad a 2 con nativeSetter (Angular no detecta .value = x directamente)
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.getElementById('visibleQuantity');" +
                        "var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(el, '" + NEW_QUANTITY + "');" +
                        "el.dispatchEvent(new Event('input',  {bubbles: true}));" +
                        "el.dispatchEvent(new Event('change', {bubbles: true}));");

        // Aceptar (JS click — evita overlay intercepted)
        WebElement acceptUpdateBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept_dialog-PrescriptionContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptUpdateBtn);

        // Esperar cierre del diálogo
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        // Verificar cantidad actualizada en lista
        WebElement updatedRow = localWait.until(d -> {
            for (WebElement cell : d.findElements(By.cssSelector("td, mat-cell"))) {
                try {
                    if (cell.getText().contains(NEW_QUANTITY + " TABLETA")) return cell;
                } catch (StaleElementReferenceException ignored) {}
            }
            return null;
        });
        Assert.assertNotNull(updatedRow,
                "La cantidad '" + NEW_QUANTITY + " TABLETA' no aparece en la lista.");
        Reporter.log("TC3 OK — Medicamento modificado a cantidad " + NEW_QUANTITY + ".");
    }

    // =========================================================================
    // TC4 — ELIMINAR MEDICAMENTO
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = "TC3_UpdateMedication")
    public void TC4_DeleteMedication() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila activa y abrir Eliminar
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('grid-gridId-0-drug').click();");
        openActionsMenuAndClick(localWait, "delete_drug");

        // Confirmar eliminación (JS click — evita overlay intercepted)
        WebElement confirmDeleteBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("alert-confirm")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmDeleteBtn);

        // Gestionar aviso de negocio: "debe esperar al menos un minuto..." (opcional)
        try {
            WebElement continueBtn = new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("continue-button")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn);
        } catch (TimeoutException ignored) {}

        // Esperar cierre de cualquier diálogo pendiente
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        Reporter.log("TC4 OK — Medicamento eliminado.");
    }

    // =========================================================================
    // TC5 — AÑADIR NUEVO MEDICAMENTO
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = "TC4_DeleteMedication")
    public void TC5_AddNewMedication() {
        openActionsMenuAndClick(new WebDriverWait(driver, TIMEOUT), "add_drug");
        fillAndSaveDrugForm();

        // Verificar que el medicamento aparece de nuevo en la lista
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);
        WebElement newRow = localWait.until(d -> {
            for (WebElement cell : d.findElements(By.cssSelector("td, mat-cell"))) {
                try {
                    if (cell.getText().toUpperCase().contains("AMOXICLAV")) return cell;
                } catch (StaleElementReferenceException ignored) {}
            }
            return null;
        });
        Assert.assertNotNull(newRow, "El nuevo AMOXICLAV no aparece en la lista.");
        Reporter.log("TC5 OK — Nuevo medicamento AMOXICLAV añadido correctamente.");
    }

    // =========================================================================
    // TC6 — VER TRAZABILIDAD Y CERRAR
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = "TC5_AddNewMedication")
    public void TC6_ViewTraceabilityAndClose() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila activa y abrir Ver trazabilidad
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('grid-gridId-0-drug').click();");
        openActionsMenuAndClick(localWait, "see_traceability");

        // Esperar que se abra el diálogo
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("close-SeeTraceabilityDialogComponent-button")));
        Reporter.log("TC6 — Diálogo Trazabilidad abierto correctamente.");

        // Cerrar (JS click — evita overlay intercepted)
        WebElement closeTraceBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("close-SeeTraceabilityDialogComponent-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeTraceBtn);

        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());

        Reporter.log("TC6 OK — Trazabilidad visualizada y cerrada.");
    }

    // =========================================================================
    // MÉTODO AUXILIAR — Rellena y guarda el formulario de prescripción
    // =========================================================================
    /**
     * Rellena el formulario "Añadir / Actualizar medicamento a tratamiento":
     *   - Medicamento : Amoxiclav bid 875/125mg (código 326051)
     *   - Vía         : Bucal topica
     *   - Intervalo   : C/1hr
     *   - Días        : todos (L–D, marcados por defecto)
     *
     * Gestiona el modal de duplicado que aparece cuando el fármaco
     * ya existe en el tratamiento activo:
     *   Título : "Confirmar"
     *   Texto  : "El fármaco seleccionado ya está en el tratamiento.
     *             ¿Quieres añadirlo de todos modos?"
     *   IDs    : alert-confirm (Confirmar) / alert-cancel (Cancelar)
     *            → se pulsa alert-confirm para continuar añadiéndolo.
     *
     * Espera el cierre completo del diálogo antes de retornar.
     */
    private void fillAndSaveDrugForm() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Esperar que se abra el formulario
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("drugConfig")));

        // ── Medicamento ───────────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("drugConfig"))).sendKeys(DRUG_CODE);
        WebElement drugOption = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(DRUG_OPTION_ID)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", drugOption);

        // ── Vía: Bucal topica ─────────────────────────────────────────────────
        WebElement intakeField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("intake")));
        intakeField.clear();
        intakeField.sendKeys(VIA_SEARCH);
        WebElement intakeOption = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(VIA_OPTION_ID)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", intakeOption);

        // ── Intervalo: C/1hr ──────────────────────────────────────────────────
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("usualPattern"))).sendKeys(INTERVAL_SEARCH);
        WebElement intervalOption = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id(INTERVAL_OPTION_ID)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", intervalOption);

        // ── Días: todos (L–D marcados por defecto, verificar presencia) ───────
        for (int i = 0; i <= 6; i++) {
            localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("days-" + i)));
        }

        // ── Aceptar formulario (JS click — evita overlay intercepted) ───────────
        WebElement acceptBtn = localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept_dialog-PrescriptionContainer-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", acceptBtn);

        // ── Modal de duplicado: "El fármaco ya está en el tratamiento" ─────────
        // Aparece en mat-dialog-container[1] si el fármaco ya existe en la lista.
        // Texto : "El fármaco seleccionado ya está en el tratamiento.
        //          ¿Quieres añadirlo de todos modos?"
        // IDs   : alert-confirm (Confirmar) / alert-cancel (Cancelar)
        // Se pulsa alert-confirm para añadirlo igualmente.
        try {
            WebElement confirmBtn = new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("alert-confirm")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
            Reporter.log("Modal duplicado gestionado: fármaco añadido de todos modos.");
        } catch (TimeoutException ignored) {
            // No apareció el modal → el fármaco era nuevo, guardado directo.
        }

        // ── Esperar cierre completo de todos los diálogos ─────────────────────
        new WebDriverWait(driver, LONG_TIMEOUT).until(d ->
                d.findElements(By.cssSelector("mat-dialog-container")).isEmpty());
    }
}
