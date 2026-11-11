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
INSERT INTO users (id, full_name, email, password_hash, role) VALUES
    (1, 'Paciente SINTETICO Uno',  'paciente1@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'patient'),
    (2, 'Paciente SINTETICO Dos',  'paciente2@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'patient'),
    (3, 'Cuidador SINTETICO Uno',  'cuidador1@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'caregiver'),
    (4, 'Cuidador SINTETICO Dos',  'cuidador2@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'caregiver'),
    (5, 'Paciente SINTETICO Tres', 'paciente3@ejemplo.test', '$2y$10$pSwklJIgwrrSf.7uNK/c/ubjagvNgpt.720DhzmdCaaT8S0MsNxNC', 'patient');

-- Vínculos:
-- Pacientes 1 y 3 están vinculados con Cuidador 1 (un cuidador puede tener varios pacientes).
-- Paciente 2 tiene un código pendiente (SINT0002) para probar la vinculación con Cuidador 2.
INSERT INTO links (id, patient_id, caregiver_id, invite_code, status, expires_at, linked_at, created_at) VALUES
    (1, 1, 3,    'SINT0001', 'active',  NOW() - INTERVAL 1 DAY + INTERVAL 48 HOUR, NOW() - INTERVAL 1 DAY + INTERVAL 1 HOUR, NOW() - INTERVAL 1 DAY),
    (2, 2, NULL, 'SINT0002', 'pending', NOW() + INTERVAL 48 HOUR, NULL, NOW()),
    (3, 5, 3,    'SINT0003', 'active',  NOW() - INTERVAL 2 HOUR + INTERVAL 48 HOUR, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 2 HOUR);

-- Medicamentos (texto libre de ejemplo)
INSERT INTO medications (id, patient_id, name, dose, active) VALUES
    (1, 1, 'Medicamento SINTETICO A', '1 tableta (dato de prueba)', 1),
    (2, 1, 'Medicamento SINTETICO B', '5 ml (dato de prueba)', 1),
    (3, 1, 'Medicamento SINTETICO D (inactivo)', '1 cápsula (dato de prueba)', 0),
    (4, 2, 'Medicamento SINTETICO C', '2 gotas (dato de prueba)', 1),
    (5, 5, 'Medicamento SINTETICO E', '1 sobre (dato de prueba)', 1);

-- Horarios diarios
INSERT INTO schedules (id, medication_id, time_of_day, active) VALUES
    (1, 1, '08:00:00', 1),
    (2, 1, '20:00:00', 1),
    (3, 2, '14:00:00', 1),
    (4, 3, '10:00:00', 0),
    (5, 4, '09:00:00', 1),
    (6, 5, '12:00:00', 1);

-- Tomas de hoy del Paciente 1: una tomada y dos pendientes
INSERT INTO intake_logs (schedule_id, scheduled_for, status, confirmed_at) VALUES
    (1, TIMESTAMP(CURDATE(), '08:00:00'), 'taken',   TIMESTAMP(CURDATE(), '08:03:00')),
    (3, TIMESTAMP(CURDATE(), '14:00:00'), 'pending', NULL),
    (2, TIMESTAMP(CURDATE(), '20:00:00'), 'pending', NULL);
