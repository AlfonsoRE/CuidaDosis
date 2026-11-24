# Lista de capturas

Todas con datos sintéticos (nombres con "SINTETICO", correos `@ejemplo.test`). Las de esta carpeta son del emulador `Medium_Phone_API_37.0` (Android 17); las de `fisico/` son del teléfono POCO M6 Pro (Android 16, HyperOS). Ninguna muestra la IP privada de la PC ni notificaciones de otras apps; la única URL visible es la del emulador (`10.0.2.2`, en 02).

En la guía de uso se citan como `[CAPTURA: nombre]`.

## Estado con la versión 1.1.3 (cierre del 23/11/2026)
De la 1.1.1 a la 1.1.3 solo cambiaron el login de la release (servidor fijo, sin enlace) y los íconos (D49, D50). Las pantallas internas son iguales. Todas las capturas nuevas son del teléfono físico (POCO M6 Pro, HyperOS) con la 1.1.3 release instalada desde el navegador, cuentas "SINTETICO Paciente P00" y "SINTETICO Cuidador P00", y sin la barra de estado (salvo la 38, que es la barra de estado). Cada manual tiene copia de las que usa en `docs/manuales/*/capturas/`. Revisión de privacidad: `REVISAR_PRIVACIDAD.md`.

### Vigentes (se reutilizan sin cambios)
- Pantallas internas del emulador: 01, 01b, 03, 04, 04b, 05, 06, 07, 08, 08b, 08c, 08d, 08e, 09, 10, 12, 13, 14, 15, 15b, 16, 17, 18, 19, 19b, 20 (pantalla de alarma) y 22.
- `fisico/`: de la 01 a la 16, 19, 21, 22, 23 (recortada sin la línea del servidor), 25 y 26. `fisico/24_servidor_url_generica_v11.png` solo sirve para el debug del equipo.
- HyperOS: `hyperos_notificaciones_categorias.png` y `hyperos_notificaciones_categorias_2.png` (no muestran el ícono).

### Reemplazadas
| Obsoleta | Motivo | Reemplazo 1.1.3 |
|---|---|---|
| `02_inicio_sesion.png` | Emulador con el enlace "Servidor" de debug (`10.0.2.2`) | Mismo nombre, ahora login de la release (igual a la 33) |
| `11_aviso.png`, `fisico/17_aviso_previo_v11.png` | Campana genérica como ícono | `34_aviso_previo_v113.png`, `34b_aviso_previo_compacto_v113.png` |
| `20b_alarma_notificacion.png`, `fisico/18_alarma_notificacion_v11.png` | Campana genérica | `35_alarma_notificacion_v113.png`, `35b_alarma_flotante_v113.png` |
| `20c_alarma_sin_sonido.png`, `fisico/20_alarma_sin_sonido_v11.png` | Campana genérica | `36_alarma_sin_sonido_v113.png`, `36b_alarma_sin_sonido_compacta_v113.png` |
| `15c_alarma_cuidador.png` (v1.0, "Revisa a..."), `21_cuidador_notificacion.png` | Campana genérica | `37_cuidador_notificacion_v113.png`, `37b_cuidador_notificacion_compacta_v113.png` ("Toma sin confirmar"; la variante "Revisa a..." sin conexión no se volvió a capturar) |
| `hyperos_bateria_sin_restricciones.png` | Ícono genérico, 1.1.0 | `hyperos_bateria_sin_restricciones_v113.png` |
| `hyperos_inicio_automatico.png` | Ícono genérico | `hyperos_inicio_automatico_v113.png` |
| `hyperos_pantalla_completa.png` | Ícono genérico, 1.1.0 | `hyperos_pantalla_completa_v113.png` |
| `hyperos_otros_permisos.png` | Vigente, pero tomada antes de reinstalar | `hyperos_otros_permisos_v113.png` |

Los archivos obsoletos se conservan en el repositorio como historial; los manuales usan los de la 1.1.3.

### Nuevas (1.1.3)
| Archivo | Pantalla | Manual |
|---|---|---|
| `27_icono_cajon_v113.png` | Búsqueda del cajón de apps con el ícono nuevo | Instalación |
| `28_info_app_version_v113.png` | Info. de la aplicación: "Versión: 1.1.3" | Instalación |
| `29_instalar_descarga_v113.png` | Chrome: "Es posible que el archivo sea dañino", Descargar de todos modos | Instalación |
| `29b_instalar_abrir_con_v113.png` | "Abrir con": Instalador de paquetes | Instalación |
| `30_instalar_origen_desconocido_v113.png` | Instalar apps desconocidas, "Confiar en esta fuente" activado para Chrome (ya estaba concedido: no se vio la solicitud) | Instalación |
| `31_instalar_play_protect_v113.png` | Play Protect: "Se recomienda analizar la app" | Instalación |
| `31b_instalar_sin_analizar_v113.png` | Play Protect con "Más detalles": "Instalar sin analizar" | Instalación |
| `32_instalar_confirmar_v113.png` | "¿Deseas instalar esta app?" | Instalación |
| `32b_instalar_resultado_xiaomi_v113.png` | Análisis de Xiaomi: "Pasó pruebas de seguridad" | Instalación |
| `33_inicio_sesion_release_v113.png` | Login de la release con el servidor fijo | Instalación, configuración |
| `33b_permiso_notificaciones_v113.png` | "¿Quieres permitir que CuidaDosis te envíe notificaciones?" | Todos |
| `34_aviso_previo_v113.png`, `34b_aviso_previo_compacto_v113.png` | Aviso a T − 5 ("Toma de las 19:17", Posponer 5 y 10 min) | Ejecución |
| `35_alarma_notificacion_v113.png`, `35b_alarma_flotante_v113.png` | Alarma a T con Tomado y Posponer (panel y flotante) | Ejecución |
| `35c_alarma_pantalla_v113.png` | Pantalla de alarma en el teléfono | Ejecución |
| `36_alarma_sin_sonido_v113.png`, `36b_alarma_sin_sonido_compacta_v113.png` | "Sigue pendiente" tras el corte de 2 minutos | Ejecución |
| `37_cuidador_notificacion_v113.png`, `37b_cuidador_notificacion_compacta_v113.png` | "Toma sin confirmar" del cuidador a T + 30 | Ejecución |
| `38_barra_estado_icono_v113.png` | Barra de estado con el ícono de la cápsula junto a la hora | Ejecución |
| `39_alarma_sobre_bloqueo_v113.png` | Pantalla de alarma sobre el bloqueo, pantalla encendida | Ejecución |
| `39b_alarma_pantalla_apagada_luz_bordes_v113.png` | Evidencia: con la pantalla apagada solo se enciende la luz de bordes (no se abre la pantalla de alarma) | Ejecución |
| `hyperos_*_v113.png` (4) | Batería, Inicio automático, pantalla completa y otros permisos | Configuración |

### Pendientes
| Pantalla | Motivo |
|---|---|
| Confirmación de identidad de Play Protect ("¿Quieres instalar la app sin analizarla?") | Android protege esa pantalla: la captura sale negra. Se describe con texto en el manual |
| Solicitud de "Instalar apps desconocidas" al instalar por primera vez | Chrome ya tenía el permiso; solo se capturó el ajuste activado (`30`). Se vería en un teléfono que nunca instaló un APK desde Chrome |
| Pantalla de alarma con la pantalla apagada | No existe en HyperOS: solo se enciende la luz de bordes (evidencia `39b`) |
| Aviso del cuidador sin conexión ("Revisa a...") con el ícono nuevo | No se forzó la falta de red durante el aviso; el texto está verificado en el código |
| Notificación con título "Recordatorio: toma de las HH:MM" tras posponer | La alarma del pospuesto se capturó como pantalla sobre el bloqueo (`39`), no en el panel |

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
