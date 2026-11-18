# Notificaciones push en la app móvil (HU52-T1 y HU52-T3)

Guía para quien haga la parte de la app (Gael). `user-api` ya tiene el lado del servidor: guarda el token de cada teléfono (ADR-9, D16). La app ya tiene **todo lo que no necesita Firebase** (registro del token, permiso de notificaciones, reintento y ajustes de avisos). Falta el SDK de Firebase, y eso **no** se agrega sin confirmarlo con el equipo: las dependencias y el proyecto de Firebase (H5) se aprueban antes.

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

Las pruebas (`DispositivoApiTest`, `DispositivoRepositoryTest`, `AvisosViewModelTest`) corren en la JVM, sin Firebase ni teléfono.

## Dónde se guardan los ajustes de avisos (HU52-T3)

**Solo en el teléfono**, en `SharedPreferences` (archivo `master_code_local`, claves `avisos_activados`, `avisos_hora` y `avisos_minuto`). Por defecto: avisos **activados** y hora **20:00**.

> **Límite que hay que tener presente:** mientras los ajustes vivan solo en el teléfono, **`user-api` no sabe** si el estudiante los desactivó ni a qué hora quiere el aviso. Si HU52-T2 (Juan) manda el aviso desde el servidor, tiene que conocerlos. La opción prevista es una ruta nueva (por ejemplo `PUT /api/yo/avisos` con `{"activados": true, "hora": "20:00"}`) que la app llame al cambiar los ajustes; esa ruta es de HU52-T2 y no existe todavía. Hasta entonces, desactivar los avisos en la app **no** detiene un aviso que mande el servidor. La hora se guarda como hora local del teléfono (la zona ya la conoce `user-api`, H6).

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

Esta prueba en un teléfono real es **HU52-T4** y no se puede hacer hasta que existan el proyecto de Firebase y las dependencias.

## Fuera de esta tarea

Enviar notificaciones desde `user-api` necesita una cuenta de servicio de Firebase: esas credenciales **no van al repositorio** y no se incluyen en este sprint. Cada tarea de notificaciones (por ejemplo HU49) decidirá qué mensajes se envían y cuándo.

## Dependencias formales

HU52-T1 depende de HU49-T1 y de H5 (proyecto de Firebase). La app queda lista para recibir el token en cuanto H5 exista; el servidor, también.
