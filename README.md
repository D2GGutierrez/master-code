# MASTER CODE

Plataforma web y móvil para que estudiantes de instituciones entrenen programación resolviendo problemas de código. Los problemas los genera una IA a partir del contexto que da el docente (tema, nivel, lenguaje y objetivos) y el docente los revisa y aprueba antes de publicarlos. El proyecto se construye con Scrum en cuatro sprints (5 de octubre a 27 de noviembre de 2026).

## Equipo

| Integrante | Rol Scrum | Rol técnico | Carpetas |
| :--- | :--- | :--- | :--- |
| Juan Diego Gutiérrez | Product Owner + dev | Backend | `admin-api`, `user-api`, `ai-service` |
| Carlos Junco | Scrum Master + dev | Frontend web | `admin-web`, `user-web` |
| Gael La Jara | Dev + QA | Móvil y pruebas | `mobile` |

## Arquitectura

Dos módulos, cada uno con su frontend y su backend, en un solo repositorio (monorepo):

| Módulo | Frontend | Backend | Puerto |
| :--- | :--- | :--- | :--- |
| Administración (docentes) | `admin-web` (React) | `admin-api` (Django) | 3000 / 8000 |
| Usuario (estudiantes) | `user-web` (React + Vite), `mobile` (Kotlin) | `user-api` (Spring Boot) | 3001 / 8081 |
| IA y sandbox | | `ai-service` (FastAPI) | 8002 |

Una sola base de datos PostgreSQL con dos schemas: `admin_schema` (lo maneja Django) y `app_schema` (lo maneja Spring con Flyway). Ningún servicio lee las tablas del otro. La autenticación la emite `admin-api` con JWT RS256 y `user-api` solo la verifica. Detalle en [`docs/decisiones-tecnicas-adr.md`](docs/decisiones-tecnicas-adr.md) y [`docs/contrato-jwt.md`](docs/contrato-jwt.md).

## Estructura

| Carpeta | Contenido |
| :--- | :--- |
| `admin-api/`, `admin-web/`, `user-api/`, `user-web/`, `mobile/`, `ai-service/` | Un servicio o aplicación por carpeta. Cada una se crea en su habilitador (ver `docs/habilitadores.md`). |
| `infra/` | Configuración de apoyo; por ahora, el SQL que crea los dos schemas. |
| `docs/` | Decisiones, historias de usuario, criterios de aceptación, flujo de Git y guías. |

## Cómo levantar todo

```
cp .env.example .env
docker compose up -d
```

Levanta la base y los cinco servicios: `admin-api` en http://localhost:8000, `admin-web` en http://localhost:3000, `user-api` en http://localhost:8081, `user-web` en http://localhost:3001 y `ai-service` en http://localhost:8002 (la base queda en el puerto 5432). admin-api aplica sus migraciones al arrancar y user-api las suyas con Flyway. También sirven `make up`, `make logs` y `make down`.

- Ver logs: `docker compose logs -f admin-api` (o el servicio que quieras).
- Bajar sin perder datos: `docker compose down`. Los datos quedan en el volumen de la base.

El SQL de `infra/db/init/` solo se ejecuta cuando el volumen está vacío; si lo cambias, hay que recrear el volumen con `docker compose down -v` (borra los datos de la base local).

En Windows, usa `copy .env.example .env` en lugar de `cp`.

## Cómo trabajamos

Se trabaja directo en `main`, sin ramas ni Pull Requests. Antes de cada `git push` se corre `git pull --rebase`, para que los commits de los tres queden en línea. Cada commit corresponde a una tarea y usa el formato `carpeta: qué hace (HU# T#)`. Cuando la fecha de un commit se modifica a pedido del profesor, la descripción del commit trae la fecha real. Las tareas y sus criterios están en [`docs/historias-tareas.md`](docs/historias-tareas.md) y [`docs/historias-criterios.md`](docs/historias-criterios.md).

## Historia del repositorio

El proyecto se reinició al inicio del Sprint 1 para aplicar un estándar de código mínimo y entendible, con trabajo directo en `main`.
