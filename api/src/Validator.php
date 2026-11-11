<?php

declare(strict_types=1);

namespace CuidaDosis;

// Valida campos de entrada y acumula errores por campo (respuesta 422)
final class Validator
{
    /** @var array<string, string> */
    private array $errors = [];

    /**
     * @param array<string, mixed> $data
     */
    public function __construct(private readonly array $data)
    {
    }

    public function string(string $field, int $min, int $max, bool $trim = true): string
    {
        $value = $this->data[$field] ?? null;
        if (!is_string($value)) {
            $this->errors[$field] = 'Es obligatorio y debe ser texto.';

            return '';
        }
        if ($trim) {
            $value = trim($value);
        }
        // Rechaza caracteres de control
        if (preg_match('/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/u', $value) === 1 || !mb_check_encoding($value, 'UTF-8')) {
            $this->errors[$field] = 'Contiene caracteres no permitidos.';

            return '';
        }
        $len = mb_strlen($value);
        if ($len < $min || $len > $max) {
            $this->errors[$field] = "Debe tener entre $min y $max caracteres.";

            return '';
        }

        return $value;
    }

    public function email(string $field): string
    {
        $value = $this->string($field, 3, 150);
        if ($value !== '' && filter_var($value, FILTER_VALIDATE_EMAIL) === false) {
            $this->errors[$field] = 'No es un correo válido.';

            return '';
        }

        return strtolower($value);
    }

    /**
     * @param list<string> $options
     */
    public function enum(string $field, array $options): string
    {
        $value = $this->data[$field] ?? null;
        if (!is_string($value) || !in_array($value, $options, true)) {
            $this->errors[$field] = 'Debe ser uno de: ' . implode(', ', $options) . '.';

            return '';
        }

        return $value;
    }

    /**
     * Lista de horas "HH:MM" (24 h), sin repetir. Devuelve "HH:MM:00" ordenadas.
     *
     * @return list<string>
     */
    public function times(string $field, int $min, int $max): array
    {
        $value = $this->data[$field] ?? null;
        if (!is_array($value) || !array_is_list($value)) {
            $this->errors[$field] = 'Debe ser una lista de horas en formato HH:MM.';

            return [];
        }
        $times = [];
        foreach ($value as $t) {
            if (!is_string($t) || preg_match('/^([01][0-9]|2[0-3]):[0-5][0-9]$/', $t) !== 1) {
                $this->errors[$field] = 'Cada hora debe tener formato HH:MM (00:00 a 23:59).';

                return [];
            }
            $times[] = $t . ':00';
        }
        if (count($times) !== count(array_unique($times))) {
            $this->errors[$field] = 'Hay horas repetidas.';

            return [];
        }
        if (count($times) < $min || count($times) > $max) {
            $this->errors[$field] = "Debe tener entre $min y $max horas.";

            return [];
        }
        sort($times);

        return $times;
    }

    public function check(): void
    {
        if ($this->errors !== []) {
            throw new ApiException(422, 'Datos inválidos.', $this->errors);
        }
    }
}
