package DB;

import java.sql.*;
import tests.ConfigReader;

public class DBUtils {

    public static Connection getConnection() throws Exception {
        String url = ConfigReader.get("db.url");
        String user = ConfigReader.get("db.user");
        String password = ConfigReader.get("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    public static String getValor(String query, String columna) throws Exception {
        try (
                Connection con = getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getString(columna);
            }
        }
        return null;
    }

    public static String GetParamValue(String codeParam) {
        String query = "SELECT DEFAULT_VALUE FROM CONFIGURATION..PARAMETERS WHERE CODE=?";
        try (
                Connection con = getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, codeParam);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("DEFAULT_VALUE");
            } else {
                return "";
            }
        } catch (Exception exception) {
            return "";
        }
    }
}