# Resumen de la Fase 6 (y verificación de la Fase 7)

Alineación de CuidaDosis con el diseño P2 v1.1 y P3 v1.0, del 21/11/2026. Todos los datos usados son sintéticos. Nada de lo que aquí aparece como "OK" se reporta sin haberse ejecutado; lo que requiere un teléfono físico dice "pendiente de ejecución manual".

- Decisiones: [docs/decisiones-fase-6.md](docs/decisiones-fase-6.md) (D1 a D36).
- Pendientes y bloqueos: [docs/PENDIENTES_FASE6.md](docs/PENDIENTES_FASE6.md).
- Evidencias: [docs/evidencias/p4/INDICE.md](docs/evidencias/p4/INDICE.md).
- Instalación, configuración y uso: [docs/instalacion.md](docs/instalacion.md), [docs/configuracion.md](docs/configuracion.md), [docs/guia-uso.md](docs/guia-uso.md).

## Resultados reales

| Prueba | Resultado | Evidencia |
|---|---|---|
| Suite automática de la API (27 casos) | 27 OK, 0 FALLO, en tres corridas | `docs/evidencias/p4/api-*.log` |
| Pruebas unitarias de la app | 65 OK (32 anteriores sin cambios más 33 nuevas) | `android/app/build/test-results/testDebugUnitTest/` |
| Casos PR-01 a PR-15 en emulador | Ver la tabla y `pruebas-emulador.csv` | `docs/evidencias/p4/pruebas-emulador.csv` |
| Tiempo de la consulta del día (50 repeticiones) | Paciente: promedio 39.7 ms, p95 72.6 ms. Cuidador: promedio 39.3 ms, p95 56.2 ms. Meta: 2000 ms | `docs/evidencias/p4/rendimiento.csv` (localhost) |
| Bloqueo web (`.htaccess`) | 403 en `api/.env`, `.git/config`, `bd/`, `api/tests/`, `docs/` y `android/local.properties` | `docs/instalacion.md` 1.4 |
| APK debug | Compila e instala en el emulador (Android 17) | `docs/instalacion.md` 2.1 y 2.2 |

## Requisitos

| Requisito | ¿Implementado? | Archivos principales | Prueba automática | Prueba en emulador | Queda para teléfono físico |
|---|---|---|---|---|---|
| RF-01, RF-02 registro y sesión | Sí | `AuthController.php`, `ui/auth`, `SessionStore` | API-01 a API-06 | PR-01 OK | No |
| RF-03, RF-04 medicamentos | Sí | `MedicationController.php` (ahora con `schedules`), `ui/medications` | API-19 (alta) | PR-02 OK | No |
| RF-05 aviso a T menos L (5 o 10) | Sí | `reminders/ReminderSchedule.kt`, `ReminderScheduler.kt`, `ui/settings/ReminderSettingsScreen.kt` | `ReminderEventTest` (L=5, L=10, reglas de L) | PR-03 OK | Puntualidad (RNF-01) con pantalla bloqueada y ahorro de batería |
| RF-06 segundo aviso a T | Sí | `ReminderSchedule.kt`, `ReminderReceiver.kt` | `ReminderEventTest` (una sola vez, no si está confirmada) | PR-03 OK | Puntualidad |
| RF-07 un toque, "Confirmada por el paciente" | Sí | `ui/patient/TodayTab.kt`, `IntakeRepository.kt`, `data/IntakeStatus.kt` | `IntakeStateTest` (etiquetas, nunca "Tomada") | PR-04 OK | No |
| RF-08 código de 10 min y aceptación | Sí | `LinkController.php`, `schema.sql` (`awaiting_acceptance`, `link_attempts`), `ui/patient/CaregiverLinkTab.kt`, `ui/caregiver` | API-07 a API-18, `LinkMessagesTest` | PR-05 OK | No |
| RF-09 revocar | Sí | `LinkController.php`, pantallas de paciente y cuidador | API-27 | PR-05 OK | No |
| RF-10 vista del cuidador, tres etiquetas, dosis oculta | Sí (dosis sin pantalla para autorizarla, G-07) | `IntakeController.php`, `ui/caregiver`, `ui/common/IntakeCard.kt` | API-24, `IntakeStateTest` | PR-06 OK | No |
| RF-11 "Sin confirmar" a T + 30, calculado | Sí | `IntakeController.php` (`state`), `IntakeStatus.kt` | API-23, `IntakeStateTest` | PR-06 OK | No |
| RF-12 última sincronización | Sí | `users.last_sync_at`, `IntakeController::sync`, `lastSyncText` | API-21 | PR-07 OK | No |
| RF-13 sin red y sin duplicados | Sí | `data/local/PatientStore.kt`, `data/OfflineQueue.kt`, `IntakeRepository.kt`, `POST /intakes/sync` | API-20, API-22, `OfflineQueueTest` | PR-08 OK | Envío con la app cerrada por el sistema (ver D29) |
| RF-14 advertencia de permisos | Sí | `ui/patient/ReminderPermission.kt` | No | PR-09 OK | Restricciones propias del fabricante |
| RF-15 historial de 7 días | Mínimo: solo API | `GET /intakes/history`, `GET /caregiver/patients/{id}/intakes/history` | API-23, API-27 usan el historial | PR-10 OK (por API) | No |
| RF-16 posponer 5 o 10 min | Sí | `ReminderNotifier.kt`, `SnoozeReceiver`, `ReminderStore.kt` | `ReminderEventTest` | PR-11 OK | No |
| RF-17 push al cuidador | No (fuera de alcance) | - | - | - | - |
| RF-18 alarma del cuidador a T + 30 | Sí | `reminders/CaregiverAlarm.kt`, `CaregiverAlarmScheduler.kt` | `CaregiverAlarmTest` | PR-12 OK (notificación a las 13:40) | Puntualidad y Doze en teléfono real |
| RNF-01 a RNF-03 puntualidad y reinicio | Parcial | `BootReceiver.kt` | No | PR-13 parcial: reinicio y retrasos de 50 a 152 ms en emulador | Sí: 10 avisos por escenario |
| RNF-04 legibilidad | Sí | `ui/theme/Type.kt`, botones de 56 dp | No | PR-15 OK (letra del sistema al 130 %) | Revisión con personas mayores |
| RNF-05, RNF-06 tokens y acceso sin vínculo | Sí | `Auth.php`, verificación de vínculo activo | API-06, API-09, API-25 a API-27 | PR-14 OK | No |
| RNF-07 tiempo de respuesta | Sí | consulta del día | `api/tests/measure_today.php` | p95 72.6 ms (localhost) | Medición por Wi-Fi desde un teléfono |

## Cambios de la base de datos (G-01 a G-07)
G-01 código de 10 min (API); G-02 estado `awaiting_acceptance`; G-03 `intake_logs.client_uuid` único; G-04 `users.last_sync_at` e `intake_logs.received_at`; G-05 tabla `link_attempts`; G-06 L solo en el teléfono; G-07 `links.share_dose` (por defecto 0) y la API no envía la dosis al cuidador mientras sea 0. Se agregó además `links.requested_at` (D2).

## Decisiones más importantes
- Archivo cifrado (AES-GCM, Keystore) en lugar de Room para el modo sin red: sin dependencias nuevas y con cifrado ya probado (D27).
- Una sola alarma exacta para el próximo evento (T menos L, T o pospuesto), revisando al sonar si la toma ya se confirmó (D21).
- Sin WorkManager: la cola se envía al tocar, al abrir "Hoy", al volver la red y con reintentos mientras la app vive (D29).
- Código vencido con 410 y bloqueo con 429 y `Retry-After`, sin registrar intentos durante el bloqueo (D6, D7).
- Lote de sincronización como `{"confirmations": [...]}` con un resultado por elemento (D11 a D13).
- Rechazar una solicitud usa `revoked` (D3).

## Bloqueos y pendientes
Ver [docs/PENDIENTES_FASE6.md](docs/PENDIENTES_FASE6.md). En resumen: pruebas en teléfono físico (puntualidad, reinicio real, ahorro de batería y alarma del cuidador), pantalla para autorizar la dosis (G-07), historial sin pantalla, sin WorkManager, pérdida de la cola al cerrar sesión sin red y despliegue en hosting no verificado.

## Commits
Fase 6: `534772d` (AGENTS.md), `7a28090` (BD v2), `632b7d8` (vínculo), `34c5215` (tomas), `dec7bd6` (pruebas API), `f5d0ef2` (avisos), `276b994` (sin red), `223501a` (vínculo en app), `10de222` (cuidador), `e71e6ef` (accesibilidad), `e36b13c` (cierre). Correcciones de la verificación: `c9ea6df`. Sin push.
