# Manual de instalación de CuidaDosis 1.1.3 (fuente verificada)

Fuente para redactar el manual de instalación. Cada paso indica la acción, el resultado esperado, la captura (en `capturas/` de esta carpeta) y qué hacer si falla. Todo corresponde al despliegue real del 23/11/2026 en `https://areproyectos.com.mx/cuidadosis/` y a la app 1.1.3. Lo que no se pudo comprobar dice **no verificado**.

Las credenciales nunca se escriben en el manual. Se usan marcadores: `<USUARIO_FTP>`, `<CONTRASEÑA_FTP>`, `<USUARIO_BD>`, `<CONTRASEÑA_BD>`, `<NOMBRE_BD>`, `<CUENTA_CPANEL>`.

> CuidaDosis es un prototipo académico. No recomienda dosis ni sustituye al médico. Solo se usan datos sintéticos.

## 1. Requisitos

### 1.1 Servidor (hosting)
| Componente | Requisito | Verificado en areproyectos.com.mx |
|---|---|---|
| Panel | cPanel o equivalente, con FTP, bases MySQL y certificado HTTPS | cPanel |
| Servidor web | Apache o LiteSpeed con `mod_rewrite` y `.htaccess` permitido | nginx delante de LiteSpeed |
| PHP | 8.2 o mayor, con `pdo_mysql` | 8.2.36 |
| Base de datos | MariaDB 10.4 o mayor, o MySQL 8, utf8mb4 | MariaDB 11.4.13 (base compartida con otros proyectos) |
| HTTPS | Certificado válido | Let's Encrypt |
| Acceso | FTP con TLS explícito y, opcional, "Remote MySQL" | Sí |

### 1.2 Computadora del equipo (para armar el paquete)
- Copia del repositorio.
- PHP 8.2 (en Windows, el de XAMPP: `C:\xampp\php\php.exe`).
- Cliente FTP con TLS (por ejemplo, curl o FileZilla) y, opcional, el cliente `mysql`.

### 1.3 Teléfono
| Requisito | Valor |
|---|---|
| Android | 8.0 (API 26) o superior; probado en Android 16 (POCO M6 Pro, HyperOS) y en el emulador Android 17 |
| Conexión | Internet (la app usa el servidor por HTTPS) |
| Espacio | Unos 9 MB para el APK y unos 45 MB instalada |

## 2. Instalación del servidor en el hosting

Pasos ejecutados el 23/11/2026. Resultados en `docs/evidencias/p4/hosting/DESPLIEGUE.md`.

1. **Comprobar la versión de PHP.**
   - Acción: en cPanel, abrir "Seleccionar versión de PHP" (o "MultiPHP Manager") y elegir 8.2 o mayor para el dominio.
   - Resultado esperado: PHP 8.2 o mayor con la extensión `pdo_mysql`.
   - Si falla: si no hay 8.2, la API no arranca (usa `declare(strict_types=1)` y funciones de PHP 8). Pedir al proveedor que la active.

2. **Crear la base de datos y el usuario.**
   - Acción: en cPanel, "Bases de datos MySQL": crear `<NOMBRE_BD>` y el usuario `<USUARIO_BD>` con una contraseña fuerte, y asignarle todos los privilegios sobre esa base.
   - Resultado esperado: la base aparece en la lista con el usuario asignado.
   - Nota: la base puede ser compartida con otros proyectos. Todas las tablas de CuidaDosis llevan el prefijo `cd_`, y los scripts solo crean o borran tablas `cd_*`.

3. **Armar el paquete en la computadora.**
   - Acción: desde la raíz del repositorio:
     ```
     C:/xampp/php/php.exe deploy/build_hosting_package.php --subdir=cuidadosis
     ```
   - Resultado esperado: carpeta `build/hosting/` con `public_html/cuidadosis/api/` (solo `index.php` y `.htaccess`), `cuidadosis_app/` (código, configuración y plantilla `.env.example`), `bd/schema_hosting.sql`, `bd/seed_hosting.sql` y `MANIFIESTO.txt` con el SHA-256 de cada archivo.
   - Si falla: el script se detiene si el paquete trae un `.env`, si `public_html` tiene algo más que `index.php` y `.htaccess`, o si el esquema toca una tabla sin prefijo `cd_`. Corregir y volver a correrlo.

4. **Importar el esquema y los datos iniciales.**
   - Acción: en phpMyAdmin, seleccionar `<NOMBRE_BD>` e importar primero `bd/schema_hosting.sql` y después `bd/seed_hosting.sql`. También se puede con el cliente `mysql` desde la computadora (ver el problema frecuente 7.2).
   - Resultado esperado: 7 tablas `cd_users`, `cd_links`, `cd_medications`, `cd_schedules`, `cd_intake_logs`, `cd_api_tokens` y `cd_link_attempts`, con 7 llaves foráneas `fk_cd_*` y 0 filas. `seed_hosting.sql` no crea cuentas: en el servidor público no hay usuarios con contraseña conocida.
   - Si falla: si aparece un error de nombre de restricción duplicado, revisar que se esté usando `schema_hosting.sql` del paquete. Las tablas de otros proyectos no se tocan.
   - Cuidado: no volver a importar `schema_hosting.sql` en un servidor con datos: borra las tablas `cd_*`.

5. **Subir los archivos por FTP con TLS.**
   - Acción: conectar por FTP con TLS explícito usando el **nombre del servidor que aparece en el certificado** (en este hosting, `svgil1.cloud-mx-ns.net`), con `<USUARIO_FTP>` y `<CONTRASEÑA_FTP>`. Subir:
     - `public_html/cuidadosis/api/` del paquete a `public_html/cuidadosis/api/` del hosting.
     - `cuidadosis_app/` a la carpeta de la cuenta (`/home/<CUENTA_CPANEL>/cuidadosis_app/`), **junto a** `public_html`, no dentro.
   - Resultado esperado: la raíz FTP muestra `public_html/` y `cuidadosis_app/` al mismo nivel.
   - Si falla: con `ftp.<dominio>` la verificación del certificado falla (el certificado es del servidor, no del dominio). No desactivar la verificación ni usar FTP sin cifrar; usar el nombre del servidor (problema frecuente 7.3).
   - Comprobación: descargar cada archivo subido y comparar su SHA-256 con `MANIFIESTO.txt` (se hizo así en el despliegue).

6. **Crear el `.env` de producción.**
   - Acción: en `cuidadosis_app/`, crear `.env` a partir de `.env.example` con: `DB_HOST=localhost`, `DB_PORT=3306`, `DB_NAME=<NOMBRE_BD>`, `DB_USER=<USUARIO_BD>`, `DB_PASS=<CONTRASEÑA_BD>`, `TOKEN_TTL_HOURS=720`, `APP_DEBUG=false` y `APP_BASE_PATH=/cuidadosis/api`. Dar permisos 600 al `.env` y 700 a la carpeta `cuidadosis_app/`.
   - Resultado esperado: el archivo existe fuera de la carpeta web y solo lo lee la cuenta.
   - Si falla: si la API responde 500 con "Error interno del servidor.", revisar los datos de la base. Nunca subir el `.env` al repositorio ni copiarlo a `public_html`.

7. **Activar HTTPS.**
   - Acción: en cPanel, "SSL/TLS Status": emitir o renovar el certificado del dominio. Se recomienda activar también "Force HTTPS Redirect" en "Dominios".
   - Resultado esperado: candado válido en `https://areproyectos.com.mx/`.
   - Estado real: el certificado funciona; la redirección de HTTP a HTTPS **no está activada** (HTTP responde 200 sin redirigir). La app no se ve afectada porque solo usa HTTPS.

8. **Publicar el APK para descarga.**
   - Acción: subir `deploy/descargas/.htaccess` y `CuidaDosis-1.1.3-beta.apk` a `public_html/cuidadosis/descargas/`. Al publicar una versión nueva, subir el APK nuevo y borrar el anterior en la misma conexión FTP.
   - Resultado esperado: `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.3-beta.apk` responde 200 con tipo `application/vnd.android.package-archive` y `Content-Disposition: attachment`, 8 830 820 bytes, SHA-256 `08c66b382cc6c88b4c56f646fc86b100f4bc8eb97686eaa149e1b68fc8af71b5`. El `.htaccess` desactiva el listado de la carpeta.
   - Si falla: si una URL borrada sigue respondiendo 200, es la caché de nginx (problema frecuente 7.6).

## 3. Verificación del servidor
| # | Acción | Resultado esperado | Resultado real (23/11/2026) |
|---|---|---|---|
| 1 | Abrir `https://areproyectos.com.mx/cuidadosis/api/` | `{"status":"ok","service":"CuidaDosis API","time":"..."}` con candado válido | OK, con la hora de México (el servidor está en UTC) |
| 2 | Abrir `https://areproyectos.com.mx/cuidadosis_app/.env` y rutas con `..` hacia `.env`, `src/` y `bd/` | 403 o 404, nunca 200 | 22 de 22 rutas sensibles con 403, 404 o 400 (`hosting/seg05_hosting.csv`) |
| 3 | Registrar e iniciar sesión desde la app | Sin errores 401 | OK |
| 4 | `GET /intakes/today` con token | Respuesta en menos de 1 s | 0.32 a 0.34 s en 5 mediciones (`hosting/rnf07_hosting.csv`) |
| 5 | `SELECT COUNT(*) FROM cd_users` justo después de importar | 0 | 0 |
| 6 | `http://areproyectos.com.mx/cuidadosis/api/` | 301 o 308 hacia HTTPS | **No cumple:** responde 200 por HTTP (pendiente "Force HTTPS Redirect") |

Las rutas sensibles se deben revisar **despacio**, con varios segundos entre cada una (problema frecuente 7.1).

## 4. Instalación de la app en el teléfono

Ejecutado el 23/11/2026 en el POCO M6 Pro (Android 16, HyperOS) con Chrome. El orden de los avisos puede variar en otros teléfonos.

1. **Descargar el APK.**
   - Acción: en el teléfono, abrir en Chrome `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.3-beta.apk`.
   - Resultado esperado: Chrome avisa "Es posible que el archivo sea dañino" y pregunta si se quiere descargar `CuidaDosis-1.1.3-beta.apk` (8.83 MB) de areproyectos.com.mx. Tocar **Descargar de todos modos**.
   - Captura: `29_instalar_descarga_v113.png`.
   - Si falla: si la página muestra "One moment, please...", la IP está bloqueada temporalmente por la protección anti-bots (7.1); esperar unos minutos.

2. **Abrir el archivo descargado.**
   - Acción: en la lista de descargas, tocar `CuidaDosis-1.1.3-beta.apk`. Si pregunta con qué abrirlo, elegir **Instalador de paquetes**.
   - Resultado esperado: se abre el instalador.
   - Captura: `29b_instalar_abrir_con_v113.png`.

3. **Permitir instalar apps de este origen (solo la primera vez).**
   - Acción: si Android lo pide, activar **Confiar en esta fuente** ("Instalar apps desconocidas") para Chrome y regresar.
   - Resultado esperado: el interruptor queda activado y aparece la pregunta de instalación.
   - Captura: `30_instalar_origen_desconocido_v113.png` (el ajuste ya activado).
   - Nota: en el teléfono del equipo Chrome ya tenía este permiso, así que **no se vio la solicitud**; la captura muestra el ajuste en Ajustes, Apps, Chrome, Instalar apps desconocidas.

4. **Confirmar la instalación.**
   - Acción: en "CuidaDosis: ¿Deseas instalar esta app?", tocar **Instalar**.
   - Captura: `32_instalar_confirmar_v113.png`.

5. **Aviso de Google Play Protect.**
   - Acción: aparece "Se recomienda analizar la app" porque la app no viene de Play Store. Para no enviar el APK a Google, tocar **Más detalles** y después **Instalar sin analizar**. No tocar "No instalar app".
   - Resultado esperado: el teléfono pide confirmar la identidad ("¿Quieres instalar la app sin analizarla?") con la huella o con la contraseña o PIN del dispositivo.
   - Capturas: `31_instalar_play_protect_v113.png` y `31b_instalar_sin_analizar_v113.png`. La pantalla de confirmación de identidad no se puede capturar: Android la protege y la captura sale negra.
   - Diferencia con versiones anteriores del documento: en la instalación por USB el aviso ofrecía "No enviar"; desde el navegador ofrece "Analizar app", "No instalar app" y, dentro de "Más detalles", "Instalar sin analizar".

6. **Fin de la instalación.**
   - Resultado esperado: el instalador muestra "Instalando..." y la app queda instalada. En Xiaomi, el análisis de seguridad propio muestra "Pasó pruebas de seguridad. No se detectaron riesgos".
   - Captura: `32b_instalar_resultado_xiaomi_v113.png` (recortada; debajo hay anuncios del sistema).
   - Comprobación: Ajustes, Aplicaciones, CuidaDosis: "Versión: 1.1.3" e ícono de la cápsula (`28_info_app_version_v113.png`, `27_icono_cajon_v113.png`).

7. **Actualizar desde una versión anterior.**
   - Desde la 1.1.2 (release): se instala encima y conserva la sesión (misma firma).
   - Desde una versión debug (1.1.1 o anterior): Android no instala encima porque la firma es distinta. Desinstalar primero (se pierden la sesión y los datos del teléfono; los del servidor no).
   - Después de desinstalar y volver a instalar se deben repetir los ajustes del teléfono (manual de configuración, sección 4): en HyperOS la batería vuelve a "Ahorro de batería" y el Inicio automático se desactiva (comprobado el 23/11/2026).

8. **Primer arranque.**
   - Acción: abrir CuidaDosis.
   - Resultado esperado: pantalla de inicio de sesión con la línea fija "Servidor: https://areproyectos.com.mx/cuidadosis/api/" (no se puede tocar ni cambiar).
   - Captura: `33_inicio_sesion_release_v113.png`.
   - Al iniciar sesión, la app pide permiso de notificaciones ("¿Quieres permitir que CuidaDosis te envíe notificaciones?"): tocar **Permitir** (`33b_permiso_notificaciones_v113.png`). El permiso de alarmas exactas se concede al instalar; la release no pide el permiso de red local.
   - Siguiente paso: manual de configuración, sección 4 (ajustes del teléfono).

## 5. Verificación de la app
| # | Acción | Resultado esperado | Estado |
|---|---|---|---|
| 1 | Abrir la app | Login con el servidor fijo | Verificado |
| 2 | Crear una cuenta sintética e iniciar sesión | Entra a "Hoy" (paciente) o "Mis pacientes" (cuidador) | Verificado |
| 3 | Agregar un medicamento a pocos minutos | Aviso previo y alarma a la hora | Verificado (ver manual de ejecución) |
| 4 | Revisar la versión | "Versión: 1.1.3" | Verificado |

## 6. Alternativa local con XAMPP (resumen)
Para desarrollo y pruebas del equipo, no para el grupo beta. Detalle en `docs/instalacion.md`, secciones 1 y 2.
1. Instalar XAMPP (Apache 2.4, PHP 8.2, MariaDB 10.4) y clonar el repositorio en `C:\xampp\htdocs\CuidaDosis`.
2. Encender Apache y MySQL; importar `bd/schema.sql` y `bd/seed.sql` (cuentas sintéticas con contraseña `Sintetico2026`).
3. Copiar `api/.env.example` a `api/.env` y llenar los datos locales. Comprobar `http://localhost/CuidaDosis/api/public/`.
4. Compilar el APK debug (`cd android` y `gradlew.bat assembleDebug`) e instalarlo con `adb install -r`. En el login, enlace **Servidor**: `http://<IP de la PC>/CuidaDosis/api/public/` (solo en debug).

## 7. Problemas frecuentes (observados en el proyecto)

### 7.1 La IP queda bloqueada por la protección anti-bots
- Síntoma: el navegador o curl reciben una página "One moment, please..." (200, HTML) en lugar de JSON; FTP y MySQL remoto no conectan.
- Causa: la protección del hosting (Imunify360) marcó la IP tras muchas peticiones seguidas a rutas sensibles (barrido de seguridad).
- Solución: esperar unos 15 minutos; se libera sola. Hacer las revisiones despacio, con varios segundos entre peticiones. Con el uso normal de la app no se ha visto.

### 7.2 MySQL remoto rechaza la conexión
- Síntoma: `Access denied` o tiempo de espera al conectar con `mysql` desde la computadora.
- Causa: la IP pública de la computadora no está autorizada (pasó al cambiar de red).
- Solución: agregar la IP actual en cPanel, "Remote MySQL", o usar phpMyAdmin. Quitarla cuando ya no se necesite.

### 7.3 FTP con TLS: el certificado no coincide
- Síntoma: error de certificado (`SEC_E_WRONG_PRINCIPAL` en Windows) al conectar a `ftp.areproyectos.com.mx`.
- Causa: el certificado del FTP está a nombre del servidor (`svgil1.cloud-mx-ns.net`), no del dominio.
- Solución: conectar al nombre del servidor (misma IP) con TLS y verificación. No desactivar la verificación ni usar FTP sin cifrar.
- En Windows, curl puede fallar al revisar la revocación del certificado de Let's Encrypt; usar `--ssl-revoke-best-effort` (sigue verificando el certificado).

### 7.4 La app release se cierra al abrir (R8)
- Síntoma: la 1.1.2 compilada con optimización se cerraba al abrir (`IllegalAccessError` en `kotlin.sequences`).
- Causa: la optimización parcial de R8 del proyecto.
- Solución aplicada: R8 desactivado en el release (D49). El APK pesa unos 8.5 a 8.8 MB en lugar de 2.4 MB y ejecuta el mismo código que debug. Activarlo requiere reglas de keep y una nueva prueba en el teléfono.

### 7.5 Una petición POST responde 403 en HTML
- Causa: el filtro del servidor rechaza los POST sin `Content-Length` (por ejemplo `curl -X POST` sin datos).
- Solución: enviar `Content-Length: 0`. La app no se ve afectada.

### 7.6 Un archivo borrado sigue descargándose
- Causa: nginx guarda en caché las respuestas sin `Cache-Control`.
- Solución: esperar o probar con un parámetro (`?v=2`); con él responde 404. La API envía `Cache-Control: no-store`.

### 7.7 Android no instala encima ("conflicto con un paquete existente")
- Causa: la versión instalada es debug (otra firma).
- Solución: desinstalar la anterior y repetir los ajustes del teléfono.

### 7.8 Después de reinstalar, los avisos se retrasan
- Causa: HyperOS devuelve la batería a "Ahorro de batería" y desactiva el Inicio automático.
- Solución: repetir el manual de configuración, sección 4. Revisar también "Pausar la actividad de la aplicación si no se utiliza" (7.9).

### 7.9 "Pausar la actividad de la aplicación si no se utiliza"
- Viene activado en Android. Si la app no se abre en meses, Android puede quitarle permisos y detener sus notificaciones.
- Se recomienda desactivarlo en Info. de la aplicación. En el teléfono del equipo quedó activado (valor por defecto) y no se cambió.
