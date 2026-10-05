# admin-web

Panel web del docente de MASTER CODE (React + Vite, JavaScript). Usa Node 24 y npm.

Todos los comandos se ejecutan dentro de `admin-web/`. Son iguales en Windows (PowerShell) y en Linux/Mac.

## Instalar

```powershell
npm install
```

## Arrancar

```powershell
npm run dev
```

Abre <http://localhost:3000>. Si el puerto 3000 está ocupado, el servidor falla en vez de usar otro.

## Revisar y construir

```powershell
npm run lint
npm run build
```

## Docker

```powershell
docker build -t master-code-admin-web .
docker run --rm -p 3000:3000 master-code-admin-web
```

## Conexión con admin-api

En desarrollo, el front llama a rutas relativas (`/api/...`) y el servidor de Vite las reenvía a `admin-api` (proxy en `vite.config.js`). El destino es `http://localhost:8000`, o el valor de la variable `API_PROXY_TARGET` si existe.

Para probar el registro del docente: levanta `admin-api` en el puerto 8000 (ver su README), arranca `npm run dev` y abre <http://localhost:3000>. La cuenta se crea inactiva hasta que el Product Owner la active.

## Peticiones con sesión

`iniciarSesion` (en `src/api/cuentas.js`) guarda los tokens. Para llamar a `admin-api` con sesión usa `peticion` de `src/api/cliente.js` en vez de `fetch`: agrega `Authorization: Bearer <access>` y, si el servidor responde 401, renueva el token y repite la petición una vez. Devuelve la `Response` de `fetch`.

```js
const respuesta = await peticion('/api/<ruta>/', { method: 'GET' }) // <ruta> es un ejemplo
```

## Recuperación de contraseña

- <http://localhost:3000/olvide-contrasena>: el docente escribe su correo y `admin-api` le envía un enlace.
- <http://localhost:3000/nueva-contrasena?uid=...&token=...>: la abre el enlace del correo; pide la contraseña dos veces y la guarda.

Para probar el flujo sin enviar correos reales, levanta `admin-api` con `EMAIL_BACKEND=django.core.mail.backends.console.EmailBackend`: el correo, con el enlace, aparece en su consola. El enlace vence en 30 minutos y sirve una sola vez.
