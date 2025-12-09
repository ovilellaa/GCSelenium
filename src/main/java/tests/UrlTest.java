package tests;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class UrlTest extends ClassBaseTest {

    public void openUrl() {

        GotoToUrl();

        String titulo = driver.getTitle();

        assertEquals(titulo, "Green Cube");
    }

}
