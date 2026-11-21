# Paso 6: PR-03 (aviso a T-5 y a T) y PR-04 (confirmación con un toque)

Fecha 2026-11-21; POCO M6 Pro (2312FPCA6G), Android 16 SDK 36, HyperOS OS3.0.307.0; API local por Wi-Fi; batería Sin restricciones, Inicio automático, notificaciones y alarmas exactas concedidas; commit `dd863c0` (APK `4944677b…49ff`).

| Caso | Pasos | Esperado | Observado | Resultado |
|---|---|---|---|---|
| PR-03 aviso a T-5 | Escenario A del paso 2 (pantalla encendida), 5 tomas | Aviso "Toma de las HH:MM" a T-5 ±60 s | 5 de 5 publicados entre +0.019 y +0.027 s (`puntualidad.csv`) | OK |
| PR-03 aviso a T | Mismo escenario, tomas sin confirmar | Segundo aviso "Es la hora de tu toma..." a T, una sola vez | 5 de 5 entre +0.013 y +0.036 s; ningún tercer aviso (el siguiente evento quedó para el día siguiente) | OK |
| PR-03 con pantalla apagada | Escenario B | Igual | 6 de 10 dentro de ±60 s; 4 con 67 a 187 s de retraso | FALLO (ver PENDIENTES) |
| PR-04 un toque sin diálogo | "Tomado" en la toma de 15:05 (toque por adb sobre el botón) | Sin diálogo; "Confirmada por el paciente a las HH:MM" | Justo después del toque no apareció ningún diálogo (no había textos "¿", "Cancelar" ni "Sí, la tomé"); la tarjeta mostró "Confirmada por el paciente a las 16:06" y el mensaje "Confirmada por el paciente: Medicamento SINTETICO Puntualidad A, 15:05." (`docs/capturas/fisico/06_confirmada_un_toque.png`) | OK |
| PR-04 llegada al servidor | BD tras el toque | Un registro con UUID | En ese momento el teléfono no alcanzaba la PC (ver nota). La confirmación quedó en la cola con "Sin conexión con el servidor" y "1 confirmación por enviar". Al reconectar el Wi-Fi del teléfono a la misma red se envió sola: confirmed_at 16:06:01, received_at 16:08:28, con client_uuid | OK |

Nota: tras el reinicio del paso 4 el teléfono recibió otra IP y no resolvía la PC por ARP, aunque sí llegaba al router. Se apagó y encendió el Wi-Fi del teléfono (`cmd wifi set-wifi-enabled`); volvió a la misma red y desde ahí llegó a la API. Fue una caída de red real, no simulada, y la app la manejó como prevé RF-13.
