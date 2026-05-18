package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class ExampleIATest extends ClassBaseTest {

    /**
     * Test para hacer login con el doctor y comprobar que se abre la pantalla de ajustes 
     * que se accede desde la parte superior derecha mediante el menu contextual 
     * que se abre al pulsar en el nombre del usuario
     */
    @Test(description = "Login como doctor y validar acceso a pantalla de ajustes desde menú contextual")
    public void testAccessSettingsFromUserMenu() {
        // Step 1: Login como doctor
        LoginAsDoctor();
        Assert.assertTrue(isUserLoggedIn(), "El usuario no pudo iniciar sesión correctamente");
        
        // Step 2: Abrir el menú contextual del usuario
        boolean menuOpened = openUserContextMenu();
        Assert.assertTrue(menuOpened, "No se pudo abrir el menú contextual del usuario");
        
        // Step 3: Hacer clic en la opción de Ajustes
        boolean settingsOpened = clickSettingsOption();
        Assert.assertTrue(settingsOpened, "No se pudo abrir la pantalla de ajustes");
        
        // Step 4: Validar que la pantalla de ajustes se abrió correctamente
        boolean isSettingsVisible = isSettingsScreenVisible();
        Assert.assertTrue(isSettingsVisible, "La pantalla de ajustes no es visible");
        
        WaitAMomentPlease();
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Verifica si el usuario está logado
     */
    private boolean isUserLoggedIn() {
        try {
            WebElement userIcon = driver.findElement(By.cssSelector("mat-icon.user-profile-icon"));
            return userIcon.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Abre el menú contextual del usuario haciendo clic en el nombre o icono del usuario
     * en la parte superior derecha
     */
    private boolean openUserContextMenu() {
        try {
            WaitAMomentPlease(1);
            
            // Intenta encontrar el elemento del usuario en varias formas
            WebElement userElement = null;
            
            try {
                // Opción 1: Buscar por el ícono de usuario específico
                userElement = driver.findElement(By.cssSelector("mat-icon.user-profile-icon"));
            } catch (NoSuchElementException e1) {
                try {
                    // Opción 2: Buscar por clase o id de perfil de usuario
                    userElement = driver.findElement(By.cssSelector("[class*='user-profile'], [id*='user-menu']"));
                } catch (NoSuchElementException e2) {
                    try {
                        // Opción 3: Buscar por aria-label
                        userElement = driver.findElement(By.cssSelector("button[aria-label*='user'], button[aria-label*='perfil'], button[aria-label*='profile']"));
                    } catch (NoSuchElementException e3) {
                        // Opción 4: Buscar en la parte superior derecha por elementos que contengan "user"
                        List<WebElement> elements = driver.findElements(By.xpath("//mat-toolbar//*[contains(@class, 'user') or contains(@class, 'perfil')]"));
                        if (!elements.isEmpty()) {
                            userElement = elements.getFirst();
                        }
                    }
                }
            }
            
            // Si encontró el elemento del usuario, haz clic para abrir el menú
            if (userElement != null) {
                userElement.click();
                WaitAMomentPlease(1);
                
                // Valida que se abrió el menú contextual
                List<WebElement> menuItems = driver.findElements(By.cssSelector("mat-menu-item, [role='menuitem']"));
                return !menuItems.isEmpty();
            }
            
            return false;
            
        } catch (Exception e) {
            System.out.println("Error al abrir el menú contextual del usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hace clic en la opción de Ajustes dentro del menú contextual del usuario
     */
    private boolean clickSettingsOption() {
        try {
            WaitAMomentPlease(1);
            
            // Busca la opción de Ajustes en el menú contextual
            List<WebElement> menuItems = driver.findElements(By.cssSelector("mat-menu-item, [role='menuitem']"));
            
            WebElement settingsOption = null;
            
            // Busca la opción que contenga "Ajustes", "Settings", "Configuración", etc.
            for (WebElement item : menuItems) {
                String itemText = item.getText().toLowerCase();
                if (itemText.contains("ajuste") || 
                    itemText.contains("setting") || 
                    itemText.contains("configuración") ||
                    itemText.contains("preferences") ||
                    itemText.contains("preferencia")) {
                    settingsOption = item;
                    break;
                }
            }
            
            // Si no encontró por texto, intenta por id
            if (settingsOption == null) {
                try {
                    settingsOption = driver.findElement(By.id("settings-button"));
                } catch (NoSuchElementException e) {
                    try {
                        settingsOption = driver.findElement(By.id("ajustes-button"));
                    } catch (NoSuchElementException e2) {
                        // Intenta buscar por clase que contenga "settings"
                        settingsOption = driver.findElement(By.cssSelector("[class*='settings']"));
                    }
                }
            }
            
            // Si encontró la opción de ajustes, haz clic
            if (settingsOption != null) {
                settingsOption.click();
                WaitAMomentPlease(2);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.out.println("Error al hacer clic en la opción de Ajustes: " + e.getMessage());
            return false;
        }
    }

    /**
     * Valida que la pantalla de ajustes se abrió correctamente
     */
    private boolean isSettingsScreenVisible() {
        try {
            WaitAMomentPlease(1);
            
            // Busca elementos que indican que estamos en la pantalla de ajustes
            List<WebElement> settingsElements = null;
            
            try {
                // Opción 1: Buscar por tag específico de componente
                settingsElements = driver.findElements(By.tagName("gc-settings, app-settings, settings-container"));
                if (!settingsElements.isEmpty() && settingsElements.getFirst().isDisplayed()) {
                    return true;
                }
            } catch (NoSuchElementException e1) {
                // Continúa con otras opciones
            }
            
            try {
                // Opción 2: Buscar por título que contenga "Ajustes"
                List<WebElement> titles = driver.findElements(By.xpath("//*[contains(text(), 'Ajuste') or contains(text(), 'Setting') or contains(text(), 'Configuración')]"));
                if (!titles.isEmpty()) {
                    return true;
                }
            } catch (NoSuchElementException e2) {
                // Continúa con otras opciones
            }
            
            try {
                // Opción 3: Buscar por id de la pantalla de ajustes
                WebElement settingsScreen = driver.findElement(By.id("settings-screen"));
                return settingsScreen.isDisplayed();
            } catch (NoSuchElementException e3) {
                // Continúa
            }
            
            try {
                // Opción 4: Buscar por clase que contenga "settings"
                List<WebElement> settingsContainers = driver.findElements(By.cssSelector("[class*='settings-container'], [class*='SettingsComponent']"));
                if (!settingsContainers.isEmpty()) {
                    return settingsContainers.getFirst().isDisplayed();
                }
            } catch (NoSuchElementException e4) {
                // Continúa
            }
            
            // Opción 5: Buscar por cualquier elemento que indique una pantalla abierta
            List<WebElement> dialogs = driver.findElements(By.cssSelector("mat-dialog-container, .dialog-container, [role='dialog']"));
            return !dialogs.isEmpty() && dialogs.getFirst().isDisplayed();
            
        } catch (Exception e) {
            System.out.println("Error al validar pantalla de ajustes: " + e.getMessage());
            return false;
        }
    }

}
