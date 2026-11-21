# Instalación de CuidaDosis

Guía reproducible para levantar el prototipo. Los comandos de la parte local se ejecutaron en la verificación del 21/11/2026, en Windows 11 con XAMPP (Apache 2.4.58, PHP 8.2.12, MariaDB 10.4.32) y Android Studio. La parte de hosting no se pudo ejecutar porque el equipo no tiene un hosting; se marca como **pendiente de verificar**.

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
- Pruebas unitarias: 65, todas en verde (`android/app/build/test-results/testDebugUnitTest/`).
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

### 2.3 Teléfono físico
- Conecta el teléfono a la misma red Wi-Fi que la PC y, en la pantalla de inicio de sesión, toca **Servidor** y escribe `http://<IP de la PC>/CuidaDosis/api/public/`. Puede ser necesario permitir Apache en el firewall de Windows.
- El APK debug permite HTTP sin cifrar solo para pruebas.
- **Pendiente de ejecución manual:** puntualidad de avisos con pantalla bloqueada, ahorro de batería y reinicio en un teléfono real (ver [PENDIENTES_FASE6.md](PENDIENTES_FASE6.md)).

## 3. Despliegue en un hosting (pendiente de verificar)

No se ejecutó: el equipo no tiene hosting. Son los pasos que el diseño exige (ADR-001, ADR-008, ADR-009):

1. **Carpeta pública:** solo `api/public/` debe quedar como raíz web (document root) del dominio o subdominio. `api/src`, `api/config`, `api/.env`, `bd/` y `docs/` deben quedar fuera de la carpeta pública. Si el hosting no deja cambiar la raíz, conserva los dos `.htaccess` del repo y comprueba con `curl` que `api/.env` responde 403.
2. **`.env`:** créalo en el servidor (no lo subas al repo) con los datos de la base del hosting, `APP_DEBUG=false` y el `TOKEN_TTL_HOURS` deseado.
3. **Base de datos:** importa `bd/schema.sql` en una base vacía (desde phpMyAdmin del hosting). Carga `bd/seed.sql` solo para demostraciones: son datos sintéticos.
4. **Zona horaria:** la API fija `America/Mexico_City` en PHP y calcula el desfase de la sesión de MySQL en cada conexión, así que no depende de la zona del servidor. Revisa que la versión de PHP sea 8.2 o mayor, con `pdo_mysql`.
5. **HTTPS:** obligatorio fuera de la red local. La versión release de la app rechaza HTTP; con HTTPS, la URL base sería `https://<dominio>/` (si `api/public/` es la raíz) y se configura con `api.baseUrl` en `android/local.properties` o desde **Servidor**.
6. Repite las comprobaciones de la sección 1.4 contra el dominio del hosting.
