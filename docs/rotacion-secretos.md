# Rotación de secretos y revisión de seguridad (C4)

Esta guía cierra la parte de secretos de C4. Quien rota los secretos eres tú (Juan Diego): se cambian en las cuentas de Google, en tu `.env` local y en los contenedores, y ninguna herramienta del asistente puede ni debe verlos. Nada de esta guía lleva valores reales.

## Qué se revisó en el repositorio

- Archivos versionados hoy: sin claves de API, tokens, llaves privadas ni contraseñas escritas en código, pruebas, `docker-compose.yml` ni `.env.example` (los valores van por variables de entorno y `.env.example` solo trae valores falsos). El único texto parecido es el ejemplo `JWT_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...` de `docs/claves-jwt.md`, que es un marcador.
- Historial de Git (183 commits): **un hallazgo.** El commit `da1bae4` («docs: flujo con ramas personales…») **borró de `.env.example`** una cuenta de Gmail y su `EMAIL_HOST_PASSWORD` con aspecto de contraseña de aplicación real. Aunque ya no esté en el archivo actual, sigue en el historial: **trátala como expuesta** (primer paso de abajo). No se encontraron en el historial valores con la forma de una clave de Gemini, de la API interna ni de Judge0.
- `.env` no se versiona y no se leyó.

## Qué rotar, en este orden

Rota primero lo que da acceso a cuentas externas.

1. **Contraseña de aplicación de Gmail** (`EMAIL_HOST_PASSWORD`, y la que quedó en el historial).
   - Entra a https://myaccount.google.com/apppasswords con la cuenta remitente, **revoca** la contraseña de aplicación antigua y crea una nueva.
   - Pégala en tu `.env` local (`EMAIL_HOST_PASSWORD`). No la pegues en chats, capturas ni commits.
   - Reinicia `admin-api` (`docker compose up -d admin-api`) y prueba «Olvidé mi contraseña» (HU03): debe llegar el correo.
   - Opcional recomendado: usar una cuenta de Gmail **solo para la plataforma** en lugar de la personal.
2. **Clave de Gemini** (`LLM_API_KEY`).
   - En https://aistudio.google.com/apikey **elimina** la clave expuesta y crea otra. Si puedes, restríngela (solo la API de Generative Language).
   - Actualiza `LLM_API_KEY` en tu `.env` y reinicia `ai-service` (`docker compose up -d ai-service`). Prueba generando un problema desde el panel del docente.
3. **Judge0** (`JUDGE0_DB_PASSWORD` y `JUDGE0_REDIS_PASSWORD`; y la clave de autenticación del servidor si la activaste, ver docs/judge0.md).
   - Genera valores nuevos: `python -c "import secrets; print(secrets.token_urlsafe(24))"`.
   - La base de Judge0 guarda la contraseña vieja en su volumen: no tiene datos que importen, así que lo más simple es recrearla: `docker compose rm -sf judge0-server judge0-worker judge0-db judge0-redis`, luego `docker volume rm` del volumen de `judge0-db` (ver `docker volume ls`) y `docker compose up -d`.
4. **Clave interna entre admin-api y user-api** (`INTERNAL_API_KEY`, D3).
   - Genera una nueva con el comando de `.env.example`. Es **la misma** para las dos APIs (las dos leen tu `.env`).
   - Reinicia **las dos** (`docker compose up -d admin-api user-api`); entre un reinicio y otro las llamadas internas dan 401.

Si el CI de GitHub usa alguno de estos valores como *secret* (Settings > Secrets and variables > Actions), actualízalo allí también.

## Después de rotar

- Corre `docker compose ps` y mira que todo esté `healthy`; entra a user-web y a admin-web una vez.
- **Limpiar el historial es opcional.** Con la contraseña revocada, el valor viejo ya no sirve. Reescribir el historial (`git filter-repo`) rompe las ramas de los tres y es una decisión del equipo; no se hizo.
- Regla del equipo: los secretos solo viven en `.env` local. Si alguno se pega en un chat, una captura o un commit, se rota al momento.

## SonarQube: qué falta

Los resultados del análisis viven en tu servidor local (`make sonar`, http://localhost:9000/dashboard?id=master-code) y no se pueden leer desde aquí. Para corregir «lo grave» (docs/sonarqube.md, «Cómo leer el resultado»), pásame la lista de **Vulnerabilities**, **Security Hotspots** y **Bugs** (una captura por pestaña, o el texto de cada hallazgo con su archivo y línea) y los corrijo en el siguiente lote; los *code smells* y la duplicación se dejan para después.
