# Verificación del correo con código

Al registrarse con correo y contraseña, la cuenta queda **pendiente de verificar**. admin-api envía un código de 6 dígitos al correo
institucional y la persona lo escribe en la web. Un correo inventado nunca recibe el código, así que no puede entrar.

## Flujo

1. `POST /api/auth/registro/estudiante/` o `/registro/docente/` crea la cuenta y envía el código. Responde `201` con
   `{ detail, email, codigo_enviado }`.
2. La web lleva a `/verificar-correo?email=...`, donde se escribe el código (`POST /api/auth/verificar-correo/` con `{ email, codigo }`).
3. Mientras no se verifique, `POST /api/auth/login/` responde `403` con `{ detail, verificar_correo: true }` (después de comprobar la
   contraseña, para no revelar qué correos existen). El inicio de sesión de las webs muestra el enlace «Escribir el código de mi correo».
4. `POST /api/auth/reenviar-codigo/` envía un código nuevo (uno por minuto; el anterior deja de servir).

## Reglas

- El código vence a los 15 minutos (`VERIFICACION_MINUTOS`) y se invalida tras 5 intentos fallidos.
- No se guarda el código: solo su huella HMAC con `DJANGO_SECRET_KEY`.
- Un correo sin verificar **no bloquea** a su dueño: si alguien lo registró sin ser suyo, el dueño real puede registrarse y reemplaza esa cuenta.
- **Docentes:** al verificar, quedan activos si su correo está en `DOCENTES_AUTORIZADOS`; si no, esperan la activación del Product Owner.
- **Google:** quien entra con Google ya tiene el correo comprobado, así que un código pendiente se da por verificado.
- Las cuentas anteriores a esta función no tienen fila en `admin_verificacion_correo` y cuentan como verificadas.
- Requiere que el correo saliente funcione (`EMAIL_*` en el `.env`, el mismo que usa «Olvidé mi contraseña»).

## Aplicar el cambio

```powershell
docker compose up -d --build admin-api admin-web user-web
```

(`admin-api` aplica la migración `cuentas/0002_verificacion_correo` al arrancar.)
