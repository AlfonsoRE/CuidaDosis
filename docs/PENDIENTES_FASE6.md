# Pendientes y bloqueos de la Fase 6

Lo que no se pudo hacer o probar, con lo que se intentó. Lo que quedó fuera de alcance por AGENTS.md también se anota aquí.

## Fuera de alcance (no se implementa)
- RF-17, notificación push al cuidador desde el servidor. Se compensa en parte con la alarma local del cuidador (RF-18).
- Reportes, más de un cuidador activo por paciente y recomendaciones de dosis.

## Pendiente de ejecución manual (teléfono físico)
- Puntualidad de los avisos T menos L y T (RNF-01: 10 avisos por escenario con pantalla encendida, bloqueada, ahorro de batería y tras reinicio).
- Reprogramación tras reiniciar en un teléfono real y comportamiento con restricciones del fabricante.
- Alarma del cuidador a T + 30 en un teléfono real.

## Limitaciones conocidas
- Sin WorkManager: si el sistema cierra la app mientras no hay red, la cola de confirmaciones se envía la próxima vez que la app arranque (al abrirla o al sonar una alarma). Ver D29.
- Si el token vence (401) con confirmaciones en la cola, la sesión se borra y con ella la copia local, así que esas confirmaciones se pierden. El token dura 30 días (720 h), así que es poco probable en el uso del prototipo.
- La pestaña "Medicamentos" requiere conexión para dar de alta o editar.
- Entre el paso 2 y el paso 7 la app del cuidador no podía vincularse (respuesta nueva de `/links/accept`).
