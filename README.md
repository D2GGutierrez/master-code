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
| `infra/` | Configuración de apoyo: el SQL que crea los dos schemas y los scripts de la prueba de humo y de los datos de demostración. |
| `docs/` | Decisiones, historias de usuario, criterios de aceptación, flujo de Git y guías. |

## Estado del proyecto

El MVP web (docente y estudiante) está entregado. La app móvil tiene el inicio de sesión, «Mis cursos» y las pantallas de predecir la salida y de completar el espacio; en `user-api` ya funcionan la corrección de los ejercicios del celular, el Dojo (sesión y siguiente ejercicio) y los cinturones con sus puntos. **Lo que no se terminó está en [`docs/fuera-del-mvp.md`](docs/fuera-del-mvp.md)**, historia por historia y con los criterios de aceptación que no se cumplen. Cómo presentarlo: [`docs/guia-demo.md`](docs/guia-demo.md).

## Cómo levantar todo

```
cp .env.example .env
docker compose up -d
```

Levanta la base y los cinco servicios: `admin-api` en http://localhost:8000, `admin-web` en http://localhost:3000, `user-api` en http://localhost:8081, `user-web` en http://localhost:3001 y `ai-service` en http://localhost:8002 (la base queda en el puerto 5432). admin-api aplica sus migraciones al arrancar y user-api las suyas con Flyway. También sirven `make up`, `make logs` y `make down`.

- Ver logs: `docker compose logs -f admin-api` (o el servicio que quieras).
- Bajar sin perder datos: `docker compose down`. Los datos quedan en el volumen de la base.
- Comprobar que todo funciona de punta a punta (docente crea un curso, estudiante se inscribe, resuelve y envía su solución): `make humo` o `python infra/scripts/humo.py`. Con el motor de ejecución simulado omite lo que necesita uno real; para activarlo, `docs/piston.md`.
- Correr las pruebas de `user-api` (necesitan la base y las variables del `.env` cargadas): `docker compose up -d db`, y desde `user-api/`, en Git Bash, `set -a; source ../.env; set +a` y `./mvnw test`. Las de `admin-api` (`python manage.py test`), `ai-service` (`pytest`) y `admin-web` (`node --test`) se corren en su carpeta; la app móvil, con `./gradlew testDebugUnitTest` en `mobile/`.
- Dejar la plataforma con datos para la demostración (1 docente, 2 cursos, problemas generados con IA y 6 estudiantes con inscripciones y envíos): `DEMO_CLAVE=<una clave tuya> make datos-demo` o `python infra/scripts/datos_demo.py`. Se puede repetir sin duplicar nada; las cuentas y los códigos de los cursos se muestran al final. Los envíos solo se califican si el motor de ejecución es real (`docs/piston.md`).

El SQL de `infra/db/init/` solo se ejecuta cuando el volumen está vacío; si lo cambias, hay que recrear el volumen con `docker compose down -v` (borra los datos de la base local).

En Windows, usa `copy .env.example .env` en lugar de `cp`.

## Cómo trabajamos

Cada integrante trabaja en su rama personal (`Gutierrez`, `carlos-junco`, `gael-lajara`) con un commit por tarea y el formato `carpeta: qué hace (HU# T#)`. **A `main` solo llega lo que ya funciona en la rama personal** (CI en verde y probado a mano), por Pull Request revisado por otra persona. Antes de empezar se corre `git pull --rebase origin main` en la rama personal. Cuando la fecha de un commit se modifica a pedido del profesor, la descripción del commit trae la fecha real. Reglas completas en [`docs/flujo-git.md`](docs/flujo-git.md). Las tareas y sus criterios están en [`docs/historias-tareas.md`](docs/historias-tareas.md) y [`docs/historias-criterios.md`](docs/historias-criterios.md); las decisiones de diseño, en [`docs/decisiones-tecnicas-adr.md`](docs/decisiones-tecnicas-adr.md). Cada sprint cierra con una etiqueta de Git (`sprint-1` a `sprint-4`, comandos en [`docs/flujo-git.md`](docs/flujo-git.md)).

## Historia del repositorio

El proyecto se reinició al inicio del Sprint 1 para aplicar un estándar de código mínimo y entendible.
