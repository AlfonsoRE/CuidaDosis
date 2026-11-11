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
| 422 | Datos inválidos (ver `details`) |
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
| POST | `/links/accept` | cuidador | Se vincula con un código |
| DELETE | `/links/active` | paciente | Desvincula a su cuidador |
| GET | `/medications` | paciente | Lista los medicamentos activos |
| POST | `/medications` | paciente | Crea un medicamento con sus horas |
| GET | `/medications/{id}` | paciente | Detalle de un medicamento |
| PUT | `/medications/{id}` | paciente | Reemplaza nombre, dosis y horas |
| DELETE | `/medications/{id}` | paciente | Desactiva el medicamento (no lo borra) |
| GET | `/intakes/today` | paciente | Tomas de hoy |
| POST | `/intakes/{id}/take` | paciente | Marca una toma de hoy como tomada |
| GET | `/caregiver/patients` | cuidador | Sus pacientes con el resumen de hoy |
| GET | `/caregiver/patients/{id}/intakes/today` | cuidador | Tomas de hoy de uno de sus pacientes |
| DELETE | `/caregiver/patients/{id}` | cuidador | Deja de seguir a ese paciente |

## Ejemplos

Usuarios sintéticos de `bd/seed.sql` (contraseña de prueba `Sintetico2026`):
`paciente1@ejemplo.test` y `paciente3@ejemplo.test` (ambos vinculados con `cuidador1@ejemplo.test`), `paciente2@ejemplo.test` (código pendiente `SINT0002`) y `cuidador2@ejemplo.test` (sin vínculo).

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
Paciente (`link.caregiver`; `pending_invite` es el código vigente o `null`):
```json
{"user":{"id":1,"full_name":"Paciente SINTETICO Uno","email":"paciente1@ejemplo.test","role":"patient","created_at":"2026-11-11 10:14:34"},"link":{"caregiver":{"id":3,"full_name":"Cuidador SINTETICO Uno"},"linked_at":"2026-11-10 11:14:34"},"pending_invite":null}
```
Sin cuidador, `link` es `null`.

Cuidador: en lugar de `link` y `pending_invite` trae la lista `patients` (vacía si no tiene pacientes):
```json
{"user":{"id":3,"full_name":"Cuidador SINTETICO Uno","email":"cuidador1@ejemplo.test","role":"caregiver","created_at":"2026-11-11 10:14:34"},"patients":[{"id":5,"full_name":"Paciente SINTETICO Tres","linked_at":"2026-11-11 09:31:25"},{"id":1,"full_name":"Paciente SINTETICO Uno","linked_at":"2026-11-10 11:14:34"}]}
```

### POST /links/invite (paciente)
Genera un código de 8 caracteres (letras y números sin I, O, 0 ni 1) que dura 48 h y sirve una sola vez. Al generar uno nuevo se revocan los pendientes anteriores.
```bash
curl -s -X POST $B/links/invite -H "Authorization: Bearer $TP"
```
`201`:
```json
{"invite_code":"EHP2V3FN","expires_at":"2026-11-13 10:15:57"}
```
Errores: `409` si el paciente ya tiene un cuidador vinculado (primero debe desvincularlo con `DELETE /links/active`).

### POST /links/accept (cuidador)
No distingue mayúsculas de minúsculas.
```bash
curl -s -X POST $B/links/accept -H "Authorization: Bearer $TC" -H 'Content-Type: application/json' \
  -d '{"invite_code":"SINT0002"}'
```
`200`:
```json
{"patient":{"id":2,"full_name":"Paciente SINTETICO Dos"},"linked_at":"2026-11-11 10:15:56"}
```
Un cuidador puede aceptar códigos de varios pacientes. Errores: `404` si el código no existe, ya se usó, se revocó o expiró; `409` si el paciente ya tiene otro cuidador activo; `422` si no tiene 8 letras o números.

### GET /medications (paciente)
```bash
curl -s $B/medications -H "Authorization: Bearer $TP"
```
```json
{"medications":[{"id":1,"name":"Medicamento SINTETICO A","dose":"1 tableta (dato de prueba)","times":["08:00","20:00"],"created_at":"2026-11-11 10:14:34","updated_at":"2026-11-11 10:14:34"}]}
```

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

### GET /intakes/today (paciente)
Crea como pendientes las tomas de hoy que todavía no existan y devuelve la lista del día ordenada por hora. Incluye las tomas de medicamentos y horarios activos, más las ya marcadas como tomadas aunque después se hayan desactivado.
```bash
curl -s $B/intakes/today -H "Authorization: Bearer $TP"
```
```json
{"date":"2026-11-11","intakes":[{"id":1,"medication_id":1,"name":"Medicamento SINTETICO A","dose":"1 tableta (dato de prueba)","time":"08:00","scheduled_for":"2026-11-11 08:00:00","status":"taken","confirmed_at":"2026-11-11 08:03:00"},{"id":2,"medication_id":2,"name":"Medicamento SINTETICO B","dose":"5 ml (dato de prueba)","time":"14:00","scheduled_for":"2026-11-11 14:00:00","status":"pending","confirmed_at":null}],"summary":{"total":3,"taken":1,"pending":2}}
```

### POST /intakes/{id}/take (paciente)
Solo para tomas propias de hoy que sigan pendientes, a cualquier hora del día. No se puede deshacer.
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
{"date":"2026-11-11","patients":[{"id":5,"full_name":"Paciente SINTETICO Tres","linked_at":"2026-11-11 09:31:25","summary":{"total":1,"taken":0,"pending":1}},{"id":1,"full_name":"Paciente SINTETICO Uno","linked_at":"2026-11-10 11:14:34","summary":{"total":3,"taken":1,"pending":2}}]}
```
Sin pacientes vinculados, `patients` es `[]`.

### GET /caregiver/patients/{id}/intakes/today (cuidador)
Tomas de hoy de un paciente vinculado, con la misma forma que `/intakes/today` más el campo `patient`.
```bash
curl -s $B/caregiver/patients/1/intakes/today -H "Authorization: Bearer $TC"
```
```json
{"patient":{"id":1,"full_name":"Paciente SINTETICO Uno"},"date":"2026-11-11","intakes":[...],"summary":{"total":3,"taken":1,"pending":2}}
```
Errores: `404` si ese paciente no tiene un vínculo activo con el cuidador.

### DELETE /caregiver/patients/{id} (cuidador)
El cuidador deja de seguir a ese paciente (el vínculo queda como `revoked`).
```bash
curl -s -X DELETE $B/caregiver/patients/5 -H "Authorization: Bearer $TC"
```
`200`: `{"message":"Paciente desvinculado."}`. Errores: `404` si ese paciente no tiene un vínculo activo con el cuidador.

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
