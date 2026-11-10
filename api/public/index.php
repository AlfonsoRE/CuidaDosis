<?php

declare(strict_types=1);

// Punto de entrada temporal (Fase 0); se reemplaza en la Fase 2
header('Content-Type: application/json; charset=utf-8');
echo json_encode(['status' => 'ok']);
