package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Test: Circuito de Radiología (solicitud, modificación, eliminación,
 * informe y firma).
 *
 * NOTA DE MIGRACIÓN (desde CarlosFreire):
 *  - Al fichero de origen le faltaba la declaración "package tests;" — no
 *    compilaba tal cual. Añadida.
 *  - Eliminado un bloque de depuración dejado en el código (volcado de
 *    todos los ítems del menú por consola en InformeYFirma, paso 7e).
 *  - El paciente ya se lee de ConfigReader (no hardcodeado) y varios
 *    diálogos ya usaban IDs reales verificados
 *    ("accept_dialog-RadiologyDialogContainer-button", "show-catalog-button",
 *    "angle-right-button", "accept-TestCatalogContainer-button",
 *    "expectedCalendar", "accept-RadiologySummaryContainer-button",
 *    "menu-option-button", "sign-UserSignComponent-button",
 *    "close-HeavyProcessLoaderDialogComponent-button") — no se han tocado.
 *  - Sustituido por ID verificado ("continue-button") el cierre de diálogos
 *    informativos genéricos. La confirmación de eliminación ya comprobaba
 *    el ID "alert-confirm" además del texto — se mantiene igual.
 * Los selectores de menú/catálogo específicos de Radiología que siguen
 * localizando por texto quedan sin tocar por no poder verificarse contra
 * la app real.
 */
public class RadiologyTest extends ClassBaseTest {

    private WebDriverWait waitLong() {
        return new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    @Test(priority = 1)
    public void Login() {
        LoginAsDoctor();
    }

    @Test(priority = 10, dependsOnMethods = "Login")
    public void ViewHistory() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("search-action"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("spotlight-search")))
                .sendKeys(ConfigReader.get("pacqah1NH"));

        wait.until(ExpectedConditions.elementToBeClickable(By.id("spotlight-list-item-0-0"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("quickAction-PATIENT_SEE_HISTORY"))).click();

        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("dialog-patient-episodes-panel-0"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-PatientEpisodesContainer-button"))).click();
        } catch (Exception ignored) {}

        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-FileHistoryAccess-button"))).click();
        } catch (Exception ignored) {}

        WaitAMomentPlease(1.5f);
        SwitchToTab(GetLastTabOpened());

        // Heredado de ClassBaseTest — mismo ID (accept-AlertsContainer-button).
        handleVitalAlertsDialog();
    }

    @Test(priority = 20, dependsOnMethods = {"Login", "ViewHistory"})
    public void NavigateToRadiology() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait waitNav = new WebDriverWait(driver, Duration.ofSeconds(20));

        waitNav.until(d -> {
            try {
                WebElement el = d.findElement(By.id("tests_map-sidebar"));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                el.click();
                return true;
            } catch (Exception e) { return false; }
        });

        waitNav.until(d -> {
            try {
                WebElement el = d.findElement(By.id("radiology-sidebar"));
                js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
                el.click();
                return true;
            } catch (Exception e) { return false; }
        });

        waitNav.until(ExpectedConditions.presenceOfElementLocated(By.tagName("gc-radiology")));
        waitNav.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        Reporter.log("PASO 3 — Radiologia OK");
    }

    @Test(priority = 30, dependsOnMethods = {"Login", "ViewHistory", "NavigateToRadiology"})
    public void CrearSolicitud() {
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);
        clickMenuItem("Nueva solicitud");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept_dialog-RadiologyDialogContainer-button")));
        WaitAMomentPlease(0.6f);

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("show-catalog-button"))));
        wait.until(d -> js().executeScript("return document.readyState").equals("complete"));

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[normalize-space()='RADIOLOGIA']"))));
        WaitAMomentPlease(0.7f);

        clickMatCheckboxLabel("ABDOMEN (VIENTRE) 1 POSICION", true);

        wait.until(d -> Boolean.FALSE.equals(js().executeScript(
                "var b=document.getElementById('angle-right-button');return b?b.disabled:true;")));
        jsClick(driver.findElement(By.id("angle-right-button")));
        WaitAMomentPlease(0.7f);

        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var allCbs = Array.from(document.querySelectorAll('mat-checkbox'));" +
                            "var midX = window.innerWidth / 2;" +
                            "for(var i = 0; i < allCbs.length; i++){" +
                            "  var r = allCbs[i].getBoundingClientRect();" +
                            "  if(r.x > midX && r.width > 0 && r.height > 0){" +
                            "    var label = allCbs[i].querySelector('label') || allCbs[i];" +
                            "    label.click(); return true;" +
                            "  }" +
                            "}" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept-TestCatalogContainer-button"))));
        wait.until(d -> js().executeScript("return document.readyState").equals("complete"));
        cerrarModalesContinuar();

        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept_dialog-RadiologyDialogContainer-button")));
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept_dialog-RadiologyDialogContainer-button"))));
        cerrarModalesContinuar();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(.,'ABDOMEN')]")));
        Reporter.log("PASO 4 — Solicitud creada OK");
    }

    @Test(priority = 40, dependsOnMethods = {"Login", "ViewHistory", "NavigateToRadiology", "CrearSolicitud"})
    public void ModificarSolicitud() {
        seleccionarFilaAbdomen();

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        esperarMenuitemHabilitado("open_request");
        clickMenuItem("Abrir solicitud");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("accept_dialog-RadiologyDialogContainer-button")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("expectedCalendar")));

        String manana = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        setFechaPrevista(manana);
        Reporter.log("   Fecha -> " + manana);

        WaitAMomentPlease(1.5f);

        jsClick(waitLong().until(ExpectedConditions.elementToBeClickable(
                By.id("accept_dialog-RadiologyDialogContainer-button"))));
        cerrarModalesContinuar();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(.,'ABDOMEN')]")));
        Reporter.log("PASO 5 — Solicitud modificada OK");
    }

    @Test(priority = 50, dependsOnMethods = {"Login", "ViewHistory", "NavigateToRadiology", "CrearSolicitud", "ModificarSolicitud"})
    public void EliminarSolicitud() {
        // Cerrar cualquier modal abierto antes de empezar
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> (Boolean) js().executeScript(
                    "var btn=Array.from(document.querySelectorAll('button'))" +
                            "  .find(function(b){return b.textContent.trim()==='Cancelar'&&b.offsetParent!==null;});" +
                            "if(btn){btn.click();return true;}return false;"));
            WaitAMomentPlease(0.8f);
        } catch (TimeoutException ignored) {}

        int filasBefore = driver.findElements(By.xpath("//td[contains(.,'ABDOMEN')]")).size();

        seleccionarFilaAbdomen();

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        clickMenuItem("Eliminar solicitud");

        waitLong().until(d -> (Boolean) js().executeScript(
                "var btn=Array.from(document.querySelectorAll('button')).find(function(b){" +
                        "  return(b.id==='alert-confirm'" +
                        "    ||b.innerText.includes('Aceptar')" +
                        "    ||b.innerText.includes('Confirmar')" +
                        "    ||b.innerText.includes('Sí'))" +
                        "    &&b.offsetParent!==null;" +
                        "});" +
                        "if(btn){btn.click();return true;}return false;"));

        cerrarModalesContinuar();

        int filasBefore_final = filasBefore;
        waitLong().until(d -> {
            int filasNow = d.findElements(By.xpath("//td[contains(.,'ABDOMEN')]")).size();
            return filasNow < filasBefore_final;
        });

        Reporter.log("PASO 6 — Solicitud eliminada OK");
    }

    @Test(priority = 60, dependsOnMethods = {"Login", "ViewHistory", "NavigateToRadiology", "EliminarSolicitud"})
    public void InformeYFirma() {
        // ── 7a. Nueva solicitud ───────────────────────────────────────
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        clickMenuItem("Nueva solicitud");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accept_dialog-RadiologyDialogContainer-button")));
        WaitAMomentPlease(0.6f);

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("show-catalog-button"))));
        wait.until(d -> js().executeScript("return document.readyState").equals("complete"));
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[normalize-space()='RADIOLOGIA']"))));
        WaitAMomentPlease(0.7f);
        clickMatCheckboxLabel("ABDOMEN (VIENTRE) 1 POSICION", true);
        wait.until(d -> Boolean.FALSE.equals(js().executeScript(
                "var b=document.getElementById('angle-right-button');return b?b.disabled:true;")));
        jsClick(driver.findElement(By.id("angle-right-button")));
        WaitAMomentPlease(0.7f);

        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var allCbs = Array.from(document.querySelectorAll('mat-checkbox'));" +
                            "var midX = window.innerWidth / 2;" +
                            "for(var i = 0; i < allCbs.length; i++){" +
                            "  var r = allCbs[i].getBoundingClientRect();" +
                            "  if(r.x > midX && r.width > 0 && r.height > 0){" +
                            "    var label = allCbs[i].querySelector('label') || allCbs[i];" +
                            "    label.click(); return true;" +
                            "  }" +
                            "}" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);

        jsClick(waitLong().until(ExpectedConditions.elementToBeClickable(
                By.id("accept-TestCatalogContainer-button"))));
        wait.until(d -> js().executeScript("return document.readyState").equals("complete"));
        cerrarModalesContinuar();
        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("accept_dialog-RadiologyDialogContainer-button")));
        jsClick(waitLong().until(ExpectedConditions.elementToBeClickable(
                By.id("accept_dialog-RadiologyDialogContainer-button"))));
        cerrarModalesContinuar();
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(.,'ABDOMEN')]")));
        Reporter.log("   7a — Solicitud creada");

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> (Boolean) js().executeScript(
                    "var b=document.getElementById('close-HeavyProcessLoaderDialogComponent-button');" +
                            "if(b&&!b.disabled){b.click();return true;}return false;"));
        } catch (TimeoutException ignored) {}

        // ── 7b. Abrir informe ─────────────────────────────────────────
        seleccionarFilaAbdomen();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        esperarMenuitemHabilitado("open_report");
        clickMenuItem("Abrir informe");
        cerrarModalesContinuar();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("menu-option-button")));
        Reporter.log("   7b — Informe abierto");

        // ── 7c. Rellenar editor ───────────────────────────────────────
        js().executeScript(
                "var editor=document.querySelector('.angular-editor-textarea');" +
                        "if(editor){" +
                        "  editor.setAttribute('contenteditable','true');editor.focus();" +
                        "  editor.innerHTML='Informe de radiologia automatizado. Sin hallazgos patologicos.';" +
                        "  editor.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "  editor.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "  editor.blur();" +
                        "}");
        WaitAMomentPlease(0.8f);
        Reporter.log("   7c — Informe rellenado");

        // ── 7d. Guardar informe ───────────────────────────────────────
        wait.until(d -> (Boolean) js().executeScript(
                "var btn=document.getElementById('accept-RadiologySummaryContainer-button');" +
                        "if(btn&&btn.offsetParent!==null){btn.scrollIntoView(false);btn.click();return true;}" +
                        "return false;"));
        cerrarModalesContinuar();
        wait.until(d -> (Boolean) js().executeScript(
                "return!Array.from(document.querySelectorAll('.cdk-overlay-container button'))" +
                        "  .some(function(b){return b.textContent.includes('Continuar');});"));
        Reporter.log("   7d — Informe guardado");

        // ── 7e. Reabrir para firmar ───────────────────────────────────
        seleccionarFilaAbdomen();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);

        esperarMenuitemHabilitado("open_report");
        clickMenuItem("Abrir informe");
        cerrarModalesContinuar();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("menu-option-button")));
        cerrarModalesContinuar();
        Reporter.log("   7e — Informe reabierto para firma");

        // ── 7f. Firma médico ──────────────────────────────────────────
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("menu-option-button"))));
        WaitAMomentPlease(0.6f);
        clickMenuItem("Firma médico");
        WaitAMomentPlease(1.0f);

        // ── 7g. Firmar ────────────────────────────────────────────────
        WebElement passField = wait.until(ExpectedConditions.elementToBeClickable(By.id("password")));
        passField.sendKeys(ConfigReader.get("password_doctor"));
        WaitAMomentPlease(0.3f);

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("sign-UserSignComponent-button"))));

        cerrarModalesContinuar();
        Reporter.log("PASO 7 — Informe rellenado y firmado OK");
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private void jsClick(WebElement el) {
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        WaitAMomentPlease(0.3f);
        js().executeScript("arguments[0].click();", el);
    }

    /**
     * Cierra en cadena los diálogos informativos "Continuar". Intenta el ID
     * "continue-button" (usado en toda la app, ver ClassBaseTest) en cada
     * iteración y cae al texto original (buscando en el overlay CDK o en
     * document.body) si no aparece.
     */
    private void cerrarModalesContinuar() {
        for (int i = 0; i < 5; i++) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(4))
                        .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                        .click();
                WaitAMomentPlease(0.8f);
                continue;
            } catch (TimeoutException ignored) {}

            try {
                new WebDriverWait(driver, Duration.ofSeconds(4)).until(d -> {
                    Boolean ok = (Boolean) js().executeScript(
                            "var roots=[document.querySelector('.cdk-overlay-container'),document.body];" +
                                    "for(var i=0;i<roots.length;i++){" +
                                    "  var root=roots[i];if(!root)continue;" +
                                    "  var btns=root.querySelectorAll('button');" +
                                    "  for(var j=0;j<btns.length;j++){" +
                                    "    if(btns[j].textContent.trim()==='Continuar'&&!btns[j].disabled){" +
                                    "      btns[j].click();return true;" +
                                    "    }" +
                                    "  }" +
                                    "}" +
                                    "return false;");
                    return Boolean.TRUE.equals(ok);
                });
                WaitAMomentPlease(0.8f);
            } catch (TimeoutException e) { break; }
        }
    }

    // TODO: ítems de menú específicos de Radiología localizados por texto —
    // no se conoce su ID real. Sustituir cuando se valide contra QA.
    private void clickMenuItem(String texto) {
        cerrarModalesContinuar();
        String needle = texto.toLowerCase();
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle='" + needle.replace("'", "\\'") + "';" +
                            "var items=Array.from(document.querySelectorAll(" +
                            "  'button[role=\"menuitem\"],.mat-menu-item,button'));" +
                            "var target=items.find(function(b){" +
                            "  return b.textContent.trim().toLowerCase().includes(needle)" +
                            "    &&b.offsetParent!==null&&!b.disabled;" +
                            "});" +
                            "if(target){target.click();return true;}return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
    }

    private void esperarMenuitemHabilitado(String menuitemId) {
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var b=document.getElementById('" + menuitemId + "');" +
                            "if(!b)return false;" +
                            "return!(b.disabled||b.classList.contains('mat-menu-item-disabled')" +
                            "  ||b.getAttribute('aria-disabled')==='true');");
            return Boolean.TRUE.equals(ok);
        });
    }

    private void clickMatCheckboxLabel(String textoContiene, boolean excluir2Posicion) {
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var cbs=document.querySelectorAll('mat-checkbox');" +
                            "for(var i=0;i<cbs.length;i++){" +
                            "  var txt=cbs[i].textContent.replace(/\\s+/g,' ').trim();" +
                            "  if(txt.includes(arguments[0])&&(!arguments[1]||!txt.includes('2 POSICION'))){" +
                            "    cbs[i].scrollIntoView({block:'center'});" +
                            "    var label=cbs[i].querySelector('label')||cbs[i];" +
                            "    label.click();return true;" +
                            "  }" +
                            "}" +
                            "return false;", textoContiene, excluir2Posicion);
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
    }

    private void seleccionarFilaAbdomen() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//td[contains(.,'ABDOMEN')]")));
        wait.until(d -> (Boolean) js().executeScript(
                "var td=Array.from(document.querySelectorAll('td'))" +
                        "  .find(function(el){return el.textContent.includes('ABDOMEN');});" +
                        "if(!td)return false;" +
                        "var tr=td.closest('tr');" +
                        "if(!tr.className.includes('row--selected')){tr.click();return false;}" +
                        "return true;"));
        WaitAMomentPlease(0.5f);
    }

    private void setFechaPrevista(String fechaDD_MM_YYYY) {
        // Convertir dd/MM/yyyy → yyyy-MM-dd (formato nativo del input[type=date])
        String[] p = fechaDD_MM_YYYY.split("/");
        String fechaISO = p[2] + "-" + p[1] + "-" + p[0];

        WebElement campo = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("expectedCalendar")));

        js().executeScript("arguments[0].scrollIntoView({block:'center'});", campo);
        WaitAMomentPlease(0.4f);

        js().executeScript("arguments[0].click();", campo);
        WaitAMomentPlease(0.3f);

        campo.sendKeys(Keys.CONTROL + "a");
        campo.sendKeys(Keys.DELETE);
        WaitAMomentPlease(0.2f);
        campo.sendKeys(fechaISO);
        WaitAMomentPlease(0.4f);
        campo.sendKeys(Keys.TAB);
        WaitAMomentPlease(0.5f);
    }
}
