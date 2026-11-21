<?php

declare(strict_types=1);

// Convierte el logcat de la app (adb logcat -v time -s CuidaDosis:I) en el CSV de puntualidad (RNF-01).
//
// Uso: php api/tests/puntualidad_csv.php <logcat.log> <escenario> <fecha YYYY-MM-DD> [monitor.log] >> salida.csv
//
// La app escribe "Próximo aviso <TIPO>: toma de <T>, disparo <instante UTC>" cada vez que programa
// la alarma, y lo hace justo después de mostrar un aviso. Un disparo se reconoce porque la línea
// aparece cuando ya llegó la hora del evento programado antes. La hora de esa línea es la de
// publicación; la esperada es T - 5 min (BEFORE) o T (DUE). Si se da el monitor de notificaciones
// (líneas "HH:MM:SS android.title=..."), se agrega la primera vez que se vio el título.

if ($argc < 4) {
    fwrite(STDERR, "Uso: php puntualidad_csv.php <logcat.log> <escenario> <fecha> [monitor.log]\n");
    exit(1);
}
[$_, $file, $scenario, $date] = $argv;
$monitor = $argv[4] ?? null;
date_default_timezone_set('America/Mexico_City');

// Hora de publicación de cada notificación según dumpsys (campo when, en ms), por título
$seen = [];
if ($monitor !== null && is_file($monitor)) {
    foreach (file($monitor) as $line) {
        if (preg_match_all('/when=(\d+)\/\d+\s+android\.title=String \(([^)]*)\)/', $line, $mm, PREG_SET_ORDER)) {
            foreach ($mm as $x) {
                $at = DateTimeImmutable::createFromFormat('U.u', sprintf('%d.%03d', intdiv((int) $x[1], 1000), (int) $x[1] % 1000))
                    ->setTimezone(new DateTimeZone('America/Mexico_City'));
                $seen[$x[2]] ??= $at;
            }
        }
    }
}

$pending = null; // evento programado: [tipo, T local, instante esperado]
$rows = [];
foreach (file($file) as $line) {
    if (!preg_match('/^(\d\d-\d\d) (\d\d:\d\d:\d\d\.\d+) .*Próximo aviso (\w+): toma de (\S+) .*disparo (\S+)Z/u', $line, $m)) {
        continue;
    }
    $logAt = new DateTimeImmutable("$date {$m[2]}");
    if ($pending !== null && $logAt >= $pending['expected']->modify('-1 second')) {
        $title = $pending['kind'] === 'BEFORE'
            ? 'Toma de las ' . $pending['t']->format('H:i')
            : 'Es la hora de tu toma de las ' . $pending['t']->format('H:i');
        // Hora real: publicación de la notificación (dumpsys); si no se vio, la del log de la app
        $real = $seen[$title] ?? $logAt;
        $source = isset($seen[$title]) ? 'dumpsys notification (when)' : 'logcat de la app';
        $diff = (float) $real->format('U.u') - (float) $pending['expected']->format('U.u');
        $rows[] = [
            $scenario,
            $pending['kind'] === 'BEFORE' ? 'T-5 (aviso previo)' : 'T (segundo aviso)',
            $pending['t']->format('H:i'),
            $pending['expected']->format('H:i:s'),
            $real->format('H:i:s.v'),
            number_format($diff, 3, '.', ''),
            abs($diff) <= 60 ? 'sí' : 'no',
            $source,
            $logAt->format('H:i:s.v'),
        ];
        $pending = null;
    }
    $t = new DateTimeImmutable(str_replace('T', ' ', $m[4]));
    $expected = (new DateTimeImmutable($m[5] . 'Z'))->setTimezone(new DateTimeZone('America/Mexico_City'));
    // Instante ideal: T - 5 o T, aunque el disparo se adelante si ya pasó
    $ideal = $m[3] === 'BEFORE' ? $t->modify('-5 minutes') : $t;
    $pending = ['kind' => $m[3], 't' => $t, 'expected' => max($ideal, $expected)];
}

foreach ($rows as $r) {
    echo implode(',', array_map(fn ($v) => str_contains((string) $v, ',') ? "\"$v\"" : $v, $r)), "\n";
}
