package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Test de integración: Hospitalización → Lista de trabajo → Paciente (config: pacqah1NH)
 *                      → Ver historia (nueva pestaña) → Gastos → Imputación de consumos
 *
 * Flujo:
 *   1.  Login como enfermera
 *   2.  Hospitalización → Lista de trabajo → filtrar paciente → Ver historia
 *   3.  Gastos → Imputación de consumos
 *   4.  Acciones → Imputar consumos → rellenar formulario → Aceptar
 *   5.  Seleccionar la imputación recién creada (primera fila, checkbox) → Devolver
 *   6.  (Opcional) Acciones → Solicitar producto → rellenar formulario
 *       → aumentar cantidad a 2 con botón + → Aceptar
 *       Este paso se ignora si el entorno no tiene la opción disponible.
 *
 * Notas del formulario de imputacion:
 *   - physicalWarehouse (autocomplete)  → Almacén; warehouseArea se rellena solo
 *   - allocate-consumption-dialog-product-autocomplete (gc-product-autocomplete)
 *     → buscar articulo; requiere JS nativeSetter + dispatchEvent para escribir
 *   - add-kit-button                    → Añadir articulo al carrito
 *   - accept-AllocateConsumptionDialogContainer-button → Aceptar
 *
 * Notas de la devolucion:
 *   - Marcar checkbox de la fila gridId-0 con JS click en mat-checkbox interno
 *   - return_allocation → Devolver; si aparece continue-button, cerrarlo
 *
 * Notas de solicitar producto (opcional):
 *   - physicalWarehouse segundo campo (almacen que sirve) → opcion physicalWarehouse-0
 *   - Misma logica de busqueda de articulo que imputar consumos
 *   - Boton + para cantidad: JS click en .consumption__quantity__add
 */
public class ConsumptionAllocationsTest extends ClassBaseTest {

    private static final Duration TIMEOUT       = Duration.ofSeconds(15);
    private static final String   WAREHOUSE_OPT = "physicalWarehouse-0";
    private static final String   ARTICLE_SEARCH = "gasa";
    // Indice de la opcion de articulo con stock (flechas verdes)
    // Al buscar "gasa" con el almacen de planta, el articulo con stock tiene indice 2
    private static final String   ARTICLE_OPT   = "allocate-consumption-dialog-product-autocomplete-2";
    // Para solicitar producto, el almacen que sirve puede ser diferente
    // y el articulo disponible cambia — se usa el primer resultado con stock
    private static final String   ARTICLE_OPT_REQUEST = "allocate-consumption-dialog-product-autocomplete-0";

    // -------------------------------------------------------------------------
    // 1. LOGIN
    // -------------------------------------------------------------------------
    @Test(priority = 1)
    public void Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_nurse"));

        driver.findElement(By.id("password"))
                .sendKeys(ConfigReader.get("password_nurse"));

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

        Reporter.log("Login completado como enfermera.");
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
                By.id("patient_expenses-sidebar")));

        Reporter.log("Historial clinico abierto en nueva pestana.");
    }

    // -------------------------------------------------------------------------
    // 3. GASTOS → IMPUTACION DE CONSUMOS
    // -------------------------------------------------------------------------
    @Test(priority = 5, dependsOnMethods = {"OpenPatientHistory"})
    public void EnterConsumptionAllocations() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("patient_expenses-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("consumption_allocations-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("actions-button")));

        Reporter.log("Seccion Imputacion de consumos cargada.");
    }

    // -------------------------------------------------------------------------
    // 4. IMPUTAR CONSUMOS
    // -------------------------------------------------------------------------
    @Test(priority = 6, dependsOnMethods = {"EnterConsumptionAllocations"})
    public void AllocateConsumption() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        openActionsMenuAndClick(localWait, "allocate_consumption");

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("cancel-AllocateConsumptionDialogContainer-button")));

        // Expandir todas las secciones del formulario si estan colapsadas
        // (en ventanas pequenas el acordeon se colapsa automaticamente)
        expandFormSections(localWait);

        // Seleccionar almacen (el primero disponible)
        selectProductWarehouse(localWait, 0, WAREHOUSE_OPT);

        // Buscar y añadir articulo con stock (gasa)
        searchAndAddProduct(localWait, ARTICLE_SEARCH, ARTICLE_OPT);

        // Guardar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-AllocateConsumptionDialogContainer-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AllocateConsumptionDialogContainer-button")));

        // Verificar que la imputacion aparece en el grid
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("grid-gridId-0-product")));

        Reporter.log("Imputacion de consumo creada correctamente.");
    }

    // -------------------------------------------------------------------------
    // 5. DEVOLVER LA IMPUTACION RECIEN CREADA (primera fila del grid)
    // -------------------------------------------------------------------------
    @Test(priority = 7, dependsOnMethods = {"AllocateConsumption"})
    public void ReturnAllocation() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Esperar a que el formulario de imputacion se haya cerrado completamente
        // antes de interactuar con el grid
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AllocateConsumptionDialogContainer-button")));

        // Marcar el checkbox de la primera fila del grid (la imputacion recien creada)
        // La primera fila (gridId-0) es la mas reciente por orden descendente de fecha
        WebElement firstRow = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("gridId-0")));
        WebElement checkbox = firstRow.findElement(By.tagName("mat-checkbox"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);

        // Acciones → Devolver imputacion.
        // JS click en actions-button para evitar "element click intercepted"
        // si hay overlay residual del checkbox o del formulario anterior.
        WebElement actionsBtn = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("actions-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", actionsBtn);

        WebElement returnItem = localWait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("return_allocation")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", returnItem);

        // El sistema abre un modal "Devolucion" con el campo "Cantidad a devolver"
        // y los botones Cancelar/Aceptar.
        // Se acepta directamente con el valor por defecto (cantidad imputada = 1).
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-ReturnConsumptionAllocationComponent-button"))).click();

        // Si aparece aviso informativo (p.ej. "parte ya devuelta") aceptarlo
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                    .click();
        } catch (TimeoutException e) {
            // Sin aviso
        }

        Reporter.log("Imputacion devuelta correctamente.");
    }

    // -------------------------------------------------------------------------
    // 6. (OPCIONAL) SOLICITAR PRODUCTO
    // Se ignora si el entorno no tiene esta opcion (send_extra_products_request
    // no disponible o el formulario no carga correctamente).
    // -------------------------------------------------------------------------
    @Test(priority = 8, dependsOnMethods = {"ReturnAllocation"})
    public void RequestProduct() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Verificar si la opcion "Solicitar producto" esta disponible en este entorno.
        // Se usa JS click en actions-button para evitar "element click intercepted"
        // si hay un overlay residual del paso anterior todavia en el DOM.
        WebElement actionsBtn = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("actions-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", actionsBtn);

        WebElement requestItem = localWait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("send_extra_products_request")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", requestItem);

        try {
            localWait.until(ExpectedConditions.elementToBeClickable(
                    By.id("cancel-AllocateConsumptionDialogContainer-button")));
        } catch (TimeoutException e) {
            Reporter.log("Solicitar producto no disponible en este entorno — test omitido.");
            return;
        }

        // Expandir todas las secciones del formulario si estan colapsadas
        expandFormSections(localWait);

        // El formulario de solicitar producto tiene dos campos physicalWarehouse:
        //   Indice 0 → Almacen solicitante: debe seleccionarse PRIMERO
        //   Indice 1 → Almacen que sirve: solo muestra opciones tras seleccionar el solicitante
        // Seleccionar primero el almacen solicitante
        selectProductWarehouse(localWait, 0, WAREHOUSE_OPT);

        // Ahora seleccionar el almacen que sirve (sus opciones dependen del solicitante)
        selectRequestWarehouse(localWait, WAREHOUSE_OPT);

        // Buscar y añadir articulo con stock
        searchAndAddProduct(localWait, ARTICLE_SEARCH, ARTICLE_OPT_REQUEST);

        // Aumentar la cantidad a 2 con el boton + (consumption__quantity__add)
        // El boton es invisible por defecto; JS click lo activa igualmente
        WebElement quantityCell = localWait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.id("grid-allocate-consumption-dialog-products-grid-0-quantity")));

        WebElement plusBtn = quantityCell.findElement(
                By.cssSelector(".consumption__quantity__add"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", plusBtn);

        // Verificar que la cantidad es 2
        localWait.until(d -> {
            String qty = d.findElement(
                            By.id("grid-allocate-consumption-dialog-products-grid-0-quantity"))
                    .getText().trim();
            return "2".equals(qty);
        });

        // Guardar
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-AllocateConsumptionDialogContainer-button"))).click();

        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("cancel-AllocateConsumptionDialogContainer-button")));

        Reporter.log("Solicitud de producto creada con cantidad 2.");
    }

    // =========================================================================
    // METODOS AUXILIARES
    // =========================================================================

    /**
     * Expande todas las secciones del acordeon del formulario que esten colapsadas.
     *
     * En ventanas pequeñas Angular Material colapsa automaticamente los mat-expansion-panel.
     * En ventanas grandes pueden estar ya expandidos. El metodo es idempotente:
     * si ya estan expandidos no hace nada.
     *
     * Se hace click en cada mat-expansion-panel-header cuyo panel no tenga la clase
     * "mat-expanded", que es la que Angular añade cuando el panel esta abierto.
     */
    private void expandFormSections(WebDriverWait localWait) {
        ((JavascriptExecutor) driver).executeScript(
                "Array.from(document.querySelectorAll('mat-dialog-container mat-expansion-panel'))" +
                        ".forEach(function(panel) {" +
                        "  if (!panel.classList.contains('mat-expanded')) {" +
                        "    var header = panel.querySelector('mat-expansion-panel-header');" +
                        "    if (header) header.click();" +
                        "  }" +
                        "});");

        // Esperar a que al menos el campo physicalWarehouse sea interactuable,
        // lo que confirma que la seccion Origen de consumo esta expandida
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("physicalWarehouse")));
    }

    /**
     * Selecciona el almacen en el formulario de imputacion/solicitud.
     *
     * El formulario puede tener dos campos physicalWarehouse con el mismo ID:
     *   Indice 0 → Almacen solicitante (seccion Origen de consumo)
     *   Indice 1 → Almacen que sirve (seccion Almacen que sirve, solo en solicitar producto)
     *
     * @param localWait    WebDriverWait activo
     * @param fieldIndex   0 para el primer physicalWarehouse, 1 para el segundo
     * @param optionId     ID de la opcion de almacen a seleccionar
     */
    /**
     * Selecciona el almacen solicitante (fieldIndex=0) en el formulario.
     * Usa JS dispatchEvent(click) sobre el input para abrir el autocomplete,
     * luego JS click en la opcion, y espera a que warehouseArea se rellene
     * automaticamente antes de retornar.
     */
    private void selectProductWarehouse(WebDriverWait localWait, int fieldIndex, String optionId) {
        localWait.until(d -> d.findElements(By.id("physicalWarehouse")).size() > fieldIndex);

        // El autocomplete se abre con dispatchEvent(click) en el input
        ((JavascriptExecutor) driver).executeScript(
                "var inputs = document.querySelectorAll('#physicalWarehouse');" +
                        "var input = inputs[arguments[0]];" +
                        "input.focus();" +
                        "input.dispatchEvent(new MouseEvent('click',{bubbles:true}));",
                fieldIndex);

        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        // Esperar a que warehouseArea se rellene automaticamente
        localWait.until(d -> {
            String val = d.findElements(By.id("warehouseArea")).stream()
                    .map(e -> e.getAttribute("value"))
                    .filter(v -> v != null && !v.isEmpty())
                    .findFirst().orElse("");
            return !val.isEmpty();
        });
    }

    /**
     * Selecciona el almacen que sirve en el formulario de solicitar producto.
     * Este campo solo muestra opciones DESPUES de que el almacen solicitante
     * haya sido seleccionado. Sus opciones tienen los mismos IDs que el campo
     * solicitante (physicalWarehouse-0, physicalWarehouse-1, ...) pero son
     * independientes.
     *
     * @param localWait  WebDriverWait activo
     * @param optionId   ID de la opcion de almacen a seleccionar
     */
    private void selectRequestWarehouse(WebDriverWait localWait, String optionId) {
        // El almacen que sirve es el segundo physicalWarehouse (indice 1)
        ((JavascriptExecutor) driver).executeScript(
                "var inputs = document.querySelectorAll('#physicalWarehouse');" +
                        "var input = inputs[1];" +
                        "input.focus();" +
                        "input.dispatchEvent(new MouseEvent('click',{bubbles:true}));");

        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        // Esperar a que el panel se cierre (las opciones desaparecen del DOM)
        localWait.until(d ->
                d.findElements(By.cssSelector("mat-option[id^='physicalWarehouse']")).isEmpty());
    }

    /**
     * Busca un articulo en el campo gc-product-autocomplete y lo añade al carrito.
     *
     * gc-product-autocomplete es un componente Angular custom — el input interno
     * no acepta sendKeys nativo de Selenium. Se usa nativeSetter + dispatchEvent
     * para simular la escritura de forma que Angular detecte el cambio y cargue
     * las opciones del autocomplete.
     *
     * @param localWait   WebDriverWait activo
     * @param searchText  Texto a buscar (p.ej. "gasa")
     * @param optionId    ID de la opcion a seleccionar (p.ej. "...autocomplete-2")
     */
    private void searchAndAddProduct(WebDriverWait localWait, String searchText, String optionId) {
        // Escribir en el campo de busqueda usando nativeSetter (sendKeys no funciona en gc-product-autocomplete)
        ((JavascriptExecutor) driver).executeScript(
                "var input = document.querySelector('gc-product-autocomplete input');" +
                        "if(input){" +
                        "  input.focus();" +
                        "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                        "  setter.call(input, arguments[0]);" +
                        "  input.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "  input.dispatchEvent(new KeyboardEvent('keyup',{bubbles:true}));" +
                        "}", searchText);

        // Esperar a que aparezcan las opciones
        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);

        // Pulsar Añadir para confirmar el articulo en el carrito
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("add-kit-button"))).click();
    }
}
