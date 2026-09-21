-- Ejemplo de script "on-demand": comprueba si existe el paciente de pruebas
-- por su número de historia. El marcador @NHC se sustituye por el valor que
-- se pase en el Map de parámetros al llamar a SqlScriptRunner.
--
-- Uso desde un test:
--   boolean existe = SqlScriptRunner.exists(
--           "check_patient_exists.sql",
--           Map.of("NHC", ConfigReader.get("pacqah1NH")));
--
SELECT 'x' FROM PATIENTMANAGEMENT..CUSTOMER WHERE HISTORYNUMBER = '@NHC';
