# Despliegue en areproyectos.com.mx y versión 1.1.2 (23/11/2026)

Decisión: D49 en `docs/decisiones-fase-6.md`. Procedimiento: `docs/instalacion.md` 2.4, 3.3 y 3.4. Este archivo no tiene credenciales, tokens, IPs ni capturas con datos privados. Todas las cuentas usadas fueron sintéticas (`@ejemplo.test`), con contraseñas aleatorias que nunca se mostraron, y se borraron al terminar.

## Entorno
| Concepto | Valor |
|---|---|
| URL de la API | `https://areproyectos.com.mx/cuidadosis/api/` |
| Servidor | cPanel; nginx delante de LiteSpeed (`SERVER_SOFTWARE` Apache); raíz web `public_html` |
| PHP | 8.2.36 (LiteSpeed), con `pdo_mysql` y `mbstring`; zona del servidor UTC |
| Base de datos | MariaDB 11.4.13, base compartida; `DB_HOST=localhost` desde el servidor |
| Certificado HTTPS | `*.areproyectos.com.mx` y `areproyectos.com.mx`, válido hasta el 21/02/2027 |
| Certificado FTP (TLS explícito) | `svgil1.cloud-mx-ns.net` (Let's Encrypt, válido hasta el 06/02/2027); misma IP que `ftp.areproyectos.com.mx` |
| Paquete | `deploy/build_hosting_package.php --subdir=cuidadosis` (18 archivos) |
| Commit del código desplegado | el de esta fase (el paquete de la API no cambió desde `c5449f7`) |

## Diagnóstico previo (solo lectura)
| Revisión | Resultado |
|---|---|
| FTP con TLS sobre `ftp.areproyectos.com.mx` | Falla la verificación (`SEC_E_WRONG_PRINCIPAL`): el certificado no incluye ese nombre |
| FTP con TLS sobre `svgil1.cloud-mx-ns.net` | OK con verificación completa; la raíz FTP es la carpeta de la cuenta (`public_html/` dentro) |
| MySQL remoto: `SELECT 1`, `SHOW TABLES` | OK; la base estaba vacía antes de importar |
| PHP del dominio | 8.2.36 (archivo de diagnóstico temporal, borrado al terminar) |
| HTTPS | Certificado válido. En Windows, curl necesita `--ssl-revoke-best-effort` porque Let's Encrypt ya no publica OCSP; la cadena y el nombre sí se verifican |
| Zona horaria | Servidor en UTC. La API respondió con la hora de México (`GET /`, campo `time`) |

## Base de datos
- Se importaron `schema_hosting.sql` y `seed_hosting.sql`. Quedaron 7 tablas: `cd_api_tokens`, `cd_intake_logs`, `cd_link_attempts`, `cd_links`, `cd_medications`, `cd_schedules` y `cd_users`.
- Llaves foráneas: `fk_cd_links_caregiver`, `fk_cd_links_patient`, `fk_cd_medications_patient`, `fk_cd_schedules_medication`, `fk_cd_intake_logs_schedule`, `fk_cd_api_tokens_user` y `fk_cd_link_attempts_user`.
- `seed_hosting.sql` no carga cuentas: después de importar, todas las tablas tenían 0 filas.
- Estado final (último conteo, 15:23, después de borrar las cuentas de prueba): 0 usuarios, 0 vínculos, 0 medicamentos, 0 horarios, 0 tomas, 0 sesiones y 0 intentos.

## Archivos subidos (FTP con TLS)
- `public_html/cuidadosis/api/`: `index.php` y `.htaccess`.
- `cuidadosis_app/`: `src/`, `config/`, `.env.example`, `.htaccess` (niega todo) y `.env` de producción. La carpeta tiene permisos 700 y el `.env` 600.
- Integridad: se descargaron los 16 archivos del paquete y el `.env`, y su SHA-256 coincidió con el local. 0 diferencias.
- `public_html/cuidadosis/descargas/`: `.htaccess` (de `deploy/descargas/`) y `CuidaDosis-1.1.2-beta.apk`.

## Prueba externa de la API con curl por HTTPS (14:49)
| Prueba | Resultado |
|---|---|
| `GET /cuidadosis/api/` | 200, `application/json`, `{"status":"ok",...}` con la hora de México, `Cache-Control: no-store`. Sin desafío ni redirecciones |
| `GET /cuidadosis/api` (sin barra final) | 301 a `/cuidadosis/api/` |
| Registro de paciente y de cuidador sintéticos | 201 y 201 |
| Login de los dos | 200, token de 64 caracteres (no se imprimió) |
| Login con contraseña incorrecta | 401 |
| `GET /me` del paciente | 200 |
| `GET /me` del cuidador en la misma URL | 200 con `role` `caregiver`: la caché de nginx no mezcla respuestas |
| `POST /medications` (1 medicamento, 2 horas) | 201 |
| `GET /intakes/today` | 200 con las 2 tomas pendientes |
| `GET /intakes/today` sin token | 401 |
| `GET /me` con un token falso | 401 |
| `GET /intakes/today` con el rol cuidador | 403 |
| Ruta inexistente | 404 |
| `POST /auth/logout` con `curl -X POST` sin cuerpo | 403 HTML del filtro del servidor (ver hallazgos) |
| El mismo POST con `Content-Length: 0` o con `{}` | Llega a la API (401 con un token falso) |
| Borrado de las cuentas de prueba | 2 filas borradas (en cascada); conteo de nuevo en 0 |

## RNF-07: tiempo de `GET /intakes/today` (meta: 2 s)
Archivo: `rnf07_hosting.csv`. Cinco mediciones desde la PC por HTTPS: 0.343, 0.324, 0.339, 0.322 y 0.337 s. Todas cumplen la meta y el promedio es de 0.333 s. No se midió desde el teléfono por Wi-Fi.

## SEG-05 en el hosting: 22 de 22
Archivo: `seg05_hosting.csv`. Las 22 rutas sensibles de la estructura del hosting (`.env`, `src`, `config`, `bd`, `cuidadosis_app`, `.git`, `deploy` y recorridos con `..` normales y codificados) dieron 403, 404 o 400. Ninguna dio 200 con contenido del proyecto.
- En la primera pasada, sin pausas, 10 rutas recibieron la página anti-bots del hosting (200 HTML "One moment, please..."). Esa página no tiene contenido del proyecto, pero no prueba el bloqueo.
- Las 7 rutas normales se repitieron a las 15:24 con 6 s entre rutas: 403 o 404.
- Las 3 rutas con `..` dieron 400 en la primera pasada: nginx las rechaza antes de llegar al sitio.
- Descargas: `/cuidadosis/descargas/` da 403 (sin listado) y `/cuidadosis/descargas/.htaccess` da 403.

## APK 1.1.2 release
| Concepto | Valor |
|---|---|
| Versión | `versionName` 1.1.2, `versionCode` 4, no debuggable |
| URL incluida | `https://areproyectos.com.mx/cuidadosis/api/` (con la diagonal final) |
| Tráfico sin cifrar | `cleartextTrafficPermitted=false` |
| Firma | v2, un firmante, certificado `CN=CuidaDosis Beta (prototipo academico), O=CuidaDosis, C=MX` |
| Huella SHA-256 del certificado | `6d03a106a1ec6cf59661b86ff111ff8bd6b1e0824eb2b1883045473eaffb29fb` |
| Tamaño | 8 865 914 bytes |
| **SHA-256 del APK** | `1a8e4e39e6389d4996bdea59ef5b760125a0f45b5b0dc35f0c9827207563022d` |
| Pruebas unitarias | 79 de 79, 0 fallos, 0 omitidas (forzadas con `--rerun`, después del último cambio) |
| Descarga pública | `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.2-beta.apk` |

Revisión de la descarga por HTTPS (15:33):
- 200, `Content-Type: application/vnd.android.package-archive`, `Content-Disposition: attachment`.
- 8 865 914 bytes en 2.4 s, con el mismo SHA-256 que el archivo local.

## Prueba en el teléfono físico (POCO M6 Pro, HyperOS)
1. Se desinstaló la 1.1.1 debug (tiene otra firma) y se instaló la 1.1.2 por USB. HyperOS mostró dos avisos:
   - Play Protect preguntó si enviar la app a revisión. Se eligió "No enviar".
   - "Instalar vía USB" pidió confirmar. Se tocó Instalar.
2. **Primera compilación (con R8 parcial):** la app se cerraba al abrir con `IllegalAccessError` (`kotlin.sequences`). Se desactivó R8, se recompiló y se volvieron a correr las 79 pruebas (D49).
3. **Compilación final:**
   - Abre en el login.
   - La línea dice "Servidor: https://areproyectos.com.mx/cuidadosis/api/" y, al tocarla, no pasa nada.
   - Registro de un paciente sintético desde la app: entró a "Hoy" ("Sin tomas programadas hoy", "Última sincronización: 23/11/2026 15:22").
   - "Salir" volvió al login, y en la base el usuario quedó con 0 tokens: el logout de la app sí llega al servidor.
   - Después se borró la cuenta.
4. Al reinstalar se perdió el ajuste de batería de HyperOS. La app lo indica con su tarjeta, y hay que volver a poner "Sin restricciones" antes de probar la puntualidad.

## Hallazgos
- **HTTP sin redirección:** `http://areproyectos.com.mx/cuidadosis/api/` responde 200 por HTTP sin cifrar. La app solo usa HTTPS. Se corrige en cPanel ("Force HTTPS Redirect"), no en el `.htaccess`: detrás de nginx, una regla con `%{HTTPS}` podría causar un ciclo de redirecciones.
- **Filtro del servidor con POST sin `Content-Length`:** responde 403 en HTML. La app envía `Content-Length: 0` y no se ve afectada.
- **Protección anti-bots y firewall:**
  - Después del barrido SEG-05 sin pausas y de unas 40 conexiones FTP seguidas para comparar hashes, la IP de la PC quedó bloqueada.
  - Por HTTPS recibió la página de desafío unos 25 minutos. MySQL tardó unos 30 minutos en volver y FTP unos 40.
  - Hay que hacer las verificaciones despacio y reutilizar la conexión FTP.

## No probado
- Avisos, alarmas, vinculación y la vista del cuidador con la 1.1.2 contra el hosting. Los hace el usuario desde la app.
- RNF-07 desde el teléfono por datos móviles o Wi-Fi.
- Comportamiento de la protección anti-bots con varios teléfonos de la beta detrás de la misma IP.
- R8 completo en release.
