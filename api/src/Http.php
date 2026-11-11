<?php

declare(strict_types=1);

namespace CuidaDosis;

final class Request
{
    /** @var array<string, mixed>|null */
    private ?array $json = null;

    public function __construct(
        public readonly string $method,
        public readonly string $path,
        private readonly string $rawBody,
        private readonly string $authorization
    ) {
    }

    public static function fromGlobals(): self
    {
        $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

        // Ruta relativa al directorio de index.php (api/public)
        $uriPath = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
        $base = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '')), '/');
        if ($base !== '' && str_starts_with($uriPath, $base)) {
            $uriPath = substr($uriPath, strlen($base));
        }
        if (str_starts_with($uriPath, '/index.php')) {
            $uriPath = substr($uriPath, strlen('/index.php'));
        }
        $path = '/' . trim($uriPath, '/');

        $auth = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';

        return new self($method, $path, (string) file_get_contents('php://input'), (string) $auth);
    }

    /**
     * Cuerpo JSON como arreglo asociativo. Cuerpo vacío equivale a {}.
     *
     * @return array<string, mixed>
     */
    public function json(): array
    {
        if ($this->json !== null) {
            return $this->json;
        }
        if (trim($this->rawBody) === '') {
            return $this->json = [];
        }
        try {
            $data = json_decode($this->rawBody, true, 16, JSON_THROW_ON_ERROR);
        } catch (\JsonException) {
            throw new ApiException(400, 'El cuerpo de la petición no es JSON válido.');
        }
        if (!is_array($data) || array_is_list($data) && $data !== []) {
            throw new ApiException(400, 'El cuerpo de la petición debe ser un objeto JSON.');
        }

        return $this->json = $data;
    }

    public function bearerToken(): ?string
    {
        if (preg_match('/^Bearer\s+([A-Fa-f0-9]{64})$/', trim($this->authorization), $m) === 1) {
            return strtolower($m[1]);
        }

        return null;
    }
}

final class Response
{
    /**
     * @param array<string, mixed> $data
     */
    public static function json(int $status, array $data): void
    {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        header('X-Content-Type-Options: nosniff');
        header('Cache-Control: no-store');
        echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
    }

    /**
     * @param array<string, string> $details
     */
    public static function error(int $status, string $message, array $details = []): void
    {
        $body = ['error' => $message];
        if ($details !== []) {
            $body['details'] = $details;
        }
        self::json($status, $body);
    }
}
