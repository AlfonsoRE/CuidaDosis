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
