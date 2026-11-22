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
| 15. Cierre de sesión con pendientes | `18_salir_con_pendientes.png` | "Confirmaciones sin enviar": Reintentar envío o Quedarme (también `22_salir_bloqueado_v11.png`, v1.1) |
| 16. Alarma (v1.1) | `20_alarma_pantalla.png` | Pantalla de alarma: hora, medicamento, Tomado, Posponer 5 min y Posponer 10 min |
| 17. Alarma en notificaciones (v1.1) | `20b_alarma_notificacion.png` | Notificación de alarma con Tomado y Posponer |
| 18. Alarma tras 2 minutos (v1.1) | `20c_alarma_sin_sonido.png` | Notificación fija sin sonido cuando nadie atendió la alarma |

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
| 10. Alarma a T + 30 | `15c_alarma_cuidador.png` | Notificación "Revisa a..." de la v1.0 |
| 10b. Aviso a T + 30 (v1.1) | `21_cuidador_notificacion.png` | Notificación "Toma sin confirmar" del cuidador (canal con vibración, sin repetir) |
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
| `fisico/14_medicamentos_v11.png` | Pestaña Medicamentos con horario, Editar y Desactivar (v1.1) | Paciente |
| `fisico/15_alta_medicamento_v11.png` | Nuevo medicamento (sin guardar) | Paciente |
| `fisico/16_ajustes_v11.png` | Ajustes: anticipación 5 o 10 min y permisos en orden | Paciente |
| `fisico/17_aviso_previo_v11.png` | Aviso a T-5 en el panel (recortada a la notificación) | Paciente |
| `fisico/18_alarma_notificacion_v11.png` | Alarma a T en el panel con Tomado y Posponer (recortada) | Paciente |
| `fisico/19_alarma_pantalla_v11.png` | Pantalla de alarma (abierta desde la notificación, sin bloqueo) | Paciente |
| `fisico/20_alarma_sin_sonido_v11.png` | Notificación fija "Sigue pendiente" tras 2 min (recortada) | Paciente |
| `fisico/21_hoy_v11.png` | Hoy con tomas confirmadas | Paciente |
| `fisico/22_salir_con_pendientes_v11.png` | "Confirmaciones sin enviar" con Quedarme y Reintentar envío (modo avión) | Paciente |
| `fisico/23_inicio_sesion_v11.png` | Inicio de sesión (recortada: sin la línea del servidor) | Ambos |
| `fisico/24_servidor_url_generica_v11.png` | Pantalla Servidor con la URL genérica `http://IP-DE-TU-PC/CuidaDosis/api/public/` (no se guardó) | Ambos |
| `fisico/25_cuidador_mis_pacientes_v11.png` | Mis pacientes con resumen, última sincronización y Cerrar sesión | Cuidador |
| `fisico/26_cuidador_detalle_estados_v11.png` | Detalle con Sin confirmar, Confirmada por el paciente y Pendiente, dosis oculta | Cuidador |

## Ajustes de HyperOS (teléfono físico, para el manual de instalación)
| Archivo | Pantalla |
|---|---|
| `hyperos_bateria_sin_restricciones.png` | Ahorro de batería de la app: "Sin restricciones" |
| `hyperos_inicio_automatico.png` | Inicio automático activado (búsqueda filtrada a CuidaDosis) |
| `hyperos_notificaciones_categorias.png` | Notificaciones de la app: flotantes, pantalla de bloqueo, sonido, vibración |
| `hyperos_notificaciones_categorias_2.png` | Las 4 categorías de la v1.1 (aviso previo, alarma, toma pendiente sin sonido, tomas sin confirmar) |
| `hyperos_pantalla_completa.png` | "Permitir notificaciones a pantalla completa" activado |
| `hyperos_otros_permisos.png` | Otros permisos: Mostrar en pantalla de bloqueo y Abrir ventanas en segundo plano |

No se conservan capturas que muestren la IP privada de la PC (pantalla Servidor) ni el diálogo del administrador de contraseñas del teléfono. En las capturas `*_v11.png` y `hyperos_*.png` se taparon los íconos de otras apps de la barra de estado (cierre del 22/11/2026); ninguna muestra el contenido de notificaciones ajenas.

## Notas
- La toma de las 08:00 del "Medicamento SINTETICO Demo B" que aparece en 06, 12, 17 y 19 se agregó por error durante las capturas de la Fase 8. El horario ya se quitó (el medicamento queda con 12:40 y 14:00), pero la toma de ese día se conserva porque estaba confirmada: así funciona el historial (las tomas confirmadas no se borran). No cambia lo que muestra cada captura.
- 06, 07, 09, 10, 12 y 17 se tomaron de nuevo en la Fase 8 con la pestaña "Medicamentos" ya corregida.
