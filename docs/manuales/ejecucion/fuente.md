# Manual de ejecución y uso de CuidaDosis 1.1.3 (fuente verificada)

Fuente para redactar el manual de usuario. Cada paso indica la acción, el resultado esperado, la captura (en `capturas/` de esta carpeta) y qué hacer si falla. Las capturas usan solo datos sintéticos ("SINTETICO", correos `@ejemplo.test`). Corresponde a la app 1.1.3 con el servidor `https://areproyectos.com.mx/cuidadosis/api/`. Lo que no se pudo comprobar dice **no verificado**.

> CuidaDosis no recomienda dosis ni sustituye al médico. El nombre y la dosis se escriben tal como los indicó el médico; la app no los interpreta. "Confirmada por el paciente" solo significa que se tocó "Tomado"; no comprueba que el medicamento se haya tomado.

Las pantallas internas no cambiaron de la 1.1.1 a la 1.1.3; algunas capturas son del emulador o de la 1.1.1 y siguen vigentes (ver `docs/capturas/LISTA.md`).

## 1. Crear cuenta e iniciar sesión (paciente y cuidador)
1. **Abrir la app.**
   - Resultado esperado: pantalla de inicio de sesión con la línea fija "Servidor: https://areproyectos.com.mx/cuidadosis/api/".
   - Captura: `02_inicio_sesion.png` (release 1.1.3).
2. **Registrarse.**
   - Acción: tocar **¿No tienes cuenta? Regístrate**, elegir **Paciente** o **Cuidador**, escribir nombre, correo y contraseña (mínimo 8 caracteres) y tocar **Crear cuenta**.
   - Resultado esperado: entra directo a "Hoy" (paciente) o "Mis pacientes" (cuidador).
   - Capturas: `01_registro_paciente.png`, `01b_registro_cuidador.png`.
   - Si falla: "Ya existe una cuenta con ese correo." si el correo ya se usó; sin conexión aparece un error de conexión.
3. **Iniciar sesión.**
   - Acción: escribir correo y contraseña y tocar **Iniciar sesión**.
   - Resultado esperado: la primera vez Android pregunta "¿Quieres permitir que CuidaDosis te envíe notificaciones?": tocar **Permitir**.
   - Captura: `33b_permiso_notificaciones_v113.png`.
   - Si falla: si se niega el permiso, aparece la tarjeta roja "Los avisos pueden no sonar a tiempo" con **Activar notificaciones** (`06_permisos.png`).

## 2. Vincular al paciente con su cuidador
1. **El paciente genera un código.**
   - Acción: pestaña **Cuidador**, **Generar código de invitación**.
   - Resultado esperado: código de 8 caracteres, válido 10 minutos y de un solo uso.
   - Captura: `07_generar_codigo.png`.
2. **El cuidador escribe el código.**
   - Acción: en "Mis pacientes", **+ Vincular paciente**, escribir el código y confirmar.
   - Resultado esperado: el paciente aparece en "Esperando aceptación", con **Retirar solicitud**.
   - Capturas: `08_introducir_codigo.png`, `08b_esperando_aceptacion.png`.
   - Si falla: código inexistente o ya usado (`08c_codigo_inexistente.png`), vencido (`08d_codigo_vencido.png`) o demasiados intentos, 5 en 10 minutos (`08e_demasiados_intentos.png`).
3. **El paciente acepta.**
   - Acción: en la pestaña **Cuidador**, ver la solicitud y tocar **Aceptar** (o **Rechazar** si no conoce a la persona).
   - Resultado esperado: "Tu cuidador" con su nombre y **Desvincular cuidador**.
   - Capturas: `09_solicitud_pendiente.png`, `10_vinculo_activo.png`.
   - Un paciente solo puede tener un cuidador activo; un cuidador puede tener varios pacientes.

## 3. Agregar medicamentos y horarios (paciente)
1. **Alta.**
   - Acción: pestaña **Medicamentos**, **+ Agregar**, escribir nombre y dosis, **Agregar hora** una o varias veces y **Guardar medicamento**.
   - Resultado esperado: el medicamento aparece con "Horario: ..." y los botones **Editar** y **Desactivar**.
   - Capturas: `04_alta_medicamento.png`, `04b_lista_medicamentos.png`.
   - Si falla: esta pestaña necesita conexión.
2. **Editar o desactivar.**
   - Acción: **Editar** para cambiar las horas (tocar la ✕ de una hora para quitarla) o **Desactivar**. Las tomas pasadas se conservan.

## 4. Avisos y alarma (paciente)
Horario de ejemplo: toma a las T con anticipación L = 5 minutos (se cambia en **Ajustes**: 5 o 10, `05_ajustes_anticipacion.png`).

1. **Aviso previo a T − L.**
   - Resultado esperado: notificación "Toma de las HH:MM", "En 5 minutos: medicamento: dosis", con sonido de notificación y vibración breve, una sola vez, y botones **Posponer 5 min** y **Posponer 10 min**. El ícono pequeño es la silueta de la cápsula.
   - Capturas: `34_aviso_previo_v113.png` (expandida, con los botones) y `34b_aviso_previo_compacto_v113.png` (como aparece en el panel). Verificado el 23/11/2026: llegó a las 19:12:02 para la toma de las 19:17.
2. **Alarma a la hora T.**
   - Resultado esperado: si la toma sigue pendiente, suena la alarma (volumen de alarma) con vibración repetida y aparece "Es la hora de tu toma de las HH:MM", "Toca Tomado o Posponer", con **Tomado**, **Posponer 5 min** y **Posponer 10 min**. Con el teléfono desbloqueado aparece como notificación flotante arriba de la pantalla; al tocarla se abre la pantalla de alarma con la hora, el medicamento, la dosis y los tres botones.
   - Capturas: `35b_alarma_flotante_v113.png` (notificación flotante), `35_alarma_notificacion_v113.png` (en el panel), `35c_alarma_pantalla_v113.png` (pantalla de alarma; equivalente a `20_alarma_pantalla.png` del emulador) y `38_barra_estado_icono_v113.png` (ícono de la cápsula junto a la hora). Verificado: sonó a las 19:17:01.
3. **Alarma con el teléfono bloqueado.**
   - Resultado esperado: con el teléfono bloqueado y la pantalla encendida, la pantalla de alarma se muestra sobre el bloqueo.
   - Captura: `39_alarma_sobre_bloqueo_v113.png`. Verificado: con el teléfono bloqueado y la pantalla encendida, la pantalla de alarma se abrió sola sobre el bloqueo (19:24:31, alarma de un pospuesto).
   - Con la pantalla apagada, en HyperOS solo se enciende la luz de bordes: la alarma suena y queda la notificación, pero la pantalla no se enciende ni se abre la pantalla de alarma. Verificado de nuevo con la 1.1.3 (alarma de las 19:50: el teléfono siguió en reposo 75 s y la pantalla de bloqueo siguió al frente). Evidencia: `39b_alarma_pantalla_apagada_luz_bordes_v113.png`. Se recomienda encender la pantalla al oír la alarma.
4. **Si nadie responde: corte a los 2 minutos.**
   - Resultado esperado: la alarma deja de sonar y queda una notificación fija sin sonido, "Sigue pendiente", con los mismos botones.
   - Capturas: `36_alarma_sin_sonido_v113.png` y `36b_alarma_sin_sonido_compacta_v113.png`. Verificado: el corte llegó a las 19:19:02, 2 minutos después de la alarma.
5. **Posponer.**
   - Acción: tocar **Posponer 5 min** o **Posponer 10 min**.
   - Resultado esperado: la alarma se detiene, la toma sigue pendiente y al terminar el plazo vuelve a sonar. Verificado: Posponer 5 min a las 19:19:30 programó la siguiente alarma para las 19:24:30 y sonó a esa hora.
6. **Si ya se confirmó la toma, la alarma no suena.**

## 5. Confirmar la toma (paciente)
1. **Confirmar.**
   - Acción: tocar **Tomado** en la alarma, en la notificación o en la pestaña **Hoy**. Verificado el 23/11/2026 desde la notificación (con la alarma sonando, la alarma se detuvo y la toma quedó confirmada).
   - Resultado esperado: un solo toque, sin diálogo. La toma queda como "Confirmada por el paciente a las HH:MM" y la alarma se detiene.
   - Capturas: `03_tomas_hoy.png`, `12_confirmada_por_el_paciente.png`.
2. **Sin internet.**
   - Resultado esperado: la confirmación se guarda en el teléfono ("Guardada en el teléfono; se enviará al haber conexión") y se envía sola al volver la red, sin duplicarse. Los avisos siguen funcionando sin internet.
   - Captura: `17_modo_sin_red.png`.
3. **Estados de una toma:** "Pendiente" (en plazo), "Confirmada por el paciente" (se tocó Tomado) y "Sin confirmar" (pasaron 30 minutos sin confirmar; se calcula, no se guarda).

## 6. Historial
- Paciente: pestaña **Historial**, últimos 7 días por día, con resumen y dosis (`19_historial_paciente.png`).
- Cuidador: en el detalle del paciente, **Ver historial de 7 días**, sin la dosis (`19b_historial_cuidador.png`).
- Necesita conexión.

## 7. Vista del cuidador
1. **Mis pacientes.**
   - Resultado esperado: cada paciente con cuántas tomas lleva registradas hoy, las "Sin confirmar" y la última sincronización de su teléfono.
   - Captura: `16_ultima_sincronizacion.png`.
   - Si el teléfono del paciente no tiene conexión, se ve el último estado que envió; revisar esa hora.
2. **Detalle.**
   - Acción: tocar un paciente.
   - Resultado esperado: tomas de hoy con las tres etiquetas, sin dosis salvo que el paciente la comparta (no hay pantalla para compartirla; G-07 pendiente).
   - Capturas: `13_cuidador_pendiente.png`, `14_cuidador_confirmada.png`, `15_cuidador_sin_confirmar.png`, `15b_letra_grande_cuidador.png` (letra al 130 %).
3. **Aviso a T + 30.**
   - Resultado esperado: si una toma sigue sin confirmar 30 minutos después de su hora, el teléfono del cuidador muestra "Toma sin confirmar: nombre del paciente", "medicamento de las HH:MM sigue sin confirmar", con la última sincronización. Sonido y vibración breves, sin repetir. Antes de avisar, la app consulta el estado; si ya se confirmó, no avisa. Sin conexión avisa "Revisa a nombre" para que se revise a mano.
   - Capturas: `37_cuidador_notificacion_v113.png` (expandida) y `37b_cuidador_notificacion_compacta_v113.png`. Verificado: para una toma de las 18:37 sin confirmar, el aviso llegó a las 19:07 (detectado a las 19:07:10, revisando cada 15 s) con el texto "Medicamento SINTETICO P00 de las 18:37 sigue sin confirmar. El teléfono del paciente aún no ha sincronizado."
   - Limitación: solo cubre las tomas del día en que el cuidador abrió la app. No hay notificación desde el servidor (RF-17, fuera de alcance).

## 8. Cerrar sesión
- Paciente: **Salir** (arriba a la derecha). Cuidador: **Cerrar sesión** (arriba a la derecha de "Mis pacientes").
- Resultado esperado: vuelve al inicio de sesión; se borran los datos guardados en el teléfono y se cancelan los avisos.
- Si el paciente tiene confirmaciones sin enviar y no hay conexión, la app no lo deja salir: "Confirmaciones sin enviar" con **Reintentar envío** y **Quedarme** (`18_salir_con_pendientes.png`, `22_salir_bloqueado_v11.png`).

## 9. Desvincular
- Paciente: pestaña **Cuidador**, **Desvincular cuidador**. El cuidador deja de ver sus tomas de inmediato; después se puede invitar a otra persona.
- Cuidador: detalle del paciente, **Dejar de seguir a este paciente**; para volver a verlo necesita un código nuevo.
- Captura de referencia: `fisico/13_desvinculado.png` del teléfono (copiada como `13_desvinculado.png`).

## 10. Problemas frecuentes (observados en el proyecto)
- **No llegan los avisos o llegan tarde:** revisar notificaciones, batería "Sin restricciones" e Inicio automático (manual de configuración, sección 4). Tras reinstalar en HyperOS hay que repetirlos.
- **Tras reiniciar el teléfono no hay avisos por un rato:** Android no avisa a la app hasta el primer desbloqueo; en HyperOS llegó unos 2.5 minutos después. Desbloquear el teléfono después de reiniciar.
- **"Pausar la actividad de la aplicación si no se utiliza":** se recomienda desactivarlo.
- **La alarma no suena:** volumen de alarma en cero o No molestar sin alarmas permitidas (**no verificado** en el teléfono).
- **Error de conexión o página "One moment, please...":** protección anti-bots del hosting; esperar unos minutos.
- **La pestaña Medicamentos o el historial no cargan:** necesitan conexión.
- **Se cambiaron los horarios desde otro teléfono con la misma cuenta:** los avisos se calculan con la copia de medicamentos del teléfono, que se actualiza al iniciar sesión o al abrir la pestaña **Medicamentos**; "Actualizar" en Hoy no la actualiza. Abrir Medicamentos después de un cambio hecho en otro lado. Si la hora ya pasó, la alarma suena en ese momento (observado el 23/11/2026: una toma de las 19:40 cuyo horario se agregó por la API sonó a las 19:42, al abrir Medicamentos).
- **Sesión vencida:** el token dura 30 días; si vence con confirmaciones sin enviar, esas confirmaciones se pierden.
