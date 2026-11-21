# Índice de evidencias P4 (Fase 6 y Fase 7)

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
