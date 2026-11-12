# Decisiones de la Fase 5

Fase 5: aviso local 5 minutos antes de cada toma (alarma en Android). No cambia la API ni la base de datos.

Las decisiones D1 a D4 se consultaron con el equipo antes de escribir código. D5 a D10 son decisiones de implementación tomadas dentro del plan aprobado.

## D1. Alarma exacta con `USE_EXACT_ALARM`
- **Duda:** desde Android 14, `SCHEDULE_EXACT_ALARM` ya no se concede por defecto, y una alarma inexacta puede retrasarse varios minutos por Doze (incluso después de la hora de la toma).
- **Opciones:** (a) `USE_EXACT_ALARM`, que se concede al instalar; (b) `SCHEDULE_EXACT_ALARM` con una tarjeta para habilitarlo en Ajustes y alarma inexacta de respaldo; (c) solo alarma inexacta.
- **Elección:** (a), con `setExactAndAllowWhileIdle`. Si el sistema reportara que no se pueden programar alarmas exactas, se usa `setAndAllowWhileIdle` como respaldo.
- **Motivo:** el aviso tiene que llegar a tiempo y el usuario no tiene que activar nada. Google Play limita `USE_EXACT_ALARM` a apps de alarmas y recordatorios, pero este prototipo no se publica.

## D2. Texto del aviso y copia local cifrada
- **Duda:** qué muestra la notificación y qué se guarda en el teléfono para reprogramar sin red (por ejemplo, al reiniciar).
- **Opciones:** (a) nombre y dosis, con copia local cifrada; (b) nombre y dosis, sin cifrar; (c) texto genérico, guardando solo las horas.
- **Elección:** (a). Título "Toma de las HH:MM", una línea por medicamento "Nombre: dosis" y el resumen "En 5 minutos". La copia (`reminders.xml`) va cifrada con AES-GCM y la clave de Android Keystore, y está excluida de respaldos.
- **Motivo:** el paciente necesita saber qué le toca. Nombre y dosis se muestran tal como los escribió; la app no los interpreta ni sugiere nada.

## D3. Se avisa aunque la toma ya esté marcada
- **Duda:** si el paciente marcó "Tomado" antes del aviso.
- **Opciones:** (a) avisar siempre; (b) consultar la API al sonar y omitir las tomas registradas.
- **Elección:** (a).
- **Motivo:** funciona sin red y sin depender del servidor. Al abrir el aviso, "Hoy" muestra si la toma ya está registrada.

## D4. Tocar el aviso abre "Hoy"; sin botón "Tomado" en la notificación
- **Opciones:** (a) solo abrir la app; (b) agregar un botón "Tomado" que registre la toma directo.
- **Elección:** (a).
- **Motivo:** mantiene la confirmación de la Fase 4 (D3 de esa fase): la toma no se puede deshacer.

## D5. Una sola alarma pendiente
- **Duda:** programar una alarma por cada hora de cada medicamento o solo la siguiente.
- **Elección:** solo la del próximo aviso. Al sonar, muestra el aviso y programa el siguiente. Los medicamentos con la misma hora salen en un solo aviso.
- **Motivo:** cancelar o reprogramar es trivial (un solo `PendingIntent`) y no hay alarmas huérfanas de medicamentos desactivados.

## D6. Horas en `America/Mexico_City`
- **Duda:** el teléfono puede estar en otra zona (el emulador está en UTC).
- **Elección:** "08:00" significa las 08:00 de México, igual que la API y que D5 de la Fase 4. El momento del disparo se calcula como instante absoluto, así que la alarma suena a la hora correcta de México en cualquier zona.

## D7. Cuándo se reprograma y cuándo se cancela
- **Se reprograma:** al iniciar sesión o abrir la app como paciente (se descarga la lista de medicamentos), cada vez que se carga la lista, al guardar o desactivar un medicamento, al sonar un aviso, y con `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` y `TIMEZONE_CHANGED` (estas usan la copia local, sin red).
- **Se cancela** (alarma, copia local y notificaciones): al cerrar sesión, cuando el token vence o es revocado (respuesta 401) y al entrar como cuidador. El cuidador nunca programa avisos.

## D8. Toma cercana y avisos repetidos
- **Si faltan menos de 5 minutos** (por ejemplo, se da de alta a las 09:58 un medicamento de las 10:00), el aviso sale de inmediato.
- **No se repite un aviso:** se guarda el conjunto de tomas ya avisadas (fecha y hora) y se excluyen al calcular el siguiente. Se usa un conjunto y no solo "la última avisada" porque, en la prueba, un medicamento nuevo a las 09:56 quedaba sin aviso cuando ya se había avisado el de las 09:58.
- **Alarma muy atrasada** (teléfono apagado): si llega más de 10 minutos después de la hora de la toma, ya no se muestra.

## D9. Permiso de notificaciones
- En Android 13 o superior se pide `POST_NOTIFICATIONS` al entrar a la pantalla del paciente. Si está negado, "Hoy" muestra una tarjeta con el botón "Activar notificaciones", que abre los ajustes de notificaciones de la app.

## D10. Cifrado compartido
- El cifrado AES-GCM de `SessionStore` se movió a `LocalCipher` y lo usan la sesión y la copia de los avisos. Conserva el mismo alias de clave, así que las sesiones guardadas antes de esta fase siguen abriendo.

## Pruebas realizadas
- 11 pruebas unitarias nuevas (`ReminderScheduleTest`) del cálculo del próximo aviso: hoy, cambio de día, aviso inmediato, agrupación por hora, teléfono en UTC, cerca de medianoche, tomas ya avisadas y lista vacía. En total pasan 32 pruebas unitarias.
- Emulador `Medium_Phone_API_37.0` (Android 17, zona UTC) con un paciente sintético nuevo ("Paciente SINTETICO F5", `pf5095012@ejemplo.test`) y medicamentos sintéticos, sin resetear la base de datos:
  - Al iniciar sesión se programó una alarma exacta (`dumpsys alarm`: `window=0`, `exactAllowReason=policy_permission`) para las 15:53 UTC, es decir 5 minutos antes de la toma de las 09:58 de México. Sonó a esa hora con la app en segundo plano y mostró un solo aviso con los dos medicamentos de esa hora.
  - Tocar el aviso abrió la pestaña "Hoy".
  - Medicamento nuevo a 3 minutos: aviso inmediato.
  - Tras `adb reboot`, el sistema arrancó la app por `BootReceiver` y se reprogramó el aviso siguiente.
  - Al desactivar un medicamento desde la app, el aviso de su hora desapareció y el siguiente pasó al día siguiente.
  - Al cerrar sesión y al entrar como cuidador: ninguna alarma de la app y copia local vacía.
  - Con el permiso de notificaciones negado: aparece la tarjeta y su botón abre los ajustes de notificaciones.
  - La copia local `reminders.xml` contiene solo texto cifrado.
- **No probado:** teléfono físico (Doze real y fabricantes con ahorro de energía agresivo), `TIME_SET` y `TIMEZONE_CHANGED` (el emulador no permite cambiar la hora sin root), y el alta o edición desde el formulario de la app (usa el mismo camino que la desactivación, que sí se probó; el alta se hizo por la API).
