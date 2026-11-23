<?php

declare(strict_types=1);

// Pruebas automáticas de la API de CuidaDosis contra el servidor local (Fase 6, paso 4).
//
// Uso (desde la raíz del repo, con Apache y MySQL de XAMPP encendidos):
//   C:\xampp\php\php.exe api\tests\run_api_tests.php [URL_BASE] [--keep]
//
// URL_BASE por defecto: http://localhost/CuidaDosis/api/public
// --keep conserva los usuarios de prueba; si no, se borran al final.
//
// Crea sus propios usuarios SINTETICO con correo @ejemplo.test, así que no depende
// del seed ni lo modifica. Usa la BD (api/.env) solo para simular el paso del tiempo
// de forma controlada: vencer un código y mover la hora de una toma.
// Guarda el log en docs/evidencias/p4/api-AAAAMMDD-HHMM.log.

use CuidaDosis\Database;
use CuidaDosis\Env;

if (PHP_SAPI !== 'cli') {
    exit(1);
}

require_once __DIR__ . '/../src/Env.php';
require_once __DIR__ . '/../src/Database.php';

$args = array_slice($argv, 1);
$keep = in_array('--keep', $args, true);
$args = array_values(array_filter($args, fn (string $a) => $a !== '--keep'));
$base = rtrim($args[0] ?? 'http://localhost/CuidaDosis/api/public', '/');

$config = require __DIR__ . '/../config/config.php';
$pdo = Database::connect($config['db']);

$started = new DateTimeImmutable('now');
$logDir = __DIR__ . '/../../docs/evidencias/p4';
if (!is_dir($logDir) && !mkdir($logDir, 0777, true) && !is_dir($logDir)) {
    fwrite(STDERR, "No se pudo crear $logDir\n");
    exit(1);
}
// Dos corridas en el mismo minuto no se sobrescriben: la segunda lleva sufijo -2, -3...
$logFile = $logDir . '/api-' . $started->format('Ymd-Hi') . '.log';
for ($n = 2; file_exists($logFile); $n++) {
    $logFile = $logDir . '/api-' . $started->format('Ymd-Hi') . "-$n.log";
}
$log = fopen($logFile, 'x');

$results = ['ok' => 0, 'fail' => 0];
$createdUsers = [];

function out(string $line): void
{
    global $log;
    echo $line, "\n";
    fwrite($log, $line . "\n");
}

/**
 * Petición HTTP. Devuelve [código, cuerpo decodificado, encabezados].
 *
 * @return array{0: int, 1: mixed, 2: array<string, string>}
 */
function http(string $method, string $path, ?string $token = null, ?array $body = null): array
{
    global $base;
    $ch = curl_init($base . $path);
    $headers = ['Accept: application/json'];
    if ($token !== null) {
        $headers[] = 'Authorization: Bearer ' . $token;
    }
    if ($body !== null) {
        $headers[] = 'Content-Type: application/json';
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body, JSON_UNESCAPED_UNICODE));
    }
    $respHeaders = [];
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 15,
        CURLOPT_HEADERFUNCTION => function ($ch, string $h) use (&$respHeaders): int {
            $parts = explode(':', $h, 2);
            if (count($parts) === 2) {
                $respHeaders[strtolower(trim($parts[0]))] = trim($parts[1]);
            }
            return strlen($h);
        },
    ]);
    $raw = curl_exec($ch);
    if ($raw === false) {
        $err = curl_error($ch);
        curl_close($ch);
        throw new RuntimeException("Sin respuesta de $base$path: $err");
    }
    $code = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    curl_close($ch);

    return [$code, json_decode((string) $raw, true), $respHeaders];
}

// Registra un caso: ID, entrada, esperado, obtenido y OK/FALLO
function check(string $id, string $input, string $expected, string $obtained, bool $pass): void
{
    global $results;
    $results[$pass ? 'ok' : 'fail']++;
    out(sprintf('[%s] %s', $pass ? 'OK   ' : 'FALLO', $id));
    out('    entrada:  ' . $input);
    out('    esperado: ' . $expected);
    out('    obtenido: ' . $obtained);
}

function brief(int $code, mixed $body): string
{
    $text = json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    if (strlen((string) $text) > 220) {
        $text = substr((string) $text, 0, 217) . '...';
    }

    return $code . ' ' . $text;
}

function register(string $label, string $role): array
{
    global $createdUsers, $started;
    static $n = 0;
    $n++;
    $email = sprintf('t%s%02d%s@ejemplo.test', $started->format('mdHis'), $n, $role === 'patient' ? 'p' : 'c');
    $body = [
        'full_name' => "$label SINTETICO prueba API",
        'email' => $email,
        'password' => 'Sintetico2026',
        'role' => $role,
    ];
    [$code, $resp] = http('POST', '/auth/register', null, $body);
    if ($code === 201) {
        $createdUsers[] = (int) $resp['user']['id'];
    }

    return ['code' => $code, 'resp' => $resp, 'email' => $email, 'token' => $resp['token'] ?? null,
        'id' => (int) ($resp['user']['id'] ?? 0)];
}

/** @return array<string, mixed>|null */
function findIntake(array $historyBody, int $intakeId): ?array
{
    foreach ($historyBody['days'] ?? [] as $day) {
        foreach ($day['intakes'] as $i) {
            if ($i['id'] === $intakeId) {
                return $i;
            }
        }
    }

    return null;
}

function uuid4(): string
{
    $b = random_bytes(16);
    $b[6] = chr((ord($b[6]) & 0x0f) | 0x40);
    $b[8] = chr((ord($b[8]) & 0x3f) | 0x80);
    $h = bin2hex($b);

    return sprintf('%s-%s-%s-%s-%s', substr($h, 0, 8), substr($h, 8, 4), substr($h, 12, 4), substr($h, 16, 4), substr($h, 20));
}

out('CuidaDosis: pruebas automáticas de la API');
out('Fecha y hora: ' . $started->format('Y-m-d H:i:s') . ' (America/Mexico_City)');
out('URL base: ' . $base);
out('Datos: usuarios SINTETICO creados por este script (@ejemplo.test)');
out(str_repeat('-', 72));

try {
    // ---------- Registro y sesión ----------
    $p = register('Paciente', 'patient');
    check('API-01 registro de paciente', 'POST /auth/register role=patient ' . $p['email'],
        '201 con token de 64 hex', brief($p['code'], ['role' => $p['resp']['user']['role'] ?? null,
            'token_len' => strlen((string) $p['token'])]),
        $p['code'] === 201 && preg_match('/^[0-9a-f]{64}$/', (string) $p['token']) === 1);

    $c = register('Cuidador', 'caregiver');
    $c2 = register('Cuidador intruso', 'caregiver');
    check('API-02 registro de cuidadores', 'POST /auth/register role=caregiver (2 cuentas)',
        '201 y 201', $c['code'] . ' y ' . $c2['code'], $c['code'] === 201 && $c2['code'] === 201);

    [$code, $body] = http('POST', '/auth/register', null, ['full_name' => 'X SINTETICO', 'email' => $p['email'],
        'password' => 'Sintetico2026', 'role' => 'patient']);
    check('API-03 correo repetido', 'POST /auth/register con ' . $p['email'], '409', brief($code, $body), $code === 409);

    [$code, $body] = http('POST', '/auth/login', null, ['email' => $p['email'], 'password' => 'Sintetico2026']);
    check('API-04 inicio de sesión', 'POST /auth/login credenciales correctas', '200 con token',
        brief($code, ['user' => $body['user']['email'] ?? null, 'has_token' => isset($body['token'])]),
        $code === 200 && isset($body['token']));

    [$code, $body] = http('POST', '/auth/login', null, ['email' => $p['email'], 'password' => 'incorrecta123']);
    check('API-05 contraseña incorrecta', 'POST /auth/login contraseña errónea', '401', brief($code, $body), $code === 401);

    [$code, $body] = http('GET', '/me', str_repeat('0', 64));
    check('API-06 token inválido', 'GET /me con token inexistente', '401', brief($code, $body), $code === 401);

    // ---------- Vínculo con aceptación ----------
    [$code, $body] = http('POST', '/links/invite', $p['token']);
    $codeStr = $body['invite_code'] ?? '';
    $ttl = $pdo->prepare('SELECT TIMESTAMPDIFF(SECOND, NOW(), expires_at) FROM cd_links WHERE invite_code = ?');
    $ttl->execute([$codeStr]);
    $ttlSec = (int) $ttl->fetchColumn();
    check('API-07 código de invitación', 'POST /links/invite (paciente)', '201, 8 caracteres, vence en 10 min (590 a 600 s)',
        brief($code, $body) . " | segundos para vencer: $ttlSec",
        $code === 201 && preg_match('/^[A-Z0-9]{8}$/', $codeStr) === 1 && $ttlSec >= 590 && $ttlSec <= 600);

    [$code, $body] = http('POST', '/links/accept', $c['token'], ['invite_code' => strtolower($codeStr)]);
    $linkId = (int) ($body['link']['id'] ?? 0);
    check('API-08 cuidador usa el código', "POST /links/accept {invite_code: $codeStr en minúsculas}",
        '200 y status awaiting_acceptance', brief($code, $body),
        $code === 200 && ($body['link']['status'] ?? '') === 'awaiting_acceptance');

    [$code, $body] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/today', $c['token']);
    check('API-09 sin aceptación no hay datos', 'GET /caregiver/patients/{id}/intakes/today antes de aceptar',
        '404', brief($code, $body), $code === 404);

    [$code, $body] = http('GET', '/links/pending', $p['token']);
    check('API-10 paciente ve la solicitud', 'GET /links/pending (paciente)', '200 con request.id = ' . $linkId,
        brief($code, $body), $code === 200 && ($body['request']['id'] ?? 0) === $linkId);

    [$code, $body] = http('POST', "/links/$linkId/confirm", $c['token']);
    check('API-11 el cuidador no puede aceptar', "POST /links/$linkId/confirm con token de cuidador", '403',
        brief($code, $body), $code === 403);

    [$code, $body] = http('POST', "/links/$linkId/confirm", $p['token']);
    check('API-12 paciente acepta', "POST /links/$linkId/confirm (paciente)", '200 y status active',
        brief($code, $body), $code === 200 && ($body['link']['status'] ?? '') === 'active');

    [$code, $body] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/today', $c['token']);
    check('API-13 cuidador con vínculo activo', 'GET /caregiver/patients/{id}/intakes/today', '200',
        brief($code, $body), $code === 200);

    [$code, $body] = http('POST', '/links/accept', $c2['token'], ['invite_code' => $codeStr]);
    check('API-14 código de un solo uso', 'POST /links/accept con el mismo código (otro cuidador)', '404',
        brief($code, $body), $code === 404);

    // ---------- Código vencido (tiempo simulado en BD) ----------
    $p2 = register('Paciente dos', 'patient');
    [, $body] = http('POST', '/links/invite', $p2['token']);
    $expired = $body['invite_code'] ?? '';
    $pdo->prepare('UPDATE cd_links SET expires_at = NOW() - INTERVAL 1 SECOND WHERE invite_code = ?')->execute([$expired]);
    [$code, $body] = http('POST', '/links/accept', $c2['token'], ['invite_code' => $expired]);
    check('API-15 código vencido', "POST /links/accept $expired con expires_at movido 1 s al pasado en BD",
        '410 con mensaje de código vencido', brief($code, $body), $code === 410);

    // ---------- 5 intentos fallidos y luego 429 ----------
    $c3 = register('Cuidador adivinador', 'caregiver');
    $codes = [];
    for ($i = 1; $i <= 5; $i++) {
        [$code] = http('POST', '/links/accept', $c3['token'], ['invite_code' => 'ZZZZZZZ' . $i]);
        $codes[] = $code;
    }
    check('API-16 cinco intentos fallidos', 'POST /links/accept x5 con códigos inexistentes ZZZZZZZ1..5',
        '404 x5', implode(', ', $codes), $codes === [404, 404, 404, 404, 404]);

    [$code, $body, $hdr] = http('POST', '/links/accept', $c3['token'], ['invite_code' => 'ZZZZZZZ6']);
    check('API-17 sexto intento bloqueado', 'POST /links/accept (6.º intento en menos de 10 min)',
        '429 con Retry-After', brief($code, $body) . ' | Retry-After: ' . ($hdr['retry-after'] ?? 'ausente'),
        $code === 429 && (int) ($hdr['retry-after'] ?? 0) > 0);

    [, $body] = http('POST', '/links/invite', $p2['token']);
    $valid = $body['invite_code'] ?? '';
    [$code, $body] = http('POST', '/links/accept', $c3['token'], ['invite_code' => $valid]);
    $stmt = $pdo->prepare('SELECT COUNT(*) FROM cd_link_attempts WHERE user_id = ? AND success = 0');
    $stmt->execute([$c3['id']]);
    $fails = (int) $stmt->fetchColumn();
    check('API-18 bloqueo también para código válido', "POST /links/accept $valid (válido) durante el bloqueo",
        '429 y siguen 5 fallos registrados en cd_link_attempts', brief($code, $body) . " | fallos: $fails",
        $code === 429 && $fails === 5);

    // ---------- Medicamento para las pruebas de tomas ----------
    [$code, $body] = http('POST', '/medications', $p['token'], [
        'name' => 'Medicamento SINTETICO prueba API',
        'dose' => '1 unidad (dato de prueba)',
        'times' => ['00:00', '00:01'],
    ]);
    $medId = (int) ($body['medication']['id'] ?? 0);
    check('API-19 alta de medicamento', 'POST /medications con horas 00:00 y 00:01', '201', brief($code, $body), $code === 201);

    [$code, $today] = http('GET', '/intakes/today', $p['token']);
    $byTime = [];
    foreach ($today['intakes'] ?? [] as $i) {
        $byTime[$i['time']] = $i;
    }
    $first = $byTime['00:00'] ?? null;
    $second = $byTime['00:01'] ?? null;

    // ---------- Sincronización idempotente ----------
    $uuid = uuid4();
    $now = (string) $pdo->query("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')")->fetchColumn();
    $batch = ['confirmations' => [[
        'client_uuid' => $uuid,
        'schedule_id' => $first['schedule_id'] ?? 0,
        'scheduled_for' => $first['scheduled_for'] ?? '',
        'confirmed_at' => $now,
    ]]];
    [$code1, $b1] = http('POST', '/intakes/sync', $p['token'], $batch);
    [$code2, $b2] = http('POST', '/intakes/sync', $p['token'], $batch);
    $stmt = $pdo->prepare('SELECT COUNT(*) FROM cd_intake_logs WHERE client_uuid = ?');
    $stmt->execute([$uuid]);
    $rowsUuid = (int) $stmt->fetchColumn();
    $stmt = $pdo->prepare('SELECT COUNT(*) FROM cd_intake_logs WHERE schedule_id = ? AND scheduled_for = ?');
    $stmt->execute([$first['schedule_id'] ?? 0, $first['scheduled_for'] ?? '']);
    $rowsIntake = (int) $stmt->fetchColumn();
    check('API-20 sincronización repetida con el mismo UUID', "POST /intakes/sync dos veces con client_uuid $uuid",
        '200 created, luego 200 duplicate; 1 fila con ese UUID y 1 toma',
        "$code1 " . ($b1['results'][0]['status'] ?? '?') . ", $code2 " . ($b2['results'][0]['status'] ?? '?')
            . " | filas UUID: $rowsUuid, filas toma: $rowsIntake",
        $code1 === 200 && $code2 === 200 && ($b1['results'][0]['status'] ?? '') === 'created'
            && ($b2['results'][0]['status'] ?? '') === 'duplicate' && $rowsUuid === 1 && $rowsIntake === 1);

    $stmt = $pdo->prepare('SELECT last_sync_at IS NOT NULL AND received_at IS NOT NULL FROM cd_users u
        JOIN cd_intake_logs l ON l.client_uuid = ? WHERE u.id = ?');
    $stmt->execute([$uuid, $p['id']]);
    $stamped = (int) $stmt->fetchColumn() === 1;
    [$code, $body] = http('GET', '/caregiver/patients', $c['token']);
    $listed = null;
    foreach ($body['patients'] ?? [] as $row) {
        if ($row['id'] === $p['id']) {
            $listed = $row;
        }
    }
    check('API-21 received_at y last_sync_at', 'BD tras la sincronización y GET /caregiver/patients',
        'received_at y last_sync_at guardados; el cuidador ve last_sync_at',
        'guardados: ' . ($stamped ? 'sí' : 'no') . ' | last_sync_at en lista: ' . ($listed['last_sync_at'] ?? 'null'),
        $stamped && ($listed['last_sync_at'] ?? null) !== null);

    [$code, $body] = http('POST', '/intakes/sync', $p2['token'], ['confirmations' => [[
        'client_uuid' => uuid4(), 'schedule_id' => $first['schedule_id'] ?? 0,
        'scheduled_for' => $first['scheduled_for'] ?? '', 'confirmed_at' => $now,
    ]]]);
    check('API-22 sincronizar horario ajeno', 'POST /intakes/sync de otro paciente con schedule_id ajeno',
        '200 con status rejected', brief($code, $body), $code === 200 && ($body['results'][0]['status'] ?? '') === 'rejected');

    // ---------- Sin confirmar a T+30 (hora de la toma movida en BD) ----------
    $secondId = (int) ($second['id'] ?? 0);
    $move = $pdo->prepare('UPDATE cd_intake_logs SET scheduled_for = NOW() - INTERVAL ? SECOND WHERE id = ?');
    $states = [];
    foreach ([29 * 60 => 'pending', 31 * 60 => 'unconfirmed'] as $ago => $expected) {
        $move->execute([$ago, $secondId]);
        [, $hist] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/history', $c['token']);
        $states[$ago / 60] = findIntake($hist, $secondId)['state'] ?? 'no encontrada';
    }
    [, $hist] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/history', $c['token']);
    $confirmedState = findIntake($hist, (int) ($first['id'] ?? 0))['state'] ?? 'no encontrada';
    $stored = $pdo->prepare('SELECT status FROM cd_intake_logs WHERE id = ?');
    $stored->execute([$secondId]);
    $storedStatus = (string) $stored->fetchColumn();
    check('API-23 estado calculado Pendiente / Sin confirmar / Confirmada',
        'toma pendiente con scheduled_for = ahora - 29 min y luego ahora - 31 min; toma sincronizada',
        'pending, unconfirmed, confirmed; en BD sigue status pending',
        "T+29: {$states[29]}, T+31: {$states[31]}, sincronizada: $confirmedState | BD: $storedStatus",
        $states[29] === 'pending' && $states[31] === 'unconfirmed' && $confirmedState === 'confirmed'
            && $storedStatus === 'pending');

    [$code, $body] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/today', $c['token']);
    $hasDose = false;
    foreach ($body['intakes'] ?? [] as $i) {
        $hasDose = $hasDose || array_key_exists('dose', $i);
    }
    check('API-24 dosis oculta al cuidador (G-07)', 'GET /caregiver/patients/{id}/intakes/today con share_dose = 0',
        '200 sin campo dose y patient.share_dose = false',
        $code . ' | dose presente: ' . ($hasDose ? 'sí' : 'no') . ' | share_dose: ' . json_encode($body['patient']['share_dose'] ?? null),
        $code === 200 && !$hasDose && ($body['patient']['share_dose'] ?? null) === false);

    // ---------- Acceso sin vínculo activo ----------
    [$code, $body] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/today', $c2['token']);
    check('API-25 cuidador sin vínculo', 'GET /caregiver/patients/{id}/intakes/today con cuidador no vinculado',
        '404', brief($code, $body), $code === 404);

    [$code, $body] = http('GET', '/intakes/today', $c['token']);
    check('API-26 cuidador en endpoint de paciente', 'GET /intakes/today con token de cuidador', '403',
        brief($code, $body), $code === 403);

    [$code, $body] = http('DELETE', '/links/active', $p['token']);
    [$codeT, $bodyT] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/today', $c['token']);
    [$codeH] = http('GET', '/caregiver/patients/' . $p['id'] . '/intakes/history', $c['token']);
    $stmt = $pdo->prepare("SELECT status, revoked_at IS NOT NULL FROM cd_links WHERE id = ?");
    $stmt->execute([$linkId]);
    $linkRow = $stmt->fetch(PDO::FETCH_NUM);
    check('API-27 acceso perdido al revocar', 'DELETE /links/active (paciente) y luego el cuidador consulta hoy e historial',
        '200; luego 404 y 404; vínculo revoked con revoked_at',
        "$code; luego $codeT y $codeH | BD: " . ($linkRow[0] ?? '?') . ', revoked_at: ' . (($linkRow[1] ?? 0) ? 'sí' : 'no'),
        $code === 200 && $codeT === 404 && $codeH === 404 && ($linkRow[0] ?? '') === 'revoked' && (int) $linkRow[1] === 1);
} catch (Throwable $e) {
    $results['fail']++;
    out('[FALLO] Error inesperado: ' . $e->getMessage());
}

out(str_repeat('-', 72));
if (!$keep && $createdUsers !== []) {
    // Borra solo los usuarios creados por este script (las llaves foráneas borran en cascada)
    $in = implode(',', array_fill(0, count($createdUsers), '?'));
    $pdo->prepare("DELETE FROM cd_users WHERE id IN ($in) AND email LIKE '%@ejemplo.test'")->execute($createdUsers);
    out('Limpieza: se borraron ' . count($createdUsers) . ' usuarios de prueba y sus datos.');
} elseif ($keep) {
    out('Limpieza omitida (--keep): usuarios de prueba ' . implode(', ', $createdUsers));
}
out(sprintf('Resultado: %d OK, %d FALLO', $results['ok'], $results['fail']));
out('Log: docs/evidencias/p4/' . basename($logFile));
fclose($log);

exit($results['fail'] === 0 ? 0 : 1);
