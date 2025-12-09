package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class ClassBaseTest {
    protected static WebDriver driver;
    protected static WebDriverWait wait;

    private String MNP_Tab;

    @BeforeSuite
    public void setUpSuite() {
        setUpEnvironment();

        // configurar driver chrome
        String driverPath = Paths.get("drivers", "chromedriver.exe").toAbsolutePath().toString();
        System.setProperty("webdriver.chrome.driver", driverPath);

        //crear instancia
        driver = new ChromeDriver();

        //Espera maxima para realizar cada paso
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));

        wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // espera como máximo 20 segundos

    }

    @AfterSuite
    public void tearDownSuite() {
        //        if (driver != null) {
        //            driver.quit();
        //        }
    }

    public void GotoToUrl() {
        //Navegar a GC
        driver.get(ConfigReader.get("gcweb.url"));

        MNP_Tab = driver.getWindowHandle();
    }

    // metodos auxiliares
    public void setUpEnvironment() {
        String env = System.getProperty("env", "qa");
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
        WebElement loginButton = driver.findElement(By.xpath("//*[@id=\"login-button\"]/span[1]/div/span[1]"));
        loginButton.click();

        // Espera unos segundos para que se renderice el siguiente paso
        try {
            // Espera a que aparezca el botón de selección de centro
            WebElement selectCenter = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[@id=\"defaultButtonId\"]/span[1]/div/span[1]")));

            // WebElement selectCenter = driver.findElement(By.xpath("//*[@id=\"defaultButtonId\"]/span[1]/div/span[1]"));

            // Si está presente y visible, haz clic
            if (selectCenter.isDisplayed()) {
                selectCenter.click();
            } else {
                System.out.println("No se requiere selección de centro.");
            }

        } catch (TimeoutException e) {

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

    public int TabOpenedCount()
    {
        return driver.getWindowHandles().size();
    }

    public String GetLastTabOpened()
    {
        List<String> tabs = new ArrayList<>(driver.getWindowHandles());

        return tabs.getLast();
    }

    public void SwitchToTab(String tab)
    {
        driver.switchTo().window(tab);
    }

    public void CloseMPTab() {
        wait.until(d -> d.getWindowHandles().size() > 1);

        List<String> tabs = new ArrayList<>(driver.getWindowHandles());

        String lastTab = tabs.getLast();

        driver.switchTo().window(lastTab);
        driver.close();

    }

    public boolean IsMNPTabActive()
    {
        String currentHandle = driver.getWindowHandle();

        return currentHandle.equals(MNP_Tab);
    }
    /// FIN TABS


    public void CloseActionsMenuWL() {
        WebElement backdrop = driver.findElement(By.className("cdk-overlay-backdrop"));
        backdrop.click();
    }

    public boolean IsDischargeReportSigned()
    {
        WebElement imgElement = driver.findElement(By.cssSelector("img.image.ng-star-inserted"));
        String srcValue = imgElement.getAttribute("src");

        return srcValue.contains("shield-check-valid.png");
    }

    public void Sign(String password)
    {
        WebElement imgElement = driver.findElement(By.cssSelector("img.image.ng-star-inserted"));
        imgElement.click();
        WebElement passFirma = driver.findElement(By.id("password"));
        passFirma.sendKeys( password);

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

        return false; // si ninguno cumple, todo está deshabilitado
    }

    public void clearAndType(WebElement field, String text) {
        try {
            field.clear();
        } catch (Exception e) {
            // fallback si clear falla
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        }
        field.sendKeys(text);
    }
}
