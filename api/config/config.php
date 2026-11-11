<?php

declare(strict_types=1);

use CuidaDosis\Env;

// Configuración de la API a partir de api/.env, con valores por defecto
date_default_timezone_set('America/Mexico_City');

$env = Env::load(__DIR__ . '/../.env');

$ttl = (int) ($env['TOKEN_TTL_HOURS'] ?? 0);

return [
    'db' => [
        'host' => $env['DB_HOST'] ?? '127.0.0.1',
        'port' => (int) ($env['DB_PORT'] ?? 3306),
        'name' => $env['DB_NAME'] ?? 'cuidadosis',
        'user' => $env['DB_USER'] ?? '',
        'pass' => $env['DB_PASS'] ?? '',
    ],
    'token_ttl_hours' => $ttl > 0 ? $ttl : 720,
    'debug' => strtolower($env['APP_DEBUG'] ?? 'false') === 'true',
];
