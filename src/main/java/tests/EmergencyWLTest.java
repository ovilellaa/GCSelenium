package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmergencyWLTest extends ClassBaseTest {

    @Test(priority = 1)
    public void EnterEmergencyWL() {
        LoginAsDoctor();

        Assert.assertTrue(OpenEmergencyWL());
    }

    @Test(priority = 2, dependsOnMethods = {"EnterEmergencyWL"})
    public void CreateEmergencySheet() {
        GotoMNPTab();

        // comprobamos si el paciente está en urgencias antes de intentar crearle una hoja de urgencias
        if (!IsPatientInEmergency()) {
            OpenNoContextualMNPActions();

            WebElement crearUrgencia = driver.findElement(By.id("create_emergency_sheet"));
            crearUrgencia.click();

            //Busca y selecciona al paciente
            WebElement paciente = driver.findElement(By.id("patient"));
            String NIF = ConfigReader.get("pacqah1NIF");
            paciente.sendKeys(NIF);
            WebElement seleccionarPaciente = driver.findElement(By.id("patient-0"));
            seleccionarPaciente.click();

            // comprobamos si se abre la pantalla de ficha paciente por si el centro lo tiene configurado así
            try {
                // Puedes ajustar el tiempo de espera si usas WebDriverWait
                WebElement cancelPatienFileButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cancel-PatientFileContainer-button")));
                if (cancelPatienFileButton.isDisplayed())
                    cancelPatienFileButton.click();
            } catch (TimeoutException e) {
            }

            WaitAMomentPlease();

            //Selecciona Tipo de remisión Voluntario
            WebElement remision = driver.findElement(By.id("referredById"));
            remision.click();
            WebElement remisionVoluntaria = driver.findElement(By.id("referredById-0"));
            remisionVoluntaria.click();

            //Selecciona el centro de remisión
            WebElement centro = driver.findElement(By.id("sourceCenterId"));
            centro.click();
            WebElement primerCentro = driver.findElement(By.id("sourceCenterId-0"));
            primerCentro.click();

            //Selecciona el Tipo de consulta
            WebElement area = driver.findElement(By.id("areaId"));
            area.click();
            WebElement primeraArea = driver.findElement(By.id("areaId-0"));
            primeraArea.click();

            WaitAMomentPlease(2);

            WebElement telefono = driver.findElement(By.id("phone"));
            String valorTelefono = telefono.getAttribute("value");

            if (valorTelefono != null && !valorTelefono.isEmpty()) {
                telefono.clear();  // borro el valor para asegurarme que saltará ventana de aviso
            }

            //Acepta el formulario
            WebElement aceptarCrearUrgencia = driver.findElement(By.id("accept-EmergencySheetContainer-button"));
            aceptarCrearUrgencia.click();

            WebElement botonConfirmarSinTelefono = driver.findElement(By.id("alert-confirm"));
            botonConfirmarSinTelefono.click();

            WaitAMomentPlease();

            List<WebElement> botonesDialogo = driver.findElements(By.id("alert-cancel"));
            if (!botonesDialogo.isEmpty()) {
                botonesDialogo.getFirst().click();
            }


        } else {
            Reporter.log("⚠ El paciente ya está en urgencias.");
        }
    }

    @Test(priority = 3, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void SeeEmergencyHistory() {
        if (IsPatientInEmergency()) {
            EnterEmergencyHistory(true);
        }
    }


    @Test(priority = 4, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void AttendTriage() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
            OpenActionMenu();

            WebElement verTriaje = driver.findElement(By.id("view_triage"));

            if (!verTriaje.isEnabled()) {
                WebElement atenderTriaje = driver.findElement(By.id("attend_triage"));
                atenderTriaje.click();
                WebElement diagramaTriaje = driver.findElement(By.id("triageDiagram"));
                diagramaTriaje.click();
                WebElement seleccionarDiagrama = driver.findElement(By.id("triageDiagram-0"));
                seleccionarDiagrama.click();
                WebElement tipoUrgencia = driver.findElement(By.id("emergencyType"));
                tipoUrgencia.click();
                WebElement seleccionarTipoUrgencia = driver.findElement(By.id("emergencyType-0"));
                seleccionarTipoUrgencia.click();

                WaitAMomentPlease();

                WebElement marcarCuestionario = driver.findElement(By.id("0"));
                marcarCuestionario.click();
                WebElement aceptarTriaje = driver.findElement(By.id("select-TriageContainer-button"));
                aceptarTriaje.click();
                CloseActionsMenuWL();
            } else {
                CloseActionsMenuWL();
                Reporter.log("⚠ El paciente ya tiene el triaje hecho.");
            }
        }
    }


    @Test(priority = 5, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet", "AttendTriage"})
    public void SeeTriage() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {

            OpenActionMenu();
            WebElement verTriaje = driver.findElement(By.id("view_triage"));


            if (verTriaje.isEnabled()) {
                verTriaje.click();

                WaitAMomentPlease();

                WebElement dialogoTriaje = wait.until(ExpectedConditions.elementToBeClickable(By.tagName("gc-triage")));

                Assert.assertTrue(dialogoTriaje.isDisplayed());

                if (dialogoTriaje.isDisplayed()) {
                    WebElement cancelarTriaje = driver.findElement(By.id("cancel-TriageContainer-button"));
                    cancelarTriaje.click();
                }
            } else {
                CloseActionsMenuWL();
                Reporter.log("⚠ El paciente no tiene el triaje hecho.");
            }
        }
    }

    @Test(priority = 6, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void AssignDoctor() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
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

    @Test(priority = 6, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void AssignNurse() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
            OpenActionMenu();

            List<WebElement> asignarEnfermeriaList = driver.findElements(By.id("assign_nurse"));

            if (!asignarEnfermeriaList.isEmpty()) {
                WebElement asignarEnfermeria = driver.findElement(By.id("assign_nurse"));
                asignarEnfermeria.click();
                WebElement filtrarMedico = driver.findElement(By.id("user-autocomplete-default-id"));
                String surnameNurse = ConfigReader.get("surname_nurse");
                filtrarMedico.sendKeys(surnameNurse);
                WebElement seleccionarEnfermera = driver.findElement(By.id("user-autocomplete-default-id-0"));
                seleccionarEnfermera.click();
                WebElement aceptarAsignacion = driver.findElement(By.id("accept-SearchDoctorNurseDialogComponent-button"));
                aceptarAsignacion.click();
            } else {
                CloseActionMenu(); // cerramos el menu de acciones que esta abierto
                Reporter.log("⚠ El botón de asignar enfermería NO está presente (posible falta de permisos).");
            }

        }
    }

    @Test(priority = 7, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void ChangePriority() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
            OpenActionMenu();

            Actions actions = new Actions(driver);
            WebElement cambiarPrioridad = driver.findElement(By.id("change_priority"));
            actions.moveToElement(cambiarPrioridad).perform();
            WebElement prioridadLeve = driver.findElement(By.id("change-priority-3"));
            prioridadLeve.click();
        }
    }

    @Test(priority = 8, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void ChangeEmergencyType() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
            OpenActionMenu();

            Actions actions = new Actions(driver);
            WebElement cambiarTipo = driver.findElement(By.id("change_emergency_type"));
            actions.moveToElement(cambiarTipo).perform();
            WebElement tipoEmergencia = driver.findElement(By.id("change-emergency-type-1"));
            tipoEmergencia.click();

        }
    }

    @Test(priority = 9, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void AttendEmergency() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
            OpenActionMenu();

            WaitAMomentPlease();
            WebElement atenderUrgencias = driver.findElement(By.id("attend_emergency"));

            if (atenderUrgencias.isEnabled()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", atenderUrgencias);

                AssingBox(0);
                //   WebElement aceptarAsignacion = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("continue-EmergencyTransferContainer-button")));
                //    WebElement aceptarAsignacion = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("accept-EmergencyTransferContainer-button")));
                //  aceptarAsignacion.click();

                // Buscar el botón "accept"
                List<WebElement> acceptButtons = driver.findElements(
                        By.id("accept-EmergencyTransferContainer-button")
                );

                if (!acceptButtons.isEmpty()) {
                    acceptButtons.getFirst().click();
                } else {
                    // Si no está, buscar el botón "continue"
                    List<WebElement> continueButtons = driver.findElements(
                            By.id("continue-EmergencyTransferContainer-button")
                    );
                    if (!continueButtons.isEmpty()) {
                        continueButtons.getFirst().click();
                    }
                }

                CloseMPTab();
                GotoMNPTab();
            } else {
                Reporter.log("⚠ El paciente no puede ser atendido porque no está habilitada la opción. Revise permisos del usuario logado.");
            }
        }
    }

    @Test(priority = 10, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet", "AttendEmergency"})
    public void MoveToBox() {
        GotoMNPTab();

        if (IsPatientInEmergency()) {
            OpenActionMenu();

            WaitAMomentPlease();
            WebElement movePatientToBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("move_patient")));
            movePatientToBox.click();

            AssingBox(1);

            WebElement aceptarAsignacion = wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-EmergencyTransferContainer-button")));
            aceptarAsignacion.click();
            CloseActionMenu(); // cerramos el menu de acciones que esta abierto
        }

    }


    @Test(priority = 11, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void CreateEmergencyAnamnesis() {
        if (IsMNPTabActive()) {
            if (IsPatientInEmergency()) {
                EnterEmergencyHistory(false);
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
                    field.click();
                    By panel = By.cssSelector("div.mat-select-panel");
                    wait.until(ExpectedConditions.visibilityOfElementLocated(panel));
                    WebElement primeraOpcion = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.mat-select-panel mat-option:first-child")));
                    primeraOpcion.click();
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
        // CloseMPTab();
    }

    @Test(priority = 12, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet", "CreateEmergencyAnamnesis"})
    public void CreateEmergencyEvolution() {

        if (IsMNPTabActive()) {
            if (IsPatientInEmergency()) {
                EnterEmergencyHistory(false);
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
        // }
    }


    @Test(priority = 100, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet"})
    public void DichargeEmergency() {
        if (IsMNPTabActive()) {
            if (IsPatientInEmergency()) {
                EnterEmergencyHistory(false);
            }
        }

        WaitAMomentPlease();

        WebElement informeAlta = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("discharge_report-sidebar")));
        informeAlta.click();

        if (!IsDischargeReportSigned()) {

            WebElement fechaAlta = driver.findElement(By.id("dischargedDate"));
            fechaAlta.click();
            WebElement horaAlta = driver.findElement(By.cssSelector("gc-time input"));
            horaAlta.click();
            WebElement destinoAlta = driver.findElement(By.id("dischargedDestinationId"));
            destinoAlta.click();
            WebElement seleccionarDestino = driver.findElement(By.id("dischargedDestinationId-0"));
            seleccionarDestino.click();
            //WebElement escribirAlta = driver.findElement(By.xpath("/html/body/gc-root/gc-home/div/gc-sidebar/mat-sidenav-container/mat-sidenav-content/gc-discharge-report/div/mat-drawer-container/mat-drawer-content/div/gc-body-content/section/div/div/div/form/div/div[2]/div/gc-html-editor/mat-form-field/div/div[1]/div/angular-editor/div/div/div"));
            WebElement editor = driver.findElement(By.cssSelector("div.angular-editor-textarea[contenteditable='true']"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String fechaHora = LocalDateTime.now().format(formatter);
            editor.sendKeys("Alta a domicilio escrita desde Selenium " + fechaHora);

            Sign(ConfigReader.get("password_doctor"));
            WaitAMomentPlease();

        } else {
            Reporter.log("⚠ El informe ya está firmado.");

        }

        CloseMPTab();
        GotoMNPTab();
        GotoMNPTab();

    }

    @Test(priority = 110, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet", "DichargeEmergency"})
    public void DischargeEmergencyFromWL() {
        Logout();
        LoginAsNurse();

        OpenEmergencyWL();


        if (IsPatientInEmergency()) {
            OpenActionMenu();

            WaitAMomentPlease();
            WebElement liberarUrgencia = driver.findElement(By.id("release_emergency"));

            if (liberarUrgencia.isEnabled()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", liberarUrgencia);

                WebElement aceptarLiberacion = driver.findElement(By.id("alert-confirm"));
                aceptarLiberacion.click();

                WaitAMomentPlease();
            } else {
                Reporter.log("⚠  La urgencia no puede ser liberada porque la opción está deshabilitada");
            }
        } else {
            Reporter.log("El paciente no está en urgencias.");
        }
    }

    // @Test(priority = 101, dependsOnMethods = {"EnterEmergencyWL", "CreateEmergencySheet", "AttendEmergency", "DichargeEmergency"})
    public void RemoveSignatureDichargeEmergency() {
        if (IsMNPTabActive()) {
            if (IsPatientInEmergency()) {
                EnterEmergencyHistory(false);
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
    public boolean OpenEmergencyWL() {
        //Acceder al módulo de Urgencias
        WebElement moduloUrgencias = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("emergency-sidebar")));

        if (moduloUrgencias.isDisplayed()) {
            moduloUrgencias.click();

            //Acceder a la WL de Urgencias
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
        // Cerrar cualquier menú u overlay abierto antes de interactuar con la worklist
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();

        WebElement filtroPaciente = driver.findElement(By.id("filter-input"));
        filtroPaciente.clear();
        TakeOffServerFilters();
        WaitAMomentPlease();

        String NH = ConfigReader.get("pacqah1NH");
        filtroPaciente.sendKeys(NH);
        WaitAMomentPlease();
        boolean isPatientInEmergency;

        try {
            // Esperar a que haya al menos una fila en el grid
            List<WebElement> filas = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                    By.cssSelector("[id^='emergencyGridId-']") // cualquier fila cuyo id empiece por emergencyGridId-
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


    public void TakeOffServerFilters() {
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

    private void OpenActionMenu() {
        List<WebElement> backdrops = driver.findElements(By.cssSelector(".cdk-overlay-backdrop-showing"));
        if (!backdrops.isEmpty()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop-showing")));
        }
        WebElement accionesUrgencias = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("actions-button-emergencyGridId")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", accionesUrgencias);
        WaitAMomentPlease();
    }

    public void CloseActionMenu() {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop-showing")));
    }

    public boolean IsBoxFree(int nBox) {
        // XPath del icono en la primera fila
        By iconLocator = By.xpath("//*[@id='grid-gridId-" + nBox + "-customer']/div/i");

        // Inicializa el booleano
        boolean iconEsperadoPresente = false;

        try {
            WebElement icono = driver.findElement(iconLocator);
            String clases = icono.getAttribute("class");

            // Verifica si contiene todas las clases esperadas
            if (clases.contains("fg") &&
                    clases.contains("fg-circle") &&
                    clases.contains("status-icon") &&
                    clases.contains("ng-star-inserted")) {
                iconEsperadoPresente = true;
            }

        } catch (NoSuchElementException e) {
        }

        return iconEsperadoPresente;
    }

    public void AssingBox(int nBox) {
        List<WebElement> listButtons = driver.findElements(By.id("list-button"));
        if (listButtons.isEmpty()) {
            // El paciente ya tiene un box asignado, no es necesario seleccionar uno
            return;
        }

        listButtons.getFirst().click();

        boolean estaLibre = IsBoxFree(nBox);

        WebElement consulta1 = driver.findElement(By.id("gridId-" + nBox));
        consulta1.click();
        WebElement accionesMover = driver.findElement(By.id("menu-actions-button"));
        accionesMover.click();
        WebElement asignarConsulta = wait.until(ExpectedConditions.elementToBeClickable(By.id("assign")));
        asignarConsulta.click();

        if (!estaLibre) {
            WebElement aceptarReemplazar = driver.findElement(By.id("alert-button1"));
            aceptarReemplazar.click();
        }
    }

    public void EnterEmergencyHistory(boolean closeTab) {

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
}

