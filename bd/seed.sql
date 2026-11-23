-- CuidaDosis: DATOS SINTETICOS DE PRUEBA
-- Todo en este archivo es ficticio. No representa a personas ni tratamientos reales.
-- Nombres y dosis son texto de ejemplo; el sistema no recomienda dosis.
--
-- Contraseña de todos los usuarios sintéticos: Sintetico2026
-- (solo para pruebas locales; no es credencial de ningún sistema real)
--
-- Requiere haber ejecutado antes bd/schema.sql.
-- Las fechas son relativas al día en que se ejecuta (CURDATE y NOW).

USE cuidadosis;

-- Usuarios: 3 pacientes y 2 cuidadores
INSERT INTO cd_users (id, full_name, email, password_hash, role) VALUES
    (1, 'Paciente SINTETICO Uno',  'paciente1@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'patient'),
    (2, 'Paciente SINTETICO Dos',  'paciente2@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'patient'),
    (3, 'Cuidador SINTETICO Uno',  'cuidador1@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'caregiver'),
    (4, 'Cuidador SINTETICO Dos',  'cuidador2@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'caregiver'),
    (5, 'Paciente SINTETICO Tres', 'paciente3@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'patient');

-- Última sincronización (G-04): Paciente 1 sincronizó hace 20 min; los demás nunca
UPDATE cd_users SET last_sync_at = NOW() - INTERVAL 20 MINUTE WHERE id = 1;

-- Vínculos (los códigos viven 10 min; G-01):
-- Pacientes 1 y 3 están vinculados con Cuidador 1 (un cuidador puede tener varios pacientes).
-- Paciente 1 no comparte la dosis (share_dose = 0); Paciente 3 sí (share_dose = 1).
-- Paciente 2 tiene un código pendiente (SINT0002) que vence 10 min después de cargar el seed.
INSERT INTO cd_links (id, patient_id, caregiver_id, invite_code, status, share_dose, expires_at, requested_at, linked_at, created_at) VALUES
    (1, 1, 3,    'SINT0001', 'active',  0, NOW() - INTERVAL 1 DAY + INTERVAL 10 MINUTE, NOW() - INTERVAL 1 DAY + INTERVAL 2 MINUTE, NOW() - INTERVAL 1 DAY + INTERVAL 5 MINUTE, NOW() - INTERVAL 1 DAY),
    (2, 2, NULL, 'SINT0002', 'pending', 0, NOW() + INTERVAL 10 MINUTE, NULL, NULL, NOW()),
    (3, 5, 3,    'SINT0003', 'active',  1, NOW() - INTERVAL 2 HOUR + INTERVAL 10 MINUTE, NOW() - INTERVAL 2 HOUR + INTERVAL 3 MINUTE, NOW() - INTERVAL 2 HOUR + INTERVAL 6 MINUTE, NOW() - INTERVAL 2 HOUR);

-- Intentos de código (G-05): un intento exitoso del Cuidador 1 por cada vínculo activo
INSERT INTO cd_link_attempts (user_id, attempted_at, success) VALUES
    (3, NOW() - INTERVAL 1 DAY + INTERVAL 2 MINUTE, 1),
    (3, NOW() - INTERVAL 2 HOUR + INTERVAL 3 MINUTE, 1);

-- Medicamentos (texto libre de ejemplo)
INSERT INTO cd_medications (id, patient_id, name, dose, active) VALUES
    (1, 1, 'Medicamento SINTETICO A', '1 tableta (dato de prueba)', 1),
    (2, 1, 'Medicamento SINTETICO B', '5 ml (dato de prueba)', 1),
    (3, 1, 'Medicamento SINTETICO D (inactivo)', '1 cápsula (dato de prueba)', 0),
    (4, 2, 'Medicamento SINTETICO C', '2 gotas (dato de prueba)', 1),
    (5, 5, 'Medicamento SINTETICO E', '1 sobre (dato de prueba)', 1);

-- Horarios diarios
INSERT INTO cd_schedules (id, medication_id, time_of_day, active) VALUES
    (1, 1, '08:00:00', 1),
    (2, 1, '20:00:00', 1),
    (3, 2, '14:00:00', 1),
    (4, 3, '10:00:00', 0),
    (5, 4, '09:00:00', 1),
    (6, 5, '12:00:00', 1);

-- Tomas de hoy del Paciente 1: una confirmada por el paciente y dos pendientes
-- (la confirmada llegó por sincronización con un UUID sintético; G-03, G-04)
INSERT INTO cd_intake_logs (schedule_id, scheduled_for, status, confirmed_at, client_uuid, received_at) VALUES
    (1, TIMESTAMP(CURDATE(), '08:00:00'), 'taken',   TIMESTAMP(CURDATE(), '08:03:00'), '00000000-0000-4000-8000-000000000001', TIMESTAMP(CURDATE(), '08:04:00')),
    (3, TIMESTAMP(CURDATE(), '14:00:00'), 'pending', NULL, NULL, NULL),
    (2, TIMESTAMP(CURDATE(), '20:00:00'), 'pending', NULL, NULL, NULL);

-- Historial de ayer del Paciente 1 (RF-15): dos confirmadas y una que quedó
-- pendiente (la API la muestra como "Sin confirmar"; no se guarda ese estado)
INSERT INTO cd_intake_logs (schedule_id, scheduled_for, status, confirmed_at, client_uuid, received_at) VALUES
    (1, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '08:00:00'), 'taken',   TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '08:01:00'), '00000000-0000-4000-8000-000000000002', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '08:01:30')),
    (3, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '14:00:00'), 'pending', NULL, NULL, NULL),
    (2, TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '20:00:00'), 'taken',   TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '20:10:00'), '00000000-0000-4000-8000-000000000003', TIMESTAMP(CURDATE() - INTERVAL 1 DAY, '21:30:00'));
