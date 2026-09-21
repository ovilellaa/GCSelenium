package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Migrado desde "Pruebas Jordi\GCSelenium" (proyecto desconectado de Git).
 * Es el equivalente de EmergencyWLTest para la lista de trabajo de
 * Hospitalización: los métodos auxiliares de navegación (OpenActionMenu,
 * IsPatientInEmergency, TakeOffEmergencyFilters, EnterHospitalizationHistory...)
 * se han endurecido con el mismo patrón ya verificado en EmergencyWLTest
 * (JS click para evitar "element click intercepted", wait.until en vez de
 * findElement directo). Los selectores específicos del formulario de ingreso
 * (destSpecialityId, admitTypeId, admitPriorityId, diagnostic...) proceden
 * de la sesión de exploración original de Jordi y no se han podido verificar
 * contra la app real, igual que en la migración de RegistroImplantesTest.
 */
public class HospitalizationWLTest extends ClassBaseTest {

    @Test(priority = 1)
    public void EnterHospitalizationWL() {
        LoginAsDoctor();

        Assert.assertTrue(OpenHospitalizationWL());
    }

    @Test(priority = 2, dependsOnMethods = {"EnterHospitalizationWL"})
    public void CreateHospitalizationSheet() {
        GotoMNPTab();

        // comprobamos si el paciente está en urgencias antes de intentar ingresarlo
        Assert.assertTrue(OpenEmergencyWL());
        if (!IsPatientInEmergency()) {
            // comprobamos si el paciente ya está hospitalizado antes de crear una nueva solicitud de ingreso
            Assert.assertTrue(OpenHospitalizationWL());
            if (!IsPatientInHospitalization()) {
                OpenNoContextualMNPActions();

                WebElement solicitudIngreso = driver.findElement(By.id("entry_request"));
                solicitudIngreso.click();

                //Busca y selecciona al paciente
                WebElement paciente = driver.findElement(By.id("patient"));
                String NIF = ConfigReader.get("pacqah1NIF");
                paciente.sendKeys(NIF);
                WebElement seleccionarPaciente = driver.findElement(By.id("patient-0"));
                seleccionarPaciente.click();

                // comprobamos si se abre la pantalla de ficha paciente por si el centro lo tiene configurado así
                try {
                    WebElement cancelPatienFileButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cancel-PatientFileContainer-button")));
                    if (cancelPatienFileButton.isDisplayed())
                        cancelPatienFileButton.click();
                } catch (TimeoutException e) {

                }

                WaitAMomentPlease();

                //Selecciona el servicio al que ingresa (Medicina General)
                WebElement servicioIngreso = driver.findElement(By.id("destSpecialityId"));
                servicioIngreso.click();
                WaitAMomentPlease();
                WebElement seleccionarServicioIngreso = driver.findElement(By.id("destSpecialityId-55"));
                seleccionarServicioIngreso.click();

                //Selecciona el Tipo de ingreso Programado
                WebElement tipoIngreso = driver.findElement(By.id("admitTypeId"));
                tipoIngreso.click();
                WebElement seleccionarTipo = driver.findElement(By.id("admitTypeId-2"));
                seleccionarTipo.click();

                //Selecciona la Prioridad de ingreso de Máxima Preferencia
                WebElement prioridadIngreso = driver.findElement(By.id("admitPriorityId"));
                prioridadIngreso.click();
                WebElement seleccionarPrioridad = driver.findElement(By.id("admitPriorityId-0"));
                seleccionarPrioridad.click();

                //Selecciona el diagnóstico de Cólera
                WebElement diagnostico = driver.findElement(By.id("diagnostic"));
                diagnostico.sendKeys("Colera");
                WebElement seleccionarDiagnostico = driver.findElement(By.id("diagnostic-0-0-0-0"));
                seleccionarDiagnostico.click();
                WebElement anadirEnfermedad = driver.findElement(By.id("add-diagnosis-button"));
                anadirEnfermedad.click();

                WaitAMomentPlease();

                //Acepta el formulario
                WebElement aceptarCrearSolicitud = driver.findElement(By.id("accept-EntryRequestContainer-button"));
                aceptarCrearSolicitud.click();

                WaitAMomentPlease();
            } else {
                Reporter.log("⚠ El paciente ya está hospitalizado.");
            }
        } else {
            Reporter.log("⚠ El paciente está en urgencias.");
        }
    }

    @Test(priority = 3, dependsOnMethods = {"EnterHospitalizationWL", "CreateHospitalizationSheet"})
    public void AdmitPatientToBed() {
        GotoMNPTab();

        Assert.assertTrue(OpenEmergencyWL());
        if (!IsPatientInEmergency()) {
            Assert.assertTrue(OpenHospitalizationWL());
            if (!IsPatientInHospitalization()) {
                Assert.assertTrue(OpenPatientAdmission());

                //Busca al paciente
                WebElement paciente = driver.findElement(By.id("patientFilter"));
                String NH = ConfigReader.get("pacqah1NH");
                paciente.sendKeys(NH);
                List<WebElement> pacientes = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("[id^='tree-node-']")
                ));

                if (!pacientes.isEmpty()) {
                    WebElement primerPaciente = wait.until(ExpectedConditions.elementToBeClickable(pacientes.getFirst()));
                    primerPaciente.click();

                    LocateFreeBed();

                    WaitAMomentPlease();
                    WebElement ingresar = driver.findElement(By.id("admit"));
                    ingresar.click();

                    List<WebElement> botonesConfirmar = driver.findElements(By.id("alert-confirm"));
                    if (!botonesConfirmar.isEmpty()) {
                        botonesConfirmar.getFirst().click();
                    } else {
                        List<WebElement> botonesCancelar = driver.findElements(By.id("alert-Cancelar"));
                        if (!botonesCancelar.isEmpty()) {
                            botonesCancelar.getFirst().click();
                        }
                    }
                } else {
                    Reporter.log("⚠ El paciente no tiene una solicitud de ingreso.");
                }
            } else {
                Reporter.log("⚠ El paciente ya está ingresado.");
            }
        } else {
            Reporter.log("⚠ El paciente está en urgencias.");
        }
    }

    @Test(priority = 4, dependsOnMethods = {"EnterHospitalizationWL", "CreateHospitalizationSheet"})
    public void SeeHospitalizationHistory() {
        GotoMNPTab();
        Assert.assertTrue(OpenHospitalizationWL());

        if (IsPatientInHospitalization()) {
            EnterHospitalizationHistory(true);
        }
    }

    @Test(priority = 5, dependsOnMethods = {"EnterHospitalizationWL", "CreateHospitalizationSheet"})
    public void AssignDoctor() {
        GotoMNPTab();

        if (IsPatientInHospitalization()) {
            OpenActionMenu();

            WebElement asignarMedico = driver.findElement(By.id("assign_doctor"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", asignarMedico);
            WebElement filtrarMedico = driver.findElement(By.id("user-autocomplete-default-id"));
            String surnameDoctor = ConfigReader.get("surname_doctor");
            filtrarMedico.sendKeys(surnameDoctor);
            WebElement seleccionarMedico = driver.findElement(By.id("user-autocomplete-default-id-0"));
            seleccionarMedico.click();
            WebElement aceptarAsignacion = driver.findElement(By.id("accept-SearchDoctorNurseDialogComponent-button"));
            aceptarAsignacion.click();
        }
    }

    @Test(priority = 6, dependsOnMethods = {"EnterHospitalizationWL", "AdmitPatientToBed"})
    public void MoveToBed() {
        GotoMNPTab();

        if (IsPatientInHospitalization()) {
            OpenActionMenu();

            WaitAMomentPlease();
            WebElement movePatientToBed = wait.until(ExpectedConditions.elementToBeClickable(By.id("move_patient")));
            movePatientToBed.click();

            WaitAMomentPlease();
            AssignBed();

            WaitAMomentPlease();
            WebElement aceptarNuevaCama = driver.findElement(By.id("accept-TransferPatientComponent-button"));
            aceptarNuevaCama.click();
        }
    }

    @Test(priority = 7, dependsOnMethods = {"EnterHospitalizationWL", "SeeHospitalizationHistory"})
    public void CreateHospitalizationAnamnesis() {
        if (IsMNPTabActive()) {
            Assert.assertTrue(OpenHospitalizationWL());
            if (IsPatientInHospitalization()) {
                EnterHospitalizationHistory(false);
            }
        }

        WebElement moduloAnamnesis = driver.findElement(By.id("anamnesis-sidebar"));
        moduloAnamnesis.click();

        By anamnesisForm = By.xpath(
                "//gc-dynamic-anamnesis-grid//mat-form-field//input | " +
                        "//gc-dynamic-anamnesis-grid//mat-form-field//textarea | " +
                        "//gc-dynamic-anamnesis-grid//mat-form-field//mat-select | " +
                        "//gc-dynamic-anamnesis-grid//mat-form-field//div[@contenteditable='true']"
        );

        if (IsFormEnabled(anamnesisForm)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = LocalDateTime.now().format(formatter);

            // Localizar todos los campos dentro de formulario
            List<WebElement> fields = driver.findElements(anamnesisForm);

            int counter = 1;
            for (WebElement field : fields) {
                String tag = field.getTagName();
                String classes = field.getAttribute("class");
                boolean isContentEditable = "true".equalsIgnoreCase(field.getAttribute("contenteditable"));

                if ("textarea".equalsIgnoreCase(tag)) {
                    clearAndType(field, "Texto de prueba " + counter, isContentEditable);
                } else if ("input".equalsIgnoreCase(tag)) {
                    boolean isMatDatepicker = (classes != null && classes.contains("mat-datepicker-input"));
                    isContentEditable = (classes != null && classes.contains("angular-editor-textarea"));
                    if (isMatDatepicker) {
                        field.click();
                    } else {
                        clearAndType(field, "Valor genérico " + counter, isContentEditable);
                    }
                } else if (isContentEditable) { // Caso editor Angular con contenteditable
                    clearAndType(field, "Texto editable " + counter, isContentEditable);
                } else { // Caso desplegable (mat-select)
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", field);
                    By panel = By.cssSelector("div.mat-select-panel");
                    wait.until(ExpectedConditions.visibilityOfElementLocated(panel));
                    WebElement primeraOpcion = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.mat-select-panel mat-option:first-child")));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", primeraOpcion);
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(panel));
                }
                counter++;
            }

            //Aceptar anamnesis
            WebElement aceptarAnamnesis = driver.findElement(By.id("acceptWidgetButton"));
            aceptarAnamnesis.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("acceptWidgetButton")));
        } else {
            Reporter.log("⚠ La anamnesis está en modo lectura.");
        }
    }

    @Test(priority = 8, dependsOnMethods = {"EnterHospitalizationWL", "SeeHospitalizationHistory", "CreateHospitalizationAnamnesis"})
    public void CreateHospitalizationEvolution() {

        if (IsMNPTabActive()) {
            Assert.assertTrue(OpenHospitalizationWL());
            if (IsPatientInHospitalization()) {
                EnterHospitalizationHistory(false);
            }
        }
        WaitAMomentPlease();

        //Abrir módulo de curso evolutivo
        WebElement moduloEvolucion = driver.findElement(By.id("evolutionary_course-sidebar"));
        moduloEvolucion.click();

        //Crear Curso evolutivo
        WebElement accionesEvolucion = driver.findElement(By.id("actions-button"));
        accionesEvolucion.click();

        WebElement crearEvolucion = driver.findElement(By.id("new"));
        crearEvolucion.click();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String fechaHora = LocalDateTime.now().format(formatter);
        WebElement editor = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.angular-editor-textarea")));
        editor.sendKeys("Texto de evolución de Selenium " + fechaHora);

        WebElement aceptarEvolucion = driver.findElement(By.id("accept-EvolutionaryCourseDialogComponent-button"));
        aceptarEvolucion.click();
        WaitAMomentPlease();
    }

    @Test(priority = 9, dependsOnMethods = {"EnterHospitalizationWL", "SeeHospitalizationHistory"})
    public void DichargeHospitalization() {
        if (IsMNPTabActive()) {
            Assert.assertTrue(OpenHospitalizationWL());
            if (IsPatientInHospitalization()) {
                EnterHospitalizationHistory(false);
            }
        }

        WaitAMomentPlease();

        WebElement informeAlta = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("discharge_report-sidebar")));
        informeAlta.click();

        if (!IsDischargeReportSigned()) {

            WebElement fechaAlta = driver.findElement(By.id("dischargedDate"));
            fechaAlta.click();
            // Selector robusto (gc-time input) en vez del xpath absoluto original —
            // mismo componente compartido ya verificado en EmergencyWLTest.DichargeEmergency().
            WebElement horaAlta = driver.findElement(By.cssSelector("gc-time input"));
            horaAlta.click();
            WebElement destinoAlta = driver.findElement(By.id("dischargedDestinationId"));
            destinoAlta.click();
            WebElement seleccionarDestino = driver.findElement(By.id("dischargedDestinationId-0"));
            seleccionarDestino.click();
            WebElement editor = driver.findElement(By.cssSelector("div.angular-editor-textarea[contenteditable='true']"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = LocalDateTime.now().format(formatter);
            editor.sendKeys("Alta a domicilio escrita desde Selenium " + fechaHora);

            Sign(ConfigReader.get("password_doctor"));
            WaitAMomentPlease();

            CloseMPTab();
            GotoMNPTab();
        } else {
            Reporter.log("⚠ El informe ya está firmado.");
        }
    }

    @Test(priority = 10, dependsOnMethods = {"EnterHospitalizationWL", "SeeHospitalizationHistory"})
    public void NurseDischargeHospitalization() {
        Logout();
        LoginAsNurse();

        if (IsMNPTabActive()) {
            Assert.assertTrue(OpenHospitalizationWL());
            if (IsPatientInHospitalization()) {
                EnterHospitalizationHistory(false);
            }
        }

        WaitAMomentPlease(3);

        WebElement altaEnfermeria = driver.findElement(By.id("discharge_report-sidebar"));
        altaEnfermeria.click();

        if (!IsDischargeReportSigned()) {

            WebElement editor = driver.findElement(By.cssSelector("div.angular-editor-textarea[contenteditable='true']"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = LocalDateTime.now().format(formatter);
            editor.sendKeys("Alta de enfermería escrita desde Selenium " + fechaHora);

            Sign(ConfigReader.get("password_nurse"));
            WaitAMomentPlease();

            CloseMPTab();
            GotoMNPTab();
        } else {
            Reporter.log("⚠ El informe ya está firmado.");
        }
    }

    @Test(priority = 11, dependsOnMethods = {"EnterHospitalizationWL", "DichargeHospitalization", "NurseDischargeHospitalization"})
    public void DischargeHospitalizationFromWL() {
        Assert.assertTrue(OpenHospitalizationWL());

        if (IsPatientInHospitalization()) {
            OpenActionMenu();

            WaitAMomentPlease();
            WebElement liberarHospitalizacion = driver.findElement(By.id("free_bed"));

            if (liberarHospitalizacion.isEnabled()) {
                liberarHospitalizacion.click();

                WebElement aceptarLiberacion = driver.findElement(By.id("alert-confirm"));
                aceptarLiberacion.click();

                WaitAMomentPlease();
            } else {
                Reporter.log("⚠ La cama no puede ser liberada porque la opción está deshabilitada.");
            }
        } else {
            Reporter.log("El paciente no está hospitalizado.");
        }
    }

    // Renombrado desde "RemoveSignatureDichargeEmergency" (nombre copiado por error
    // del original de Jordi). Sin @Test, igual que su equivalente en EmergencyWLTest —
    // pendiente de validar en entorno real antes de activarlo.
    // @Test(priority = 101, dependsOnMethods = {"EnterHospitalizationWL", "DichargeHospitalization"})
    public void RemoveSignatureDichargeHospitalization() {
        if (IsMNPTabActive()) {
            Assert.assertTrue(OpenHospitalizationWL());
            if (IsPatientInHospitalization()) {
                EnterHospitalizationHistory(false);
            }
        }
        WaitAMomentPlease();

        WebElement informeAlta = driver.findElement(By.id("discharge_report-sidebar"));
        informeAlta.click();

        if (IsDischargeReportSigned()) {
            WebElement acciones = driver.findElement(By.id("discharge-report-actions-button"));
            acciones.click();

            WebElement removeSignature = driver.findElement(By.id("discharge-report-remove-signature-doctor"));
            removeSignature.click();

            WebElement password = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));
            password.sendKeys(ConfigReader.get("password_doctor"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = LocalDateTime.now().format(formatter);
            WebElement reason = driver.findElement(By.id("reason"));
            reason.sendKeys("Firma quitada por Selenium " + fechaHora);

            WebElement boton = driver.findElement(By.id("remove_signature-RemoveSignatureComponent-button"));
            boton.click();
        } else {
            Reporter.log("⚠ El informe NO está firmado.");
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // métodos auxiliares
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean OpenHospitalizationWL() {
        goToMainMenu();

        WebElement moduloHospitalizacion = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("hospitalization-sidebar")));

        if (moduloHospitalizacion.isDisplayed()) {
            moduloHospitalizacion.click();

            WebElement worklistHospitalizacionMenu = driver.findElement(By.id("worklist-sidebar"));
            worklistHospitalizacionMenu.click();

            WebElement hospitalizationList = driver.findElement(By.tagName("gc-hospitalization-list"));
            hospitalizationList.click();

            WaitAMomentPlease();

            return true;
        } else {
            return false;
        }
    }

    public boolean IsPatientInHospitalization() {
        // Cerrar cualquier menú u overlay abierto antes de interactuar con la worklist
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();

        WebElement filtroPaciente = driver.findElement(By.id("filter-input"));
        filtroPaciente.clear();
        TakeOffHospitalizationFilters();
        WaitAMomentPlease();

        String NH = ConfigReader.get("pacqah1NH");
        filtroPaciente.sendKeys(NH);
        WaitAMomentPlease();
        boolean isPatientInHospitalization;

        try {
            List<WebElement> filas = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("[id^='gridId-']")
            ));

            if (!filas.isEmpty()) {
                WebElement primeraFila = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id^='gridId-']")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", primeraFila);
                isPatientInHospitalization = true;
            } else {
                isPatientInHospitalization = false;
            }

        } catch (TimeoutException e) {
            isPatientInHospitalization = false;
        }
        WaitAMomentPlease();
        return isPatientInHospitalization;
    }

    public void TakeOffHospitalizationFilters() {
        List<WebElement> backdrops = driver.findElements(By.cssSelector(".cdk-overlay-backdrop-showing"));
        if (!backdrops.isEmpty()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop-showing")));
        }

        WebElement filtroWL = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("filters-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filtroWL);

        WebElement withoutFilter = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("hospitalization-filter-not-filtered")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", withoutFilter);
    }

    public boolean OpenEmergencyWL() {
        goToMainMenu();

        WebElement moduloUrgencias = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("emergency-sidebar")));

        if (moduloUrgencias.isDisplayed()) {
            moduloUrgencias.click();

            WebElement worklistUrgenciasMenu = driver.findElement(By.id("worklist-sidebar"));
            worklistUrgenciasMenu.click();

            WebElement emergencyList = driver.findElement(By.tagName("gc-emergency-list"));
            emergencyList.click();

            WaitAMomentPlease();

            return true;
        } else {
            return false;
        }
    }

    public boolean IsPatientInEmergency() {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();

        WebElement filtroPaciente = driver.findElement(By.id("filter-input"));
        filtroPaciente.clear();
        TakeOffEmergencyFilters();
        WaitAMomentPlease();

        String NH = ConfigReader.get("pacqah1NH");
        filtroPaciente.sendKeys(NH);
        WaitAMomentPlease();
        boolean isPatientInEmergency;

        try {
            List<WebElement> filas = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("[id^='emergencyGridId-']")
            ));

            if (!filas.isEmpty()) {
                WebElement primeraFila = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id^='emergencyGridId-']")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", primeraFila);
                isPatientInEmergency = true;
            } else {
                isPatientInEmergency = false;
            }

        } catch (TimeoutException e) {
            isPatientInEmergency = false;
        }
        WaitAMomentPlease();
        return isPatientInEmergency;
    }

    public void TakeOffEmergencyFilters() {
        List<WebElement> backdrops = driver.findElements(By.cssSelector(".cdk-overlay-backdrop-showing"));
        if (!backdrops.isEmpty()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop-showing")));
        }

        WebElement filtroWL = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("filters-button-emergencyGridId")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filtroWL);

        WebElement withoutFilter = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("filter-not-filtered")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", withoutFilter);
    }

    public boolean OpenPatientAdmission() {
        goToMainMenu();

        WebElement moduloAdmision = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("admission-sidebar")));

        if (moduloAdmision.isDisplayed()) {
            moduloAdmision.click();

            WebElement ingresoPaciente = driver.findElement(By.id("patient_admission-sidebar"));
            ingresoPaciente.click();

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("floors")));

            WaitAMomentPlease();

            return true;
        } else {
            return false;
        }
    }

    private void OpenActionMenu() {
        List<WebElement> backdrops = driver.findElements(By.cssSelector(".cdk-overlay-backdrop-showing"));
        if (!backdrops.isEmpty()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop-showing")));
        }
        WebElement accionesHospitalizacion = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("actions-button")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", accionesHospitalizacion);
        WaitAMomentPlease();
    }

    public void CloseActionMenu() {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop-showing")));
    }

    public void EnterHospitalizationHistory(boolean closeTab) {

        int nTabs = TabOpenedCount();

        OpenActionMenu();

        WebElement verHistoria = driver.findElement(By.id("see_history"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", verHistoria);
        if (closeTab) {
            CloseMPTab();
        } else {
            WaitAMomentPlease();
            if (nTabs != TabOpenedCount()) {
                SwitchToTab(GetLastTabOpened());
            }
        }
    }

    public void AssignBed() {
        WebElement cama = driver.findElement(By.id("bed"));
        String camaActual = cama.getText();
        cama.click();

        WebElement nuevaCama = driver.findElement(By.id("bed-0"));
        if (!camaActual.equals(nuevaCama.getText())) {
            nuevaCama.click();
        } else {
            nuevaCama = driver.findElement(By.id("bed-1"));
            nuevaCama.click();
        }
    }

    public void LocateFreeBed() {
        WebElement plantas = driver.findElement(
                By.xpath("//a[contains(@class,'enable') and normalize-space()='Todas']")
        );
        plantas.click();

        List<WebElement> desplegables = driver.findElements(
                By.cssSelector("mat-icon[fonticon='fg-angle-down']")
        );

        for (WebElement desplegable : desplegables) {
            desplegable.click();

            try {
                WebElement opcion = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//button[@mat-menu-item and @title=' Ingresar']")
                        )
                );

                String ariaDisabled = opcion.getAttribute("aria-disabled");

                if ("false".equals(ariaDisabled)) {
                    return;
                }
            } catch (TimeoutException e) {
                Reporter.log("Fallo al abrir el desplegable de planta.");
            }
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        }
        Reporter.log("⚠ No hay camas libres.");
    }
}
