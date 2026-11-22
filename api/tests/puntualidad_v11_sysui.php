<?php

declare(strict_types=1);

// Uso: php api/tests/puntualidad_v11_sysui.php <logcat.log> <escenario> <fecha YYYY-MM-DD> <HH:MM,...>
// Puntualidad por id de notificación (id = hora*60 + minuto de la toma) a partir de las líneas de SystemUI.
// Útil con la pantalla encendida, cuando el sistema no registra el bloqueo de publicación.
date_default_timezone_set('America/Mexico_City');
[$_, $file, $scenario, $date, $timesArg] = $argv;
$lines = file($file);
$out = [];
foreach (explode(',', $timesArg) as $t) {
    [$h, $mi] = array_map('intval', explode(':', $t));
    $id = $h * 60 + $mi;
    $T = strtotime("$date $t:00");
    foreach ([['T-5 (aviso previo)', $T - 300], ['T (alarma)', $T]] as [$ev, $exp]) {
        $first = null;
        foreach ($lines as $l) {
            if (!str_contains($l, "|mx.cuidadosis.app|$id|")) {
                continue;
            }
            if (!preg_match('/^\d\d-\d\d (\d\d:\d\d:\d\d)\.(\d+) /', $l, $m)) {
                continue;
            }
            $at = strtotime("$date {$m[1]}") + (float) ('0.' . $m[2]);
            if ($at >= $exp - 1 && $at < $exp + 90) {
                $first = $at;
                break;
            }
        }
        $out[] = [$exp, $first === null
            ? [$scenario, $ev, $t, date('H:i:s', $exp), 'no observada', '', 'no']
            : [$scenario, $ev, $t, date('H:i:s', $exp), date('H:i:s', (int) $first) . sprintf('.%03d', (int) round(($first - floor($first)) * 1000) % 1000), number_format($first - $exp, 3, '.', ''), abs($first - $exp) <= 60 ? 'sí' : 'no']];
    }
}
usort($out, fn ($a, $b) => $a[0] <=> $b[0]);
foreach ($out as [, $r]) {
    echo implode(',', $r), "\n";
}
