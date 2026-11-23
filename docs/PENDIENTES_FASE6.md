# Pendientes y bloqueos

Estado al 23/11/2026 (versión 1.1.3, desplegada en areproyectos.com.mx; D49 y D50). Ordenado por prioridad: primero lo que bloquea la evaluación con el grupo beta, al final lo que queda fuera de alcance. Lo que no se pudo hacer o probar dice por qué.

## Prioridad alta (teléfono físico, con una persona)
1. **PR-12, alarma del cuidador a T + 30 con el teléfono en mano.** Preparado y no ejecutado por instrucción del usuario. Pasos y comandos en `docs/evidencias/p4/fisico/PENDIENTE_v1.1_fisico.md`, sección 1. En la v1.0 solo se observó la rama "sin conexión"; falta la rama "Toma sin confirmar" con la red estable.
2. **Sonido y vibración reales.** Que la alarma a T suene con el volumen de alarma, vibre de forma repetida y se corte con Tomado, Posponer o sola a los 2 min; que el aviso previo vibre una sola vez. Nadie la ha oído en el teléfono (en el emulador se oyó en las bocinas de la PC).
3. **Modo silencio, modo vibración y No molestar** (con y sin "Alarmas" permitidas). No probado en ningún dispositivo.
4. **Pantalla de alarma sobre el bloqueo con la pantalla apagada.** En la Fase 10 HyperOS solo encendió la luz de bordes y dejó la notificación; con la pantalla encendida y bloqueada sí se abrió sola. Confirmar a mano y capturarla si aparece (D45).

## Prioridad media (puntualidad y arranque)
5. **Reposo prolongado sin cable (escenario C de RNF-01).** Todas las mediciones fueron con el cable USB conectado. Requiere desconectar el cable, emparejar la depuración inalámbrica (`adb pair`) y la red estable (el teléfono alterna con otra red guardada del mismo rango sin ruta a la PC).
6. **Arranque directo tras reiniciar sin desbloquear.** Android no entrega `BOOT_COMPLETED` antes del primer desbloqueo y HyperOS lo entregó unos 2.5 min después; mientras tanto no hay avisos, y el vencido sale al reprogramar (+254 s en la Fase 10). Mejora posible, sin implementar ni probar: `LOCKED_BOOT_COMPLETED` con los datos en almacenamiento protegido del dispositivo (D42).
7. **Retest de la 1.1.1 en el emulador.** No se hizo: el cambio es de una línea (corte de 2 min con `setAlarmClock`), se probó en el teléfono y pasan las 79 pruebas unitarias.

## Prioridad baja (limitaciones conocidas del prototipo)
8. **Token vencido con confirmaciones pendientes.** Si el token vence (401) con confirmaciones en la cola, la sesión se borra con la copia local y esas confirmaciones se pierden. "Salir" sí está protegido (D37), pero el 401 no pasa por "Salir". El token dura 30 días, así que es poco probable en el prototipo.
9. **Interfaz para que el paciente comparta la dosis con el cuidador (G-07).** No hay pantalla; `cd_links.share_dose` queda en 0 y la dosis oculta al cuidador. Solo se activa en la BD (D32).
10. **Sin WorkManager:** si el sistema cierra la app sin red, la cola se envía la próxima vez que la app arranque (D29).
11. **La pestaña "Medicamentos" y el historial de 7 días requieren conexión.**
12. **Hosting (desplegado el 23/11/2026, D49):**
   - **HTTP sin redirección.** `http://areproyectos.com.mx/cuidadosis/api/` responde por HTTP sin cifrar. La app solo usa HTTPS. Se corrige en cPanel con "Force HTTPS Redirect" del dominio.
   - **R8 desactivado en el release** porque la optimización parcial cerraba la app. Activarlo completo requiere reglas de keep y una nueva prueba en el teléfono (opcional).
   - **Protección anti-bots del hosting.** Bloquea la IP unos minutos ante ráfagas de peticiones sospechosas. Con uso normal de la app no se ha visto.
13. **RNF-07 desde el teléfono por Wi-Fi, legibilidad con personas mayores y otros fabricantes:** no medidos.

## Fuera de alcance (no se implementa)
- **RF-17, notificación push al cuidador desde el servidor.** Queda como Futuro; se compensa en parte con la alarma local del cuidador (RF-18).
- Reportes, más de un cuidador activo por paciente y recomendaciones de dosis.

## Historial de hallazgos ya resueltos
- **v1.0, pantalla apagada: 6 de 10 avisos a tiempo** (retrasos de 67 a 187 s por agrupación de alarmas de HyperOS). Resuelto en la 1.1.0 con una alarma de reloj por aviso (D42): 20 de 20 en el teléfono.
- **1.1.0, corte de 2 min tardío con la pantalla apagada** (la alarma siguió sonando de 145 a 385 s). Resuelto en la 1.1.1 (D44).
- **"Salir" perdía confirmaciones pendientes.** Resuelto en la Fase 8 (D37).
- Entre los pasos 2 y 7 de la Fase 6 la app del cuidador no podía vincularse (respuesta nueva de `/links/accept`); corregido en esa fase.
