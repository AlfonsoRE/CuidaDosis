# Lista de capturas

Todas con datos sintéticos (nombres con "SINTETICO", correos `@ejemplo.test`). Las de esta carpeta son del emulador `Medium_Phone_API_37.0` (Android 17); las de `fisico/` son del teléfono POCO M6 Pro (Android 16, HyperOS). Ninguna muestra la IP privada de la PC ni notificaciones de otras apps; la única URL visible es la del emulador (`10.0.2.2`, en 02).

En la guía de uso se citan como `[CAPTURA: nombre]`.

## Vigencia con la versión 1.1.3 (revisión del 23/11/2026)
De la 1.1.1 a la 1.1.3 solo cambiaron el login de la release (servidor fijo, sin enlace) y los íconos (D49, D50). Las pantallas internas son iguales.

- **Vigentes (pantallas internas sin cambio):** todas las de Paciente y Cuidador de este archivo salvo las marcadas abajo; `20_alarma_pantalla.png` (la pantalla de alarma no muestra el ícono); en `fisico/`, de la 01 a la 16, 19, 21, 22, 25 y 26; `hyperos_notificaciones_categorias.png`, `hyperos_notificaciones_categorias_2.png` y `hyperos_otros_permisos.png` (no muestran el ícono).
- **Vigente con nota:** `fisico/23_inicio_sesion_v11.png` está recortada sin la línea del servidor; sirve para el login, pero no muestra el servidor fijo de la release.
- **Solo para el manual del equipo (debug):** `fisico/24_servidor_url_generica_v11.png`. La pantalla Servidor no existe en la release del grupo beta.
- **Reemplazadas por una versión 1.1.3 (ya tomada):** `hyperos_bateria_sin_restricciones.png` y `hyperos_pantalla_completa.png` (mostraban el ícono genérico y la 1.1.0).
- **Hay que volver a tomarlas con la 1.1.3:** `02_inicio_sesion.png` (emulador, enlace "Servidor" de debug con `10.0.2.2`), `hyperos_inicio_automatico.png` (ícono genérico) y todas las notificaciones, porque muestran la campana genérica como ícono pequeño: `11_aviso.png`, `15c_alarma_cuidador.png`, `20b_alarma_notificacion.png`, `20c_alarma_sin_sonido.png`, `21_cuidador_notificacion.png`, `fisico/17_aviso_previo_v11.png`, `fisico/18_alarma_notificacion_v11.png` y `fisico/20_alarma_sin_sonido_v11.png`.

### Capturas 1.1.3 hechas (teléfono físico, 23/11/2026)
| Archivo | Pantalla | Para |
|---|---|---|
| `27_icono_cajon_v113.png` | Búsqueda del cajón de apps de HyperOS con el ícono nuevo (recortada a la app) | Instalación y usuario |
| `28_info_app_version_v113.png` | Info. de la aplicación: ícono y "Versión: 1.1.3" (sin barra de estado) | Instalación |
| `hyperos_bateria_sin_restricciones_v113.png` | Detalles de la batería de CuidaDosis con "Sin restricciones" (reemplaza a la anterior) | Instalación |
| `hyperos_pantalla_completa_v113.png` | Notificaciones de pantalla completa permitidas, CuidaDosis 1.1.3 (reemplaza a la anterior) | Instalación |

Se tomaron solo mirando las pantallas: no se cambió ningún ajuste ni permiso, ni la sesión de la app. Se descartaron sin guardar una captura con una notificación de otra app encima y la de la app abierta (tenía una sesión del equipo que no es sintética).

### Capturas 1.1.3 pendientes
| Archivo sugerido | Pantalla | Por qué sigue pendiente |
|---|---|---|
| `29_instalar_descarga_v113.png` | Navegador del teléfono descargando `CuidaDosis-1.1.3-beta.apk` (aviso de archivo que puede ser dañino) | Requiere instalar desde el navegador; no se reinstaló la app para no perder la sesión ni la batería "Sin restricciones" |
| `30_instalar_origen_desconocido_v113.png` | "Instalar apps desconocidas" activado para el navegador | Igual que la anterior |
| `31_instalar_play_protect_v113.png` | Aviso de Google Play Protect al instalar | Igual que la anterior |
| `32_instalar_confirmar_v113.png` | Pantalla final "Instalar" (y el aviso de seguridad de Xiaomi, si aparece) | Igual que la anterior |
| `33_inicio_sesion_release_v113.png` | Login de la release con "Servidor: https://areproyectos.com.mx/cuidadosis/api/" como texto fijo | El teléfono tenía abierta una sesión del equipo (cuidador); cerrarla la habría perdido junto con sus alarmas |
| `34_aviso_previo_v113.png` | Aviso a T − L con el ícono de la cápsula (recortada a la notificación) | Requiere una cuenta sintética con sesión en el teléfono (mismo motivo que la 33) |
| `35_alarma_notificacion_v113.png` | Alarma a T con Tomado y Posponer (recortada) | Mismo motivo que la 33 |
| `36_alarma_sin_sonido_v113.png` | Notificación fija "Sigue pendiente" tras 2 min | Mismo motivo que la 33 |
| `37_cuidador_notificacion_v113.png` | "Toma sin confirmar" del cuidador a T + 30 | Mismo motivo que la 33 |
| `38_barra_estado_icono_v113.png` | Ícono pequeño de la cápsula en la barra de estado | Mismo motivo que la 33 |
| `hyperos_inicio_automatico_v113.png` | Inicio automático activado para CuidaDosis | En el teléfono el Inicio automático de CuidaDosis está **desactivado** (las reinstalaciones lo restablecieron); no se cambió por instrucción. Tomarla después de activarlo |
| `39_alarma_sobre_bloqueo_v113.png` | Pantalla de alarma sobre el bloqueo con la pantalla apagada | En la Fase 10 HyperOS solo encendió la luz de bordes (D45); requiere una persona con el teléfono |

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
| `hyperos_bateria_sin_restricciones.png` | Ahorro de batería de la app: "Sin restricciones" (1.1.0, ícono genérico; usar la `_v113`) |
| `hyperos_bateria_sin_restricciones_v113.png` | Lo mismo con la 1.1.3 y el ícono nuevo |
| `hyperos_inicio_automatico.png` | Inicio automático activado (búsqueda filtrada a CuidaDosis; ícono genérico, pendiente de retomar) |
| `hyperos_notificaciones_categorias.png` | Notificaciones de la app: flotantes, pantalla de bloqueo, sonido, vibración |
| `hyperos_notificaciones_categorias_2.png` | Las 4 categorías de la v1.1 (aviso previo, alarma, toma pendiente sin sonido, tomas sin confirmar) |
| `hyperos_pantalla_completa.png` | "Permitir notificaciones a pantalla completa" activado (1.1.0; usar la `_v113`) |
| `hyperos_pantalla_completa_v113.png` | Lo mismo con la 1.1.3 y el ícono nuevo |
| `hyperos_otros_permisos.png` | Otros permisos: Mostrar en pantalla de bloqueo y Abrir ventanas en segundo plano |

No se conservan capturas que muestren la IP privada de la PC (pantalla Servidor) ni el diálogo del administrador de contraseñas del teléfono. En las capturas `*_v11.png` y `hyperos_*.png` se taparon los íconos de otras apps de la barra de estado (cierre del 22/11/2026); ninguna muestra el contenido de notificaciones ajenas. Las `*_v113.png` se recortaron sin la barra de estado.

## Notas
- La toma de las 08:00 del "Medicamento SINTETICO Demo B" que aparece en 06, 12, 17 y 19 se agregó por error durante las capturas de la Fase 8. El horario ya se quitó (el medicamento queda con 12:40 y 14:00), pero la toma de ese día se conserva porque estaba confirmada: así funciona el historial (las tomas confirmadas no se borran). No cambia lo que muestra cada captura.
- 06, 07, 09, 10, 12 y 17 se tomaron de nuevo en la Fase 8 con la pestaña "Medicamentos" ya corregida.
