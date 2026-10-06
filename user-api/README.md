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

## Imagen de Docker

```powershell
docker build -t master-code-user-api .
```
