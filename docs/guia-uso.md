# Guía de uso de CuidaDosis

CuidaDosis recuerda las tomas de medicamentos al paciente y permite que un cuidador vea si las confirmó. Es una sola app: muestra las pantallas del paciente o del cuidador según la cuenta.

> CuidaDosis no recomienda dosis ni sustituye a tu médico. Nombre y dosis se escriben tal como los indicó el médico; la app no los interpreta. "Confirmada por el paciente" solo significa que se tocó "Tomado"; no comprueba que el medicamento se haya tomado.

Las capturas están en `docs/capturas/` y usan solo datos sintéticos.

## 1. Crear cuenta e iniciar sesión
1. En la pantalla de inicio toca **¿No tienes cuenta? Regístrate**.
2. Elige **Paciente** o **Cuidador**, escribe tu nombre, correo y contraseña (mínimo 8 caracteres) y toca **Crear cuenta**.

[CAPTURA: 01_registro_paciente]

3. Después, entra con tu correo y contraseña en **Iniciar sesión**.

[CAPTURA: 02_inicio_sesion]

La primera vez, Android pide permiso para buscar dispositivos de la red local (necesario para conectar con el servidor) y para enviar notificaciones (necesario para los avisos). Acepta ambos.

## 2. Paciente

### 2.1 Hoy
La pestaña **Hoy** muestra las tomas del día con su estado:
- **Pendiente:** todavía está en plazo.
- **Confirmada por el paciente a las HH:MM:** tocaste "Tomado".
- **Sin confirmar:** pasaron 30 minutos de la hora sin confirmación.

Toca **Tomado** cuando tomes tu medicamento. Es un solo toque, sin preguntas.

[CAPTURA: 03_tomas_hoy]

[CAPTURA: 12_confirmada_por_el_paciente]

### 2.2 Sin internet
Puedes confirmar aunque no haya conexión. La toma se guarda en el teléfono con la nota "Guardada en el teléfono; se enviará al haber conexión", y se envía sola cuando vuelve la red, sin duplicarse. Los avisos siguen funcionando sin internet. Arriba verás la hora de la última sincronización.

[CAPTURA: 17_modo_sin_red]

### 2.3 Medicamentos
En **Medicamentos** toca **+ Agregar**, escribe el nombre y la dosis, agrega una o varias horas con **Agregar hora** y toca **Guardar medicamento**. Puedes editar las horas o desactivar un medicamento; las tomas pasadas se conservan. Esta pestaña necesita conexión.

[CAPTURA: 04_alta_medicamento]

### 2.4 Avisos
- El teléfono avisa **5 o 10 minutos antes** de cada toma, según tu ajuste.
- Si a la hora de la toma sigue pendiente, avisa **una vez más**.
- En la notificación puedes tocar **Posponer 5 min** o **Posponer 10 min**: vuelve a avisar después y la toma sigue pendiente.
- Si ya confirmaste la toma, no suena.

[CAPTURA: 11_aviso]

### 2.5 Historial
La pestaña **Historial** muestra las tomas de los últimos 7 días, agrupadas por día (Hoy, Ayer y las fechas anteriores), con su estado y cuántas quedaron sin confirmar. Necesita conexión.

[CAPTURA: 19_historial_paciente]

### 2.6 Ajustes
Toca **Ajustes** (arriba) para elegir la anticipación del aviso: 5 minutos (por defecto) o 10 minutos. El ajuste solo se guarda en tu teléfono. Abajo aparece el estado de los permisos.

[CAPTURA: 05_ajustes_anticipacion]

Si falta un permiso (notificaciones, alarmas exactas) o el ahorro de batería puede retrasar los avisos, en **Hoy** aparece una tarjeta roja con un botón para corregir cada punto. Desaparece cuando todo está bien.

[CAPTURA: 06_permisos]

### 2.7 Vincular a tu cuidador
1. En la pestaña **Cuidador** toca **Generar código de invitación**. El código tiene 8 caracteres, dura **10 minutos** y sirve una sola vez.

[CAPTURA: 07_generar_codigo]

2. Dáselo a tu cuidador. Cuando lo use, verás su solicitud con **Aceptar** y **Rechazar**. Si no conoces a esa persona, rechaza.

[CAPTURA: 09_solicitud_pendiente]

3. Al aceptar, tu cuidador puede ver tus tomas de cada día y si las confirmaste. No ve la dosis.

[CAPTURA: 10_vinculo_activo]

Puedes **Desvincular cuidador** en cualquier momento; deja de ver tus tomas de inmediato y después puedes invitar a otra persona. Solo puedes tener un cuidador a la vez.

## 3. Cuidador

### 3.1 Vincular un paciente
Toca **+ Vincular paciente** y escribe el código que generó tu paciente. Queda **Esperando aceptación** hasta que el paciente acepte en su app; mientras tanto no ves sus datos y puedes **Retirar solicitud**.

[CAPTURA: 08_introducir_codigo]

Mensajes posibles: el código no existe o ya se usó; el código venció (dura 10 minutos); demasiados intentos con códigos incorrectos (espera 10 minutos).

### 3.2 Mis pacientes
La lista muestra a cada paciente con cuántas tomas confirmó hoy, cuántas están **Sin confirmar** y la **última sincronización** de su teléfono. Si el teléfono del paciente no tiene conexión, verás el último estado que envió; revisa esa hora antes de alarmarte.

[CAPTURA: 16_ultima_sincronizacion]

### 3.3 Detalle del paciente
Toca un paciente para ver sus tomas de hoy con las tres etiquetas. Es de solo lectura. La dosis solo aparece si el paciente la comparte.

[CAPTURA: 13_cuidador_pendiente]

[CAPTURA: 14_cuidador_confirmada]

[CAPTURA: 15_cuidador_sin_confirmar]

Con **Ver historial de 7 días** ves sus tomas de los últimos 7 días por día, sin la dosis salvo que el paciente la comparta.

[CAPTURA: 19b_historial_cuidador]

Puedes **Dejar de seguir a este paciente**; para volver a verlo necesitarás un código nuevo.

### 3.4 Alarma a los 30 minutos
Si una toma de tu paciente sigue sin confirmar 30 minutos después de su hora, tu teléfono muestra **Toma sin confirmar**. Antes de avisar, la app consulta el estado; si ya se confirmó, no suena. Si no hay conexión, te avisa que revises e indica la última sincronización conocida. Funciona para las tomas del día en que abriste la app.

## 4. Cerrar sesión
**Salir** (paciente) o **Cerrar sesión** (cuidador) borra los datos guardados en el teléfono y cancela los avisos.

Si eres paciente y tienes confirmaciones sin enviar, **Salir** primero intenta enviarlas. Si no hay conexión, la app no te deja salir y lo explica: así no se pierden. Conéctate a internet y toca **Reintentar envío**, o **Quedarme** para seguir usando la app.

[CAPTURA: 18_salir_con_pendientes]
