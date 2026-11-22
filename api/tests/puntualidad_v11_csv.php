<?php

declare(strict_types=1);

// Puntualidad v1.1 en el teléfono a partir del log del sistema (adb logcat -v time).
//
// Uso: php api/tests/puntualidad_v11_csv.php <logcat.log> <escenario> <fecha YYYY-MM-DD> <HH:MM,HH:MM,...> [lead=5]
//
// En la v1.1 cada aviso tiene su propia alarma, así que cada línea
//   "[xiaomi_power]sending alarm Alarm{... origWhen <ms> ...} ... *walarm*:mx.cuidadosis.app.action.INTAKE_REMINDER"
// trae la hora esperada exacta del aviso (origWhen). La publicación de la notificación es la siguiente
// línea "acquireWakeLockInternal ... NotificationManagerService:post:mx.cuidadosis.app". El tipo de aviso
// se deduce de la hora esperada: T - lead (aviso previo) o T (alarma), según la lista de tomas.

if ($argc < 5) {
    fwrite(STDERR, "Uso: php puntualidad_v11_csv.php <logcat.log> <escenario> <fecha> <HH:MM,...> [lead]\n");
    exit(1);
}
[$_, $file, $scenario, $date, $timesArg] = $argv;
$lead = (int) ($argv[5] ?? 5);
date_default_timezone_set('America/Mexico_City');

$expected = []; // instante esperado (s) => [evento, toma]
foreach (explode(',', $timesArg) as $t) {
    $ts = strtotime("$date $t:00");
    $expected[$ts - $lead * 60] = ['T-' . $lead . ' (aviso previo)', $t];
    $expected[$ts] = ['T (alarma)', $t];
}

$rows = [];
$pending = null;
foreach (file($file) as $line) {
    if (!preg_match('/^\d\d-\d\d (\d\d:\d\d:\d\d\.\d+) /', $line, $m)) {
        continue;
    }
    $at = (float) strtotime("$date " . substr($m[1], 0, 8)) + (float) ('0.' . substr($m[1], 9));
    if (preg_match('/sending alarm .*origWhen (\d+) .*mx\.cuidadosis\.app\.action\.INTAKE_REMINDER/', $line, $a)) {
        $exp = intdiv((int) $a[1], 1000);
        if (isset($expected[$exp])) {
            $pending = ['exp' => $exp, 'sent' => $at];
        }
        continue;
    }
    if ($pending !== null && str_contains($line, 'NotificationManagerService:post:mx.cuidadosis.app')
        && str_contains($line, 'acquireWakeLockInternal')) {
        [$ev, $t] = $expected[$pending['exp']];
        $rows[$pending['exp']] = [$ev, $t, $pending['exp'], $pending['sent'], $at];
        $pending = null;
    }
}
ksort($rows);
ksort($expected);

$fmt = fn (float $s) => date('H:i:s', (int) $s) . sprintf('.%03d', (int) round(($s - floor($s)) * 1000) % 1000);
foreach ($expected as $exp => [$ev, $t]) {
    if (!isset($rows[$exp])) {
        echo implode(',', [$scenario, $ev, $t, date('H:i:s', $exp), 'no observada', '', '', 'no']), "\n";
        continue;
    }
    [, , , $sent, $post] = $rows[$exp];
    $diff = $post - $exp;
    echo implode(',', [
        $scenario, $ev, $t, date('H:i:s', $exp), $fmt($sent), $fmt($post),
        number_format($diff, 3, '.', ''), abs($diff) <= 60 ? 'sí' : 'no',
    ]), "\n";
}
