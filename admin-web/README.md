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

## Mis cursos

<http://localhost:3000/cursos> muestra la tabla con los cursos del docente con sesión (nombre, lenguaje y cantidad de problemas), cargada con `peticion`. Necesita los tokens guardados en `localStorage` (claves `access` y `refresh`); mientras no exista la pantalla de login, se obtienen con el login de `admin-api` y se guardan a mano desde la consola del navegador.

## Nuevo curso

<http://localhost:3000/cursos/nuevo> muestra el formulario para crear un curso (nombre y lenguaje: Python, Java o JavaScript). Usa `peticion`, así que necesita los tokens en `localStorage`, igual que `/cursos`. El docente dueño del curso lo decide `admin-api` a partir del token.

## Inicio de sesión y sesión expirada

<http://localhost:3000/login> inicia la sesión del docente y lleva a `/cursos`. Si la sesión expira del todo (el refresh venció o fue rechazado), cualquier pantalla que use `peticion` borra la sesión, recuerda la ruta en `sessionStorage` y lleva a `/login` con un aviso; al volver a entrar, regresa a esa ruta.

Para probar la expiración: inicia sesión, cambia a mano los valores de `access` y `refresh` en `localStorage` (herramientas del navegador) y abre `/cursos`.

## Nuevo problema

<http://localhost:3000/problemas/nuevo> crea un problema en uno de los cursos del docente: curso, título, enunciado (editor Tiptap con negrita, cursiva, título, listas y bloque de código; se guarda como HTML), lenguaje, dificultad, plataforma (Web, Móvil o Ambas; por defecto Web) y tipo de ejercicio (por defecto Escritura de código). Necesita sesión: entra antes por `/login`.

Según el tipo de ejercicio se muestra solo su sección de respuesta (casos de prueba, alternativas, salida esperada o texto que completa el espacio); por ahora cada una muestra «Esta sección estará disponible próximamente.». En `Formulario`, un campo con `mostrarSi(valores)` solo se ve y se envía cuando devuelve `true`.

Si `admin-api` rechaza una combinación imposible de plataforma y tipo de ejercicio, su mensaje aparece en rojo bajo «Plataforma» (`role="alert"`, con `aria-invalid` y `aria-describedby` en el campo). En todos los formularios, el error de un campo se oculta en cuanto se modifica ese campo.

Con «Escritura de código», «Casos de prueba» (`components/CasosPrueba.jsx`) permite agregar y quitar casos (entrada y salida esperada); se envían en el mismo POST como `casos_prueba`, sin recortar los textos, y se descartan solo los casos totalmente en blanco. Los errores de `admin-api` por caso aparecen bajo el campo del caso.

## Cerrar sesión

Con sesión guardada, todas las pantallas salvo `/login` muestran arriba a la derecha el botón «Cerrar sesión». Al pulsarlo se envía el refresh a `admin-api` (`/api/auth/logout/`) para revocarlo, se borran los tokens y la ruta recordada, y se va a `/login`. Si `admin-api` no responde, la sesión del navegador se cierra igual. El access ya emitido sigue valiendo hasta que vence (15 minutos).

En `/cursos` la tabla muestra 10 cursos por página (se pagina en el navegador, porque `admin-api` devuelve la lista completa) y, si el docente no tiene cursos, un mensaje con el vínculo «Crear mi primer curso».

El buscador de `/cursos` filtra por nombre al pulsar «Buscar» o Enter (envía `?search=` a `admin-api`; coincidencia parcial y sin distinguir mayúsculas) y «Limpiar» vuelve a la lista completa.

La columna «Acciones» de `/cursos` tiene «Eliminar», con confirmación en la misma fila. Si `admin-api` bloquea el borrado (409, el curso tiene problemas), su mensaje se muestra en un aviso informativo azul, no como error.

«Editar» lleva a <http://localhost:3000/cursos/ID/editar>: el mismo formulario de «Nuevo curso» con el nombre y el lenguaje actuales; «Guardar cambios» los envía con PUT y vuelve a `/cursos`.

Antes de guardar, un cuadro de confirmación (`components/Confirmacion.jsx`, sobre `<dialog>`) resume qué cambia; si no cambió nada no se abre y aparece «No hay cambios para guardar.».
