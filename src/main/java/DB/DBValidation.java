package DB;

import org.testng.Assert;


public class DBValidation {



    public void validarDatosEnSQLServer() throws Exception {

        String query = "SELECT nombre FROM usuarios WHERE id=123";
        String nombre = DBUtils.getValor(query, "nombre");

        Assert.assertEquals(nombre, "Luis", "El nombre en SQL Server no coincide");
    }
}


