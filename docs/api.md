# API de CuidaDosis

API REST JSON en PHP 8.2 (sin framework). El único punto de entrada es `api/public/index.php`.

- **URL base local:** `http://localhost/CuidaDosis/api/public`
- **Desde el emulador de Android:** `http://10.0.2.2/CuidaDosis/api/public`
- **Formato:** peticiones y respuestas en JSON (`Content-Type: application/json`), UTF-8.
- **Fechas:** hora local `America/Mexico_City`, formato `YYYY-MM-DD HH:MM:SS`. Las horas de los medicamentos van en formato `HH:MM` (24 h).

> La API no interpreta, valida clínicamente ni recomienda dosis. El nombre y la dosis del medicamento son texto libre que escribe el usuario. "Tomado" solo registra que el usuario tocó el botón.

## Autenticación

1. `POST /auth/register` o `POST /auth/login` devuelve un `token` (64 caracteres hexadecimales) y su `expires_at`.
2. En las demás peticiones se envía el encabezado `Authorization: Bearer <token>`.
3. El token dura `TOKEN_TTL_HOURS` horas (720 por defecto). En la base de datos solo se guarda su hash SHA-256.
4. `POST /auth/logout` invalida el token actual.

Roles: `patient` (paciente) y `caregiver` (cuidador). Cada endpoint revisa el rol y que el recurso sea del usuario. Un paciente tiene como máximo un cuidador activo; un cuidador puede tener varios pacientes y solo ve a los que tienen vínculo activo con él.

## Errores

Todas las respuestas de error tienen esta forma:
```json
{ "error": "Mensaje en español", "details": { "campo": "Motivo" } }
```
`details` solo aparece en errores de validación (422), o en errores 500 cuando `APP_DEBUG=true`.

| Código | Cuándo |
|---|---|
| 400 | El cuerpo no es JSON válido o no es un objeto |
| 401 | Falta el token, no es válido o expiró; o el login falló |
| 403 | El rol del usuario no puede usar ese endpoint |
| 404 | La ruta no existe, o el recurso no existe o no es del usuario |
| 405 | Método HTTP no permitido en esa ruta |
| 409 | Conflicto: correo ya registrado, el paciente ya tiene cuidador, toma ya confirmada |
| 410 | El código de invitación existe pero ya venció (10 min) |
| 422 | Datos inválidos (ver `details`) |
| 429 | Demasiados intentos fallidos de código de invitación (5 en 10 min); trae `Retry-After` en segundos |
| 500 | Error interno |

## Resumen de endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/` | público | Estado de la API |
| POST | `/auth/register` | público | Crea una cuenta y devuelve un token |
| POST | `/auth/login` | público | Inicia sesión y devuelve un token |
| POST | `/auth/logout` | cualquiera | Cierra la sesión actual |
| GET | `/me` | cualquiera | Datos del usuario y su vínculo |
| POST | `/links/invite` | paciente | Genera un código de invitación |
| POST | `/links/accept` | cuidador | Usa un código; la solicitud queda esperando aceptación |
| GET | `/links/pending` | paciente | Solicitud de vínculo que espera su aceptación |
| POST | `/links/{id}/confirm` | paciente | Acepta la solicitud: el vínculo pasa a activo |
| POST | `/links/{id}/reject` | paciente | Rechaza la solicitud |
| DELETE | `/links/active` | paciente | Desvincula a su cuidador |
| GET | `/medications` | paciente | Lista los medicamentos activos |
| POST | `/medications` | paciente | Crea un medicamento con sus horas |
| GET | `/medications/{id}` | paciente | Detalle de un medicamento |
| PUT | `/medications/{id}` | paciente | Reemplaza nombre, dosis y horas |
| DELETE | `/medications/{id}` | paciente | Desactiva el medicamento (no lo borra) |
| GET | `/intakes/today` | paciente | Tomas de hoy |
| POST | `/intakes/sync` | paciente | Sincroniza en lote confirmaciones con UUID (idempotente) |
| GET | `/intakes/history` | paciente | Tomas de los últimos 7 días |
| POST | `/intakes/{id}/take` | paciente | Confirma una toma de hoy (compatibilidad) |
| GET | `/caregiver/patients` | cuidador | Sus pacientes con el resumen de hoy |
| GET | `/caregiver/patients/{id}/intakes/today` | cuidador | Tomas de hoy de uno de sus pacientes |
| GET | `/caregiver/patients/{id}/intakes/history` | cuidador | Tomas de 7 días de uno de sus pacientes |
| DELETE | `/caregiver/patients/{id}` | cuidador | Deja de seguir a ese paciente o retira su solicitud |

## Ejemplos

Usuarios sintéticos de `bd/seed.sql` (contraseña de prueba `Sintetico2026`):
`paciente1@ejemplo.test` (no comparte la dosis) y `paciente3@ejemplo.test` (sí la comparte), ambos vinculados con `cuidador1@ejemplo.test`; `paciente2@ejemplo.test` (código pendiente `SINT0002`, vence 10 min después de cargar el seed) y `cuidador2@ejemplo.test` (sin vínculo).

Los ejemplos usan sintaxis de Git Bash. En `cmd` de Windows cambia las comillas simples por dobles y escapa las internas (`\"`).

```bash
B=http://localhost/CuidaDosis/api/public
```

### GET /
```bash
curl -s $B/
```
```json
{"status":"ok","service":"CuidaDosis API","time":"2026-11-11 10:15:34"}
```

### POST /auth/register
Campos: `full_name` (1 a 100), `email` (se guarda en minúsculas), `password` (8 a 72) y `role` (`patient` o `caregiver`).
```bash
curl -s -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"full_name":"Paciente SINTETICO Tres","email":"paciente3@ejemplo.test","password":"Sintetico2026","role":"patient"}'
```
`201`:
```json
{"user":{"id":5,"full_name":"Paciente SINTETICO Tres","email":"paciente3@ejemplo.test","role":"patient","created_at":"2026-11-11 10:15:36"},"token":"d8df…b2d6","expires_at":"2026-12-11 10:15:36"}
```
Errores: `409` si el correo ya existe; `422` con detalle por campo.

### POST /auth/login
```bash
curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"paciente1@ejemplo.test","password":"Sintetico2026"}'
```
`200`: misma forma que el registro. Si falla el correo o la contraseña: `401` con `"Correo o contraseña incorrectos."`.

Guarda el token para los siguientes ejemplos:
```bash
TP=<token del paciente>
TC=<token del cuidador>
```

### POST /auth/logout
```bash
curl -s -X POST $B/auth/logout -H "Authorization: Bearer $TP"
```
`200`: `{"message":"Sesión cerrada."}`

### GET /me
```bash
curl -s $B/me -H "Authorization: Bearer $TP"
```
Paciente (`link.caregiver`; `pending_invite` es el código vigente o `null`; `pending_request` es la solicitud de un cuidador que espera su aceptación o `null`):
```json
{"user":{"id":1,"full_name":"Paciente SINTETICO Uno","email":"paciente1@ejemplo.test","role":"patient","created_at":"2026-11-11 10:14:34"},"link":{"caregiver":{"id":3,"full_name":"Cuidador SINTETICO Uno"},"linked_at":"2026-11-10 11:14:34"},"pending_invite":null,"pending_request":null}
```
Sin cuidador, `link` es `null`.

Cuidador: en lugar de `link`, `pending_invite` y `pending_request` trae la lista `patients` (vacía si no tiene pacientes) y la lista `awaiting` con sus solicitudes sin aceptar:
```json
{"user":{"id":4,"full_name":"Cuidador SINTETICO Dos","email":"cuidador2@ejemplo.test","role":"caregiver","created_at":"2026-11-21 12:01:39"},"patients":[],"awaiting":[{"id":4,"requested_at":"2026-11-21 12:08:09","patient":{"id":2,"full_name":"Paciente SINTETICO Dos"}}]}
```

### POST /links/invite (paciente)
El vínculo pasa por estos estados (ADR-004): `pending` (código generado) → `awaiting_acceptance` (el cuidador usó el código) → `active` (el paciente aceptó). Rechazar o revocar lo deja en `revoked` con `revoked_at`; nunca se borra.

Genera un código de 8 caracteres (letras y números sin I, O, 0 ni 1) que dura **10 min** y sirve una sola vez. Al generar uno nuevo se revocan los códigos pendientes y las solicitudes sin aceptar.
```bash
curl -s -X POST $B/links/invite -H "Authorization: Bearer $TP"
```
`201`:
```json
{"invite_code":"5MYRZ938","expires_at":"2026-11-21 12:18:09"}
```
Errores: `409` si el paciente ya tiene un cuidador vinculado (primero debe desvincularlo con `DELETE /links/active`).

### POST /links/accept (cuidador)
No distingue mayúsculas de minúsculas. **No activa el vínculo**: lo deja esperando la aceptación del paciente. El cuidador todavía no ve datos del paciente.
```bash
curl -s -X POST $B/links/accept -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' \
  -d '{"invite_code":"5MYRZ938"}'
```
`200`:
```json
{"link":{"id":4,"status":"awaiting_acceptance","requested_at":"2026-11-21 12:08:09"},"patient":{"id":2,"full_name":"Paciente SINTETICO Dos"},"message":"Solicitud enviada. Espera a que el paciente la acepte."}
```
Cada uso se registra en `link_attempts`. Un cuidador puede usar códigos de varios pacientes. Errores:
- `404` si el código no existe, ya se usó o se revocó.
- `410` si el código existe pero venció: `"El código venció. Pide al paciente que genere uno nuevo."`
- `422` si no tiene 8 letras o números.
- `429` tras 5 intentos fallidos (cualquiera de los anteriores) en 10 min. Trae `Retry-After` con los segundos que faltan. Mientras dura el bloqueo no se registran intentos nuevos y tampoco se acepta un código válido.

### GET /links/pending (paciente)
```bash
curl -s $B/links/pending -H "Authorization: Bearer $TP"
```
`200` (`request` es `null` si no hay solicitud):
```json
{"request":{"id":4,"requested_at":"2026-11-21 12:08:09","caregiver":{"id":4,"full_name":"Cuidador SINTETICO Dos"}}}
```

### POST /links/{id}/confirm (paciente)
Acepta la solicitud `{id}`. Desde ese momento el cuidador ve las tomas del paciente.
```bash
curl -s -X POST $B/links/4/confirm -H "Authorization: Bearer $TP"
```
`200`:
```json
{"link":{"id":4,"status":"active","linked_at":"2026-11-21 12:08:10"},"caregiver":{"id":4,"full_name":"Cuidador SINTETICO Dos"}}
```
Errores: `404` si `{id}` no es una solicitud del paciente en `awaiting_acceptance`; `409` si ya tiene un cuidador activo.

### POST /links/{id}/reject (paciente)
Rechaza la solicitud `{id}` (queda `revoked`). El paciente puede generar otro código.
```bash
curl -s -X POST $B/links/4/reject -H "Authorization: Bearer $TP"
```
`200`: `{"message":"Solicitud rechazada."}`. Errores: `404` igual que `confirm`.

### GET /medications (paciente)
```bash
curl -s $B/medications -H "Authorization: Bearer $TP"
```
```json
{"medications":[{"id":1,"name":"Medicamento SINTETICO A","dose":"1 tableta (dato de prueba)","times":["08:00","20:00"],"schedules":[{"id":1,"time":"08:00"},{"id":2,"time":"20:00"}],"created_at":"2026-11-21 12:01:39","updated_at":"2026-11-21 12:01:39"}]}
```
`schedules` trae el identificador de cada hora; la app lo guarda para confirmar tomas sin red con `/intakes/sync`. `times` se conserva por compatibilidad. Lo mismo aplica a las respuestas de alta, detalle y edición.

### POST /medications (paciente)
Campos: `name` y `dose` (texto libre, de 1 a 100) y `times` (de 1 a 10 horas `HH:MM`, sin repetir).
```bash
curl -s -X POST $B/medications -H "Authorization: Bearer $TP" -H 'Content-Type: application/json' \
  -d '{"name":"Medicamento SINTETICO E","dose":"1 tableta (dato de prueba)","times":["21:00","07:30","13:00"]}'
```
`201` (las horas se devuelven ordenadas):
```json
{"medication":{"id":5,"name":"Medicamento SINTETICO E","dose":"1 tableta (dato de prueba)","times":["07:30","13:00","21:00"],"created_at":"2026-11-11 10:15:47","updated_at":"2026-11-11 10:15:47"}}
```

### GET /medications/{id} (paciente)
```bash
curl -s $B/medications/5 -H "Authorization: Bearer $TP"
```
`200` con `{"medication":{...}}`. `404` si no existe, está desactivado o es de otro paciente.

### PUT /medications/{id} (paciente)
Mismos campos que el alta. `times` es la lista **completa**: las horas que no vienen se desactivan, las nuevas se crean y las que vuelven se reactivan.
```bash
curl -s -X PUT $B/medications/5 -H "Authorization: Bearer $TP" -H 'Content-Type: application/json' \
  -d '{"name":"Medicamento SINTETICO E2","dose":"2 tabletas (dato de prueba)","times":["07:30","18:00","21:00"]}'
```
`200` con el medicamento actualizado.

### DELETE /medications/{id} (paciente)
Desactiva el medicamento y sus horarios. No borra nada, así se conserva el historial de tomas.
```bash
curl -s -X DELETE $B/medications/6 -H "Authorization: Bearer $TP"
```
`200`: `{"message":"Medicamento desactivado."}`

### Estado de una toma (ADR-005)
Cada toma trae `status` (lo guardado: `pending` o `taken`) y `state` (lo que se muestra, calculado en cada consulta con la hora del servidor):

| `state` | Etiqueta en la app | Regla |
|---|---|---|
| `pending` | Pendiente | `status = pending` y ahora ≤ `scheduled_for` + 30 min |
| `confirmed` | Confirmada por el paciente | `status = taken` |
| `unconfirmed` | Sin confirmar | `status = pending` y ahora > `scheduled_for` + 30 min |

"Sin confirmar" **no se guarda**. "Confirmada por el paciente" solo significa que el paciente tocó "Tomado"; no verifica la ingesta. Las respuestas traen `now` (hora del servidor) para que la app calcule igual. `summary` trae `total`, `confirmed`, `unconfirmed` y, por compatibilidad, `taken` (= `confirmed`) y `pending` (todo lo no confirmado).

### GET /intakes/today (paciente)
Crea como pendientes las tomas de hoy que todavía no existan y devuelve la lista del día ordenada por hora. Incluye las tomas de medicamentos y horarios activos, más las ya confirmadas aunque después se hayan desactivado.
```bash
curl -s $B/intakes/today -H "Authorization: Bearer $TP"
```
```json
{"date":"2026-11-21","now":"2026-11-21 12:11:45","intakes":[{"id":1,"schedule_id":1,"medication_id":1,"name":"Medicamento SINTETICO A","dose":"1 tableta (dato de prueba)","time":"08:00","scheduled_for":"2026-11-21 08:00:00","status":"taken","state":"confirmed","confirmed_at":"2026-11-21 08:03:00","received_at":"2026-11-21 08:04:00"},{"id":3,"schedule_id":2,"medication_id":1,"name":"Medicamento SINTETICO A","dose":"1 tableta (dato de prueba)","time":"20:00","scheduled_for":"2026-11-21 20:00:00","status":"pending","state":"pending","confirmed_at":null,"received_at":null}],"summary":{"total":2,"taken":1,"pending":1,"confirmed":1,"unconfirmed":0}}
```

### POST /intakes/sync (paciente)
Envía en lote las confirmaciones hechas en el teléfono, incluso sin red al momento del toque (RF-13; ADR-003). Cada una lleva un `client_uuid` generado en el teléfono. Es **idempotente**: reenviar el mismo lote no duplica nada ni da error. Actualiza `users.last_sync_at` (RF-12) aunque el lote venga vacío, así que también sirve para avisar que el teléfono tiene conexión.

Cada confirmación: `client_uuid` (UUID), `schedule_id` (entero), `scheduled_for` y `confirmed_at` (`YYYY-MM-DD HH:MM:SS`, hora de México). Máximo 100 por envío.
```bash
curl -s -X POST $B/intakes/sync -H "Authorization: Bearer $TP" -H 'Content-Type: application/json' \
  -d '{"confirmations":[{"client_uuid":"11111111-2222-4333-8444-555555555555","schedule_id":3,"scheduled_for":"2026-11-21 14:00:00","confirmed_at":"2026-11-21 12:11:44"}]}'
```
`200`, con un resultado por confirmación en el mismo orden:
```json
{"results":[{"client_uuid":"11111111-2222-4333-8444-555555555555","status":"created","intake_id":2}],"last_sync_at":"2026-11-21 12:11:44"}
```
| `status` | Significado | ¿La app la borra de su cola? |
|---|---|---|
| `created` | Se guardó la confirmación | Sí |
| `duplicate` | Ese `client_uuid` ya se había recibido | Sí |
| `already_confirmed` | La toma ya estaba confirmada por otro envío; se conserva la primera | Sí |
| `rejected` | Datos inválidos (`error` dice por qué): UUID mal formado, horario ajeno, hora que no coincide con el horario, fecha fuera de los últimos 7 días o `confirmed_at` en el futuro (más de 5 min) | Sí, no tiene caso reintentar |

Errores del lote completo: `422` si `confirmations` no es una lista o trae más de 100; `403` para el cuidador. La app debe reintentar solo ante errores de red o `5xx`.

### GET /intakes/history (paciente)
Tomas de los últimos 7 días, hoy incluido, agrupadas por día del más reciente al más antiguo (RF-15). Los días que nadie consultó se rellenan como pendientes (y por lo tanto "Sin confirmar"), solo para horarios activos que ya existían a esa hora.
```bash
curl -s $B/intakes/history -H "Authorization: Bearer $TP"
```
```json
{"from":"2026-11-15","to":"2026-11-21","now":"2026-11-21 12:12:00","days":[{"date":"2026-11-21","intakes":[...],"summary":{...}},{"date":"2026-11-20","intakes":[...],"summary":{"total":3,"taken":2,"pending":1,"confirmed":2,"unconfirmed":1}}]}
```

### POST /intakes/{id}/take (paciente)
Se conserva por compatibilidad; la app usa `/intakes/sync`. Solo para tomas propias de hoy que sigan pendientes, a cualquier hora del día. No se puede deshacer. También actualiza `last_sync_at`.
```bash
curl -s -X POST $B/intakes/2/take -H "Authorization: Bearer $TP"
```
`200`:
```json
{"intake":{"id":2,"medication_id":2,"name":"Medicamento SINTETICO B","dose":"5 ml (dato de prueba)","time":"14:00","scheduled_for":"2026-11-11 14:00:00","status":"taken","confirmed_at":"2026-11-11 10:15:53"}}
```
Errores: `404` si no es una toma de hoy del paciente (o su medicamento está desactivado); `409` si ya estaba tomada.

### DELETE /links/active (paciente)
Desvincula al cuidador actual. El vínculo no se borra: queda como `revoked` con `revoked_at`, y el cuidador pierde el acceso en ese momento. Después el paciente puede generar un código nuevo para otro cuidador.
```bash
curl -s -X DELETE $B/links/active -H "Authorization: Bearer $TP"
```
`200`: `{"message":"Cuidador desvinculado."}`. Errores: `404` si no tiene cuidador vinculado.

### GET /caregiver/patients (cuidador)
Lista los pacientes vinculados (un cuidador puede tener varios), con el resumen de las tomas de hoy de cada uno. Crea como pendientes las tomas de hoy que falten, igual que `/intakes/today`.
```bash
curl -s $B/caregiver/patients -H "Authorization: Bearer $TC"
```
```json
{"date":"2026-11-21","now":"2026-11-21 12:11:45","patients":[{"id":5,"full_name":"Paciente SINTETICO Tres","linked_at":"2026-11-21 10:07:39","last_sync_at":null,"summary":{"total":1,"taken":0,"pending":1,"confirmed":0,"unconfirmed":0}},{"id":1,"full_name":"Paciente SINTETICO Uno","linked_at":"2026-11-20 12:06:39","last_sync_at":"2026-11-21 12:11:45","summary":{"total":3,"taken":2,"pending":1,"confirmed":2,"unconfirmed":0}}]}
```
Sin pacientes vinculados, `patients` es `[]`. `last_sync_at` es la última vez que el teléfono del paciente sincronizó (`null` si nunca); con él el cuidador distingue una toma sin confirmar de un teléfono sin conexión (RF-12).

### GET /caregiver/patients/{id}/intakes/today (cuidador)
Tomas de hoy de un paciente vinculado, con la misma forma que `/intakes/today` más el campo `patient`. La dosis (`dose`) **no se envía** salvo que el paciente la comparta (`patient.share_dose`, G-07).
```bash
curl -s $B/caregiver/patients/1/intakes/today -H "Authorization: Bearer $TC"
```
```json
{"patient":{"id":1,"full_name":"Paciente SINTETICO Uno","share_dose":false,"last_sync_at":"2026-11-21 12:11:45"},"date":"2026-11-21","now":"2026-11-21 12:11:45","intakes":[...],"summary":{"total":3,"taken":2,"pending":1,"confirmed":2,"unconfirmed":0}}
```
Errores: `404` si ese paciente no tiene un vínculo activo con el cuidador.

### GET /caregiver/patients/{id}/intakes/history (cuidador)
Igual que `/intakes/history`, con el campo `patient` y sin `dose` salvo `share_dose`. `404` sin vínculo activo.

### DELETE /caregiver/patients/{id} (cuidador)
El cuidador deja de seguir a ese paciente, o retira su solicitud si el paciente no la ha aceptado (el vínculo queda como `revoked`).
```bash
curl -s -X DELETE $B/caregiver/patients/5 -H "Authorization: Bearer $TC"
```
`200`: `{"message":"Paciente desvinculado."}`. Errores: `404` si ese paciente no tiene un vínculo activo ni una solicitud pendiente con el cuidador.

## Estructura del código

```
api/
  config/config.php           Carga api/.env y valores por defecto
  public/index.php            Punto de entrada y tabla de rutas
  src/Env.php                 Lector de .env
  src/Database.php            Conexión PDO (sql_mode estricto, zona horaria)
  src/Http.php                Request y Response
  src/Router.php              Enrutador
  src/Validator.php           Validación de entradas
  src/Auth.php                Tokens y control de rol
  src/ApiException.php        Errores con código HTTP
  src/Controllers/            Auth, Link, Medication e Intake
```
