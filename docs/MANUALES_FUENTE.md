# Material fuente para los manuales de CuidaDosis

Datos verificados para redactar el manual de instalación y el manual de usuario. No es un manual: es la fuente. Procedimientos completos en [instalacion.md](instalacion.md), [configuracion.md](configuracion.md) y [guia-uso.md](guia-uso.md); capturas en [capturas/LISTA.md](capturas/LISTA.md).

Estado al 23/11/2026: versión **1.1.3**, la que usa el grupo beta (D49 y D50 en [decisiones-fase-6.md](decisiones-fase-6.md)).

> CuidaDosis es un prototipo académico. No recomienda dosis ni sustituye al médico. "Confirmada por el paciente" solo indica que se tocó "Tomado"; no verifica la ingesta. Solo se usan datos sintéticos.

## 1. Aplicación
| Dato | Valor |
|---|---|
| Nombre | CuidaDosis |
| Paquete (applicationId) | `mx.cuidadosis.app` |
| Versión | `versionName` 1.1.3, `versionCode` 5 (la v1.0 evaluada en el teléfono está etiquetada `v1.0-evaluada`) |
| Archivo para el grupo beta | `CuidaDosis-1.1.3-beta.apk`, release firmado (8 830 820 bytes) |
| Descarga | `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.3-beta.apk` |
| SHA-256 del APK | `08c66b382cc6c88b4c56f646fc86b100f4bc8eb97686eaa149e1b68fc8af71b5` |
| Huella SHA-256 del certificado de firma | `6d03a106a1ec6cf59661b86ff111ff8bd6b1e0824eb2b1883045473eaffb29fb` |
| Servidor | Fijo en la release: `https://areproyectos.com.mx/cuidadosis/api/` (solo HTTPS) |
| Android mínimo | 8.0 (API 26) |
| Android objetivo | API 37 (Android 17) |
| Probado en | Emulador Android 17 (API 37) y POCO M6 Pro con Android 16 (API 36, HyperOS OS3.0) |
| Una sola app para dos roles | Paciente y Cuidador, según la cuenta |

### Release y debug
| | Release (grupo beta) | Debug (equipo, pruebas locales) |
|---|---|---|
| Servidor | Fijo, el del proyecto. El login muestra "Servidor: https://areproyectos.com.mx/cuidadosis/api/" como texto; al tocarlo no pasa nada | Configurable: enlace **Servidor** en el login (XAMPP en la PC o el emulador) |
| HTTP sin cifrar | No permitido | Permitido solo para la red local |
| Firma | Keystore del proyecto (fuera del repositorio) | Clave de depuración de Android Studio |
| Actualizar encima | Sí, entre versiones release (1.1.2 a 1.1.3 comprobado) | No se puede pasar de debug a release sin desinstalar (firmas distintas) |

### Ícono (D50)
- **Ícono de la app:** cápsula blanca y verde claro inclinada, con una insignia redonda con palomita, sobre fondo verde azulado en degradado. Así aparece en el cajón de apps y en Info. de la aplicación ([CAPTURA: 27_icono_cajon_v113], [CAPTURA: 28_info_app_version_v113]).
- **Ícono de las notificaciones:** silueta blanca de la cápsula (sin la palomita) en la barra de estado y en el panel. Lo usan el aviso previo, la alarma y la notificación del cuidador. Verificado en el código y en el render; **no hay captura en una notificación real** (ver [capturas/LISTA.md](capturas/LISTA.md)).
- **Ícono temático (Android 13 o superior):** capa monocroma con la misma silueta. Solo se revisó en el render (`docs/assets/icono/render_verificacion.png`).

## 2. Instalación en el teléfono (grupo beta)
Pasos para el manual de instalación. La descarga por HTTPS se comprobó desde la PC (200, tipo `application/vnd.android.package-archive`, SHA-256 igual al local). En el teléfono del equipo la 1.1.3 se instaló por USB con `adb`; **la instalación desde el navegador del teléfono no se ha capturado** (ver [capturas/LISTA.md](capturas/LISTA.md)).

1. En el teléfono, abre el enlace `https://areproyectos.com.mx/cuidadosis/descargas/CuidaDosis-1.1.3-beta.apk` en el navegador (o recibe el archivo APK) y descárgalo. El navegador puede advertir que el archivo puede ser dañino: elige **Descargar de todas formas**.
2. Abre el archivo descargado. La primera vez Android pide permitir **Instalar apps desconocidas** para el navegador o el administrador de archivos: actívalo solo para esa app y regresa.
3. Toca **Instalar**. **Google Play Protect** puede mostrar un aviso porque la app no viene de Play Store: elige instalar de todas formas (en el Xiaomi del equipo, "No enviar" también instala). En Xiaomi puede aparecer además un aviso de seguridad propio con una cuenta regresiva antes de **Instalar**.
4. Si tenías una versión debug de CuidaDosis, desinstálala antes: Android no instala encima por la firma distinta. Si tenías la 1.1.2 release, la 1.1.3 se instala encima y conserva la sesión.
5. Abre la app, acepta los permisos (sección 4) y haz la configuración de la sección 5.
6. Para comprobar la versión: Ajustes, Aplicaciones, CuidaDosis: "Versión: 1.1.3" ([CAPTURA: 28_info_app_version_v113]).

Después de instalar de nuevo (desinstalar y volver a instalar) hay que repetir "Sin restricciones" e "Inicio automático" en HyperOS.

## 3. Requisitos del servidor
| Componente | Requisito | Local (XAMPP) | Hosting del proyecto |
|---|---|---|---|
| Servidor web | Apache o LiteSpeed con `mod_rewrite` y `.htaccess` permitido | Apache 2.4.58 | nginx delante de LiteSpeed |
| PHP | 8.2 o mayor, con `pdo_mysql` | 8.2.12 | 8.2.36 |
| Base de datos | MariaDB 10.4 o mayor, o MySQL 8, utf8mb4 | MariaDB 10.4.32 | MariaDB 11.4 (base compartida, 7 tablas `cd_*`) |
| Zona horaria | La API fija `America/Mexico_City` en PHP y en la sesión de MySQL; no depende del servidor | | Servidor en UTC, la API responde en hora de México |
| Carpeta pública | Solo `api/public/` (o `public_html/cuidadosis/api/` del paquete) | | `public_html/cuidadosis/api/` y `descargas/` |
| HTTPS | Obligatorio fuera de la red local | No | Sí (Let's Encrypt) |

Paquete para hosting: `deploy/build_hosting_package.php --subdir=cuidadosis` (ver [instalacion.md](instalacion.md), sección 3). Pendiente en el hosting: redirigir HTTP a HTTPS ("Force HTTPS Redirect" en cPanel); la app solo usa HTTPS.

## 3b. Variables del `.env` (sin valores)
| Variable | Para qué |
|---|---|
| `DB_HOST` | Servidor de la base de datos |
| `DB_PORT` | Puerto (normalmente 3306) |
| `DB_NAME` | Nombre de la base |
| `DB_USER` | Usuario de la base |
| `DB_PASS` | Contraseña de la base |
| `TOKEN_TTL_HOURS` | Vigencia de la sesión en horas (720 = 30 días) |
| `APP_DEBUG` | `false` en servidores públicos |
| `APP_BASE_PATH` | Ruta pública de la API cuando no está en la raíz (en el hosting: `/cuidadosis/api`) |

Plantillas: `api/.env.example` (local) y `api/.env.hosting.example` (hosting). El `.env` real nunca se sube al repositorio y no es accesible por web (verificado: 403 en local; fuera de la carpeta web en el hosting).

## 4. Permisos que pide la app
| Permiso | Cuándo | Para qué | Si se niega |
|---|---|---|---|
| Notificaciones | Al abrir la app (Android 13 o superior) | Aviso previo, alarma y aviso del cuidador | Tarjeta roja en "Hoy" y en Ajustes; no hay avisos |
| Alarmas y recordatorios (alarmas exactas) | Se concede al instalar (`USE_EXACT_ALARM`) | Que el aviso y la alarma salgan a la hora exacta | Se avisa en la tarjeta de permisos |
| Pantalla completa (notificaciones a pantalla completa) | Ajuste del sistema | Mostrar la pantalla de alarma sobre el bloqueo | La alarma queda solo como notificación |
| Red local (`ACCESS_LOCAL_NETWORK`, Android 17) | Al abrir la app | Solo para el debug con XAMPP en la red local; la release usa Internet | En la release no afecta |

## 5. Configuración especial de teléfonos Xiaomi / HyperOS
Verificada en el POCO M6 Pro. Sin esto, HyperOS puede retrasar o anular los avisos y la reprogramación tras reiniciar. Ver [instalacion.md](instalacion.md) 2.3 y [guia-uso.md](guia-uso.md) sección 5.

| Ajuste | Dónde | Captura |
|---|---|---|
| Notificaciones permitidas, con sonido, vibración, pantalla de bloqueo y flotantes | Info. de la aplicación, Notificaciones | `hyperos_notificaciones_categorias.png`, `hyperos_notificaciones_categorias_2.png` |
| Notificaciones a pantalla completa | Info. de la aplicación, Notificaciones | `hyperos_pantalla_completa_v113.png` |
| Ahorro de batería **Sin restricciones** | Info. de la aplicación, Ahorro de batería | `hyperos_bateria_sin_restricciones_v113.png` |
| **Inicio automático** (Autoencendido en segundo plano) | App Seguridad o Info. de la aplicación | `hyperos_inicio_automatico.png` (ícono genérico; retomar con la 1.1.3) |
| Mostrar en pantalla de bloqueo y Abrir ventanas en segundo plano | Info. de la aplicación, Otros permisos | `hyperos_otros_permisos.png` |
| Volumen de alarma por encima de cero | Volumen del sistema | (sin captura) |

Observado el 23/11/2026 en el teléfono del equipo, sin cambiar nada:
- **Inicio automático de CuidaDosis desactivado.** En la Fase 10 estaba activo; lo más probable es que se haya restablecido al desinstalar la debug para instalar la release. Hay que activarlo de nuevo después de cada reinstalación, igual que "Sin restricciones" (que sí sigue activo).
- "Pausar la actividad de la aplicación si no se utiliza" está activado (valor por defecto de Android). Si la app no se abre en meses, Android puede quitarle permisos; conviene desactivarlo en Info. de la aplicación. No se cambió en el teléfono.

## 6. Avisos del paciente y del cuidador (v1.1)
| Momento | Quién | Qué pasa | Texto |
|---|---|---|---|
| T − L (L = 5 o 10 min, en Ajustes) | Paciente | Notificación con sonido y vibración breve, una vez | "Toma de las HH:MM", "En 5 minutos: medicamento: dosis", con Posponer 5 min y Posponer 10 min |
| T | Paciente | Alarma: sonido de alarma (volumen de alarma) y vibración que se repiten hasta tocar **Tomado** o **Posponer**; pantalla de alarma sobre el bloqueo si el sistema lo permite | "Es la hora de tu toma de las HH:MM", "Toca Tomado o Posponer" |
| T + 2 min sin respuesta | Paciente | La alarma deja de sonar; queda una notificación fija sin sonido | "Sigue pendiente" |
| Al terminar un pospuesto | Paciente | Vuelve a sonar la alarma | "Recordatorio: toma de las HH:MM" |
| T + 30 min | Cuidador | La app consulta el estado; solo avisa si sigue sin confirmar o no pudo consultar. Notificación con sonido y vibración breves, sin repetir | "Toma sin confirmar: nombre" o "Revisa a nombre" (sin conexión) |

Tocar **Tomado** confirma con un solo toque, sin diálogo, y guarda la hora real. Si no hay red, la confirmación queda en la cola y se envía después.

### Canales de notificación
| Canal | Uso | Sonido y vibración |
|---|---|---|
| `intake_before_v2` "Aviso antes de la toma" | Aviso T − L | Sonido de notificación y vibración breve |
| `intake_alarm_v2` "Alarma de la toma" | Alarma a T y al terminar un pospuesto | Sonido de alarma (volumen de alarma) y vibración que se repiten, máximo 2 min |
| `intake_alarm_silent_v2` "Toma pendiente (sin sonido)" | Notificación fija tras los 2 min | Sin sonido ni vibración |
| `caregiver_alerts_v2` "Tomas sin confirmar" | Cuidador a T + 30 | Sonido y vibración breves, sin repetir |

Los canales de la v1.0 (`intake_reminders`, `caregiver_alerts`) se eliminan al abrir la v1.1. Con No molestar, la alarma solo suena si están permitidas las alarmas; en modo silencio o vibración sigue el volumen de alarma (comportamiento de Android; **no probado** en el teléfono, ver [PENDIENTES_FASE6.md](PENDIENTES_FASE6.md)).

## 7. Pantallas existentes
Las pantallas internas no cambiaron de la 1.1.1 a la 1.1.3; solo el login (servidor fijo en la release) y los íconos.

| Rol | Pantalla | Qué hace |
|---|---|---|
| Ambos | Inicio de sesión | Correo y contraseña; enlace a registro; línea "Servidor: ..." (texto fijo en la release, enlace en debug) |
| Ambos | Servidor (solo debug) | URL de la API, **Probar conexión**, **Guardar**, restablecer. No existe en la release |
| Ambos | Crear cuenta | Rol Paciente o Cuidador, nombre, correo, contraseña |
| Paciente | Hoy | Tomas del día con tres etiquetas, botón **Tomado** (un toque), última sincronización, aviso de sin conexión, tarjeta de permisos |
| Paciente | Medicamentos | Lista con horarios; alta, edición (horas) y desactivación |
| Paciente | Historial | Últimos 7 días por día, con resumen |
| Paciente | Cuidador | Generar código (10 min), aceptar o rechazar solicitud, desvincular |
| Paciente | Ajustes | Anticipación del aviso (5 o 10 min) y estado de permisos |
| Paciente | Diálogo al salir | Bloquea el cierre de sesión si hay confirmaciones sin enviar |
| Paciente | Notificaciones | Ver sección 6 |
| Paciente | Pantalla de alarma | Hora y medicamentos de la toma, botones Tomado, Posponer 5 min y Posponer 10 min; se muestra sobre la pantalla de bloqueo |
| Cuidador | Mis pacientes | Resumen del día, "Sin confirmar", última sincronización, "Esperando aceptación", **Vincular paciente**, **Cerrar sesión** |
| Cuidador | Vincular paciente | Diálogo para escribir el código |
| Cuidador | Detalle del paciente | Tomas de hoy con etiquetas (sin dosis salvo autorización), **Ver historial de 7 días**, **Dejar de seguir** |
| Cuidador | Historial del paciente | Últimos 7 días sin dosis |
| Cuidador | Notificación | "Toma sin confirmar" a T + 30 min |

## 8. No implementado
- **RF-17:** notificación push al cuidador desde el servidor. Fuera de alcance; la compensa en parte la alarma local del cuidador a T + 30 (RF-18), que solo funciona si el cuidador abrió la app ese día.
- **G-07 (pantalla):** no hay pantalla para que el paciente autorice mostrar la dosis al cuidador. La dosis queda oculta por defecto (`cd_links.share_dose = 0`); solo se puede activar en la base de datos.
- Borrar la cuenta desde la app o la API: no existe; las cuentas de prueba se borran en la base de datos.
- Más de un cuidador activo por paciente, reportes y recomendaciones de dosis: fuera de alcance.

## 9. Limitaciones conocidas
- **Token vencido con confirmaciones pendientes:** si la sesión vence (401, tras 30 días) mientras hay confirmaciones sin enviar, la app borra la sesión y la copia local, y esas confirmaciones se pierden. El cierre de sesión manual sí está protegido (bloquea "Salir").
- Sin WorkManager: si el sistema cierra la app sin red, las confirmaciones pendientes se envían la próxima vez que la app arranque.
- La pestaña Medicamentos y el historial necesitan conexión.
- La alarma del cuidador solo cubre las tomas de los días en que abrió la app.
- Las horas son de México (`America/Mexico_City`); no se soporta viajar entre zonas horarias.
- La release no permite cambiar de servidor: si el hosting cambia, hay que publicar otra versión.
- La protección anti-bots del hosting puede bloquear unos minutos una IP que hace muchas peticiones seguidas; con uso normal no se ha visto.
- Verificación física en un solo modelo (POCO M6 Pro); otros fabricantes pueden restringir más los avisos.
