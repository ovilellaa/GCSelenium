package tests;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.util.List;

public class ModulesMNPTest extends ClassBaseTest {


    @Test(priority = 1)
    public void Login() {
       LoginAsDoctor();
    }

    @Test(priority = 2, dependsOnMethods = {"Login"})
    public void ModuleAdmission() {

        if (OpenModule(By.id("admission-sidebar"), By.id("admission-back-button"))) {
            CheckModule("Multicitas", By.id("multiappointments-sidebar"), By.tagName("gc-appointment"));
            CloseDeskPopup();
            CheckModule("Solicitudes de ingreso", By.id("admission_requests-sidebar"), By.tagName("gc-request-worklist"));
            CheckModule("Ingreso de pacientes", By.id("patient_admission-sidebar"), By.tagName("gc-patient-admissions"));
            CheckModule("Citas diferidas", By.id("deferred_appointments-sidebar"), By.tagName("gc-deferred-appointments"));
            CheckModule("Citas", By.id("day_view-sidebar"), By.tagName("gc-day-view"));
            CloseDeskPopup();
            CheckModule("Peticiones RX", By.id("xr_requests-sidebar"), By.tagName("gc-radiology-requests"));
        }

        BackToMainMenu();
    }


    @Test(priority = 10, dependsOnMethods = {"Login"})
    public void ModuleOutpatientVisits() {


        CheckModule("Consultas Externas", By.id("outpatient-sidebar"), By.tagName("gc-outpatient"));

        BackToMainMenu();
    }


    @Test(priority = 20, dependsOnMethods = {"Login"})
    public void ModuleEmergency() {

        if (OpenModule(By.id("emergency-sidebar"), By.id("emergency-back-button"))) {
            CheckModule("Lista trabajo", By.id("worklist-sidebar"), By.tagName("gc-emergency-list"));
            CheckModule("Planificador de cuidados", By.id("care_planner-sidebar"), By.cssSelector("th.mat-column-patientFullName"));
            CheckModule("Medicación", By.id("medication-sidebar"), By.tagName("gc-medication-worklist"));
        }

        BackToMainMenu();
    }

    @Test(priority = 30, dependsOnMethods = {"Login"})
    public void ModuleHospitalization() {

        if (OpenModule(By.id("hospitalization-sidebar"), By.id("emergency-back-button"))) {
            CheckModule("Lista trabajo", By.id("worklist-sidebar"), By.tagName("gc-hospitalization-list"));
            CheckModule("Planificador de cuidados", By.id("care_planner-sidebar"), By.cssSelector("th.mat-column-patientFullName"));
        //    CheckModule("Control de pruebas", By.id("test_control-sidebar"), By.id("filters-button"));
            CheckModule("Dietas", By.id("diets-sidebar"), By.tagName("gc-diets-worklist-container"));
            CheckModule("Pacientes sin dieta activa", By.id("inactive_diets-sidebar"), By.tagName("gc-inactive-diets-worklist-grid"));
            CheckModule("Mapa de Hospitalización", By.id("hospitalization_map-sidebar"), By.tagName("gc-hospitalization-map"));
            CheckModule("Medicación", By.id("medication-sidebar"), By.tagName("gc-medication-worklist"));
        }

        BackToMainMenu();
    }


    @Test(priority = 40, dependsOnMethods = {"Login"})
    public void ModuleICU() {


        CheckModule("UCI", By.id("ICU-sidebar"), By.tagName("gc-icu-worklist"));

        BackToMainMenu();
    }

    @Test(priority = 50, dependsOnMethods = {"Login"})
    public void ModuleSurgery() {

        if (OpenModule(By.id("operating_room-sidebar"), By.id("operating-room-back-button"))) {
            CheckModule("Lista trabajo", By.id("worklist-sidebar"), By.tagName("gc-operating-room-worklist"));
            CheckModule("Planificador", By.id("scheduler-sidebar"), By.tagName("gc-operating-room-scheduler"));
            CheckModule("Buscador de intervenciones", By.id("interventions_search-sidebar"), By.tagName("gc-operating-room-interventions"));
            CheckModule("Buscador de implantes", By.id("implants_search-sidebar"), By.tagName("gc-operating-room-implants"));
        }

        BackToMainMenu();
    }


    @Test(priority = 60, dependsOnMethods = {"Login"})
    public void ModuleDiagnosticWorkup() {

        if (OpenModule(By.id("diagnostic_means-sidebar"), By.id("diagnostic-means-back-button"))) {
            if (OpenModule(By.id("diagnostic_imaging-sidebar"), By.id("diagnostic-imaging-back-button"))) {
                CheckModule("Lista trabajo Rx", By.id("worklist-sidebar"), By.tagName("gc-operating-room-worklist"));
                CheckModule("Buscar Rx", By.id("search-sidebar"), By.tagName("gc-radiology-requests"));
                CheckModule("Plantillas Rx", By.id("worklist-sidebar"), By.tagName("gc-templates"));

                BackTo(By.id("diagnostic-imaging-back-button"));
            }
            CheckModule("Anatomía Patológica", By.id("pathological_anatomy-sidebar"), By.tagName("gc-pathological-anatomy-worklist"));
            BackTo(By.id("pathological-anatomy-back-button"));
            CheckModule("Otras pruebas", By.id("other_tests-sidebar"), By.tagName("gc-other-test-worklist"));
            CheckModule("Laboratorio", By.id("laboratory-sidebar"), By.tagName("gc-lab-worklist"));
            CheckModule("Endoscopias", By.id("endoscopy-sidebar"), By.tagName("gc-endoscopie-worklist"));
        }

        BackToMainMenu();
    }

    @Test(priority = 70, dependsOnMethods = {"Login"})
    public void ModuleMaternity() {

        if (OpenModule(By.id("maternity-sidebar"), By.id("maternity-back-button"))) {

            CheckModule("Lista de trabajo", By.id("worklist-sidebar"), By.tagName("gc-maternity-worklist"));
            CheckModule("Libro de partos", By.id("birth_book-sidebar"), By.tagName("gc-maternity-birth-book"));

        }

        BackToMainMenu();
    }


    @Test(priority = 80, dependsOnMethods = {"Login"})
    public void ModuleDayHospital() {

        CheckModule("Hospital de día", By.id("day_hospital-sidebar"), By.tagName("gc-day-hospital-worklist"));

        BackToMainMenu();
    }

    @Test(priority = 90, dependsOnMethods = {"Login"})
    public void ModuleHemodialysis() {

        CheckModule("Hemodiálisis", By.id("hemodialysis-sidebar"), By.tagName("gc-hemodialysis-worklist"));

        BackToMainMenu();
    }


    @Test(priority = 100, dependsOnMethods = {"Login"})
    public void ModulePharmacy() {

        if (OpenModule(By.id("pharmacy-sidebar"), By.id("pharmacy-back-button"))) {
            CheckModule("Validación", By.id("validation-sidebar"), By.tagName("gc-pharmacy-validation"));
            CheckModule("Carro unidosis", By.id("unidosis_carts-sidebar"), By.tagName("gc-unidosis-cart-worklist"));
            CheckModule("Perfil farmacoterapeútico", By.id("pharmacy_pharmacotherapeutic_profile-sidebar"), By.tagName("gc-pharma-worklist"));
            CheckModule("Guia farmacoterapeútica", By.id("pharmacotherapy_guide-sidebar"), By.tagName("gc-pharmacotherapy-guide-worklist"));
            CheckModule("Solicitudes extraordinarias", By.id("extra_products_requests-sidebar"), By.tagName("gc-extra-products-requests-worklist"));
        }

        BackToMainMenu();
    }

    @Test(priority = 110, dependsOnMethods = {"Login"})
    public void ModuleInterconsultations() {

        if (OpenModule(By.id("interconsultation_search-sidebar"), By.id("interconsultation-search-back-button"))) {
            CheckModule("Lista de trabajo", By.id("worklist-sidebar"), By.tagName("gc-interconsultation"));
        }

        BackToMainMenu();
    }


    @Test(priority = 120, dependsOnMethods = {"Login"})
    public void ModuleReporting() {

        CheckModule("Reporting", By.id("reporting-sidebar"), By.tagName("gc-reporting"));

        BackToMainMenu();
    }


    @Test(priority = 130, dependsOnMethods = {"Login"})
    public void ModuleMaintenances() {

        if (OpenModule(By.id("maintenances-sidebar"), By.id("maintenances-back-button"))) {
            CheckModule("Configuración", By.id("configuration-sidebar"), By.tagName("gc-parameters"));
            CheckModule("Agendas", By.id("schedules-sidebar"), By.tagName("gc-consultation-schedules-maintenance"));
            CheckModule("Escalas", By.id("scales-sidebar"), By.tagName("gc-scales"));
            CheckModule("Permisos", By.id("grants-sidebar"), By.tagName("gc-grants"));
            CheckModule("Notas de citación", By.id("citation_notes-sidebar"), By.tagName("gc-citation-notes"));
            CheckModule("Rangos de protocolos de creatinina", By.id("protocol_creatinine_range-sidebar"), By.tagName("gc-protocol-creatinine-range"));
            CheckModule("Factores de riesgo", By.id("personal_risk_factors-sidebar"), By.tagName("gc-personal-risk-factors"));
            CheckModule("Anamnesis", By.id("anamnesis-sidebar"), By.tagName("gc-anamnesis"));
            CheckModule("Plantillas", By.id("templates-sidebar"), By.tagName("gc-templates"));

            if (OpenModule(By.id("maintenances-sidebar"), By.id("maintenances-back-button"))
            ) {
                CheckModule("Planes de vacunación", By.id("vaccine_plans-sidebar"), By.tagName("gc-vaccine-plans"));
                CheckModule("Vacunas", By.id("vaccines-sidebar"), By.tagName("gc-vaccines"));
                CheckModule("Grupos de población", By.id("population_groups-sidebar"), By.tagName("gc-population-groups-worklist"));
                CheckModule("Zonas NADO", By.id("nado_zones-sidebar"), By.tagName("gc-nado-zones-worklist"));

                if (OpenModule(By.id("locations_parent-sidebar"), By.id("locations-parent-back-button"))
                ) {
                    CheckModule("Localizaciones", By.id("locations-sidebar"), By.tagName("gc-locations-maintenance"));
                    CheckModule("Ubicaciones", By.id("areas_maintenance-sidebar"), By.tagName("gc-areas-maintenance"));
                    CheckModule("Locales", By.id("municipalities-sidebar"), By.tagName("gc-municipalities-maintenance"));
                    BackTo(By.id("locations-parent-back-button"));
                }
                BackTo(By.id("maintenances-back-button"));
            }

            CheckModule("Maestros", By.id("masters-sidebar"), By.tagName("gc-master-tables-translations"));
            CheckModule("Usuarios", By.id("users-sidebar"), By.tagName("gc-users-worklist"));
            CheckModule("Mensajes", By.id("messages-sidebar"), By.tagName("gc-messages"));
            CheckModule("Equipos", By.id("teams-sidebar"), By.tagName("gc-teams-worklist"));
            CheckModule("Cuestionarios", By.id("questionnaires-sidebar"), By.tagName("gc-telederma-questionnaires"));

            if (OpenModule(By.id("incidents-sidebar"), By.id("incidents-back-button"))
            ) {
                CheckModule("Grupo incidentes", By.id("incident_group-sidebar"), By.tagName("gc-incident-group"));
                CheckModule("Tipo de incidente", By.id("incident_type-sidebar"), By.tagName("gc-incident-type"));
                CheckModule("Incidente", By.id("incident-sidebar"), By.tagName("gc-incident"));
                CheckModule("Grupos de trabajo", By.id("group_review-sidebar"), By.tagName("gc-group-review"));

                BackTo(By.id("incidents-back-button"));
            }

            if (OpenModule(By.id("external_codes_mapping-sidebar"), By.id("external-codes-mapping-back-button"))) {
                CheckModule("Pruebas externas", By.id("external_tests-sidebar"), By.tagName("gc-external-tests"));
                CheckModule("Colectivos externos", By.id("external_colectives-sidebar"), By.tagName("gc-external-colectives"));

                BackTo(By.id("external-codes-mapping-back-button"));
            }

            CheckModule("Roles y perfiles", By.id("profiles-and-roles-sidebar"), By.tagName("gc-profiles-and-roles"));

            if (OpenModule(By.id("pre_book_qx-sidebar"), By.id("pre-book-qx-back-button"))) {
                CheckModule("PRE reservas médico/quirófano", By.id("pre-book-qx-doc-sidebar"), By.tagName("gc-pre-book-qx-doc-worklist"));
                CheckModule("Perfiles de intervenciones", By.id("intervention_profiles-sidebar"), By.tagName("gc-intervention-profiles"));
                CheckModule("Recorsos por intervención", By.id("intervention_resources_worklist-sidebar"), By.tagName("gc-intervention-resources"));

                BackTo(By.id("pre-book-qx-back-button"));
            }

            CheckModule("Permisos para citas", By.id("appointment_permissions-sidebar"), By.tagName("gc-appointment-permissions-worklist"));
            CheckModule("Acuerdos médicos", By.id("doctor_agreements-sidebar"), By.tagName("gc-doctor-agreements"));
            CheckModule("Acciones adicionales", By.id("additional_actions-sidebar"), By.tagName("gc-additional-actions"));
            CheckModule("Autorizaciones", By.id("authorizations-sidebar"), By.tagName("gc-authorizations-maintenance"));


        }

        BackToMainMenu();
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // métodos auxiliares
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
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
                break;
            }
            backButtons.getFirst().click();
            WaitAMomentPlease();
        }
    }

    public void BackTo(By byLocatorBack) {
        List<WebElement> elements = driver.findElements(byLocatorBack);
        if (!elements.isEmpty()) {
            elements.getFirst().click();
        }
    }

    public void CloseDeskPopup() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.cdk-overlay-backdrop.cdk-overlay-backdrop-showing")));

            WebElement cancelButton = driver.findElement(By.id("cancel-DialogAppointmentOriginComponent-button"));
            cancelButton.click();
        } catch (TimeoutException | NoSuchElementException | StaleElementReferenceException e) {
        }
    }
}

