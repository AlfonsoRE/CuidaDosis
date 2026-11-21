# Paso 11: PR-12 alarma del cuidador a T + 30 (RF-18)

Teléfono físico como **cuidador** (`ctel1121@ejemplo.test`), POCO M6 Pro, Android 16, HyperOS; paciente sintético `palarma1121@ejemplo.test` creado por API, vinculado desde la app del teléfono (código escrito en el teléfono y aceptado por API); emulador sin sesión. Batería "Sin restricciones", Inicio automático, notificaciones concedidas. USB conectado. Commit `dd863c0`.

## Corrida 1 (observada)
| Elemento | Valor |
|---|---|
| Toma sin confirmar | "Medicamento SINTETICO Alarma" a las 16:28, creada por API y nunca confirmada |
| Hora esperada de la alarma (T + 30) | **16:58:00** (`dumpsys alarm`: `origWhen=2026-11-21 16:58:00.000`, `window=0`, `exactAllowReason=policy_permission`) |
| Alarma entregada | 16:58:00.008 (`[xiaomi_power]sending alarm ... CAREGIVER_CHECK`), pantalla apagada (`mWakefulness=Dozing`) |
| Notificación publicada | 16:58:02.438 (`when` de dumpsys), +2.4 s |
| Contenido | "Revisa a Paciente SINTETICO Alarma: No se pudo consultar la toma de las 16:28 (sin conexión). El teléfono del paciente aún no ha sincronizado." |
| Causa del "sin conexión" | La app registró "Error de red al llamar a la API" a las 16:58:02.429. El teléfono se había pasado a otra red Wi-Fi guardada (no la de la PC), del mismo rango 192.168.1.x pero sin ruta a la PC (`No route to host`) |

**Resultado:**
- Alarma a T + 30: **OK** (a tiempo, +0.008 s la entrega y +2.4 s la notificación, con la pantalla apagada).
- Rama "sin conexión" de RF-18 (avisar que revise, con la última sincronización conocida): **OK**, se observó tal como la define el requisito.
- Rama "sigue sin confirmar" (aviso "Toma sin confirmar" tras consultar la API): **NO OBSERVADA en el teléfono físico**, pendiente de ejecución manual. Sí se observó en el emulador (Fase 7, PR-12).

## Corrida 2 (no completada)
Se intentó repetir a las 17:01 con una toma de las 16:34 (T + 30 = 17:04), después de reconectar el teléfono a la red de la PC. En menos de un minuto el teléfono volvió a la otra red guardada y la app no pudo descargar las tomas para programar la alarma. Se detuvo la corrida sin marcarla como OK.
