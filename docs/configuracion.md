# Configuración de CuidaDosis

## 1. API: `api/.env`

Se crea copiando `api/.env.example`. **Nunca se sube al repositorio** (está en `.gitignore`) y no es accesible por web (403).

| Variable | Ejemplo | Uso |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | Servidor de MySQL o MariaDB |
| `DB_PORT` | `3306` | Puerto |
| `DB_NAME` | `cuidadosis` | Base de datos (la crea `bd/schema.sql`) |
| `DB_USER` | (tu usuario local) | Usuario de MySQL |
| `DB_PASS` | (tu contraseña local) | Contraseña de MySQL |
| `TOKEN_TTL_HOURS` | `720` | Vigencia del token de sesión, en horas (720 = 30 días) |
| `APP_DEBUG` | `false` | `true` solo en desarrollo: agrega el detalle técnico a los errores 500 |
| `APP_BASE_PATH` | vacío | Prefijo de la URL donde se sirve la API, por ejemplo `/cuidadosis/api` en el hosting. Vacío: se deduce de la ubicación de `index.php` (en XAMPP se deja vacío) |

Valores fijos en el código (no se configuran):
- Zona horaria `America/Mexico_City` en PHP y en la sesión de MySQL (ADR-008).
- `sql_mode` estricto en cada conexión.
- Código de invitación: 8 caracteres, 10 minutos, un solo uso. Bloqueo tras 5 intentos fallidos en 10 minutos (`LinkController`).
- "Sin confirmar" a los 30 minutos (`IntakeController::UNCONFIRMED_AFTER_MINUTES`).
- Historial de 7 días; lotes de sincronización de hasta 100 confirmaciones.

## 2. App Android

### 2.1 URL de la API
| Dónde | Valor |
|---|---|
| Emulador (por defecto) | `http://10.0.2.2/CuidaDosis/api/public/` |
| Teléfono en la misma red | `http://<IP de la PC>/CuidaDosis/api/public/` |
| Hosting (subcarpeta) | `https://areproyectos.com.mx/cuidadosis/api/` (desplegado el 23/11/2026; fijo en la versión release, D49) |
| Al compilar | `api.baseUrl=<URL>` en `android/local.properties` (no se versiona) |
| Sin recompilar (solo debug) | Inicio de sesión → **Servidor** → escribir URL → **Probar conexión** → **Guardar**. La release no tiene esta opción |

HTTP sin cifrar solo se permite en la versión debug (`src/debug`, network security config). La versión release exige HTTPS.

### 2.2 Ajustes del paciente (solo en el teléfono)
- **Anticipación del aviso (L):** 5 o 10 minutos, 5 por defecto. Pantalla **Ajustes**. No se envía al servidor (G-06).
- **Posponer:** 5 o 10 minutos, desde los botones de la notificación.

### 2.3 Permisos
| Permiso | Para qué | Cómo se obtiene |
|---|---|---|
| Dispositivos cercanos o red local (`ACCESS_LOCAL_NETWORK`) | Llegar a XAMPP en la red local (Android 17) | La app lo pide al abrir solo si el servidor está en la red local (debug); la release no lo pide |
| Notificaciones (`POST_NOTIFICATIONS`) | Avisos del paciente y alarma del cuidador | La app lo pide al entrar; si se niega, la tarjeta de permisos lleva a los ajustes |
| Alarmas exactas (`USE_EXACT_ALARM`) | Avisos puntuales | Se concede al instalar; si el sistema lo quitara, la tarjeta lo indica |
| Sin restricción de batería | Que el ahorro de energía no retrase avisos | Tarjeta de permisos → **Ajustar batería** |
| Arranque (`RECEIVE_BOOT_COMPLETED`) | Reprogramar avisos tras reiniciar | Se concede al instalar |
| Estado de la red (`ACCESS_NETWORK_STATE`) | Enviar confirmaciones pendientes al volver la red | Se concede al instalar |

### 2.4 Datos guardados en el teléfono
Cifrados con AES-GCM y clave en Android Keystore, y excluidos de respaldos: sesión (`session.xml`), copia del paciente con medicamentos, tomas de hoy y cola de confirmaciones (`patient_local.xml`) y lista de tomas que vigila el cuidador (`caregiver_alarms.xml`). Sin cifrar, porque no tienen datos personales: ajustes (`settings.xml`) y registro de avisos mostrados (`reminders.xml`, solo fechas y horas). Al cerrar sesión se borran la sesión, las copias del paciente y del cuidador y el registro de avisos; los ajustes (URL y anticipación) se conservan.

## 3. Datos sintéticos
- `bd/seed.sql`: 5 usuarios (contraseña `Sintetico2026`), medicamentos y tomas de hoy y de ayer, relativos al día en que se carga.
- `share_dose`: el paciente 1 no comparte la dosis y el paciente 3 sí, para probar G-07. No hay pantalla para cambiarlo (ver `docs/PENDIENTES_FASE6.md`).
