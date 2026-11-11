<?php

declare(strict_types=1);

namespace CuidaDosis\Controllers;

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Request;
use CuidaDosis\Response;
use CuidaDosis\Validator;
use PDO;

final class AuthController
{
    // Hash ficticio para que el login tarde lo mismo exista o no el correo
    private const DUMMY_HASH = '$2y$10$gf2ztwSP4OJpFwInxqckkeFMZ7M79H9/iN1LFcT4oeUdfBQk2OiBi';

    public function __construct(private readonly PDO $pdo, private readonly Auth $auth)
    {
    }

    public function register(Request $request): void
    {
        $v = new Validator($request->json());
        $fullName = $v->string('full_name', 1, 100);
        $email = $v->email('email');
        $password = $v->string('password', 8, 72, false);
        $role = $v->enum('role', ['patient', 'caregiver']);
        $v->check();

        try {
            $stmt = $this->pdo->prepare(
                'INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, ?)'
            );
            $stmt->execute([$fullName, $email, password_hash($password, PASSWORD_DEFAULT), $role]);
        } catch (\PDOException $e) {
            if (($e->errorInfo[1] ?? null) === 1062) {
                throw new ApiException(409, 'Ya existe una cuenta con ese correo.');
            }
            throw $e;
        }

        $user = $this->findUser((int) $this->pdo->lastInsertId());
        $session = $this->auth->issueToken($user['id']);

        Response::json(201, ['user' => $user] + $session);
    }

    public function login(Request $request): void
    {
        $data = $request->json();
        $email = is_string($data['email'] ?? null) ? strtolower(trim($data['email'])) : '';
        $password = is_string($data['password'] ?? null) ? $data['password'] : '';
        if ($email === '' || $password === '') {
            throw new ApiException(422, 'Datos inválidos.', [
                'email' => 'El correo y la contraseña son obligatorios.',
            ]);
        }

        $stmt = $this->pdo->prepare('SELECT id, password_hash FROM users WHERE email = ?');
        $stmt->execute([$email]);
        $row = $stmt->fetch();

        $hash = $row === false ? self::DUMMY_HASH : $row['password_hash'];
        $ok = password_verify($password, $hash);
        if ($row === false || !$ok) {
            throw new ApiException(401, 'Correo o contraseña incorrectos.');
        }

        $userId = (int) $row['id'];
        if (password_needs_rehash($row['password_hash'], PASSWORD_DEFAULT)) {
            $this->pdo->prepare('UPDATE users SET password_hash = ? WHERE id = ?')
                ->execute([password_hash($password, PASSWORD_DEFAULT), $userId]);
        }

        $session = $this->auth->issueToken($userId);
        Response::json(200, ['user' => $this->findUser($userId)] + $session);
    }

    public function logout(Request $request): void
    {
        $this->auth->user($request);
        $this->auth->revokeToken((string) $request->bearerToken());
        Response::json(200, ['message' => 'Sesión cerrada.']);
    }

    public function me(Request $request): void
    {
        $user = $this->auth->user($request);
        $result = ['user' => $this->findUser($user['id']), 'link' => null];

        if ($user['role'] === 'patient') {
            $stmt = $this->pdo->prepare(
                "SELECT u.id, u.full_name, l.linked_at
                   FROM links l JOIN users u ON u.id = l.caregiver_id
                  WHERE l.patient_id = ? AND l.status = 'active'"
            );
            $stmt->execute([$user['id']]);
            $row = $stmt->fetch();
            if ($row !== false) {
                $result['link'] = [
                    'caregiver' => ['id' => (int) $row['id'], 'full_name' => $row['full_name']],
                    'linked_at' => $row['linked_at'],
                ];
            }

            // Código de invitación pendiente y vigente, si existe
            $stmt = $this->pdo->prepare(
                "SELECT invite_code, expires_at FROM links
                  WHERE patient_id = ? AND status = 'pending' AND expires_at > NOW()
                  ORDER BY id DESC LIMIT 1"
            );
            $stmt->execute([$user['id']]);
            $invite = $stmt->fetch();
            $result['pending_invite'] = $invite === false ? null : $invite;
        } else {
            $stmt = $this->pdo->prepare(
                "SELECT u.id, u.full_name, l.linked_at
                   FROM links l JOIN users u ON u.id = l.patient_id
                  WHERE l.caregiver_id = ? AND l.status = 'active'"
            );
            $stmt->execute([$user['id']]);
            $row = $stmt->fetch();
            if ($row !== false) {
                $result['link'] = [
                    'patient' => ['id' => (int) $row['id'], 'full_name' => $row['full_name']],
                    'linked_at' => $row['linked_at'],
                ];
            }
        }

        Response::json(200, $result);
    }

    /**
     * @return array{id: int, full_name: string, email: string, role: string, created_at: string}
     */
    private function findUser(int $id): array
    {
        $stmt = $this->pdo->prepare('SELECT id, full_name, email, role, created_at FROM users WHERE id = ?');
        $stmt->execute([$id]);
        $user = $stmt->fetch();
        $user['id'] = (int) $user['id'];

        return $user;
    }
}
