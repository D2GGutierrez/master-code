# Habilitadores de arranque

Los documentos de tareas suponen que cada servicio ya existe. Como este repositorio empieza vacío, estas tareas van antes que las del Sprint 1. Cada habilitador se trabaja como una tarea normal: un commit en la rama personal, un Pull Request revisado hacia `main` cuando funciona y el control de entendimiento. Las horas se estiman en el Planning; cuentan contra la capacidad del Sprint 1.

Errores ya vividos que todo habilitador debe evitar: schema sin crear antes de migrar, host de Docker frente a `localhost`, CORS, puertos repetidos, Gradle Wrapper ausente y finales de línea CRLF.

| ID | Tarea | Resp. | Depende de | Hecho cuando |
| :--- | :--- | :--- | :--- | :--- |
| H1 | PostgreSQL con los schemas `admin_schema` y `app_schema` en `docker compose`. | Juan Diego | — | Incluido en el esqueleto: `docker compose up -d db` levanta la base y `\dn` muestra los dos schemas. |
| H2 | Esqueleto de `admin-api`: proyecto Django con DRF, conexión a `admin_schema`, `GET /health`, una prueba mínima, Dockerfile y workflow de CI (lint, test, build). | Juan Diego | H1 | `GET /health` responde `ok`, la prueba pasa y el CI está en verde. |
| H3 | Esqueleto de `user-api`: Spring Boot 3.3.4, Java 21, Maven con wrapper, Flyway sobre `app_schema`, `GET /health` público, una prueba mínima, Dockerfile y workflow de CI. | Juan Diego | H1 | `mvn clean verify` pasa, Flyway migra sin errores y `/health` responde `ok`. |
| H4 | Esqueleto de `admin-web`: React con una página mínima, Dockerfile y workflow de CI. | Carlos | — | `npm run build` y las pruebas pasan, la página abre en el puerto 3000. |
| H5 | Esqueleto de `user-web`: React + Vite + TypeScript con una página mínima, Dockerfile y workflow de CI. | Carlos | — | `npm run build` y las pruebas pasan, la página abre en el puerto 3001. |
| H6 | Esqueleto de `mobile`: Kotlin + Jetpack Compose con una pantalla mínima, Gradle Wrapper y workflow de CI. | Gael | — | `./gradlew test` y el build pasan y la app abre en un emulador. |
| H7 | Esqueleto de `ai-service`: FastAPI con `GET /health`, una prueba mínima, Dockerfile y workflow de CI. | Juan Diego | — | `GET /health` responde `ok` en el puerto 8002 y el CI está en verde. |
| H8 | `docker-compose.yml` unificado: la base y los servicios web y de backend en la misma red, con los puertos 3000, 3001, 8000, 8081 y 8002 y sus variables en `.env.example`. | Juan Diego | H2, H3, H4, H5, H7 | `docker compose up --build` levanta todo sin pasos manuales y los `/health` responden. |
| H9 | Tablero de Trello con las historias y tareas (IDs `HU09-T1`). | Gael | — | Cada tarea de `docs/historias-tareas.md` tiene su tarjeta con responsable. |
| H10 | Protecciones de rama en GitHub y `CODEOWNERS` con los usuarios reales. | Juan Diego | — | Un push directo a `main` es rechazado. |
