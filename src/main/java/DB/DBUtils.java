package DB;

import java.sql.*;

public class DBUtils {

    private static final String URL = "jdbc:sqlserver://172.17.11.11;databaseName=Configuration;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "cubeuser";
    private static final String PASSWORD = "R1b0nucl31c0";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
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