package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Dietas y recomendaciones
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → Ver historia
 *   3.  Dietas y recomendaciones
 *   4.  Acciones → Nueva dieta → elegir dieta del select → se carga plantilla → Aceptar
 *   5.  Seleccionar la dieta del grid → Acciones → Abrir → modificar texto → Aceptar
 *   6.  Seleccionar la dieta del grid → Acciones → Eliminar → Confirmar
 *   7.  Acciones → Nueva dieta → elegir otra dieta → Aceptar
 *
 * NOTA sobre el modo solo lectura:
 *   Al entrar en la historia con este médico puede aparecer el banner "Modo solo lectura"
 *   si otro usuario tiene la sesión activa. Para salir de él hay que navegar al dashboard
 *   y volver a la sección — el handleReadOnlyAlert del ClassBaseTest gestiona el
 *   continue-button al abrir la historia; si persiste el modo lectura en dietas,
 *   se navega al dashboard y se vuelve.
 *
 * IDs clave:
 *   - template                                   → mat-select de dietas
 *   - accept-PatientAdviceDialogComponent-button → Aceptar formulario
 *   - cancel-PatientAdviceDialogComponent-button → Cancelar formulario
 *   - .angular-editor-textarea                   → editor de texto (sin ID)
 *   - grid-gridId-0-date                         → primera fila del grid
 *   - alert-confirm / alert-cancel               → confirmacion de eliminacion
 */
public class DietsAndRecommendationsTest extends ClassBaseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // Dietas a usar (indices del mat-select template)
    private static final String FIRST_DIET_OPTION  = "template-4";  // DIETA 2000 CALORIAS
    private static final String SECOND_DIET_OPTION = "template-9";  // POBRE EN SAL

    // -------------------------------------------------------------------------
    // 1. LOGIN
    // -------------------------------------------------------------------------
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_doctor"));

        driver.findElement(By.id("password"))
                .sendKeys(ConfigReader.get("password_doctor"));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        try {
            WebElement centerBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("defaultButtonId")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerBtn);
        } catch (TimeoutException e) {
            // Sin pantalla de seleccion de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado como medico.");
    }

    // -------------------------------------------------------------------------
    // 2. HOSPITALIZACION → LISTA DE TRABAJO
    // -------------------------------------------------------------------------
    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void EnterHospitalizationWL() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("filter-input")));

        Reporter.log("Lista de trabajo de Hospitalizacion cargada.");
    }

    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL"})
    public void FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        String patientNH = ConfigReader.get("pacqah1NH");

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(patientNH);

        try {
            WebElement loadBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("load-all-information")));
            loadBtn.click();
        } catch (TimeoutException e) {
            // El paciente ya era visible
        }

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-patient"))).click();

        Reporter.log("Paciente NH=" + patientNH + " seleccionado.");
    }

    @Test(priority = 4, dependsOnMethods = {"FilterAndSelectPatient"})
    public void OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "see_history");

        SelectAccessReason("Guardia");

        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        handleVitalAlertsDialog();
        handleReadOnlyAlert();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("diets_and_recommendations-sidebar")));

        Reporter.log("Historial clinico abierto en nueva pestana.");
    }

    // -------------------------------------------------------------------------
    // 3. DIETAS Y RECOMENDACIONES
    // -------------------------------------------------------------------------
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterDietsAndRecommendations() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("diets_and_recommendations-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button")));

        // Si persiste el modo solo lectura tras entrar en la seccion,
        // navegar al dashboard y volver — esto desactiva la sesion de solo lectura
        handleReadOnlyMode(localWait);

        Reporter.log("Seccion Dietas y recomendaciones cargada.");
    }

    // -------------------------------------------------------------------------
    // 4. NUEVA DIETA → GUARDAR
    // -------------------------------------------------------------------------
    @Test(priority = 6, dependsOnMethods = {"EnterDietsAndRecommendations"})
    public void CreateDiet() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_diet");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-PatientAdviceDialogComponent-button")));

        // Seleccionar la dieta del select — se carga la plantilla automaticamente
        selectWhenReady(localWait, "template", FIRST_DIET_OPTION);

        // Verificar que la plantilla se cargo en el editor
        localWait.until(d -> {
            WebElement editor = d.findElement(By.cssSelector(".angular-editor-textarea"));
            return !editor.getText().trim().isEmpty();
        });

        // Guardar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-PatientAdviceDialogComponent-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-PatientAdviceDialogComponent-button")));

        // Verificar que la dieta aparece en el grid
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("grid-gridId-0-date")));

        Reporter.log("Dieta creada correctamente.");
    }

    // -------------------------------------------------------------------------
    // 5. ABRIR LA DIETA → MODIFICAR TEXTO → GUARDAR
    // -------------------------------------------------------------------------
    @Test(priority = 7, dependsOnMethods = {"CreateDiet"})
    public void EditDiet() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la dieta del grid
        WebElement dietRow = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-date")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", dietRow);

        // Acciones → Abrir
        openActionsMenuAndClick(localWait, "open");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-PatientAdviceDialogComponent-button")));

        // Esperar a que el editor tenga contenido
        localWait.until(d -> {
            WebElement editor = d.findElement(By.cssSelector(".angular-editor-textarea"));
            return !editor.getText().trim().isEmpty();
        });

        // Modificar el texto: insertar texto al inicio del editor usando execCommand
        ((JavascriptExecutor) driver).executeScript(
                "var editor = document.querySelector('.angular-editor-textarea');" +
                        "editor.focus();" +
                        "var range = document.createRange();" +
                        "var firstNode = editor.firstChild || editor;" +
                        "range.setStart(firstNode, 0);" +
                        "range.setEnd(firstNode, 0);" +
                        "window.getSelection().removeAllRanges();" +
                        "window.getSelection().addRange(range);" +
                        "document.execCommand('insertText', false, 'MODIFICADO: ');");

        // Guardar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-PatientAdviceDialogComponent-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-PatientAdviceDialogComponent-button")));

        Reporter.log("Dieta editada correctamente.");
    }

    // -------------------------------------------------------------------------
    // 6. ELIMINAR LA DIETA
    // -------------------------------------------------------------------------
    @Test(priority = 8, dependsOnMethods = {"EditDiet"})
    public void DeleteDiet() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la dieta del grid
        WebElement dietRow = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-date")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", dietRow);

        // Acciones → Eliminar
        openActionsMenuAndClick(localWait, "delete");

        // Confirmar eliminacion — dialogo "¿Esta seguro de que desea borrar esta dieta?"
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("alert-confirm"))).click();

        // Esperar a que la fila desaparezca del grid
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("alert-confirm")));

        Reporter.log("Dieta eliminada correctamente.");
    }

    // -------------------------------------------------------------------------
    // 7. AÑADIR UNA SEGUNDA DIETA DISTINTA
    // -------------------------------------------------------------------------
    @Test(priority = 9, dependsOnMethods = {"DeleteDiet"})
    public void CreateSecondDiet() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_diet");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-PatientAdviceDialogComponent-button")));

        // Seleccionar una dieta diferente a la primera
        selectWhenReady(localWait, "template", SECOND_DIET_OPTION);

        // Verificar que la plantilla se cargo en el editor
        localWait.until(d -> {
            WebElement editor = d.findElement(By.cssSelector(".angular-editor-textarea"));
            return !editor.getText().trim().isEmpty();
        });

        // Guardar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-PatientAdviceDialogComponent-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-PatientAdviceDialogComponent-button")));

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("grid-gridId-0-date")));

        Reporter.log("Segunda dieta (distinta) creada correctamente.");
    }

    // =========================================================================
    // METODOS AUXILIARES
    // =========================================================================

    /**
     * Gestiona el modo solo lectura que puede aparecer en la seccion de Dietas
     * cuando otra sesion tiene la historia abierta.
     *
     * Si el banner "Modo solo lectura" esta presente, navega al dashboard y
     * vuelve a la seccion de dietas — esto desactiva la sesion de solo lectura.
     */
    private void handleReadOnlyMode(WebDriverWait localWait) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(".read_only_container")));

            Reporter.log("Modo solo lectura detectado — navegando al dashboard para salir.");

            // Navegar al dashboard y volver
            localWait.until(ExpectedConditions.elementToBeClickable(
                    By.id("summary-sidebar"))).click();

            localWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("diets_and_recommendations-sidebar")));

            localWait.until(ExpectedConditions.elementToBeClickable(
                    By.id("diets_and_recommendations-sidebar"))).click();

            localWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("actions-button")));

            // Verificar que el modo solo lectura desaparecio
            localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".read_only_container")));

            Reporter.log("Modo solo lectura desactivado.");
        } catch (TimeoutException e) {
            Reporter.log("Sin modo solo lectura — continuar normalmente.");
        }
    }

    /**
     * Abre un mat-select y selecciona la opcion indicada.
     * Usa mat-option isEmpty como sincronizacion.
     */
    private void selectWhenReady(WebDriverWait localWait, String selectId, String optionId) {
        localWait.until(d -> d.findElements(By.tagName("mat-option")).isEmpty());

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id(selectId))).click();

        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        localWait.until(d -> d.findElements(By.tagName("mat-option")).isEmpty());
    }
}
