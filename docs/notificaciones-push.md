# Notificaciones push en la app móvil (HU52-T1)

Guía para quien haga la parte de la app (Gael). `user-api` ya tiene el lado del servidor: guarda el token de cada teléfono (ADR-9, D16). Falta que la app lo obtenga y lo registre. Esta guía **no** agrega nada al repositorio por sí sola: las dependencias de Firebase se confirman con el equipo antes de añadirlas.

## Qué ya existe en `user-api`

- `PUT /api/dispositivo` con cuerpo `{"token": "..."}` y el token de sesión del estudiante (el mismo `Authorization: Bearer` de siempre).
- Respuestas: **204** si se guardó (repetirlo no cambia nada), **400** si el token falta, está en blanco o pasa de 1 024 caracteres, **401** sin sesión.
- Si otro estudiante inicia sesión en el mismo teléfono y registra el mismo token, el token pasa a ser suyo. Un estudiante puede tener varios teléfonos.
- Tabla `app_schema.app_dispositivo` (migración V10).

## Qué falta en la app

1. **Proyecto de Firebase (H5).** Crear el proyecto en la consola de Firebase, registrar la app Android con su `applicationId` y descargar `google-services.json`. Ese archivo **no se sube al repositorio** (agregarlo al `.gitignore` de `mobile/`).
2. **Dependencias.** El plugin `com.google.gms.google-services` y `firebase-messaging` (con el BOM de Firebase). Preguntar al equipo antes de añadirlas.
3. **Permiso en Android 13 o superior.** Pedir `POST_NOTIFICATIONS` en tiempo de ejecución.
4. **Obtener el token.** `FirebaseMessaging.getInstance().token` al iniciar sesión.
5. **Registrarlo.** Llamar a `PUT /api/dispositivo` con ese token justo después de iniciar sesión.
6. **Renovación.** Implementar `onNewToken` en un `FirebaseMessagingService` y volver a llamar a `PUT /api/dispositivo` cuando Firebase cambie el token (si hay sesión abierta; si no, al próximo inicio de sesión).
7. **Cerrar sesión.** Por ahora no hay ruta para borrar el token; si se necesita, se agrega aparte.

## Cómo probar sin código de servidor

1. Iniciar sesión en la app con un estudiante de prueba y comprobar que el `PUT` devuelve 204.
2. Copiar el token (por ejemplo desde el registro de la app, **solo en builds de desarrollo**).
3. En la consola de Firebase: *Engage > Messaging > Nueva campaña > Mensaje de prueba*, pegar el token y enviar.
4. La notificación debe aparecer en el teléfono (con la app en segundo plano).

## Fuera de esta tarea

Enviar notificaciones desde `user-api` necesita una cuenta de servicio de Firebase: esas credenciales **no van al repositorio** y no se incluyen en este sprint. Cada tarea de notificaciones (por ejemplo HU49) decidirá qué mensajes se envían y cuándo.

## Dependencias formales

HU52-T1 depende de HU49-T1 y de H5 (proyecto de Firebase). Esta guía deja el lado del servidor listo para cuando esas dos existan.
