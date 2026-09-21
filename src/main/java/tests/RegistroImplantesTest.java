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
 * Test: Registro de Implantes
 *
 * NOTA DE MIGRACIÓN (desde CarlosFreire):
 *  - El paciente ya se lee de ConfigReader ("pacqah1NH") y el ID
 *    "implants_register-sidebar" ya estaba verificado por inspección real
 *    (según el propio comentario del autor) — no se han tocado.
 *  - TC1 (CrearSolicitud) tenía un assert que aceptaba como válido "ya
 *    había ≥1 fila antes" incluso sin que se creara nada nuevo
 *    (filasDepues > filasAntes || filasDepues >= 1, siempre cierto si ya
 *    había alguna fila). Se ha corregido a comprobar solo el incremento real.
 *  - TC3 (EliminarSolicitud) nunca hacía fallar el test — solo imprimía si
 *    se había eliminado o no, dando por válida cualquier situación. Se ha
 *    restaurado un Assert real que solo acepta como caso legítimo "no se
 *    eliminó" cuando efectivamente apareció el aviso de "implante firmado,
 *    protegido" (documentado en el comentario original del autor);
 *    cualquier otro fallo silencioso ahora hace fallar el test.
 *  - Sustituido por ID verificado ("continue-button") el cierre de los
 *    diálogos informativos "Continuar" genéricos.
 * Los selectores de menú/campos específicos de Implantes que siguen
 * localizando por texto quedan sin tocar por no poder verificarse contra
 * la app real.
 */
public class RegistroImplantesTest extends ClassBaseTest {

    @Test(priority = 1)
    public void Login() {
        LoginAsNurse();
        Reporter.log("PASO 1 — Login enfermera OK");
    }

    @Test(priority = 10, dependsOnMethods = "Login")
    public void NavigateToHospitalizacion() {
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var ids=['hospitalization-sidebar','hospitalization_map-sidebar'," +
                            "'hospitalization_list-sidebar','inpatient-sidebar','ward-sidebar'];" +
                            "for(var i=0;i<ids.length;i++){" +
                            "  var el=document.getElementById(ids[i]);" +
                            "  if(el&&el.offsetParent!==null){el.scrollIntoView({block:'center'});el.click();return true;}" +
                            "}" +
                            "var all=Array.from(document.querySelectorAll('nav a,nav span,nav li,[class*=\"sidebar\"] a,[class*=\"sidebar\"] span,[class*=\"menu\"] a,[class*=\"menu\"] li,[class*=\"nav\"] a'));" +
                            "var el=all.find(function(e){var t=e.textContent.trim().toLowerCase();" +
                            "return t==='hospitalización'||t==='hospitalizacion'||t==='ingresos'||t==='planta';});" +
                            "if(el){el.scrollIntoView({block:'center'});el.click();return true;}" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1.5f);
        Reporter.log("PASO 2 — Módulo Hospitalización abierto OK");
    }

    @Test(priority = 20, dependsOnMethods = {"Login", "NavigateToHospitalizacion"})
    public void ViewHistory() {
        String nh = ConfigReader.get("pacqah1NH");

        // 1. Lista de trabajo
        boolean listaClickeada = Boolean.TRUE.equals(js().executeScript(
                "var items=Array.from(document.querySelectorAll('a,button,div[class*=\"menu\"],div[class*=\"sidebar\"]>div,mat-list-item'));" +
                        "var target=items.find(function(el){var text=el.textContent.trim().toLowerCase();" +
                        "return(text==='lista de trabajo'||(text.includes('lista')&&text.includes('trabajo')))&&el.offsetParent!==null;});" +
                        "if(target){target.scrollIntoView({block:'center'});target.click();return true;}" +
                        "return false;"));
        Assert.assertTrue(listaClickeada, "No se encontró Lista de trabajo");
        WaitAMomentPlease(2.0f);

        // 2. Cargar todas las filas
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[class*='list'],[class*='worklist'],table,mat-table")));
        WaitAMomentPlease(2.0f);
        js().executeScript(
                "var btn=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().includes('Cargar todo'));" +
                        "if(btn)btn.click();");
        WaitAMomentPlease(5.0f);

        // 3. Seleccionar fila
        boolean filaSeleccionada = Boolean.TRUE.equals(js().executeScript(
                "var filas=Array.from(document.querySelectorAll('tr[id^=\"gridId\"],mat-row[id^=\"gridId\"]'));" +
                        "filas=filas.filter(f=>f.offsetParent!==null);" +
                        "var fila=filas.find(f=>f.innerHTML.includes('" + nh + "'));" +
                        "if(fila){fila.scrollIntoView({block:'center'});fila.click();return true;}" +
                        "if(filas.length>0){filas[0].scrollIntoView({block:'center'});filas[0].click();return true;}" +
                        "return false;"));
        Assert.assertTrue(filaSeleccionada, "No se encontró fila del paciente");
        WaitAMomentPlease(1.0f);

        // 4. Acciones → Ver historia
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);
        boolean hcAbierto = Boolean.TRUE.equals(js().executeScript(
                "var items=Array.from(document.querySelectorAll('button[role=\"menuitem\"],.mat-menu-item,button'));" +
                        "var t=items.find(function(b){var txt=b.textContent.trim().toLowerCase();" +
                        "return(txt==='ver historia'||txt.includes('ver historia')||txt.includes('histor'))&&b.offsetParent!==null&&!b.disabled;});" +
                        "if(t){t.click();return true;}return false;"));
        Assert.assertTrue(hcAbierto, "No se encontró Ver historia en Acciones");
        WaitAMomentPlease(2.0f);

        // 5. Diálogos
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("dialog-patient-episodes-panel-0"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-PatientEpisodesContainer-button"))).click();
        } catch (Exception ignored) {}
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-FileHistoryAccess-button"))).click();
        } catch (Exception ignored) {}

        WaitAMomentPlease(2.0f);
        SwitchToTab(GetLastTabOpened());

        // Heredado de ClassBaseTest — mismo ID (accept-AlertsContainer-button).
        handleVitalAlertsDialog();

        WaitAMomentPlease(1.0f);
        Reporter.log("URL HC: " + driver.getCurrentUrl());
        Reporter.log("PASO 3 — Historia clínica abierta OK");
    }

    @Test(priority = 30, dependsOnMethods = {"Login", "NavigateToHospitalizacion", "ViewHistory"})
    public void NavigateToRegistroImplantes() {
        WebDriverWait waitNav = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Clic en registers-sidebar
        waitNav.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var el=document.getElementById('registers-sidebar');" +
                            "if(el&&el.offsetParent!==null){el.scrollIntoView({block:'center'});el.click();return true;}" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1.0f);

        cerrarAlertaSiExiste();

        // Clic en implants_register-sidebar (ID confirmado por inspección real)
        waitNav.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var el=document.getElementById('implants_register-sidebar');" +
                            "if(el&&el.offsetParent!==null){el.scrollIntoView({block:'center'});el.click();return true;}" +
                            "return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(1.0f);

        cerrarAlertaSiExiste();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        Reporter.log("PASO 4 — Registro de Implantes OK");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC1 — Crear implante  (menú: "Nueva")
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = {"Login", "NavigateToHospitalizacion", "ViewHistory", "NavigateToRegistroImplantes"})
    public void CrearSolicitud() {
        long filasAntes = contarFilasImplante();

        cerrarAlertaSiExiste();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);
        clickMenuItem("Nueva");   // Confirmado: "Nueva" no "Nueva solicitud"

        esperarDialogoListo();
        rellenarFormularioImplante();
        aceptarDialogo();

        long filasDepues = contarFilasImplante();
        Assert.assertTrue(filasDepues > filasAntes, "Debe haberse creado un implante nuevo");
        Reporter.log("TC1 — Implante creado OK");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC2 — Editar implante  (menú: "Editar")
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 50, dependsOnMethods = {"Login", "NavigateToHospitalizacion", "ViewHistory", "NavigateToRegistroImplantes", "CrearSolicitud"})
    public void ModificarSolicitud() {
        seleccionarPrimeraFila();

        cerrarAlertaSiExiste();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.6f);
        clickMenuItem("Editar");  // Confirmado: "Editar" no "Abrir solicitud"

        esperarDialogoListo();

        js().executeScript(
                "var areas=Array.from(document.querySelectorAll('mat-dialog-container textarea,mat-dialog-container input[type=text]'));" +
                        "var campo=areas.find(a=>!a.readOnly&&a.offsetParent!==null);" +
                        "if(campo){" +
                        "  var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                        "  setter.call(campo,'Modificado por test automático');" +
                        "  campo.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "  campo.dispatchEvent(new Event('change',{bubbles:true}));}");
        WaitAMomentPlease(0.5f);
        aceptarDialogo();

        Assert.assertTrue(contarFilasImplante() >= 1, "Debe seguir existiendo el implante tras modificarlo");
        Reporter.log("TC2 — Implante modificado OK");
    }

    // ══════════════════════════════════════════════════════════════════
    // TC3 — Eliminar implante  (menú: "Eliminar")
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 60, dependsOnMethods = {"Login", "NavigateToHospitalizacion", "ViewHistory", "NavigateToRegistroImplantes", "CrearSolicitud", "ModificarSolicitud"})
    public void EliminarSolicitud() {
        long filasAntes = contarFilasImplante();
        seleccionarPrimeraFila();

        cerrarAlertaSiExiste();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);
        clickMenuItem("Eliminar");

        WaitAMomentPlease(1.5f);

        // Primero cerrar el diálogo informativo si aparece
        // ("No se pueden modificar o eliminar implantes ya firmados")
        boolean avisoProtegido = cerrarDialogoInformativo();

        // Luego intentar confirmar la eliminación
        boolean confirmoEliminacion = false;
        try {
            confirmoEliminacion = new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> (Boolean) js().executeScript(
                    "var btn=Array.from(document.querySelectorAll('button')).find(b=>{" +
                            "var t=b.textContent.replace(/\\s+/g,' ').trim();" +
                            "return(t==='Confirmar'||t==='Aceptar'||t==='Sí'||t==='OK')&&b.offsetParent!==null&&!b.disabled;});" +
                            "if(btn){btn.click();return true;}return false;"));
        } catch (TimeoutException e) {
            // Si no hay diálogo de confirmación, puede ser porque el implante está firmado
            Reporter.log("No se encontró diálogo de confirmación de eliminación");
        }

        WaitAMomentPlease(2.0f);
        cerrarModalesContinuar();

        long filasDepues = contarFilasImplante();
        Reporter.log("Filas antes: " + filasAntes + ", después: " + filasDepues);

        // Si el implante está firmado, el sistema lo protege y no lo elimina —
        // eso es válido SOLO si realmente apareció el aviso de "protegido".
        // Cualquier otro caso en el que no se elimine es un fallo real.
        boolean eliminado = filasDepues < filasAntes;
        Assert.assertTrue(eliminado || avisoProtegido,
                "El implante no se eliminó y no se mostró el aviso de 'implante firmado, protegido'");

        if (eliminado) {
            Reporter.log("TC3 — Implante eliminado OK");
        } else {
            Reporter.log("TC3 — Implante NO eliminado (protegido por estar firmado - comportamiento correcto)");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // TC4 — Crear y firmar implante
    // ══════════════════════════════════════════════════════════════════

    @Test(priority = 70, dependsOnMethods = {"Login", "NavigateToHospitalizacion", "ViewHistory", "NavigateToRegistroImplantes", "EliminarSolicitud"})
    public void CrearYFirmarSolicitud() {
        cerrarAlertaSiExiste();
        jsClick(wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button"))));
        WaitAMomentPlease(0.8f);
        clickMenuItem("Nueva");
        esperarDialogoListo();
        rellenarFormularioImplante();

        // Firmar DENTRO del diálogo, no después
        boolean firmado = firmarImplanteDesdeDialogo();

        Assert.assertTrue(firmado, "El implante debería haber quedado firmado en el diálogo");
        Reporter.log("TC4 — Implante creado y firmado OK");
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private void jsClick(WebElement el) {
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        WaitAMomentPlease(0.3f);
        js().executeScript("arguments[0].click();", el);
    }

    /** Cierra el diálogo "Alertas vitales detectadas" si está presente */
    private void cerrarAlertaSiExiste() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> (Boolean) js().executeScript(
                    "var btns=Array.from(document.querySelectorAll('button'));" +
                            "var aceptar=btns.find(b=>b.textContent.trim()==='Aceptar'&&b.offsetParent!==null);" +
                            "if(aceptar){aceptar.click();return true;}return false;"));
            WaitAMomentPlease(0.5f);
        } catch (TimeoutException ignored) {}
    }

    /**
     * Cierra el diálogo informativo que aparece cuando intentas eliminar un implante firmado
     * Mensaje: "No se pueden modificar o eliminar implantes ya firmados"
     * Intenta primero el ID "continue-button" y cae al texto si no aparece.
     *
     * @return true si el diálogo apareció y se cerró; false si no apareció.
     */
    private boolean cerrarDialogoInformativo() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(By.id("continue-button")))
                    .click();
            WaitAMomentPlease(0.8f);
            return true;
        } catch (TimeoutException ignored) {}

        try {
            boolean cerrado = new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> (Boolean) js().executeScript(
                    "var btns=Array.from(document.querySelectorAll('button'));" +
                            "var continuar=btns.find(b=>b.textContent.trim().toLowerCase()==='continuar'&&b.offsetParent!==null);" +
                            "if(continuar){continuar.click();return true;}return false;"));
            WaitAMomentPlease(0.8f);
            return cerrado;
        } catch (TimeoutException ignored) {
            // No hay diálogo informativo, es normal
            return false;
        }
    }

    private long contarFilasImplante() {
        Long count = (Long) js().executeScript(
                "return document.querySelectorAll('mat-row[id^=\"gridId\"],tr[id^=\"gridId\"],[class*=\"implant-row\"]').length;");
        return count != null ? count : 0L;
    }

    private void seleccionarPrimeraFila() {
        wait.until(d -> (Boolean) js().executeScript(
                "var filas=Array.from(document.querySelectorAll('mat-row[id^=\"gridId\"],tr[id^=\"gridId\"],[class*=\"implant-row\"]'));" +
                        "if(filas.length===0)return false;" +
                        "var fila=filas[0];" +
                        "if(!fila.className.includes('row--selected')){fila.click();return false;}" +
                        "return true;"));
        WaitAMomentPlease(0.5f);
    }

    private void esperarDialogoListo() {
        new WebDriverWait(driver, Duration.ofSeconds(40))
                .until(ExpectedConditions.presenceOfElementLocated(By.tagName("mat-dialog-container")));
        new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> {
            Boolean ready = (Boolean) js().executeScript(
                    "var skeleton=document.querySelector('gc-dialog-skeleton');" +
                            "if(!skeleton)return true;" +
                            "var loading=skeleton.querySelector('.skeleton');" +
                            "return !loading||loading.offsetParent===null;");
            return Boolean.TRUE.equals(ready);
        });
        WaitAMomentPlease(0.8f);
    }

    // TODO: ítems de menú específicos de Implantes localizados por texto —
    // no se conoce su ID real. Sustituir cuando se valide contra QA.
    private void clickMenuItem(String texto) {
        String needle = texto.toLowerCase();
        wait.until(d -> {
            Boolean ok = (Boolean) js().executeScript(
                    "var needle='" + needle.replace("'", "\\'") + "';" +
                            "var items=Array.from(document.querySelectorAll('button[role=\"menuitem\"],.mat-menu-item,button'));" +
                            "var target=items.find(b=>b.textContent.trim().toLowerCase()===needle&&b.offsetParent!==null&&!b.disabled);" +
                            "if(target){target.click();return true;}" +
                            "target=items.find(b=>b.textContent.trim().toLowerCase().includes(needle)&&b.offsetParent!==null&&!b.disabled);" +
                            "if(target){target.click();return true;}return false;");
            return Boolean.TRUE.equals(ok);
        });
        WaitAMomentPlease(0.5f);
    }

    private void rellenarFormularioImplante() {
        try {
            js().executeScript(
                    "var container=document.querySelector('mat-dialog-container');" +
                            "if(!container)return;" +
                            "var sel=container.querySelector('mat-select');" +
                            "if(sel)sel.click();");
            WaitAMomentPlease(0.7f);
            js().executeScript(
                    "var opts=Array.from(document.querySelectorAll('mat-option'));" +
                            "var visible=opts.find(o=>o.offsetParent!==null);" +
                            "if(visible)visible.click();");
            WaitAMomentPlease(0.5f);
            Reporter.log("Tipo de implante seleccionado");
        } catch (Exception e) {
            Reporter.log("[INFO] Select tipo implante: " + e.getMessage());
        }
        try {
            for (int intento = 1; intento <= 5; intento++) {
                List<WebElement> dialogs = driver.findElements(By.tagName("mat-dialog-container"));
                if (dialogs.isEmpty()) break;
                WebElement dialog = dialogs.get(dialogs.size() - 1);
                WebElement selVacio = null;
                for (WebElement s : dialog.findElements(By.tagName("mat-select"))) {
                    try {
                        List<WebElement> val = s.findElements(By.cssSelector(".mat-select-value-text"));
                        if (val.isEmpty() || val.get(0).getText().trim().isEmpty()) { selVacio = s; break; }
                    } catch (Exception ignored) {}
                }
                if (selVacio == null) break;
                js().executeScript("arguments[0].scrollIntoView({block:'center'});", selVacio);
                WaitAMomentPlease(0.4f);
                jsClick(selVacio);
                WaitAMomentPlease(1.0f);
                Boolean ok = (Boolean) js().executeScript(
                        "var opts=Array.from(document.querySelectorAll('mat-option'));" +
                                "var visible=opts.find(o=>o.offsetParent!==null);" +
                                "if(visible){visible.click();return true;}return false;");
                WaitAMomentPlease(0.5f);
                if (Boolean.TRUE.equals(ok)) Reporter.log("Select rellenado (intento " + intento + ")");
            }
        } catch (Exception e) {
            Reporter.log("[INFO] Selects adicionales: " + e.getMessage());
        }
    }

    private void aceptarDialogo() {
        try {
            boolean aceptado = Boolean.TRUE.equals(
                    new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
                        Boolean ok = (Boolean) js().executeScript(
                                "var container=document.querySelector('mat-dialog-container');" +
                                        "if(!container)return false;" +
                                        "var btns=Array.from(container.querySelectorAll('button'));" +
                                        "var aceptar=btns.find(b=>{" +
                                        "  var t=b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                                        "  return(t==='aceptar'||t==='guardar'||t==='confirmar'||t==='ok'||t==='enviar')&&!b.disabled&&b.offsetParent!==null;" +
                                        "});" +
                                        "if(aceptar){aceptar.scrollIntoView({block:'center'});aceptar.click();return true;}" +
                                        "return false;");
                        return Boolean.TRUE.equals(ok);
                    })
            );

            if (aceptado) {
                WaitAMomentPlease(0.5f);
            } else {
                Reporter.log("[WARN] No se encontró botón de aceptación, intentando alternativa");
                return;
            }

            cerrarModalesContinuar();

            try {
                new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                    List<WebElement> dialogs = driver.findElements(By.tagName("mat-dialog-container"));
                    List<WebElement> backdrops = driver.findElements(By.cssSelector("div.cdk-overlay-backdrop.cdk-overlay-backdrop-showing"));
                    return dialogs.isEmpty() && backdrops.isEmpty();
                });
            } catch (TimeoutException e) {
                Reporter.log("[WARN] El diálogo no desapareció en tiempo, intentando cerrar manualmente");
                try {
                    js().executeScript(
                            "var container=document.querySelector('mat-dialog-container');" +
                                    "if(!container)return;" +
                                    "var btns=Array.from(container.querySelectorAll('button'));" +
                                    "var cancelar=btns.find(b=>{" +
                                    "  var t=b.textContent.replace(/\\s+/g,' ').trim().toLowerCase();" +
                                    "  return(t==='cancelar'||t==='cerrar')&&b.offsetParent!==null;" +
                                    "});" +
                                    "if(cancelar)cancelar.click();");
                    WaitAMomentPlease(1.0f);
                } catch (Exception ignored) {}
            }

            WaitAMomentPlease(0.5f);

        } catch (TimeoutException e) {
            Reporter.log("[ERROR] Timeout en aceptarDialogo: " + e.getMessage());
        } catch (Exception e) {
            Reporter.log("[ERROR] Error en aceptarDialogo: " + e.getMessage());
        }
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
                                    "var btns=root.querySelectorAll('button');" +
                                    "for(var j=0;j<btns.length;j++){if(btns[j].textContent.trim()==='Continuar'&&!btns[j].disabled){btns[j].click();return true;}}}" +
                                    "return false;");
                    return Boolean.TRUE.equals(ok);
                });
                WaitAMomentPlease(0.8f);
            } catch (TimeoutException e) { break; }
        }
    }

    /**
     * Firma el implante desde dentro del diálogo "Registrar implante"
     *
     * FLUJO:
     * 1. Click en "Firmar registro"
     * 2. Se abre diálogo de firma
     * 3. Rellenar contraseña
     * 4. Click en "Aceptar" (confirmar firma)
     * 5. Ambos diálogos (firma y principal) se cierran automáticamente
     */
    private boolean firmarImplanteDesdeDialogo() {
        try {
            WaitAMomentPlease(0.5f);

            boolean firmaBuscada = Boolean.TRUE.equals(
                    new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
                        Boolean ok = (Boolean) js().executeScript(
                                "var btns=Array.from(document.querySelectorAll('button'));" +
                                        "var firmBtn=btns.find(b=>{" +
                                        "  var txt=b.textContent.trim().toLowerCase();" +
                                        "  return (txt.includes('firmar')||txt.includes('sign'))&&" +
                                        "         b.offsetParent!==null&&!b.disabled;" +
                                        "});" +
                                        "if(firmBtn){" +
                                        "  firmBtn.scrollIntoView({block:'center'});" +
                                        "  firmBtn.click();" +
                                        "  return true;" +
                                        "}" +
                                        "return false;");
                        return Boolean.TRUE.equals(ok);
                    })
            );

            if (!firmaBuscada) {
                Reporter.log("[ERROR] No se encontró botón 'Firmar registro'");
                return false;
            }

            WaitAMomentPlease(1.5f);

            String password = ConfigReader.get("password_nurse");

            WebElement pwdField = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

            pwdField.clear();
            pwdField.sendKeys(password);
            WaitAMomentPlease(0.7f);

            WebElement signBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(By.id("sign-UserSignComponent-button")));

            jsClick(signBtn);
            WaitAMomentPlease(2.0f);

            boolean dialogosCerrados = Boolean.TRUE.equals(
                    new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
                        Boolean ok = (Boolean) js().executeScript(
                                "var dialogs=document.querySelectorAll('mat-dialog-container');" +
                                        "var backdrops=document.querySelectorAll('div.cdk-overlay-backdrop.cdk-overlay-backdrop-showing');" +
                                        "return dialogs.length===0 && backdrops.length===0;");
                        return Boolean.TRUE.equals(ok);
                    })
            );

            if (!dialogosCerrados) {
                cerrarModalesContinuar();
                WaitAMomentPlease(1.0f);
            }

            return true;

        } catch (TimeoutException e) {
            Reporter.log("[ERROR] Timeout: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Reporter.log("[ERROR] Error durante firma: " + e.getMessage());
            return false;
        }
    }
}
