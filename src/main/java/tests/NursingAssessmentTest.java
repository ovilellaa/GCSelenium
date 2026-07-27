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
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Test: Valoración de Enfermería
 *
 * ── Credenciales ──────────────────────────────────────────────────────────────
 *   username_nurse / password_nurse
 *
 * ── Paciente ──────────────────────────────────────────────────────────────────
 *   pacqah1NH  (De La Torre Albuquerque, Juan)
 *
 * ── Ruta ──────────────────────────────────────────────────────────────────────
 *   Hospitalización → Lista de trabajo → paciente → Ver historia (nueva pestaña)
 *   → Curso clínico → Valoración de enfermería
 *
 * ── Flujo principal ───────────────────────────────────────────────────────────
 *   1.  Login enfermera
 *   2.  Hospitalización → worklist → filtrar y seleccionar paciente
 *   3.  Acciones → Ver historia → nueva pestaña
 *   4.  Curso clínico → Valoración de enfermería
 *   5.  Botón "Nueva"  (id literal = "undefined")
 *   6.  Modal de confirmación → Confirmar  (alert-confirm)  [opcional por entorno]
 *   7.  Patrones:
 *         · Percepción (patrón 0) → Sí
 *         · Resto de patrones     → No
 *         Cada botón se clica con arguments[0].click() + sleep(300ms) para que
 *         Angular NgZone procese el cambio antes del siguiente click.
 *   8.  Escalas: detección dinámica del nº de escalas presentes;
 *         por cada escala → abrir → rellenar todos los campos → Aceptar
 *   9.  Guardar valoración  (acceptWidgetButton)
 *
 * ── Notas de IDs relevantes ───────────────────────────────────────────────────
 *   Botón "Nueva"                   → id="undefined"  (generado así por Angular)
 *   Modal confirmación nueva        → alert-confirm / alert-cancel
 *   Modal "episodio en otra sesión" → continue-button              [opcional]
 *   Modal alertas vitales           → alert-confirm                [opcional]
 *
 *   Botones de PATRÓN  → clase "check-pattern pattern"  (sin ID)
 *                        22 botones = 11 patrones × [Sí, No]  en orden DOM
 *                        índice par   = Sí del patrón N
 *                        índice impar = No del patrón N
 *   ⚠ NO usar "button.check-pattern" a secas: incluye también los botones de
 *     escala ("check-pattern scale") y rompe la lógica de pares.
 *   ⚠ Los clicks en patrones deben hacerse de uno en uno con arguments[0].click()
 *     y una pausa de 300ms entre cada uno para que Angular NgZone registre el
 *     cambio. Un click JS en batch no dispara el ciclo de detección de cambios.
 *
 *   Botones de ESCALA  → clase "check-pattern scale"    (sin ID)
 *   Opciones de mat-select          → patrón  "{selectId}-{índice}"
 *   Aceptar diálogo escala          → accept-ScaleRegisterContainer-button
 *   Cancelar diálogo escala         → cancel-ScaleRegisterContainer-button
 *   Guardar valoración              → acceptWidgetButton
 *   Cancelar valoración             → cancelWidgetButton
 */
public class NursingAssessmentTest extends ClassBaseTest {

    private static final Duration TIMEOUT      = Duration.ofSeconds(15);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);
    private static final Random   RNG          = new Random();

    // =========================================================================
    // TC1 — LOGIN
    // =========================================================================
    @Test(priority = 1)
    public void TC1_Login() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        GotoToUrl();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("username"))).sendKeys(ConfigReader.get("username_nurse"));

        driver.findElement(By.id("password"))
                .sendKeys(ConfigReader.get("password_nurse"));

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("login-button"))).click();

        // Selector de centro (puede no aparecer en todos los entornos)
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("defaultButtonId")))
                    .click();
        } catch (TimeoutException ignored) {}

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("hospitalization-sidebar")));

        Reporter.log("TC1 OK — Login enfermera completado.");
    }

    // =========================================================================
    // TC2 — HOSPITALIZACIÓN → LISTA DE TRABAJO
    // =========================================================================
    @Test(priority = 2, dependsOnMethods = {"TC1_Login"})
    public void TC2_EnterHospitalizationWorklist() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("hospitalization-sidebar"))).click();

        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("worklist-sidebar"))).click();

        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("filter-input")));

        Reporter.log("TC2 OK — Lista de hospitalización cargada.");
    }

    // =========================================================================
    // TC3 — FILTRAR Y SELECCIONAR PACIENTE
    // =========================================================================
    @Test(priority = 3, dependsOnMethods = {"TC2_EnterHospitalizationWorklist"})
    public void TC3_FilterAndSelectPatient() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        WebElement filterInput = localWait.until(
                ExpectedConditions.elementToBeClickable(By.id("filter-input")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].select();", filterInput);
        filterInput.sendKeys(ConfigReader.get("pacqah1NH"));

        // Cargar filas extra si aparece el botón (la lista puede estar paginada)
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.id("load-all-information")))
                    .click();
        } catch (TimeoutException ignored) {}

        // Esperar y clicar la primera celda de paciente que aparezca
        WebElement patientCell = localWait.until(d -> {
            List<WebElement> cells = d.findElements(
                    By.cssSelector("[id^='grid-gridId-'][id$='-patient']"));
            return cells.isEmpty() ? null : cells.get(0);
        });
        patientCell.click();

        Reporter.log("TC3 OK — Paciente seleccionado.");
    }

    // =========================================================================
    // TC4 — ABRIR HISTORIA CLÍNICA
    // =========================================================================
    @Test(priority = 4, dependsOnMethods = {"TC3_FilterAndSelectPatient"})
    public void TC4_OpenPatientHistory() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Acciones → Ver historia
        openActionsMenuAndClick(localWait, "see_history");

        // Cambiar a la nueva pestaña
        localWait.until(d -> d.getWindowHandles().size() > 1);
        SwitchToTab(GetLastTabOpened());

        // Modal "episodio abierto en otra sesión" → Continuar  [puede no aparecer]
        clickIfPresent("continue-button", 5);

        // Modal de alertas vitales → Aceptar  [puede no aparecer]
        clickIfPresent("alert-confirm", 5);

        // Esperar a que la historia esté cargada
        localWait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("Curso clínico-sidebar")));

        Reporter.log("TC4 OK — Historia clínica abierta en nueva pestaña.");
    }

    // =========================================================================
    // TC5 — CURSO CLÍNICO → VALORACIÓN DE ENFERMERÍA
    // =========================================================================
    @Test(priority = 5, dependsOnMethods = {"TC4_OpenPatientHistory"})
    public void TC5_NavigateToNursingAssessment() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Expandir Curso clínico en el menú lateral
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("Curso clínico-sidebar"))).click();

        // Entrar en Valoración de enfermería
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("nursing_assessment-sidebar"))).click();

        // Confirmar que la pantalla está cargada (botón Nueva presente)
        localWait.until(ExpectedConditions.presenceOfElementLocated(By.id("undefined")));

        Reporter.log("TC5 OK — Valoración de enfermería cargada.");
    }

    // =========================================================================
    // TC6 — CREAR NUEVA VALORACIÓN
    // =========================================================================
    @Test(priority = 6, dependsOnMethods = {"TC5_NavigateToNursingAssessment"})
    public void TC6_CreateNewAssessment() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Esperar a que el campo "Motivo" (id="reason") esté cargado con valor antes
        // de clicar "Nueva". Si se clica antes de que Angular termine de cargar el
        // motivo del episodio, el formulario se resetea al recibir la respuesta.
        localWait.until(d -> {
            WebElement reason = d.findElement(By.id("reason"));
            String val = reason.getAttribute("value");
            return val != null && !val.trim().isEmpty();
        });

        // El botón "Nueva" tiene literalmente id="undefined" (así lo genera Angular)
        jsClick("undefined");

        // Modal "¿Desea crear una nueva valoración?" → Confirmar  [puede no aparecer]
        boolean confirmed = clickIfPresent("alert-confirm", 5);
        if (confirmed) Reporter.log("Modal de confirmación de nueva valoración aceptado.");

        // Esperar a que aparezcan los botones de patrón
        localWait.until(d ->
                !d.findElements(By.cssSelector("button.check-pattern.pattern")).isEmpty());

        Reporter.log("TC6 OK — Nueva valoración creada, patrones visibles.");
    }

    // =========================================================================
    // TC7 — SELECCIONAR PATRONES
    // =========================================================================
    @Test(priority = 7, dependsOnMethods = {"TC6_CreateNewAssessment"})
    public void TC7_SelectPatterns() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // Esperar a que los botones estén en el DOM
        localWait.until(d ->
                !d.findElements(By.cssSelector("button.check-pattern.pattern")).isEmpty());

        // Obtener el número total de patrones (total botones / 2)
        int totalPatterns = driver.findElements(
                By.cssSelector("button.check-pattern.pattern")).size() / 2;

        // ── Patrón 0 (Percepción) → Sí ──────────────────────────────────────
        // XPath posicional: primer botón "Sí" entre los botones de patrón
        // WebElement.click() nativo es necesario: executeScript(".click()") no
        // pasa por el sistema de eventos del navegador y Angular no detecta el cambio.
        driver.findElement(By.xpath(
                "(//button[contains(@class,'check-pattern') and " +
                        "contains(@class,'pattern') and normalize-space(text())='Sí'])[1]")).click();
        sleep(500);

        // ── Resto de patrones → No ───────────────────────────────────────────
        // Hay 11 botones "No" en total (uno por patrón, posiciones 1..11 en XPath).
        // Posición 1 = No de Percepción (patrón 0) → NO clickar, ya tiene Sí.
        // Posiciones 2..11 = No de los patrones 1..10 → clickar todos.
        // Se re-localiza en cada iteración para evitar StaleElementReferenceException.
        for (int n = 2; n <= totalPatterns; n++) {
            // XPath posicional: n-ésimo botón "No" (1-based) entre los de patrón
            driver.findElement(By.xpath(
                    "(//button[contains(@class,'check-pattern') and " +
                            "contains(@class,'pattern') and normalize-space(text())='No'])[" + n + "]")).click();
            sleep(300);
        }

        // Verificar que aparecen botones de escala (consecuencia del Sí en Percepción)
        localWait.until(d ->
                !d.findElements(By.cssSelector("button.check-pattern.scale")).isEmpty());

        Reporter.log("TC7 OK — Patrones seleccionados. Escalas visibles.");
    }

    // =========================================================================
    // TC8 — RELLENAR ESCALAS (detección y relleno dinámico)
    // =========================================================================
    @Test(priority = 8, dependsOnMethods = {"TC7_SelectPatterns"})
    public void TC8_FillScales() {
        WebDriverWait localWait = new WebDriverWait(driver, LONG_TIMEOUT);

        // Contar cuántas escalas hay (sin hardcodear el número)
        List<WebElement> scaleBtns = localWait.until(d -> {
            List<WebElement> btns = d.findElements(
                    By.cssSelector("button.check-pattern.scale"));
            return btns.isEmpty() ? null : btns;
        });

        int totalScales = scaleBtns.size();
        Reporter.log("Escalas detectadas: " + totalScales);

        for (int i = 0; i < totalScales; i++) {
            // Obtener botones frescos en cada iteración
            List<WebElement> currentBtns = driver.findElements(
                    By.cssSelector("button.check-pattern.scale"));
            WebElement scaleBtn = currentBtns.get(i);
            String scaleName = scaleBtn.getText().trim();
            Reporter.log("→ Abriendo escala [" + (i + 1) + "/" + totalScales + "]: " + scaleName);

            // Abrir el formulario de la escala
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", scaleBtn);

            // Esperar a que el diálogo esté listo
            localWait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("accept-ScaleRegisterContainer-button")));

            // Rellenar todos los campos detectados dinámicamente
            fillOpenScaleForm(localWait);

            // Aceptar y esperar a que el diálogo se cierre
            jsClick("accept-ScaleRegisterContainer-button");
            localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("accept-ScaleRegisterContainer-button")));

            Reporter.log("  ✓ Escala guardada: " + scaleName);
        }

        Reporter.log("TC8 OK — Todas las escalas rellenadas y guardadas.");
    }

    // =========================================================================
    // TC9 — GUARDAR VALORACIÓN
    // =========================================================================
    @Test(priority = 9, dependsOnMethods = {"TC8_FillScales"})
    public void TC9_SaveAssessment() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // El botón guardar (check verde del toolbar) tiene id="acceptWidgetButton"
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("acceptWidgetButton"))).click();

        // Siempre aparece un modal informativo al guardar:
        //   "Recuerde cumplimentar las escalas obligatorias de su centro..."
        // Es un aviso fijo del centro — hay que aceptarlo siempre con continue-button.
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("continue-button"))).click();

        // Esperar a que el modo edición termine (acceptWidgetButton desaparece al guardar)
        localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("acceptWidgetButton")));

        Reporter.log("TC9 OK — Valoración de enfermería guardada correctamente.");
    }


    // =========================================================================
    // TC10 — VERIFICAR QUE LA VALORACIÓN SE HA GUARDADO
    // =========================================================================
    @Test(priority = 10, dependsOnMethods = {"TC9_SaveAssessment"})
    public void TC10_VerifySavedAssessment() {
        WebDriverWait localWait = new WebDriverWait(driver, TIMEOUT);

        // ── Salir a otra sección y volver ──────────────────────────────────────
        // Clicar "Evolución" para salir de Valoración de enfermería
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("evolution-sidebar"))).click();

        // Volver a Valoración de enfermería
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("nursing_assessment-sidebar"))).click();

        // Esperar a que el selector de fecha (flecha) esté disponible
        localWait.until(ExpectedConditions.elementToBeClickable(By.id("date")));

        // ── Abrir el selector de fecha y seleccionar la valoración más reciente ─
        // date-0 es siempre la valoración más reciente (la que acabamos de guardar)
        localWait.until(ExpectedConditions.elementToBeClickable(By.id("date"))).click();
        localWait.until(ExpectedConditions.elementToBeClickable(By.id("date-0"))).click();

        // ── Verificar que la valoración se ha cargado correctamente ────────────
        // Esperar a que los patrones estén visibles
        localWait.until(d ->
                !d.findElements(By.cssSelector("button.check-pattern.pattern")).isEmpty());

        // Verificar 1: Percepción=Sí está marcado.
        // El botón seleccionado tiene background-color con la CSS variable del tema
        // (--theme-primary-color-A100). Se comprueba que el valor NO es el gris por defecto.
        String siColor = (String) ((JavascriptExecutor) driver).executeScript(
                "var btns = document.querySelectorAll('button.check-pattern.pattern');" +
                        "return btns[0] ? btns[0].style.backgroundColor : '';");

        boolean percepcionSiMarcado = siColor != null && !siColor.isEmpty()
                && !siColor.equals("rgb(241, 241, 241)");
        Assert.assertTrue(percepcionSiMarcado,
                "La valoración guardada no tiene Percepción=Sí marcado. Color: " + siColor);

        // Verificar 2: todas las escalas están rellenas.
        // Una escala rellena tiene un div.severity-icon con background-color (naranja/rojo/verde...).
        // Una escala vacía no tiene ese div o su background-color está vacío.
        boolean todasEscalasRellenas = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "var scaleBtns = document.querySelectorAll('button.check-pattern.scale');" +
                        "if (scaleBtns.length === 0) return false;" +
                        "for (var i = 0; i < scaleBtns.length; i++) {" +
                        "    var icon = scaleBtns[i].querySelector('.severity-icon');" +
                        "    if (!icon || !icon.style.backgroundColor || icon.style.backgroundColor === '') {" +
                        "        return false;" +
                        "    }" +
                        "}" +
                        "return true;");

        Assert.assertTrue(todasEscalasRellenas,
                "La valoración guardada tiene escalas sin rellenar.");

        Reporter.log("TC10 OK — Valoración verificada: Percepción=Sí, todas las escalas rellenas.");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Analiza el diálogo de escala abierto y rellena TODOS sus campos según su tipo.
     *
     * Tipos detectados dentro de mat-dialog-container:
     *   · mat-select[id]              → abre el panel y elige la opción "{id}-0"
     *   · input[id]                   → fecha / hora / texto según label o input.type
     *   · mat-button-toggle-group[id] → elige el toggle con id="true" (Sí)
     *
     * IDs excluidos (pertenecen al formulario de fondo, no al diálogo de escala):
     *   date, profile, reason, user, mat-input-0
     */
    @SuppressWarnings("unchecked")
    private void fillOpenScaleForm(WebDriverWait localWait) {

        // Esperar a que los campos del formulario estén en el DOM.
        // El botón Aceptar aparece antes que los campos — sin este wait
        // el executeScript devuelve lista vacía y no se rellena nada.
        localWait.until(d ->
                !d.findElements(By.cssSelector("mat-dialog-container mat-select[id]")).isEmpty());

        List<Map<String, Object>> fields = (List<Map<String, Object>>)
                ((JavascriptExecutor) driver).executeScript(
                        "var result  = [];" +
                                "var dialog  = document.querySelector('mat-dialog-container');" +
                                "if (!dialog) return result;" +
                                "var BG_IDS  = new Set(['date','profile','reason','user','mat-input-0']);" +

                                // mat-select con ID → tipo 'select'
                                "dialog.querySelectorAll('mat-select[id]').forEach(function(el) {" +
                                "    result.push({ id: el.id, type: 'select' });" +
                                "});" +

                                // inputs con ID → inferir tipo por label o input.type
                                "dialog.querySelectorAll('input[id]').forEach(function(el) {" +
                                "    if (BG_IDS.has(el.id)) return;" +
                                "    var ff    = el.closest('mat-form-field') || el.parentElement;" +
                                "    var lbl   = ff ? ff.querySelector('mat-label, label') : null;" +
                                "    var label = lbl ? lbl.textContent.trim().toLowerCase() : '';" +
                                "    var iType = el.type ? el.type.toLowerCase() : 'text';" +
                                "    var fType = 'text';" +
                                "    if (iType === 'date'  || label.indexOf('fecha') >= 0) fType = 'date';" +
                                "    if (iType === 'time'  || label.indexOf('hora')  >= 0) fType = 'time';" +
                                "    result.push({ id: el.id, type: fType });" +
                                "});" +

                                // mat-button-toggle-group con ID → tipo 'boolean'
                                "dialog.querySelectorAll('mat-button-toggle-group[id]').forEach(function(el) {" +
                                "    result.push({ id: el.id, type: 'boolean' });" +
                                "});" +

                                "return result;");

        for (Map<String, Object> field : fields) {
            String fieldId   = (String) field.get("id");
            String fieldType = (String) field.get("type");

            switch (fieldType) {

                case "select":
                    // WebElement.click() nativo — jsClick() (JS .click()) no funciona
                    // en Angular Material: no pasa por el sistema de eventos del navegador
                    localWait.until(ExpectedConditions.elementToBeClickable(
                            By.id(fieldId))).click();
                    localWait.until(ExpectedConditions.elementToBeClickable(
                            By.id(fieldId + "-0"))).click();
                    // Esperar a que el panel se cierre
                    localWait.until(ExpectedConditions.invisibilityOfElementLocated(
                            By.cssSelector(".mat-select-panel")));
                    break;

                case "date":
                    fillInputNative(fieldId, randomDate());
                    break;

                case "time":
                    fillInputNative(fieldId, randomTime());
                    break;

                case "text":
                    fillInputNative(fieldId, randomText());
                    break;

                case "boolean":
                    // Click nativo en el toggle "true" (Sí)
                    driver.findElement(By.xpath(
                                    "//*[@id='" + fieldId + "']//mat-button-toggle[@id='true']//button"))
                            .click();
                    break;

                default:
                    Reporter.log("Tipo de campo desconocido: " + fieldType + " (id=" + fieldId + ")");
            }
        }
    }

    // ── Helpers de interacción ────────────────────────────────────────────────

    /** Click vía JS por ID. Evita errores "intercepted by overlay". */
    private void jsClick(String id) {
        ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('" + id + "').click();");
    }

    /**
     * Intenta clicar un elemento por ID esperando hasta timeoutSecs segundos.
     * No lanza error si el elemento no aparece (modales opcionales).
     *
     * @return true si se pudo clicar, false si no apareció.
     */
    private boolean clickIfPresent(String id, int timeoutSecs) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSecs))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id(id)));
            jsClick(id);
            return true;
        } catch (TimeoutException ignored) {
            return false;
        }
    }

    /** Pausa en milisegundos sin lanzar excepción checked. */
    private void sleep(int millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignored) {}
    }

    /**
     * Rellena un campo input/textarea usando nativeSetter + eventos Angular.
     * Necesario para campos de Angular Material donde sendKeys no sincroniza
     * el valor con el FormControl reactivo.
     */
    private void fillInputNative(String fieldId, String value) {
        String escaped = value.replace("\\", "\\\\").replace("'", "\\'");
        ((JavascriptExecutor) driver).executeScript(
                "var el     = document.getElementById('" + fieldId + "');" +
                        "if (!el) return;" +
                        "var proto  = el instanceof HTMLTextAreaElement" +
                        "             ? window.HTMLTextAreaElement.prototype" +
                        "             : window.HTMLInputElement.prototype;" +
                        "var setter = Object.getOwnPropertyDescriptor(proto, 'value').set;" +
                        "setter.call(el, '" + escaped + "');" +
                        "el.dispatchEvent(new Event('input',  { bubbles: true }));" +
                        "el.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "el.blur();");
    }

    // ── Generadores de valores aleatorios ─────────────────────────────────────

    /** Fecha aleatoria entre 2022-01-01 y hoy, en formato dd/MM/yyyy. */
    private String randomDate() {
        LocalDate start = LocalDate.of(2022, 1, 1);
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now());
        LocalDate random = start.plusDays((long) (RNG.nextDouble() * days));
        return random.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Hora aleatoria en formato HH:mm. */
    private String randomTime() {
        return String.format("%02d:%02d", RNG.nextInt(24), RNG.nextInt(60));
    }

    /** Texto aleatorio compuesto por 4 palabras del dominio. */
    private String randomText() {
        String[] words = {"prueba", "automatico", "test", "valoracion", "datos",
                "ejemplo", "relleno", "selenium", "enfermeria", "escala"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[RNG.nextInt(words.length)]);
        }
        return sb.toString();
    }
}
