# Cómo continuar

Estado al cierre del lunes 23/11/2026. Detalle en D46 a D50 (`decisiones-fase-6.md`), `docs/evidencias/p4/hosting/DESPLIEGUE.md` y `docs/evidencias/p4/INDICE.md`.

## Dónde estamos
- Fases 0 a 12 hechas. Versión **1.1.3** (`versionCode` 5, ícono propio, D50). Es un release firmado con servidor fijo `https://areproyectos.com.mx/cuidadosis/api/` (D49).
- **Desplegada** en `https://areproyectos.com.mx/cuidadosis/` (API en `/cuidadosis/api/`). El grupo beta ya usa ese hosting.
- **APK publicado:** `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.3-beta.apk`. SHA-256 `08c66b382cc6c88b4c56f646fc86b100f4bc8eb97686eaa149e1b68fc8af71b5`. La 1.1.2 se borró del servidor.
- **BD del hosting:** 7 tablas `cd_*` en la base compartida, sin cuentas de demo. Al cierre tenía 5 cuentas (del grupo beta y del equipo); las cuentas sintéticas P00 de las capturas se borraron.
- **Teléfono del equipo (POCO M6 Pro):**
  - 1.1.3 instalada, con una sesión de cuidador abierta.
  - Batería "Sin restricciones" activa.
  - **Inicio automático desactivado:** activarlo antes de la prueba sin cable.
  - Quedó bloqueado y sin `adb`.
- **Manuales:** fuente actualizada a la 1.1.3 en `docs/MANUALES_FUENTE.md`. Vigencia de capturas y pendientes en `docs/capturas/LISTA.md`.
- **Instrumentos de P4:** plantillas 1.1.3 en `docs/evidencias/p4/instrumentos/`. `manual/` y `beta/` solo tienen su README.
- **Llaves y versiones locales:**
  - El keystore de firma está en `.secrets/` (ignorada por git).
  - El APK debug (1.1.1 y anteriores) sigue sirviendo para pruebas locales con XAMPP.
  - El tag `v1.0-evaluada` existe solo en local.

## Qué falta
1. **Pruebas manuales del equipo** con la 1.1.3 contra el hosting, usando el Complemento manual. El documento lleno va en `docs/evidencias/p4/manual/`.
   - PR-12: `docs/evidencias/p4/fisico/PENDIENTE_v1.1_fisico.md`, sección 1.
   - Sonido, vibración, modo silencio y No molestar.
   - Alarma sobre el bloqueo con la pantalla apagada.
   - Reposo sin cable.
   - Lista por prioridad en `docs/PENDIENTES_FASE6.md`.
2. **Respuestas del grupo beta:** anonimizar (P01, P02...), consolidar en CSV y calcular SUS en `docs/evidencias/p4/beta/`. Los .docx originales no se suben.
3. **Capturas pendientes para los manuales** (instalación desde el navegador, login de la release y notificaciones con el ícono nuevo; lista y nombres en `docs/capturas/LISTA.md`). Después, redactar el manual de instalación y el de usuario con esas capturas.
4. **Entregables:** documento P4, artículo, diapositivas y video.

## Pendientes opcionales de seguridad
- Cambiar las contraseñas de FTP y de la BD del hosting. La de la BD se expuso en la salida de una sesión el 23/11, así que es la más urgente. Después, actualizar `.secrets/hosting.env` y el `.env` del servidor.
- Activar "Force HTTPS Redirect" en cPanel: hoy HTTP responde sin redirigir.
- Respaldar el keystore (`.secrets/cuidadosis-release.jks` y su `.properties`) fuera de la PC. Sin él no hay actualizaciones encima.
- Cambiar el PIN del teléfono, que se compartió para las pruebas, y decidir si se borra `.secrets/phone.env`.
- Quitar la IP de la PC de "Remote MySQL" en cPanel cuando ya no haga falta.

## Comandos útiles (Git Bash, desde la raíz del repo)
```bash
# Compilar el APK y correr las pruebas unitarias (79)
cd android && ./gradlew.bat assembleDebug testDebugUnitTest && cd ..
sha256sum android/app/build/outputs/apk/debug/app-debug.apk

# Pruebas de la API (XAMPP con Apache y MySQL encendidos)
/c/xampp/php/php.exe api/tests/run_api_tests.php
/c/xampp/php/php.exe api/tests/route_path_test.php

# APK release firmado (1.1.3) (docs/instalacion.md 2.4; necesita .secrets/release-signing.properties)
cd android && ./gradlew.bat testDebugUnitTest --rerun assembleRelease && cd ..
sha256sum android/app/build/outputs/apk/release/app-release.apk

# Paquete para areproyectos.com.mx/cuidadosis/ (docs/instalacion.md 3.1.1 y 3.3)
/c/xampp/php/php.exe deploy/build_hosting_package.php --subdir=cuidadosis

# Datos opcionales para la prueba física sin cable (docs/instalacion.md 1.6; no se ejecuta solo)
# mysql ... "$DB_NAME" < bd/preparar_prueba_fisica.sql

# Instalar en el teléfono (siempre con -s; el ID sale en "adb devices")
A="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$A" devices
"$A" -s <ID> install -r android/app/build/outputs/apk/debug/app-debug.apk

# Guardar el log del sistema durante una prueba de puntualidad
"$A" -s <ID> logcat -v time > puntualidad.log

# Recalcular la puntualidad desde el log (fecha AAAA-MM-DD, horas de las tomas)
/c/xampp/php/php.exe api/tests/puntualidad_v11_sysui.php puntualidad.log "B pantalla apagada" 2026-11-21 21:40,21:44
/c/xampp/php/php.exe api/tests/puntualidad_v11_csv.php puntualidad.log "B pantalla apagada" 2026-11-21 21:40,21:44
```
- El emulador (`Medium_Phone_API_37.0`) consume mucha memoria y puede cerrarse solo; enciéndelo solo si hace falta.
- Con emulador y teléfono conectados, `adb` sin `-s` falla o actúa sobre el dispositivo equivocado.

## Qué NO hacer
- No subir `api/.env`, credenciales, tokens, el APK, `build/` ni logs crudos con IP o nombres de red.
- No usar datos reales de personas ni de salud; solo cuentas `@ejemplo.test` y nombres con "SINTETICO".
- No escribir el PIN del teléfono en archivos, capturas ni commits (poner [PIN]).
- No subir etiquetas (`git push --tags`) ni hacer `push --force` sin pedirlo.
- No publicar capturas con notificaciones de otras apps, correos personales o la IP de la PC.
