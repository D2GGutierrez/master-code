# Inicio de sesión con Google

Los estudiantes y docentes pueden entrar con su cuenta institucional de Google (`@tecsup.edu.pe`). Google certifica que el correo
es real; MASTER CODE decide el rol. El inicio con correo y contraseña sigue funcionando.

## Cómo funciona

1. La web o la app muestra el botón **Continuar con Google** y recibe un *token de identidad* firmado por Google.
2. Lo envía a `POST /api/auth/google/` de admin-api con `{ "id_token": "...", "cliente": "admin-web" | "user-web" | "mobile" }`.
3. admin-api verifica la firma con Google y exige: ID de cliente propio como audiencia, `email_verified = true`, dominio del
   correo igual a `DOMINIO_INSTITUCIONAL` **y** `hd` (dominio de Google Workspace) igual a `DOMINIO_INSTITUCIONAL`.
   Una cuenta personal con otro correo, o sin `hd`, no pasa.
4. Responde con los mismos `access` y `refresh` del login normal. user-api no cambia.

| Cliente | Quién entra | Regla |
|---|---|---|
| `admin-web` | Docentes | El correo debe estar en `DOCENTES_AUTORIZADOS` (o ser un docente que el administrador ya activó). La primera vez se crea el perfil de docente, activo. |
| `user-web`, `mobile` | Estudiantes | Cualquier correo institucional. La primera vez se crea la cuenta de estudiante. Un correo de `DOCENTES_AUTORIZADOS` no puede crear cuenta de estudiante. |

Si ya existía una cuenta con ese correo (registrada con contraseña), Google entra a esa misma cuenta y la contraseña sigue valiendo.

## Configuración

### 1. Crear el ID de cliente en Google Cloud

En <https://console.cloud.google.com/> → *APIs y servicios* → *Credenciales*:

1. Configura la **pantalla de consentimiento** (tipo *Interno* si el proyecto está en la organización de Tecsup; si no, *Externo* en modo prueba y agrega los correos de prueba).
2. **Crear credenciales → ID de cliente de OAuth → Aplicación web**. En *Orígenes autorizados de JavaScript* agrega `http://localhost:3000` y `http://localhost:3001` (y las URL de producción cuando existan). Copia el ID (`xxxx.apps.googleusercontent.com`). No es un secreto.
3. Para la app Android, en el **mismo proyecto**: **Crear credenciales → ID de cliente de OAuth → Android**, con el paquete `com.mastercode.app` y el SHA-1 de tu llavero de depuración (`.\gradlew.bat signingReport` en `mobile`). Este ID no se pone en el código; basta con que exista.

### 2. Variables de entorno (`.env` de la raíz)

```
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
DOCENTES_AUTORIZADOS=docente1@tecsup.edu.pe,docente2@tecsup.edu.pe
```

Luego `docker compose up -d --build admin-api admin-web user-web`. Sin `GOOGLE_CLIENT_ID` los botones no se muestran y el endpoint responde 503.

### 3. App móvil

En `mobile/local.properties` (no se sube al repositorio) agrega el **mismo** ID de cliente de tipo *Aplicación web*:

```
google.client.id=xxxx.apps.googleusercontent.com
```

Sin esa línea la app no muestra el botón. El emulador necesita una imagen con *Google Play* y una cuenta de Google iniciada.

## Pruebas

- admin-api: `cuentas/test_google.py` simula la respuesta de Google (sin internet): dominio, `hd`, correo sin verificar, audiencia, lista de docentes, cuentas existentes y errores.
- user-web: `BotonGoogle.test.tsx`.
- móvil: `AuthRepositoryTest` y `LoginViewModelTest` (casos de Google).
