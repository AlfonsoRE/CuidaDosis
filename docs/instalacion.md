# Instalación de CuidaDosis

Guía reproducible para levantar el prototipo. Los comandos de la parte local se ejecutaron en la verificación del 21/11/2026, en Windows 11 con XAMPP (Apache 2.4.58, PHP 8.2.12, MariaDB 10.4.32) y Android Studio. El paquete de hosting se armó y probó en la PC; subirlo a un hosting real no se ejecutó y se marca como **pendiente de verificar**.

> Solo datos sintéticos (nombres con "SINTETICO" y correos `@ejemplo.test`). La app no recomienda dosis ni sustituye al médico.

## 1. Instalación local con XAMPP

### 1.1 Requisitos
- XAMPP con Apache y MySQL (MariaDB).
- Android Studio (incluye el JDK `jbr` y el SDK con emulador).
- Git.

### 1.2 Código
1. Clona el repositorio en `C:\xampp\htdocs\CuidaDosis` (htdocs es público).
2. No borres `/.htaccess` ni `api/public/.htaccess`: bloquean todo el repo por web, excepto `api/public/`.

### 1.3 Base de datos
1. Inicia **Apache** y **MySQL** en el panel de XAMPP.
2. Crea `api/.env` a partir de `api/.env.example` y llena `DB_USER` y `DB_PASS` (ver [configuracion.md](configuracion.md)).
3. Carga el esquema y los datos sintéticos. **`schema.sql` borra y recrea las tablas.** Desde Git Bash, en la raíz del repo, leyendo las credenciales de `api/.env` (así no se escriben en la terminal):
   ```bash
   set -a && . ./api/.env && set +a && export MYSQL_PWD="$DB_PASS"
   /c/xampp/mysql/bin/mysql.exe -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4 < bd/schema.sql
   /c/xampp/mysql/bin/mysql.exe -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4 "$DB_NAME" < bd/seed.sql
   ```
   También se pueden importar desde phpMyAdmin, primero `schema.sql` y luego `seed.sql`.
4. Usuarios del seed: ver la tabla del [README](../README.md). Contraseña de prueba: `Sintetico2026`.

### 1.4 Comprobar la API
```bash
curl -s -i http://localhost/CuidaDosis/api/public/
```
Resultado obtenido: `HTTP/1.1 200 OK` y `{"status":"ok","service":"CuidaDosis API",...}`.

Comprobar el bloqueo (todas deben responder `403`):
```bash
for u in api/.env .git/config bd/ api/tests/run_api_tests.php docs/ android/local.properties; do
  echo "$(curl -s -o /dev/null -w '%{http_code}' http://localhost/CuidaDosis/$u) $u"
done
```
Resultado obtenido: `403` en los seis.

### 1.5 Pruebas automáticas de la API
```bash
C:/xampp/php/php.exe api/tests/run_api_tests.php
```
Crea y borra sus propios usuarios sintéticos; no modifica el seed. Resultado: 27 OK, 0 FALLO. El log queda en `docs/evidencias/p4/api-AAAAMMDD-HHMM.log`.

Tiempo de respuesta de la consulta del día (al menos 30 repeticiones; requiere el seed cargado):
```bash
C:/xampp/php/php.exe api/tests/measure_today.php 50
```
Escribe `docs/evidencias/p4/rendimiento.csv`.

Rutas de la API dentro de una subcarpeta (sin red ni BD; comprueba XAMPP, `/cuidadosis/api` y `APP_BASE_PATH`):
```bash
C:/xampp/php/php.exe api/tests/route_path_test.php
```
Resultado del 23/11/2026: 13 OK, 0 FALLO.

### 1.6 Tablas con prefijo `cd_` y datos opcionales para la prueba física
Desde el 23/11/2026 todas las tablas llevan el prefijo `cd_` (`cd_users`, `cd_links`, `cd_medications`, `cd_schedules`, `cd_intake_logs`, `cd_api_tokens`, `cd_link_attempts`), igual que sus llaves foráneas, restricciones e índices (`fk_cd_links_patient`). Motivo: en el hosting la base se comparte con otros proyectos (D46).

**Si tu base local es anterior** (tablas sin prefijo), `schema.sql` ya no las borra: crea las `cd_*` junto a ellas. Para quitarlas, primero respalda y luego bórralas a mano, solo si esa base es exclusiva de CuidaDosis:
```bash
set -a && . ./api/.env && set +a && export MYSQL_PWD="$DB_PASS"
M="/c/xampp/mysql/bin/mysql.exe -h $DB_HOST -P $DB_PORT -u $DB_USER --default-character-set=utf8mb4"
mkdir -p /c/xampp/backup_cuidadosis
/c/xampp/mysql/bin/mysqldump.exe -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4 --single-transaction --databases "$DB_NAME" > /c/xampp/backup_cuidadosis/cuidadosis_$(date +%Y%m%d-%H%M%S).sql
$M "$DB_NAME" -e "SET FOREIGN_KEY_CHECKS=0; DROP TABLE IF EXISTS link_attempts, api_tokens, intake_logs, schedules, medications, links, users; SET FOREIGN_KEY_CHECKS=1;"
$M < bd/schema.sql
$M "$DB_NAME" < bd/seed.sql
```
El respaldo queda fuera del repo (`C:\xampp\backup_cuidadosis\`) y no se versiona. Comprueba que el archivo no está vacío y termina con `-- Dump completed` antes del `DROP`.

**Datos para la prueba física sin cable (opcional, no se ejecuta solo).** `bd/preparar_prueba_fisica.sql` deja a paciente1 vinculado con cuidador1 y le crea cinco medicamentos "Medicamento de prueba A..E (SINTETICO)" con una toma cada uno, a ahora + 10, 50, 90, 130 y 170 minutos. Desactiva (no borra) los de una corrida anterior, así que se puede repetir. Al final muestra la hora de cada toma, de su aviso previo con L = 5 y de la alarma del cuidador (T + 30). El aviso previo no está en la BD: es el ajuste local del teléfono (5 min por defecto). Requiere el seed cargado:
```bash
set -a && . ./api/.env && set +a && export MYSQL_PWD="$DB_PASS"
/c/xampp/mysql/bin/mysql.exe -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4 -t "$DB_NAME" < bd/preparar_prueba_fisica.sql
```
Después inicia sesión en el teléfono como paciente1 (o actualiza "Hoy") para que la app descargue las horas y programe los avisos. Para volver al estado limpio, recarga `schema.sql` y `seed.sql`. Probado el 23/11/2026 solo en una base temporal (dos corridas seguidas y sin seed, donde se detiene con "FALTA paciente1 o cuidador1"); no se ejecutó en la base principal.

## 2. App Android

### 2.1 Compilar el APK debug
Desde Git Bash, en `android/`. Se usa el JDK de Android Studio, porque en algunas máquinas Gradle elige una JRE sin `jlink`; si un daemon anterior quedó con otra JRE, se detiene primero:
```bash
cd android
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew.bat --stop
./gradlew.bat assembleDebug testDebugUnitTest -Dorg.gradle.java.installations.auto-detect=false "-Dorg.gradle.java.installations.paths=C:/Program Files/Android/Android Studio/jbr"
```
- APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
- Pruebas unitarias: 79, todas en verde (última corrida: 23/11/2026) (`android/app/build/test-results/testDebugUnitTest/`).
- El APK no se versiona. Para comprobar que dos personas tienen el mismo archivo: `sha256sum android/app/build/outputs/apk/debug/app-debug.apk`.

En Android Studio basta con abrir `android/` y usar Run.

### 2.2 Instalar en el emulador
Emulador usado: AVD `Medium_Phone_API_37.0` (Android 17).
```bash
"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Medium_Phone_API_37.0 -no-snapshot-save -no-boot-anim
"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" install -r "C:/xampp/htdocs/CuidaDosis/android/app/build/outputs/apk/debug/app-debug.apk"
```
Resultado: `Success`. Al abrir la app:
1. Acepta el permiso de **dispositivos cercanos o red local**; sin él, Android 17 bloquea la conexión a XAMPP.
2. Acepta el permiso de **notificaciones**; sin él no hay avisos. Si falta algún permiso, la app lo indica con una tarjeta.

La URL por defecto en el emulador es `http://10.0.2.2/CuidaDosis/api/public/`.

### 2.3 Teléfono físico (verificado el 21/11/2026 en un POCO M6 Pro, Android 16, HyperOS)
1. En el teléfono activa **Opciones de desarrollador** y **Depuración USB**, conéctalo por USB y acepta la huella de la PC.
2. Instala el APK. Si hay más de un dispositivo conectado (por ejemplo, el emulador), indica el teléfono con `-s` y el ID que muestra `adb devices -l`:
   ```bash
   "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices -l
   "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s <ID del teléfono> install -r "C:/xampp/htdocs/CuidaDosis/android/app/build/outputs/apk/debug/app-debug.apk"
   ```
   Resultado en la verificación: `Success`. En algunos Xiaomi hay que activar antes **Instalar vía USB** en las opciones de desarrollador; si no, falla con `INSTALL_FAILED_USER_RESTRICTED`.
3. Teléfono y PC en la misma red Wi-Fi. Obtén la IP de la PC con `ipconfig` (adaptador Wi-Fi). En la app, en la pantalla de inicio de sesión, toca **Servidor**, escribe `http://<IP de la PC>/CuidaDosis/api/public/`, toca **Probar conexión** (debe decir "Conexión correcta con la API.") y **Guardar**. Si la IP de la PC cambia, repite este paso.
4. Si no conecta, permite Apache en el firewall de Windows (en una terminal como administrador):
   ```
   netsh advfirewall firewall add rule name="Apache XAMPP CuidaDosis (TCP 80)" dir=in action=allow protocol=TCP localport=80 program="C:\xampp\apache\bin\httpd.exe" profile=private,public remoteip=localsubnet
   ```
   En la verificación no hizo falta: el teléfono llegó a la API sin agregar la regla.
5. El APK debug permite HTTP sin cifrar solo para pruebas; la versión release exige HTTPS.

#### Configuración especial de Xiaomi / HyperOS (necesaria para que los avisos suenen a tiempo)
HyperOS cierra o retrasa las apps en segundo plano. En la verificación se usó esta configuración, que es condición de las pruebas de puntualidad:
1. **Notificaciones:** acepta "Permitir" cuando la app lo pida.
2. **Batería sin restricciones:** Ajustes → Aplicaciones → CuidaDosis (Info. de la aplicación) → Ahorro de batería → **Sin restricciones**.
3. **Inicio automático:** app Seguridad → Autoencendido en segundo plano → activa **CuidaDosis**. Sin esto, la app puede no reprogramar los avisos al reiniciar el teléfono.
4. Las alarmas exactas (`USE_EXACT_ALARM`) se conceden al instalar; no hay que activarlas.
5. **v1.1, alarma de la toma:** permite "Mostrar en pantalla de bloqueo" y "Ventanas emergentes" para CuidaDosis (Info. de la aplicación → Notificaciones y Otros permisos) para que la pantalla de alarma aparezca con el teléfono bloqueado, y deja el **volumen de alarma** por encima de cero.
6. Al abrir "Hoy" no debe aparecer la tarjeta roja de permisos.

Los resultados medidos en el teléfono físico están en `docs/evidencias/p4/fisico/RESUMEN.md`.

### 2.4 APK release de la beta (versión 1.1.2, servidor fijo)
El release apunta siempre a `https://areproyectos.com.mx/cuidadosis/api/`, solo usa HTTPS y no tiene la opción de cambiar el servidor (D49). Necesita el keystore en `.secrets/` (ignorada por git):
- `.secrets/cuidadosis-release.jks`
- `.secrets/release-signing.properties` con `storeFile=cuidadosis-release.jks`, `storePassword`, `keyAlias` y `keyPassword`.

Sin ese archivo, Gradle compila el release sin firmar. Respalda el keystore fuera de la PC: sin él no se pueden publicar actualizaciones que se instalen encima.
```bash
cd android
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew.bat testDebugUnitTest --rerun assembleRelease
sha256sum app/build/outputs/apk/release/app-release.apk
```
- `--rerun` obliga a ejecutar las 79 pruebas aunque Gradle las marque como `UP-TO-DATE`.
- APK: `android/app/build/outputs/apk/release/app-release.apk`, firmado con el esquema v2. Comprobación: `apksigner verify --print-certs` (huella SHA-256 del certificado `6d03a106a1ec6cf59661b86ff111ff8bd6b1e0824eb2b1883045473eaffb29fb`).
- Para pasar de la 1.1.1 debug a la 1.1.2 release primero desinstala la anterior, porque las firmas son distintas. Después repite en HyperOS "Batería sin restricciones" (2.3).
- Instalar por USB en un Xiaomi: aparecen dos avisos. Play Protect pregunta si enviar la app a revisión ("No enviar" también instala) y después "Instalar vía USB" tiene 10 s para tocar **Instalar**.
- Descarga pública: `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.2-beta.apk`. En el teléfono hay que permitir "instalar apps desconocidas" para el navegador.

## 3. Despliegue en un hosting

El paquete se arma y se prueba en la PC. **Desplegado el 23/11/2026 en `https://areproyectos.com.mx/cuidadosis/api/`** con `--subdir=cuidadosis`: pasos en 3.3, verificación en 3.4 y resultados en `docs/evidencias/p4/hosting/DESPLIEGUE.md` (D49).

### 3.1 Armar el paquete (ejecutado)
```bash
C:/xampp/php/php.exe deploy/build_hosting_package.php
```
Crea `build/hosting/` (git lo ignora) con esta estructura, pensada para un hosting compartido donde la raíz web es `public_html/`:

| Ruta en el paquete | Se sube a | Contenido | ¿Accesible por web? |
|---|---|---|---|
| `public_html/` | la raíz web de la cuenta | solo `index.php` y `.htaccess` (de `api/public/`) | Sí, es el único punto de entrada |
| `src/` | la carpeta de la cuenta, **junto a** `public_html` | código de la API | No |
| `config/` | junto a `public_html` | carga del `.env` y zona horaria | No |
| `.env.example` | junto a `public_html`, renombrado a `.env` en el servidor | plantilla (`api/.env.hosting.example`) | No |
| `bd/schema_hosting.sql` | no se sube: se importa en phpMyAdmin | `bd/schema.sql` sin `CREATE DATABASE` ni `USE` | No |
| `bd/seed_hosting.sql` | no se sube: se importa después del esquema | sin cuentas de demostración; solo verifica que las 7 tablas existen y están vacías | No |
| `MANIFIESTO.txt` | no se sube | SHA-256 de cada archivo | No |

`index.php` busca `../src` y `../config`, y `config.php` busca `../.env`; por eso esas tres van un nivel arriba de `public_html`, fuera de la web. El script falla si el paquete trae un `.env` o si `public_html` tiene algo más que `index.php` y `.htaccess`. Nunca se suben `api/.env`, `bd/seed.sql`, `docs/`, `android/` ni `api/tests/`.

Si el hosting permite elegir la raíz del dominio (VPS o panel con "document root"), se puede subir la carpeta `api/` completa sin `.env` y apuntar la raíz a `api/public/`.

#### 3.1.1 API en una subcarpeta de un dominio compartido (areproyectos.com.mx/cuidadosis/)
Cuando el dominio aloja varios proyectos y la base de datos también es compartida:
```bash
C:/xampp/php/php.exe deploy/build_hosting_package.php --subdir=cuidadosis
```
| Ruta en el paquete | Se sube a | ¿Accesible por web? |
|---|---|---|
| `public_html/cuidadosis/api/` | `public_html/cuidadosis/api/` del hosting: solo `index.php` y `.htaccess` | Sí: `https://areproyectos.com.mx/cuidadosis/api/` |
| `cuidadosis_app/` | la carpeta de la cuenta, **junto a** `public_html` (no dentro): `src/`, `config/`, `.env.example` y un `.htaccess` que niega todo | No |
| `bd/` | no se sube: se importa en phpMyAdmin | No |

- El `index.php` del paquete se genera apuntando a `../../../cuidadosis_app` (en el repo sigue siendo `..`). Si la raíz web del hosting no se llama `public_html` o no se puede escribir fuera de ella, avisa antes de subir: la ruta cambia.
- `APP_BASE_PATH=/cuidadosis/api` en el `.env` del servidor (ya viene en la plantilla). Si queda vacío, la API deduce el prefijo de la ubicación de `index.php`, que da lo mismo; en XAMPP se deja vacío.
- El `.htaccess` usa reglas relativas a su carpeta, así que no necesita cambios para la subcarpeta. Solo si el hosting usa alias y todas las rutas dan 404, agrega `RewriteBase /cuidadosis/api/` (está indicado como comentario).
- La base compartida: todas las tablas de CuidaDosis empiezan con `cd_` y `schema_hosting.sql` solo crea o borra tablas `cd_*`; el script de paquete se niega a armarlo si el esquema toca otra tabla.
- En la app, la URL del servidor es `https://areproyectos.com.mx/cuidadosis/api/` (con la diagonal final).

### 3.2 Prueba local del paquete (ejecutada el 21/11/2026)
Con una base temporal y el servidor embebido de PHP, sin tocar la base principal:
```bash
set -a && . ./api/.env && set +a && export MYSQL_PWD="$DB_PASS"
M="/c/xampp/mysql/bin/mysql.exe -h $DB_HOST -P $DB_PORT -u $DB_USER --default-character-set=utf8mb4"
$M -e "CREATE DATABASE cuidadosis_pkgtest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
$M cuidadosis_pkgtest < build/hosting/bd/schema_hosting.sql
$M cuidadosis_pkgtest < build/hosting/bd/seed_hosting.sql
sed 's/^DB_NAME=.*/DB_NAME=cuidadosis_pkgtest/; s/^APP_DEBUG=.*/APP_DEBUG=false/' api/.env > build/hosting/.env
cd build/hosting && C:/xampp/php/php.exe -S 127.0.0.1:8099 -t public_html public_html/index.php
```
Resultado: 7 tablas con 0 filas; `GET /` respondió `ok` con la hora de México; el registro de un usuario sintético funcionó; `/../.env`, `/.env`, `/../src/Auth.php`, `/../config/config.php` y `/../bd/schema_hosting.sql` dieron 404. El servidor embebido no aplica `.htaccess`; las reglas del `.htaccess` ya están probadas en Apache (sección 1.4). Al terminar se borraron la base temporal (`DROP DATABASE cuidadosis_pkgtest`) y `build/hosting/.env`.

**Prueba con subcarpeta y base compartida (ejecutada el 23/11/2026).** Paquete con `--subdir=cuidadosis`. La base temporal tenía antes dos tablas de "otro proyecto" (`users` y `links`, con una llave foránea llamada `fk_links_patient` y una fila cada una). Se importaron `schema_hosting.sql` y `seed_hosting.sql`, se creó `build/hosting/cuidadosis_app/.env` apuntando a esa base con `APP_BASE_PATH=/cuidadosis/api` y se sirvió con `php -S 127.0.0.1:8099 -t public_html` (el servidor embebido envía las rutas inexistentes al `index.php` de la carpeta más cercana, como el `.htaccess`):
- Esquema: 7 tablas `cd_*` con 0 filas, sin error por nombres de restricciones; las tablas del otro proyecto siguieron con 1 fila cada una.
- `GET /cuidadosis/api/` 200 `ok`; registro 201; login con token de 64 caracteres; `GET /cuidadosis/api/me` 200; `GET /cuidadosis/api/intakes/today` 200; `/me` sin token 401; ruta inexistente 404. El usuario quedó en `cd_users`.
- `/cuidadosis_app/.env`, `/../cuidadosis_app/.env`, `/cuidadosis/api/../../../cuidadosis_app/.env`, `.../src/Auth.php` y `/bd/schema_hosting.sql`: 404.
- Al terminar se borraron la base temporal y el `.env` temporal, y se rearmó el paquete.
- No probado: Apache o LiteSpeed reales sirviendo `/cuidadosis/api/` (en XAMPP el mismo `.htaccess` funciona en `/CuidaDosis/api/public/`, que también es una subcarpeta).

### 3.3 Subir al hosting (ejecutado el 23/11/2026 en areproyectos.com.mx)
Cómo se hizo en areproyectos.com.mx (cPanel, nginx con LiteSpeed y PHP 8.2.36):
- **Credenciales:** van en `.secrets/hosting.env` (`FTP_*` y `DB_*`), carpeta ignorada por git. Se leen línea por línea, sin `source`: si una contraseña trae `;`, `(` o `{`, `source` falla y el error muestra el valor.
- **FTP con TLS explícito:** usa el nombre del servidor que viene en el certificado (`svgil1.cloud-mx-ns.net`, misma IP que `ftp.areproyectos.com.mx`). Con `ftp.areproyectos.com.mx` la verificación del certificado falla y no se debe desactivar. Ejemplo con curl, pasando el usuario por `-K -` para que no aparezca en la línea de comandos: `printf 'user = "%s:%s"\n' "$FTP_USER" "$FTP_PASS" | curl --ssl-reqd -K - -T archivo ftp://svgil1.cloud-mx-ns.net/ruta`.
- **Rutas:** la raíz FTP es la carpeta de la cuenta y la raíz web es `public_html/`. Se suben `public_html/cuidadosis/api/` y `cuidadosis_app/` del paquete. El `.env` de producción va en `cuidadosis_app/.env` con permisos 600 (`SITE CHMOD 600`), y la carpeta con 700.
- **`.env`:** `DB_HOST=localhost` (desde el propio servidor), `DB_NAME` y `DB_USER` de la cuenta, `APP_DEBUG=false`, `APP_BASE_PATH=/cuidadosis/api`.
- **Base:** `schema_hosting.sql` y `seed_hosting.sql` se importaron con el cliente `mysql` desde la PC. Antes hay que autorizar la IP en "Remote MySQL" de cPanel; phpMyAdmin también sirve.
- **Integridad:** se descargó cada archivo subido y se comparó su SHA-256 con el local.
- **Cuidado con los barridos de seguridad:** pedir muchas rutas sensibles seguidas (`.env`, `.git`, `..`) activa la protección anti-bots del hosting, que bloquea la IP unos minutos (HTTPS con la página "One moment, please...", y FTP y MySQL sin conexión). Deja varios segundos entre rutas.

Pasos generales para cualquier hosting:
1. Crea en el panel una base MySQL o MariaDB y un usuario con permisos sobre ella.
2. En phpMyAdmin del hosting, selecciona esa base e importa `bd/schema_hosting.sql` y luego `bd/seed_hosting.sql` (debe mostrar 0 en todas las columnas). Si la base es compartida, solo se crean o reemplazan tablas `cd_*`; las de otros proyectos no se tocan.
3. Sube por el administrador de archivos o SFTP: el contenido de `public_html/` a la raíz web, y `src/`, `config/` y `.env.example` a la carpeta de la cuenta (un nivel arriba). Con `--subdir=cuidadosis`: `public_html/cuidadosis/api/` a la misma ruta del hosting y `cuidadosis_app/` a la carpeta de la cuenta (ver 3.1.1); el `.env` va dentro de `cuidadosis_app/`.
4. En el servidor, renombra `.env.example` a `.env` y llena `DB_HOST`, `DB_NAME`, `DB_USER` y `DB_PASS` con los datos del panel. Deja `APP_DEBUG=false`.
5. Activa HTTPS (certificado del panel, por ejemplo Let's Encrypt) y la redirección de HTTP a HTTPS.
6. En la app, compila con `api.baseUrl=https://<dominio>/` (con subcarpeta: `https://areproyectos.com.mx/cuidadosis/api/`) en `android/local.properties` o escríbelo en **Servidor**. La versión release solo acepta HTTPS.

### 3.4 Lista de verificación después de subir (ejecutada el 23/11/2026 en areproyectos.com.mx)
En areproyectos.com.mx se cumplieron todas las filas de la tabla, con una excepción: `http://` no redirige a `https://` y responde 200 por HTTP sin cifrar. La app release no se ve afectada porque solo usa HTTPS. Resultados detallados: `docs/evidencias/p4/hosting/DESPLIEGUE.md`.

| Revisión | Cómo | Esperado |
|---|---|---|
| HTTPS | abrir `https://<dominio>/` | `{"status":"ok"...}` con candado válido |
| HTTP redirige | `curl -s -o /dev/null -w '%{http_code}' http://<dominio>/` | `301` o `308` |
| Zona horaria `America/Mexico_City` | campo `time` de `GET /` | hora de México, no UTC; la API la fija en PHP y calcula el desfase para MySQL en cada conexión |
| PHP | panel del hosting | 8.2 o mayor, con `pdo_mysql` |
| `.env` no accesible | `curl -s -o /dev/null -w '%{http_code}' https://<dominio>/.env` y `https://<dominio>/../.env` | `403` o `404`, nunca `200` |
| `src` no accesible | `https://<dominio>/src/Auth.php`, `https://<dominio>/../src/Auth.php` | `403` o `404` |
| `bd` no accesible | `https://<dominio>/bd/schema_hosting.sql` | `403` o `404` (no debe estar en el servidor) |
| Encabezado de token | iniciar sesión desde la app | sin errores 401 (el `.htaccess` pasa `Authorization` a PHP) |
| Sin cuentas de demo | `SELECT COUNT(*) FROM cd_users` en phpMyAdmin tras subir | `0` |
| Debug apagado | `.env` | `APP_DEBUG=false` |
| Subcarpeta (con `--subdir`) | `https://areproyectos.com.mx/cuidadosis/api/` y `https://areproyectos.com.mx/cuidadosis_app/.env` | `{"status":"ok"...}`; el segundo `403` o `404` |
| Tablas con prefijo | en phpMyAdmin, la lista de tablas | solo 7 tablas `cd_*` de CuidaDosis; las de otros proyectos sin cambios |

### 3.5 Actualizar
Vuelve a correr `deploy/build_hosting_package.php` (con el mismo `--subdir`, si lo usaste), compara `MANIFIESTO.txt` con el anterior y sube solo `public_html/`, `src/` y `config/` (con subcarpeta: `public_html/cuidadosis/api/`, `cuidadosis_app/src/` y `cuidadosis_app/config/`). No vuelvas a importar `schema_hosting.sql`: borra las tablas `cd_*` y sus datos.
