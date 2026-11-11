<?php

declare(strict_types=1);

namespace CuidaDosis\Controllers;

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Request;
use CuidaDosis\Response;
use PDO;

// Tomas del día. "taken" solo registra el clic en "Tomado", no la ingesta real.
final class IntakeController
{
    public function __construct(private readonly PDO $pdo, private readonly Auth $auth)
    {
    }

    public function today(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        Response::json(200, $this->todayFor($patient['id']));
    }

    public function take(Request $request, array $params): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        $id = $params['id'];

        $this->pdo->beginTransaction();
        try {
            // Solo tomas propias, de hoy y de horarios y medicamentos activos
            $stmt = $this->pdo->prepare(
                'SELECT l.status FROM intake_logs l
                   JOIN schedules s ON s.id = l.schedule_id
                   JOIN medications m ON m.id = s.medication_id
                  WHERE l.id = ? AND m.patient_id = ?
                    AND l.scheduled_for >= CURDATE() AND l.scheduled_for < CURDATE() + INTERVAL 1 DAY
                    AND s.active = 1 AND m.active = 1
                  FOR UPDATE'
            );
            $stmt->execute([$id, $patient['id']]);
            $row = $stmt->fetch();
            if ($row === false) {
                throw new ApiException(404, 'Toma no encontrada entre las tomas de hoy.');
            }
            if ($row['status'] === 'taken') {
                throw new ApiException(409, 'Esta toma ya estaba marcada como tomada.');
            }
            $this->pdo->prepare("UPDATE intake_logs SET status = 'taken', confirmed_at = NOW() WHERE id = ?")
                ->execute([$id]);
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, ['intake' => $this->findIntake($id)]);
    }

    // El cuidador lista sus pacientes vinculados con el resumen de hoy de cada uno
    public function caregiverPatients(Request $request): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');

        $stmt = $this->pdo->prepare(
            "SELECT u.id, u.full_name, l.linked_at FROM links l JOIN users u ON u.id = l.patient_id
              WHERE l.caregiver_id = ? AND l.status = 'active'
              ORDER BY u.full_name, u.id"
        );
        $stmt->execute([$caregiver['id']]);

        $patients = [];
        foreach ($stmt->fetchAll() as $row) {
            $today = $this->todayFor((int) $row['id']);
            $patients[] = [
                'id' => (int) $row['id'],
                'full_name' => $row['full_name'],
                'linked_at' => $row['linked_at'],
                'summary' => $today['summary'],
            ];
        }

        Response::json(200, ['date' => date('Y-m-d'), 'patients' => $patients]);
    }

    // El cuidador consulta las tomas de hoy de uno de sus pacientes vinculados
    public function caregiverPatientToday(Request $request, array $params): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');

        $stmt = $this->pdo->prepare(
            "SELECT u.id, u.full_name FROM links l JOIN users u ON u.id = l.patient_id
              WHERE l.caregiver_id = ? AND l.patient_id = ? AND l.status = 'active'"
        );
        $stmt->execute([$caregiver['id'], $params['id']]);
        $patient = $stmt->fetch();
        if ($patient === false) {
            throw new ApiException(404, 'Ese paciente no está vinculado contigo.');
        }

        $data = $this->todayFor((int) $patient['id']);
        Response::json(200, [
            'patient' => ['id' => (int) $patient['id'], 'full_name' => $patient['full_name']],
        ] + $data);
    }

    /**
     * Crea como pendientes las tomas de hoy que falten y devuelve la lista del día.
     *
     * @return array<string, mixed>
     */
    private function todayFor(int $patientId): array
    {
        $this->pdo->prepare(
            'INSERT IGNORE INTO intake_logs (schedule_id, scheduled_for)
             SELECT s.id, TIMESTAMP(CURDATE(), s.time_of_day)
               FROM schedules s JOIN medications m ON m.id = s.medication_id
              WHERE m.patient_id = ? AND m.active = 1 AND s.active = 1'
        )->execute([$patientId]);

        $stmt = $this->pdo->prepare(
            $this->selectSql() . "
              WHERE m.patient_id = ?
                AND l.scheduled_for >= CURDATE() AND l.scheduled_for < CURDATE() + INTERVAL 1 DAY
                AND (l.status = 'taken' OR (s.active = 1 AND m.active = 1))
              ORDER BY l.scheduled_for, m.name, l.id"
        );
        $stmt->execute([$patientId]);
        $intakes = array_map(fn (array $r) => $this->format($r), $stmt->fetchAll());

        $taken = count(array_filter($intakes, fn (array $i) => $i['status'] === 'taken'));

        return [
            'date' => date('Y-m-d'),
            'intakes' => $intakes,
            'summary' => ['total' => count($intakes), 'taken' => $taken, 'pending' => count($intakes) - $taken],
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function findIntake(int $id): array
    {
        $stmt = $this->pdo->prepare($this->selectSql() . ' WHERE l.id = ?');
        $stmt->execute([$id]);

        return $this->format($stmt->fetch());
    }

    private function selectSql(): string
    {
        return 'SELECT l.id, l.scheduled_for, l.status, l.confirmed_at,
                       m.id AS medication_id, m.name, m.dose
                  FROM intake_logs l
                  JOIN schedules s ON s.id = l.schedule_id
                  JOIN medications m ON m.id = s.medication_id';
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function format(array $row): array
    {
        return [
            'id' => (int) $row['id'],
            'medication_id' => (int) $row['medication_id'],
            'name' => $row['name'],
            'dose' => $row['dose'],
            'time' => substr($row['scheduled_for'], 11, 5),
            'scheduled_for' => $row['scheduled_for'],
            'status' => $row['status'],
            'confirmed_at' => $row['confirmed_at'],
        ];
    }
}
