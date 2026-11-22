# Verificación en teléfono físico: resumen

- **Fecha:** 21/11/2026, de 14:40 a 17:05 (hora de México).
- **Teléfono:** POCO M6 Pro (Xiaomi, 2312FPCA6G), HyperOS OS3.0.307.0, Android 16 (SDK 36), zona `America/Mexico_City` con hora automática.
- **Segundo dispositivo:** emulador `Medium_Phone_API_37.0` (Android 17).
- **Servidor:** XAMPP local en la PC (Apache 2.4.58, PHP 8.2.12, MariaDB 10.4.32), por Wi-Fi.
- **Código:** commit `dd863c0`; APK debug 1.0 (1), SHA-256 `4944677b3fe8ced85e3ce72665d432cdc3fca97b59b14cd804ecfd42bdc749ff`, compilado del commit `3fbba32`.
- **Condiciones de HyperOS en todas las pruebas:** notificaciones permitidas, `USE_EXACT_ALARM` concedido, Ahorro de batería **Sin restricciones** (en la lista de exenciones del sistema), **Autoencendido en segundo plano** activado, grupo de reposo 5 (EXEMPTED). Teléfono conectado por USB y cargando en todas las pruebas.
- **Cuentas:** `ptel1121@ejemplo.test` (paciente), `ctel1121@ejemplo.test` (cuidador) y `palarma1121@ejemplo.test` (paciente para PR-12). Contraseña de prueba `Sintetico2026`. Solo datos sintéticos.

## Resultados
| Paso | Caso | Resultado | Dato medido | Evidencia |
|---|---|---|---|---|
| 0 | Instalación | OK | `adb install` = Success; "Probar conexión" correcta | `00_instalacion_y_compatibilidad.md` |
| 1 | Compatibilidad | OK | Android 16 SDK 36; permisos y batería según condiciones | `00_instalacion_y_compatibilidad.md` |
| 2A | RNF-01 pantalla encendida (5 tomas, avisos T-5 y T) | **OK, 10 de 10** | Diferencia de +0.013 a +0.036 s | `puntualidad.csv` |
| 2B | RNF-01 pantalla apagada (5 tomas) | **FALLO, 6 de 10** (meta 9 de 10) | 6 avisos entre +0.02 y +17 s; 4 entre +67 y +187 s | `puntualidad.csv`, `puntualidad_B_log_sistema.log` |
| 3 | RNF-01 reposo prolongado sin cable | **NO EJECUTADO** | Requiere desconectar el cable y emparejar la depuración inalámbrica | ver "No medido" |
| 4 | RNF-03 reinicio (2 tomas) | **Parcial** | Reprogramación sin abrir la app 2 min 18 s después de desbloquear (antes de desbloquear no hay alarma); 2 de 4 avisos a tiempo, 2 con +145 s | `reinicio.csv`, `reinicio_observacion.txt` |
| 5 | PR-08 sin red, sin duplicados | OK | 1 fila con 1 UUID; enviada 35 s después de volver la red | `paso05_pr08_sin_red.md` |
| 6 | PR-03 avisos a T-5 y T | OK con pantalla encendida; FALLO con pantalla apagada (paso 2B) | ver paso 2 | `paso06_pr03_pr04.md` |
| 6 | PR-04 un toque sin diálogo | OK | Sin diálogo; "Confirmada por el paciente a las 16:06"; llegó al servidor al reconectar | `paso06_pr03_pr04.md` |
| 7 | PR-05 vínculo (teléfono y emulador) | OK | Código de 10 min → esperando aceptación (404) → aceptado (200) → revocado (404, `revoked_at`) | `paso07_pr05_vinculo.md` |
| 8 | Salir con pendientes | OK | Bloqueado sin red; la confirmación llegó al volver la red | `paso08_salir_con_pendientes.md` |
| 8 | Token vencido con pendientes | LIMITACIÓN | No corregida ni reproducida | `paso08_salir_con_pendientes.md` |
| 9 | RF-15 historial | OK | Paciente con dosis; cuidador sin dosis (0 campos `dose`) | `paso09_historial.md` |
| 10 | SEG-05 | OK | 22 rutas sensibles → 403 | `seg05_acceso.csv` |
| 11 | PR-12 alarma del cuidador a T+30 | **Parcial** | Alarma a las 16:58:00.008 y notificación a las 16:58:02 (a tiempo, con la pantalla apagada); se observó la rama "sin conexión" ("Revisa a..."), no la rama "Toma sin confirmar" | `paso11_pr12_alarma_cuidador.md` |

## No medido y por qué
- **Escenario C (reposo prolongado sin cable):** necesita que alguien desconecte el cable y empareje la depuración inalámbrica con un código que solo da el teléfono. No se simuló.
- **PR-12, rama "Toma sin confirmar":** el teléfono se pasa solo a otra red Wi-Fi guardada sin ruta a la PC; al sonar la alarma no pudo consultar la API. El segundo intento se detuvo por la misma causa.
- **Token vencido con pendientes:** requiere esperar 30 días o invalidar el token en la base; quedó como limitación.
- **Tiempo de respuesta por Wi-Fi desde el teléfono (RNF-07):** no se midió en el teléfono (no tiene `curl`); la medición de 2 s se hizo en localhost.

## Limitaciones de esta verificación
- **Cable:** todas las pruebas se hicieron con el USB conectado y el teléfono cargando. Con carga, Android no entra en Doze profundo, así que el escenario B no representa el reposo real sin cable. Aun así, HyperOS retrasó alarmas exactas.
- **Simulaciones:** ninguna. Las tomas se crearon por API (el mismo endpoint que usa la app) para tener horas cada 2 minutos, que el selector de la app no permite. Los toques fueron por adb.
- **Un solo modelo:** POCO M6 Pro con HyperOS. Otros fabricantes pueden comportarse distinto.
- **Configuración de batería:** se probó solo con "Sin restricciones" e Inicio automático activados. Con la configuración de fábrica ("Ahorro de batería recomendado") se esperan más retrasos; no se midió.
- **Red:** el teléfono alterna entre dos redes Wi-Fi guardadas; eso afectó a los pasos 6 y 11.
- **Bloqueo de pantalla:** la pantalla se apaga a los 60 s; para los pasos con toques se desbloqueó con [PIN]. En los escenarios B y C la pantalla no se despertó hasta medir.

## Hallazgo principal
Con la pantalla apagada, HyperOS entregó tarde (de 67 a 187 s) algunas alarmas exactas programadas con `setExactAndAllowWhileIdle`, aun con batería sin restricciones. Como la app encadena una sola alarma (la del próximo evento), un retraso arrastra a los eventos siguientes: al llegar la alarma atrasada, los pendientes salen de golpe, cada 5 s. Mejoras posibles, sin probar: `setAlarmClock()` o una alarma por evento. Ver `docs/PENDIENTES_FASE6.md`.

---

# Verificación de la v1.1 en el teléfono físico (Fase 10)

- **Fecha:** 21/11/2026, de 21:30 a 23:35 (hora de México). Mismo teléfono, misma configuración de HyperOS (batería "Sin restricciones", Inicio automático), USB conectado y cargando, Wi-Fi de la PC (la otra red guardada ya no está en el teléfono).
- **APK:** 1.1.0 (`versionCode` 2, SHA-256 `21227df1…1240`) para instalación y puntualidad; **1.1.1** (`versionCode` 3, SHA-256 `157fcf45…2938`, D44) para reinicio, alarma, Posponer, límite de 2 minutos y cierre de sesión.
- **Evidencia:** `00_instalacion_v1.1.md`, `01_puntualidad_v1.1.md`, `02_reinicio_v1.1.md`, `03_alarma_pr03_pr04_pr11_v1.1.md` y los CSV y logs con sufijo `_v1.1`.
- **Observación humana:** ninguna. Todo se midió por adb y logs; nadie oyó ni sintió la alarma (pendiente en `PENDIENTE_v1.1_fisico.md`).

## Resultados v1.1
| Caso | Resultado | Dato |
|---|---|---|
| Instalación y permisos (pantalla completa, bloqueo, ventanas en segundo plano, batería, inicio automático) | OK | `00_instalacion_v1.1.md` y capturas `hyperos_*.png` |
| RNF-01 pantalla apagada (10 tomas, 20 avisos) | **OK, 20 de 20** | +0.24 a +0.39 s (SystemUI); +0.04 a +0.30 s (publicación) |
| RNF-01 pantalla encendida (10 tomas, 20 avisos) | **OK, 20 de 20** | +0.25 a +1.66 s |
| RNF-03 reinicio (2 tomas) | **Parcial, 3 de 4** | Sin desbloquear no se reprograma; tras desbloquear, 2 min 39 s; el aviso que venció antes del desbloqueo salió al reprogramar (+254 s) |
| PR-03 aviso previo y alarma a T | OK | Alarma publicada a T y `buzzBeep` (sonido y vibración) a +0.5 s |
| Pantalla de alarma sobre el bloqueo | **Parcial** | Se abrió sola con la pantalla encendida y bloqueada (22:25 a 22:29); con la pantalla apagada HyperOS solo encendió la luz de bordes |
| PR-04 Tomado (notificación y pantalla de alarma) | OK | Un toque, sin diálogo; deja de sonar; BD con `client_uuid` |
| PR-11 Posponer 5 y 10 | OK | Vuelve a sonar al plazo (±0.1 s) |
| Límite de 2 minutos | 1.1.0: **FALLO**; 1.1.1: **OK** | 1.1.0: HyperOS agrupó los cortes (sonó 145 a 385 s). 1.1.1: corte a +0.1 s con la pantalla apagada |
| Salir con pendientes (modo avión) | OK | Diálogo; Quedarme; la cola se envió al volver la red, una fila |
| PR-12 alarma del cuidador | **NO EJECUTADO** | Preparado; lo observa el usuario (`PENDIENTE_v1.1_fisico.md`) |

## Comparación v1.0 contra v1.1 (mismo teléfono y condiciones)
Diferencia = hora real menos hora esperada; meta RNF-01: ±60 s. En la v1.0 se midieron 5 tomas por escenario (10 avisos); en la v1.1, 10 tomas (20 avisos). La v1.1 se mide con la publicación en el log del sistema, el mismo método que la v1.0 usó con la pantalla apagada.

| Escenario | Versión | Cumplen | Mínimo | Mediana | Máximo |
|---|---|---|---|---|---|
| Pantalla apagada | v1.0 | 6 de 10 (60 %) | +0.02 s | +12.2 s | +187.2 s |
| Pantalla apagada | v1.1.0 | **20 de 20 (100 %)** | +0.04 s | +0.07 s | +0.30 s |
| Reinicio | v1.0 | 2 de 4 (50 %) | -11.7 s | +72.6 s | +145.1 s |
| Reinicio | v1.1.1 | **3 de 4 (75 %)** | +0.08 s | +8.3 s | +254.5 s |
| Pantalla encendida (referencia) | v1.0 | 10 de 10 | +0.01 s | +0.02 s | +0.04 s |
| Pantalla encendida (referencia) | v1.1.0 | 20 de 20 | +0.25 s | +0.27 s | +1.66 s |

Lectura:
- **Pantalla apagada:** la v1.1 corrige el retraso. Con una alarma de reloj por aviso, HyperOS no las agrupa ("not align this alarm"); en la v1.0 la cadena de una sola alarma exacta se retrasó hasta 187 s.
- **Reinicio:** mejora en los avisos posteriores al desbloqueo (todos a tiempo), pero **no** en el que vence antes del desbloqueo: Android no entrega `BOOT_COMPLETED` hasta el primer desbloqueo (D42) y HyperOS tarda unos 2.5 min más en entregarlo a la app. En la v1.1 se esperó a propósito con el teléfono bloqueado hasta pasar el primer aviso, por eso su máximo (+254 s) es mayor; en la v1.0 se desbloqueó 10 min antes del primer aviso y el retraso fue de HyperOS. Las condiciones no son idénticas.
- **Pantalla encendida:** las dos cumplen; la v1.1 se midió con otra fuente (SystemUI), que suma unos 0.25 s de dibujo, por eso sus valores son mayores.
- **Límite:** cable conectado y cargando en las dos versiones (sin Doze profundo); un solo modelo.

## Observación del emulador (no del teléfono)
El usuario oyó el sonido de la alarma en las bocinas de la laptop con el emulador (Fase 9). Es una observación del emulador; en el teléfono el sonido no se ha confirmado por una persona.
