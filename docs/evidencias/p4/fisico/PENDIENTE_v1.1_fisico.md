# Pendiente: verificación de la v1.1 en el teléfono físico

Lista exacta de lo que falta ejecutar en el POCO M6 Pro (Android 16, HyperOS) con la versión 1.1.0. **Nada de esto se ha ejecutado.** Los resultados de la v1.0 (`puntualidad.csv`, `reinicio.csv` y los demás de esta carpeta) no se modifican: la v1.1 se registra en archivos nuevos con sufijo `_v1.1`.

Reglas: solo datos sintéticos; usar siempre `-s <ID del teléfono>`; el PIN no se escribe en ningún archivo (poner [PIN]); en las pruebas con pantalla apagada no despertar la pantalla hasta medir; si una medición se interrumpe, repetirla desde cero.

## 0. Preparación
- [ ] Red estable: desactivar en el teléfono la conexión automática a la otra red Wi-Fi guardada, para que se quede en la red de la PC durante toda la sesión (en la v1.0 ese cambio de red causó fallos de conexión).
- [ ] Compilar e instalar la v1.1 (desde la raíz del repo, Git Bash):
  ```bash
  cd android && export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" && ./gradlew.bat --stop && ./gradlew.bat assembleDebug -Dorg.gradle.java.installations.auto-detect=false "-Dorg.gradle.java.installations.paths=C:/Program Files/Android/Android Studio/jbr" && cd ..
  sha256sum android/app/build/outputs/apk/debug/app-debug.apk
  "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s <ID> install -r "C:/xampp/htdocs/CuidaDosis/android/app/build/outputs/apk/debug/app-debug.apk"
  "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s <ID> shell dumpsys package mx.cuidadosis.app | grep -E "versionName|versionCode="
  ```
  Esperado: `versionName=1.1.0`, `versionCode=2`.
- [ ] Comprobar canales nuevos (y que los de la v1.0 se eliminaron):
  ```bash
  "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s <ID> shell dumpsys notification | grep -E "NotificationChannel\{mId='(intake|caregiver)" | grep -oE "mId='[^']*'|mImportance=[0-9]|mVibrationEnabled=[a-z]+|usage=[A-Z_]+" | paste - - - -
  ```
  Esperado: `intake_before_v2`, `intake_alarm_v2` (`USAGE_ALARM`), `intake_alarm_silent_v2`, `caregiver_alerts_v2`, con vibración en todos menos el silencioso; sin `intake_reminders` ni `caregiver_alerts`.
- [ ] Mantener la configuración de HyperOS de la v1.0: batería "Sin restricciones", Autoencendido activado; además permitir "Mostrar en pantalla de bloqueo" y "Ventanas emergentes" para la pantalla de alarma, y volumen de alarma mayor que cero.
- [ ] Entrar como `ptel1121@ejemplo.test` (paciente).

## 1. Puntualidad con pantalla apagada (RNF-01, 10 tomas)
- [ ] Crear por API un medicamento sintético con 10 horas, una por minuto, empezando 12 minutos después de la hora actual (como en la v1.0 con `POST /medications`).
- [ ] Abrir la pestaña Medicamentos para sincronizar y comprobar que hay una alarma por aviso:
  ```bash
  "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s <ID> shell dumpsys alarm | grep -A2 "mx.cuidadosis.app" | grep -c "origWhen="
  ```
- [ ] `logcat -c`, apagar la pantalla (`shell input keyevent 26`) y no tocar el teléfono hasta 2 minutos después de la última toma.
- [ ] Al terminar, sin despertar la pantalla, guardar:
  ```bash
  "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s <ID> logcat -d -v time | grep -E "sending alarm .*mx.cuidadosis.app|acquireWakeLockInternal.*post:mx.cuidadosis" > docs/evidencias/p4/fisico/puntualidad_B_v1.1_log_sistema.log
  ```
- [ ] Registrar en `puntualidad_v1.1.csv`: hora esperada (T-5 y T), hora real de publicación, diferencia y si cumple ±60 s. Meta RNF-01: al menos 9 de 10 por tipo de aviso.

## 2. Reinicio (RNF-03, 2 tomas)
- [ ] Dos tomas a 10 y 12 minutos. Sincronizar y `adb -s <ID> reboot` sin abrir la app.
- [ ] Antes de desbloquear: `dumpsys alarm` (se espera que no haya alarmas de la app: Android no entrega `BOOT_COMPLETED` antes del primer desbloqueo; ver D42).
- [ ] Desbloquear con [PIN] sin abrir la app y repetir `dumpsys alarm` cada 20 s hasta ver las alarmas; anotar cuánto tardó.
- [ ] Anotar si sonaron el aviso previo y la alarma, y a qué hora (log del sistema), en `reinicio_v1.1.csv`.

## 3. PR-03, PR-04 y PR-11 con la alarma
- [ ] PR-03: con una toma a 7 min, verificar con `dumpsys notification --noredact` el aviso previo (canal `intake_before_v2`) y la alarma a T (canal `intake_alarm_v2`, banderas `ONGOING_EVENT|INSISTENT`, hora `when`).
- [ ] PR-04: en la alarma, tocar "Tomado" una vez (en la pantalla de alarma o en la notificación); comprobar que no hay diálogo, que la notificación desaparece, que deja de sonar y que la BD tiene un registro con `client_uuid`.
- [ ] PR-11: en otra toma, "Posponer 5 min" (deja de sonar, vuelve a sonar al plazo) y luego "Posponer 10 min" (igual); al final "Tomado".
- [ ] Toma confirmada antes de T: la alarma no suena.
- [ ] Límite de 2 minutos: dejar una alarma sin atender y comprobar que a los 2 minutos la notificación pasa al canal `intake_alarm_silent_v2` sin `INSISTENT`.

## 4. Sonido y vibración (solo persona)
- [ ] Con el teléfono en la mano, confirmar que la alarma **suena** (volumen de alarma) y **vibra** de forma repetida, y que se detiene con Tomado o Posponer. El emulador no permite comprobarlo (ver `docs/evidencias/p4/emulador_v1.1/RESUMEN.md`).
- [ ] Repetir con el modo vibración del teléfono y con No molestar activo (con y sin "Alarmas" permitidas) y anotar qué pasa.
- [ ] Comprobar que el aviso previo y la notificación del cuidador vibran una vez y no se repiten.
- [ ] Comprobar si la pantalla de alarma aparece sola con el teléfono bloqueado (pantalla completa) o solo al tocar la notificación.

## 5. PR-12 (alarma del cuidador a T + 30)
- [ ] Con la red estable: teléfono como cuidador (`ctel1121@ejemplo.test`), paciente sintético por API, toma sin confirmar y esperar T + 30 con la pantalla apagada.
- [ ] Esperado: notificación "Toma sin confirmar" en el canal `caregiver_alerts_v2`, que suena y vibra una vez, sin repetir. En la v1.0 solo se pudo observar la rama "sin conexión".

## 6. Comparación v1.0 contra v1.1
- [ ] Tabla en `docs/evidencias/p4/fisico/comparacion_v1.0_v1.1.md` con los mismos casos: puntualidad con pantalla apagada (v1.0: 6 de 10), reinicio (v1.0: 2 de 4 avisos a tiempo, reprogramación tras desbloquear), PR-12. Indicar condiciones iguales (cable, batería, red) o las diferencias.
