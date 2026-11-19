# Notificaciones push en la app móvil (HU52-T1, HU52-T3 y HU52-T4)

Guía para quien haga la parte de la app (Gael). `user-api` ya tiene el lado del servidor: guarda el token de cada teléfono (ADR-9, D16). La app ya tiene **todo lo que no necesita Firebase** (registro del token, permiso de notificaciones, reintento, ajustes de avisos y su envío a `user-api`). Falta el SDK de Firebase, y eso **no** se agrega sin confirmarlo con el equipo: las dependencias y el proyecto de Firebase (H5) se aprueban antes.

## Qué ya existe en `user-api`

- `PUT /api/dispositivo` con cuerpo `{"token": "..."}` y el token de sesión del estudiante (el mismo `Authorization: Bearer` de siempre).
- Respuestas: **204** si se guardó (repetirlo no cambia nada), **400** si el token falta, está en blanco o pasa de 1 024 caracteres, **401** sin sesión.
- Si otro estudiante inicia sesión en el mismo teléfono y registra el mismo token, el token pasa a ser suyo. Un estudiante puede tener varios teléfonos.
- Tabla `app_schema.app_dispositivo` (migración V10).

## Qué ya existe en la app (sin Firebase)

| Pieza | Dónde | Qué hace |
| :--- | :--- | :--- |
| `HttpDispositivoApi` | `data/dispositivo/DispositivoApi.kt` | `PUT /api/dispositivo` con `{"token": ...}`; solo el 204 es éxito. |
| `DispositivoRepository` | `data/dispositivo/DispositivoRepository.kt` | `registrarElDelTelefono()` (al abrir con sesión) y `alRenovarseElToken(token)` (cuando Firebase lo cambie). Usa `conAccess`: renueva el access si venció. |
| `ProveedorDeToken` | mismo archivo | De dónde sale el token. Hoy es `SinFirebase` (devuelve `null`, no se llama a `user-api`). **Es lo único que se cambia al agregar Firebase.** |
| `AlmacenDeToken` / `PreferenciasDeToken` | `data/dispositivo/` | El token que no pudo enviarse (sin conexión, sesión vencida o error 5xx) queda guardado y se manda en la próxima apertura. Un 400 (token rechazado) se descarta: reintentarlo siempre fallaría. |
| `POST_NOTIFICATIONS` | `AndroidManifest.xml` y `AvisosScreen` | El permiso se declara y, desde Android 13, se pide al abrir «Avisos de práctica» o al activar el interruptor. Si el estudiante lo niega, los avisos quedan desactivados. |
| Ajustes de avisos (HU52-T3) | `ui/avisos/` y `data/avisos/` | Interruptor para desactivar los avisos (CA-52.3) y selector de hora (CA-52.4). Se abre desde «Mis cursos» → «Avisos de práctica». |
| Envío de los ajustes (HU52-T4) | `data/avisos/AvisosApi.kt` | `HttpAvisosApi` hace `PUT /api/yo/avisos` con `{"activados": ..., "hora": "HH:mm"}` (solo el 204 es éxito). `AvisosRepository.enviar()` usa `conAccess`. Se llama en cada cambio de «Avisos de práctica» (`MasterCodeApp.enviarAvisos`, que sigue aunque el estudiante salga de la pantalla) y al abrir la app con sesión (`MainActivity`), así lo que no llegó por falta de señal llega después. Un fallo no se muestra al estudiante. |

Las pruebas (`DispositivoApiTest`, `DispositivoRepositoryTest`, `AvisosViewModelTest`, `AvisosApiTest`) corren en la JVM, sin Firebase ni teléfono.

## Dónde se guardan los ajustes de avisos (HU52-T3)

**Solo en el teléfono**, en `SharedPreferences` (archivo `master_code_local`, claves `avisos_activados`, `avisos_hora` y `avisos_minuto`). Por defecto: avisos **activados** y hora **20:00**.

> **El servidor ya tiene la ruta (HU52-T2, ADR-15 D26) y la app ya la llama (HU52-T4, ADR-15 D27):** `PUT /api/yo/avisos` con `{"activados": true, "hora": "20:00"}` (`hora` es `HH:mm`, en la hora local del teléfono; si no se manda, queda la que ya tenía). Responde 204, y 400 si `activados` falta o la hora no tiene ese formato. La app la llama cada vez que el estudiante cambia los ajustes en «Avisos de práctica» y al abrir sesión, por si los ajustes se cambiaron sin conexión; un envío que falla se reintenta la próxima vez que se abra la app con sesión. Con eso, desactivar los avisos en la app **sí** detiene el aviso del servidor en cuanto el envío llega (antes de eso el servidor usa los valores por defecto: activados, 20:00). El servidor decide el aviso con el día local del estudiante (H6): avisa una vez al día a quien practicó ayer y todavía no hoy. Hoy no sale ninguno porque no hay Firebase (`EnviadorSinFirebase`) y la tarea está apagada (`AVISOS_TAREA_ACTIVA=false`).

## Qué falta para tener push de verdad

1. **Proyecto de Firebase (H5).** Crear el proyecto en la consola de Firebase, registrar la app Android con su `applicationId` y descargar `google-services.json`. Ese archivo **no se sube al repositorio** (agregarlo al `.gitignore` de `mobile/`).
2. **Dependencias (preguntar al equipo antes).** Con el catálogo de versiones o sin él, son estas líneas (confirmar las versiones vigentes al agregarlas):

   `mobile/build.gradle.kts` (raíz):

   ```kotlin
   plugins {
       id("com.google.gms.google-services") version "<versión>" apply false
   }
   ```

   `mobile/app/build.gradle.kts`:

   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }

   dependencies {
       implementation(platform("com.google.firebase:firebase-bom:<versión>"))
       implementation("com.google.firebase:firebase-messaging")
   }
   ```

3. **El proveedor real del token.** Una clase que reemplaza a `SinFirebase`. Se escribe con `suspendCancellableCoroutine` para no agregar otra dependencia (`kotlinx-coroutines-play-services`):

   ```kotlin
   object TokenDeFirebase : ProveedorDeToken {
       override suspend fun obtener(): String? = suspendCancellableCoroutine { continuacion ->
           FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
               continuacion.resume(if (tarea.isSuccessful) tarea.result else null)
           }
       }
   }
   ```

   Y en `MasterCodeApp`, `DispositivoRepository(..., TokenDeFirebase, PreferenciasDeToken(this))` en lugar de `SinFirebase`.

4. **Renovación del token.** El servicio que Firebase avisa cuando cambia el token:

   ```kotlin
   class ServicioDeAvisos : FirebaseMessagingService() {
       override fun onNewToken(token: String) {
           val app = application as MasterCodeApp
           // Fuera del hilo principal. Si no hay sesión o señal, el repositorio guarda el token y lo manda en la próxima apertura.
           CoroutineScope(Dispatchers.IO).launch { app.dispositivoRepository.alRenovarseElToken(token) }
       }
   }
   ```

   Y se declara en `AndroidManifest.xml`:

   ```xml
   <service android:name=".ServicioDeAvisos" android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
   ```

5. **Canal de notificaciones (Android 8 o superior).** Crear un canal «Avisos de práctica» y declararlo con `com.google.firebase.messaging.default_notification_channel_id` en el manifiesto, para que el estudiante pueda silenciarlo desde los ajustes del sistema.
6. **Cerrar sesión.** Por ahora no hay ruta para borrar el token; si se necesita, se agrega aparte.

## Cómo probar sin código de servidor

1. Iniciar sesión en la app con un estudiante de prueba y comprobar que el `PUT` devuelve 204.
2. Copiar el token (por ejemplo desde el registro de la app, **solo en builds de desarrollo**).
3. En la consola de Firebase: *Engage > Messaging > Nueva campaña > Mensaje de prueba*, pegar el token y enviar.
4. La notificación debe aparecer en el teléfono (con la app en segundo plano).

Esta prueba en un teléfono real es **HU52-T4**; está descrita paso a paso en la sección siguiente y no se puede completar hasta que existan el proyecto de Firebase y las dependencias.

## HU52-T4: el aviso llega a un teléfono real

**Estado: lista para probar, todavía no probada.** La app ya manda los ajustes al servidor y el servidor ya decide a quién avisar, pero faltan dos cosas que no dependen de este código: el proyecto de Firebase con sus dependencias (H5, las aprueba el equipo) y un enviador real en `user-api` (hoy `EnviadorSinFirebase` no envía nada y `AVISOS_TAREA_ACTIVA=false`). Por eso la prueba se divide: la parte A se puede hacer hoy; la parte B necesita Firebase y un teléfono Android físico con Google Play Services.

### Parte A. Los ajustes llegan al servidor (hoy, en emulador o teléfono)

1. Iniciar sesión con una cuenta de prueba (por ejemplo `ana.torres.demo`). Al abrir la app con sesión se manda el ajuste guardado (por defecto: activados, 20:00).
2. En la base de `user-api`: `SELECT activados, hora FROM app_schema.app_aviso_ajuste WHERE estudiante_id = (SELECT id FROM app_schema.app_estudiante WHERE email = '<correo de la cuenta>');`. **Esperado:** una fila con `true` y `20:00:00`.
3. «Mis cursos» → **Avisos de práctica** → **Cambiar la hora** → 07:30. Repetir la consulta. **Esperado:** `07:30:00`.
4. Apagar el interruptor y repetir la consulta. **Esperado:** `activados = false` y la hora no cambia.
5. Poner el modo avión, cambiar la hora a 08:15 y salir de la pantalla. Quitar el modo avión, **cerrar la app de verdad** y abrirla con la sesión iniciada. **Esperado:** la consulta da `08:15:00` (el envío se reintenta al abrir).
6. En todos los pasos, aunque el envío falle, la app **no** muestra ningún error.

### Parte B. El aviso llega (necesita Firebase)

Requisitos: teléfono físico con la app de depuración conectada al servidor de la PC (ver `docs/recorrido-aceptacion-movil.md` §1 punto 3) y el permiso de notificaciones concedido (Android 13 o más: se pide al abrir «Avisos de práctica»).

1. Iniciar sesión y comprobar que el token se registró: `SELECT count(*) FROM app_schema.app_dispositivo WHERE estudiante_id = <id>;` da 1 o más (CA-52.2).
2. En la consola de Firebase, enviar un mensaje de prueba a ese token con la app en segundo plano. **Esperado:** la notificación aparece (comprueba Firebase → teléfono, sin pasar por `user-api`).
3. **El aviso real.** Con el enviador real puesto y `AVISOS_TAREA_ACTIVA=true` (para la prueba, `AVISOS_INTERVALO=PT1M`): dejar a la cuenta con la racha viva —un acierto **ayer** y ninguno hoy, ajustando la fecha del dato de prueba como en CA-49.4 del recorrido— y poner en la app la hora de aviso 2 minutos más adelante. **Esperado:** pasada esa hora llega el aviso con los días de racha en peligro (CA-52.1).
4. Esperar otros minutos. **Esperado:** el aviso **no** llega dos veces el mismo día.
5. Apagar los avisos en la app, esperar a que el envío llegue (se ve en la base, como en la parte A) y repetir el paso 3 con la hora 2 minutos adelante. **Esperado:** **no** llega (CA-52.3).
6. Repetir el paso 3 con la app **cerrada de verdad**. **Esperado:** llega igual.
7. Con la misma cuenta en dos teléfonos, **Esperado:** llega a los dos.

Anotar: modelo y versión de Android, hora configurada contra hora real de llegada, y una captura de cada aviso. Si algo no llega, revisar primero el permiso de notificaciones, el ahorro de batería del fabricante y que el token del paso 1 siga en `app_dispositivo`.

## Fuera de esta tarea

Enviar notificaciones desde `user-api` necesita una cuenta de servicio de Firebase: esas credenciales **no van al repositorio** y no se incluyen en este sprint. Cada tarea de notificaciones (por ejemplo HU49) decidirá qué mensajes se envían y cuándo.

## Dependencias formales

HU52-T1 depende de HU49-T1 y de H5 (proyecto de Firebase). La app queda lista para recibir el token en cuanto H5 exista; el servidor, también.
