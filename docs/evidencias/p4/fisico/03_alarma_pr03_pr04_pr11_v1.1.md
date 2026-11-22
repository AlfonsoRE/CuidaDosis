# Pasos 3 a 7 (v1.1): alarma, Tomado, Posponer, límite de 2 minutos y cierre de sesión

- **Fecha:** 21/11/2026, 22:52 a 23:32 (hora de México). APK 1.1.1 (`versionCode` 3). Paciente `ptel1121@ejemplo.test`, "Medicamento SINTETICO Alarma v11" con tomas a las 23:02, 23:06, 23:10 y 23:16 (desactivado al terminar). Wi-Fi de la PC estable salvo el modo avión del paso 7.
- **Evidencia:** `alarma_v1.1_log.txt` (toques, BD y log del sistema), `limite_2min_v1.1.1.csv` y capturas en `docs/capturas/fisico/` (17 a 24).
- **Quién observó:** solo la automatización por adb. Nadie oyó ni sintió la alarma: el sonido y la vibración siguen pendientes con una persona.

## Paso 3: PR-03 (aviso previo y alarma)
| Caso | Resultado | Dato |
|---|---|---|
| Aviso a T-5 | OK | "Toma de las 23:02 · En 5 minutos" publicado a las 22:57 (captura `17_aviso_previo_v11.png`, recortada) |
| Alarma a T | OK | "Es la hora de tu toma de las 23:02": publicada 23:02:00.08, `buzzBeep` 23:02:00.64; botones Tomado, Posponer 5 min y Posponer 10 min (`18_alarma_notificacion_v11.png`) |
| Pantalla de alarma | OK al abrirla | Se abre al tocar la notificación (`19_alarma_pantalla_v11.png`) |
| Pantalla de alarma sola sobre el bloqueo | **Parcial** | En el paso 1 se abrió sola sobre el bloqueo (22:25 a 22:29, `mOccluded=true`). En este paso no: con la pantalla apagada (23:02) HyperOS solo encendió la luz de bordes y dejó la notificación; a las 23:06 y 23:10 la pantalla bloqueada se apagaba sola en segundos (tiempo de espera de la pantalla de bloqueo) y la alarma quedó como notificación en el bloqueo. Sobre el bloqueo, un toque en la notificación no la abre (HyperOS pide desbloquear). Se deja la captura sobre el bloqueo como pendiente manual |
| Notificación fija tras 2 min | OK | "Es la hora de tu toma de las 23:06 · Sigue pendiente", sin sonido, con los tres botones (`20_alarma_sin_sonido_v11.png`) |

## Paso 4: PR-04 ("Tomado" con un toque)
| Vía | Toque | Resultado | BD (`intake_logs`) |
|---|---|---|---|
| Notificación (toma 23:02) | 23:03:42 | Sin diálogo; la notificación desapareció y dejó de sonar | `taken`, confirmada 23:03:42, recibida 23:03:42, `client_uuid` 3a3aa971… |
| Pantalla de alarma (toma 23:16, tras posponer) | 23:23:48 | Sin diálogo; la pantalla se cerró y la notificación desapareció | `taken`, confirmada 23:23:51, recibida 23:23:51, `client_uuid` 5ed2f716… |

**Incidencia:** la toma de 23:10 se confirmó a las 23:22:03 sin un toque deliberado: el gesto de desbloqueo de la automatización cayó en el botón "Tomado" de la notificación emergente (como en el emulador). No cuenta como prueba.

## Paso 5: PR-11 (Posponer)
| Acción | Toque | Volvió a sonar | Esperado |
|---|---|---|---|
| "Posponer 10 min" en la pantalla de alarma (toma 23:10) | 23:11:29 (procesado 23:11:32) | 23:21:32.06 (publicada) | 23:21:32 |
| "Posponer 5 min" en la notificación (toma 23:16) | 23:16:39 (procesado 23:16:39.7) | 23:21:39.6 (`buzzBeep`), título "Recordatorio: toma de las 23:16" | 23:21:39.7 |

En ambos casos la notificación desapareció al posponer y la toma siguió pendiente en la BD hasta confirmarla.

## Paso 6: límite de 2 minutos (1.1.1)
| Alarma | Corte esperado | Corte real | Pantalla |
|---|---|---|---|
| 22:46 (paso 2) | 22:48:00.15 | 22:48:00.18 | encendida |
| 23:06 | 23:08:00.05 | 23:08:00.13 (publicación) | **apagada** desde 23:06:48 |

Tras el corte, la notificación quedó en `intake_alarm_silent_v2` (importancia 3) con `ONGOING_EVENT|ONLY_ALERT_ONCE|SILENT`. Con la 1.1.0 el corte se retrasó de 25 a 265 s con la pantalla apagada (`limite_2min_v1.1.0.csv`); con la 1.1.1, menos de 0.2 s.

## Paso 7: cierre de sesión con una confirmación pendiente
1. 23:24:27, modo avión activado.
2. 23:24:45, "Tomado" en "Hoy" para la toma de 23:06: la tarjeta pasó a "Confirmada por el paciente a las 23:24" y quedó en la cola local.
3. "Salir": diálogo "Confirmaciones sin enviar" con "Quedarme" y "Reintentar envío" (`22_salir_con_pendientes_v11.png`). Se tocó "Quedarme" y la sesión siguió abierta.
4. Al quitar el modo avión, la cola se envió sola: la BD registró la toma confirmada a las 23:24:45 y recibida a las 23:25:48, una sola fila (`client_uuid` 3f3ab810…). Un "Actualizar" justo al reconectar mostró "Sin conexión con el servidor" (el Wi-Fi aún se estaba conectando); a las 23:26 sincronizó bien.
5. "Salir" ya sin pendientes cerró la sesión de inmediato y canceló todas las alarmas de la app (0 en `dumpsys alarm`).
