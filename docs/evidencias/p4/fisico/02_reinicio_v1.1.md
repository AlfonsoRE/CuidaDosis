# Paso 2 (v1.1): reinicio con 2 tomas (RNF-03)

- **Fecha:** 21/11/2026 (hora de México). APK 1.1.1 (`versionCode` 3; frente a la 1.1.0 solo cambia el corte de 2 minutos, D44). Paciente `ptel1121@ejemplo.test`, "Medicamento SINTETICO Reinicio v11" con tomas a las 22:46 y 22:50; la app sincronizó y programó 8 avisos (hoy y mañana) antes de reiniciar.

## Secuencia
| Hora | Hecho |
|---|---|
| 22:34:33 | `adb reboot` (sin abrir la app después) |
| 22:35:57 | Android envía `LOCKED_BOOT_COMPLETED`; `sys.boot_completed=1` a las 22:35:58 |
| 22:35:58 a 22:41:50 | Pantalla bloqueada; `dumpsys alarm` cada 20 s: **0 alarmas de la app**. El aviso de las 22:41 (T-5 de la toma de 22:46) **no salió** |
| 22:42:24 | Desbloqueo con [PIN], sin abrir la app |
| 22:42:26 | Android envía `BOOT_COMPLETED` |
| 22:45:03 | `BootReceiver` de la app reprograma: "Avisos programados: 8" (**2 min 39 s después del desbloqueo**; HyperOS retrasa la entrega a apps de terceros) |

## ¿Se reprograman sin desbloquear?
**No.** Igual que en la v1.0 (D42): la app no recibe `BOOT_COMPLETED` hasta el primer desbloqueo, y la copia local cifrada con Android Keystore no se puede leer antes.

## Avisos (`reinicio_v1.1.csv`)
| Evento | Esperado | Real | Diferencia |
|---|---|---|---|
| T-5, toma 22:46 | 22:41:00 | 22:45:14.5 | +254 s (no cumple; pasó antes del desbloqueo y salió al reprogramar) |
| T-5, toma 22:50 | 22:45:00 | 22:45:16.4 | +16 s (cumple) |
| T, toma 22:46 | 22:46:00 | 22:46:00.1 | +0.1 s |
| T, toma 22:50 | 22:50:00 | 22:50:00.1 | +0.1 s |

**3 de 4 dentro de ±60 s** (v1.0: 2 de 4). El aviso perdido no se perdió del todo: la app lo mostró en cuanto pudo.

## Corte de 2 minutos con la 1.1.1 (dato adicional)
La alarma de 22:46 se cortó a las 22:48:00.180 (+0.18 s). La pantalla estaba encendida y desbloqueada; la prueba con la pantalla apagada es el paso 6.

Archivos: `reinicio_v1.1.csv`, `reinicio_v1.1_log.txt`.
