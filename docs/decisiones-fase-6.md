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

## Fase 8

### D37. Cerrar sesión con confirmaciones sin enviar
- **Opciones:** (a) bloquear "Salir" hasta sincronizar; (b) preguntar y permitir salir perdiendo las confirmaciones; (c) conservar la cola cifrada después de salir.
- **Elección:** (a). "Salir" del paciente intenta enviar la cola; si quedan pendientes, muestra "Confirmaciones sin enviar" con cuántas son, que se perderían y qué hacer, y solo ofrece "Reintentar envío" y "Quedarme". No hay opción para descartarlas.
- **Motivo:** es lo más simple que no pierde datos de salud registrados por el paciente. (c) dejaría datos de un usuario en el teléfono después de salir. La regla está en `canLogout` y `pendingLogoutMessage` (`LogoutGuardTest`).
- **Prueba en emulador:** en modo avión se confirmó la toma de 13:10 y "Salir" mostró el diálogo (`docs/capturas/18_salir_con_pendientes.png`). "Reintentar envío" sin red mantuvo el diálogo. Con la red de vuelta, la confirmación llegó al servidor (received_at 14:11:53) y la sesión se cerró.

### D38. Paquete de despliegue para hosting
- **Elección:** un script (`deploy/build_hosting_package.php`) arma `build/hosting/` con `public_html/` (solo `index.php` y `.htaccess`) y, un nivel arriba, `src/`, `config/` y `.env.example`. El esquema para hosting se genera de `bd/schema.sql` quitando `CREATE DATABASE` y `USE`, porque el hosting asigna el nombre de la base. El script falla si el paquete trae un `.env` o archivos extra en `public_html`.
- **Motivo:** en hosting compartido la raíz web suele ser fija (`public_html`). Con esta estructura, `.env`, `src` y `config` quedan fuera de la web aunque el hosting ignore los `.htaccess`. Generar el esquema evita mantener dos copias.
- **Seed sin cuentas de demo:** `bd/seed_hosting.sql` no inserta nada; solo verifica que las 7 tablas existen y están vacías. En un servidor público las cuentas se crean desde la app.
- **Plantilla:** `api/.env.hosting.example` con `APP_DEBUG=false`.
- **Prueba:** solo local (base temporal y servidor embebido de PHP); nada contra servidores externos. Ver `docs/instalacion.md` 3.2.

### D39. Pantalla de historial de 7 días (RF-15)
- **Elección:** una pantalla reutilizable (`ui/common/HistoryList.kt`) con los días agrupados ("Hoy", "Ayer" y fechas), el resumen de cada día y las mismas tarjetas de toma. El paciente la ve en la pestaña nueva "Historial"; el cuidador, con el botón "Ver historial de 7 días" del detalle del paciente.
- **Motivo:** reutiliza `GET /intakes/history` y `GET /caregiver/patients/{id}/intakes/history` (paso 3) y las etiquetas de `IntakeCard`, sin lógica nueva de estado. La dosis sigue oculta al cuidador porque la API no la envía.
- **Limitación:** necesita conexión; no se guarda copia local del historial.
- **Prueba:** `HistoryLabelTest` (3 pruebas). En emulador: el paciente demo ve hoy con 7 de 7; el Cuidador 1 del seed ve "Ayer, 20/11/2026: 2 de 3 tomas registradas, 1 sin confirmar" de su paciente, sin dosis.

## Fase 9 (versión 1.1.0)

### D40. Etiqueta y versión
- El commit evaluado en el teléfono físico (`827eef4`) quedó etiquetado como `v1.0-evaluada` (etiqueta local, sin push).
- `versionName` pasa a `1.1.0` y `versionCode` a `2`. APK debug compilado antes de los cambios de código de esta fase: `app-debug.apk`, SHA-256 `f3842fe4a53a99d35c99b6d8a990671a06b7fabef479068705218db269fa86f5`. El APK final de la fase (con los cambios) se registra en `docs/evidencias/p4/emulador_v1.1/RESUMEN.md`.

### D41. Ajuste de requisito por decisión del equipo: RF-06 pasa a alarma del paciente
- **Antes (v1.0):** a la hora T, un segundo aviso que sonaba una sola vez.
- **Ahora (v1.1):** a la hora T suena una **alarma** con sonido y vibración que se repiten hasta que el paciente toca "Tomado" o "Posponer 5/10 min" en la notificación o en la pantalla de alarma. Límite: 2 minutos de sonido; después queda una notificación persistente sin sonido, con los mismos botones, hasta que la atienda. Si la toma ya está confirmada, la alarma no suena. Lo mismo vale al terminar un pospuesto.
- **Aviso previo (T - L, RF-05):** sin cambios de comportamiento; ahora su canal tiene vibración.
- **Cuidador (RF-18):** sin cambios de comportamiento; notificación normal (sonido y vibración breves, sin repetir) con el mensaje de respaldo sin red. Su canal nuevo tiene vibración.
- **Mecanismo elegido:**
  - Canal `intake_alarm_v2`, importancia alta, vibración activada, sonido de alarma del sistema (`RingtoneManager.TYPE_ALARM`) con `AudioAttributes.USAGE_ALARM`, y la bandera `Notification.FLAG_INSISTENT` (repite sonido y vibración hasta que se cancela la notificación). Categoría `CATEGORY_ALARM`, `ongoing` y `fullScreenIntent` a `AlarmActivity`, que se muestra sobre la pantalla de bloqueo.
  - El corte de 2 minutos es una alarma exacta aparte (`RingLimitReceiver`): si la toma sigue pendiente, vuelve a publicar la notificación en el canal `intake_alarm_silent_v2` (sin sonido ni vibración, persistente); si ya se confirmó, la quita.
  - "Tomado" en la notificación o en la pantalla de alarma confirma todos los medicamentos pendientes de esa hora con la misma cola local de RF-13 y la envía si hay red. "Tomado" y "Posponer" cancelan la notificación (eso corta el sonido) y el corte de 2 minutos. Confirmar desde "Hoy" también detiene la alarma.
  - **Motivo:** `FLAG_INSISTENT` es el mecanismo estándar para repetir el sonido de una notificación hasta que se atiende, sin un servicio en primer plano ni reproducir audio propio; `USAGE_ALARM` usa el volumen de alarma.
- **Modo silencio y No molestar:** con `USAGE_ALARM` el sonido sigue el **volumen de alarma**, así que en modo vibración o silencio el teléfono suele sonar si ese volumen no está en cero (depende del fabricante). Con No molestar, las notificaciones de categoría alarma pasan solo si el usuario permite "Alarmas" en No molestar (lo habitual por defecto); la app no pide el permiso de política de No molestar para saltárselo. Esto se indica en la guía de uso.
- **Pantalla completa:** se declaró `USE_FULL_SCREEN_INTENT`. En Android 14 o superior el sistema puede no conceder la pantalla completa a apps que no son de reloj o llamadas; en ese caso la alarma queda como notificación emergente que sigue sonando, y la pantalla se abre al tocarla.
- **Canales anteriores:** `intake_reminders` (v1.0, configuración anterior sin vibración) y `caregiver_alerts` se eliminan al iniciar la app; los nuevos tienen IDs distintos porque Android no cambia los ajustes de un canal ya creado.
- La "Tomado" en la notificación sustituye a D4 de la Fase 5 (antes solo se abría la app): ahora la confirmación está en la cola local y no requiere red.

### D42. Corrección de alarmas atrasadas: una alarma de reloj por aviso
- **Hallazgo (verificación física v1.0):** con la pantalla apagada, HyperOS entregó tarde algunas alarmas `setExactAndAllowWhileIdle`, y como la app programaba una sola alarma a la vez (la del próximo evento), un retraso arrastraba a los siguientes avisos.
- **Opciones:** (a) una alarma por aviso con el mismo método; (b) `setAlarmClock` para la alarma única; (c) las dos cosas.
- **Elección:** (c). `upcomingReminderEvents` calcula todos los avisos pendientes de ayer, hoy y mañana (T - L, T y pospuestos) y `reschedule()` programa cada uno con su propio `PendingIntent` mediante `AlarmManager.setAlarmClock`. Los códigos son deterministas (`alarmRequestCode`: día en ciclo de 400 días, minuto del día y tipo), se guardan en `ReminderStore`, y los que sobran se cancelan. También se cancela la alarma única de la v1.0 (código 0) al actualizar.
- **Motivo:** `setAlarmClock` es el tipo de alarma que el sistema trata como alarma de reloj del usuario: no la difiere por Doze ni la agrupa, y sale del modo de reposo poco antes. Una alarma por aviso elimina el arrastre aunque alguna llegara tarde. El cambio es local al planificador; la lógica de qué avisos tocan no cambió.
- **Efecto visible:** Android muestra el ícono de alarma en la barra de estado y la "próxima alarma" en el reloj del sistema; tocarla abre la app.
- **Reinicio:** `BootReceiver` llama a `reschedule()`, que vuelve a programar todos los avisos. **El desbloqueo sigue siendo necesario:** Android entrega `BOOT_COMPLETED` hasta el primer desbloqueo, y los datos de la app (cifrados, en el almacenamiento de credenciales) no se pueden leer antes. Evitarlo requeriría reestructurar la app para el arranque directo (receptor `directBootAware` con `LOCKED_BOOT_COMPLETED` y una copia de las horas en el almacenamiento protegido del dispositivo, sin acceder al resto de la app antes del desbloqueo). Se deja como pendiente.

## Fase 10 (pruebas de la v1.1.0 en el teléfono físico)

### D43. Método de medición en el teléfono con la v1.1
- **Elección:** la hora esperada sale de `origWhen` de cada línea `sending alarm ... INTAKE_REMINDER` del log del sistema (en la v1.1 cada aviso tiene su propia alarma, así que coincide exactamente con T - 5 o T), y la hora real es la siguiente publicación (`NotificationManagerService:post:mx.cuidadosis.app`). El log se guarda en la PC con un flujo continuo filtrado a la app (`adb logcat`), que no despierta la pantalla. Script: `api/tests/puntualidad_v11_csv.php`.
- **Motivo:** en la v1.0, HyperOS no guardó el log propio de la app con la pantalla apagada; el log del sistema sí. El flujo continuo evita perder líneas si el búfer rota.
- **Pantalla apagada con la v1.1:** la alarma a T abre la pantalla de alarma, que enciende la pantalla por diseño (`setTurnScreenOn`). La pantalla vuelve a apagarse sola a los 60 s; por eso las tomas se espacian 2 minutos, igual que en la v1.0, y el escenario se mantiene comparable. No se toca el teléfono durante la medición.
- **Observación del emulador:** el usuario oyó el sonido de la alarma en las bocinas de la laptop durante el retest en el emulador; queda como observación del emulador, sin sustituir la prueba física.

### D44. Versión 1.1.1: el corte de 2 minutos también como alarma de reloj
- **Hallazgo (teléfono físico, v1.1.0, pantalla apagada):** los avisos (`setAlarmClock`) llegaron puntuales y HyperOS registró "not align this alarm" para cada uno, pero los cortes de 2 minutos de la alarma (`RingLimitReceiver`, programados con `setExactAndAllowWhileIdle`) los agrupó: se entregaron juntos a las 21:57:25, 22:02:25, 22:07:25 y 22:12:25. La notificación insistente siguió activa entre 145 y 385 s, en lugar de 120 s.
- **Elección:** programar el corte con `setAlarmClock` (mismo mecanismo que los avisos) y subir a 1.1.1 (`versionCode` 3). APK: SHA-256 `157fcf454f381fa2daaadd7712a0cd0b86ee1cbe1d080062e9dd6ccd0f3d2938`.
- **Motivo:** es el cambio mínimo; en este teléfono las alarmas de reloj no se agrupan (comprobado con los avisos). Efecto secundario: Android también muestra el corte como "próxima alarma" durante esos 2 minutos.
- **Prueba:** se instala después de terminar las mediciones de la 1.1.0 y se repite solo el límite de 2 minutos con la pantalla apagada (paso 6).

### D45. Pasos con toques y capturas en el teléfono (Fase 10)
- **APK de los pasos 2 a 7:** 1.1.1, instalada después de medir la puntualidad con la 1.1.0; solo cambia el corte de 2 minutos (D44).
- **Capturas sin datos ajenos:** las pantallas del panel de notificaciones y del inicio de sesión se recortan con `System.Drawing` (PowerShell) a la tarjeta de CuidaDosis o por encima de la línea "Servidor", porque el panel muestra notificaciones de otras apps, el fondo de la pantalla de bloqueo puede tener fotos y el inicio de sesión muestra la IP de la PC. Una captura del aviso previo que mostró notificaciones de otra app se borró sin conservarse. La pantalla Servidor se capturó con la URL genérica `http://IP-DE-TU-PC/CuidaDosis/api/public/` escrita sin guardar.
- **Pantalla de alarma sobre el bloqueo:** se acepta como evidencia el registro del paso 1 (apertura automática sobre el bloqueo, `mOccluded=true`, 22:25 a 22:29). No se pudo repetir por adb para la captura: HyperOS apaga la pantalla bloqueada en segundos y con la pantalla apagada solo enciende la luz de bordes. Queda como pendiente manual.
- **PR-12:** por instrucción del usuario se deja preparado y no se ejecuta; la observación del sonido la hace el usuario con el teléfono en mano. Para las capturas del cuidador se inició su sesión y se cerró antes de su primera alarma a T + 30 (00:10), lo que canceló las alarmas.

## Fase 11 (preparación del hosting compartido, 23/11/2026)

### D46. Prefijo `cd_` en todas las tablas por despliegue compartido
- **Contexto:** la API se alojará en `areproyectos.com.mx/cuidadosis/`, en un hosting donde una misma base de datos contiene tablas de varios proyectos. Nombres como `users` o `links` chocarían con los de otros proyectos, y `schema.sql` borraba esas tablas con `DROP TABLE IF EXISTS users`.
- **Elección:** todas las tablas llevan el prefijo `cd_` al inicio (`cd_users`, `cd_links`, `cd_medications`, `cd_schedules`, `cd_intake_logs`, `cd_api_tokens`, `cd_link_attempts`), así se agrupan al ordenarlas. Las llaves foráneas, restricciones CHECK, llaves únicas e índices también: `fk_cd_links_patient`, `chk_cd_links_share_dose`, `uq_cd_users_email`, `idx_cd_links_patient`. En MariaDB y MySQL los nombres de llaves foráneas son únicos en toda la base, así que sin el prefijo chocarían con los de otro proyecto.
- **Alcance:** `bd/schema.sql`, `bd/seed.sql`, `bd/seed_hosting.sql`, todas las consultas de `api/src/` (incluidos JOIN, subconsultas e `INSERT IGNORE`), `api/tests/run_api_tests.php` y la documentación vigente (README, AGENTS, `docs/api.md`, `docs/instalacion.md`, `docs/configuracion.md`, `MANUALES_FUENTE.md`, `PENDIENTES_FASE6.md`). Los diagramas y documentos de P2 y P3, los ADR y las evidencias fechadas (logs y registros de pruebas anteriores) se dejan con los nombres lógicos o los de su momento.
- **`schema.sql` ya no borra tablas sin prefijo:** solo hace `DROP TABLE IF EXISTS cd_*`. Borrar `users` en una base compartida podría destruir datos de otro proyecto. Por eso, en una base local anterior las tablas viejas se quitan a mano, después de un respaldo (`docs/instalacion.md` 1.6).
- **Protección en el paquete:** `deploy/build_hosting_package.php` se niega a armar el paquete si el esquema crea, borra, vacía, altera o referencia una tabla sin `cd_` (probado con una copia alterada: falla con "Sin prefijo: users").
- **Prefijo literal, no constante de PHP (decisión propia, sin consultar por estar el usuario ausente):** se evaluó una constante (`Tables::PREFIX`) y se descartó. El prefijo también vive en los archivos SQL y en las pruebas, que no pueden leer una constante de PHP, así que no sería una sola fuente real; además habría que convertir unas 70 consultas en concatenaciones, lo que vuelve el SQL más difícil de leer y de revisar con grep, con riesgo de errores sin beneficio práctico. Con el nombre literal, un solo grep comprueba que no quedan nombres viejos.
- **Lógica de negocio:** sin cambios. Solo cambian los nombres físicos.
- **Pruebas (23/11/2026, BD local regenerada):** suite de la API 27 OK, 0 FALLO (`docs/evidencias/p4/api-20261123-1208.log`); pruebas unitarias de Android 79 de 79 (forzadas con `--rerun`; no dependen de la BD); SEG-05 22 de 22 rutas con 403 (11 por localhost y 11 por la IP local); login y consultas reales de paciente1 y cuidador1. Base compartida simulada: con tablas `users` y `links` de "otro proyecto" (y una llave `fk_links_patient`) en la misma base, el esquema se importó sin errores y no las tocó.
- **Respaldo previo:** `mysqldump` de la base local en `C:\xampp\backup_cuidadosis\` (fuera del repo, no versionado), verificado antes de borrar las tablas viejas.

### D47. API servible en una subcarpeta (`APP_BASE_PATH`) y paquete con `--subdir`
- **Rutas:** `Request::routePath` quita el prefijo de `APP_BASE_PATH` (nuevo en el `.env`, opcional) si la URL empieza con él; si está vacío o no coincide, quita el directorio de `index.php` como antes. Así funciona sin cambios en XAMPP (`/CuidaDosis/api/public`) y en `/cuidadosis/api`, sin escribir el dominio ni la ruta en el código. El prefijo se compara por segmentos completos (`/cuidadosis/api` no coincide con `/cuidadosis/apix`). Prueba: `api/tests/route_path_test.php`, 13 de 13.
- **`.htaccess`:** sin cambios de reglas. Sus reglas son relativas a su carpeta y ya funcionan en una subcarpeta de XAMPP. Solo se agregó un comentario con `RewriteBase /cuidadosis/api/` para el caso de un hosting con alias.
- **Paquete (decisión propia):** con `--subdir=cuidadosis`, el script deja `public_html/cuidadosis/api/` con solo `index.php` y `.htaccess`, y `cuidadosis_app/` (src, config, `.env.example` y un `.htaccess` que niega todo) junto a `public_html`, fuera de la web. Motivo: con el paquete anterior, subir la API a una subcarpeta dejaba `src/`, `config/` y el `.env` "un nivel arriba", es decir dentro de `public_html/cuidadosis/`, accesibles por web. Para eso `index.php` define una sola variable `$appRoot` (en el repo `__DIR__ . '/..'`) y el script la cambia en la copia del paquete; si no la encuentra, falla. Sin `--subdir`, el paquete es igual que antes.
- **Supuesto:** la raíz web del hosting se llama `public_html` y se puede escribir en la carpeta de la cuenta, junto a ella. Si no es así, hay que revisarlo antes de subir (`docs/instalacion.md` 3.1.1).
- **Prueba:** paquete con `--subdir=cuidadosis`, base temporal compartida y servidor embebido de PHP (`docs/instalacion.md` 3.2): `/cuidadosis/api/` respondió, registro, login, `/me` e `/intakes/today` correctos, 401 sin token, 404 en rutas inexistentes y en todas las rutas hacia `cuidadosis_app/`. **No probado:** Apache o LiteSpeed reales en `areproyectos.com.mx`; no se ejecutó nada contra servidores externos.

### D48. Datos opcionales para la prueba física sin cable
- `bd/preparar_prueba_fisica.sql` deja a paciente1 vinculado con cuidador1 y crea cinco "Medicamento de prueba A..E (SINTETICO)", una toma cada uno a ahora + 10, 50, 90, 130 y 170 min (cada 40 min), para correr la prueba sin teclear en el teléfono. Usa `SET time_zone = '-06:00'` (México no tiene horario de verano desde 2022), igual que la API, para que sirva también en un servidor en UTC.
- El aviso previo de 5 min no se guarda en la BD (G-06): es el ajuste local del teléfono y vale 5 por defecto; el script lo recuerda y muestra las horas esperadas del aviso y de la alarma del cuidador.
- Se puede repetir: desactiva (no borra) los de la corrida anterior. Si faltan paciente1 o cuidador1 se detiene antes de cambiar nada.
- No se ejecuta automáticamente. Probado solo en una base temporal (dos corridas y sin seed); la base principal quedó con el seed.

## Fase 12 (despliegue en el hosting y versión 1.1.2, 23/11/2026)

### D49. Despliegue en hosting propio, APK release sin cambio de servidor, versión 1.1.2
- **Dónde:** API en `https://areproyectos.com.mx/cuidadosis/api/` (cPanel; nginx delante de LiteSpeed; PHP 8.2.36; MariaDB 11.4). Paquete de `deploy/build_hosting_package.php --subdir=cuidadosis`: `public_html/cuidadosis/api/` solo con `index.php` y `.htaccess`, y `/home/<cuenta>/cuidadosis_app/` (código, configuración y `.env`) fuera de la web, con la carpeta en 700 y el `.env` en 600. La raíz web real se confirmó por FTP antes de subir (`public_html`, `DOCUMENT_ROOT` del dominio).
- **`.env` de producción:** creado fuera del repo y subido directo a `cuidadosis_app/`; nunca se versiona. `DB_HOST=localhost` (la conexión interna del servidor), `APP_DEBUG=false`, `APP_BASE_PATH=/cuidadosis/api`. La zona horaria la fija la API: PHP en `America/Mexico_City` y la sesión MySQL con el desfase calculado en cada conexión (el servidor está en UTC y la respuesta de `GET /` mostró la hora de México).
- **Base de datos compartida:** se importaron `schema_hosting.sql` y `seed_hosting.sql` en la base asignada; quedaron 7 tablas `cd_*` y sus 7 llaves foráneas `fk_cd_*`, con 0 filas. `seed_hosting.sql` no crea cuentas, así que en el servidor público no hay usuarios con contraseña conocida. Las cuentas de prueba creadas durante las verificaciones se borraron al terminar.
- **Credenciales:** se leen de `.secrets/` (ignorada por git desde el commit `5d1f559`, antes de cualquier otro cambio). Nunca se escriben en comandos visibles, logs, documentos ni en el APK.
- **FTP con TLS:** el certificado del servidor FTP es para el nombre del servidor (`svgil1.cloud-mx-ns.net`), no para `ftp.areproyectos.com.mx`; con este último la verificación falla. Se usó el nombre del servidor (misma IP) con TLS y verificación completa; nunca se cayó a FTP sin cifrar ni se desactivó la verificación.
- **App, versión 1.1.2 (`versionCode` 4):** el release apunta fijo a `https://areproyectos.com.mx/cuidadosis/api/` y no permite cambiar el servidor: `BuildConfig.ALLOW_SERVER_CHANGE = false`, el login muestra la dirección sin enlace y `SettingsStore` ignora cualquier URL guardada. En debug todo sigue igual (XAMPP por `local.properties` o el emulador, y la pantalla Servidor). El release solo permite HTTPS (sin cambios en `network_security_config`).
- **Firma:** keystore nuevo (RSA 3072, validez 10000 días) en `.secrets/cuidadosis-release.jks`, con sus datos en `.secrets/release-signing.properties`, que Gradle lee si existe; sin ese archivo el release sale sin firmar. **Hay que respaldar el keystore fuera de la PC:** sin él no se pueden publicar actualizaciones que instalen encima de la 1.1.2.
- **R8 desactivado en release:** con la optimización parcial que traía el proyecto (`packageScope` de androidx, kotlin y kotlinx) el APK compilaba y las pruebas pasaban, pero la app se cerraba al abrir en el teléfono (`IllegalAccessError` en una clase de `kotlin.sequences`). Se desactivó la optimización: el release ejecuta el mismo código que debug, ya probado. El APK pasa de 2.4 MB a 8.5 MB. Activar R8 completo requeriría reglas de keep para kotlinx.serialization y Retrofit y probarlo de nuevo; queda como pendiente opcional.
- **Paso de la 1.1.1 debug a la 1.1.2 release:** la firma es distinta, así que Android no instala encima. Hay que desinstalar la versión anterior (se pierden la sesión y la copia local del teléfono; los datos del servidor no). Después de instalar se debe volver a quitar la restricción de batería en HyperOS (`docs/instalacion.md` 2.3).
- **Distribución:** `public_html/cuidadosis/descargas/CuidaDosis-1.1.2-beta.apk` con un `.htaccess` (`deploy/descargas/.htaccess`) que fija el tipo `application/vnd.android.package-archive` y desactiva el listado de la carpeta.
- **Hallazgos del servidor:**
  - Un filtro del servidor (probablemente ModSecurity) responde 403 en HTML a los POST que llegan sin `Content-Length` (por ejemplo `curl -X POST` sin datos). La app no se ve afectada: OkHttp siempre envía `Content-Length: 0` en un POST sin cuerpo; con ese encabezado la API responde normal (comprobado con el logout desde la app).
  - La protección anti-bots del hosting (Imunify360) marcó temporalmente la IP de la PC después del barrido SEG-05 de rutas sensibles seguidas: durante unos 15 minutos HTTPS devolvió una página "One moment, please..." (200, HTML) y FTP y MySQL remoto no conectaron. Se liberó sola. Un barrido así contra el hosting debe hacerse despacio (varios segundos entre rutas). Si un teléfono de la beta llegara a recibir esa página, la app mostraría un error de conexión.
  - nginx guarda en caché respuestas que no traen `Cache-Control`. La API siempre envía `Cache-Control: no-store`; se comprobó que dos cuentas distintas reciben su propio `/me` en la misma URL.
- **Pruebas:** ver `docs/evidencias/p4/hosting/DESPLIEGUE.md`.
