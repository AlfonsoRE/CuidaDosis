# Paso 1 (v1.1): puntualidad en el teléfono físico (RNF-01)

- **Fecha:** 21/11/2026 (hora de México). Teléfono POCO M6 Pro, HyperOS OS3.0.307.0, Android 16, por USB y cargando, Wi-Fi de la PC. APK 1.1.0 (`versionCode` 2, commit `198fe64`). L = 5 min.
- **Datos:** `ptel1121@ejemplo.test`; "Medicamento SINTETICO Apagada v11" (10 tomas, 21:52 a 22:10, cada 2 min) y "Medicamento SINTETICO Encendida v11" (10 tomas, 22:20 a 22:29, cada minuto), creados por API. Tras abrir la app: "Avisos programados: 80" y 81 alarmas de la app en `dumpsys alarm` (80 avisos más la de la sincronización).
- **Método (D43):** log del sistema guardado en la PC con un flujo continuo de `adb logcat` filtrado a la app, que no despierta la pantalla. Hora real = primera línea de SystemUI con el id de la notificación (id = hora × 60 + minuto de la toma). Cruce: en el escenario B también se midió con la publicación (`NotificationManagerService:post`) tras cada `sending alarm` (`puntualidad_B_v1.1_wakelock.csv`). Scripts: `api/tests/puntualidad_v11_sysui.php` y `api/tests/puntualidad_v11_csv.php`.

## Resultados
| Escenario | Avisos | Dentro de ±60 s | Mínimo | Mediana | Máximo |
|---|---|---|---|---|---|
| B pantalla apagada (`Dozing` desde 21:39:35, sin tocarla) | 20 (10 previos y 10 alarmas) | **20 de 20** | +0.244 s | +0.307 s | +0.385 s |
| A pantalla encendida (despertada cada 20 s por adb, bloqueada) | 20 | **20 de 20** | +0.246 s | +0.273 s | +1.656 s |

Cruce del escenario B con la publicación: 20 de 20, de +0.037 a +0.295 s. HyperOS anotó "not align this alarm" para cada alarma de la app: no las agrupó.

## Hora de inicio de la alarma a T
- **Sonido:** SystemUI registró `buzzBeep` (sonar y vibrar) de cada alarma a T entre +0.5 y +0.6 s (por ejemplo, toma de 21:52: publicada 21:52:00.064, `buzzBeep` 21:52:00.569).
- **Pantalla de alarma:** con la pantalla apagada (escenario B) **no se abrió**: solo quedó la notificación insistente. Con la pantalla encendida y bloqueada se abrió sola sobre el bloqueo (`occludedChanged mOccluded=true`) en las tomas de 22:25 a 22:29 (por ejemplo, 22:25:00.603, +0.6 s); en las de 22:20 a 22:24 se mostró como notificación emergente sin abrirla. No se determinó qué condición de HyperOS decide entre ambas formas.
- **Sonido real:** que se oiga y vibre lo confirma una persona (pendiente manual).

## Incidencias
- A las 21:54:46 se abrió la pantalla principal de la app (`MainActivity`) durante el escenario B; nadie operó el teléfono desde la automatización y no se identificó la causa en el log filtrado. La pantalla volvió a apagarse y no alteró las mediciones (todos los avisos posteriores dentro de 0.4 s).
- **Límite de 2 minutos:** HyperOS agrupó los cortes de 2 minutos (programados con `setExactAndAllowWhileIdle`): la alarma siguió insistente entre 145 y 385 s (`limite_2min_v1.1.0.csv`). Se corrigió en la 1.1.1 (D44) y se vuelve a probar en el paso 6.

Archivos: `puntualidad_v1.1.csv`, `puntualidad_B_v1.1_wakelock.csv`, `puntualidad_v1.1_log_sistema.log`, `limite_2min_v1.1.0.csv`.
