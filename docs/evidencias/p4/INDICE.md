# Índice de evidencias P4 (Fases 6 a 10)

Todas las evidencias usan solo datos sintéticos (nombres con "SINTETICO" y correos `@ejemplo.test`). Fecha de ejecución: 21/11/2026, hora de México.

## Pruebas automáticas de la API
| Archivo | Qué es | Resultado |
|---|---|---|
| `api-20261121-1215.log` | Primera corrida completa de `api/tests/run_api_tests.php` (paso 4) | 27 OK, 0 FALLO |
| `api-20261121-1223.log` | Corrida tras agregar `schedules` a medicamentos (paso 5) | 27 OK, 0 FALLO |
| `api-20261121-1321.log` | Corrida final de la Fase 7 | 27 OK, 0 FALLO |

Script: `api/tests/run_api_tests.php` (casos API-01 a API-27: registro y sesión, vínculo con aceptación, código vencido, 5 fallos y 429, sincronización repetida con el mismo UUID, "Sin confirmar" a T + 30 con horas movidas en BD, dosis oculta, cuidador sin vínculo, acceso perdido al revocar). Se comprobó que detecta errores: con la regla cambiada a propósito a 20 min, API-23 falló (ver `docs/decisiones-fase-6.md`, D20).

## Pruebas en emulador
| Archivo | Qué es |
|---|---|
| `pruebas-emulador.csv` | Casos PR-01 a PR-15 de `docs/p3/trazabilidad_p3.csv` ejecutados en el emulador `Medium_Phone_API_37.0` (Android 17), con condiciones, pasos, esperado, observado, OK/FALLO, incidencia, fecha y versión. Lo que requiere teléfono físico dice "pendiente de ejecución manual". |

## Rendimiento
| Archivo | Qué es |
|---|---|
| `rendimiento.csv` | Tiempo de respuesta de la consulta del día (`GET /intakes/today` y la del cuidador), 50 repeticiones cada una, con promedio, mínimo, máximo y p95, y el entorno. Script: `api/tests/measure_today.php`. Medido en la misma PC (localhost), no por Wi-Fi desde un teléfono. |

## Capturas (`docs/capturas/`)
| Archivo | Pantalla |
|---|---|
| `01_registro_paciente.png` | Registro de una cuenta de paciente |
| `02_inicio_sesion.png` | Inicio de sesión (cuenta de cuidador) |
| `03_tomas_hoy.png` | "Hoy" del paciente |
| `04_alta_medicamento.png` | Alta de medicamento con tres horas |
| `05_ajustes_anticipacion.png` | Ajustes: anticipación 5 o 10 min y permisos en orden |
| `06_permisos.png` | Tarjeta de permisos faltantes (notificaciones y batería) |
| `07_generar_codigo.png` | Código de invitación de 10 min |
| `08_introducir_codigo.png` | El cuidador escribe el código |
| `08b_esperando_aceptacion.png` | Lista del cuidador con "Esperando aceptación" |
| `08c_codigo_inexistente.png` | Mensaje de código inexistente |
| `08d_codigo_vencido.png` | Mensaje de código vencido |
| `08e_demasiados_intentos.png` | Mensaje de bloqueo tras 5 intentos (429) |
| `09_solicitud_pendiente.png` | El paciente ve la solicitud con Aceptar y Rechazar |
| `10_vinculo_activo.png` | Vínculo activo |
| `11_aviso.png` | Notificación del aviso previo con "Posponer 5 min" y "Posponer 10 min" |
| `12_confirmada_por_el_paciente.png` | Tomas "Confirmada por el paciente a las HH:MM" y una "Sin confirmar" |
| `13_cuidador_pendiente.png` | Detalle del cuidador con una toma "Pendiente" |
| `14_cuidador_confirmada.png` | Detalle del cuidador con tomas "Confirmada por el paciente" y la dosis oculta |
| `15_cuidador_sin_confirmar.png` | Detalle del cuidador con una toma "Sin confirmar" |
| `15b_letra_grande_cuidador.png` | Detalle del cuidador con la letra del sistema al 130 % |
| `15c_alarma_cuidador.png` | Notificación de la alarma del cuidador a T + 30 |
| `16_ultima_sincronizacion.png` | Lista del cuidador con la última sincronización |
| `17_modo_sin_red.png` | "Hoy" en modo avión con confirmaciones por enviar (2 en la versión de la Fase 8) |
| `18_salir_con_pendientes.png` | "Salir" bloqueado con confirmaciones sin enviar (Fase 8) |
| `19_historial_paciente.png` | Pestaña "Historial" del paciente (Fase 8) |
| `19b_historial_cuidador.png` | Historial de 7 días visto por el cuidador: "Ayer" con una toma "Sin confirmar" y sin dosis (Fase 8) |

Nota: 06, 07, 09, 10, 12 y 17 se volvieron a tomar en la Fase 8 con la pestaña "Medicamentos" ya corregida (c9ea6df). Todas las capturas actuales tienen la corrección. 12 muestra "Sin confirmar" y "Confirmada por el paciente"; "Pendiente" se ve en 13.

## Teléfono físico (`fisico/`), Fase 8 Parte B
POCO M6 Pro (Xiaomi, 2312FPCA6G), Android 16 (SDK 36), HyperOS OS3.0.307.0. Condiciones: batería "Sin restricciones", Inicio automático activado, notificaciones y alarmas exactas concedidas. Resumen de resultados: `fisico/RESUMEN.md`.

| Archivo | Qué es |
|---|---|
| `fisico/00_instalacion_y_compatibilidad.md` | Pasos 0 y 1: instalación, hash del APK, cuentas, permisos y configuración de HyperOS |
| `fisico/seg05_acceso.csv` | Paso 10 (SEG-05): 22 rutas sensibles responden 403 por localhost y por la IP local |
| `fisico/puntualidad.csv` | Paso 2 (RNF-01): escenarios A (pantalla encendida) y B (pantalla apagada), con hora esperada, real y diferencia |
| `fisico/puntualidad_A_logcat_app.log` | Log de la app durante el escenario A |
| `fisico/puntualidad_A_notificaciones.txt` | Hora de publicación (`when`) de cada notificación del escenario A |
| `fisico/puntualidad_B_log_sistema.log` | Líneas del sistema (AlarmManager y NotificationManagerService) del escenario B |
| `fisico/reinicio.csv` | Paso 4 (RNF-03): reprogramación tras `adb reboot` y puntualidad de 2 tomas |
| `fisico/reinicio_observacion.txt` | Estado de alarma, proceso y notificación cada 20 s después del reinicio |
| `fisico/reinicio_log_sistema.log` | Arranque de la app por BootReceiver y entrega de alarmas |
| `fisico/paso05_pr08_sin_red.md` | Paso 5 (PR-08): modo avión, una confirmación, un solo registro |
| `fisico/paso06_pr03_pr04.md` | Paso 6: avisos a T-5 y a T, confirmación con un toque |
| `fisico/paso07_pr05_vinculo.md` | Paso 7 (PR-05): vínculo entre teléfono (paciente) y emulador (cuidador) |
| `fisico/paso08_salir_con_pendientes.md` | Paso 8: bloqueo de "Salir" con pendientes; limitación del token vencido |
| `fisico/paso09_historial.md` | Paso 9 (RF-15): historial de paciente y de cuidador sin dosis |
| `fisico/paso11_pr12_alarma_cuidador.md` | Paso 11 (PR-12): alarma del cuidador a T + 30 en el teléfono |
| `fisico/pr12_observacion.txt`, `fisico/pr12_log_sistema.log` | Notificación y log del sistema de PR-12 |
| `fisico/RESUMEN.md` | Tabla de resultados, lo no medido y las limitaciones |

## Emulador, versión 1.1 (`emulador_v1.1/`), Fase 9
Emulador `Medium_Phone_API_37.0` (API 37), APK 1.1.0. Los tiempos del emulador no son representativos del teléfono. Resumen: `emulador_v1.1/RESUMEN.md`.

| Archivo | Qué es |
|---|---|
| `emulador_v1.1/RESUMEN.md` | Resultados del retest (PR-03, PR-04, PR-11, toma confirmada, límite de 2 min, puntualidad, reinicio, regresión) |
| `emulador_v1.1/canales.txt` | Configuración de los canales v1.1 según el sistema |
| `emulador_v1.1/alarmas_programadas.txt` | `dumpsys alarm`: una alarma de reloj por aviso |
| `emulador_v1.1/funcionales_notificaciones.csv` | Canal, banderas y hora de publicación de cada notificación de las pruebas funcionales |
| `emulador_v1.1/puntualidad_v1.1.csv` | 10 tomas con pantalla encendida y 10 con pantalla apagada (aviso previo y alarma) |
| `emulador_v1.1/reinicio_v1.1.csv` | Reprogramación tras reiniciar y puntualidad de 2 tomas |

## Teléfono físico, versión 1.1 (`fisico/`, Fase 10)
POCO M6 Pro (Android 16, HyperOS), APK 1.1.0 y 1.1.1. Resumen y comparación con la v1.0: `fisico/RESUMEN.md` (segunda parte).

| Archivo | Contenido |
|---|---|
| `fisico/00_instalacion_v1.1.md` | Instalación, SHA-256, permisos de alarma y ajustes de HyperOS, limpieza de datos |
| `fisico/01_puntualidad_v1.1.md` | Puntualidad con pantalla apagada y encendida, hora de inicio de la alarma |
| `fisico/puntualidad_v1.1.csv` | 40 avisos con hora esperada, real y diferencia (SystemUI) |
| `fisico/puntualidad_B_v1.1_wakelock.csv` | Pantalla apagada medida con la publicación (mismo método que la v1.0) |
| `fisico/puntualidad_v1.1_log_sistema.log` | Extracto del log del sistema de la puntualidad |
| `fisico/limite_2min_v1.1.0.csv`, `fisico/limite_2min_v1.1.1.csv` | Corte de 2 minutos antes y después de la 1.1.1 |
| `fisico/02_reinicio_v1.1.md`, `fisico/reinicio_v1.1.csv`, `fisico/reinicio_v1.1_log.txt` | Reinicio con 2 tomas |
| `fisico/03_alarma_pr03_pr04_pr11_v1.1.md`, `fisico/alarma_v1.1_log.txt` | PR-03, PR-04, PR-11, límite de 2 minutos y salir con pendientes |
| `api/tests/puntualidad_v11_sysui.php`, `api/tests/puntualidad_v11_csv.php` | Scripts que calculan la puntualidad a partir del log |
| `fisico/PENDIENTE_v1.1_fisico.md` | PR-12 preparado (pasos y comandos) y comprobaciones que solo puede hacer una persona |

### Capturas de la v1.1
Lista completa con su uso en los manuales: `docs/capturas/LISTA.md`.
- Emulador (Fase 9): `20_alarma_pantalla.png`, `20b_alarma_notificacion.png`, `20c_alarma_sin_sonido.png`, `21_cuidador_notificacion.png`, `22_salir_bloqueado_v11.png`.
- Teléfono físico (Fase 10): `fisico/14_medicamentos_v11.png` a `fisico/26_cuidador_detalle_estados_v11.png` (13 capturas).
- Ajustes de HyperOS (Fase 10): `hyperos_*.png` (6 capturas).

Todas con datos sintéticos; sin correos personales, sin la IP de la PC y sin notificaciones de otras apps (en las de la Fase 10 se taparon los íconos de otras apps de la barra de estado).
