-- Resuelve el CUSTOMERID interno y el nombre del paciente a partir del
-- número de historia (NH), para poder mostrarlo antes de un borrado.
-- Uso: SqlScriptRunner.query("get_customerid_by_nh.sql", Map.of("NHC", nh))
SELECT CUSTOMERID, NAMECUSTOMER, FIRSTSURNAMECUSTOMER, SECONDSURNAMECUSTOMER
FROM PATIENTMANAGEMENT..CUSTOMER WHERE HISTORYNUMBER = '@NHC'
