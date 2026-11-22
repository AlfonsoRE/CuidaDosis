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

## 3. Despliegue en un hosting

El paquete se arma y se prueba en la PC. **No se ha ejecutado nada contra un servidor externo**: los pasos 3.3 a 3.5 (subir al hosting) están pendientes de verificar en un hosting real.

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

### 3.3 Subir al hosting (pendiente de verificar)
1. Crea en el panel una base MySQL o MariaDB y un usuario con permisos sobre ella.
2. En phpMyAdmin del hosting, selecciona esa base e importa `bd/schema_hosting.sql` y luego `bd/seed_hosting.sql` (debe mostrar 0 en todas las columnas).
3. Sube por el administrador de archivos o SFTP: el contenido de `public_html/` a la raíz web, y `src/`, `config/` y `.env.example` a la carpeta de la cuenta (un nivel arriba).
4. En el servidor, renombra `.env.example` a `.env` y llena `DB_HOST`, `DB_NAME`, `DB_USER` y `DB_PASS` con los datos del panel. Deja `APP_DEBUG=false`.
5. Activa HTTPS (certificado del panel, por ejemplo Let's Encrypt) y la redirección de HTTP a HTTPS.
6. En la app, compila con `api.baseUrl=https://<dominio>/` en `android/local.properties` o escríbelo en **Servidor**. La versión release solo acepta HTTPS.

### 3.4 Lista de verificación después de subir (pendiente de verificar)
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
| Sin cuentas de demo | `SELECT COUNT(*) FROM users` en phpMyAdmin tras subir | `0` |
| Debug apagado | `.env` | `APP_DEBUG=false` |

### 3.5 Actualizar
Vuelve a correr `deploy/build_hosting_package.php`, compara `MANIFIESTO.txt` con el anterior y sube solo `public_html/`, `src/` y `config/`. No vuelvas a importar `schema_hosting.sql`: borra las tablas.
