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
- La pestaña "Medicamentos" y el historial de 7 días requieren conexión.
- Entre el paso 2 y el paso 7 la app del cuidador no podía vincularse (respuesta nueva de `/links/accept`).
- G-07: no hay pantalla para que el paciente autorice compartir la dosis (`share_dose`); la dosis queda oculta al cuidador por defecto. Solo se puede activar en la BD (como en el seed). Ver D32.
- Resuelto en la Fase 8 (D37): "Salir" envía antes las confirmaciones pendientes y, si no puede, bloquea el cierre de sesión. Sigue el caso de token vencido (401), que borra la sesión sin pasar por "Salir".
- Despliegue en hosting: el paquete y el procedimiento están listos y probados en la PC (D38), pero subirlo y la lista de verificación de `docs/instalacion.md` 3.4 están pendientes de ejecutar en un hosting real.
