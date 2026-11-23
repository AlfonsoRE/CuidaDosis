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

    /**
     * @param string $basePath APP_BASE_PATH del .env (por ejemplo "/cuidadosis/api"); vacío = se deduce
     */
    public static function fromGlobals(string $basePath = ''): self
    {
        $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
        $uriPath = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
        $scriptDir = str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? ''));
        $path = self::routePath($uriPath, $basePath, $scriptDir);

        $auth = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';

        return new self($method, $path, (string) file_get_contents('php://input'), (string) $auth);
    }

    /**
     * Ruta de la API sin el prefijo del sitio. Si APP_BASE_PATH está definido y la URL empieza
     * con él, se quita ese prefijo; si no, se quita el directorio de index.php (SCRIPT_NAME),
     * que funciona igual en XAMPP (/CuidaDosis/api/public) y en una subcarpeta del hosting.
     */
    public static function routePath(string $uriPath, string $basePath, string $scriptDir): string
    {
        $base = self::normalizeBase($basePath);
        if ($base === '' || !self::hasPrefix($uriPath, $base)) {
            $base = self::normalizeBase($scriptDir);
        }
        if ($base !== '' && self::hasPrefix($uriPath, $base)) {
            $uriPath = substr($uriPath, strlen($base));
        }
        if ($uriPath === '/index.php' || str_starts_with($uriPath, '/index.php/')) {
            $uriPath = substr($uriPath, strlen('/index.php'));
        }

        return '/' . trim($uriPath, '/');
    }

    private static function normalizeBase(string $base): string
    {
        $base = trim(str_replace('\\', '/', trim($base)), '/');

        return $base === '' || $base === '.' ? '' : '/' . $base;
    }

    // Prefijo completo por segmentos: "/cuidadosis/api" no coincide con "/cuidadosis/apix"
    private static function hasPrefix(string $path, string $base): bool
    {
        return $path === $base || str_starts_with($path, $base . '/');
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
