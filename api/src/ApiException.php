<?php

declare(strict_types=1);

namespace CuidaDosis;

// Error controlado de la API: código HTTP, mensaje en español y detalles opcionales
final class ApiException extends \RuntimeException
{
    /**
     * @param array<string, string> $details
     */
    public function __construct(
        public readonly int $status,
        string $message,
        public readonly array $details = []
    ) {
        parent::__construct($message, $status);
    }
}
