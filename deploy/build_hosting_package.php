<?php

declare(strict_types=1);

// Arma el paquete de despliegue para un hosting en build/hosting/ (no se versiona).
//
// Uso, desde la raíz del repo:
//   C:\xampp\php\php.exe deploy\build_hosting_package.php
//
// Estructura del paquete (se sube tal cual a la carpeta de la cuenta del hosting):
//   public_html/      raíz web: solo index.php y .htaccess (de api/public)
//   src/              código de la API (fuera de la web)
//   config/           carga de configuración (fuera de la web)
//   .env.example      plantilla; en el servidor se copia como .env (fuera de la web)
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

copyTree($root . '/api/public', $out . '/public_html');
copyTree($root . '/api/src', $out . '/src');
copyTree($root . '/api/config', $out . '/config');
copy($root . '/api/.env.hosting.example', $out . '/.env.example');
copy($root . '/bd/seed_hosting.sql', $out . '/bd/seed_hosting.sql');

// El hosting asigna la base de datos: se quitan CREATE DATABASE y USE
$schema = file_get_contents($root . '/bd/schema.sql');
$schema = preg_replace('/^CREATE DATABASE IF NOT EXISTS cuidadosis\s+CHARACTER SET utf8mb4\s+COLLATE utf8mb4_unicode_ci;\s*$/m', '', $schema, 1, $n1);
$schema = preg_replace('/^USE cuidadosis;\s*$/m', '', $schema, 1, $n2);
if ($n1 !== 1 || $n2 !== 1) {
    fwrite(STDERR, "No se encontró CREATE DATABASE o USE en bd/schema.sql; revisa el script.\n");
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
if (array_values($public) !== ['public_html/.htaccess', 'public_html/index.php']) {
    $errors[] = 'public_html debe contener solo index.php y .htaccess: ' . implode(', ', $public);
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

echo "Paquete en build/hosting/ con " . count($files) . " archivos (más MANIFIESTO.txt):\n";
foreach (array_keys($files) as $rel) {
    echo "  $rel\n";
}
