-- CuidaDosis: PREPARACIÓN OPCIONAL DE LA PRUEBA FÍSICA SIN CABLE. DATOS SINTETICOS.
-- Todo lo que crea es ficticio: no representa personas ni tratamientos reales.
-- El nombre y la dosis son texto de ejemplo; el sistema no recomienda dosis.
--
-- NO se ejecuta automáticamente ni forma parte del seed. Úsalo solo cuando vayas a correr
-- la prueba en el teléfono, después de bd/seed.sql (necesita paciente1 y cuidador1).
-- Nunca en una base con usuarios reales.
--
-- Qué hace, para paciente1@ejemplo.test:
--   1. Deja activo su vínculo con cuidador1@ejemplo.test (revoca otro vínculo activo, si hay).
--   2. Desactiva los "Medicamento de prueba" de una corrida anterior (no borra nada).
--   3. Crea 5 medicamentos "Medicamento de prueba A..E (SINTETICO)", uno por toma, a
--      ahora + 10, 50, 90, 130 y 170 minutos (redondeado al minuto).
--
-- El aviso previo de 5 min NO se guarda en la BD (G-06): es el ajuste local del teléfono,
-- que vale 5 por defecto. Revísalo en la app (Ajustes de avisos) antes de empezar.
-- Si alguna toma cae después de las 23:59, su hora es de mañana.
-- Los medicamentos SINTETICO A y B del seed siguen activos (08:00, 14:00 y 20:00).
--
-- Uso (Git Bash, raíz del repo, credenciales leídas de api/.env sin escribirlas):
--   set -a && . ./api/.env && set +a && export MYSQL_PWD="$DB_PASS"
--   /c/xampp/mysql/bin/mysql.exe -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
--       --default-character-set=utf8mb4 "$DB_NAME" < bd/preparar_prueba_fisica.sql
-- Después, en el teléfono: inicia sesión como paciente1 (o toca actualizar en "Hoy" y
-- abre "Medicamentos") para que la app descargue las horas y programe los avisos.

-- Hora de México (sin horario de verano desde 2022), igual que la API
SET time_zone = '-06:00';

SET @patient = (SELECT id FROM cd_users WHERE email = 'paciente1@ejemplo.test' AND role = 'patient');
SET @caregiver = (SELECT id FROM cd_users WHERE email = 'cuidador1@ejemplo.test' AND role = 'caregiver');

-- Si falta alguno de los dos, se detiene aquí: el INSERT en una columna NOT NULL falla
SELECT IF(@patient IS NULL OR @caregiver IS NULL,
          'FALTA paciente1 o cuidador1: carga antes bd/seed.sql', 'Usuarios encontrados') AS verificacion;
DROP TEMPORARY TABLE IF EXISTS tmp_cd_check;
CREATE TEMPORARY TABLE tmp_cd_check (v INT NOT NULL);
INSERT INTO tmp_cd_check VALUES (IF(@patient IS NULL OR @caregiver IS NULL, NULL, 1));
DROP TEMPORARY TABLE tmp_cd_check;

START TRANSACTION;

-- 1. Vínculo activo paciente1 con cuidador1 (un solo cuidador activo por paciente)
UPDATE cd_links SET status = 'revoked', revoked_at = NOW()
 WHERE patient_id = @patient AND status IN ('active', 'awaiting_acceptance', 'pending')
   AND (caregiver_id IS NULL OR caregiver_id <> @caregiver);

INSERT INTO cd_links (patient_id, caregiver_id, invite_code, status, share_dose, expires_at, requested_at, linked_at)
SELECT @patient, @caregiver, CONCAT('PF', LPAD(FLOOR(RAND() * 1000000), 6, '0')), 'active', 0,
       NOW(), NOW(), NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM cd_links WHERE patient_id = @patient AND caregiver_id = @caregiver AND status = 'active'
 );

-- 2. Desactiva los de una corrida anterior (se conservan para el historial)
UPDATE cd_schedules s JOIN cd_medications m ON m.id = s.medication_id
   SET s.active = 0
 WHERE m.patient_id = @patient AND m.name LIKE 'Medicamento de prueba %';
UPDATE cd_medications SET active = 0
 WHERE patient_id = @patient AND name LIKE 'Medicamento de prueba %';

-- 3. Cinco tomas cada 40 minutos desde ahora + 10 min
SET @base = TIME_TO_SEC(TIME(NOW())) DIV 60 * 60;

INSERT INTO cd_medications (patient_id, name, dose) VALUES
    (@patient, 'Medicamento de prueba A (SINTETICO)', '1 unidad (dato de prueba)');
INSERT INTO cd_schedules (medication_id, time_of_day) VALUES
    (LAST_INSERT_ID(), SEC_TO_TIME(MOD(@base + 10 * 60, 86400)));

INSERT INTO cd_medications (patient_id, name, dose) VALUES
    (@patient, 'Medicamento de prueba B (SINTETICO)', '1 unidad (dato de prueba)');
INSERT INTO cd_schedules (medication_id, time_of_day) VALUES
    (LAST_INSERT_ID(), SEC_TO_TIME(MOD(@base + 50 * 60, 86400)));

INSERT INTO cd_medications (patient_id, name, dose) VALUES
    (@patient, 'Medicamento de prueba C (SINTETICO)', '1 unidad (dato de prueba)');
INSERT INTO cd_schedules (medication_id, time_of_day) VALUES
    (LAST_INSERT_ID(), SEC_TO_TIME(MOD(@base + 90 * 60, 86400)));

INSERT INTO cd_medications (patient_id, name, dose) VALUES
    (@patient, 'Medicamento de prueba D (SINTETICO)', '1 unidad (dato de prueba)');
INSERT INTO cd_schedules (medication_id, time_of_day) VALUES
    (LAST_INSERT_ID(), SEC_TO_TIME(MOD(@base + 130 * 60, 86400)));

INSERT INTO cd_medications (patient_id, name, dose) VALUES
    (@patient, 'Medicamento de prueba E (SINTETICO)', '1 unidad (dato de prueba)');
INSERT INTO cd_schedules (medication_id, time_of_day) VALUES
    (LAST_INSERT_ID(), SEC_TO_TIME(MOD(@base + 170 * 60, 86400)));

COMMIT;

-- Resumen: horas de las tomas, del aviso previo (con L = 5) y de la alarma del cuidador (T + 30)
SELECT m.name AS medicamento,
       TIME_FORMAT(s.time_of_day, '%H:%i') AS toma,
       TIME_FORMAT(SEC_TO_TIME(MOD(TIME_TO_SEC(s.time_of_day) - 5 * 60 + 86400, 86400)), '%H:%i') AS aviso_previo_L5,
       TIME_FORMAT(SEC_TO_TIME(MOD(TIME_TO_SEC(s.time_of_day) + 30 * 60, 86400)), '%H:%i') AS alarma_cuidador
  FROM cd_medications m JOIN cd_schedules s ON s.medication_id = m.id
 WHERE m.patient_id = @patient AND m.active = 1 AND s.active = 1 AND m.name LIKE 'Medicamento de prueba %'
 ORDER BY m.name;
