# Decisiones de la Fase 6

Fase 6: alinear el prototipo con el diseño objetivo de P2 v1.1 y P3 v1.0 (`docs/p2`, `docs/p3`, `docs/adr`, `docs/diagramas`). El alcance quedó en AGENTS.md (secciones 1 y 2), aprobado por el equipo antes de escribir código.

Este archivo se completa paso a paso. Cada sección dice a qué paso pertenece.

## Paso 1. Base de datos v2

### D1. `bd/schema.sql` incorpora el esquema v2 completo
- **Duda:** `docs/p3/schema_v2.sql` está escrito como `ALTER TABLE` sobre el esquema anterior.
- **Elección:** los cambios se integran en los `CREATE TABLE` de `bd/schema.sql`, que sigue borrando y recreando todo. `schema_v2.sql` queda como referencia del diseño.
- **Motivo:** un solo script reproducible; no hay datos reales que migrar.

### D2. Columna extra `links.requested_at`
- **Duda:** el diseño no dice cuándo el cuidador usó el código.
- **Elección:** se agrega `requested_at DATETIME NULL` y una restricción: un vínculo en `awaiting_acceptance` debe tener `caregiver_id` y `requested_at`.
- **Motivo:** el paciente ve desde cuándo espera la solicitud, y la BD impide un estado de espera sin cuidador.

### D3. Rechazar una solicitud usa `revoked`
- **Duda:** P2 pide que el paciente acepte; el paso 7 pide también rechazar, y el ENUM de diseño no tiene `rejected`.
- **Elección:** rechazar deja el vínculo en `revoked` con `revoked_at`, igual que revocar.
- **Motivo:** se respeta el ENUM del diseño y la regla de no borrar vínculos.

### D4. Seed v2
- Paciente 1 sincronizó hace 20 min (`last_sync_at`); su toma de las 08:00 llegó con un UUID sintético (`00000000-0000-4000-8000-00000000000N`) y `received_at`.
- Historial de ayer del Paciente 1 para RF-15: dos confirmadas y una pendiente (se mostrará como "Sin confirmar").
- `share_dose`: Paciente 1 no comparte la dosis con Cuidador 1; Paciente 3 sí. Sirve para probar G-07 con un mismo cuidador.
- El código pendiente `SINT0002` vence 10 min después de cargar el seed (G-01).
- Dos intentos exitosos en `link_attempts`, uno por cada vínculo activo.

### D5. G-07: la API omite la dosis
- **Elección:** en `GET /caregiver/patients/{id}/intakes/today` el campo `dose` no se envía si `share_dose = 0`, y `patient.share_dose` indica si se comparte. El paciente siempre ve su dosis.
- **Cambio en la app:** `IntakeDto.dose` pasa a opcional y la tarjeta de la toma solo la muestra si llegó.
- **Pendiente:** la pantalla para que el paciente cambie `share_dose` es opcional (G-07 solo si sobra tiempo); mientras no exista, la dosis queda oculta por defecto.

### Pruebas realizadas (Paso 1)
- `schema.sql` y `seed.sql` cargados con `STRICT_ALL_TABLES` sobre MariaDB 10.4.32 sin errores; las 7 tablas existen y los datos coinciden con D4.
- Restricciones comprobadas con consultas que deben fallar: UUID repetido (1062), `share_dose = 2` (`chk_links_share_dose`), `awaiting_acceptance` sin cuidador (`chk_links_awaiting_has_caregiver`), estado desconocido (1265) e intento de un usuario inexistente (llave foránea).
- Con curl contra Apache local: Cuidador 1 no recibe `dose` del Paciente 1 y sí del Paciente 3; el Paciente 1 sigue viendo su dosis en `/intakes/today`.

## Paso 2. API de vínculo con aceptación (RF-08, RF-09; G-01, G-02, G-05; ADR-004)

### D6. Código vencido: 410 con mensaje propio
- **Duda:** antes, un código vencido daba el mismo 404 que uno inexistente.
- **Elección:** si el código existe, sigue `pending` y venció, responde `410` con "El código venció. Pide al paciente que genere uno nuevo." Inexistente, usado o revocado sigue en `404`.
- **Motivo:** el paso 7 pide un mensaje claro ante código vencido. Distinguirlo filtra muy poco, porque el código es aleatorio de 8 caracteres y los intentos están limitados.

### D7. Qué cuenta como intento fallido y cómo se bloquea
- **Cuenta como fallo:** formato inválido (422), código inexistente, usado o revocado (404) y código vencido (410). Un uso correcto se registra con `success = 1`.
- **Bloqueo:** con 5 fallos del mismo cuidador en los últimos 10 min, la API responde `429` con `Retry-After` (segundos hasta que el fallo más antiguo salga de la ventana) **antes** de leer el código. Mientras dura, no se registran intentos nuevos (así el bloqueo no se alarga solo) y tampoco se acepta un código válido.
- Los fallos se guardan fuera de la transacción del vínculo, para que el `rollBack` no los borre.

### D8. Rechazo y retiro de la solicitud
- `POST /links/{id}/reject` (paciente) deja la solicitud en `revoked` (D3).
- `DELETE /caregiver/patients/{id}` (cuidador) ahora también retira una solicitud sin aceptar.
- Generar un código nuevo revoca los códigos pendientes **y** las solicitudes sin aceptar del paciente.

### D9. Dónde ve cada rol la solicitud
- Paciente: `GET /links/pending` y el campo `pending_request` de `/me`.
- Cuidador: la lista `awaiting` de `/me` (pacientes que aún no aceptan). No hay endpoint aparte.
- Una solicitud en espera no da acceso a datos: los endpoints del cuidador siguen exigiendo `status = 'active'`.

### D10. Incompatibilidad temporal con la app
- `POST /links/accept` ya no devuelve `linked_at`. La app actual espera ese campo, así que la vinculación desde el cuidador falla al leer la respuesta hasta el paso 7, que adapta la app al flujo con aceptación.

### Pruebas realizadas (Paso 2)
Con curl contra Apache local y la BD del seed v2 (usuarios sintéticos):
- Flujo completo Paciente 2 y Cuidador 2: invitar (201, vence en 10 min) → usar código (200, `awaiting_acceptance`) → el cuidador recibe 404 en las tomas del paciente → `GET /links/pending` y `/me` (`awaiting`) muestran la solicitud → aceptar (200, `active`) → el cuidador ya ve las tomas, sin `dose`.
- Mismo código usado dos veces: 404. Aceptar dos veces: 404. Cuidador intentando aceptar: 403.
- Código con `expires_at` movido al pasado por SQL: 410.
- Rechazo: 200 y el vínculo queda `revoked` con `revoked_at`.
- Cuidador 1 con 5 códigos inexistentes: 404 cinco veces, el sexto 429 con `Retry-After: 599`; quedaron 5 fallos registrados; un código válido durante el bloqueo también da 429.

## Paso 3. API de tomas (RF-07, RF-10 a RF-13, RF-15; G-03, G-04; ADR-003, ADR-005)

### D11. Forma del lote de sincronización
- **Duda:** el prompt describe el lote como una lista, pero la API solo acepta objetos JSON como cuerpo.
- **Elección:** `{"confirmations":[{client_uuid, schedule_id, scheduled_for, confirmed_at}, ...]}`, máximo 100 por envío, con un resultado por elemento: `created`, `duplicate`, `already_confirmed` o `rejected`.
- **Motivo:** se conserva la validación del cuerpo de todas las rutas. Con un resultado por elemento, un dato malo no tumba el lote y la app sabe qué sacar de su cola.

### D12. Reglas de idempotencia
- Un `client_uuid` ya recibido del mismo paciente devuelve `duplicate`, sin tocar la BD. Si es de otro paciente se rechaza sin revelar nada.
- Dos envíos simultáneos del mismo UUID: el segundo choca con el índice único (1062) y también responde `duplicate`.
- Si la toma ya estaba confirmada con otro UUID (por ejemplo, desde `/take` o desde otro envío), se conserva la primera confirmación y responde `already_confirmed`.
- Si la toma no existía (por ejemplo, el teléfono confirmó sin red y nadie consultó ese día), se crea ya confirmada.

### D13. Validación de cada confirmación
- El horario debe ser del paciente (aunque después se haya desactivado: la confirmación pudo hacerse antes), la hora de `scheduled_for` debe coincidir con la del horario y la fecha debe estar en los últimos 7 días.
- `confirmed_at` es la hora del toque en el teléfono (RF-07). Se acepta hasta 5 min en el futuro (reloj adelantado) y desde un día antes de la toma (confirmaciones tempranas, como permitía `/take`). `received_at` es la hora del servidor.

### D14. `last_sync_at` se actualiza en cada sincronización, aunque venga vacía
- **Motivo:** RF-12 quiere que el cuidador sepa cuándo el teléfono se comunicó por última vez. Un lote vacío sirve de señal de vida. `/take` también lo actualiza.

### D15. Estado calculado en SQL
- `state` (`pending`, `confirmed` o `unconfirmed`) se calcula en la consulta con `NOW()` de MariaDB (zona `America/Mexico_City`), y nunca se guarda (ADR-005). Se mantiene `status` y los campos viejos de `summary` para no romper la app actual.
- Las respuestas incluyen `now`, para que la app compare con la hora del servidor.

### D16. Historial de 7 días y días no consultados
- **Duda:** las tomas solo se creaban al consultar el día; un día sin consultas no tendría tomas y una omisión no se vería.
- **Elección:** `/intakes/history` rellena con `INSERT IGNORE` los días faltantes (hoy menos 6 a ayer) como pendientes, solo para horarios activos cuya `created_at` es anterior a esa hora. Esos huecos aparecen como "Sin confirmar".
- **Limitación:** de un horario ya desactivado no se rellenan días, porque no se guarda cuándo se desactivó; solo se ven las tomas que ya existían.
- El cuidador tiene su propio historial (`/caregiver/patients/{id}/intakes/history`), con las mismas reglas de vínculo activo y dosis oculta.

### Pruebas realizadas (Paso 3)
Con curl contra Apache local (usuarios sintéticos):
- Lote de 4 con Paciente 1: uno `created`; horario de otro paciente, UUID mal formado y hora que no coincide con el horario, `rejected`. Reenviar el mismo lote: `duplicate` para el primero y en BD sigue una sola fila con ese UUID y una sola toma de ese horario y día.
- Otro UUID para la misma toma: `already_confirmed`. Lote vacío: 200 y `last_sync_at` actualizado. Sin `confirmations`: 422. Cuidador: 403.
- El cuidador ve `last_sync_at` en la lista y en el detalle, y `state` en cada toma.
- Historial del Paciente 1: ayer 14:00 sale `unconfirmed` (pendiente del seed), las confirmadas `confirmed`; el cuidador recibe lo mismo sin `dose`; Cuidador 2 sin vínculo recibe 404.
- El límite exacto de 30 min se prueba con horas controladas en el paso 4.

## Paso 4. Pruebas automáticas de la API

### D17. Script en PHP CLI con cURL
- **Elección:** `api/tests/run_api_tests.php`, ejecutado con `C:\xampp\php\php.exe` (trae cURL y PDO). Sin dependencias nuevas.
- **Motivo:** corre igual en cualquier máquina con XAMPP, y PHP permite consultar la BD para verificar lo que el HTTP no muestra (filas duplicadas, intentos registrados, `revoked_at`).

### D18. Datos propios y limpieza
- Cada corrida registra sus usuarios (`t<fecha><n>p@ejemplo.test`, nombres con SINTETICO) y al final los borra, con sus datos en cascada. `--keep` los conserva para revisarlos.
- No depende del seed ni lo modifica, así que se puede correr con la BD en cualquier estado.

### D19. Tiempo simulado
- El reloj no se adelanta: se mueven fechas en la BD de forma controlada. Para el código vencido, `expires_at` 1 s en el pasado. Para "Sin confirmar", la hora programada de una toma se pone a ahora menos 29 min (debe seguir Pendiente) y a ahora menos 31 min (debe pasar a Sin confirmar), y se verifica que en la BD sigue `pending`.
- La consulta se hace con el historial del cuidador, que abarca 7 días; así la prueba funciona aunque se corra poco después de medianoche.

### D20. Log
- `docs/evidencias/p4/api-AAAAMMDD-HHMM.log`. Si ya existe uno de ese minuto se agrega `-2`, `-3`, etc., para no sobrescribir evidencia. Código de salida 0 si todo pasa y 1 si algo falla.

### Pruebas realizadas (Paso 4)
- Corrida completa: 27 OK, 0 FALLO (`docs/evidencias/p4/api-20261121-1215.log`).
- Comprobación de que el script detecta errores: con la regla cambiada a propósito a 20 min, API-23 falló ("T+29: unconfirmed") y el script salió con código 1. Se restauró la regla de 30 min y se repitió la corrida limpia. El log de la corrida saboteada se borró, porque la había sobrescrito en el mismo minuto (por eso se agregó D20).
- `api/tests/` y `docs/evidencias/` responden 403 por web.

## Paso 5. App: avisos y confirmación (RF-05, RF-06, RF-07, RF-14, RF-16; ADR-002, ADR-006)

Nota: desde este paso el equipo pidió trabajo autónomo. Las decisiones se toman con la opción más simple que cumple el diseño y se anotan aquí; los bloqueos van en `docs/PENDIENTES_FASE6.md`.

### D21. Una sola alarma pendiente con tres tipos de evento
- Se conserva el esquema de la Fase 5 (D5): una alarma exacta, la del próximo evento. Ahora hay tres tipos: `BEFORE` (T menos L), `DUE` (T, una sola vez, RF-06) y `SNOOZE` (pospuesto, RF-16). La función pura `nextReminderEvent` los calcula para ayer, hoy y mañana (hora de México).
- Cada evento se registra al mostrarse (`ReminderStore.fired`), así no se repite.
- **Al sonar se revisa la copia local:** si todos los medicamentos de esa hora ya están confirmados, no se muestra nada. Esto reemplaza la D3 de la Fase 5 ("se avisa aunque ya esté marcada"), porque ahora el teléfono sabe qué se confirmó.
- Límite de retraso: `DUE` y `SNOOZE` no se muestran si llegan más de 10 min tarde (igual que D8 de la Fase 5). `BEFORE` solo vale antes de T.

### D22. Anticipación L
- Ajuste local en `SettingsStore` (no se envía al servidor, G-06), pantalla "Ajustes" del paciente con dos opciones (5 o 10 min, 5 por defecto). Cualquier valor guardado que no sea 5 o 10 se lee como 5 (`normalizeLead`). Al cambiarlo se reprograma de inmediato.

### D23. Posponer desde la notificación
- La notificación trae los botones "Posponer 5 min" y "Posponer 10 min" (`SnoozeReceiver`, no exportado). Posponer no marca la toma. Un pospuesto por toma: posponer otra vez lo reemplaza.
- Si el pospuesto cae a la hora T o después, sustituye al segundo aviso, para no sonar dos veces seguidas.
- No se agregó botón "Tomado" en la notificación: tocarla abre "Hoy" (se mantiene D4 de la Fase 5).

### D24. Copia local cifrada `PatientStore`
- Medicamentos con sus horarios (incluido el `schedule_id`) y tomas que el servidor reporta como confirmadas, en `patient_local.xml` cifrado con `LocalCipher` (AES-GCM, Keystore) y excluido de respaldos. Reemplaza la copia de la Fase 5 en `reminders.xml`, que ahora solo guarda eventos mostrados y pospuestos (sin nombres ni dosis).
- **Cambio en la API:** `GET /medications` y `/medications/{id}` agregan `schedules: [{id, time}]` (se mantiene `times`). Sin el identificador, el teléfono no podría saber qué toma confirmó ni enviarla sin red.

### D25. "Tomado" con un toque y etiquetas
- Se quitó el diálogo de confirmación (RF-07, ADR-006). El botón se desactiva mientras se envía para evitar dobles toques.
- Etiquetas en todas las tarjetas: "Pendiente", "Confirmada por el paciente a las HH:MM" y "Sin confirmar", recalculadas con la hora de México. La palabra "Tomada" desapareció de la interfaz. Se muestra la leyenda de que la confirmación no verifica la ingesta.
- En este paso "Tomado" todavía usa `POST /intakes/{id}/take` (requiere red); el paso 6 lo cambia a la cola local.

### D26. Advertencia de permisos (RF-14), adelantada del paso 9
- `PermissionsCard` revisa notificaciones, alarmas exactas y ahorro de batería al volver a la app, con un botón para corregir cada uno, y desaparece cuando todo está bien. Va en "Hoy" y, siempre visible, en "Ajustes". Se hizo aquí porque la pantalla de ajustes la necesita.

### Pruebas realizadas (Paso 5)
- `assembleDebug` compila. Pruebas unitarias: 48 en verde (las 32 anteriores sin cambios, más 12 de `ReminderEventTest` y 4 de `IntakeStateTest`).
- Suite de la API repetida tras el cambio en medicamentos: 27 OK (`docs/evidencias/p4/api-20261121-1223.log`).
- La verificación en emulador va con los casos PR de la Fase 7. Puntualidad, reinicio y ahorro de batería en teléfono físico: pendiente de ejecución manual.

## Paso 6. App: modo sin red (RF-13; ADR-003)

### D27. Archivo cifrado en lugar de Room
- **Opciones:** (a) Room; (b) Room con SQLCipher; (c) el JSON cifrado que ya usa la app (`LocalCipher`, AES-GCM con clave en Android Keystore) en un archivo de preferencias.
- **Elección:** (c), en `PatientStore` (`patient_local.xml`, excluido de respaldos).
- **Motivo:** son pocos datos (medicamentos activos, tomas de hoy y una cola corta), se leen y escriben completos y no hacen falta consultas. Room no cifra por sí mismo; cifrar exigiría SQLCipher, más dependencias y un procesador de anotaciones. La opción (c) no agrega dependencias (regla de AGENTS.md) y reutiliza el cifrado ya probado de la sesión.

### D28. Cola de confirmaciones
- "Tomado" crea en el teléfono una `LocalConfirmation` con `UUID.randomUUID()`, la hora de México del toque y el `schedule_id`. Se guarda antes de cualquier llamada a la red, así que la pantalla y los avisos se actualizan sin conexión.
- Un doble toque o una toma ya confirmada en el servidor no se vuelve a encolar (`enqueueConfirmation`).
- Al sincronizar, se quitan de la cola las respuestas `created`, `duplicate`, `already_confirmed` y `rejected`. Las tres primeras pasan a "confirmadas en el servidor"; una rechazada se descarta (reintentar no la arreglaría) y se registra en el log.
- Se reintenta solo ante falta de red o `5xx` (`shouldRetrySync`).

### D29. Cuándo se envía la cola (sin WorkManager)
- Justo después de cada toque, al abrir o actualizar "Hoy" (con lote vacío, para registrar la última sincronización), al recuperar la red (`registerDefaultNetworkCallback`, con permiso `ACCESS_NETWORK_STATE`) y con reintentos de 30 s, 1, 2 y hasta 5 min mientras queden pendientes.
- **Motivo:** cubre el caso de uso sin agregar WorkManager. **Limitación:** si el sistema cierra la app sin red, la cola se envía la próxima vez que la app arranque (al abrirla o cuando suene una alarma). Anotado en PENDIENTES.

### D30. "Hoy" desde la copia local
- La pantalla se arma con `buildTodayList`: la última lista de hoy que mandó el servidor (incluye tomas confirmadas de medicamentos ya desactivados), más las horas de los medicamentos guardados que falten, con la cola encima. Muestra "Guardada en el teléfono; se enviará al haber conexión" en las tomas sin enviar, un aviso de "Sin conexión" y la hora de la última sincronización. El estado se recalcula cada minuto.
- La pestaña "Medicamentos" (alta y edición) sigue necesitando red: dar de alta un medicamento sin servidor no tendría `schedule_id`.

### Pruebas realizadas (Paso 6)
- Compila. 57 pruebas unitarias en verde (9 nuevas en `OfflineQueueTest`: doble toque, toma ya confirmada, respuesta repetida idempotente, sin respuesta sigue en cola, rechazada, 3 confirmaciones sin red, reglas de reintento y lista de hoy local).
- Prueba en emulador con modo avión: en la Fase 7 (PR-08).

## Paso 7. App: vínculo con aceptación (RF-08, RF-09)

### D31. Pantallas
- Paciente, pestaña "Cuidador": si hay una solicitud (`pending_request` de `/me`), muestra el nombre del cuidador, cuándo usó el código y los botones grandes "Aceptar" y "Rechazar", con la sugerencia de rechazar si no lo conoce. El texto del código dice que dura 10 minutos.
- Cuidador: al usar el código, el mensaje es "Solicitud enviada a X. Espera a que la acepte". La lista principal muestra la sección "Esperando aceptación" (`awaiting` de `/me`) con "Retirar solicitud".
- Mensajes claros (`linkErrorMessage`): 410 "El código venció (dura 10 minutos)...", 429 "Demasiados intentos... Espera 10 minutos", 404 "no existe o ya se usó". 422 conserva el mensaje del campo.
- Con esto se resuelve la incompatibilidad temporal de D10.

### D32. `share_dose` sin pantalla (G-07)
- No se agregó un control para que el paciente comparta la dosis: el prompt lo marca como opcional y el diseño permite diferirlo (ADR-010). La dosis queda oculta al cuidador por defecto, que es lo que exige RF-10. Anotado en PENDIENTES.

### Pruebas realizadas (Paso 7)
- Compila. 60 pruebas unitarias en verde (3 nuevas en `LinkMessagesTest`). El flujo completo en emulador se ejecuta en la Fase 7 (PR-05).

## Paso 8. App: vista del cuidador (RF-10 a RF-12, RF-18; ADR-007, ADR-010)

### D33. Vista
- Tarjetas con las tres etiquetas (mismo `IntakeCard` que el paciente), con la leyenda de que "Confirmada por el paciente" no verifica la ingesta. Se quitó el texto viejo que decía "Tomada".
- Lista de pacientes: "N de M tomas registradas", "Sin confirmar: N" en rojo si hay, y "Última sincronización: fecha hora" (RF-12). El detalle repite la última sincronización y explica que, sin conexión del paciente, se ve el último estado enviado.
- La dosis solo aparece si la API la manda (`share_dose = 1`); si no, se indica "La dosis no se muestra: el paciente no la comparte".

### D34. Alarma del cuidador a T + 30 (RF-18)
- `CaregiverAlarmScheduler` guarda (cifrado, sin respaldo, `caregiver_alarms.xml`) las tomas pendientes de hoy de todos sus pacientes con T + 30 en el futuro, obtenidas de `GET /caregiver/patients` y `GET /caregiver/patients/{id}/intakes/today`. Una sola alarma exacta, la más cercana.
- Se actualiza al iniciar sesión o abrir la app como cuidador, al cargar la lista de pacientes y tras reiniciar el teléfono (con la copia local).
- Al sonar (`goAsync`, máximo 15 s) vuelve a consultar al servidor: si la toma sigue pendiente avisa "Toma sin confirmar"; si no pudo consultar avisa "Revisa a ...", con la última sincronización conocida; si el vínculo se revocó (404) no avisa y deja de vigilar a ese paciente; si ya está confirmada, no avisa.
- Limitación (ADR-007): solo cubre tomas de los días en que el cuidador abrió la app. Las de mañana se programan cuando la abra.

### Pruebas realizadas (Paso 8)
- Compila. 65 pruebas unitarias en verde (5 nuevas en `CaregiverAlarmTest`: T + 30, qué tomas se vigilan, la más cercana, avisar solo si sigue sin confirmar, sin conexión y vínculo revocado). La alarma en emulador y teléfono se prueba en la Fase 7 (PR-12).

## Paso 9. Permisos y accesibilidad (RF-14, RNF-04)

### D35. Permisos
- La tarjeta de permisos (D26) también aparece en la lista del cuidador, y la app pide el permiso de notificaciones al cuidador, porque su alarma a T + 30 lo necesita.
- Revisa notificaciones, alarmas exactas (`canScheduleExactAlarms`) y ahorro de batería (`isIgnoringBatteryOptimizations`). Cada punto trae un botón que abre el ajuste del sistema correspondiente. Para la batería se abre la lista de optimización en lugar de pedir la exención directa, para no requerir el permiso `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. También se recuerda revisar No molestar, que la app no puede consultar sin permisos especiales.

### D36. Tamaño de letra y botones
- Tipografía base más grande (cuerpo 18 sp, títulos 19 a 26 sp; nada por debajo de 14 sp), todo en `sp` para que también crezca con el tamaño de letra del sistema.
- Botones principales ("Tomado", "Aceptar", "Rechazar") de al menos 56 dp de alto; los demás de al menos 48 dp, que es el mínimo de Material.
- Las tarjetas exponen su estado a TalkBack (`stateDescription`) con la etiqueta de la toma.

### Pruebas realizadas (Paso 9)
- Compila; 65 pruebas unitarias en verde. La revisión visual con letra del sistema grande se hace con capturas en la Fase 7.

## Paso 10. Pruebas unitarias del cliente y cierre

- Pruebas unitarias: 65 en verde. Las 32 de las fases anteriores no cambiaron. Las 33 nuevas cubren: estado calculado (`IntakeStateTest`), cálculo de avisos T menos L, T, pospuestos y reglas de L (`ReminderEventTest`), idempotencia de la cola (`OfflineQueueTest`), alarma T + 30 del cuidador (`CaregiverAlarmTest`) y mensajes de vínculo (`LinkMessagesTest`).
- README actualizado (alcance RF-01 a RF-16 y RF-18, RF-17 fuera, cómo correr las pruebas). `docs/api.md` incluye los endpoints nuevos y `schedules` en medicamentos.
- La tabla de requisitos con archivos, pruebas y pendientes manuales está en `RESUMEN_FASE6.md` (raíz), con los resultados de las pruebas en emulador de la Fase 7.
