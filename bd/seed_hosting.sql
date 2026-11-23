-- CuidaDosis: carga inicial para hosting
-- Intencionalmente SIN cuentas de demostración ni datos de prueba: en un servidor público
-- las cuentas se crean desde la app. Para una demostración con datos sintéticos se usa
-- bd/seed.sql solo en el entorno local.
--
-- Importar después de schema_hosting.sql, en la misma base. Solo verifica que el esquema
-- quedó vacío y completo; no inserta nada.

SELECT
    (SELECT COUNT(*) FROM cd_users)         AS usuarios,
    (SELECT COUNT(*) FROM cd_links)         AS vinculos,
    (SELECT COUNT(*) FROM cd_medications)   AS medicamentos,
    (SELECT COUNT(*) FROM cd_schedules)     AS horarios,
    (SELECT COUNT(*) FROM cd_intake_logs)   AS tomas,
    (SELECT COUNT(*) FROM cd_api_tokens)    AS sesiones,
    (SELECT COUNT(*) FROM cd_link_attempts) AS intentos;
