# Pasos 0 y 1: instalación y compatibilidad en el teléfono físico

- **Fecha:** 21/11/2026, 14:40 a 14:55 (hora de México).
- **Teléfono:** POCO M6 Pro (Xiaomi), modelo 2312FPCA6G, HyperOS OS3.0.307.0.WNFMIXM, Android 16, SDK 36, parche de seguridad 2026-09-01. Zona horaria del teléfono `America/Mexico_City`, hora automática.
- **Entorno:** API local en la PC (XAMPP: Apache 2.4.58, PHP 8.2.12, MariaDB 10.4.32), misma red Wi-Fi que el teléfono. Teléfono conectado por USB (depuración USB), cargando por USB.
- **Commit del código:** `dd863c0` (el APK se compiló de `3fbba32`; `dd863c0` solo cambia documentación).

## Paso 0. Instalación
| Acción | Resultado |
|---|---|
| `adb -s <ID> install -r app-debug.apk` | `Success` (no hizo falta activar "Instalar vía USB") |
| SHA-256 del APK | `4944677b3fe8ced85e3ce72665d432cdc3fca97b59b14cd804ecfd42bdc749ff` |
| Versión instalada | `versionName=1.0`, `versionCode=1`, `minSdk=26`, `targetSdk=37` |
| Servidor | Se escribió la URL local de la PC en **Servidor**; **Probar conexión** respondió "Conexión correcta con la API." y se guardó. La captura no se conserva porque muestra la IP privada de la PC |
| Cuenta de paciente | `ptel1121@ejemplo.test` ("Paciente SINTETICO Telefono"), registrada desde la app del teléfono |
| Cuenta de cuidador | `ctel1121@ejemplo.test` ("Cuidador SINTETICO Telefono"), registrada desde la app del emulador (segundo dispositivo) |

## Paso 1. Compatibilidad y configuración de HyperOS (condición de todas las pruebas)
| Elemento | Estado |
|---|---|
| `POST_NOTIFICATIONS` | Concedido (se aceptó el diálogo "Permitir" al entrar) |
| `USE_EXACT_ALARM` | Concedido al instalar |
| `RECEIVE_BOOT_COMPLETED`, `INTERNET`, `ACCESS_NETWORK_STATE` | Concedidos |
| `ACCESS_LOCAL_NETWORK` | Declarado; en Android 16 no aplica (la app solo lo pide en Android 17) |
| Ahorro de batería de HyperOS | **Sin restricciones** (Info. de la aplicación → Batería). El sistema lo refleja en la lista de exenciones: `user,mx.cuidadosis.app` |
| Inicio automático de HyperOS | **Activado** (Seguridad → Autoencendido en segundo plano) |
| Grupo de reposo (`am get-standby-bucket`) | `5` (EXEMPTED) |
| Tarjeta de permisos de la app | No aparece: la app considera que todo está en orden |
| Instalación de la app | La instalación por USB funcionó sin cambiar "Instalar vía USB" |
