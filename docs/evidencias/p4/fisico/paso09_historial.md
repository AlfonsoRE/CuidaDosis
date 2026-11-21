# Paso 9: historial de 7 días (RF-15)

Fecha 2026-11-21; mismo entorno que el paso 7, con el vínculo activo; commit `dd863c0`.

| Vista | Observado | Resultado |
|---|---|---|
| Paciente en el teléfono, pestaña "Historial" | "Últimos 7 días", "Hoy, 21/11/2026: 2 de 12 tomas registradas, 8 sin confirmar", con la dosis "1 tableta (dato de prueba)" en cada toma (`fisico/12_historial_paciente.png`) | OK |
| Cuidador en el emulador, "Ver historial de 7 días" | El mismo resumen, sin ninguna línea de dosis en pantalla (`fisico/12b_emulador_historial_cuidador_sin_dosis.png`) | OK |
| API del cuidador `GET /caregiver/patients/{id}/intakes/history` | 0 campos `dose` en la respuesta (`share_dose = 0`) | OK |

La cuenta del teléfono es nueva, así que solo tiene tomas de hoy; la agrupación de varios días ya se verificó en el emulador con el seed (Fase 8, captura 19b).
