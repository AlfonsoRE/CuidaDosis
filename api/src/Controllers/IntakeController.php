<?php

declare(strict_types=1);

namespace CuidaDosis\Controllers;

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Request;
use CuidaDosis\Response;
use PDO;

// Tomas. "taken" solo registra el toque en "Tomado" (Confirmada por el paciente),
// no la ingesta real. El estado visible se calcula (ADR-005):
//   confirmed   = taken
//   unconfirmed = pending y ahora > scheduled_for + 30 min ("Sin confirmar", no se guarda)
//   pending     = pending dentro del plazo
final class IntakeController
{
    private const UNCONFIRMED_AFTER_MINUTES = 30;
    private const HISTORY_DAYS = 7;
    private const MAX_BATCH = 100;
    // Tolerancia para relojes de teléfono adelantados
    private const CLOCK_SKEW_MINUTES = 5;

    public function __construct(private readonly PDO $pdo, private readonly Auth $auth)
    {
    }

    public function today(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        Response::json(200, $this->todayFor($patient['id']));
    }

    // Se mantiene por compatibilidad; la app usa /intakes/sync (ADR-003)
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
                throw new ApiException(409, 'Esta toma ya estaba confirmada por el paciente.');
            }
            $this->pdo->prepare(
                "UPDATE intake_logs SET status = 'taken', confirmed_at = NOW(), received_at = NOW() WHERE id = ?"
            )->execute([$id]);
            $this->touchSync((int) $patient['id']);
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        Response::json(200, ['intake' => $this->findIntake($id)]);
    }

    /**
     * Sincronización en lote de confirmaciones hechas en el teléfono (RF-13; G-03, G-04; ADR-003).
     * Idempotente: un client_uuid ya recibido devuelve "duplicate" sin duplicar ni fallar.
     */
    public function sync(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        $patientId = (int) $patient['id'];
        $items = $request->json()['confirmations'] ?? null;
        if (!is_array($items) || !array_is_list($items)) {
            throw new ApiException(422, 'Datos inválidos.', [
                'confirmations' => 'Debe ser una lista (puede estar vacía).',
            ]);
        }
        if (count($items) > self::MAX_BATCH) {
            throw new ApiException(422, 'Datos inválidos.', [
                'confirmations' => 'Máximo ' . self::MAX_BATCH . ' confirmaciones por envío.',
            ]);
        }

        $results = [];
        foreach ($items as $item) {
            $results[] = $this->syncOne($patientId, is_array($item) ? $item : []);
        }

        // RF-12: el teléfono se comunicó, aunque el lote venga vacío
        $this->touchSync($patientId);
        $stmt = $this->pdo->prepare('SELECT last_sync_at FROM users WHERE id = ?');
        $stmt->execute([$patientId]);

        Response::json(200, ['results' => $results, 'last_sync_at' => $stmt->fetchColumn()]);
    }

    // Historial de los últimos 7 días del paciente (RF-15)
    public function history(Request $request): void
    {
        $patient = $this->auth->requireRole($request, 'patient');
        Response::json(200, $this->historyFor((int) $patient['id'], true));
    }

    // El cuidador lista sus pacientes vinculados con el resumen de hoy de cada uno
    public function caregiverPatients(Request $request): void
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');

        $stmt = $this->pdo->prepare(
            "SELECT u.id, u.full_name, u.last_sync_at, l.linked_at FROM links l JOIN users u ON u.id = l.patient_id
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
                'last_sync_at' => $row['last_sync_at'],
                'summary' => $today['summary'],
            ];
        }

        Response::json(200, ['date' => date('Y-m-d'), 'now' => $this->now(), 'patients' => $patients]);
    }

    // El cuidador consulta las tomas de hoy de uno de sus pacientes vinculados
    public function caregiverPatientToday(Request $request, array $params): void
    {
        $patient = $this->linkedPatient($request, (int) $params['id']);
        $data = $this->todayFor($patient['id']);
        $data['intakes'] = $this->hideDose($data['intakes'], $patient['share_dose']);
        Response::json(200, ['patient' => $patient] + $data);
    }

    // El cuidador consulta el historial de 7 días de uno de sus pacientes vinculados
    public function caregiverPatientHistory(Request $request, array $params): void
    {
        $patient = $this->linkedPatient($request, (int) $params['id']);
        $data = $this->historyFor($patient['id'], $patient['share_dose']);
        Response::json(200, ['patient' => $patient] + $data);
    }

    /**
     * Paciente con vínculo activo con el cuidador; si no, 404 (no revela si existe).
     *
     * @return array{id: int, full_name: string, share_dose: bool, last_sync_at: ?string}
     */
    private function linkedPatient(Request $request, int $patientId): array
    {
        $caregiver = $this->auth->requireRole($request, 'caregiver');
        $stmt = $this->pdo->prepare(
            "SELECT u.id, u.full_name, u.last_sync_at, l.share_dose FROM links l JOIN users u ON u.id = l.patient_id
              WHERE l.caregiver_id = ? AND l.patient_id = ? AND l.status = 'active'"
        );
        $stmt->execute([$caregiver['id'], $patientId]);
        $row = $stmt->fetch();
        if ($row === false) {
            throw new ApiException(404, 'Ese paciente no está vinculado contigo.');
        }

        return [
            'id' => (int) $row['id'],
            'full_name' => $row['full_name'],
            'share_dose' => (int) $row['share_dose'] === 1,
            'last_sync_at' => $row['last_sync_at'],
        ];
    }

    /**
     * G-07: la dosis no sale de la API hacia el cuidador sin autorización del paciente.
     *
     * @param list<array<string, mixed>> $intakes
     * @return list<array<string, mixed>>
     */
    private function hideDose(array $intakes, bool $shareDose): array
    {
        if ($shareDose) {
            return $intakes;
        }

        return array_map(function (array $i): array {
            unset($i['dose']);
            return $i;
        }, $intakes);
    }

    /**
     * @param array<string, mixed> $item
     * @return array<string, mixed>
     */
    private function syncOne(int $patientId, array $item): array
    {
        $uuid = is_string($item['client_uuid'] ?? null) ? strtolower(trim($item['client_uuid'])) : '';
        $result = ['client_uuid' => $uuid];
        $reject = function (string $reason) use (&$result): array {
            return $result + ['status' => 'rejected', 'error' => $reason];
        };

        if (preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/', $uuid) !== 1) {
            return $reject('client_uuid debe ser un UUID.');
        }
        $scheduleId = $item['schedule_id'] ?? null;
        if (!is_int($scheduleId) || $scheduleId < 1) {
            return $reject('schedule_id debe ser un entero positivo.');
        }
        $scheduledFor = $this->parseDateTime($item['scheduled_for'] ?? null);
        $confirmedAt = $this->parseDateTime($item['confirmed_at'] ?? null);
        if ($scheduledFor === null || $confirmedAt === null) {
            return $reject('scheduled_for y confirmed_at deben tener formato YYYY-MM-DD HH:MM:SS.');
        }

        $this->pdo->beginTransaction();
        try {
            // UUID ya recibido: idempotente
            $stmt = $this->pdo->prepare(
                'SELECT l.id, m.patient_id FROM intake_logs l
                   JOIN schedules s ON s.id = l.schedule_id
                   JOIN medications m ON m.id = s.medication_id
                  WHERE l.client_uuid = ?'
            );
            $stmt->execute([$uuid]);
            $dup = $stmt->fetch();
            if ($dup !== false) {
                $this->pdo->rollBack();
                if ((int) $dup['patient_id'] !== $patientId) {
                    return $reject('client_uuid ya usado.');
                }

                return $result + ['status' => 'duplicate', 'intake_id' => (int) $dup['id']];
            }

            // El horario debe ser del paciente, la hora debe coincidir y la fecha estar en los últimos 7 días
            $stmt = $this->pdo->prepare(
                'SELECT s.id FROM schedules s JOIN medications m ON m.id = s.medication_id
                  WHERE s.id = ? AND m.patient_id = ?
                    AND TIME(?) = s.time_of_day
                    AND DATE(?) BETWEEN CURDATE() - INTERVAL ' . (self::HISTORY_DAYS - 1) . ' DAY AND CURDATE()
                    AND ? <= NOW() + INTERVAL ' . self::CLOCK_SKEW_MINUTES . ' MINUTE
                    AND ? >= ? - INTERVAL 1 DAY'
            );
            $stmt->execute([$scheduleId, $patientId, $scheduledFor, $scheduledFor, $confirmedAt, $confirmedAt, $scheduledFor]);
            if ($stmt->fetch() === false) {
                $this->pdo->rollBack();
                return $reject('La toma no corresponde a un horario tuyo de los últimos 7 días, o las horas no son válidas.');
            }

            $stmt = $this->pdo->prepare(
                'SELECT id, status FROM intake_logs WHERE schedule_id = ? AND scheduled_for = ? FOR UPDATE'
            );
            $stmt->execute([$scheduleId, $scheduledFor]);
            $row = $stmt->fetch();

            if ($row !== false && $row['status'] === 'taken') {
                // Ya confirmada por otro envío: se conserva la primera confirmación
                $this->pdo->rollBack();
                return $result + ['status' => 'already_confirmed', 'intake_id' => (int) $row['id']];
            }

            if ($row === false) {
                $this->pdo->prepare(
                    "INSERT INTO intake_logs (schedule_id, scheduled_for, status, confirmed_at, client_uuid, received_at)
                     VALUES (?, ?, 'taken', ?, ?, NOW())"
                )->execute([$scheduleId, $scheduledFor, $confirmedAt, $uuid]);
                $intakeId = (int) $this->pdo->lastInsertId();
            } else {
                $this->pdo->prepare(
                    "UPDATE intake_logs SET status = 'taken', confirmed_at = ?, client_uuid = ?, received_at = NOW()
                      WHERE id = ?"
                )->execute([$confirmedAt, $uuid, $row['id']]);
                $intakeId = (int) $row['id'];
            }
            $this->pdo->commit();
        } catch (\PDOException $e) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            // Dos envíos simultáneos del mismo UUID: el segundo choca con el índice único
            if (($e->errorInfo[1] ?? null) === 1062) {
                return $result + ['status' => 'duplicate'];
            }
            throw $e;
        }

        return $result + ['status' => 'created', 'intake_id' => $intakeId];
    }

    private function parseDateTime(mixed $value): ?string
    {
        if (!is_string($value)) {
            return null;
        }
        $dt = \DateTimeImmutable::createFromFormat('!Y-m-d H:i:s', $value);
        if ($dt === false || $dt->format('Y-m-d H:i:s') !== $value) {
            return null;
        }

        return $value;
    }

    private function touchSync(int $patientId): void
    {
        $this->pdo->prepare('UPDATE users SET last_sync_at = NOW() WHERE id = ?')->execute([$patientId]);
    }

    private function now(): string
    {
        return (string) $this->pdo->query('SELECT NOW()')->fetchColumn();
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

        return [
            'date' => date('Y-m-d'),
            'now' => $this->now(),
            'intakes' => $intakes,
            'summary' => $this->summary($intakes),
        ];
    }

    /**
     * Últimos 7 días (hoy incluido), del más reciente al más antiguo.
     * Rellena como pendientes los días que nadie consultó, solo para horarios
     * activos que ya existían a esa hora.
     *
     * @return array<string, mixed>
     */
    private function historyFor(int $patientId, bool $withDose): array
    {
        $this->todayFor($patientId);
        $days = implode(' UNION ALL ', array_map(fn (int $n) => "SELECT $n AS n", range(1, self::HISTORY_DAYS - 1)));
        $this->pdo->prepare(
            "INSERT IGNORE INTO intake_logs (schedule_id, scheduled_for)
             SELECT s.id, TIMESTAMP(CURDATE() - INTERVAL d.n DAY, s.time_of_day)
               FROM schedules s JOIN medications m ON m.id = s.medication_id
               CROSS JOIN ($days) d
              WHERE m.patient_id = ? AND m.active = 1 AND s.active = 1
                AND TIMESTAMP(CURDATE() - INTERVAL d.n DAY, s.time_of_day) >= s.created_at"
        )->execute([$patientId]);

        $stmt = $this->pdo->prepare(
            $this->selectSql() . "
              WHERE m.patient_id = ?
                AND l.scheduled_for >= CURDATE() - INTERVAL " . (self::HISTORY_DAYS - 1) . " DAY
                AND l.scheduled_for < CURDATE() + INTERVAL 1 DAY
                AND (l.status = 'taken' OR (s.active = 1 AND m.active = 1))
              ORDER BY l.scheduled_for DESC, m.name, l.id"
        );
        $stmt->execute([$patientId]);
        $intakes = $this->hideDose(array_map(fn (array $r) => $this->format($r), $stmt->fetchAll()), $withDose);

        $byDay = [];
        foreach ($intakes as $i) {
            $byDay[substr($i['scheduled_for'], 0, 10)][] = $i;
        }
        $result = [];
        foreach ($byDay as $date => $list) {
            $result[] = ['date' => $date, 'intakes' => $list, 'summary' => $this->summary($list)];
        }

        return [
            'from' => date('Y-m-d', strtotime('-' . (self::HISTORY_DAYS - 1) . ' days')),
            'to' => date('Y-m-d'),
            'now' => $this->now(),
            'days' => $result,
        ];
    }

    /**
     * @param list<array<string, mixed>> $intakes
     * @return array<string, int>
     */
    private function summary(array $intakes): array
    {
        $count = fn (string $state) => count(array_filter($intakes, fn (array $i) => $i['state'] === $state));
        $confirmed = $count('confirmed');

        return [
            'total' => count($intakes),
            'taken' => $confirmed,
            'pending' => count($intakes) - $confirmed,
            'confirmed' => $confirmed,
            'unconfirmed' => $count('unconfirmed'),
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
        return "SELECT l.id, l.schedule_id, l.scheduled_for, l.status, l.confirmed_at, l.received_at,
                       m.id AS medication_id, m.name, m.dose,
                       CASE
                           WHEN l.status = 'taken' THEN 'confirmed'
                           WHEN NOW() > l.scheduled_for + INTERVAL " . self::UNCONFIRMED_AFTER_MINUTES . " MINUTE
                               THEN 'unconfirmed'
                           ELSE 'pending'
                       END AS state
                  FROM intake_logs l
                  JOIN schedules s ON s.id = l.schedule_id
                  JOIN medications m ON m.id = s.medication_id";
    }

    /**
     * @param array<string, mixed> $row
     * @return array<string, mixed>
     */
    private function format(array $row): array
    {
        return [
            'id' => (int) $row['id'],
            'schedule_id' => (int) $row['schedule_id'],
            'medication_id' => (int) $row['medication_id'],
            'name' => $row['name'],
            'dose' => $row['dose'],
            'time' => substr($row['scheduled_for'], 11, 5),
            'scheduled_for' => $row['scheduled_for'],
            'status' => $row['status'],
            'state' => $row['state'],
            'confirmed_at' => $row['confirmed_at'],
            'received_at' => $row['received_at'],
        ];
    }
}
