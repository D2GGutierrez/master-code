# user-web

Web del estudiante de MASTER CODE (React + Vite, TypeScript). Usa Node 24 y npm.

Todos los comandos se ejecutan dentro de `user-web/`. Son iguales en Windows (PowerShell) y en Linux/Mac.

## Instalar

```powershell
npm install
```

## Arrancar

```powershell
npm run dev
```

Abre <http://localhost:3001>. Si el puerto 3001 está ocupado, el servidor falla en vez de usar otro.

## Revisar y construir

```powershell
npm run lint
npm run build
```

`npm run build` también revisa los tipos de TypeScript (`tsc -b`).

## Docker

```powershell
docker build -t master-code-user-web .
docker run --rm -p 3001:3001 master-code-user-web
```

## Conexión con admin-api

El registro llama a `admin-api` directamente desde el navegador (admin-api ya permite el origen `http://localhost:3001` por CORS). La URL base es `http://localhost:8000`, o el valor de la variable `VITE_API_URL` si existe; Vite la lee al arrancar o al construir.

Para probar el registro del estudiante: levanta `admin-api` en el puerto 8000 (ver su README), arranca `npm run dev` y abre <http://localhost:3001>. La cuenta queda activa al crearse.

## Inicio de sesión

<http://localhost:3001/login> muestra el inicio de sesión; cualquier otra ruta muestra el registro. Al iniciar sesión se guardan el access y el refresh token en `localStorage`, y la sesión se renueva sola 60 segundos antes de que venza el access (también tras recargar la página). Si la renovación falla, la sesión se borra.

Para probarlo: levanta `admin-api` en el puerto 8000, regístrate en <http://localhost:3001> (la cuenta del estudiante queda activa) e inicia sesión en `/login`.

«Olvidé mi contraseña» en `/login` lleva a `/olvide-contrasena`, que pide a `admin-api` el enlace de recuperación; el enlace del correo abre `/nueva-contrasena?uid=…&token=…` para elegir la nueva contraseña.

## Mi avance

<http://localhost:3001/avance> muestra el avance del estudiante (envíos desde la web y el móvil) con `GET /api/avance` de `user-api`, cuya URL base es `http://localhost:8081` o el valor de `VITE_USER_API_URL`; sin sesión lleva a `/login`.

## Editor de código

`src/components/EditorCodigo.tsx` envuelve el editor Monaco (`@monaco-editor/react`); Monaco se descarga de un CDN al abrir el editor, así que necesita conexión a internet. <http://localhost:3001/editor-prueba> es una página provisional para probarlo (cuenta caracteres y líneas) y se reemplazará por la pantalla del problema (HU33).

«Ejecutar» (`src/components/EjecutorCodigo.tsx`) corre el código con la entrada opcional mediante `POST /api/ejecuciones` de `user-api` y muestra la salida o el error, sin crear un envío; exige sesión, así que en `/editor-prueba` sin sesión lleva a `/login`.

`idMonaco` (`src/lenguajes.ts`) traduce el lenguaje del curso o problema (`python`, `java`, `javascript`) al resaltado de Monaco; cualquier otro valor se muestra como texto plano.

## Mis cursos

Al iniciar sesión se llega a <http://localhost:3001/inicio>: una tarjeta por curso inscrito (nombre, lenguaje y fecha de inscripción) con `GET /api/mis-cursos` de `user-api`; sin sesión lleva a `/login`.

Cada tarjeta de `/inicio` lleva a <http://localhost:3001/cursos/ID/problemas>: los problemas publicados del curso para la web (`GET /api/cursos/{id}/problemas?plataforma=web` de `user-api`), con su dificultad y tipo de ejercicio; aún no se abren (HU32).

<http://localhost:3001/cursos/buscar> («Buscar cursos» en `/inicio`) lista los cursos publicados de la institución del estudiante y los busca por nombre con `GET /api/cursos?buscar=` de `user-api`; para inscribirse hace falta el código de invitación del docente.

<http://localhost:3001/problemas/ID> muestra un problema (enlace desde el título en el listado del curso): título, dificultad, tipo, enunciado y los ejemplos públicos. El enunciado se guarda como HTML sin sanitizar, así que `src/enunciado.ts` lo limpia con `dompurify` (solo etiquetas de texto y enlaces http/https/mailto) antes de mostrarlo; es el único `dangerouslySetInnerHTML` del proyecto.

El listado de `/cursos/ID/problemas` se ordena en el navegador (dificultad de fácil a difícil por defecto, o por fecha de publicación) y se pagina de 10 en 10.

El listado se filtra por dificultad y por estado (resuelto, intentado o sin abrir) con botones; el filtro lo aplica `user-api` y el último usado se recuerda en `localStorage` (`mc_filtro_problemas`), igual para todos los cursos.

En un problema de escritura de código, «Ir al editor de código» lleva a <http://localhost:3001/problemas/ID/editor>: el editor con «Ejecutar» en el lenguaje del problema (pantalla mínima; HU33 agregará el enunciado al lado). Los demás tipos no abren el editor.
