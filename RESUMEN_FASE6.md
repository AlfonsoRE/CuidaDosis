# Resumen de la Fase 6 (con la verificación de la Fase 7 y los ajustes de la Fase 8)

Alineación de CuidaDosis con el diseño P2 v1.1 y P3 v1.0, del 21/11/2026. Todos los datos usados son sintéticos. Nada de lo que aquí aparece como "OK" se reporta sin haberse ejecutado; lo que requiere un teléfono físico dice "pendiente de ejecución manual".

- Decisiones: [docs/decisiones-fase-6.md](docs/decisiones-fase-6.md) (D1 a D39; la Fase 8 es D37 a D39).
- Pendientes y bloqueos: [docs/PENDIENTES_FASE6.md](docs/PENDIENTES_FASE6.md).
- Evidencias: [docs/evidencias/p4/INDICE.md](docs/evidencias/p4/INDICE.md).
- Instalación, configuración y uso: [docs/instalacion.md](docs/instalacion.md), [docs/configuracion.md](docs/configuracion.md), [docs/guia-uso.md](docs/guia-uso.md).

## Resultados reales

| Prueba | Resultado | Evidencia |
|---|---|---|
| Suite automática de la API (27 casos) | 27 OK, 0 FALLO, en tres corridas | `docs/evidencias/p4/api-*.log` |
| Pruebas unitarias de la app | 71 OK (32 anteriores sin cambios, 33 de la Fase 6 y 6 de la Fase 8) | `android/app/build/test-results/testDebugUnitTest/` |
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
| RF-06 alarma a T (v1.1: repetida hasta Tomado o Posponer, máx. 2 min) | Sí | `ReminderSchedule.kt`, `ReminderReceiver.kt`, `ReminderNotifier.kt`, `AlarmActivity.kt` | `ReminderEventTest`, `AlarmSchedulingTest` (alarma a T, límite de 2 min, no si está confirmada) | PR-03 OK | Puntualidad |
| RF-07 un toque, "Confirmada por el paciente" | Sí | `ui/patient/TodayTab.kt`, `IntakeRepository.kt`, `data/IntakeStatus.kt` | `IntakeStateTest` (etiquetas, nunca "Tomada") | PR-04 OK | No |
| RF-08 código de 10 min y aceptación | Sí | `LinkController.php`, `schema.sql` (`awaiting_acceptance`, `link_attempts`), `ui/patient/CaregiverLinkTab.kt`, `ui/caregiver` | API-07 a API-18, `LinkMessagesTest` | PR-05 OK | No |
| RF-09 revocar | Sí | `LinkController.php`, pantallas de paciente y cuidador | API-27 | PR-05 OK | No |
| RF-10 vista del cuidador, tres etiquetas, dosis oculta | Sí (dosis sin pantalla para autorizarla, G-07) | `IntakeController.php`, `ui/caregiver`, `ui/common/IntakeCard.kt` | API-24, `IntakeStateTest` | PR-06 OK | No |
| RF-11 "Sin confirmar" a T + 30, calculado | Sí | `IntakeController.php` (`state`), `IntakeStatus.kt` | API-23, `IntakeStateTest` | PR-06 OK | No |
| RF-12 última sincronización | Sí | `users.last_sync_at`, `IntakeController::sync`, `lastSyncText` | API-21 | PR-07 OK | No |
| RF-13 sin red y sin duplicados | Sí; desde la Fase 8 "Salir" no pierde confirmaciones | `data/local/PatientStore.kt`, `data/OfflineQueue.kt`, `IntakeRepository.kt`, `POST /intakes/sync`, `ui/AppNavHost.kt` | API-20, API-22, `OfflineQueueTest`, `LogoutGuardTest` | PR-08 OK; "Salir" bloqueado sin red (Fase 8) | Envío con la app cerrada por el sistema (ver D29) |
| RF-14 advertencia de permisos | Sí | `ui/patient/ReminderPermission.kt` | No | PR-09 OK | Restricciones propias del fabricante |
| RF-15 historial de 7 días | Sí (requiere conexión) | `GET /intakes/history`, `GET /caregiver/patients/{id}/intakes/history`, `ui/common/HistoryList.kt`, pestaña "Historial" y `CaregiverHistoryScreen.kt` | API-23, API-27, `HistoryLabelTest` | PR-10 OK (API y pantallas, Fase 8) | No |
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

## Fase 8
| Paso | Resultado | Evidencia |
|---|---|---|
| 1. Cerrar sesión sin perder confirmaciones | "Salir" envía antes la cola; sin red, bloquea con el diálogo "Confirmaciones sin enviar" (Reintentar envío o Quedarme). Probado en emulador: bloqueó en modo avión y, con red, envió (received_at 14:11:53) y cerró sesión | D37, `LogoutGuardTest`, captura 18 |
| 2. Paquete de despliegue | `deploy/build_hosting_package.php` arma `build/hosting/` (public_html con solo index.php y .htaccess; src, config y .env.example fuera de la web; esquema sin CREATE DATABASE; seed sin cuentas de demo). Probado solo en la PC con una base temporal y el servidor embebido de PHP; nada contra servidores externos | D38, `docs/instalacion.md` sección 3 |
| 3. Capturas con la pestaña corregida | 06, 07, 09, 10, 12 y 17 retomadas en el emulador | `docs/evidencias/p4/INDICE.md` |
| 4. Historial de 7 días | Pestaña "Historial" del paciente y "Ver historial de 7 días" del cuidador; probado en emulador (Ayer con 1 sin confirmar, sin dosis) | D39, `HistoryLabelTest`, capturas 19 y 19b |

Durante el paso 3 se conectó un teléfono físico a la PC. No se ejecutó nada en él: los comandos de adb se fijaron al emulador (`ANDROID_SERIAL=emulator-5554`).

## Verificación en teléfono físico (Fase 8, Parte B)
POCO M6 Pro, Android 16, HyperOS, con batería "Sin restricciones" e Inicio automático, por USB. Detalle en [docs/evidencias/p4/fisico/RESUMEN.md](docs/evidencias/p4/fisico/RESUMEN.md).

| Caso | Resultado en el teléfono |
|---|---|
| RNF-01 pantalla encendida | OK, 10 de 10 (≤ 0.04 s) |
| RNF-01 pantalla apagada | **FALLO, 6 de 10** (retrasos de 67 a 187 s de HyperOS) |
| RNF-01 reposo sin cable (escenario C) | No ejecutado |
| RNF-03 reinicio | Parcial: se reprograma tras desbloquear; 2 de 4 avisos a tiempo |
| PR-03 / PR-04 | OK con pantalla encendida / OK |
| PR-05, PR-08, salir con pendientes, RF-15, SEG-05 | OK |
| PR-12 | Parcial: alarma a tiempo; solo se observó la rama "sin conexión" por la inestabilidad del Wi-Fi |

**Casos que quedan para el formato manual de complemento:** RNF-01 escenario C (sin cable, reposo prolongado); repetir RNF-01 B y RNF-03 tras una mejora de alarmas; PR-12 rama "Toma sin confirmar" con red estable; RNF-07 desde el teléfono por Wi-Fi; token vencido con pendientes (limitación); legibilidad con personas mayores; otros fabricantes.

## Fase 9 (versión 1.1.0)
Ajuste de requisito por decisión del equipo y corrección del hallazgo de la verificación física. La v1.0 evaluada quedó etiquetada `v1.0-evaluada` (local).

| Cambio | Detalle | Evidencia |
|---|---|---|
| RF-06 alarma del paciente | A la hora T, alarma con sonido de alarma y vibración que se repite (`FLAG_INSISTENT`) hasta Tomado o Posponer; máximo 2 min, luego notificación fija sin sonido; pantalla de alarma sobre el bloqueo | D41 |
| RF-05 y RF-18 | Sin cambios de comportamiento; canales nuevos con vibración (`_v2`); los de la v1.0 se eliminan | D41 |
| Alarmas atrasadas | Una alarma de reloj (`setAlarmClock`) por aviso, en lugar de una sola encadenada | D42 |
| Pruebas unitarias | 79 de 79 OK (8 nuevas en `AlarmSchedulingTest`) | |
| Retest en emulador (API 37) | PR-03, PR-04, PR-11, toma confirmada, límite de 2 min, reinicio y regresión: OK. Puntualidad: 20 de 20 con pantalla encendida y 20 de 20 con pantalla apagada (máximo +1.19 s). **Tiempos del emulador no representativos del teléfono** | `docs/evidencias/p4/emulador_v1.1/RESUMEN.md` |
| Teléfono físico | Hecho en la Fase 10 (ver abajo), salvo PR-12, sonido y vibración | `docs/evidencias/p4/fisico/RESUMEN.md` |

No se pudo probar en esta fase: que la alarma se oiga y vibre, el modo silencio y No molestar, la pantalla completa con el teléfono bloqueado, y los avisos antes del primer desbloqueo tras reiniciar (sigue haciendo falta desbloquear; D42).

## Fase 10 (v1.1 en el teléfono físico)
POCO M6 Pro (Android 16, HyperOS), mismas condiciones que la v1.0 (batería sin restricciones, inicio automático, USB y cargando). Solo automatización por adb; nadie oyó la alarma.

| Caso | Resultado | Evidencia |
|---|---|---|
| Instalación, permisos de alarma y HyperOS | OK (pantalla completa, bloqueo, ventanas en segundo plano, batería, inicio automático) | `fisico/00_instalacion_v1.1.md` |
| Puntualidad pantalla apagada | **20 de 20** (v1.0: 6 de 10); máximo +0.30 s (v1.0: +187 s) | `fisico/01_puntualidad_v1.1.md` |
| Puntualidad pantalla encendida | 20 de 20 | `fisico/puntualidad_v1.1.csv` |
| Reinicio | 3 de 4 (v1.0: 2 de 4); sin desbloquear no se reprograma; el aviso vencido antes del desbloqueo sale al reprogramar | `fisico/02_reinicio_v1.1.md` |
| PR-03, PR-04, PR-11, salir con pendientes | OK | `fisico/03_alarma_pr03_pr04_pr11_v1.1.md` |
| Límite de 2 minutos | 1.1.0 falló con la pantalla apagada (hasta 385 s); corregido en **1.1.1** (corte como alarma de reloj, +0.1 s) | D44 |
| Pantalla de alarma sobre el bloqueo | Parcial: sola con la pantalla encendida y bloqueada; con la pantalla apagada, solo luz de bordes y notificación | D45 |
| PR-12 | Preparado, no ejecutado (lo observa el usuario) | `fisico/PENDIENTE_v1.1_fisico.md` |
| Capturas para manuales | 13 del teléfono y 6 de ajustes de HyperOS, sin datos personales ni IP | `docs/capturas/LISTA.md` |

Versión actual: **1.1.1** (`versionCode` 3).

## Estado al cierre del 22 de noviembre
Punto de partida para continuar. Resumen corto en [docs/CONTINUAR.md](docs/CONTINUAR.md).

- **Último commit de trabajo:** `2afe9c1` (Fase 10); después va el commit de este cierre, que se subió a `origin/main` junto con los de la Fase 10.
- **Versión actual:** 1.1.1 (`versionCode` 3). Es la que se entrega al grupo beta.
- **APK debug final:** `android/app/build/outputs/apk/debug/app-debug.apk` (no se versiona), 1.1.1, SHA-256 `157fcf454f381fa2daaadd7712a0cd0b86ee1cbe1d080062e9dd6ccd0f3d2938`.
- **Etiqueta local:** `v1.0-evaluada` (no subida).

| Fase | Qué se hizo | Dónde se probó |
|---|---|---|
| 3 | App base: registro, inicio de sesión por rol y medicamentos | Emulador |
| 4 | Tomas de hoy con "Tomado", vínculo y vista del cuidador | Emulador |
| 5 | Primeras alarmas locales (aviso 5 min antes) | Emulador |
| 6 | Alineación con P2 v1.1 y P3: BD v2, vínculo con aceptación, sin red, avisos T menos L y T, alarma del cuidador | API (27 casos) y emulador |
| 7 | Verificación de la Fase 6 (PR-01 a PR-15) | Emulador |
| 8 | Salir sin perder confirmaciones, paquete de hosting, historial de 7 días; verificación de la v1.0 | Emulador y teléfono físico (POCO M6 Pro, HyperOS) |
| 9 | 1.1.0: alarma del paciente a T repetida (máx. 2 min), una alarma de reloj por aviso | Emulador |
| 10 | v1.1 en el teléfono; 1.1.1 corrige el corte de 2 min | Teléfono físico |

**Resultados reales, v1.0 contra v1.1 (teléfono físico, con cable y batería sin restricciones):**

| Caso | v1.0 | v1.1 |
|---|---|---|
| Puntualidad, pantalla apagada | 6 de 10, máximo +187 s | 20 de 20, máximo +0.30 s (mediana +0.07 s) |
| Puntualidad, pantalla encendida | 10 de 10 | 20 de 20 |
| Reinicio | 2 de 4 | 3 de 4 (el aviso vencido antes de desbloquear sale +254 s) |
| Corte de la alarma a los 2 min | No aplica | 1.1.0: de 145 a 385 s con pantalla apagada (FALLO); 1.1.1: +0.1 s |
| PR-03, PR-04, PR-11, salir con pendientes | OK | OK |
| PR-12 alarma del cuidador | Parcial (solo rama "sin conexión") | Preparado, no ejecutado |

La 1.1.1 no tiene retest en el emulador (cambio de una línea, probado en el teléfono, 79 de 79 pruebas unitarias). Lo que se escucha o se siente (sonido, vibración, No molestar) no lo comprobó ninguna persona. Pendientes en orden de prioridad: [docs/PENDIENTES_FASE6.md](docs/PENDIENTES_FASE6.md).

## Bloqueos y pendientes
Ver [docs/PENDIENTES_FASE6.md](docs/PENDIENTES_FASE6.md). En resumen: pruebas en teléfono físico (puntualidad, reinicio real, ahorro de batería y alarma del cuidador), pantalla para autorizar la dosis (G-07), sin WorkManager, pérdida de la cola solo si el token vence (401) con pendientes, y subir el paquete a un hosting real (pendiente de verificar).

## Commits
Fase 6: `534772d` (AGENTS.md), `7a28090` (BD v2), `632b7d8` (vínculo), `34c5215` (tomas), `dec7bd6` (pruebas API), `f5d0ef2` (avisos), `276b994` (sin red), `223501a` (vínculo en app), `10de222` (cuidador), `e71e6ef` (accesibilidad), `e36b13c` (cierre). Correcciones de la verificación: `c9ea6df`. Fase 7: `40117d3`, `4a34e4e` (subidos a origin). Fase 8: `24917b7` (cierre de sesión), `c1f73db` (paquete de hosting), `0539a35` (capturas), `3fbba32` (historial) y el commit de este resumen. Fase 8 Parte A y verificación física (Parte B y C): de `24917b7` en adelante. La Fase 8 se subió (`827eef4`). Fase 9: de `f71b212` en adelante. Fase 10: de `5d98cfa` a `2afe9c1`, subidos con el commit del cierre del 22 de noviembre.
