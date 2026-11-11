<?php

declare(strict_types=1);

namespace CuidaDosis;

use PDO;

final class Database
{
    /**
     * @param array{host: string, port: int, name: string, user: string, pass: string} $cfg
     */
    public static function connect(array $cfg): PDO
    {
        $dsn = sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $cfg['host'], $cfg['port'], $cfg['name']);
        $pdo = new PDO($dsn, $cfg['user'], $cfg['pass'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);

        // MariaDB de XAMPP no es estricto por defecto: valores inválidos deben dar error
        $pdo->exec("SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'");

        // Offset de America/Mexico_City calculado en PHP (XAMPP no trae tablas de zonas)
        $offset = (new \DateTimeImmutable('now'))->format('P');
        $stmt = $pdo->prepare('SET time_zone = ?');
        $stmt->execute([$offset]);

        return $pdo;
    }
}
