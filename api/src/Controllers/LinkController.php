<?php

declare(strict_types=1);

namespace CuidaDosis\Controllers;

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Request;
use CuidaDosis\Response;
use PDO;

final class LinkController
{
    // Sin I, O, 0 ni 1 para evitar confusiones al dictar el código
    private const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

    public function __construct(private readonly PDO $pdo, private readonly Auth $auth)
    {
    }

    // El paciente genera un código (8 caracteres, 48 h, un solo uso)
    public function invite(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');

        $this->pdo->beginTransaction();
        try {
            // Bloquea las filas del paciente para evitar dos códigos simultáneos
            $stmt = $this->pdo->prepare('SELECT id, status FROM links WHERE patient_id = ? FOR UPDATE');
            $stmt->execute([$patient['id']]);
            foreach ($stmt->fetchAll() as $row) {
                if ($row['status'] === 'active') {
                    throw new ApiException(409, 'Ya tienes un cuidador vinculado.');
                }
            }

            $this->pdo->prepare(
                "UPDATE links SET status = 'revoked', revoked_at = NOW() WHERE patient_id = ? AND status = 'pending'"
            )
                ->execute([$patient['id']]);

            $id = null;
            for ($attempt = 0; $attempt < 5 && $id === null; $attempt++) {
                try {
                    $this->pdo->prepare(
                        'INSERT INTO links (patient_id, invite_code, expires_at) VALUES (?, ?, NOW() + INTERVAL 48 HOUR)'
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

            $stmt = $this->pdo->prepare('SELECT invite_code, expires_at FROM links WHERE id = ?');
            $stmt->execute([$id]);
            $link = $stmt->fetch();
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(201, $link);
    }

    // El cuidador usa el código para vincularse con el paciente
    public function accept(Request $request): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');
        $data = $request->json();
        $code = is_string($data['invite_code'] ?? null) ? strtoupper(trim($data['invite_code'])) : '';
        if (preg_match('/^[A-Z0-9]{8}$/', $code) !== 1) {
            throw new ApiException(422, 'Datos inválidos.', [
                'invite_code' => 'Debe tener 8 letras o números.',
            ]);
        }

        $this->pdo->beginTransaction();
        try {
            $stmt = $this->pdo->prepare(
                "SELECT id, patient_id FROM links
                  WHERE invite_code = ? AND status = 'pending' AND expires_at > NOW()
                  FOR UPDATE"
            );
            $stmt->execute([$code]);
            $link = $stmt->fetch();
            if ($link === false) {
                throw new ApiException(404, 'El código no existe, ya se usó o expiró.');
            }

            // Un cuidador puede tener varios pacientes; el índice único solo impide
            // que el paciente tenga dos cuidadores activos
            try {
                $this->pdo->prepare(
                    "UPDATE links SET caregiver_id = ?, status = 'active', linked_at = NOW() WHERE id = ?"
                )->execute([$caregiver['id'], $link['id']]);
            } catch (\PDOException $e) {
                if (($e->errorInfo[1] ?? null) === 1062) {
                    throw new ApiException(409, 'El paciente ya tiene un cuidador vinculado.');
                }
                throw $e;
            }

            $stmt = $this->pdo->prepare(
                'SELECT l.linked_at, u.id, u.full_name FROM links l JOIN users u ON u.id = l.patient_id WHERE l.id = ?'
            );
            $stmt->execute([$link['id']]);
            $row = $stmt->fetch();
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, [
            'patient' => ['id' => (int) $row['id'], 'full_name' => $row['full_name']],
            'linked_at' => $row['linked_at'],
        ]);
    }

    // El paciente revoca a su cuidador actual (después puede invitar a otro)
    public function unlinkCaregiver(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        $stmt = $this->pdo->prepare(
            "UPDATE links SET status = 'revoked', revoked_at = NOW() WHERE patient_id = ? AND status = 'active'"
        );
        $stmt->execute([$patient['id']]);
        if ($stmt->rowCount() === 0) {
            throw new ApiException(404, 'No tienes un cuidador vinculado.');
        }
        Response::json(200, ['message' => 'Cuidador desvinculado.']);
    }

    // El cuidador deja de observar a uno de sus pacientes
    public function unlinkPatient(Request $request, array $params): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');
        $stmt = $this->pdo->prepare(
            "UPDATE links SET status = 'revoked', revoked_at = NOW()
              WHERE patient_id = ? AND caregiver_id = ? AND status = 'active'"
        );
        $stmt->execute([$params['id'], $caregiver['id']]);
        if ($stmt->rowCount() === 0) {
            throw new ApiException(404, 'Ese paciente no está vinculado contigo.');
        }
        Response::json(200, ['message' => 'Paciente desvinculado.']);
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
