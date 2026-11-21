# Paso 5: PR-08 sin red y sin duplicados

Fecha 2026-11-21; POCO M6 Pro (2312FPCA6G), Android 16 SDK 36, HyperOS OS3.0.307.0; API local por Wi-Fi; batería Sin restricciones, Inicio automático; commit `dd863c0`.

| Paso | Hora (teléfono) | Observado |
|---|---|---|
| `cmd connectivity airplane-mode enable` | 16:09:10 | Modo avión activo (`enabled`); el comando funcionó |
| "Tomado" en la toma de 15:07 | 16:09:28 | "Sin conexión con el servidor", "1 confirmación por enviar" (`docs/capturas/fisico/07_sin_red_confirmada.png`) |
| BD antes de reconectar | 16:09 | 1 fila de esa toma, pendiente (`taken=0`) |
| `cmd connectivity airplane-mode disable` | 16:10:17 | Sin abrir ni tocar la app |
| BD después | 16:10:52 | 1 fila, taken, confirmed_at 16:09:28 (hora del toque sin red), received_at 16:10:52, 1 client_uuid. "Última sincronización: 16:10" en la app |

**Resultado: OK.** Un solo registro; la cola se envió sola 35 s después de volver la red.
