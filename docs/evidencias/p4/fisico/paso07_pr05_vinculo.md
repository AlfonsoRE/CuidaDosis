# Paso 7: PR-05 vínculo con aceptación y revocación (dos dispositivos)

Fecha 2026-11-21; paciente `ptel1121@ejemplo.test` en el POCO M6 Pro (Android 16, HyperOS); cuidador `ctel1121@ejemplo.test` en el emulador `Medium_Phone_API_37.0` (Android 17); API local; commit `dd863c0`.

| Hora | Dispositivo | Acción | Respuesta observada | Resultado |
|---|---|---|---|---|
| 16:16:11 | Teléfono (paciente) | Generar código de invitación | Código de 8 caracteres con "Vence: 21/11/2026 16:26" (10 min) (`fisico/09_generar_codigo.png`) | OK |
| 16:16:43 | Emulador (cuidador) | Vincular paciente con ese código | Lista con "Esperando aceptación: Paciente SINTETICO Telefono. Solicitud enviada el 21/11/2026 16:16" (`fisico/09b_emulador_esperando_aceptacion.png`) | OK |
| 16:17 | API (cuidador) | `GET /caregiver/patients/{id}/intakes/today` antes de aceptar | `404` | OK |
| 16:18 | Teléfono (paciente) | Pestaña Cuidador | "Solicitud de vínculo: Cuidador SINTETICO Telefono" con Aceptar y Rechazar (`fisico/10_solicitud_por_aceptar.png`) | OK |
| 16:18:18 | Teléfono (paciente) | Aceptar | "Tu cuidador ... Vinculado desde 21/11/2026 16:18" (`fisico/11_vinculo_activo.png`); la API del cuidador pasa a `200` | OK |
| 16:20:55 | Teléfono (paciente) | Desvincular cuidador (diálogo "¿Desvincular a Cuidador SINTETICO Telefono?") | "No tienes cuidador vinculado" (`fisico/13_desvinculado.png`); la API del cuidador vuelve a `404`; BD: link 27 `revoked` con `revoked_at` 16:20:55 | OK |

Nota: los toques en el teléfono se hicieron por adb. Dos veces la pantalla se había bloqueado por el apagado automático (60 s); se desbloqueó con [PIN] y se repitió el paso, sin efectos en los datos.
