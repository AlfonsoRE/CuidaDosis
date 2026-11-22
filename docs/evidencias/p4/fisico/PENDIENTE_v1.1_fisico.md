# Pendiente: verificación de la v1.1 en el teléfono físico

Estado al 21/11/2026, 23:35 (Fase 10). El teléfono es el POCO M6 Pro (Android 16, HyperOS) con la **1.1.1** instalada (`versionCode` 3), sin sesión abierta y sin alarmas de la app. Los resultados de la v1.0 de esta carpeta no se modificaron; los de la v1.1 están en los archivos con sufijo `_v1.1`.

Reglas: solo datos sintéticos; usar siempre `-s <ID del teléfono>`; el PIN no se escribe en ningún archivo (poner [PIN]).

## Hecho en la Fase 10 (automatización por adb, sin persona)
- [x] Instalación, permisos y ajustes de HyperOS (`00_instalacion_v1.1.md`).
- [x] Puntualidad con pantalla apagada y encendida, 20 de 20 cada una (`01_puntualidad_v1.1.md`).
- [x] Reinicio: sin desbloquear no se reprograma; tras desbloquear, 3 de 4 avisos a tiempo (`02_reinicio_v1.1.md`).
- [x] PR-03, PR-04 (notificación y pantalla de alarma), PR-11 (Posponer 5 y 10), límite de 2 minutos con la 1.1.1 y cierre de sesión con pendientes (`03_alarma_pr03_pr04_pr11_v1.1.md`).
- [x] Capturas para los manuales (`docs/capturas/LISTA.md`).

## 1. PR-12: alarma del cuidador a T + 30 (PREPARADO, NO EJECUTADO)
Lo ejecuta el usuario con el teléfono en la mano para oírla. Todo está listo:
- Cuidador `ctel1121@ejemplo.test` con vínculo activo con "Paciente SINTETICO Alarma" (`palarma1121@ejemplo.test`); contraseña de prueba `Sintetico2026`.
- `palarma1121` no tiene medicamentos activos (el de las capturas, "Medicamento SINTETICO Cuidador v11", quedó desactivado).
- La alarma del cuidador se programa al iniciar sesión y al cargar o actualizar "Mis pacientes", para cada toma cuyo T + 30 todavía no pasó; usa el canal `caregiver_alerts_v2` (notificación con sonido de notificación y vibración, **una vez**, sin repetir). Su volumen es el de notificaciones, no el de alarma.

Pasos (Git Bash en la PC, desde la raíz del repo; XAMPP con Apache y MySQL encendidos):
1. Teléfono en el Wi-Fi de la PC, volumen de notificaciones alto, sin modo silencio. Comprobar la red:
   ```bash
   A="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"; ID=<ID del teléfono>
   "$A" -s $ID shell cmd wifi status | grep -oE 'SSID: "[^"]*"' | head -1
   ```
2. En el teléfono: abrir CuidaDosis e iniciar sesión como `ctel1121@ejemplo.test`.
3. Crear la toma. Elegir **T = hora actual menos 25 minutos** (formato HH:MM, hora de México), para que T + 30 caiga 5 minutos después:
   ```bash
   B=http://localhost/CuidaDosis/api/public; T=HH:MM
   TA=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' -d '{"email":"palarma1121@ejemplo.test","password":"Sintetico2026"}' | /c/xampp/php/php.exe -r 'echo json_decode(stream_get_contents(STDIN),true)["token"];')
   curl -s -X POST $B/medications -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d "{\"name\":\"Medicamento SINTETICO PR12\",\"dose\":\"1 tableta (dato de prueba)\",\"times\":[\"$T\"]}"; echo
   curl -s $B/intakes/today -H "Authorization: Bearer $TA" | grep -o "\"time\":\"$T\"[^}]*"; echo
   date "+publicado %H:%M:%S"
   ```
   Anotar la hora "publicado" y la esperada (T + 30). La toma debe salir `pending`.
4. En el teléfono: en "Mis pacientes" tocar **Actualizar** y comprobar que hay alarma del cuidador:
   ```bash
   "$A" -s $ID shell dumpsys alarm | grep -A2 mx.cuidadosis.app | grep -oE 'origWhen=[^ ]+'
   ```
   Debe aparecer `origWhen` con la fecha de hoy y la hora T + 30.
5. Apagar la pantalla (botón de encendido) y **no tocar** el teléfono. A la hora T + 30 debe sonar y vibrar **una vez** la notificación "Toma sin confirmar: Paciente SINTETICO Alarma" (texto: "Medicamento SINTETICO PR12 de las HH:MM sigue sin confirmar…"). Si el teléfono no llega al servidor al consultar (15 s de espera), el título es "Revisa a Paciente SINTETICO Alarma" (rama sin conexión, la única observada en la v1.0).
6. Guardar la hora de publicación sin despertar la pantalla:
   ```bash
   "$A" -s $ID shell dumpsys notification --noredact | grep -A40 "pkg=mx.cuidadosis.app" | grep -E "channel=|when=|android.title=|android.text="
   ```
   `when` está en milisegundos desde 1970 (UTC); diferencia = `when` - hora esperada.
7. Anotar en `docs/evidencias/p4/fisico/pr12_v1.1.md`: T, T + 30, publicación de la notificación, título y texto, canal, si sonó y vibró una sola vez, y quién la observó. Captura opcional del panel **recortada** a la notificación (sin notificaciones de otras apps).
8. Limpieza: desactivar el medicamento (`DELETE /medications/<id>` con `$TA`) y cerrar sesión del cuidador en el teléfono.

## 2. Sonido, vibración y pantalla (solo persona)
- [ ] La alarma a T **suena** con el volumen de alarma y **vibra** de forma repetida, y se detiene con Tomado o Posponer, o sola a los 2 minutos.
- [ ] Repetir con el modo vibración y con No molestar activo (con y sin "Alarmas" permitidas) y anotar qué pasa.
- [ ] El aviso previo vibra una vez y no se repite.
- [ ] Pantalla de alarma **sola sobre el bloqueo** con la pantalla apagada: en la Fase 10 HyperOS solo encendió la luz de bordes y dejó la notificación; con la pantalla encendida y bloqueada sí se abrió sola (paso 1, 22:25 a 22:29). Confirmarlo a mano y, si aparece, capturarla para el manual.

## 3. Comparación v1.0 contra v1.1
Hecha en `RESUMEN.md` de esta carpeta (falta solo la fila de PR-12).
