# Retest de la v1.1 en el emulador

- **Fecha:** 21/11/2026, de 19:26 a 21:06 (hora de México).
- **Dispositivo:** emulador `Medium_Phone_API_37.0`, Android 17 (API 37), en la PC (`adb -s emulator-5554`). El teléfono físico no se usó en esta fase.
- **Versión:** 1.1.0 (`versionCode` 2).
  - Las pruebas funcionales y de puntualidad corrieron con el APK del commit `ad01d7d` (SHA-256 `4f0e572f204405ca5b052d4817c06bba6f5b1269ec7750d94b843e1186cb47bc`).
  - El reinicio y la regresión corrieron con el APK final del commit `198fe64` (SHA-256 `21227df1afd6f9252796f89e8cb8a9ade6f69c148f2df1aeaa75026497a91240`), que solo agrega una corrección visual a la pantalla de alarma.
- **Condiciones:** API local en XAMPP; app exenta de ahorro de batería en el emulador; L = 5 min; datos sintéticos nuevos (`pv11@ejemplo.test` paciente, `cv11@ejemplo.test` cuidador, contraseña de prueba `Sintetico2026`).

> **Los tiempos del emulador NO son representativos del teléfono.** El emulador corre en la PC, conectado a la corriente, sin restricciones de fabricante; con pantalla apagada entra en modo "Dozing" pero no en el reposo real de un teléfono. La comparación válida con la v1.0 está pendiente en el teléfono físico (`docs/evidencias/p4/fisico/PENDIENTE_v1.1_fisico.md`).

## Resultados
| Caso | Resultado | Dato observado | Evidencia |
|---|---|---|---|
| Canales v1.1 | OK | Existen `intake_before_v2` (importancia 4, vibración, `USAGE_NOTIFICATION`), `intake_alarm_v2` (importancia 4, vibración, sonido de alarma, `USAGE_ALARM`), `intake_alarm_silent_v2` (importancia 3, sin sonido ni vibración) y `caregiver_alerts_v2` (importancia 4, vibración). Los canales v1.0 `intake_reminders` y `caregiver_alerts` ya no existen | `canales.txt` |
| Una alarma por aviso | OK | 4 tomas → 16 alarmas separadas, cada una `RTC_WAKEUP` con `window=0`, `exactAllowReason=policy_permission`, `flags=0x3` (alarma de reloj); con 27 tomas, 93 alarmas | dumpsys alarm (ver texto) |
| PR-03 aviso a T-L | OK | Canal `intake_before_v2`, `AUTO_CANCEL`, publicado a T-5 (p. ej. 19:40:00.008 para la toma de 19:45) | `funcionales_notificaciones.csv` |
| PR-03 alarma a T | OK | Canal `intake_alarm_v2`, importancia 4, banderas `ONGOING_EVENT|INSISTENT|HIGH_PRIORITY`, publicada a T (19:42:00.013, 19:45:00.010, 19:51:00.013) | `funcionales_notificaciones.csv`, capturas 20 y 20b |
| PR-04 "Tomado" con un toque | OK | En la pantalla de alarma, un toque: sin diálogo; la notificación se canceló (deja de sonar) y el corte de 2 min quedó cancelado (`alarm_cancelled` a las 19:42:43); BD: confirmada 19:42:43, recibida 19:42:45. También desde el botón "Tomado" de la notificación (toma de 19:45 tras posponer): confirmada 20:00:31, recibida 20:00:35 | sección Detalle |
| PR-11 "Posponer 5" y "Posponer 10" | OK | Posponer 5 a las 19:45:13: la notificación desapareció; volvió a sonar a las 19:50:13.033. Posponer 10 a las 19:50:24: volvió a sonar a las 20:00:23.010 | `funcionales_notificaciones.csv` |
| Toma ya confirmada | OK | La toma de 19:48, confirmada a las 19:34 desde "Hoy": no hubo aviso previo ni alarma (0 apariciones en el monitor) | monitor |
| Límite de 2 minutos | OK | Alarma de 19:51 sin atender: a las 19:53:00.043 la notificación pasó a `intake_alarm_silent_v2` con `ONGOING_EVENT|ONLY_ALERT_ONCE|SILENT` (sin `INSISTENT`) | captura 20c |
| Puntualidad, pantalla encendida (10 tomas) | 20 de 20 dentro de ±60 s | Diferencias de +0.007 a +1.190 s | `puntualidad_v1.1.csv` |
| Puntualidad, pantalla apagada (10 tomas) | 20 de 20 dentro de ±60 s | Diferencias de +0.010 a +0.979 s; pantalla apagada (`Dozing`) de 20:22:02 hasta el final, sin tocarla | `puntualidad_v1.1.csv` |
| Reinicio con 2 tomas | OK | Tras `adb reboot`, sin abrir la app, BootReceiver reprogramó 52 avisos (1 min 43 s después del arranque); los 4 eventos salieron entre +0.11 y +0.33 s | `reinicio_v1.1.csv` |
| Regresión: inicio de sesión y registro | OK | Registro de `pv11` desde la app; inicios de sesión posteriores correctos | |
| Regresión: alta de medicamento | OK | "Medicamento SINTETICO Regresion" a las 21:00 desde la interfaz | |
| Regresión: vínculo con aceptación | OK | Código en el emulador → cuidador por API (`awaiting_acceptance`) → aceptar en la app → el cuidador recibe 200 | |
| Regresión: sin red y sincronización | OK | Modo avión, "Tomado" en la toma de 19:51: "1 confirmación por enviar"; al volver la red se recibió a las 21:02:41, 1 fila | |
| Regresión: historial | OK | "Últimos 7 días · Hoy: 5 de 27 tomas registradas, 13 sin confirmar" | |
| Regresión: cierre de sesión con pendientes | OK | Diálogo "Confirmaciones sin enviar" con Quedarme y Reintentar envío | captura 22 |
| Regresión: alarma del cuidador a T+30 | OK | Toma de 20:35 de otro paciente; notificación "Toma sin confirmar" a las 21:05:00.570 (+0.57 s), canal `caregiver_alerts_v2`, `AUTO_CANCEL` (sin repetir) | captura 21 |
| Pruebas unitarias | 79 de 79 OK | 71 anteriores y 8 nuevas en `AlarmSchedulingTest` | |

## No se pudo probar y por qué
- **Que la alarma se escuche y vibre:** el emulador no permite oír ni sentir la alarma desde la automatización. Solo se verificó la configuración (canal con sonido de alarma y vibración, bandera `INSISTENT`) y que la repetición se corta a los 2 minutos. Pendiente en el teléfono físico, con una persona.
- **Modo silencio, vibración y No molestar:** no se probaron.
- **Pantalla completa sobre el bloqueo:** el emulador no tiene bloqueo de pantalla. La pantalla de alarma se abrió al tocar la notificación (captura 20); falta ver si aparece sola con el teléfono bloqueado.
- **Avisos antes del primer desbloqueo tras reiniciar:** no aplica en el emulador (no tiene bloqueo); en el teléfono sigue haciendo falta desbloquear (D42).

## Incidencias durante la prueba
- Dos veces, un toque de la automatización sobre "Salir" cerró la sesión del paciente cuando no había pendientes. Es el comportamiento correcto sin pendientes, pero canceló las alarmas; se volvió a entrar antes del escenario A (20:04:17, 92 alarmas reprogramadas) y antes de la regresión.
- Mientras una alarma suena, su notificación emergente tapa la barra superior de la app (incluido "Salir"); hay que deslizarla hacia arriba o atenderla. Un toque de la automatización cayó en el botón "Tomado" de esa notificación y confirmó la toma de 20:54 (después de medir su puntualidad). Se anota como observación de uso.
- La pantalla de alarma quedaba pegada a la barra de estado; se corrigió en `198fe64`.
- El emulador mostró dos veces "System UI isn't responding" justo después de arrancar en frío; se cerró con "Wait".

## Limitaciones
- Emulador en la PC: tiempos no representativos del teléfono, sin restricciones de fabricante y con la app exenta de ahorro de batería.
- Sonido y vibración no audibles ni medibles en el emulador.
- Un solo dispositivo virtual (API 37).
