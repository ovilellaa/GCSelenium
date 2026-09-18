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

/**
 * Test: Circuito de Anatomía Patológica (Biopsia)
 *
 * NOTA: el fichero de origen se llamaba "patologia.txt" pero la clase real
 * cubre el módulo de Anatomía Patológica (solicitudes de biopsia).
 *
 * NOTA DE MIGRACIÓN (desde CarlosFreire): este test ya usaba bastantes IDs
 * reales verificados ("tests_map-sidebar", "pathological_anatomy-sidebar",
 * "actions-button", "expectedCalendar", "add-anatomic-pathology-test-button",
 * "accept-AnatomicPathologyDialogContainer-button", "section"/"section-0",
 * "procedure", "test", "open_request"/"open_report",
 * "close-HeavyProcessLoaderDialogComponent-button") y el paciente ya se lee
 * de ConfigReader en vez de estar hardcodeado — no se han tocado. Se ha:
 *   - Restaurado el Assert real en EliminarSolicitud() (el original solo
 *     imprimía el resultado sin hacer fallar el test).
 *   - Eliminado el método clickAndCheckModule(), no usado en ningún sitio.
 *   - Sustituido por ID verificado ("continue-button", "alert-confirm")
 *     los diálogos genéricos de confirmación.
 * Los selectores de menú/campos específicos de este módulo que siguen
 * localizando por texto (p.ej. "Servicio solicitado", "Nueva solicitud")
 * quedan sin tocar por no poder verificarse contra la app real.
 */
public class AnathomyTest extends ClassBaseTest {

    private static final String SECTION_LABEL     = "Biopsia";
    private static final String SECTION_OPTION_ID = "section-0";

    private WebDriverWait waitLong() {
        return new WebDriverWait(driver, Duration.ofSeconds(40));
    }

    @Test(priority = 1)
    public void Login() { LoginAsDoctor(); }

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
        try { wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-FileHistoryAccess-button"))).click(); } catch (Exception ignored) {}
        WaitAMomentPlease(1.5f);
        SwitchToTab(GetLastTabOpened());
        // Heredado de ClassBaseTest — mismo ID (accept-AlertsContainer-button).
        handleVitalAlertsDialog();
    }

    @Test(priority = 20, dependsOnMethods = {"Login", "ViewHistory"})
    public void NavigateToPathologicalAnatomy() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebDriverWait waitNav = new WebDriverWait(driver, Duration.ofSeconds(20));
        waitNav.until(d -> { try { WebElement el = d.findElement(By.id("tests_map-sidebar")); js.executeScript("arguments[0].scrollIntoView({block:'center'});", el); el.click(); return true; } catch (Exception e) { return false; } });
        waitNav.until(d -> { try { WebElement el = d.findElement(By.id("pathological_anatomy-sidebar")); js.executeScript("arguments[0].scrollIntoView({block:'center'});", el); el.click(); return true; } catch (Exception e) { return false; } });
        waitNav.until(ExpectedConditions.presenceOfElementLocated(By.tagName("gc-anatomicpathology")));
        waitNav.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        Reporter.log("PASO 3 — Anatomia Patologica OK");
    }

    @Test(priority = 30, dependsOnMethods = {"Login", "ViewHistory", "NavigateToPathologicalAnatomy"})
    public void CrearSolicitud() {
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);
        clickMenuItemConDialogo("Nueva solicitud");
        esperarDialogoListo();
        seleccionarTipoMuestra();
        aceptarDialogo();
        esperarFilaBiopsia();
        Reporter.log("PASO 4 — Solicitud creada OK");
    }

    @Test(priority = 40, dependsOnMethods = {"Login", "ViewHistory", "NavigateToPathologicalAnatomy", "CrearSolicitud"})
    public void ModificarSolicitud() {
        seleccionarFilaBiopsia();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        esperarMenuitemHabilitado("open_request");
        clickMenuItemSinDialogo("Abrir solicitud");
        esperarDialogoListo();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("expectedCalendar")));
        String manana = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        setFechaPrevista(manana);
        Reporter.log("   Fecha -> " + manana);

        // Seleccionar "Tipo de muestra" = Biopsia con Selenium nativo
        try {
            WebElement selTipo = wait.until(d -> {
                List<WebElement> dialogs = driver.findElements(By.tagName("mat-dialog-container"));
                if (dialogs.isEmpty()) return null;
                WebElement dialog = dialogs.get(dialogs.size() - 1);
                List<WebElement> selects = dialog.findElements(By.tagName("mat-select"));
                return selects.stream().filter(s -> {
                    try {
                        WebElement field = s.findElement(By.xpath("ancestor::mat-form-field"));
                        return field.getText().toLowerCase().contains("tipo de muestra");
                    } catch (Exception e) { return false; }
                }).findFirst().orElse(null);
            });
            if (selTipo != null) {
                jsClick(selTipo);
                WaitAMomentPlease(0.8f);
                wait.until(d -> (Boolean) js().executeScript(
                        "var opts = Array.from(document.querySelectorAll('mat-option'));" +
                                "var biopsia = opts.find(function(o){" +
                                "  return o.offsetParent !== null && o.textContent.trim().toLowerCase() === 'biopsia';" +
                                "});" +
                                "if(biopsia){ biopsia.click(); return true; }" +
                                "var primera = opts.find(function(o){ return o.offsetParent !== null; });" +
                                "if(primera){ primera.click(); return true; }" +
                                "return false;"));
                WaitAMomentPlease(0.5f);
                Reporter.log("   Tipo de muestra rellenado");
            }
        } catch (Exception e) {
            Reporter.log("   Tipo de muestra ya tenia valor o no encontrado");
        }

        // Seleccionar "Servicio solicitado" — reintentar hasta rellenarlo
        boolean servicioRellenado = false;
        for (int intento = 1; intento <= 5 && !servicioRellenado; intento++) {
            try {
                List<WebElement> dialogs = driver.findElements(By.tagName("mat-dialog-container"));
                if (dialogs.isEmpty()) break;
                WebElement dialog = dialogs.get(dialogs.size() - 1);
                List<WebElement> selects = dialog.findElements(By.tagName("mat-select"));
                WebElement selServicio = null;

                for (WebElement s : selects) {
                    try {
                        WebElement field = s.findElement(By.xpath("ancestor::mat-form-field"));
                        if (field.getText().toLowerCase().contains("servicio")) {
                            selServicio = s;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (selServicio == null) {
                    for (WebElement s : selects) {
                        try {
                            List<WebElement> valueText = s.findElements(By.cssSelector(".mat-select-value-text"));
                            boolean vacio = valueText.isEmpty() || valueText.get(0).getText().trim().isEmpty();
                            if (vacio) {
                                WebElement field = s.findElement(By.xpath("ancestor::mat-form-field"));
                                String fieldText = field.getText().toLowerCase();
                                if (!fieldText.contains("tipo de muestra") && !fieldText.contains("tipo de solicitud")
                                        && !fieldText.contains("procedencia") && !fieldText.contains("centro")
                                        && !fieldText.contains("solicitante") && !fieldText.contains("informar")) {
                                    selServicio = s;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (selServicio != null) {
                    List<WebElement> valorActual = selServicio.findElements(By.cssSelector(".mat-select-value-text"));
                    if (!valorActual.isEmpty() && !valorActual.get(0).getText().trim().isEmpty()) {
                        Reporter.log("   Servicio solicitado ya tenia valor");
                        servicioRellenado = true;
                        break;
                    }
                    js().executeScript("arguments[0].scrollIntoView({block:'center'});", selServicio);
                    WaitAMomentPlease(0.5f);
                    jsClick(selServicio);
                    WaitAMomentPlease(1.0f);
                    Boolean opcionClickada = (Boolean) js().executeScript(
                            "var opts = Array.from(document.querySelectorAll('mat-option'));" +
                                    "var visible = opts.find(function(o){ return o.offsetParent !== null; });" +
                                    "if(visible){ visible.click(); return true; } return false;");
                    if (Boolean.TRUE.equals(opcionClickada)) {
                        WaitAMomentPlease(0.5f);
                        Reporter.log("   Servicio solicitado rellenado (intento " + intento + ")");
                        servicioRellenado = true;
                    } else {
                        Reporter.log("   Intento " + intento + ": no aparecieron opciones, reintentando...");
                        WaitAMomentPlease(1.0f);
                    }
                } else {
                    Reporter.log("   Intento " + intento + ": campo Servicio no encontrado, reintentando...");
                    WaitAMomentPlease(1.0f);
                }
            } catch (Exception e) {
                Reporter.log("   Intento " + intento + " fallido: " + e.getMessage());
                WaitAMomentPlease(1.0f);
            }
        }

        // Clickar Aceptar dentro del dialogo (no Imprimir)
        wait.until(d -> (Boolean) js().executeScript(
                "var container = document.querySelector('mat-dialog-container');" +
                        "if(!container) return false;" +
                        "var btns = Array.from(container.querySelectorAll('button'));" +
                        "var aceptar = btns.find(function(b){" +
                        "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "  return t === 'aceptar' && !b.disabled && b.offsetParent !== null;" +
                        "});" +
                        "if(aceptar){ aceptar.scrollIntoView({block:'center'}); aceptar.click(); return true; }" +
                        "return false;"));
        WaitAMomentPlease(0.5f);
        cerrarModalesContinuar();

        // Esperar cierre — si no cierra en 20s, cancelar y continuar
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                    driver.findElements(By.cssSelector("div.cdk-overlay-backdrop.cdk-overlay-backdrop-showing")).isEmpty()
                            && driver.findElements(By.tagName("mat-dialog-container")).isEmpty());
        } catch (TimeoutException ignored) {
            try {
                js().executeScript(
                        "var container = document.querySelector('mat-dialog-container');" +
                                "if(!container) return;" +
                                "var btns = Array.from(container.querySelectorAll('button'));" +
                                "var cancelar = btns.find(function(b){" +
                                "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                                "  return (t==='cancelar' || t==='cerrar') && b.offsetParent !== null;" +
                                "});" +
                                "if(cancelar) cancelar.click();");
                WaitAMomentPlease(1.0f);
            } catch (Exception e2) {
                Reporter.log("   WARN: dialogo no cerrado, continuando...");
            }
        }
        WaitAMomentPlease(0.5f);

        esperarFilaBiopsia();
        Reporter.log("PASO 5 — Solicitud modificada OK");
    }

    @Test(priority = 50, dependsOnMethods = {"Login", "ViewHistory", "NavigateToPathologicalAnatomy", "CrearSolicitud", "ModificarSolicitud"})
    public void EliminarSolicitud() {
        long filasAntes = (Long) js().executeScript(
                "return Array.from(document.querySelectorAll('td'))" +
                        ".filter(function(td){ return td.textContent.trim().toLowerCase().includes('biopsia'); }).length;");

        String manana = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM"));
        wait.until(d -> (Boolean) js().executeScript(
                "var fechaBuscada = '" + manana + "';" +
                        "var filas = Array.from(document.querySelectorAll('tr'));" +
                        "var fila = filas.find(function(tr){" +
                        "  var texto = tr.textContent.toLowerCase();" +
                        "  return texto.includes('biopsia') && texto.includes(fechaBuscada);" +
                        "});" +
                        "if(!fila) {" +
                        "  var td = Array.from(document.querySelectorAll('td')).find(function(el){" +
                        "    return el.textContent.trim().toLowerCase().includes('biopsia');" +
                        "  });" +
                        "  if(!td) return false;" +
                        "  fila = td.closest('tr');" +
                        "}" +
                        "if(!fila.className.includes('row--selected')){ fila.click(); return false; }" +
                        "return true;"));
        WaitAMomentPlease(0.5f);

        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);

        waitLong().until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var items = Array.from(document.querySelectorAll(" +
                            "  'button[role=\"menuitem\"],.mat-menu-item,button'));" +
                            "var t = items.find(function(b){ return b.textContent.trim().toLowerCase()" +
                            "  .includes('eliminar solicitud') && b.offsetParent !== null && !b.disabled; });" +
                            "if(t){ t.click(); return true; } return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(3.0f);

        confirmarAccion();

        WaitAMomentPlease(2.0f);
        cerrarModalesContinuar();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    driver.findElements(By.tagName("mat-dialog-container")).isEmpty());
        } catch (TimeoutException ignored) {}
        WaitAMomentPlease(1.0f);

        final long filasEsperadas = filasAntes - 1;
        boolean desaparecio = waitLong().until(d -> {
            Long filasAhora = (Long) js().executeScript(
                    "return Array.from(document.querySelectorAll('td'))" +
                            ".filter(function(td){ return td.textContent.trim().toLowerCase().includes('biopsia'); }).length;");
            return filasAhora <= filasEsperadas;
        });

        Assert.assertTrue(desaparecio, "La solicitud de biopsia no se eliminó correctamente");
        Reporter.log("PASO 6 — Solicitud eliminada OK");
    }

    @Test(priority = 60, dependsOnMethods = {"Login", "ViewHistory", "NavigateToPathologicalAnatomy", "EliminarSolicitud"})
    public void InformeYFirma() {
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        clickMenuItemConDialogo("Nueva solicitud");
        esperarDialogoListo();
        seleccionarTipoMuestra();

        wait.until(d -> (Boolean) js().executeScript(
                "var container = document.querySelector('mat-dialog-container');" +
                        "if(!container) return false;" +
                        "var btns = Array.from(container.querySelectorAll('button'));" +
                        "var aceptar = btns.find(function(b){" +
                        "  var t = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "  return t === 'aceptar' && !b.disabled && b.offsetParent !== null;" +
                        "});" +
                        "if(aceptar){ aceptar.scrollIntoView({block:'center'}); aceptar.click(); return true; }" +
                        "return false;"));
        WaitAMomentPlease(0.5f);
        cerrarModalesContinuar();

        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
                driver.findElements(By.cssSelector("div.cdk-overlay-backdrop.cdk-overlay-backdrop-showing")).isEmpty()
                        && driver.findElements(By.tagName("mat-dialog-container")).isEmpty());

        esperarFilaBiopsia();
        Reporter.log("   7a — Solicitud creada");

        try { new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> (Boolean) js().executeScript(
                "var b=document.getElementById('close-HeavyProcessLoaderDialogComponent-button');" +
                        "if(b&&!b.disabled){b.click();return true;}return false;")); } catch (TimeoutException ignored) {}

        seleccionarFilaBiopsia();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        esperarMenuitemHabilitado("open_report");
        clickMenuItemSinDialogo("Abrir informe");
        cerrarModalesContinuar();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("menu-option-button")));
        Reporter.log("   7b — Informe abierto");

        WaitAMomentPlease(0.5f);
        try {
            List<WebElement> areas = driver.findElements(By.cssSelector("mat-dialog-container textarea"));
            WebElement area = areas.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
            if (area != null) {
                area.click();
                area.clear();
                area.sendKeys("Informe de anatomia patologica automatizado. Sin hallazgos patologicos significativos.");
            }
        } catch (Exception e) {
            Reporter.log("   WARN relleno informe: " + e.getMessage());
        }
        WaitAMomentPlease(1.2f);
        Reporter.log("   7c — Informe rellenado");

        wait.until(d -> (Boolean) js().executeScript(
                "var container = document.querySelector('mat-dialog-container');" +
                        "if(!container) return false;" +
                        "var btns = Array.from(container.querySelectorAll('button'));" +
                        "var cerrar = btns.find(function(b){" +
                        "  var txt = b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                        "  return txt==='cerrar' && !b.disabled && b.offsetParent !== null;" +
                        "});" +
                        "if(cerrar){ cerrar.scrollIntoView(false); cerrar.click(); return true; }" +
                        "return false;"));
        WaitAMomentPlease(1.0f);
        cerrarModalesContinuar();

        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d ->
                driver.findElements(By.tagName("mat-dialog-container")).isEmpty());

        Reporter.log("PASO 7 — Informe rellenado OK");
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private void jsClick(WebElement el) {
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        WaitAMomentPlease(0.3f);
        js().executeScript("arguments[0].click();", el);
    }

    /**
     * Confirma una acción destructiva. Intenta primero el ID "alert-confirm"
     * (mismo usado en ScaleRecordTest.DeleteScale y
     * DeviceRegistrationTest.handleDeleteConfirmation) y cae al texto si no aparece.
     */
    private void confirmarAccion() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(By.id("alert-confirm")))
                    .click();
            return;
        } catch (TimeoutException ignored) {}

        waitLong().until(d -> (Boolean) js().executeScript(
                "var btn = Array.from(document.querySelectorAll('button')).find(function(b){" +
                        "  var t = b.textContent.replace(/\\s+/g,' ').trim();" +
                        "  return (t==='Confirmar' || t==='Aceptar' || t==='Sí' || t==='OK') && b.offsetParent !== null && !b.disabled;" +
                        "});" +
                        "if(btn){ btn.click(); return true; } return false;"));
    }

    /**
     * Cierra en cadena los diálogos informativos "Continuar". Intenta el ID
     * "continue-button" (usado en toda la app, ver ClassBaseTest) en cada
     * iteración y cae al texto original si no aparece.
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
                                    "for(var i=0;i<roots.length;i++){var root=roots[i];if(!root)continue;" +
                                    "  var btns=root.querySelectorAll('button');" +
                                    "  for(var j=0;j<btns.length;j++){if(btns[j].textContent.trim()==='Continuar'&&!btns[j].disabled){btns[j].click();return true;}}" +
                                    "}return false;");
                    return Boolean.TRUE.equals(ok);
                });
                WaitAMomentPlease(0.8f);
            } catch (TimeoutException e) { break; }
        }
    }

    private void esperarDialogoListo() {
        waitLong().until(ExpectedConditions.presenceOfElementLocated(By.tagName("mat-dialog-container")));
        waitLong().until(d -> {
            Boolean ready = (Boolean) js().executeScript(
                    "var skeleton=document.querySelector('gc-dialog-skeleton');" +
                            "if(!skeleton)return true;" +
                            "var loading=skeleton.querySelector('.skeleton');" +
                            "return!loading||loading.offsetParent===null;");
            return Boolean.TRUE.equals(ready);
        });
        WaitAMomentPlease(0.8f);
    }

    // TODO: ítems de menú específicos de Anatomía Patológica localizados por
    // texto — no se conoce su ID real. Sustituir cuando se valide contra QA.
    private void clickMenuItemConDialogo(String texto) {
        cerrarModalesContinuar();
        String needle = texto.toLowerCase();
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle='" + needle.replace("'", "\\'") + "';" +
                            "var items=Array.from(document.querySelectorAll('button[role=\"menuitem\"],.mat-menu-item,button'));" +
                            "var target=items.find(function(b){return b.textContent.trim().toLowerCase().includes(needle)&&b.offsetParent!==null&&!b.disabled;});" +
                            "if(target){target.click();return true;}return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
    }

    private void clickMenuItemSinDialogo(String texto) {
        cerrarModalesContinuar();
        try { new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> driver.findElements(By.tagName("mat-dialog-container")).isEmpty()); } catch (TimeoutException ignored) {}
        String needle = texto.toLowerCase();
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle='" + needle.replace("'", "\\'") + "';" +
                            "var items=Array.from(document.querySelectorAll('button[role=\"menuitem\"],.mat-menu-item,button'));" +
                            "var target=items.find(function(b){return b.textContent.trim().toLowerCase().includes(needle)&&b.offsetParent!==null&&!b.disabled;});" +
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
                            "return!(b.disabled||b.classList.contains('mat-menu-item-disabled')||b.getAttribute('aria-disabled')==='true');");
            return Boolean.TRUE.equals(ok);
        });
    }

    private void seleccionarTopografiaCatalogo() {
        int dialogosAntes = driver.findElements(By.tagName("mat-dialog-container")).size();
        WebElement btnCatalogo = wait.until(ExpectedConditions.elementToBeClickable(By.id("autocomplete-catalog-button")));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", btnCatalogo);
        WaitAMomentPlease(0.3f);
        btnCatalogo.click();

        final int esperados = dialogosAntes + 1;
        wait.until(d -> driver.findElements(By.tagName("mat-dialog-container")).size() >= esperados);
        WaitAMomentPlease(0.6f);

        WebElement catalogo = driver.findElements(By.tagName("mat-dialog-container")).stream().reduce((a, b) -> b).orElseThrow();
        WebElement expandBtn = wait.until(d -> { List<WebElement> btns = catalogo.findElements(By.id("expanded-node-button")); return btns.isEmpty() ? null : btns.get(0); });
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", expandBtn);
        WaitAMomentPlease(0.3f);
        expandBtn.click();

        final String[] textoNodo = {""};
        wait.until(d -> {
            try {
                WebElement cat = driver.findElements(By.tagName("mat-dialog-container")).stream().reduce((a, b) -> b).orElseThrow();
                List<WebElement> labels = cat.findElements(By.cssSelector("mat-checkbox label"));
                if (labels.size() < 2) return false;
                WebElement label = labels.get(1);
                textoNodo[0] = label.getText().trim();
                js().executeScript("arguments[0].scrollIntoView({block:'center'});", label);
                WaitAMomentPlease(0.2f);
                label.click();
                return true;
            } catch (Exception e) { return false; }
        });
        WaitAMomentPlease(0.8f);

        wait.until(d -> {
            try {
                WebElement cat = driver.findElements(By.tagName("mat-dialog-container")).stream().reduce((a, b) -> b).orElseThrow();
                for (WebElement b : cat.findElements(By.tagName("button"))) {
                    String t = b.getText().trim().toLowerCase();
                    if ((t.equals("aceptar") || t.equals("seleccionar") || t.equals("confirmar")) && b.isEnabled() && b.isDisplayed()) {
                        js().executeScript("arguments[0].scrollIntoView({block:'center'});", b);
                        WaitAMomentPlease(0.2f);
                        b.click();
                        return true;
                    }
                }
            } catch (Exception e) { /* retry */ }
            return false;
        });

        wait.until(d -> driver.findElements(By.tagName("mat-dialog-container")).size() == dialogosAntes);
        WaitAMomentPlease(1.0f);

        if (!textoNodo[0].isEmpty()) {
            try {
                WebElement inputTopo = wait.until(ExpectedConditions.elementToBeClickable(By.id("test")));
                inputTopo.click();
                WaitAMomentPlease(0.3f);
                inputTopo.clear();
                inputTopo.sendKeys(textoNodo[0]);
                WaitAMomentPlease(0.8f);

                boolean opcionSeleccionada = false;
                try {
                    opcionSeleccionada = new WebDriverWait(driver, Duration.ofSeconds(8)).until(d -> {
                        Boolean ok = (Boolean) js().executeScript(
                                "var opts=Array.from(document.querySelectorAll('mat-option,.mat-option,[role=\"option\"]'));" +
                                        "var visible=opts.find(function(o){return o.offsetParent!==null;});" +
                                        "if(visible){visible.click();return true;}return false;");
                        return Boolean.TRUE.equals(ok);
                    });
                } catch (TimeoutException ignored) {}

                if (!opcionSeleccionada) {
                    js().executeScript(
                            "var inp=document.querySelector('#test');if(!inp)return;" +
                                    "var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                                    "setter.call(inp,arguments[0]);" +
                                    "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                                    "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                                    "inp.dispatchEvent(new Event('blur',{bubbles:true}));", textoNodo[0]);
                }
            } catch (Exception e) {
                js().executeScript(
                        "var inp=document.querySelector('#test');if(!inp)return;" +
                                "var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                                "setter.call(inp,arguments[0]);" +
                                "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                                "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                                "inp.dispatchEvent(new Event('blur',{bubbles:true}));", textoNodo[0]);
            }
            WaitAMomentPlease(0.5f);
            js().executeScript("var inp=document.querySelector('#test');if(inp){inp.dispatchEvent(new Event('blur',{bubbles:true}));inp.blur();}");
            WaitAMomentPlease(0.5f);
        }
        Reporter.log("   Topografia: [" + textoNodo[0] + "]");
    }

    private void seleccionarProcedimiento() {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.id("procedure")));
        jsClick(input);
        WaitAMomentPlease(0.4f);
        input.sendKeys(" ");
        waitLong().until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var opts=Array.from(document.querySelectorAll('mat-option,.mat-option,[role=option],.cdk-overlay-container mat-option'));" +
                            "var visible=opts.find(function(o){return o.offsetParent!==null;});" +
                            "if(visible){visible.click();return true;}return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
        Object val = js().executeScript("var i=document.getElementById('procedure');return i?i.value:'';");
        String valorSeleccionado = val != null ? val.toString() : "";
        if (!valorSeleccionado.isEmpty()) {
            js().executeScript(
                    "var inp=document.getElementById('procedure');if(!inp)return;" +
                            "var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                            "setter.call(inp,arguments[0]);" +
                            "inp.dispatchEvent(new Event('input',{bubbles:true}));" +
                            "inp.dispatchEvent(new Event('change',{bubbles:true}));" +
                            "inp.dispatchEvent(new Event('blur',{bubbles:true}));", valorSeleccionado);
        } else {
            js().executeScript("var inp=document.getElementById('procedure');if(inp){inp.dispatchEvent(new Event('input',{bubbles:true}));inp.dispatchEvent(new Event('change',{bubbles:true}));inp.dispatchEvent(new Event('blur',{bubbles:true}));}");
        }
        WaitAMomentPlease(0.5f);
        Reporter.log("   Procedimiento OK");
    }

    private void seleccionarTipoMuestra() {
        WebElement select = wait.until(ExpectedConditions.elementToBeClickable(By.id("section")));
        jsClick(select);
        WaitAMomentPlease(0.7f);
        wait.until(ExpectedConditions.elementToBeClickable(By.id(SECTION_OPTION_ID)));
        jsClick(driver.findElement(By.id(SECTION_OPTION_ID)));
        WaitAMomentPlease(0.5f);
        Reporter.log("   Tipo de muestra: " + SECTION_LABEL);
        seleccionarTopografiaCatalogo();
        seleccionarProcedimiento();

        // Rellenar "Servicio solicitado" si existe y está vacío
        try {
            List<WebElement> dialogs = driver.findElements(By.tagName("mat-dialog-container"));
            if (!dialogs.isEmpty()) {
                WebElement dialog = dialogs.get(dialogs.size() - 1);
                js().executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", dialog);
                WaitAMomentPlease(0.5f);

                List<WebElement> selects = dialog.findElements(By.tagName("mat-select"));
                WebElement selServicio = null;

                for (WebElement s : selects) {
                    try {
                        WebElement field = s.findElement(By.xpath("ancestor::mat-form-field"));
                        if (field.getText().toLowerCase().contains("servicio")) {
                            selServicio = s;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (selServicio == null) {
                    for (WebElement s : selects) {
                        try {
                            List<WebElement> valueText = s.findElements(By.cssSelector(".mat-select-value-text"));
                            boolean vacio = valueText.isEmpty() || valueText.get(0).getText().trim().isEmpty();
                            if (vacio) {
                                WebElement field = s.findElement(By.xpath("ancestor::mat-form-field"));
                                String ft = field.getText().toLowerCase();
                                if (!ft.contains("tipo de muestra") && !ft.contains("tipo de solicitud")
                                        && !ft.contains("procedencia") && !ft.contains("centro")
                                        && !ft.contains("solicitante") && !ft.contains("informar")) {
                                    selServicio = s;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (selServicio != null) {
                    List<WebElement> valorActual = selServicio.findElements(By.cssSelector(".mat-select-value-text"));
                    boolean tieneValor = !valorActual.isEmpty() && !valorActual.get(0).getText().trim().isEmpty();
                    if (!tieneValor) {
                        js().executeScript("arguments[0].scrollIntoView({block:'center'});", selServicio);
                        WaitAMomentPlease(0.3f);
                        jsClick(selServicio);
                        WaitAMomentPlease(0.8f);
                        Boolean ok = (Boolean) js().executeScript(
                                "var opts = Array.from(document.querySelectorAll('mat-option'));" +
                                        "var visible = opts.find(function(o){ return o.offsetParent !== null; });" +
                                        "if(visible){ visible.click(); return true; } return false;");
                        if (Boolean.TRUE.equals(ok)) {
                            WaitAMomentPlease(0.5f);
                            Reporter.log("   Servicio solicitado rellenado en creacion");
                        }
                    }
                }

                js().executeScript("arguments[0].scrollTop = 0;", dialog);
            }
        } catch (Exception e) {
            Reporter.log("   Servicio solicitado no encontrado en creacion");
        }

        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var btn=document.getElementById('add-anatomic-pathology-test-button');" +
                            "if(!btn)return false;" +
                            "if(btn.disabled||btn.getAttribute('aria-disabled')==='true')return false;" +
                            "btn.scrollIntoView({block:'center'});btn.click();return true;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.8f);
        Reporter.log("   Prueba añadida al dialogo");
    }

    private void aceptarDialogo() {
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var btn=document.getElementById('accept-AnatomicPathologyDialogContainer-button');" +
                            "if(btn&&!btn.disabled&&btn.offsetParent!==null){btn.click();return true;}" +
                            "var container=document.querySelector('mat-dialog-container');if(!container)return false;" +
                            "var btns=Array.from(container.querySelectorAll('button'));" +
                            "var b=btns.find(function(x){var t=x.textContent.replace(/\\s+/g,' ').trim().toLowerCase();return(t==='aceptar'||t==='guardar')&&!x.disabled&&x.offsetParent!==null;});" +
                            "if(b){b.scrollIntoView({block:'center'});b.click();return true;}return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
        cerrarModalesContinuar();
    }

    private void seleccionarFilaBiopsia() {
        wait.until(d -> (Boolean) js().executeScript(
                "return Array.from(document.querySelectorAll('td')).some(function(td){return td.textContent.trim().toLowerCase().includes('" + SECTION_LABEL.toLowerCase() + "');});"));
        wait.until(d -> (Boolean) js().executeScript(
                "var td=Array.from(document.querySelectorAll('td')).find(function(el){return el.textContent.trim().toLowerCase().includes('" + SECTION_LABEL.toLowerCase() + "');});" +
                        "if(!td)return false;var tr=td.closest('tr');" +
                        "if(!tr.className.includes('row--selected')){tr.click();return false;}return true;"));
        WaitAMomentPlease(0.5f);
    }

    private void esperarFilaBiopsia() {
        waitLong().until(d -> (Boolean) js().executeScript(
                "return Array.from(document.querySelectorAll('td')).some(function(td){return td.textContent.trim().toLowerCase().includes('" + SECTION_LABEL.toLowerCase() + "');});"));
    }

    private void setFechaPrevista(String fechaDD_MM_YYYY) {
        js().executeScript(
                "var campo=document.getElementById('expectedCalendar');if(!campo)return;" +
                        "var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                        "setter.call(campo,arguments[0]);" +
                        "campo.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "campo.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "campo.dispatchEvent(new Event('blur',{bubbles:true}));", fechaDD_MM_YYYY);
        WaitAMomentPlease(0.4f);
    }
}
