# MASTER CODE — instrucciones para Claude Code

Monorepo `master-code-v2`: plataforma web y móvil para entrenar programación con Scrum; la IA genera los problemas con el contexto del docente. Quien te usa es un estudiante que debe poder explicar cada línea que entregues.

## Carpetas y puertos
admin-api (Django, 8000) · admin-web (React, 3000) · user-api (Spring Boot, 8081) · user-web (React+Vite+TS, 3001) · mobile (Kotlin+Compose) · ai-service (FastAPI, 8002) · infra · docs

## Reglas fijas
- Cada persona trabaja en su rama personal (Juan Diego: `Gutierrez`). A `main` solo llega lo que ya funciona en la rama personal, por Pull Request. No crees ramas ni cambies de rama.
- NUNCA ejecutes `git add`, `git commit`, `git push`, `git merge`, `git pull`, `git rebase` ni `git reset`. Los commits los hago yo, a mano, con el formato `carpeta: qué hace (HU# T#)`. Antes de cada push yo corro `git pull --rebase origin main`.
- Código mínimo: solo lo que la tarea pide. Sin dependencias, archivos ni opciones "por si acaso". Pregunta antes de agregar una dependencia.
- No leas, imprimas ni modifiques `.env`. Solo `.env.example`, y siempre con valores falsos.
- Ningún secreto en el repositorio, en logs ni en tu salida.
- ADR-1: una base PostgreSQL con dos schemas. Django usa `admin_schema`, Spring usa `app_schema`. Sin claves foráneas entre schemas.
- Finales de línea LF. Mi terminal es Git Bash en Windows: usa rutas con barras normales (`.venv/Scripts/python.exe`).
- Si algo contradice el prompt o la documentación, detente y pregúntame.
- Responde y comenta el código en español.

## Documentación (léela solo cuando la tarea lo pida)
docs/historias-tareas.md · docs/historias-criterios.md · docs/decisiones-tecnicas-adr.md · docs/contrato-jwt.md · docs/flujo-git.md · docs/habilitadores.md · docs/registro-cambios.md · docs/modelo-de-datos.md · docs/judge0.md · docs/piston.md
