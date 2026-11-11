# Decisiones de la Fase 4

Fase 4: en la app, tomas de hoy con el botón "Tomado", vinculación y desvinculación con código, y vista del cuidador (lista de pacientes y detalle).

El equipo pidió hacer esta fase sin detenerse a preguntar. Cada decisión que normalmente se habría consultado queda anotada aquí: la duda, las opciones, la elección y el motivo.

## D1. Proceso: sin modo plan ni aprobación previa
- **Duda:** CLAUDE.md pide proponer un plan y esperar aprobación al iniciar cada fase.
- **Opciones:** (a) detenerse a pedir aprobación; (b) seguir con el plan recomendado y documentarlo aquí.
- **Elección:** (b).
- **Motivo:** el equipo lo pidió explícitamente para esta fase. Este archivo reemplaza la revisión previa.

## D2. Navegación del paciente: pestañas en lugar de pantallas sueltas
- **Duda:** cómo acomodar "Hoy", "Medicamentos" y "Cuidador" para el paciente.
- **Opciones:** (a) barra inferior con íconos, que exige agregar la dependencia `material-icons`; (b) pestañas de texto (`PrimaryTabRow`) bajo la barra superior; (c) botones en la barra superior que abren otras pantallas.
- **Elección:** (b). La pestaña inicial es "Hoy".
- **Motivo:** no agrega dependencias, las tres secciones quedan a un toque y "Hoy" es lo que el paciente usa a diario.

## D3. "Tomado" pide confirmación
- **Duda:** marcar la toma con un solo toque o pedir confirmación.
- **Opciones:** (a) un toque; (b) un diálogo de confirmación.
- **Elección:** (b).
- **Motivo:** la API no permite deshacer (decisión de la Fase 2), así que un toque accidental no tendría arreglo. El diálogo aclara que se registra el toque, no la ingesta.

## D4. Datos de prueba sin resetear la base de datos
- **Duda:** las pruebas en el emulador cambian datos (marcar tomas, vincular y desvincular).
- **Opciones:** (a) recargar `schema.sql` y `seed.sql`; (b) crear usuarios sintéticos nuevos por la API y hacer con ellos las pruebas que escriben datos.
- **Elección:** (b). Los usuarios del seed solo se usan para pruebas de lectura (por ejemplo, cuidador1 viendo a paciente1 y paciente3).
- **Motivo:** el equipo pidió no hacer acciones destructivas como resetear la BD. Crear usuarios nuevos solo agrega filas sintéticas.
- **Nota:** la BD se recargó una vez al cerrar la Fase 3, justo antes de recibir esta instrucción. Desde entonces no se volvió a resetear.
- **Datos que agregaron las pruebas:** "Paciente SINTETICO F4" (`pf41140@ejemplo.test`) y "Cuidador SINTETICO F4" (`cf41140@ejemplo.test`), ambos con la contraseña de prueba del seed, más un medicamento sintético, sus tomas de hoy y vínculos revocados. Desaparecen la próxima vez que alguien recargue `schema.sql` y `seed.sql`.

## D5. Hora de referencia para "la hora ya pasó"
- **Duda:** el emulador está en UTC y la API trabaja en hora de México. Con la hora del teléfono, una toma de las 08:00 podía marcarse como pasada o no según la zona del dispositivo.
- **Opciones:** (a) hora local del teléfono; (b) siempre `America/Mexico_City`, igual que la API; (c) pedir la hora al servidor.
- **Elección:** (b).
- **Motivo:** es la misma convención de AGENTS.md (todas las fechas en hora de México) y no requiere cambiar la API. Es solo un aviso visual ("Pendiente, la hora ya pasó"); no tiene lógica clínica.

## D6. Estado "la hora ya pasó"
- **Duda:** si mostrar alguna señal en las tomas pendientes cuya hora ya pasó.
- **Opciones:** (a) solo "Pendiente"; (b) "Pendiente, la hora ya pasó" en color de error.
- **Elección:** (b).
- **Motivo:** ayuda al cuidador a ver de un vistazo lo que falta, sin juzgar ni recomendar nada (no se habla de dosis ni de "olvidos").

## D7. Código de invitación tolerante al escribirlo
- **Duda:** el cuidador puede escribir el código en minúsculas, con espacios o con guion.
- **Opciones:** (a) exigir el formato exacto; (b) quitar todo lo que no sea letra o número y pasarlo a mayúsculas antes de enviarlo.
- **Elección:** (b). Se probó escribiendo `f4fz-sygy` y vinculó bien.
- **Motivo:** menos errores al dictar el código. La API ya valida el formato final (8 letras o números).

## D8. Cuidador que pierde el vínculo mientras ve el detalle
- **Duda:** qué mostrar si el paciente lo desvincula mientras el cuidador está en la pantalla de detalle.
- **Opciones:** (a) mostrar el error genérico de la API; (b) detectar el 404 y mostrar "Ya no estás vinculado con este paciente" con un botón para volver.
- **Elección:** (b).
- **Motivo:** explica claramente lo que pasó. Se probó desvinculando por la API con el detalle abierto.

## D9. "Generar código nuevo" sin confirmación
- **Duda:** pedir confirmación antes de generar otro código (el anterior deja de servir).
- **Opciones:** (a) diálogo de confirmación; (b) un texto de aviso fijo bajo el botón.
- **Elección:** (b).
- **Motivo:** no se pierde nada importante: basta con compartir el código nuevo. Así no se agrega otro diálogo.

## D10. Nombres de archivos
- `MedicationListScreen.kt` pasó a llamarse `MedicationListTab.kt`, porque ahora es una pestaña dentro de `PatientHomeScreen`. Se renombró con `git mv`, así que se conserva el historial.

## D11. Redacción del resumen
- "0 de 1 tomas registradas" se cambió a "0 de 1 toma registrada" (singular cuando hay una sola toma). Lo detecté en la prueba con cuidador1.

## Pruebas realizadas
Todas se ejecutaron el 11/11/2026 en el emulador `Medium_Phone_API_37.0` (Android 17), contra la API local.

**Compilación y análisis**
- `gradlew assembleDebug`, `assembleRelease`, `testDebugUnitTest` y `lintDebug`: correctos. Pasan 21 pruebas unitarias. Lint da 0 errores; sus 22 advertencias son las mismas de la Fase 3.

**Paciente F4**
- Pestaña "Hoy": 2 tomas. La de las 08:00 aparece como "Pendiente, la hora ya pasó".
- "Tomado": abre el diálogo de confirmación; al aceptar se ve "Tomada a las 11:45" y en la BD queda `status = taken`.
- Pestaña "Cuidador": genera un código y muestra cuándo vence (48 h). "Generar código nuevo" da un código distinto.
- Pestaña "Medicamentos": lista con su horario y el botón "Agregar".

**Cuidador F4**
- Lista vacía al inicio.
- Código equivocado: muestra "El código no existe, ya se usó o expiró".
- Código escrito como `f4fz-sygy`: vincula bien y la lista muestra "1 de 2 tomas registradas".
- Detalle de solo lectura: muestra la tomada y la pendiente.
- "Dejar de seguir": confirma y vuelve a la lista vacía. En la BD queda `revoked` con `revoked_at`.

**Casos con los dos lados**
- El paciente desvincula (por la API) mientras el cuidador ve el detalle: al tocar "Actualizar" aparece "Ya no estás vinculado con este paciente" con el botón para volver.
- El paciente desvincula desde su app: confirma y vuelve a "No tienes cuidador vinculado".

**Cuidador1 del seed (solo lectura)**
- Ve a Paciente SINTETICO Tres ("0 de 1 toma registrada") y a Paciente SINTETICO Uno ("1 de 3 tomas registradas").

## Pendientes y notas
- **ANR al reinstalar:** durante las pruebas apareció un ANR ("Input dispatching timed out, no focused window"). Fue porque `adb` envió teclas mientras la actividad todavía arrancaba tras reinstalar. Esperando unos segundos no se repitió. No se pudo atribuir a la app, pero conviene vigilarlo en un teléfono físico.
- **Teléfono físico:** no se probó; solo el emulador.
- **Script de la API:** no se volvió a correr la batería de curl de la Fase 2, porque asume una BD recién recargada y resetearla está fuera de lo permitido en esta fase. La API no cambió en la Fase 4.
- **Cambio de día:** no se probó el comportamiento a medianoche. La app recarga al volver a la pantalla, así que basta con tocar "Actualizar".
