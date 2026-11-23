# Material fuente para los manuales de CuidaDosis

Datos verificados para redactar el manual de instalación y el manual de usuario. No es un manual: es la fuente. Procedimientos completos en [instalacion.md](instalacion.md), [configuracion.md](configuracion.md) y [guia-uso.md](guia-uso.md); capturas en [capturas/LISTA.md](capturas/LISTA.md).

> CuidaDosis es un prototipo académico. No recomienda dosis ni sustituye al médico. "Confirmada por el paciente" solo indica que se tocó "Tomado"; no verifica la ingesta. Solo se usan datos sintéticos.

## 1. Aplicación
| Dato | Valor |
|---|---|
| Nombre | CuidaDosis |
| Paquete (applicationId) | `mx.cuidadosis.app` |
| Versión | `versionName` 1.1.0, `versionCode` 2 (la v1.0 evaluada en el teléfono está etiquetada `v1.0-evaluada`) |
| Archivo | `app-debug.apk` (`android/app/build/outputs/apk/debug/`); no se versiona |
| SHA-256 del APK usado en la verificación física | `4944677b3fe8ced85e3ce72665d432cdc3fca97b59b14cd804ecfd42bdc749ff` (compilado del commit `3fbba32`) |
| Android mínimo | 8.0 (API 26) |
| Android objetivo | API 37 (Android 17) |
| Probado en | Emulador Android 17 (API 37) y POCO M6 Pro con Android 16 (API 36, HyperOS OS3.0) |
| Tipo de build | Debug (permite HTTP sin cifrar para la red local). La release exige HTTPS y no se distribuye |
| Una sola app para dos roles | Paciente y Cuidador, según la cuenta |

## 2. Requisitos del servidor
| Componente | Requisito | Versión verificada |
|---|---|---|
| Servidor web | Apache con `mod_rewrite` y `.htaccess` permitido (`AllowOverride`) | Apache 2.4.58 (XAMPP) |
| PHP | 8.2 o mayor, con `pdo_mysql` | 8.2.12 |
| Base de datos | MariaDB 10.4 o mayor, o MySQL 8, utf8mb4 | MariaDB 10.4.32 |
| Zona horaria | La API fija `America/Mexico_City` en PHP y en la sesión de MySQL; no depende del servidor | |
| Red | El teléfono debe llegar al servidor (misma red local o HTTPS público) | |
| Carpeta pública | Solo `api/public/` (o `public_html/` del paquete de hosting) | |
| HTTPS | Obligatorio fuera de la red local | |

Paquete para hosting: `deploy/build_hosting_package.php` (ver [instalacion.md](instalacion.md), sección 3).

## 3. Variables del `.env` (sin valores)
| Variable | Para qué |
|---|---|
| `DB_HOST` | Servidor de la base de datos |
| `DB_PORT` | Puerto (normalmente 3306) |
| `DB_NAME` | Nombre de la base |
| `DB_USER` | Usuario de la base |
| `DB_PASS` | Contraseña de la base |
| `TOKEN_TTL_HOURS` | Vigencia de la sesión en horas (720 = 30 días) |
| `APP_DEBUG` | `false` en servidores públicos |

Plantillas: `api/.env.example` (local) y `api/.env.hosting.example` (hosting). El `.env` real nunca se sube al repositorio y no es accesible por web (verificado: 403).

## 4. Pantallas existentes
| Rol | Pantalla | Qué hace |
|---|---|---|
| Ambos | Inicio de sesión | Correo y contraseña; enlace a registro; enlace **Servidor** |
| Ambos | Servidor | URL de la API, **Probar conexión**, **Guardar**, restablecer |
| Ambos | Crear cuenta | Rol Paciente o Cuidador, nombre, correo, contraseña |
| Paciente | Hoy | Tomas del día con tres etiquetas, botón **Tomado** (un toque), última sincronización, aviso de sin conexión, tarjeta de permisos |
| Paciente | Medicamentos | Lista con horarios; alta, edición (horas) y desactivación |
| Paciente | Historial | Últimos 7 días por día, con resumen |
| Paciente | Cuidador | Generar código (10 min), aceptar o rechazar solicitud, desvincular |
| Paciente | Ajustes | Anticipación del aviso (5 o 10 min) y estado de permisos |
| Paciente | Diálogo al salir | Bloquea el cierre de sesión si hay confirmaciones sin enviar |
| Paciente | Notificaciones (v1.1) | Aviso a T − L con sonido y vibración; alarma a T que suena y vibra hasta Tomado o Posponer 5/10 min (máximo 2 min, luego notificación fija sin sonido) |
| Paciente | Pantalla de alarma (v1.1) | Hora y medicamentos de la toma, botones Tomado, Posponer 5 min y Posponer 10 min; se muestra sobre la pantalla de bloqueo |
| Cuidador | Mis pacientes | Resumen del día, "Sin confirmar", última sincronización, "Esperando aceptación", **Vincular paciente**, **Cerrar sesión** |
| Cuidador | Vincular paciente | Diálogo para escribir el código |
| Cuidador | Detalle del paciente | Tomas de hoy con etiquetas (sin dosis salvo autorización), **Ver historial de 7 días**, **Dejar de seguir** |
| Cuidador | Historial del paciente | Últimos 7 días sin dosis |
| Cuidador | Notificación | "Toma sin confirmar" a T + 30 min |

## 5. Configuración especial de teléfonos Xiaomi / HyperOS
Verificada en el POCO M6 Pro: notificaciones permitidas, Ahorro de batería **Sin restricciones** y **Autoencendido en segundo plano** activado para CuidaDosis. Sin esto, HyperOS puede retrasar o anular los avisos y la reprogramación tras reiniciar. Ver [instalacion.md](instalacion.md) 2.3 y [guia-uso.md](guia-uso.md) sección 5.

## 5b. Canales de notificación (v1.1)
| Canal | Uso | Sonido y vibración |
|---|---|---|
| `intake_before_v2` "Aviso antes de la toma" | Aviso T − L | Sonido de notificación y vibración breve |
| `intake_alarm_v2` "Alarma de la toma" | Alarma a T y al terminar un pospuesto | Sonido de alarma (volumen de alarma) y vibración que se repiten, máximo 2 min |
| `intake_alarm_silent_v2` "Toma pendiente (sin sonido)" | Notificación fija tras los 2 min | Sin sonido ni vibración |
| `caregiver_alerts_v2` "Tomas sin confirmar" | Cuidador a T + 30 | Sonido y vibración breves, sin repetir |

Los canales de la v1.0 (`intake_reminders`, `caregiver_alerts`, configuración anterior sin vibración) se eliminan al abrir la v1.1. Con No molestar, la alarma solo suena si están permitidas las alarmas; en modo silencio o vibración sigue el volumen de alarma.

## 6. No implementado
- **RF-17:** notificación push al cuidador desde el servidor. Fuera de alcance; la compensa en parte la alarma local del cuidador a T + 30 (RF-18), que solo funciona si el cuidador abrió la app ese día.
- **G-07 (pantalla):** no hay pantalla para que el paciente autorice mostrar la dosis al cuidador. La dosis queda oculta por defecto (`cd_links.share_dose = 0`); solo se puede activar en la base de datos.
- Más de un cuidador activo por paciente, reportes y recomendaciones de dosis: fuera de alcance.

## 7. Limitaciones conocidas
- **Token vencido con confirmaciones pendientes:** si la sesión vence (401, tras 30 días) mientras hay confirmaciones sin enviar, la app borra la sesión y la copia local, y esas confirmaciones se pierden. El cierre de sesión manual sí está protegido (bloquea "Salir").
- Sin WorkManager: si el sistema cierra la app sin red, las confirmaciones pendientes se envían la próxima vez que la app arranque.
- La pestaña Medicamentos y el historial necesitan conexión.
- La alarma del cuidador solo cubre las tomas de los días en que abrió la app.
- Las horas son de México (`America/Mexico_City`); no se soporta viajar entre zonas horarias.
- El APK verificado es debug (HTTP en red local). Para uso fuera de la red local hace falta HTTPS y un build release firmado.
- Verificación física en un solo modelo (POCO M6 Pro); otros fabricantes pueden restringir más los avisos.
