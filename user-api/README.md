# user-api

API del estudiante de MASTER CODE (Spring Boot 3.3.4, Java 21). Usa la base PostgreSQL del `docker-compose.yml` de la raíz, solo en `app_schema`, y lee la conexión de las variables `POSTGRES_*` del entorno (las mismas del `.env` de la raíz).

Todos los comandos se ejecutan dentro de `user-api/` y con la base levantada (`docker compose up -d db` desde la raíz). El wrapper descarga Maven solo; basta con tener Java 21.

## Compilar y arrancar (Windows, PowerShell)

```powershell
.\mvnw.cmd -B verify
.\mvnw.cmd spring-boot:run
```

En Linux/Mac (o Git Bash): usa `./mvnw` en lugar de `.\mvnw.cmd`.

Comprueba que responde en <http://localhost:8081/health>.

## Seguridad (JWT de admin-api)

user-api no tiene login ni la clave privada: solo verifica los tokens que firma admin-api (RS256) con la clave pública que publica en su JWKS. Todo `/api/**` exige un access token válido con `rol = estudiante`; un docente recibe 403. `/health` es público.

| Variable | Por defecto | Para qué |
|---|---|---|
| `JWKS_URI` | `http://localhost:8000/.well-known/jwks.json` | Dónde descargar la clave pública (en Docker: `http://admin-api:8000/.well-known/jwks.json`). |
| `JWT_ISSUER` | `master-code-admin-api` | Debe ser igual al `ISSUER` de `SIMPLE_JWT` en admin-api. |

El JWKS se descarga en la primera petición, así que user-api puede arrancar antes que admin-api. La primera petición de un estudiante crea su perfil en `app_schema.app_estudiante`.

`GET /api/yo` devuelve los datos del token y el id del perfil:

```powershell
curl.exe -H "Authorization: Bearer <access_token_de_estudiante>" http://localhost:8081/api/yo
# {"email":"<correo>","rol":"estudiante","institucion":"<dominio>","cliente":"user-web","perfil":{"id":<id>}}
```

El access se obtiene con `POST http://localhost:8000/api/auth/login/` y `"cliente": "user-web"`.

**Plan B** (si el JWKS diera problemas): Spring también puede leer la clave pública desde un archivo PEM con `spring.security.oauth2.resourceserver.jwt.public-key-location`. Habría que quitar el `JwtDecoder` de `SeguridadConfig` y nunca configurar a la vez `jwk-set-uri` y `public-key-location`, porque la autoconfiguración queda ambigua.

## Imagen de Docker

```powershell
docker build -t master-code-user-api .
```
