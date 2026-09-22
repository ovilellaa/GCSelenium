package tests;

import DB.SqlScriptRunner;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.Console;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ClassBaseTest {
    protected static WebDriver driver;
    protected static WebDriverWait wait;

    private static boolean suiteHasFailed = false;

    private String MNP_Tab;

    @BeforeSuite
    public void setUpSuite() {
        setUpEnvironment();

        // Deja la BBDD en el estado que necesitan los tests antes de arrancar,
        // por si algo externo a la suite ha modificado el paciente/dato de pruebas.
        SqlScriptRunner.runBeforeSuiteScripts();
        resetTestPatientEpisode();

        // configurar driver chrome
        String driverPath = Paths.get("drivers", "chromedriver.exe").toAbsolutePath().toString();
    //    System.setProperty("webdriver.chrome.driver", driverPath);

        //crear instancia
        driver = new ChromeDriver();

        //Espera maxima para realizar cada paso
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    }

    @AfterMethod(alwaysRun = true)
    public void trackTestResult(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            suiteHasFailed = true;
        }
    }

    @AfterSuite
    public void tearDownSuite() {
        // Se ejecuta siempre, incluso si algún test ha fallado, para dejar la
        // BBDD en su estado inicial de cara a la siguiente ejecución.
        resetTestPatientEpisode();
        SqlScriptRunner.runAfterSuiteScripts();

        if (driver != null && !suiteHasFailed) {
            driver.quit();
        }
    }

    /**
     * Pregunta por consola el episodio (processid) del paciente de pruebas a
     * borrar y, si se indica uno, borra todos sus datos. Se llama antes y
     * después de la suite (ver setUpSuite()/tearDownSuite()): antes, por si
     * quedó un episodio de una ejecución anterior; después, por si el propio
     * test acaba de crear uno (p.ej. al crear una hoja de urgencias o un
     * ingreso) y se quiere dejar la BBDD limpia para la siguiente vez.
     *
     * Dejar la respuesta en blanco (Intro) omite el borrado — importante
     * porque hay tests que dependen de que ese episodio siga existiendo, o
     * porque se está lanzando una prueba puntual sin querer tocar datos que
     * usarán otras pruebas después.
     *
     * Si no hay consola interactiva (p.ej. el jar se lanza en un pipeline de
     * CI sin terminal, como el de Azure DevOps configurado como remoto), no
     * se pregunta nada y se omite el borrado directamente, para no bloquear
     * la ejecución esperando una respuesta que nunca llegará.
     */
    private void resetTestPatientEpisode() {
        Console console = System.console();
        if (console == null) {
            return;
        }

        String nh = ConfigReader.get("pacqah1NH");
        List<Map<String, Object>> filas = SqlScriptRunner.query("get_customerid_by_nh.sql", Map.of("NHC", nh));
        if (filas.isEmpty()) {
            System.out.println("No se encontró el paciente NH=" + nh + "; se omite el borrado de episodio.");
            return;
        }
        Map<String, Object> paciente = filas.get(0);
        String customerId = textValue(paciente.get("CUSTOMERID"));
        String nombreCompleto = (textValue(paciente.get("NAMECUSTOMER")) + " "
                + textValue(paciente.get("FIRSTSURNAMECUSTOMER")) + " "
                + textValue(paciente.get("SECONDSURNAMECUSTOMER"))).trim();

        String episodio = console.readLine(
                "Episodio (processid) del paciente de pruebas " + nombreCompleto
                        + " con NH " + nh + " a borrar, o Intro para omitir: ");
        if (episodio == null || episodio.isBlank()) {
            return;
        }
        episodio = episodio.trim();

        SqlScriptRunner.run("borrar_datos_paciente_prueba.sql", Map.of(
                "customerid", customerId,
                "processid", episodio
        ));
        System.out.println("Episodio " + episodio + " del paciente " + nombreCompleto + " (NH " + nh + ") borrado.");
    }

    /** Convierte a texto un valor de columna, tratando null como cadena vacía y recortando espacios (columnas CHAR de ancho fijo). */
    private static String textValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public void GotoToUrl() {
        //Navegar a GC
        driver.get(ConfigReader.get("gcweb.url"));

        MNP_Tab = driver.getWindowHandle();
    }

    // metodos auxiliares
    public void setUpEnvironment() {
        String env = System.getProperty("env", "qaazure");
        System.out.println("Entorno seleccionado: " + env);

        ConfigReader.load(env);
    }

    public void LoginAsDoctor() {
        String username = ConfigReader.get("username_doctor");
        String password = ConfigReader.get("password_doctor");
        Login(username, password);
    }


    public void LoginAsNurse() {
        String username = ConfigReader.get("username_nurse");
        String password = ConfigReader.get("password_nurse");
        Login(username, password);
    }

    public void LoginAsAdministrative() {
        String username = ConfigReader.get("username_admin");
        String password = ConfigReader.get("password_admin");
        Login(username, password);
    }


    public void Logout() {
        WebElement icon = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("mat-icon.user-profile-icon")));

        icon.click();

        WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("logout-button")));

        logoutButton.click();

        WaitAMomentPlease();
    }

    private void Login(String username, String password) {

        GotoToUrl();

        //Escribe el username
        WebElement loginUser = driver.findElement(By.id("username"));
        loginUser.sendKeys(username);

        //Escribe la password
        WebElement loginPass = driver.findElement(By.id("password"));
        loginPass.sendKeys(password);

        //Inicia sesión
        WebElement loginButton = driver.findElement(By.id("login-button"));
        loginButton.click();

        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("defaultButtonId")));
            btn.click();
        } catch (TimeoutException e) {
            // El botón de selección de centro no aparece en todos los entornos
        }
    }

    public void WaitAMomentPlease(float seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void WaitAMomentPlease() {
        WaitAMomentPlease(1f);
    }


    /// TABS
    public void GotoMNPTab() {
        Set<String> allTabs = driver.getWindowHandles();

        for (String tab : allTabs) {
            if (tab.equals(MNP_Tab)) {
                driver.switchTo().window(tab);
            }
        }


    }

    public int TabOpenedCount() {
        return driver.getWindowHandles().size();
    }

    public String GetLastTabOpened() {
        List<String> tabs = new ArrayList<>(driver.getWindowHandles());

        return tabs.getLast();
    }

    public void SwitchToTab(String tab) {
        driver.switchTo().window(tab);
    }

    public void CloseMPTab() {
        wait.until(d -> d.getWindowHandles().size() > 1);

        List<String> tabs = new ArrayList<>(driver.getWindowHandles());

        String lastTab = tabs.getLast();

        driver.switchTo().window(lastTab);
        driver.close();

    }

    public boolean IsMNPTabActive() {
        String currentHandle = driver.getWindowHandle();

        return currentHandle.equals(MNP_Tab);
    }

    /// FIN TABS


    public void CloseActionsMenuWL() {
        WebElement backdrop = driver.findElement(By.className("cdk-overlay-backdrop"));
        backdrop.click();
    }

    public boolean IsDischargeReportSigned() {
        WebElement imgElement = driver.findElement(By.cssSelector("img.image.ng-star-inserted"));
        String srcValue = imgElement.getAttribute("src");

        return srcValue.contains("shield-check-valid.png");
    }

    public void Sign(String password) {
        WebElement imgElement = driver.findElement(By.cssSelector("img.image.ng-star-inserted"));
        imgElement.click();
        WebElement passFirma = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));
        passFirma.sendKeys(password);

        WebElement firmar = driver.findElement(By.id("sign-UserSignComponent-button"));
        firmar.click();

    }

    public boolean IsFormEnabled(By formularioLocator) {
        WebElement formulario = driver.findElement(formularioLocator);

        // Selecciona todos los posibles campos
        List<WebElement> campos = driver.findElements(formularioLocator);

        for (WebElement campo : campos) {

            // 1. Caso estándar: atributo disabled
            if (campo.isEnabled()) {
                return true;
            }

            // 2. Caso readonly
            String readonly = campo.getAttribute("readonly");
            if (readonly == null) {
                return true;
            }

            // 3. Caso Angular Material: aria-disabled
            String ariaDisabled = campo.getAttribute("aria-disabled");
            if (ariaDisabled != null && ariaDisabled.equals("false")) {
                return true;
            }

            // 4. Caso Angular Material: clase CSS
            String clases = campo.getAttribute("class");
            if (clases != null && !clases.contains("mat-select-disabled") && !clases.contains("mat-input-disabled")) {
                return true;
            }
        }

        return false;
    }

    public void clearAndType(WebElement field, String text, boolean editorEnriquecido) {
        try {
            if (editorEnriquecido) {

                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].innerHTML = '';", field);
                field.sendKeys(text);
            } else {
                try {
                    field.clear();
                } catch (Exception e) {
                    // fallback si clear falla
                    field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
                }
                field.sendKeys(text);
            }
        } catch (Exception e) {
        }
    }


    public void OpenNoContextualMNPActions() {
        // abre las acciones del boton + del MNP
        WebElement botonCrear = driver.findElement(By.id("add-action"));
        botonCrear.click();
    }

    /**
     * Vuelve al menú principal pulsando repetidamente cualquier botón "volver"
     * (id termina en "-back-button") hasta que no quede ninguno. Necesario para
     * navegar entre módulos distintos (Urgencias, Hospitalización, Admisión)
     * dentro del mismo test.
     */
    public void goToMainMenu() {
        while (true) {
            List<WebElement> backButtons = driver.findElements(By.cssSelector("[id$='-back-button']"));
            if (backButtons.isEmpty()) {
                break;
            }
            backButtons.get(0).click();
            WaitAMomentPlease();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS COMUNES DE GESTIÓN DE MODALES
    // Presentes en todos los tests que abren el historial clínico de un paciente.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Selecciona el motivo en el modal de acceso a historia ajena y pulsa Continuar.
     * Aparece cuando el paciente no está asignado al usuario en sesión.
     * IDs: "reason" (mat-select), "continue-MessageComponent-button".
     *
     * @param reason texto de la opción a seleccionar, p.ej. "Guardia"
     */
    protected void SelectAccessReason(String reason) {
        try {
            WebElement motivoSelect = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("reason")));
            motivoSelect.click();

            WebElement option = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//mat-option[contains(.,'" + reason + "')]")));
            option.click();

            WebElement continuar = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.id("continue-MessageComponent-button")));
            continuar.click();

            Reporter.log("Motivo de acceso seleccionado: " + reason);
        } catch (TimeoutException | NoSuchElementException e) {
            Reporter.log("No apareció el modal de motivo de acceso (paciente asignado).");
        }
    }

    /**
     * Gestiona el modal gc-alerts "Alertas vitales detectadas" si aparece.
     * Botón: id="accept-AlertsContainer-button".
     * Llamar ANTES de handleReadOnlyAlert().
     */
    protected void handleVitalAlertsDialog() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(4));
            shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.id("accept-AlertsContainer-button")));
            driver.findElement(By.id("accept-AlertsContainer-button")).click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("gc-alerts")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".cdk-overlay-backdrop")));
            Reporter.log("Modal 'Alertas vitales detectadas' gestionado.");
        } catch (TimeoutException e) {
            Reporter.log("No apareció modal de alertas vitales.");
        }
    }

    /**
     * Gestiona el componente gc-alert (modo solo lectura / sesión duplicada).
     * Botón: id="continue-button".
     * Llamar DESPUÉS de handleVitalAlertsDialog().
     */
    protected void handleReadOnlyAlert() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(4));
            WebElement continuarBtn = shortWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("continue-button")));
            continuarBtn.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("gc-alert")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".cdk-overlay-backdrop")));
            Reporter.log("Aviso gc-alert gestionado.");
        } catch (TimeoutException e) {
            Reporter.log("No apareció gc-alert.");
        }
    }

    /**
     * Gestiona el aviso de validación (gc-alert con id="continue-button") que
     * puede aparecer al guardar si algún campo obligatorio está incorrecto.
     */
    protected void handleValidationAlert() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement continuarBtn = shortWait.until(
                    ExpectedConditions.elementToBeClickable(By.id("continue-button")));
            continuarBtn.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".cdk-overlay-backdrop")));
            Reporter.log("Aviso de validación gestionado.");
        } catch (TimeoutException e) {
            Reporter.log("No apareció aviso de validación.");
        }
    }

    /**
     * Gestiona la pantalla de bloqueo por inactividad si aparece.
     * IDs: "password" (input contraseña), "unlock-session-button" (botón Desbloquear).
     * Llamar antes de cualquier acción cuando el test puede haber tardado mucho.
     *
     * @param password  Contraseña del usuario activo
     */
    protected void handleSessionLock(String password) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement passField = shortWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("unlock-session-button")));
            driver.findElement(By.id("password")).sendKeys(password);
            driver.findElement(By.id("unlock-session-button")).click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("unlock-session-button")));
            Reporter.log("Sesión desbloqueada.");
        } catch (TimeoutException e) {
            // No hay pantalla de bloqueo — continuar
        }
    }

    /**
     * Abre el menú de Acciones (id="actions-button") y hace click en el ítem
     * indicado via JavascriptExecutor.
     *
     * El click directo falla con "element click intercepted" porque el
     * cdk-overlay-pane de Angular Material se posiciona sobre el ítem mientras
     * el menú se anima. JS bypasea el overlay.
     *
     * @param localWait  WebDriverWait a usar
     * @param menuItemId ID del ítem de menú (ej. "new_registry", "add", "edit")
     */
    protected void openActionsMenuAndClick(WebDriverWait localWait, String menuItemId) {
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id("actions-button"))).click();

        WebElement menuItem = localWait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id(menuItemId)));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menuItem);
    }

    /**
     * Abre un mat-select por su ID y selecciona la opción indicada via JS.
     *
     * Las opciones de Angular Material mat-select pueden tener el problema del
     * cdk-overlay, por lo que se usa JS click en la opción.
     *
     * @param localWait WebDriverWait a usar
     * @param selectId  ID del mat-select a abrir
     * @param optionId  ID de la opción (patrón habitual: {selectId}-{índice})
     */
    protected void selectOption(WebDriverWait localWait, String selectId, String optionId) {
        localWait.until(ExpectedConditions.elementToBeClickable(
                By.id(selectId))).click();

        // Se usa presenceOfElementLocated en vez de visibilityOfElementLocated porque
        // cuando el select está dentro de un modal (mat-dialog-container), el backdrop
        // del modal puede hacer que Selenium considere la opción como "no visible"
        // aunque esté renderizada y clickable. JS click bypasea esa restricción.
        WebElement option = localWait.until(
                ExpectedConditions.presenceOfElementLocated(By.id(optionId)));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
    }
}
