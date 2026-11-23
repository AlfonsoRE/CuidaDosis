<?php

declare(strict_types=1);

// Prueba sin red ni BD de Request::routePath: la API debe encontrar sus rutas en XAMPP
// (/CuidaDosis/api/public) y en una subcarpeta del hosting (/cuidadosis/api), con o sin
// APP_BASE_PATH.
//
// Uso: C:\xampp\php\php.exe api\tests\route_path_test.php

use CuidaDosis\Request;

if (PHP_SAPI !== 'cli') {
    exit(1);
}

require_once __DIR__ . '/../src/ApiException.php';
require_once __DIR__ . '/../src/Http.php';

// [URL pedida, APP_BASE_PATH, directorio de index.php (SCRIPT_NAME), ruta esperada]
$cases = [
    ['/CuidaDosis/api/public/', '', '/CuidaDosis/api/public', '/'],
    ['/CuidaDosis/api/public/auth/login', '', '/CuidaDosis/api/public', '/auth/login'],
    ['/CuidaDosis/api/public/index.php/me', '', '/CuidaDosis/api/public', '/me'],
    ['/cuidadosis/api/', '', '/cuidadosis/api', '/'],
    ['/cuidadosis/api/caregiver/patients/5/intakes/today', '', '/cuidadosis/api', '/caregiver/patients/5/intakes/today'],
    ['/cuidadosis/api/me', '/cuidadosis/api', '/cuidadosis/api', '/me'],
    ['/cuidadosis/api/me', '/cuidadosis/api/', '/cuidadosis/api', '/me'],
    ['/cuidadosis/api/me', 'cuidadosis/api', '', '/me'],
    // El hosting reescribe a un index.php de otra carpeta: manda APP_BASE_PATH
    ['/cuidadosis/api/medications/3', '/cuidadosis/api', '/cuidadosis', '/medications/3'],
    // APP_BASE_PATH de otro entorno: se ignora y se usa el directorio de index.php
    ['/CuidaDosis/api/public/me', '/cuidadosis/api', '/CuidaDosis/api/public', '/me'],
    // Prefijo por segmentos completos
    ['/cuidadosis/apix/me', '/cuidadosis/api', '/cuidadosis/api', '/cuidadosis/apix/me'],
    // API en la raíz del dominio (paquete sin --subdir)
    ['/me', '', '/', '/me'],
    ['/', '', '\\', '/'],
];

$fail = 0;
foreach ($cases as $i => [$uri, $base, $dir, $expected]) {
    $got = Request::routePath($uri, $base, $dir);
    $ok = $got === $expected;
    $fail += $ok ? 0 : 1;
    printf("[%s] %2d %s (APP_BASE_PATH='%s', dir='%s') -> %s%s\n", $ok ? 'OK' : 'FALLO', $i + 1, $uri, $base, $dir, $got, $ok ? '' : " (esperado $expected)");
}
printf("Resultado: %d OK, %d FALLO\n", count($cases) - $fail, $fail);
exit($fail === 0 ? 0 : 1);
