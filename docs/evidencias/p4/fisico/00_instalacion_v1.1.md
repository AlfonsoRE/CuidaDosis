# Paso 0 (v1.1): instalación, permisos y configuración

- **Fecha:** 21/11/2026, 21:30 a 21:38 (hora de México).
- **Teléfono:** POCO M6 Pro (Xiaomi, 2312FPCA6G), HyperOS OS3.0.307.0, Android 16 (SDK 36), por USB y cargando.
- **Red:** Wi-Fi de la PC; el teléfono llega a la API (`HTTP/1.1 200 OK` con `nc`). La otra red guardada que causaba fallos en la v1.0 ya no está entre las redes del teléfono.
- **APK:** `app-debug.apk`, `versionName` 1.1.0, `versionCode` 2, SHA-256 `21227df1afd6f9252796f89e8cb8a9ade6f69c148f2df1aeaa75026497a91240`, compilado del commit `198fe64`. Desde ese commit no cambió código de la app, la API ni la base (hasta `823eab4`). Instalación: `adb -s <ID> install -r` → `Success`.

## Permisos y configuración (condición de todas las pruebas de la v1.1)
| Elemento | Estado | Cómo se verificó |
|---|---|---|
| `POST_NOTIFICATIONS` | Concedido | `dumpsys package` |
| `USE_EXACT_ALARM` | Concedido | `dumpsys package` |
| `USE_FULL_SCREEN_INTENT` | Concedido; appop `USE_FULL_SCREEN_INTENT: allow`; en Ajustes "Permitir notificaciones a pantalla completa" activado | `dumpsys package`, `cmd appops get`, captura `hyperos_pantalla_completa.png` |
| `VIBRATE`, `RECEIVE_BOOT_COMPLETED` | Concedidos | `dumpsys package` |
| Ahorro de batería | "Sin restricciones"; en la lista de exenciones (`user,mx.cuidadosis.app`); grupo de reposo 5 (EXEMPTED); `RUN_ANY_IN_BACKGROUND: allow` | `dumpsys deviceidle`, `am get-standby-bucket`, captura `hyperos_bateria_sin_restricciones.png` |
| Inicio automático (HyperOS) | Activado (`MIUIOP(10008): allow`) | `cmd appops get`, captura `hyperos_inicio_automatico.png` |
| Mostrar en pantalla de bloqueo (HyperOS) | "Permitir siempre" | Info. de la aplicación → Otros permisos, captura `hyperos_otros_permisos.png` |
| Abrir nuevas ventanas mientras se ejecuta en segundo plano (HyperOS) | "Permitir siempre" | Misma pantalla |
| Notificaciones de la app | Todas activas: flotantes, en pantalla de bloqueo, sonido, vibración, permanentes; 4 categorías v1.1 | Capturas `hyperos_notificaciones_categorias*.png` |
| Canales | `intake_before_v2`, `intake_alarm_v2` (importancia 4, vibración), `intake_alarm_silent_v2` (importancia 3, sin vibración), `caregiver_alerts_v2`; los de la v1.0 (`intake_reminders`, `caregiver_alerts`) quedaron `mDeleted=true` | `dumpsys notification` |

## Limpieza de datos de prueba
Se desactivaron por API (`DELETE /medications/{id}`, que desactiva sin borrar) los medicamentos sintéticos anteriores: 3 de `ptel1121@ejemplo.test` y 1 de `palarma1121@ejemplo.test`. Tras reiniciar la app, "Avisos programados: 0" y ninguna alarma de la app en `dumpsys alarm`.
