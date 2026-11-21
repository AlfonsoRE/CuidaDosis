-- CuidaDosis: esquema de base de datos v2 (diseño P3, cambios G-01 a G-07)
-- Compatible con MariaDB 10.4 (XAMPP) y MySQL 8.
-- Ejecutar de nuevo REINICIA las tablas y borra sus datos locales.
-- Fechas y horas en hora local (America/Mexico_City).

CREATE DATABASE IF NOT EXISTS cuidadosis
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cuidadosis;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS link_attempts;
DROP TABLE IF EXISTS api_tokens;
DROP TABLE IF EXISTS intake_logs;
DROP TABLE IF EXISTS schedules;
DROP TABLE IF EXISTS medications;
DROP TABLE IF EXISTS links;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- Usuarios con rol paciente o cuidador
CREATE TABLE users (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('patient', 'caregiver') NOT NULL,
    -- G-04: última sincronización de tomas desde el teléfono del paciente (RF-12)
    last_sync_at  DATETIME NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Vinculación paciente y cuidador por código de invitación (ADR-004).
-- El código lo genera el paciente (caregiver_id NULL, status pending),
-- caduca a los 10 min (G-01, lo fija la API) y es de un solo uso.
-- Al usarlo el cuidador pasa a awaiting_acceptance (G-02) y solo el
-- paciente lo activa (active). Rechazar o revocar deja status revoked.
-- share_dose (G-07): el cuidador solo recibe la dosis si vale 1.
-- Un paciente tiene como máximo un cuidador activo; un cuidador puede tener varios pacientes.
-- Desvincular no borra la fila: pasa a status revoked con revoked_at.
-- Que patient_id sea paciente y caregiver_id cuidador lo valida la API.
CREATE TABLE links (
    id                  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    patient_id          INT UNSIGNED NOT NULL,
    caregiver_id        INT UNSIGNED NULL,
    invite_code         CHAR(8) NOT NULL,
    status              ENUM('pending', 'awaiting_acceptance', 'active', 'revoked') NOT NULL DEFAULT 'pending',
    share_dose          TINYINT(1) NOT NULL DEFAULT 0,
    expires_at          DATETIME NOT NULL,
    requested_at        DATETIME NULL,
    linked_at           DATETIME NULL,
    revoked_at          DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Solo tiene valor si el vínculo está activo; con UNIQUE garantiza
    -- un cuidador activo por paciente
    active_patient_id   INT UNSIGNED AS (IF(status = 'active', patient_id, NULL)) PERSISTENT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_links_invite_code (invite_code),
    UNIQUE KEY uq_links_active_patient (active_patient_id),
    KEY idx_links_patient (patient_id),
    KEY idx_links_caregiver (caregiver_id),
    CONSTRAINT fk_links_patient FOREIGN KEY (patient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_links_caregiver FOREIGN KEY (caregiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_links_distinct_users CHECK (caregiver_id IS NULL OR caregiver_id <> patient_id),
    CONSTRAINT chk_links_active_has_caregiver CHECK (
        status <> 'active' OR (caregiver_id IS NOT NULL AND linked_at IS NOT NULL)
    ),
    CONSTRAINT chk_links_awaiting_has_caregiver CHECK (
        status <> 'awaiting_acceptance' OR (caregiver_id IS NOT NULL AND requested_at IS NOT NULL)
    ),
    CONSTRAINT chk_links_share_dose CHECK (share_dose IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Medicamentos del paciente. Nombre y dosis son texto libre del usuario;
-- el sistema no los interpreta ni recomienda dosis.
-- Se desactivan (active = 0) en lugar de borrarse para conservar el historial.
CREATE TABLE medications (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    patient_id INT UNSIGNED NOT NULL,
    name       VARCHAR(100) NOT NULL,
    dose       VARCHAR(100) NOT NULL,
    active     TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_medications_patient_active (patient_id, active),
    CONSTRAINT fk_medications_patient FOREIGN KEY (patient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_medications_active CHECK (active IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Horas diarias de cada medicamento (una o varias)
CREATE TABLE schedules (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    medication_id INT UNSIGNED NOT NULL,
    time_of_day   TIME NOT NULL,
    active        TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_schedules_medication_time (medication_id, time_of_day),
    CONSTRAINT fk_schedules_medication FOREIGN KEY (medication_id) REFERENCES medications (id) ON DELETE CASCADE,
    CONSTRAINT chk_schedules_active CHECK (active IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tomas programadas. La API crea las del día como pendientes al consultar
-- el día; "taken" solo registra el toque en "Tomado" (Confirmada por el
-- paciente), no la ingesta. "Sin confirmar" se calcula, no se guarda (ADR-005).
-- client_uuid (G-03): UUID generado en el teléfono; hace idempotente la
-- sincronización en lote (ADR-003). received_at (G-04): llegada al servidor.
CREATE TABLE intake_logs (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    schedule_id   INT UNSIGNED NOT NULL,
    scheduled_for DATETIME NOT NULL,
    status        ENUM('pending', 'taken') NOT NULL DEFAULT 'pending',
    confirmed_at  DATETIME NULL,
    client_uuid   CHAR(36) NULL,
    received_at   DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_intake_logs_schedule_time (schedule_id, scheduled_for),
    UNIQUE KEY uq_intake_logs_client_uuid (client_uuid),
    KEY idx_intake_logs_scheduled_for (scheduled_for),
    CONSTRAINT fk_intake_logs_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id) ON DELETE CASCADE,
    CONSTRAINT chk_intake_logs_confirmation CHECK (
        (status = 'taken' AND confirmed_at IS NOT NULL)
        OR (status = 'pending' AND confirmed_at IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tokens de sesión. Solo se guarda el hash SHA-256, nunca el token en claro.
CREATE TABLE api_tokens (
    id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    INT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_api_tokens_hash (token_hash),
    KEY idx_api_tokens_user (user_id),
    KEY idx_api_tokens_expires (expires_at),
    CONSTRAINT fk_api_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Intentos de usar un código de invitación (G-05, ADR-004).
-- Tras 5 fallos de un cuidador en 10 min la API responde 429.
CREATE TABLE link_attempts (
    id           INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id      INT UNSIGNED NOT NULL,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success      TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_link_attempts_user_time (user_id, attempted_at),
    CONSTRAINT fk_link_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_link_attempts_success CHECK (success IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
