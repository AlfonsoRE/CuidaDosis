# Cómo continuar

Estado al 22/11/2026. Detalle completo en `RESUMEN_FASE6.md` (sección "Estado al cierre del 22 de noviembre").

## Dónde estamos
- Fases 0 a 10 hechas. Versión **1.1.1** (`versionCode` 3), la del grupo beta.
- APK debug: `android/app/build/outputs/apk/debug/app-debug.apk` (no se versiona). SHA-256 `157fcf454f381fa2daaadd7712a0cd0b86ee1cbe1d080062e9dd6ccd0f3d2938`.
- La v1.1 cumple la puntualidad en el teléfono físico (20 de 20 con pantalla apagada; la v1.0 dio 6 de 10).
- El teléfono quedó sin sesión y sin medicamentos de prueba activos.
- 23/11/2026 (Fase 11): todas las tablas llevan el prefijo `cd_` y la API se puede servir en `/cuidadosis/api/` (`APP_BASE_PATH`, paquete con `--subdir=cuidadosis`). Ver D46 a D48. La BD local se regeneró con el esquema nuevo y el seed. Si una copia local aún tiene las tablas sin prefijo, ver `docs/instalacion.md` 1.6.

## Qué falta
Lista por prioridad en `docs/PENDIENTES_FASE6.md`. Lo primero:
1. PR-12 con el teléfono en mano: `docs/evidencias/p4/fisico/PENDIENTE_v1.1_fisico.md`, sección 1.
2. Sonido, vibración, modo silencio y No molestar (sección 2 del mismo archivo).
3. Pantalla de alarma sobre el bloqueo con la pantalla apagada.

## Comandos útiles (Git Bash, desde la raíz del repo)
```bash
# Compilar el APK y correr las pruebas unitarias (79)
cd android && ./gradlew.bat assembleDebug testDebugUnitTest && cd ..
sha256sum android/app/build/outputs/apk/debug/app-debug.apk

# Pruebas de la API (XAMPP con Apache y MySQL encendidos)
/c/xampp/php/php.exe api/tests/run_api_tests.php
/c/xampp/php/php.exe api/tests/route_path_test.php

# Paquete para areproyectos.com.mx/cuidadosis/ (docs/instalacion.md 3.1.1)
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
