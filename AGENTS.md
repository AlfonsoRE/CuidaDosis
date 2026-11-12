# AGENTS.md

Guía para cualquier asistente de IA (y persona) que trabaje en este repositorio. Es la fuente única de verdad sobre reglas, alcance y convenciones. Si otro archivo contradice este, manda este.

## 1. Contexto del proyecto

CuidaDosis es un prototipo académico (Seminario de Análisis y Diseño de Sistemas) con plazo corto. Un paciente registra sus medicamentos y horarios diarios, recibe un aviso 5 minutos antes de cada toma y marca "Tomado". Un cuidador consulta el estado de las tomas de sus pacientes vinculados (puede tener varios).

Componentes:
- App Android (Kotlin, Jetpack Compose). Es una sola app para ambos roles: muestra las pantallas de paciente o de cuidador según el rol de la cuenta.
- API REST JSON en PHP.
- Base de datos MySQL (en local es MariaDB de XAMPP).

## 2. Alcance

### Alcance mínimo (solo esto)
1. Registro e inicio de sesión con roles: paciente y cuidador.
2. Vinculación paciente y cuidador con un código de invitación. El paciente o el cuidador pueden desvincularse; después el paciente puede invitar a otro cuidador.
3. Alta de medicamentos con horario diario.
4. Aviso 5 minutos antes de la toma (alarma local en Android).
5. Botón "Tomado" y vista de estado para el cuidador (lista de sus pacientes y detalle de cada uno).

### Fuera de alcance (no implementar)
- Notificaciones push.
- Reportes.
- Más de un cuidador activo por paciente (un cuidador sí puede tener varios pacientes).
- Recomendaciones de dosis o cualquier lógica clínica.

Si una tarea parece requerir algo fuera de alcance, detente y pregunta.

## 3. Reglas del proyecto

- La app NO recomienda dosis ni sustituye al médico. El nombre y la dosis del medicamento son texto libre que escribe el usuario; el sistema no los interpreta, valida clínicamente ni sugiere.
- Solo datos sintéticos de prueba, etiquetados como tales (por ejemplo, nombres con "SINTETICO" y correos `@ejemplo.test`). Nunca datos reales de salud ni de personas.
- Credenciales solo en `api/.env`, que nunca se versiona. La plantilla es `api/.env.example` (sin valores reales).
- El repo vive en `C:\xampp\htdocs\CuidaDosis` y htdocs es público. Por eso:
  - El único punto de entrada web es `api/public/`.
  - `/.htaccess` (raíz) niega el acceso a todo el repo; `api/public/.htaccess` lo vuelve a permitir solo ahí y bloquea archivos ocultos.
  - Nunca coloques `.env`, scripts SQL, credenciales ni código de `src/` o `config/` dentro de `api/public/`.
  - No borres ni debilites esos `.htaccess`.
- Seguridad básica obligatoria en la API:
  - Contraseñas con `password_hash` y `password_verify`.
  - PDO con sentencias preparadas siempre; nunca concatenar entradas en SQL.
  - Autenticación con token de sesión (se guarda solo su hash en BD, con expiración).
  - Validar y sanear todas las entradas.
  - Cada endpoint valida el rol y que el recurso pertenezca al usuario. Un cuidador solo ve a sus pacientes con vínculo activo, y pierde el acceso al desvincularse.
- Respeta la estructura del repo. El proyecto de Android Studio en `android/` ya existe: no lo regeneres ni cambies su paquete (`mx.cuidadosis.app`).

## 4. Estructura de carpetas

```
CuidaDosis/
  AGENTS.md         Fuente única de verdad para asistentes
  CLAUDE.md         Importa AGENTS.md y agrega notas de Claude Code
  README.md         Documentación para personas
  .htaccess         Niega el acceso web a todo el repo
  .gitignore
  android/          Proyecto de Android Studio (app Kotlin + Compose)
  api/              Backend PHP
    .env.example    Plantilla de configuración (api/.env no se versiona)
    public/         Único directorio expuesto por Apache (index.php, .htaccess)
    src/            Código de la API (Fase 2)
    config/         Configuración y carga del .env (Fase 2)
  bd/               Scripts SQL: schema.sql y seed.sql (Fase 1)
  docs/             Documentación técnica: api.md (Fase 2), decisiones-fase-4.md y decisiones-fase-5.md
```

## 5. Stack y versiones

| Parte | Tecnología | Versión |
|---|---|---|
| Servidor local | XAMPP: Apache | 2.4.58 |
| Backend | PHP (sin framework pesado) | 8.2.12 |
| Base de datos | MariaDB (XAMPP, compatible con MySQL) | 10.4.32 |
| Android | Android Gradle Plugin | 9.4.1 |
| Android | Gradle | 9.6.0 |
| Android | Kotlin | 2.2.10 |
| Android | Compose BOM | 2026.02.01 |
| Android | compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| Android | JDK del daemon de Gradle (toolchain) | 25 |

El SQL debe ser compatible con MariaDB 10.4 (por ejemplo, evitar funciones exclusivas de MySQL 8).

En esta máquina `php` y `mysql` no están en el PATH. Rutas:
- `C:\xampp\php\php.exe`
- `C:\xampp\mysql\bin\mysql.exe`

## 6. Cómo levantar y probar cada parte

### Base de datos
1. Inicia MySQL desde el panel de XAMPP.
2. Crea la estructura y los datos sintéticos (`schema.sql` borra y recrea las tablas):
   ```
   C:\xampp\mysql\bin\mysql.exe -u <usuario> -p --default-character-set=utf8mb4 < bd\schema.sql
   C:\xampp\mysql\bin\mysql.exe -u <usuario> -p --default-character-set=utf8mb4 cuidadosis < bd\seed.sql
   ```
3. Usuarios sintéticos y su contraseña de prueba: ver "Base de datos" en README.md.

### API
1. Inicia Apache desde el panel de XAMPP.
2. Copia `api/.env.example` a `api/.env` y llena los valores locales.
3. Comprueba que responde: `curl -i http://localhost/CuidaDosis/api/public/`
4. Comprueba el bloqueo (todas deben responder 403):
   ```
   curl -i http://localhost/CuidaDosis/api/.env
   curl -i http://localhost/CuidaDosis/.git/config
   curl -i http://localhost/CuidaDosis/bd/
   ```
5. Endpoints y ejemplos curl: `docs/api.md` (Fase 2).
6. Revisión de sintaxis: `C:\xampp\php\php.exe -l <archivo.php>`

### Android
1. Abre `android/` en Android Studio.
2. Compila desde terminal: `cd android` y luego `gradlew.bat assembleDebug`. Pruebas unitarias: `gradlew.bat testDebugUnitTest`.
   - En esta máquina Gradle puede elegir una JRE sin `jlink`. Si falla así, usa `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` y agrega `-Dorg.gradle.java.installations.auto-detect=false -Dorg.gradle.java.installations.paths=<esa ruta>`.
3. URL base de la API:
   - Emulador: `http://10.0.2.2/CuidaDosis/api/public/` (valor por defecto).
   - Teléfono físico en la misma red: `http://<IP de la PC>/CuidaDosis/api/public/`
   - Por defecto al compilar: `api.baseUrl` en `android/local.properties`. En la app: enlace "Servidor" del login.
4. El tráfico HTTP sin cifrar solo se permite en debug (network security config en `src/debug`).
5. Android 17 (API 37) exige el permiso de ejecución `ACCESS_LOCAL_NETWORK` para llegar a IPs privadas; sin él la conexión a XAMPP da timeout. La app lo pide al arrancar.
6. Emulador disponible en esta máquina: AVD `Medium_Phone_API_37.0`.

## 7. Convenciones de código

- Identificadores (variables, funciones, clases, tablas, columnas, rutas de la API) en inglés.
- Textos de la interfaz y comentarios breves en español.
- No uses guiones largos ni medios en textos de la interfaz ni en la documentación; usa comas, dos puntos o paréntesis.
- PHP: `declare(strict_types=1);`, PSR-12, respuestas JSON con códigos HTTP correctos y mensajes de error en español.
- SQL: utf8mb4, InnoDB, nombres en `snake_case` plural para tablas, llaves foráneas e índices explícitos.
- XAMPP trae MariaDB sin modo estricto: la conexión PDO debe activar `sql_mode` con `STRICT_ALL_TABLES` para que valores inválidos (por ejemplo, un ENUM desconocido) den error en lugar de guardarse truncados.
- Fechas en hora local `America/Mexico_City`, tanto en PHP como en la sesión de MySQL.
- Reglas de datos: un cuidador activo por paciente, un cuidador puede tener varios pacientes, los vínculos se revocan (`status = 'revoked'`, `revoked_at`) en lugar de borrarse, código de invitación de 8 caracteres con 48 h de vigencia y un solo uso, varias horas diarias por medicamento, tomas del día creadas como pendientes al consultar el día (`INSERT IGNORE`), medicamentos y horarios se desactivan en lugar de borrarse.
- Kotlin: estilo oficial, arquitectura simple (pantalla Compose, ViewModel, repositorio, Retrofit). Inyección manual con `AppContainer`, JSON con kotlinx.serialization, navegación con navigation-compose y rutas `@Serializable`. El token se guarda cifrado con Android Keystore (`SessionStore`) y no se respalda. Paquetes de UI: `ui/auth`, `ui/patient` (pestañas Hoy, Medicamentos y Cuidador), `ui/medications`, `ui/caregiver` y `ui/common`.
- Avisos locales en el paquete `reminders`: una sola alarma exacta pendiente (la del próximo aviso), calculada en hora de México con la copia local cifrada de los medicamentos; solo para el rol paciente y se cancela al cerrar sesión o al expirar el token.
- Las horas que se comparan con "ahora" en la app usan `America/Mexico_City`, igual que la API, sin importar la zona del teléfono.
- Mantén el código simple; es un prototipo. No agregues dependencias sin justificarlo.

## 8. Convenciones de commits y flujo de trabajo

- Conventional Commits: tipo en inglés y descripción en español, en imperativo y breve.
  - Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `db`.
  - Ejemplo: `feat(api): agrega endpoint de confirmación de toma`.
- El trabajo se hace por fases. Al terminar cada fase: explicar qué se hizo, cómo probarlo, actualizar la sección "Estado del proyecto" del README y hacer un commit. No se avanza a la siguiente fase sin aprobación del equipo.
- Las decisiones de diseño importantes se consultan antes de implementarse.
- No inventes resultados de pruebas. Si algo no se pudo probar, dilo explícitamente.

## 9. Nunca hacer

- Usar o guardar datos reales de salud o de personas.
- Subir credenciales, `.env`, tokens o contraseñas al repo (ni en código, ni en documentación, ni en ejemplos).
- Recomendar, calcular, validar o sugerir dosis, o presentar la app como sustituto del médico.
- Exponer por web algo fuera de `api/public/` o debilitar los `.htaccess`.
- Concatenar entradas del usuario en consultas SQL.
- Regenerar el proyecto de Android o agregar funciones fuera del alcance mínimo.
- Reportar como probado algo que no se ejecutó.
