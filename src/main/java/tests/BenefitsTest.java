package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Prestaciones
 *
 * Flujo:
 *   1.  Login como médico
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → Ver historia
 *   3.  Prestaciones
 *   4.  Acciones → Nueva prestación → Catálogo → elegir servicio → expandir árbol
 *       → marcar prestación → mover a lista final → Aceptar catálogo
 *       → limpiar benefitType → Aceptar formulario
 *   5.  Seleccionar prestación → Acciones → Ver prestación → cambiar cantidad → Aceptar
 *   6.  Seleccionar prestación → Acciones → Eliminar → modal se cierra automáticamente
 *   7.  Nueva prestación distinta con el mismo flujo del catálogo
 *
 * IDs clave:
 *   Formulario prestación:
 *     - benefitType                              → gc-autocomplete de prestación (limpiar antes de guardar)
 *     - show-catalog-button                      → abrir catálogo
 *     - amount                                   → cantidad (editable)
 *     - accept-BenefitFormComponent-button       → guardar
 *     - cancel-BenefitFormComponent-button       → cancelar
 *   Catálogo:
 *     - select-default-id                        → filtrar por servicio
 *     - select-default-id-{N}                    → opciones de servicio
 *     - expanded-node-button                     → expandir nodo del árbol
 *     - tree-checkbox-initial-{CODE}-{LEVEL}     → checkbox prestación (lista izquierda)
 *     - angle-right-button                       → mover selección a lista final (>)
 *     - tree-checkbox-final-{CODE}-{LEVEL}       → checkbox prestación (lista derecha, confirma selección)
 *     - accept-BenefitTypesCatalogContainer-button → aceptar catálogo
 *     - cancel-BenefitTypesCatalogContainer-button → cancelar catálogo
 *   Grid prestaciones:
 *     - grid-gridId-0-benefit / amount / date / service / entity / medical
 *   Acciones:
 *     - new_benefit / see_benefit / delete_benefit / print_benefit
 *   Modal de proceso (eliminación):
 *     - close-HeavyProcessLoaderDialogComponent-button → presente mientras procesa,
 *       desaparece AUTOMÁTICAMENTE al completar. NO requiere click.
 *
 * NOTA sobre benefitType:
 *   El campo gc-autocomplete puede quedar con texto residual tras usar el catálogo.
 *   Se limpia vía nativeSetter antes de guardar para evitar el error "Valor no encontrado".
 *
 * NOTA sobre openActionsMenuAndClick (heredado de ClassBaseTest):
 *   Usa elementToBeClickable para el botón "Acciones" y visibilityOfElementLocated
 *   para el ítem del menú, seguido de JS click. Es la estrategia correcta para
 *   Angular Material mat-menu con overlay cdk.
 */
public class BenefitsTest extends ClassBaseTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // Servicio del catálogo (select-default-id-N)
    private static final String SERVICE_OPTION = "select-default-id-0"; // ENFERMERÍA

    // Primera prestación: AGUJA P/ULTRASONIDO 2.0
    private static final String BENEFIT_CODE_1 = "1111020001";

    // Segunda prestación: ANGIOGRAFIA CAROTIDEA UNIL.xPUNCION FEMO
    private static final String BENEFIT_CODE_2 = "3131010001";

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
            // Sin pantalla de selección de centro
        }

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("Login completado como médico.");
    }

    // -------------------------------------------------------------------------
    // 2. HOSPITALIZACIÓN → LISTA DE TRABAJO
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

        Reporter.log("Lista de trabajo de Hospitalización cargada.");
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
            // El paciente ya era visible sin necesidad de cargar todo
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
                By.id("benefits-sidebar")));

        Reporter.log("Historial clínico abierto en nueva pestaña.");
    }

    // -------------------------------------------------------------------------
    // 3. PRESTACIONES
    // -------------------------------------------------------------------------
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterBenefits() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("benefits-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button")));

        Reporter.log("Sección Prestaciones cargada.");
    }

    // -------------------------------------------------------------------------
    // 4. NUEVA PRESTACIÓN → GUARDAR
    // -------------------------------------------------------------------------
    @Test(priority = 6, dependsOnMethods = {"EnterBenefits"})
    public void CreateBenefit() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_benefit");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-BenefitFormComponent-button")));

        selectBenefitFromCatalog(localWait, SERVICE_OPTION, BENEFIT_CODE_1);

        acceptBenefitForm(localWait);

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("grid-gridId-0-benefit")));

        Reporter.log("Prestación creada: " + BENEFIT_CODE_1);
    }

    // -------------------------------------------------------------------------
    // 5. VER/EDITAR PRESTACIÓN → CAMBIAR CANTIDAD → GUARDAR
    // -------------------------------------------------------------------------
    @Test(priority = 7, dependsOnMethods = {"CreateBenefit"})
    public void EditBenefit() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila del grid
        WebElement row = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-benefit")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", row);

        // Acciones → Ver prestación
        openActionsMenuAndClick(localWait, "see_benefit");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-BenefitFormComponent-button")));

        // Cambiar la cantidad a 2
        WebElement amountField = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("amount")));
        amountField.click();
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].select();" +
                        "document.execCommand('insertText', false, '2');", amountField);

        acceptBenefitForm(localWait);

        // Verificar que la cantidad quedó actualizada en el grid
        localWait.until(d ->
                "2".equals(d.findElement(By.id("grid-gridId-0-amount")).getText().trim()));

        Reporter.log("Prestación editada: cantidad cambiada a 2.");
    }

    // -------------------------------------------------------------------------
    // 6. ELIMINAR PRESTACIÓN
    // -------------------------------------------------------------------------
    @Test(priority = 8, dependsOnMethods = {"EditBenefit"})
    public void DeleteBenefit() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Seleccionar la fila del grid
        WebElement row = localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("grid-gridId-0-benefit")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", row);

        // Acciones → Eliminar prestación
        openActionsMenuAndClick(localWait, "delete_benefit");

        // El modal "Procesando / Proceso completado con éxito" se cierra AUTOMÁTICAMENTE.
        // No tiene botón que el usuario deba pulsar: Angular lo destruye al terminar.
        // Estrategia: esperar a que el elemento aparezca (proceso en curso)
        // y luego a que desaparezca (proceso terminado) antes de continuar.
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("close-HeavyProcessLoaderDialogComponent-button")));
        longWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("close-HeavyProcessLoaderDialogComponent-button")));

        // Verificar que la fila desapareció del grid
        longWait.until(d ->
                d.findElements(By.id("grid-gridId-0-benefit")).isEmpty());

        Reporter.log("Prestación eliminada correctamente.");
    }

    // -------------------------------------------------------------------------
    // 7. NUEVA PRESTACIÓN DISTINTA
    // -------------------------------------------------------------------------
    @Test(priority = 9, dependsOnMethods = {"DeleteBenefit"})
    public void CreateSecondBenefit() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "new_benefit");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-BenefitFormComponent-button")));

        selectBenefitFromCatalog(localWait, SERVICE_OPTION, BENEFIT_CODE_2);

        acceptBenefitForm(localWait);

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("grid-gridId-0-benefit")));

        Reporter.log("Segunda prestación creada: " + BENEFIT_CODE_2);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Abre el catálogo de prestaciones, filtra por servicio, expande el árbol,
     * marca el checkbox de la prestación indicada, la mueve a la lista final
     * y acepta el catálogo.
     *
     * El catálogo usa un árbol de dos listas (inicial/final):
     *   - Lista izquierda (initial): prestaciones disponibles del servicio
     *   - Lista derecha (final): prestaciones seleccionadas
     *   - angle-right-button (>): mueve la selección de izquierda a derecha
     *
     * Los checkboxes tienen IDs estables por código:
     *   tree-checkbox-initial-{CODE}-{LEVEL}  → lista izquierda
     *   tree-checkbox-final-{CODE}-{LEVEL}    → lista derecha (confirma que se movió)
     *
     * Todos los clicks usan JS para evitar "element click intercepted" causado
     * por los overlays cdk del mat-select y el propio catálogo.
     *
     * @param localWait    WebDriverWait activo
     * @param serviceOptId ID de la opción de servicio (ej. "select-default-id-0")
     * @param benefitCode  Código de la prestación (ej. "1111020001")
     */
    private void selectBenefitFromCatalog(WebDriverWait localWait,
                                          String serviceOptId, String benefitCode) {
        // Abrir catálogo
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("show-catalog-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('show-catalog-button').click();");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-BenefitTypesCatalogContainer-button")));

        // Filtrar por servicio (mat-select — se usa selectOption de ClassBaseTest)
        selectOption(localWait, "select-default-id", serviceOptId);

        // Expandir árbol tras carga
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("expanded-node-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('expanded-node-button').click();");

        // Marcar checkbox de la prestación
        String checkboxId = "tree-checkbox-initial-" + benefitCode + "-1";
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id(checkboxId)));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('" + checkboxId + "').click();");

        // Verificar que el checkbox quedó marcado (Angular añade "mat-checkbox-checked")
        localWait.until(d -> {
            Object checked = ((JavascriptExecutor) d).executeScript(
                    "var cb = document.getElementById('" + checkboxId + "');" +
                            "return cb ? cb.classList.contains('mat-checkbox-checked') : false;");
            return Boolean.TRUE.equals(checked);
        });

        // Mover a lista final (>)
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("angle-right-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('angle-right-button').click();");

        // Confirmar que aparece en la lista derecha
        String finalCheckboxId = "tree-checkbox-final-" + benefitCode + "-1";
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id(finalCheckboxId)));

        // Aceptar catálogo
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept-BenefitTypesCatalogContainer-button")));
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('accept-BenefitTypesCatalogContainer-button').click();");

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-BenefitTypesCatalogContainer-button")));

        Reporter.log("Prestación " + benefitCode + " seleccionada del catálogo.");
    }

    /**
     * Limpia el campo benefitType y guarda el formulario de prestación.
     *
     * El gc-autocomplete puede quedar con texto residual tras usar el catálogo.
     * La limpieza via nativeSetter + dispatchEvent resetea el estado Angular
     * del componente antes de guardar, evitando el error "Valor no encontrado".
     *
     * @param localWait WebDriverWait activo
     */
    private void acceptBenefitForm(WebDriverWait localWait) {
        ((JavascriptExecutor) driver).executeScript(
                "var f = document.getElementById('benefitType');" +
                        "if(f){" +
                        "  var setter = Object.getOwnPropertyDescriptor(" +
                        "    window.HTMLInputElement.prototype,'value').set;" +
                        "  setter.call(f, '');" +
                        "  f.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "}");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-BenefitFormComponent-button"))).click();

        // Gestionar aviso informativo si aparece (mismo botón que handleReadOnlyAlert)
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                    .click();
        } catch (TimeoutException e) {
            // Sin aviso
        }

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-BenefitFormComponent-button")));
    }
}
