# Paso 8: cierre de sesión con confirmaciones sin enviar

Fecha 2026-11-21; POCO M6 Pro (2312FPCA6G), Android 16 SDK 36, HyperOS OS3.0.307.0; commit `dd863c0`.

| Paso | Observado | Resultado |
|---|---|---|
| En modo avión, con 1 confirmación en cola, tocar "Salir" | Diálogo "Confirmaciones sin enviar" con "Quedarme" y "Reintentar envío" (`docs/capturas/fisico/08_salir_bloqueado.png`) | OK |
| Tocar "Quedarme" | La sesión siguió abierta ("Paciente SINTETICO Telefono" en pantalla) | OK |
| Restablecer la red | La confirmación llegó al servidor (received_at 16:10:52), una sola fila | OK |

**LIMITACIÓN (no corregida, por instrucción):** si el token vence (respuesta 401) mientras hay confirmaciones en la cola, la app borra la sesión y la copia local sin pasar por "Salir", y esas confirmaciones se pierden. No se reprodujo en el teléfono: requiere esperar 30 días o invalidar el token en la BD. Documentada en `docs/PENDIENTES_FASE6.md` y `docs/MANUALES_FUENTE.md`.
