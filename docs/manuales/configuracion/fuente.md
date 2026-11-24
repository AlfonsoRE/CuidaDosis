# Manual de configuración de CuidaDosis 1.1.3 (fuente verificada)

Fuente para redactar el manual de configuración del servidor y del teléfono. Cada paso indica la acción, el resultado esperado, la captura (en `capturas/` de esta carpeta) y qué hacer si falla. Corresponde al hosting `https://areproyectos.com.mx/cuidadosis/` y a la app 1.1.3 del 23/11/2026. Lo que no se pudo comprobar dice **no verificado**.

Nunca se escriben valores secretos; se usan marcadores como `<USUARIO_BD>` y `<CONTRASEÑA_BD>`.

## 1. Archivo `.env` del servidor
Vive en `/home/<CUENTA_CPANEL>/cuidadosis_app/.env`, fuera de la carpeta web, con permisos 600. Se crea a partir de `.env.example` del paquete (plantilla `api/.env.hosting.example` en el repositorio). Nunca se sube al repositorio.

| Variable | Valor en el hosting | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | La API se conecta desde el mismo servidor |
| `DB_PORT` | `3306` | Puerto de MySQL o MariaDB |
| `DB_NAME` | `<NOMBRE_BD>` | Base asignada por el panel (puede ser compartida) |
| `DB_USER` | `<USUARIO_BD>` | Usuario con privilegios sobre esa base |
| `DB_PASS` | `<CONTRASEÑA_BD>` | Contraseña del usuario |
| `TOKEN_TTL_HOURS` | `720` | Vigencia de la sesión: 30 días |
| `APP_DEBUG` | `false` | Siempre `false` en un servidor público; `true` agrega detalles técnicos a los errores 500 |
| `APP_BASE_PATH` | `/cuidadosis/api` | Ruta pública de la API cuando no está en la raíz del dominio |

Pasos:
1. **Llenar el `.env`.**
   - Acción: copiar `.env.example` a `.env` dentro de `cuidadosis_app/` y escribir los valores del panel.
   - Resultado esperado: `GET https://areproyectos.com.mx/cuidadosis/api/` responde `{"status":"ok",...}`.
   - Si falla: con datos de base incorrectos la API responde 500 "Error interno del servidor."; revisar `DB_*`. Con un `APP_BASE_PATH` incorrecto todas las rutas dan 404 (si queda vacío, la API lo deduce de la ubicación de `index.php`, que en este hosting da lo mismo).
2. **Proteger el archivo.**
   - Acción: permisos 600 al `.env` y 700 a `cuidadosis_app/` (en FTP: `SITE CHMOD`).
   - Resultado esperado: `https://areproyectos.com.mx/cuidadosis_app/.env` responde 403 o 404.

### 1.1 Zona horaria
- La API fija `America/Mexico_City` en PHP y calcula el desfase para la sesión de MySQL en cada conexión. No depende de la zona del servidor ni se configura en el `.env`.
- Verificado: el servidor está en UTC y `GET /cuidadosis/api/` devuelve la hora de México en el campo `time`.
- La app también calcula las horas en `America/Mexico_City`, sin importar la zona del teléfono. No se soporta viajar entre zonas horarias.

### 1.2 Valores fijos en el código (no se configuran)
- Código de invitación: 8 caracteres, 10 minutos, un solo uso; bloqueo (429) tras 5 intentos fallidos en 10 minutos.
- "Sin confirmar": pendiente y más de 30 minutos después de la hora.
- Historial de 7 días; lotes de sincronización de hasta 100 confirmaciones.
- `sql_mode` estricto en cada conexión.

## 2. Seguridad del servidor
| Qué | Cómo se protege | Comprobación (23/11/2026) |
|---|---|---|
| `.env` | Fuera de `public_html`, permisos 600 | 403 o 404 |
| `src/` y `config/` | En `cuidadosis_app/`, fuera de la web, con un `.htaccess` que niega todo | 403 o 404 |
| `bd/` (scripts SQL) | No se suben al servidor | 404 |
| Rutas con `..` | El `.htaccess` y el servidor las rechazan | 400 o 404 |
| Archivos ocultos en `descargas/` | `.htaccess` los niega y desactiva el listado | Regla del `.htaccess` (no se probó cada archivo) |
| Resumen | 22 rutas sensibles | 22 de 22 con 403, 404 o 400 (`docs/evidencias/p4/hosting/seg05_hosting.csv`) |
| Contraseñas | `password_hash` en `cd_users`; los tokens se guardan solo como hash | Revisado en el código |
| Respuestas de la API | `Cache-Control: no-store` (nginx no las guarda en caché) | Dos cuentas distintas reciben su propio `/me` |
| HTTP sin cifrar | Debería redirigir a HTTPS | **Pendiente:** activar "Force HTTPS Redirect" en cPanel |

Se recomienda, además:
- Cambiar las contraseñas de FTP y de la base si se compartieron o se expusieron, y actualizar el `.env`.
- Respaldar fuera de la computadora el keystore de firma de la app (sin él no se pueden publicar actualizaciones que se instalen encima).
- Quitar de "Remote MySQL" las IP que ya no se usen.

## 3. Versión release y versión debug de la app
| | Release (grupo beta) | Debug (equipo) |
|---|---|---|
| Servidor | Fijo: `https://areproyectos.com.mx/cuidadosis/api/`. El login muestra "Servidor: ..." como texto y no se puede cambiar (`BuildConfig.ALLOW_SERVER_CHANGE = false`) | Configurable en el login, enlace **Servidor**: escribir la URL, **Probar conexión** y **Guardar** |
| HTTP sin cifrar | No permitido | Permitido para la red local |
| Permiso de red local (Android 17) | No se pide | Se pide si el servidor está en la red local |
| Firma | Keystore del proyecto, guardado en `.secrets/` (no se versiona) | Clave de depuración |
| Distribución | `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.3-beta.apk` | Solo el equipo, por USB |

Captura del login de la release: `33_inicio_sesion_release_v113.png`. La pantalla Servidor solo existe en debug (`24_servidor_url_generica_v11.png`, de referencia para el equipo).

Para apuntar la release a otro servidor hay que cambiar `releaseApiBaseUrl` en `android/app/build.gradle.kts`, subir `versionCode` y publicar otro APK firmado con el mismo keystore.

## 4. Ajustes del teléfono
Verificados el 23/11/2026 en el POCO M6 Pro (HyperOS, Android 16) después de reinstalar la 1.1.3. En otros fabricantes los nombres cambian.

1. **Permitir notificaciones.**
   - Acción: al iniciar sesión, en "¿Quieres permitir que CuidaDosis te envíe notificaciones?" tocar **Permitir**.
   - Resultado esperado: no aparece la tarjeta roja "Los avisos pueden no sonar a tiempo".
   - Captura: `33b_permiso_notificaciones_v113.png`.
   - Si falla: si se tocó "No permitir", la tarjeta roja de "Hoy" lleva a los ajustes (`06_permisos.png`); también Ajustes, Aplicaciones, CuidaDosis, Notificaciones.

2. **Alarmas y recordatorios (alarmas exactas).**
   - La app usa `USE_EXACT_ALARM`, que Android concede al instalar; no hay que activarlo.
   - Comprobación: en Ajustes de la app (paciente), "Permisos" muestra todo en orden (`05_ajustes_anticipacion.png`).
   - Si falla: la tarjeta de permisos lo indica.

3. **Notificaciones con sonido, vibración, pantalla de bloqueo y flotantes.**
   - Acción: Ajustes, Aplicaciones, CuidaDosis, Notificaciones. Dejar activados "Mostrar notificaciones", "Notificaciones flotantes", "Notificaciones en la Pantalla de bloqueo", "Sonido" y "Vibración", y las 4 categorías (Aviso antes de la toma, Alarma de la toma, Toma pendiente sin sonido, Tomas sin confirmar).
   - Capturas: `hyperos_notificaciones_categorias.png`, `hyperos_notificaciones_categorias_2.png`.
   - Las categorías aparecen después del primer inicio de sesión.

4. **Notificaciones a pantalla completa.**
   - Acción: pantalla "Notificaciones de pantalla completa" de CuidaDosis: "Permitir notificaciones a pantalla completa desde esta aplicación" activado. En el teléfono del equipo se abrió con el acceso directo del sistema; la ruta exacta del menú de HyperOS no se verificó.
   - Resultado esperado: la pantalla de alarma puede abrirse sobre el bloqueo.
   - Captura: `hyperos_pantalla_completa_v113.png`. En el teléfono del equipo venía activado tras instalar.

5. **Otros permisos de HyperOS.**
   - Acción: Info. de la aplicación, Otros permisos: "Mostrar en pantalla de bloqueo" y "Abrir nuevas ventanas mientras se ejecuta en segundo plano" en **Permitir siempre**.
   - Captura: `hyperos_otros_permisos_v113.png`. En el teléfono del equipo venían permitidos tras instalar.

6. **Batería sin restricciones.**
   - Acción: Info. de la aplicación, Encendido (Uso de la batería), Ahorro de batería: **Sin restricciones**.
   - Resultado esperado: la opción queda marcada en azul con una palomita.
   - Captura: `hyperos_bateria_sin_restricciones_v113.png`.
   - Si falla: con "Ahorro de batería (recomendado)", HyperOS retrasó los avisos en las pruebas de la v1.0. Tras reinstalar, vuelve a esta opción y hay que cambiarla otra vez.

7. **Inicio automático.**
   - Acción: app Seguridad, Autoencendido en segundo plano (Inicio automático), buscar "Cuida" y activar **CuidaDosis**.
   - Resultado esperado: el interruptor queda activado.
   - Captura: `hyperos_inicio_automatico_v113.png`.
   - Si falla: sin este ajuste la app puede no reprogramar los avisos al reiniciar el teléfono. Después de reinstalar queda desactivado (comprobado el 23/11/2026).

8. **"Pausar la actividad de la aplicación si no se utiliza".**
   - Viene activado. Se recomienda desactivarlo en Info. de la aplicación para que Android no quite permisos si la app pasa meses sin abrirse. En el teléfono del equipo se dejó como estaba (activado).

9. **Volumen de alarma.**
   - Acción: Ajustes, Sonido y vibración, volumen **Alarma** por encima de cero.
   - Resultado esperado: la alarma de la toma suena con ese volumen (no con el de llamadas ni el de notificaciones). El aviso previo usa el volumen de notificaciones, que en HyperOS va unido al del tono de llamada.
   - Con No molestar, la alarma solo suena si las alarmas están permitidas en No molestar. **No verificado** en el teléfono (tampoco el modo silencio ni el modo vibración).

10. **Anticipación del aviso (paciente).**
    - Acción: en la app, **Ajustes**, elegir 5 (por defecto) o 10 minutos.
    - Captura: `05_ajustes_anticipacion.png`. El ajuste solo se guarda en el teléfono.

## 5. Problemas frecuentes (observados en el proyecto)
- **La IP queda bloqueada por la protección anti-bots del hosting:** página "One moment, please..." y FTP y MySQL sin conexión durante unos 15 minutos tras muchas peticiones seguidas. Esperar y repetir despacio.
- **MySQL remoto sin IP autorizada:** al cambiar de red, la nueva IP no está en "Remote MySQL" de cPanel. Agregarla o usar phpMyAdmin.
- **FTP con TLS:** el certificado es del nombre del servidor (`svgil1.cloud-mx-ns.net`), no de `ftp.<dominio>`. Usar ese nombre; no desactivar la verificación.
- **R8:** la optimización cerraba la release al abrir; quedó desactivada (D49).
- **Permisos de HyperOS tras reinstalar:** batería e Inicio automático vuelven a su valor por defecto; repetir los pasos 6 y 7.
- **Inicio automático desactivado:** los avisos pueden no reprogramarse tras reiniciar.
- **"Pausar la actividad de la aplicación si no se utiliza":** activado por defecto; desactivarlo.
- **Las notificaciones no suenan:** revisar el permiso (paso 1), las categorías (paso 3) y los volúmenes (paso 9).
