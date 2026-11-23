<?php

declare(strict_types=1);

// Arma el paquete de despliegue para un hosting en build/hosting/ (no se versiona).
//
// Uso, desde la raíz del repo:
//   C:\xampp\php\php.exe deploy\build_hosting_package.php                     (API en la raíz del dominio)
//   C:\xampp\php\php.exe deploy\build_hosting_package.php --subdir=cuidadosis  (API en /cuidadosis/api/)
//
// Estructura del paquete sin --subdir (se sube tal cual a la carpeta de la cuenta del hosting):
//   public_html/      raíz web: solo index.php y .htaccess (de api/public)
//   src/              código de la API (fuera de la web)
//   config/           carga de configuración (fuera de la web)
//   .env.example      plantilla; en el servidor se copia como .env (fuera de la web)
//
// Con --subdir=NOMBRE (dominio compartido con otros proyectos):
//   public_html/NOMBRE/api/  solo index.php (apunta a ../../../NOMBRE_app) y .htaccess
//   NOMBRE_app/              src/, config/, .env.example y un .htaccess que niega todo (fuera de la web)
//   bd/schema_hosting.sql  esquema sin CREATE DATABASE ni USE (se importa en la base asignada)
//   bd/seed_hosting.sql    sin cuentas de demostración
//   MANIFIESTO.txt    lista de archivos con su SHA-256
//
// Nunca incluye api/.env ni datos de prueba. No se conecta a ningún servidor.

if (PHP_SAPI !== 'cli') {
    exit(1);
}

$root = realpath(__DIR__ . '/..');
$out = $root . '/build/hosting';

$subdir = '';
foreach (array_slice($argv, 1) as $arg) {
    if (preg_match('/^--subdir=([a-z0-9_-]{1,40})$/', $arg, $m) === 1) {
        $subdir = $m[1];
    } else {
        fwrite(STDERR, "Argumento no válido: $arg (uso: --subdir=nombre, con minúsculas, números, - o _)\n");
        exit(1);
    }
}
$webRel = $subdir === '' ? 'public_html' : "public_html/$subdir/api";
$appRel = $subdir === '' ? '' : $subdir . '_app';
$appOut = $appRel === '' ? $out : "$out/$appRel";

function rmTree(string $dir): void
{
    if (!is_dir($dir)) {
        return;
    }
    $it = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($dir, FilesystemIterator::SKIP_DOTS),
        RecursiveIteratorIterator::CHILD_FIRST
    );
    foreach ($it as $f) {
        $f->isDir() ? rmdir($f->getPathname()) : unlink($f->getPathname());
    }
    rmdir($dir);
}

function copyTree(string $from, string $to): void
{
    if (!is_dir($to)) {
        mkdir($to, 0777, true);
    }
    foreach (new DirectoryIterator($from) as $f) {
        if ($f->isDot()) {
            continue;
        }
        $target = $to . '/' . $f->getFilename();
        $f->isDir() ? copyTree($f->getPathname(), $target) : copy($f->getPathname(), $target);
    }
}

// Solo se borra la carpeta de salida del propio script
rmTree($out);
mkdir($out . '/bd', 0777, true);

copyTree($root . '/api/public', "$out/$webRel");
copyTree($root . '/api/src', $appOut . '/src');
copyTree($root . '/api/config', $appOut . '/config');
copy($root . '/api/.env.hosting.example', $appOut . '/.env.example');
copy($root . '/bd/seed_hosting.sql', $out . '/bd/seed_hosting.sql');

if ($subdir !== '') {
    // index.php busca src/, config/ y .env en ../../../NOMBRE_app (fuera de public_html)
    $indexFile = "$out/$webRel/index.php";
    $index = str_replace(
        "\$appRoot = __DIR__ . '/..';",
        "\$appRoot = __DIR__ . '/../../../$appRel';",
        file_get_contents($indexFile),
        $nr
    );
    if ($nr !== 1) {
        fwrite(STDERR, "No se encontró la línea \$appRoot en api/public/index.php; revisa el script.\n");
        exit(1);
    }
    file_put_contents($indexFile, $index);
    // Defensa extra por si la carpeta de la app se sube dentro de la web por error
    file_put_contents("$appOut/.htaccess", "# Carpeta privada de CuidaDosis: nunca debe servirse por web\nRequire all denied\n");
}

// El hosting asigna la base de datos: se quitan CREATE DATABASE y USE
$schema = file_get_contents($root . '/bd/schema.sql');
$schema = preg_replace('/^CREATE DATABASE IF NOT EXISTS cuidadosis\s+CHARACTER SET utf8mb4\s+COLLATE utf8mb4_unicode_ci;\s*$/m', '', $schema, 1, $n1);
$schema = preg_replace('/^USE cuidadosis;\s*$/m', '', $schema, 1, $n2);
if ($n1 !== 1 || $n2 !== 1) {
    fwrite(STDERR, "No se encontró CREATE DATABASE o USE en bd/schema.sql; revisa el script.\n");
    exit(1);
}
// La base puede ser compartida con otros proyectos: el esquema solo puede tocar tablas cd_
preg_match_all('/\b(?:CREATE TABLE|DROP TABLE(?: IF EXISTS)?|TRUNCATE(?: TABLE)?|REFERENCES|ALTER TABLE)\s+`?(\w+)/i', $schema, $tm);
$bad = array_unique(array_filter($tm[1], fn (string $t) => !str_starts_with($t, 'cd_')));
if ($tm[1] === [] || $bad !== []) {
    fwrite(STDERR, 'bd/schema.sql debe tocar solo tablas con prefijo cd_. Sin prefijo: ' . implode(', ', $bad) . "\n");
    exit(1);
}
$header = "-- CuidaDosis: esquema para hosting (generado de bd/schema.sql por deploy/build_hosting_package.php)\n"
    . "-- Importar en la base de datos que asigna el hosting. Borra y recrea las tablas.\n";
file_put_contents($out . '/bd/schema_hosting.sql', $header . $schema);

// Comprobaciones de seguridad del paquete
$files = [];
$it = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($out, FilesystemIterator::SKIP_DOTS));
foreach ($it as $f) {
    $rel = str_replace('\\', '/', substr($f->getPathname(), strlen($out) + 1));
    $files[$rel] = hash_file('sha256', $f->getPathname());
}
ksort($files);
$errors = [];
foreach (array_keys($files) as $rel) {
    if ($rel === '.env' || str_ends_with($rel, '/.env')) {
        $errors[] = "El paquete contiene un .env: $rel";
    }
}
$public = array_filter(array_keys($files), fn (string $r) => str_starts_with($r, 'public_html/'));
if (array_values($public) !== ["$webRel/.htaccess", "$webRel/index.php"]) {
    $errors[] = "public_html debe contener solo $webRel/index.php y .htaccess: " . implode(', ', $public);
}
if ($errors !== []) {
    fwrite(STDERR, implode("\n", $errors) . "\n");
    exit(1);
}

$manifest = "CuidaDosis: paquete para hosting\nGenerado: " . date('Y-m-d H:i:s') . "\n\n";
foreach ($files as $rel => $sha) {
    $manifest .= "$sha  $rel\n";
}
file_put_contents($out . '/MANIFIESTO.txt', $manifest);

echo 'Paquete en build/hosting/ (' . ($subdir === '' ? 'API en la raíz del dominio' : "API en /$subdir/api/") . ') con ' . count($files) . " archivos (más MANIFIESTO.txt):\n";
foreach (array_keys($files) as $rel) {
    echo "  $rel\n";
}
