<?php

declare(strict_types=1);

// Mide el tiempo de respuesta de la consulta del día (RNF-07, meta 2 s).
//
// Uso (desde la raíz del repo, con Apache y MySQL encendidos y el seed cargado):
//   C:\xampp\php\php.exe api\tests\measure_today.php [REPETICIONES] [URL_BASE]
//
// Por defecto: 50 repeticiones contra http://localhost/CuidaDosis/api/public.
// Mide GET /intakes/today (paciente1) y GET /caregiver/patients/1/intakes/today (cuidador1),
// usuarios sintéticos del seed. Escribe docs/evidencias/p4/rendimiento.csv.

if (PHP_SAPI !== 'cli') {
    exit(1);
}

date_default_timezone_set('America/Mexico_City');

$reps = max(30, (int) ($argv[1] ?? 50));
$base = rtrim($argv[2] ?? 'http://localhost/CuidaDosis/api/public', '/');

function call(string $method, string $url, ?string $token, ?array $body = null): array
{
    $ch = curl_init($url);
    $headers = ['Accept: application/json'];
    if ($token !== null) {
        $headers[] = 'Authorization: Bearer ' . $token;
    }
    if ($body !== null) {
        $headers[] = 'Content-Type: application/json';
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    }
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 15,
        // Conexión nueva en cada petición, como una app que abre la pantalla
        CURLOPT_FORBID_REUSE => true,
        CURLOPT_FRESH_CONNECT => true,
    ]);
    $raw = curl_exec($ch);
    $code = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    $total = (float) curl_getinfo($ch, CURLINFO_TOTAL_TIME);
    curl_close($ch);

    return [$code, json_decode((string) $raw, true), $total];
}

function login(string $base, string $email): string
{
    [$code, $body] = call('POST', "$base/auth/login", null, ['email' => $email, 'password' => 'Sintetico2026']);
    if ($code !== 200) {
        fwrite(STDERR, "No se pudo iniciar sesión con $email (HTTP $code). ¿Está cargado el seed?\n");
        exit(1);
    }

    return $body['token'];
}

function stats(array $ms): array
{
    sort($ms);
    $n = count($ms);
    $p95 = $ms[(int) ceil(0.95 * $n) - 1];

    return [
        'n' => $n,
        'avg' => array_sum($ms) / $n,
        'min' => $ms[0],
        'max' => $ms[$n - 1],
        'p95' => $p95,
    ];
}

$tp = login($base, 'paciente1@ejemplo.test');
$tc = login($base, 'cuidador1@ejemplo.test');

$targets = [
    'GET /intakes/today (paciente)' => ["$base/intakes/today", $tp],
    'GET /caregiver/patients/1/intakes/today (cuidador)' => ["$base/caregiver/patients/1/intakes/today", $tc],
];

$started = date('Y-m-d H:i:s');
$rows = [];
$errors = 0;
foreach ($targets as $label => [$url, $token]) {
    call('GET', $url, $token); // calentamiento, no cuenta
    $ms = [];
    for ($i = 0; $i < $reps; $i++) {
        [$code, , $t] = call('GET', $url, $token);
        if ($code !== 200) {
            $errors++;
        }
        $ms[] = $t * 1000;
    }
    $rows[$label] = stats($ms);
}

$env = sprintf(
    'Cliente PHP %s cURL en la misma PC que el servidor (localhost); Apache 2.4.58, PHP 8.2.12, MariaDB 10.4.32 (XAMPP); %s; seed v2',
    PHP_VERSION,
    php_uname('s') . ' ' . php_uname('r')
);

$out = __DIR__ . '/../../docs/evidencias/p4/rendimiento.csv';
$fh = fopen($out, 'w');
fputcsv($fh, ['consulta', 'repeticiones', 'promedio_ms', 'minimo_ms', 'maximo_ms', 'p95_ms', 'meta_ms', 'cumple', 'errores_http', 'fecha', 'entorno']);
foreach ($rows as $label => $s) {
    fputcsv($fh, [
        $label, $s['n'], round($s['avg'], 1), round($s['min'], 1), round($s['max'], 1), round($s['p95'], 1),
        2000, $s['p95'] <= 2000 ? 'sí' : 'no', $errors, $started, $env,
    ]);
    printf("%s: n=%d promedio=%.1f ms min=%.1f max=%.1f p95=%.1f\n", $label, $s['n'], $s['avg'], $s['min'], $s['max'], $s['p95']);
}
fclose($fh);
echo "Errores HTTP: $errors\nCSV: docs/evidencias/p4/rendimiento.csv\n";
