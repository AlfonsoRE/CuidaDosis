# Lista de capturas

Todas con datos sintéticos (nombres con "SINTETICO", correos `@ejemplo.test`). Las de esta carpeta son del emulador `Medium_Phone_API_37.0` (Android 17); las de `fisico/` son del teléfono POCO M6 Pro (Android 16, HyperOS). Ninguna muestra la IP privada de la PC ni notificaciones de otras apps; la única URL visible es la del emulador (`10.0.2.2`, en 02).

En la guía de uso se citan como `[CAPTURA: nombre]`.

## Paciente
| Paso | Archivo | Pantalla |
|---|---|---|
| 1. Registro | `01_registro_paciente.png` | Crear cuenta con el rol Paciente |
| 2. Inicio de sesión | `02_inicio_sesion.png` | Inicio de sesión (la misma pantalla para ambos roles) |
| 3. Alta de medicamento | `04_alta_medicamento.png` | Nuevo medicamento con nombre, dosis y tres horas |
| 4. Horarios | `04b_lista_medicamentos.png` | Lista de medicamentos con "Horario: ...", Editar y Desactivar |
| 5. Ajustes | `05_ajustes_anticipacion.png` | Anticipación del aviso (5 o 10 min) y permisos en orden |
| 6. Permisos faltantes | `06_permisos.png` | Tarjeta roja: notificaciones desactivadas y ahorro de batería |
| 7. Aviso | `11_aviso.png` | Notificación "Toma de las HH:MM" con "Posponer 5 min" y "Posponer 10 min" |
| 8. Tomas de hoy | `03_tomas_hoy.png` | "Hoy" con tomas confirmadas |
| 9. Confirmar toma | `12_confirmada_por_el_paciente.png` | "Confirmada por el paciente a las HH:MM" y una "Sin confirmar" |
| 10. Sin red | `17_modo_sin_red.png` | Modo avión: "Sin conexión con el servidor" y confirmaciones por enviar |
| 11. Vincular: generar código | `07_generar_codigo.png` | Código de 8 caracteres con vencimiento a 10 min |
| 12. Aceptar al cuidador | `09_solicitud_pendiente.png` | Solicitud de vínculo con Aceptar y Rechazar |
| 13. Vínculo activo | `10_vinculo_activo.png` | "Tu cuidador", con Desvincular cuidador |
| 14. Historial | `19_historial_paciente.png` | Pestaña Historial (últimos 7 días) |
| 15. Cierre de sesión con pendientes | `18_salir_con_pendientes.png` | "Confirmaciones sin enviar": Reintentar envío o Quedarme |

## Cuidador
| Paso | Archivo | Pantalla |
|---|---|---|
| 1. Registro | `01b_registro_cuidador.png` | Crear cuenta con el rol Cuidador |
| 2. Inicio de sesión | `02_inicio_sesion.png` | Inicio de sesión con la cuenta del cuidador |
| 3. Vincular: introducir código | `08_introducir_codigo.png` | Diálogo "Vincular paciente" |
| 4. Esperando aceptación | `08b_esperando_aceptacion.png` | Lista con la sección "Esperando aceptación" y Retirar solicitud |
| 5. Errores del código | `08c_codigo_inexistente.png`, `08d_codigo_vencido.png`, `08e_demasiados_intentos.png` | Código inexistente, vencido y bloqueo tras 5 intentos |
| 6. Estado: lista de pacientes | `16_ultima_sincronizacion.png` | "Mis pacientes" con resumen y última sincronización; botón Cerrar sesión |
| 7. Estado: pendiente | `13_cuidador_pendiente.png` | Detalle con una toma "Pendiente" |
| 8. Estado: confirmada | `14_cuidador_confirmada.png` | Detalle con "Confirmada por el paciente" y la dosis oculta |
| 9. Estado: sin confirmar | `15_cuidador_sin_confirmar.png` | Detalle con una toma "Sin confirmar" |
| 10. Alarma a T + 30 | `15c_alarma_cuidador.png` | Notificación "Toma sin confirmar" con la última sincronización |
| 11. Historial | `19b_historial_cuidador.png` | Historial de 7 días del paciente, sin dosis |
| 12. Letra grande | `15b_letra_grande_cuidador.png` | Detalle con la letra del sistema al 130 % |
| 13. Cierre de sesión | `16_ultima_sincronizacion.png` | El botón "Cerrar sesión" está arriba a la derecha de esa pantalla |

## Teléfono físico (`fisico/`)
| Archivo | Pantalla | Rol |
|---|---|---|
| `fisico/01_registro_paciente.png` | Registro de la cuenta de paciente | Paciente |
| `fisico/06_confirmada_un_toque.png` | "Confirmada por el paciente" tras un toque | Paciente |
| `fisico/07_sin_red_confirmada.png` | Modo avión: confirmación guardada para enviar | Paciente |
| `fisico/08_salir_bloqueado.png` | "Salir" bloqueado con confirmaciones sin enviar | Paciente |
| `fisico/09_generar_codigo.png` | Código de invitación de 10 min | Paciente |
| `fisico/09b_emulador_esperando_aceptacion.png` | (Emulador) "Esperando aceptación" | Cuidador |
| `fisico/10_solicitud_por_aceptar.png` | Solicitud de vínculo con Aceptar y Rechazar | Paciente |
| `fisico/11_vinculo_activo.png` | Vínculo activo | Paciente |
| `fisico/12_historial_paciente.png` | Historial de 7 días con dosis | Paciente |
| `fisico/12b_emulador_historial_cuidador_sin_dosis.png` | (Emulador) Historial del paciente sin dosis | Cuidador |
| `fisico/13_desvinculado.png` | Tras desvincular al cuidador | Paciente |

No se conservan capturas que muestren la IP privada de la PC (pantalla Servidor) ni el diálogo del administrador de contraseñas del teléfono.

## Notas
- La toma de las 08:00 del "Medicamento SINTETICO Demo B" que aparece en 06, 12, 17 y 19 se agregó por error durante las capturas de la Fase 8. El horario ya se quitó (el medicamento queda con 12:40 y 14:00), pero la toma de ese día se conserva porque estaba confirmada: así funciona el historial (las tomas confirmadas no se borran). No cambia lo que muestra cada captura.
- 06, 07, 09, 10, 12 y 17 se tomaron de nuevo en la Fase 8 con la pestaña "Medicamentos" ya corregida.
