package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña)
 *                      → Registros → Registro de escalas
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo → buscar paciente por NH → seleccionar
 *   3.  Acciones → Ver historia → cambiar a nueva pestaña
 *   4.  Registros → Registro de escalas
 *   5.  Añadir escala "Nivel de Conciencia" → rellenar → Aceptar → verificar fila
 *   6.  Seleccionar registro → Editar → cambiar valor → Aceptar → verificar cambio
 *   7.  Seleccionar registro → Eliminar → Confirmar → verificar tabla vacía
 *   8.  Añadir escala final → rellenar → Aceptar → verificar fila
 *
 * Escala usada: "Nivel de Conciencia" (Escala de Glasgow)
 *   Campos:
 *     - Apertura Ocular  → ID: 63BB0196-CC11-42CC-9C8B-5335820A6AC6
 *       opciones: -0 Espontánea | -1 A la voz | -2 Al dolor | -3 Sin respuesta
 *     - Respuesta verbal → ID: F626CFFE-1D45-49E8-9271-C47144631674
 *       opciones: -0 Orientado | -1 Confusa | -2 Palabras incongruentes | ...
 *     - Respuesta motora → ID: 39F69981-5DB2-46AB-AB7C-C91EC61935CF
 *       opciones: -0 Obedece órdenes | -1 Localiza estímulo | ...
 *
 *   Los GUIDs son el ID de la pregunta en el catálogo — estables para esta escala.
 *   Las opciones siguen el patrón {GUID}-{índice}.
 *
 * IDs de acción:
 *   - add_46-Nivel-de-Conciencia   → seleccionar escala en submenú Añadir
 *   - add                          → botón submenú "Añadir" del menú Acciones
 *   - edit                         → "Editar" del menú Acciones
 *   - delete                       → "Eliminar" del menú Acciones
 *   - accept-ScaleRegisterContainer-button / cancel-ScaleRegisterContainer-button
 *   - alert-confirm                → confirmar eliminación
 *   - gridId-0                     → primera fila de la tabla
 *   - grid-gridId-0-name           → celda Nombre (verificación)
 *   - grid-gridId-0-value          → celda Valor numérico (verificación)
 */
public class ScaleRecordTest extends ClassBaseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // IDs de los selects de la escala "Nivel de Conciencia" (GUIDs estables del catálogo)
    private static final String SELECT_APERTURA_OCULAR  = "63BB0196-CC11-42CC-9C8B-5335820A6AC6";
    private static final String SELECT_RESPUESTA_VERBAL = "F626CFFE-1D45-49E8-9271-C47144631674";
    private static final String SELECT_RESPUESTA_MOTORA = "39F69981-5DB2-46AB-AB7C-C91EC61935CF";

    // Opciones para el registro inicial: Espontánea(0) + Orientado(0) + Obedece órdenes(0) = 15pts
    private static final String APERTURA_CREATE  = SELECT_APERTURA_OCULAR  + "-0"; // Espontánea
    private static final String VERBAL_CREATE    = SELECT_RESPUESTA_VERBAL + "-0"; // Orientado
    private static final String MOTORA_CREATE    = SELECT_RESPUESTA_MOTORA + "-0"; // Obedece órdenes
    private static final String VALUE_CREATE     = "15";

    // Para el modify: cambia Apertura Ocular a "Al dolor"(2) → valor baja a 13
    private static final String APERTURA_MODIFY  = SELECT_APERTURA_OCULAR  + "-2"; // Al dolor
    private static final String VALUE_MODIFY     = "13";

    // Registro final: A la voz(1) + Orientado(0) + Obedece órdenes(0) = 14pts
    private static final String APERTURA_FINAL   = SELECT_APERTURA_OCULAR  + "-1"; // A la voz
    private static final String VALUE_FINAL      = "14";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. LOGIN
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_nurse"));
        driver.findElement(By.id("password")).sendKeys(ConfigReader.get("password_nurse"));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        try {
            WebDriverWait centerWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            centerWait.until(ExpectedConditions.elementToBeClickable(
                    By.id("defaultButtonId"))).click();
        } catch (TimeoutException e) {
            // Sin pantalla de selección de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. HOSPITALIZACIÓN → LISTA DE TRABAJO
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void EnterHospitalizationWL() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.tagName("gc-hospitalization-list")));

        Reporter.log("Lista de trabajo de Hospitalización cargada.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BUSCAR Y SELECCIONAR PACIENTE
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        String patientNH = ConfigReader.get("pacqah1NH");

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        filterInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        filterInput.sendKeys(patientNH);

        try {
            WebDriverWait loadWait = new WebDriverWait(driver, Duration.ofSeconds(30));
            WebElement loadBtn = localWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("load-all-information")));
            loadBtn.click();
            loadWait.until(ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        } catch (TimeoutException e) {
            // Paciente ya visible sin necesidad de carga adicional
        }

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("gridId-0"))).click();

        Reporter.log("Paciente NH=" + patientNH + " seleccionado.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. VER HISTORIA → NUEVA PESTAÑA
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 4, dependsOnMethods = {"FilterAndSelectPatient"})
    public void OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("see_history"))).click();

        // Heredado de ClassBaseTest
        SelectAccessReason("Guardia");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("clinical-history-back-button")));

        // Esperar a que el overlay initial-loading desaparezca antes de interactuar
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("initial-loading")));

        // Heredados de ClassBaseTest
        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        Reporter.log("Historial clínico abierto.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. NAVEGAR A REGISTRO DE ESCALAS
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void NavigateToScaleRegistry() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Desplegar sección Registros y esperar a que el submenú sea visible
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("registers-sidebar"))).click();

        // Esperar a que el ítem del submenú sea clickable antes de hacer click
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("scale_registers-sidebar"))).click();

        // Esperar al botón Acciones — ID estable que aparece cuando
        // el componente de escalas ha terminado de cargar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button")));

        Reporter.log("Sección Registro de escalas cargada.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. AÑADIR ESCALA
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 6, dependsOnMethods = {"NavigateToScaleRegistry"})
    public void AddScale() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Contar filas antes de añadir
        int rowsBefore = driver.findElements(By.cssSelector("[id^='gridId-']")).size();

        openActionsMenuAndClick(localWait, "add");

        // Seleccionar "Nivel de Conciencia" en el submenú via JS para evitar overlay CDK
        WebElement scaleOption = localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("add_46-Nivel-de-Conciencia")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", scaleOption);

        // Esperar a que el modal esté visible
        localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id(SELECT_APERTURA_OCULAR)));

        // Rellenar los tres campos de la Escala de Glasgow
        selectOption(localWait, SELECT_APERTURA_OCULAR,  APERTURA_CREATE);
        selectOption(localWait, SELECT_RESPUESTA_VERBAL, VERBAL_CREATE);
        selectOption(localWait, SELECT_RESPUESTA_MOTORA, MOTORA_CREATE);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-ScaleRegisterContainer-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Verificar que hay una fila más que antes
        localWait.until(d ->
                d.findElements(By.cssSelector("[id^='gridId-']")).size() == rowsBefore + 1);

        Reporter.log("Escala añadida. Total filas: "
                + driver.findElements(By.cssSelector("[id^='gridId-']")).size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. MODIFICAR LA ESCALA
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 7, dependsOnMethods = {"AddScale"})
    public void ModifyScale() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la última fila — la recién creada (tabla ordena ascendente)
        clickLastRow(localWait);

        openActionsMenuAndClick(localWait, "edit");

        // Esperar a que el modal de edición esté visible
        localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id(SELECT_APERTURA_OCULAR)));

        // Cambiar Apertura Ocular: Espontánea → Al dolor (valor pasa de 15 a 13)
        selectOption(localWait, SELECT_APERTURA_OCULAR, APERTURA_MODIFY);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-ScaleRegisterContainer-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Verificar que el valor de la última fila cambió a 13
        localWait.until(d -> {
            java.util.List<org.openqa.selenium.WebElement> rows =
                    d.findElements(By.cssSelector("[id^='gridId-']"));
            if (rows.isEmpty()) return false;
            String lastId = "grid-" + rows.getLast().getAttribute("id") + "-value";
            org.openqa.selenium.WebElement cell = d.findElement(By.id(lastId));
            return VALUE_MODIFY.equals(cell.getText().trim());
        });

        Reporter.log("Escala modificada. Valor: " + VALUE_MODIFY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. ELIMINAR LA ESCALA
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 8, dependsOnMethods = {"ModifyScale"})
    public void DeleteScale() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = driver.findElements(By.cssSelector("[id^='gridId-']")).size();

        // Seleccionar la última fila — la recién modificada
        clickLastRow(localWait);

        openActionsMenuAndClick(localWait, "delete");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));

        // Verificar que el número de filas decrementó en 1
        localWait.until(d ->
                d.findElements(By.cssSelector("[id^='gridId-']")).size() == rowsBefore - 1);

        Reporter.log("Escala eliminada. Filas restantes: "
                + driver.findElements(By.cssSelector("[id^='gridId-']")).size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. AÑADIR ESCALA FINAL
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 9, dependsOnMethods = {"DeleteScale"})
    public void AddFinalScale() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        int rowsBefore = driver.findElements(By.cssSelector("[id^='gridId-']")).size();

        openActionsMenuAndClick(localWait, "add");

        // Seleccionar "Nivel de Conciencia" en el submenú via JS para evitar overlay CDK
        WebElement scaleOption = localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("add_46-Nivel-de-Conciencia")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", scaleOption);

        localWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id(SELECT_APERTURA_OCULAR)));

        // Apertura: A la voz(1) + Verbal: Orientado(0) + Motora: Obedece órdenes(0) = 14pts
        selectOption(localWait, SELECT_APERTURA_OCULAR,  APERTURA_FINAL);
        selectOption(localWait, SELECT_RESPUESTA_VERBAL, VERBAL_CREATE);
        selectOption(localWait, SELECT_RESPUESTA_MOTORA, MOTORA_CREATE);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-ScaleRegisterContainer-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.tagName("mat-dialog-container")));

        // Verificar que hay una fila más que antes
        localWait.until(d ->
                d.findElements(By.cssSelector("[id^='gridId-']")).size() == rowsBefore + 1);

        Reporter.log("Escala final añadida. Total filas: "
                + driver.findElements(By.cssSelector("[id^='gridId-']")).size());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    /**
     * Selecciona la última fila de la tabla.
     * La tabla ordena ascendente por fecha, así que el registro más reciente
     * siempre está en la última posición (gridId-N con N más alto).
     */
    private void clickLastRow(WebDriverWait localWait) {
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='gridId-']")));
        java.util.List<WebElement> rows = driver.findElements(
                By.cssSelector("[id^='gridId-']"));
        rows.getLast().click();
    }
}
