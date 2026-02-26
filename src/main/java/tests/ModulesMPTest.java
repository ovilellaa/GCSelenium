package tests;


import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.util.List;


public class ModulesMPTest extends ClassBaseTest {

    @Test(priority = 1)
    public void Login() {
        LoginAsDoctor();
    }

    @Test(priority = 10, dependsOnMethods = "Login")
    public void ViewHistory() {
        //Spotlight
        WebElement buscarPaciente = driver.findElement(By.id("search-action"));
        buscarPaciente.click();
        WebElement textoBusqueda = driver.findElement(By.id("spotlight-search"));
        textoBusqueda.sendKeys(ConfigReader.get("pacqah1NH"));

        WebElement seleccionarPaciente = driver.findElement(By.id("spotlight-list-item-0-0"));
        seleccionarPaciente.click();
        WebElement verHistoria = driver.findElement(By.id("quickAction-PATIENT_SEE_HISTORY"));
        verHistoria.click();

        WebElement seleccionarEpisodio = driver.findElement(By.id("dialog-patient-episodes-panel-0"));
        seleccionarEpisodio.click();
        WebElement aceptarEpisodio = driver.findElement(By.id("accept-PatientEpisodesContainer-button"));
        aceptarEpisodio.click();

        try {
            // pantalla de aviso de paciente que no estas a cargo
            WebElement aceptarVerEpisodio = wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-FileHistoryAccess-button")));
            aceptarVerEpisodio.click();
        } catch (Exception e) {
        }

        GoToMP();

        try {
            // pantalla modal aviso alertas
            WebElement aceptarAlertas = wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-AlertsContainer-button")));
            aceptarAlertas.click();
        } catch (Exception e) {
        }
    }

    @Test(priority = 20, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleSummary() {
        CheckModule("Resumen", By.id("summary-sidebar"), By.tagName("gc-dashboard"));
    }

    @Test(priority = 25, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleClinicalDocuments() {
        if (OpenModule(By.id("clinical_documents-sidebar"), By.id("clinical-documents-back-button"))) {
            CheckModule("Lista de trabajo", By.id("worklist-sidebar"), By.tagName("gc-clinical-documents"));

        }
        BackToMainMenu();
    }

    @Test(priority = 30, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleAnamnesis() {
        CheckModule("Anamnesis", By.id("anamnesis-sidebar"), By.tagName("gc-dynamic-anamnesis"));

    }

    @Test(priority = 40, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleEvolutionaryCourse() {
        CheckModule("Curso evolutivo", By.id("evolutionary_course-sidebar"), By.tagName("gc-evolutionary-course"));
    }

    @Test(priority = 45, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModulePharmacotherapeuticProfile() {
        if (OpenModule(By.id("pharmacotherapeutic_profile-sidebar"), By.id("pharmacotherapeutic-profile-back-button"))) {
            CheckModule("Datos generales", By.id("pp_general_data-sidebar"), By.tagName("gc-dashboard"));
            CheckModule("Errores de medicación", By.id("pp_medication_errors-sidebar"), By.tagName("gc-medication-errors"));
        }
        BackToMainMenu();
    }

    @Test(priority = 50, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleConstantsRegistry() {
        CheckModule("Registro de constantes", By.id("constant_registry-sidebar"), By.tagName("gc-patient-constants"));
    }

    @Test(priority = 60, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleBalancesRegistry() {
        CheckModule("Registro de balances", By.id("registry_of_balances-sidebar"), By.tagName("gc-water-balances"));
    }

    @Test(priority = 70, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleMedicalHistory() {
        CheckModule("Antecedentes", By.id("registry_of_balances-sidebar"), By.tagName("gc-water-balances"));
    }


    @Test(priority = 80, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleAllergiesAndAlerts() {
        CheckModule("Alergias y alertas", By.id("registry_of_balances-sidebar"), By.tagName("gc-water-balances"));
    }

    @Test(priority = 90, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleDiagnoses() {
        CheckModule("Diagnósticoss", By.id("diagnostics-sidebar"), By.tagName("gc-patient-diagnosis"));
    }

    @Test(priority = 95, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleTreatments() {
        if (OpenModule(By.id("treatments-sidebar"), By.id("treatments-back-button"))) {
            CheckModule("Medicación", By.id("medication-sidebar"), By.tagName("gc-medication"));
            CheckModule("Dietas", By.id("diets-sidebar"), By.tagName("gc-diets"));
            CheckModule("Cuidados", By.id("cares-sidebar"), By.tagName("gc-care-planner"));
            CheckModule("Oxigenoterapia", By.id("oxygen_therapy-sidebar"), By.tagName("gc-oxygen-therapy"));
            CheckModule("Recetas", By.id("medical_prescriptions-sidebar"), By.tagName("gc-medical_prescriptions"));
        }
        BackToMainMenu();
    }

    @Test(priority = 96, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleTestsMap() {
        if (OpenModule(By.id("tests_map-sidebar"), By.id("tests-map-back-button"))) {
            CheckModule("Radiología", By.id("radiology-sidebar"), By.tagName("gc-radiology"));
            CheckModule("Laboratorio", By.id("laboratory-sidebar"), By.tagName("gc-laboratory"));
            CheckModule("Anatomía patológica", By.id("pathological_anatomy-sidebar"), By.tagName("gc-anatomicpathology"));
            CheckModule("Endoscopias", By.id("endoscopies-sidebar"), By.tagName("gc-endoscopies"));
            CheckModule("Otras pruebas", By.id("other_tests-sidebar"), By.tagName("gc-other-tests"));
        }
        BackToMainMenu();
    }


    @Test(priority = 100, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleBloodProducts() {
        CheckModule("Hemoderivados", By.id("blood_products-sidebar"), By.tagName("gc-blood-products"));
    }

    @Test(priority = 110, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleSurgicalInterventions() {
        CheckModule("Intervenciones", By.id("interventions-sidebar"), By.tagName("gc-surgical-documents"));
    }

    @Test(priority = 120, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleDischargeReport() {
        CheckModule("Informe de alta", By.id("discharge_report-sidebar"), By.tagName("gc-discharge-report"));
    }

    @Test(priority = 130, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleDietsAndRecomendations() {
        CheckModule("Dietas y recomendaciones", By.id("diets_and_recommendations-sidebar"), By.tagName("gc-diets-recommendations"));
    }

    @Test(priority = 140, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleBenefits() {
        CheckModule("Prestaciones", By.id("benefits-sidebar"), By.tagName("gc-benefits"));
    }

    @Test(priority = 150, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleClinicalReports() {
        CheckModule("Informes clínicos", By.id("clinical_reports-sidebar"), By.tagName("gc-clinical-reports"));
    }

    @Test(priority = 160, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleInterconsultation() {
        CheckModule("Interconsultas", By.id("interconsultation-sidebar"), By.tagName("gc-interconsultation"));
    }

    @Test(priority = 170, dependsOnMethods = {"Login", "ViewHistory"})
    public void ModuleConsumptions() {
        CheckModule("Imputación de consumos", By.id("consumption_allocations-sidebar"), By.tagName("gc-consumption-allocations"));
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // métodos auxiliares
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void GoToMP() {

        int nTabs = TabOpenedCount();

        WaitAMomentPlease();
        if (nTabs != TabOpenedCount()) {
            SwitchToTab(GetLastTabOpened());
        }
    }


    public boolean OpenModule(By byLocatorModule, By byLocatorBack) {

        boolean isInModule = false;

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(byLocatorBack));
            isInModule = true;
        } catch (TimeoutException | NoSuchElementException | StaleElementReferenceException e) {

        }

        if (!isInModule) {
            try {
                //Acceder al módulo
                WebElement modulo = wait.until(ExpectedConditions.presenceOfElementLocated(byLocatorModule));

                if (modulo.isDisplayed()) {
                    modulo.click();
                    return true;
                } else {
                    return false;
                }
            } catch (TimeoutException | NoSuchElementException | StaleElementReferenceException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    public void CheckModule(String module, By byLocatorClick, By byLocatorCheck) {
        if (clickAndCheck(byLocatorClick, byLocatorCheck)) {
            Reporter.log(" " + module + " => OK");
        } else {
            Reporter.log("⚠ " + module + " => NO está accesible.");
        }

    }


    private boolean clickAndCheck(By byLocatorClick, By byLocatorCheck) {
        try {
            WebElement clickable = driver.findElement(byLocatorClick);
            clickable.click();

            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(byLocatorCheck));

            // Si lo encuentra y está visible → true
            return element.isDisplayed();

        } catch (TimeoutException | NoSuchElementException | StaleElementReferenceException e) {
            // Si no aparece en el tiempo dado o hay problemas con el DOM o ha dado error inesperado
            // en caso de error inesperado, intentamos cerrar el dialogo
            List<WebElement> continueButtons = driver.findElements(By.id("continue-button"));
            while (!continueButtons.isEmpty()) {
                for (WebElement button : continueButtons) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        try {
                            button.click();
                            break; // salimos después de hacer clic en el primero válido

                        } catch (Exception ex) {
                        }
                    }
                }
                continueButtons = driver.findElements(By.id("continue-button"));
            }

            return false;
        }
    }

    public void BackToMainMenu() {
        while (true) {
            List<WebElement> backButtons = driver.findElements(By.cssSelector("[id$='-back-button']"));

            if (backButtons.isEmpty()) {
                break; // No hay más botones, salimos
            }

            WebElement button = backButtons.getFirst();
            String buttonId = button.getAttribute("id");

            if ("clinical-history-back-button".equals(buttonId)) {
                break; // Encontramos el botón final, salimos
            }

            button.click();
            WaitAMomentPlease();
        }

    }

}
