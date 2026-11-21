-- CuidaDosis: carga inicial para hosting
-- Intencionalmente SIN cuentas de demostración ni datos de prueba: en un servidor público
-- las cuentas se crean desde la app. Para una demostración con datos sintéticos se usa
-- bd/seed.sql solo en el entorno local.
--
-- Importar después de schema_hosting.sql, en la misma base. Solo verifica que el esquema
-- quedó vacío y completo; no inserta nada.

SELECT
    (SELECT COUNT(*) FROM users)         AS usuarios,
    (SELECT COUNT(*) FROM links)         AS vinculos,
    (SELECT COUNT(*) FROM medications)   AS medicamentos,
    (SELECT COUNT(*) FROM schedules)     AS horarios,
    (SELECT COUNT(*) FROM intake_logs)   AS tomas,
    (SELECT COUNT(*) FROM api_tokens)    AS sesiones,
    (SELECT COUNT(*) FROM link_attempts) AS intentos;
