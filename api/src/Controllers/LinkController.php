<?php

declare(strict_types=1);

namespace CuidaDosis\Controllers;

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Request;
use CuidaDosis\Response;
use PDO;

// Vínculo paciente y cuidador con aceptación explícita del paciente (ADR-004):
// pending (código generado) -> awaiting_acceptance (el cuidador usó el código)
// -> active (el paciente aceptó). Rechazar o revocar deja el vínculo en revoked.
final class LinkController
{
    // Sin I, O, 0 ni 1 para evitar confusiones al dictar el código
    private const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    // G-01: vigencia del código
    private const CODE_TTL_MINUTES = 10;
    // G-05: intentos fallidos permitidos por cuidador en la ventana
    private const MAX_FAILED_ATTEMPTS = 5;
    private const ATTEMPT_WINDOW_MINUTES = 10;

    public function __construct(private readonly PDO $pdo, private readonly Auth $auth)
    {
    }

    // El paciente genera un código (8 caracteres, 10 min, un solo uso)
    public function invite(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');

        $this->pdo->beginTransaction();
        try {
            // Bloquea las filas del paciente para evitar dos códigos simultáneos
            $stmt = $this->pdo->prepare('SELECT id, status FROM cd_links WHERE patient_id = ? FOR UPDATE');
            $stmt->execute([$patient['id']]);
            foreach ($stmt->fetchAll() as $row) {
                if ($row['status'] === 'active') {
                    throw new ApiException(409, 'Ya tienes un cuidador vinculado.');
                }
            }

            // Un código nuevo invalida los anteriores y las solicitudes sin aceptar
            $this->pdo->prepare(
                "UPDATE cd_links SET status = 'revoked', revoked_at = NOW()
                  WHERE patient_id = ? AND status IN ('pending', 'awaiting_acceptance')"
            )->execute([$patient['id']]);

            $id = null;
            for ($attempt = 0; $attempt < 5 && $id === null; $attempt++) {
                try {
                    $this->pdo->prepare(
                        'INSERT INTO cd_links (patient_id, invite_code, expires_at)
                         VALUES (?, ?, NOW() + INTERVAL ' . self::CODE_TTL_MINUTES . ' MINUTE)'
                    )->execute([$patient['id'], $this->randomCode()]);
                    $id = (int) $this->pdo->lastInsertId();
                } catch (\PDOException $e) {
                    if (($e->errorInfo[1] ?? null) !== 1062) {
                        throw $e;
                    }
                }
            }
            if ($id === null) {
                throw new \RuntimeException('No se pudo generar un código único.');
            }

            $stmt = $this->pdo->prepare('SELECT invite_code, expires_at FROM cd_links WHERE id = ?');
            $stmt->execute([$id]);
            $link = $stmt->fetch();
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(201, $link);
    }

    // El cuidador usa el código; el vínculo queda esperando la aceptación del paciente
    public function accept(Request $request): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');
        $this->guardAttempts((int) $caregiver['id']);

        $data = $request->json();
        $code = is_string($data['invite_code'] ?? null) ? strtoupper(trim($data['invite_code'])) : '';
        if (preg_match('/^[A-Z0-9]{8}$/', $code) !== 1) {
            $this->recordAttempt((int) $caregiver['id'], false);
            throw new ApiException(422, 'Datos inválidos.', [
                'invite_code' => 'Debe tener 8 letras o números.',
            ]);
        }

        $this->pdo->beginTransaction();
        try {
            $stmt = $this->pdo->prepare(
                'SELECT id, patient_id, status, expires_at > NOW() AS vigente FROM cd_links WHERE invite_code = ? FOR UPDATE'
            );
            $stmt->execute([$code]);
            $link = $stmt->fetch();

            $failure = null;
            if ($link === false || $link['status'] !== 'pending') {
                $failure = new ApiException(404, 'El código no existe o ya se usó.');
            } elseif ((int) $link['vigente'] !== 1) {
                $failure = new ApiException(410, 'El código venció. Pide al paciente que genere uno nuevo.');
            }
            if ($failure !== null) {
                $this->pdo->rollBack();
                $this->recordAttempt((int) $caregiver['id'], false);
                throw $failure;
            }

            $this->pdo->prepare(
                "UPDATE cd_links SET caregiver_id = ?, status = 'awaiting_acceptance', requested_at = NOW() WHERE id = ?"
            )->execute([$caregiver['id'], $link['id']]);
            $this->pdo->prepare('INSERT INTO cd_link_attempts (user_id, success) VALUES (?, 1)')
                ->execute([$caregiver['id']]);

            $stmt = $this->pdo->prepare(
                'SELECT l.id, l.status, l.requested_at, u.id AS patient_id, u.full_name
                   FROM cd_links l JOIN cd_users u ON u.id = l.patient_id WHERE l.id = ?'
            );
            $stmt->execute([$link['id']]);
            $row = $stmt->fetch();
            $this->pdo->commit();
        } catch (ApiException $e) {
            throw $e;
        } catch (\Throwable $e) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            throw $e;
        }

        Response::json(200, [
            'link' => [
                'id' => (int) $row['id'],
                'status' => $row['status'],
                'requested_at' => $row['requested_at'],
            ],
            'patient' => ['id' => (int) $row['patient_id'], 'full_name' => $row['full_name']],
            'message' => 'Solicitud enviada. Espera a que el paciente la acepte.',
        ]);
    }

    // El paciente consulta la solicitud de vínculo que espera su aceptación
    public function pending(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        Response::json(200, ['request' => $this->pendingRequestFor((int) $patient['id'])]);
    }

    // El paciente acepta la solicitud: el vínculo pasa a activo
    public function confirm(Request $request, array $params): void
    {
        $patient = $this->auth->requireRole($request, 'patient');

        $this->pdo->beginTransaction();
        try {
            $this->lockAwaiting((int) $params['id'], (int) $patient['id']);
            try {
                $this->pdo->prepare("UPDATE cd_links SET status = 'active', linked_at = NOW() WHERE id = ?")
                    ->execute([$params['id']]);
            } catch (\PDOException $e) {
                if (($e->errorInfo[1] ?? null) === 1062) {
                    throw new ApiException(409, 'Ya tienes un cuidador vinculado.');
                }
                throw $e;
            }
            $stmt = $this->pdo->prepare(
                'SELECT l.id, l.linked_at, u.id AS caregiver_id, u.full_name
                   FROM cd_links l JOIN cd_users u ON u.id = l.caregiver_id WHERE l.id = ?'
            );
            $stmt->execute([$params['id']]);
            $row = $stmt->fetch();
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, [
            'link' => ['id' => (int) $row['id'], 'status' => 'active', 'linked_at' => $row['linked_at']],
            'caregiver' => ['id' => (int) $row['caregiver_id'], 'full_name' => $row['full_name']],
        ]);
    }

    // El paciente rechaza la solicitud: el vínculo queda revocado (no se borra)
    public function reject(Request $request, array $params): void
    {
        $patient = $this->auth->requireRole($request, 'patient');

        $this->pdo->beginTransaction();
        try {
            $this->lockAwaiting((int) $params['id'], (int) $patient['id']);
            $this->pdo->prepare("UPDATE cd_links SET status = 'revoked', revoked_at = NOW() WHERE id = ?")
                ->execute([$params['id']]);
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, ['message' => 'Solicitud rechazada.']);
    }

    // El paciente revoca a su cuidador actual (después puede invitar a otro)
    public function unlinkCaregiver(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        $stmt = $this->pdo->prepare(
            "UPDATE cd_links SET status = 'revoked', revoked_at = NOW() WHERE patient_id = ? AND status = 'active'"
        );
        $stmt->execute([$patient['id']]);
        if ($stmt->rowCount() === 0) {
            throw new ApiException(404, 'No tienes un cuidador vinculado.');
        }
        Response::json(200, ['message' => 'Cuidador desvinculado.']);
    }

    // El cuidador deja de observar a uno de sus pacientes, o retira su solicitud sin aceptar
    public function unlinkPatient(Request $request, array $params): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');
        $stmt = $this->pdo->prepare(
            "UPDATE cd_links SET status = 'revoked', revoked_at = NOW()
              WHERE patient_id = ? AND caregiver_id = ? AND status IN ('active', 'awaiting_acceptance')"
        );
        $stmt->execute([$params['id'], $caregiver['id']]);
        if ($stmt->rowCount() === 0) {
            throw new ApiException(404, 'Ese paciente no está vinculado contigo.');
        }
        Response::json(200, ['message' => 'Paciente desvinculado.']);
    }

    /**
     * Solicitud en awaiting_acceptance del paciente, o null.
     *
     * @return array<string, mixed>|null
     */
    private function pendingRequestFor(int $patientId): ?array
    {
        $stmt = $this->pdo->prepare(
            "SELECT l.id, l.requested_at, u.id AS caregiver_id, u.full_name
               FROM cd_links l JOIN cd_users u ON u.id = l.caregiver_id
              WHERE l.patient_id = ? AND l.status = 'awaiting_acceptance'
              ORDER BY l.id DESC LIMIT 1"
        );
        $stmt->execute([$patientId]);
        $row = $stmt->fetch();
        if ($row === false) {
            return null;
        }

        return [
            'id' => (int) $row['id'],
            'requested_at' => $row['requested_at'],
            'caregiver' => ['id' => (int) $row['caregiver_id'], 'full_name' => $row['full_name']],
        ];
    }

    // Bloquea la solicitud si es del paciente y sigue esperando aceptación
    private function lockAwaiting(int $linkId, int $patientId): void
    {
        $stmt = $this->pdo->prepare(
            "SELECT id FROM cd_links WHERE id = ? AND patient_id = ? AND status = 'awaiting_acceptance' FOR UPDATE"
        );
        $stmt->execute([$linkId, $patientId]);
        if ($stmt->fetch() === false) {
            throw new ApiException(404, 'No hay una solicitud de vínculo pendiente con ese identificador.');
        }
    }

    // G-05: con 5 fallos en los últimos 10 min responde 429 sin registrar otro intento
    private function guardAttempts(int $userId): void
    {
        $stmt = $this->pdo->prepare(
            'SELECT COUNT(*) AS n, MIN(attempted_at) AS first_at FROM cd_link_attempts
              WHERE user_id = ? AND success = 0
                AND attempted_at > NOW() - INTERVAL ' . self::ATTEMPT_WINDOW_MINUTES . ' MINUTE'
        );
        $stmt->execute([$userId]);
        $row = $stmt->fetch();
        if ((int) $row['n'] < self::MAX_FAILED_ATTEMPTS) {
            return;
        }

        // Segundos hasta que el fallo más antiguo de la ventana deje de contar
        $stmt = $this->pdo->prepare(
            'SELECT GREATEST(1, TIMESTAMPDIFF(SECOND, NOW(), ? + INTERVAL ' . self::ATTEMPT_WINDOW_MINUTES . ' MINUTE)) AS s'
        );
        $stmt->execute([$row['first_at']]);
        $retry = (int) $stmt->fetch()['s'];
        header('Retry-After: ' . $retry);
        throw new ApiException(
            429,
            'Demasiados intentos con códigos incorrectos. Espera unos minutos y vuelve a intentarlo.'
        );
    }

    private function recordAttempt(int $userId, bool $success): void
    {
        $this->pdo->prepare('INSERT INTO cd_link_attempts (user_id, success) VALUES (?, ?)')
            ->execute([$userId, $success ? 1 : 0]);
    }

    private function randomCode(): string
    {
        $code = '';
        $max = strlen(self::ALPHABET) - 1;
        for ($i = 0; $i < 8; $i++) {
            $code .= self::ALPHABET[random_int(0, $max)];
        }

        return $code;
    }
}
