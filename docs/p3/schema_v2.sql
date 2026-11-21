-- CuidaDosis: esquema v2 (DISEÑO OBJETIVO P3; cambios G-01..G-07 sobre bd/schema.sql)
-- No aplicado aún a la app; se implementará con Claude Code en la fase de modificación.
ALTER TABLE users ADD COLUMN last_sync_at DATETIME NULL;                       -- G-04
ALTER TABLE links MODIFY status ENUM('pending','awaiting_acceptance','active','revoked') NOT NULL DEFAULT 'pending'; -- G-02
ALTER TABLE links ADD COLUMN share_dose TINYINT(1) NOT NULL DEFAULT 0,          -- G-07 (diferible)
  ADD CONSTRAINT chk_links_share_dose CHECK (share_dose IN (0,1));
-- G-01: la vigencia del código pasa de 48 h a 10 min (expires_at = NOW() + INTERVAL 10 MINUTE, en la API)
ALTER TABLE intake_logs ADD COLUMN client_uuid CHAR(36) NULL,                   -- G-03
  ADD COLUMN received_at DATETIME NULL,                                         -- G-04
  ADD UNIQUE KEY uq_intake_logs_client_uuid (client_uuid);
CREATE TABLE link_attempts (                                                    -- G-05
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id INT UNSIGNED NOT NULL,
  attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  success TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_link_attempts_user_time (user_id, attempted_at),
  CONSTRAINT fk_link_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- G-06: anticipación L (5|10 min) se guarda en el teléfono; sin cambio en servidor.
