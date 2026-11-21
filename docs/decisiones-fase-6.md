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
