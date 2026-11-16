# Guía de la demostración (C3)

Para quien presenta el proyecto: cómo dejar todo listo, qué recorrido hacer y qué **no** enseñar porque no está terminado (ver `docs/fuera-del-mvp.md`). Estado al 20 de noviembre de 2026; repásala el día de la demostración.

## 1. Antes de la demostración (en este orden)

1. `cp .env.example .env` (en Windows, `copy .env.example .env`) y completa las claves en el `.env` de tu máquina. Nunca se sube al repositorio.
2. `docker compose up -d --build`. Sin `--build`, compose reutiliza imágenes viejas. Espera a que `docker compose ps` muestre todo arriba.
3. **Motor de ejecución:** Piston, con `docker compose --profile piston up -d piston` y `SANDBOX_PROVIDER=piston` en el `.env` (`docs/piston.md`). Judge0 no funciona en Windows ni en WSL2. Con el motor simulado nada se califica de verdad.
4. **IA:** en el `.env`, `LLM_PROVIDER` y `LLM_MODEL` de un modelo que responda (el 29 de octubre funcionó `gemini-3.5-flash-lite`). Tras cambiar el `.env`: `docker compose up -d ai-service`. Si «Generar» falla con un 503 de Google, vuelve a intentarlo: el código no reintenta.
5. `python ai-service/scripts/probar_sandbox.py` (debe dar 16 de 16) y `python infra/scripts/humo.py` (recorre docente, curso, problema, IA, estudiante, envíos y avance; termina con código 1 si algo falla; con `--sin-ia` si no hay modelo).
6. **Datos de ejemplo:** `DEMO_CLAVE=<una clave tuya> make datos-demo` (1 docente, 2 cursos, problemas generados con IA, 6 estudiantes con envíos). Se puede repetir sin duplicar nada; al final imprime las cuentas y los códigos de invitación. Los envíos solo se califican con un motor real. Todavía **no** siembra intentos móviles, racha ni cinturón (pieza C1 del roadmap).

## 2. Recorrido de la web del docente (admin-web, puerto 3000)

1. Iniciar sesión con la cuenta del docente de la demo → **Mis cursos**: el código de invitación de cada curso y el número de estudiantes inscritos.
2. **Generar con IA** en un curso: tema, nivel, lenguaje, plataforma y cantidad. Lo generado queda en la **bandeja de revisión** como pendiente. Cada problema trae ahora un **tema** que propone la IA.
3. **Nuevo problema** (formulario manual), lo nuevo del Sprint 4 (H3, H4): según el tipo de ejercicio aparecen los casos de prueba y el código base (escritura de código), las **alternativas** con la correcta marcada (opción múltiple), la **salida esperada** (predecir la salida) o el **texto que completa el espacio**, con el hueco marcado con `___` en el enunciado (completar el espacio). El **tema** es opcional. Un ejercicio incompleto se avisa debajo del campo que falta.
4. **Evaluaciones**: crear una con fechas, límite de tiempo y puntaje por problema; el total de puntos se ve al armarla.
5. **Avance y aciertos** de un curso: resueltos, intentados y pendientes por estudiante, y el porcentaje de acierto por problema.

**No enseñar:** la bandeja de revisión solo tiene «Descartar» y no tiene «Aprobar» ni edición (la ruta existe en la API). Para publicar un problema de la IA hay que aprobarlo por la API o crear el problema a mano.

## 3. Recorrido de la web del estudiante (user-web, puerto 3001)

1. Iniciar sesión con un estudiante de la demo → **Inscribirse** con el código de invitación de un curso, o usar un curso ya inscrito.
2. El **listado de problemas** del curso con su estado (resuelto, intentado, pendiente), filtrable.
3. Abrir un problema: enunciado a la izquierda y editor a la derecha, con el código base si lo hay y el **borrador** que se guarda solo.
4. **Ejecutar** con los casos públicos y **Enviar** (envío oficial, calificado con todos los casos; los ocultos nunca se muestran).
5. El **historial de envíos** del problema y el detalle de uno, con el mensaje y la línea del error cuando falló.
6. El **avance** del curso y una **evaluación** dentro de su ventana de tiempo.

## 4. Recorrido de la app móvil (mobile)

Se abre `mobile/` en Android Studio (ver `mobile/README.md`) y se corre en el emulador. En el emulador el servidor de tu PC es `10.0.2.2`.

1. **Iniciar sesión** con una cuenta de estudiante → pantalla de inicio → **Mis cursos**.
2. Las pantallas de **predecir la salida** y de **completar el espacio** (el campo de texto dentro del fragmento de código): cargan el problema desde `user-api`.

**No enseñar:** esas pantallas todavía no envían la respuesta (HU45-T3 y HU48-T2), no hay pantalla de opción múltiple, del Dojo, de cinturones, de racha ni de duelos, y no llegan avisos push. Lo que sí funciona se muestra mejor por la API (sección 5).

## 5. Lo que ya funciona en el servidor y todavía no tiene pantalla

Con un token de estudiante (el que devuelve el inicio de sesión de `admin-api`; en los ejemplos, `$TOKEN`) y `user-api` en el puerto 8081:

| Qué mostrar | Petición | Qué responde |
| :--- | :--- | :--- |
| Responder un ejercicio del celular | `POST /api/problemas/{id}/respuesta` con `{"respuesta": "5"}` o `{"opcion_id": 12}` | `{"resultado": "correcto"}` o `{"resultado": "incorrecto", "respuesta_correcta": "…"}`. Se ignoran mayúsculas y espacios de más (ADR-9 D15). |
| Empezar una sesión del Dojo | `POST /api/dojo/sesiones` con `{"lenguaje": "python"}` | 201 con el `id` de la sesión. |
| Siguiente ejercicio del Dojo | `POST /api/dojo/sesiones/{id}/siguiente` | El ejercicio (sin la respuesta ni la marca de la alternativa correcta); 204 cuando ya no quedan ejercicios sin repetir. |
| Puntos y cinturón | Resolver un ejercicio correctamente | Suma los puntos del tipo de ejercicio al cinturón del lenguaje, solo la primera vez por ejercicio (ADR-9 D18). Se ve en la tabla `app_schema.app_cinturon`. |

Los siete cinturones (blanco, amarillo, naranja, verde, azul, marrón y negro) están como imágenes vectoriales en `mobile/app/src/main/res/drawable/cinturon_*.xml`, y su umbral de puntos en `app_schema.app_nivel_cinturon` (0, 100, 250, 500, 900, 1 400 y 2 000).

## 6. Si algo falla

- **`./mvnw test` falla con «password authentication failed for user "${POSTGRES_USER}"»:** las pruebas leen las variables del entorno y no el `.env`. Desde `user-api`, con la base arriba (`docker compose up -d db`), en Git Bash: `set -a; source ../.env; set +a` y luego `./mvnw test`. Con las variables cargadas, `SandboxSeguridadTest` se salta si `SANDBOX_PROVIDER=piston`.
- **El emulador dice «No hay conexión con el servidor»:** revisa que `docker compose ps` muestre `admin-api` arriba, que `DJANGO_ALLOWED_HOSTS` incluya `10.0.2.2` (ya lo trae `docker-compose.yml`) y, en Android reciente, que la app de depuración tenga concedido el permiso de red local (después de reinstalarla, concédelo en los permisos de la app en Ajustes del emulador).
- **Un envío no se califica:** el motor de ejecución es el simulado. Activa Piston (paso 3 de la sección 1) y repite.
- **La IA responde 503 al generar:** es el proveedor (cuota o saturación). Espera un minuto y vuelve a intentarlo.
- **Piston:** Java no tiene etapa de compilación, así que un error de compilación llega como fallo de ejecución; `ai-service` lo reconoce y corrige el nombre del archivo.

## 7. Comprobaciones que se pueden mostrar

| Dónde | Comando | Resultado esperado |
| :--- | :--- | :--- |
| `user-api` | `./mvnw test` (con las variables cargadas) | Sin fallos; unas pocas pruebas «omitidas» son las del sandbox real. |
| `admin-api` | `python manage.py test` | Sin fallos. |
| `ai-service` | `pytest` | Sin fallos. |
| `admin-web` | `npm run lint` y `npm run build`; `node --test` para la lógica del formulario | Sin errores. |
| `user-web` | `npm run lint` y `npm run build` | Sin errores. |
| `mobile` | `./gradlew testDebugUnitTest` | Sin fallos (en Windows, `.\gradlew.bat`). |
