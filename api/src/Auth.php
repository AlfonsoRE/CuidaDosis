<?php

declare(strict_types=1);

namespace CuidaDosis;

use PDO;

// Tokens de sesión: el cliente recibe el token en claro; en BD solo se guarda su SHA-256
final class Auth
{
    public function __construct(private readonly PDO $pdo, private readonly int $ttlHours)
    {
    }

    /**
     * @return array{token: string, expires_at: string}
     */
    public function issueToken(int $userId): array
    {
        $token = bin2hex(random_bytes(32));
        $expires = (new \DateTimeImmutable("+{$this->ttlHours} hours"))->format('Y-m-d H:i:s');

        // Limpieza oportunista de tokens vencidos del mismo usuario
        $this->pdo->prepare('DELETE FROM api_tokens WHERE user_id = ? AND expires_at <= NOW()')
            ->execute([$userId]);
        $this->pdo->prepare('INSERT INTO api_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)')
            ->execute([$userId, hash('sha256', $token), $expires]);

        return ['token' => $token, 'expires_at' => $expires];
    }

    public function revokeToken(string $token): void
    {
        $this->pdo->prepare('DELETE FROM api_tokens WHERE token_hash = ?')
            ->execute([hash('sha256', $token)]);
    }

    /**
     * Usuario autenticado por el token Bearer, o error 401.
     *
     * @return array{id: int, full_name: string, email: string, role: string}
     */
    public function user(Request $request): array
    {
        $token = $request->bearerToken();
        if ($token === null) {
            throw new ApiException(401, 'Falta el token de sesión o no es válido.');
        }
        $stmt = $this->pdo->prepare(
            'SELECT u.id, u.full_name, u.email, u.role
               FROM api_tokens t
               JOIN users u ON u.id = t.user_id
              WHERE t.token_hash = ? AND t.expires_at > NOW()'
        );
        $stmt->execute([hash('sha256', $token)]);
        $user = $stmt->fetch();
        if ($user === false) {
            throw new ApiException(401, 'La sesión no es válida o ya expiró. Inicia sesión de nuevo.');
        }
        $user['id'] = (int) $user['id'];

        return $user;
    }

    /**
     * Usuario autenticado que además tiene el rol indicado, o error 403.
     *
     * @return array{id: int, full_name: string, email: string, role: string}
     */
    public function requireRole(Request $request, string $role): array
    {
        $user = $this->user($request);
        if ($user['role'] !== $role) {
            $label = $role === 'patient' ? 'paciente' : 'cuidador';
            throw new ApiException(403, "Esta acción solo está permitida para el rol $label.");
        }

        return $user;
    }
}
