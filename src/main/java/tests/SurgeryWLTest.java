package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

import java.util.List;

public class SurgeryWLTest extends ClassBaseTest {

    @Test(priority = 1)
    public void EnterSurgeryWL() {
        LoginAsAdministrative();

        Assert.assertTrue(OpenSurgeryWL());
    }

    @Test(priority = 2, dependsOnMethods = {"EnterSurgeryWL"})
    public void CreateOperatingSheet() {

        if (!IsPatientInSurgeryWL()) {

            OpenNoContextualMNPActions();

            WebElement crearIntervencion = driver.findElement(By.id("intervention_proposal"));
            crearIntervencion.click();

            //Busca y selecciona al paciente
            WebElement paciente = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("patient")));
            String NIF = ConfigReader.get("pacqah1NIF");
            paciente.sendKeys(NIF);
            WebElement seleccionarPaciente = wait.until(ExpectedConditions.elementToBeClickable(By.id("patient-0")));
            seleccionarPaciente.click();

            WebElement tipoIntervencion = driver.findElement(By.id("interventionType"));
            tipoIntervencion.click();
            WebElement seleccionarTipoIntervencion = driver.findElement(By.id("interventionType-0"));
            seleccionarTipoIntervencion.click();

            //Selecciona Prioridad de intervención
            WebElement prioridadIntervencion = driver.findElement(By.id("intervention-proposal-intervention-priority"));
            prioridadIntervencion.click();
            WebElement seleccionarPrioridadIntervencion = driver.findElement(By.id("intervention-proposal-intervention-priority-0"));
            seleccionarPrioridadIntervencion.click();



            //Rellena la duración de la intervención
            WebElement duracionIntervencion = driver.findElement(By.id("surgeryDurationTime"));
            duracionIntervencion.sendKeys("30m");

            WebElement tecnicaPrevista = driver.findElement(By.id("predictedTechniques"));
            tecnicaPrevista.sendKeys("Técnica escrita por Selenium");

            WebElement aceptarCrearPropuesta = wait.until(ExpectedConditions.elementToBeClickable(By.id("accept-InterventionProposalContainer-button")));
            aceptarCrearPropuesta.click();

            WebElement dialogClose = wait.until(ExpectedConditions.elementToBeClickable(By.id("close-HeavyProcessLoaderDialogComponent-button")));
            dialogClose.click();


        } else {
            Reporter.log("⚠ El paciente ya está en quirófano.");
        }
    }

    @Test(priority = 3, dependsOnMethods = {"EnterSurgeryWL", "CreateOperatingSheet"})
    public void PlanningSurgery() {

        if (OpenSurgeryPlanningWL()) {

            WebElement visionDia = driver.findElement(By.id("daily-button"));
            visionDia.click();

            OpenPlanningActionMenu();

            WebElement planificarIntervencion = driver.findElement(By.id("schedule_intervention"));
            planificarIntervencion.click();

            // intentamos planificar en el primer hueco del dia
            WebElement hueco = wait.until(ExpectedConditions.elementToBeClickable(By.id("gap-id-2-0")));
            hueco.click();



        }

    }


    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // métodos auxiliares
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean OpenSurgeryModule() {

        boolean isInSurgeryModule = false;
        List<WebElement> elements = driver.findElements(By.id("operating-room-back-button"));
        if (!elements.isEmpty()) {
            isInSurgeryModule = elements.getFirst().isDisplayed();
        }

        if (!isInSurgeryModule) {
            //Acceder al módulo de Urgencias
            WebElement moduloQuirofano = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("operating_room-sidebar")));

            if (moduloQuirofano.isDisplayed()) {
                moduloQuirofano.click();
                return true;
            } else {
                return false;
            }
        }
        else {
            return true;
        }
    }

    public boolean OpenSurgeryWL() {

        if (OpenSurgeryModule()) {
            WebElement wlQuirofano = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("worklist-sidebar")));
            wlQuirofano.click();

            WaitAMomentPlease();

            return true;
        } else {
            return false;
        }
    }

    public boolean OpenSurgeryPlanningWL() {

        if (OpenSurgeryModule()) {
            WebElement wlPlanificador = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("scheduler-sidebar")));
            wlPlanificador.click();

            WaitAMomentPlease();

            return true;
        } else {
            return false;
        }
    }

    public boolean IsPatientInSurgeryWL() {
        //Filtra por el paciente
        WebElement filtroPaciente = driver.findElement(By.id("filter-input"));
        filtroPaciente.clear();
        TakeOffServerFilters();
        WaitAMomentPlease();

        String NH = ConfigReader.get("pacqah1NH");
        filtroPaciente.sendKeys(NH);
        WaitAMomentPlease();
        boolean isPatientInSurgeryWL;

        try {
            // Esperar a que haya al menos una fila en el grid
            List<WebElement> filas = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("[id^='gridId-']")
            ));

            if (!filas.isEmpty()) {
                // Esperar a que la primera fila sea clicable
                WebElement primeraFila = wait.until(ExpectedConditions.elementToBeClickable(filas.getFirst()));
                primeraFila.click();
                isPatientInSurgeryWL = true;
            } else {
                isPatientInSurgeryWL = false;
            }

        } catch (TimeoutException e) {
            isPatientInSurgeryWL = false;
        }
        WaitAMomentPlease();
        return isPatientInSurgeryWL;
    }

    public void TakeOffServerFilters() {
        WebElement filtroWL = wait.until(ExpectedConditions.elementToBeClickable(By.id("filters-button")));
        filtroWL.click();

        WebElement withoutFilter = wait.until(ExpectedConditions.elementToBeClickable(By.id("filter-not-filtered")));
        withoutFilter.click();

        try {
            WebElement dialogo = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mat-dialog-0")));
            if (dialogo.isDisplayed()) {
                WebElement botonConfirmar = driver.findElement(By.id("continue-button"));
                botonConfirmar.click();

            }
        } catch (TimeoutException e) {
        }

    }

    private void OpenSurgeryWLActionMenu() {
        WebElement accionesSurgeryWLActionMenu = wait.until(ExpectedConditions.elementToBeClickable(By.id("actions-button")));
        accionesSurgeryWLActionMenu.click();
        WaitAMomentPlease();
    }

    private void OpenPlanningActionMenu()
    {
        WebElement accionesSurgeryWLActionMenu = wait.until(ExpectedConditions.elementToBeClickable(By.id("operating-room-actions-button")));
        accionesSurgeryWLActionMenu.click();
        WaitAMomentPlease();
    }
}
