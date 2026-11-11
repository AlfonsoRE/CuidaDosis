<?php

declare(strict_types=1);

namespace CuidaDosis\Controllers;

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Request;
use CuidaDosis\Response;
use CuidaDosis\Validator;
use PDO;

// Medicamentos del paciente. Nombre y dosis son texto libre: no se interpretan ni se validan clínicamente.
final class MedicationController
{
    public function __construct(private readonly PDO $pdo, private readonly Auth $auth)
    {
    }

    public function list(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');

        $stmt = $this->pdo->prepare(
            'SELECT id, name, dose, created_at, updated_at FROM medications
              WHERE patient_id = ? AND active = 1 ORDER BY name, id'
        );
        $stmt->execute([$patient['id']]);
        $meds = $stmt->fetchAll();

        $stmt = $this->pdo->prepare(
            'SELECT s.medication_id, s.time_of_day FROM schedules s
               JOIN medications m ON m.id = s.medication_id
              WHERE m.patient_id = ? AND m.active = 1 AND s.active = 1
              ORDER BY s.time_of_day'
        );
        $stmt->execute([$patient['id']]);
        $times = [];
        foreach ($stmt->fetchAll() as $row) {
            $times[(int) $row['medication_id']][] = substr($row['time_of_day'], 0, 5);
        }

        $result = array_map(
            fn (array $m) => $this->format($m, $times[(int) $m['id']] ?? []),
            $meds
        );
        Response::json(200, ['medications' => $result]);
    }

    public function show(Request $request, array $params): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        Response::json(200, ['medication' => $this->load($params['id'], $patient['id'])]);
    }

    public function create(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        [$name, $dose, $times] = $this->validate($request);

        $this->pdo->beginTransaction();
        try {
            $this->pdo->prepare('INSERT INTO medications (patient_id, name, dose) VALUES (?, ?, ?)')
                ->execute([$patient['id'], $name, $dose]);
            $id = (int) $this->pdo->lastInsertId();
            $insert = $this->pdo->prepare('INSERT INTO schedules (medication_id, time_of_day) VALUES (?, ?)');
            foreach ($times as $time) {
                $insert->execute([$id, $time]);
            }
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(201, ['medication' => $this->load($id, $patient['id'])]);
    }

    // Reemplaza nombre, dosis y la lista completa de horas
    public function update(Request $request, array $params): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        $id = $params['id'];
        [$name, $dose, $times] = $this->validate($request);

        $this->pdo->beginTransaction();
        try {
            $this->lockOwned($id, $patient['id']);

            $this->pdo->prepare('UPDATE medications SET name = ?, dose = ?, updated_at = NOW() WHERE id = ?')
                ->execute([$name, $dose, $id]);

            $stmt = $this->pdo->prepare('SELECT id, time_of_day FROM schedules WHERE medication_id = ?');
            $stmt->execute([$id]);
            $existing = [];
            foreach ($stmt->fetchAll() as $row) {
                $existing[$row['time_of_day']] = (int) $row['id'];
            }

            $setActive = $this->pdo->prepare('UPDATE schedules SET active = ? WHERE id = ?');
            $insert = $this->pdo->prepare('INSERT INTO schedules (medication_id, time_of_day) VALUES (?, ?)');
            foreach ($times as $time) {
                if (isset($existing[$time])) {
                    $setActive->execute([1, $existing[$time]]);
                } else {
                    $insert->execute([$id, $time]);
                }
            }
            foreach ($existing as $time => $scheduleId) {
                if (!in_array($time, $times, true)) {
                    $setActive->execute([0, $scheduleId]);
                }
            }
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, ['medication' => $this->load($id, $patient['id'])]);
    }

    // No se borra: se desactiva el medicamento y sus horarios para conservar el historial
    public function deactivate(Request $request, array $params): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        $id = $params['id'];

        $this->pdo->beginTransaction();
        try {
            $this->lockOwned($id, $patient['id']);
            $this->pdo->prepare('UPDATE medications SET active = 0 WHERE id = ?')->execute([$id]);
            $this->pdo->prepare('UPDATE schedules SET active = 0 WHERE medication_id = ?')->execute([$id]);
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, ['message' => 'Medicamento desactivado.']);
    }

    /**
     * @return array{0: string, 1: string, 2: list<string>}
     */
    private function validate(Request $request): array
    {
        $v = new Validator($request->json());
        $name = $v->string('name', 1, 100);
        $dose = $v->string('dose', 1, 100);
        $times = $v->times('times', 1, 10);
        $v->check();

        return [$name, $dose, $times];
    }

    private function lockOwned(int $id, int $patientId): void
    {
        $stmt = $this->pdo->prepare(
            'SELECT id FROM medications WHERE id = ? AND patient_id = ? AND active = 1 FOR UPDATE'
        );
        $stmt->execute([$id, $patientId]);
        if ($stmt->fetch() === false) {
            throw new ApiException(404, 'Medicamento no encontrado.');
        }
    }

    /**
     * Medicamento activo del paciente con sus horas activas, o 404.
     *
     * @return array<string, mixed>
     */
    private function load(int $id, int $patientId): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT id, name, dose, created_at, updated_at FROM medications
              WHERE id = ? AND patient_id = ? AND active = 1'
        );
        $stmt->execute([$id, $patientId]);
        $med = $stmt->fetch();
        if ($med === false) {
            throw new ApiException(404, 'Medicamento no encontrado.');
        }

        $stmt = $this->pdo->prepare(
            'SELECT time_of_day FROM schedules WHERE medication_id = ? AND active = 1 ORDER BY time_of_day'
        );
        $stmt->execute([$id]);
        $times = array_map(fn (string $t) => substr($t, 0, 5), $stmt->fetchAll(PDO::FETCH_COLUMN));

        return $this->format($med, $times);
    }

    /**
     * @param array<string, mixed> $med
     * @param list<string> $times
     * @return array<string, mixed>
     */
    private function format(array $med, array $times): array
    {
        return [
            'id' => (int) $med['id'],
            'name' => $med['name'],
            'dose' => $med['dose'],
            'times' => $times,
            'created_at' => $med['created_at'],
            'updated_at' => $med['updated_at'],
        ];
    }
}
