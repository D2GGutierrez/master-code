# Roadmap — Sprint 2 · MASTER CODE

**Sprint:** 19 – 30 de octubre de 2026 · **Hito:** editor + sandbox, generación con IA e inscripción a cursos
**Estado de partida:** Sprint 1 cerrado (confirmado por Juan Diego). No hay tareas arrastradas.
**Plan:** 16 historias, 67 tareas, 91 h, tomadas de `docs/historias-tareas.md` (IDs, responsables y dependencias son los del documento). Se agregan 3 decisiones sin horas (D1, D3, D4, sección 6) que conviene cerrar primero.

---

## 1. Qué ya está hecho

Todo el Sprint 1 (HU1 a HU4, HU9 a HU16, HU21 a HU24 y los habilitadores H1 a H9), verificado en `main`. Sobre eso se apoya este sprint:

| Ya existe | Dónde | Lo usa el Sprint 2 en |
|---|---|---|
| Registro, login y renovación con JWT RS256, JWKS y CORS para `admin-web` | admin-api | todas las pantallas autenticadas |
| `user-api` valida el JWT, crea el perfil (`app_estudiante`), `GET /api/yo`, `GET /api/avance` y tabla `app_envio` mínima | user-api | HU25 a HU32 |
| Cursos, problemas (plataforma, tipo, combinaciones válidas) y casos de prueba públicos y ocultos | admin-api | HU17 a HU20, HU25 a HU32 |
| `docker-compose.yml` con `db` y los cinco servicios; `ai-service` es solo el esqueleto (`/health`) | infra, ai-service | HU7, HU8 y HU17 |
| Esqueletos y login de `admin-web`, `user-web` y `mobile` | web, móvil | HU5, HU6, HU25 a HU32 |

## 2. La idea del roadmap en cuatro líneas

1. **Dos cadenas largas definen el sprint.** El sandbox (HU7 → HU6) y la IA (HU17 → HU18 → HU19). Las dos son de Juan Diego, así que empiezan el primer día.
2. **La cadena que más espera es la de los estudiantes.** «Curso publicado» exige un problema aprobado (HU25-T1 depende de HU19-T2) y el listado de problemas exige HU31-T1, así que HU29 a HU32 quedan al final (ola 13). Para no dejar parados a Carlos y a Gael, conviene **sembrar a mano un curso con problemas aprobados** y trabajar contra el contrato JSON mientras llega la IA.
3. **Las pruebas de Gael llegan al final de cada historia.** Mientras esperan, Gael y Carlos adelantan con datos simulados.
4. **Las decisiones D1, D3 y D4 van antes de todo:** sin ellas, tres cadenas empiezan con una suposición.

Reglas que no cambian: se trabaja en la rama personal, solo se lleva a `main` lo que ya funciona allí, por Pull Request con otro revisor; commit `carpeta: qué hace (HU# T#)`; control de entendimiento antes del commit.

---

## 3. Las olas: el orden global, mezclando a los tres

Una **ola** agrupa las tareas que pueden empezar cuando terminaron las de las que dependen. Se lee de arriba hacia abajo; dentro de una celda van de la que más desbloquea a la que menos. Una tarea solo espera a sus dependencias, no a que termine toda su ola.

| Ola | Juan Diego | Carlos | Gael |
|---|---|---|---|
| **Ola 0<br>Decisiones** | **D1** quién llama al sandbox<br>**D3** autenticación de la API interna<br>**D4** CORS de `user-api` | — | — |
| **Ola 1** | **HU08-T1** Definir en ai-service la interfaz común que debe cumplir cualquier proveedor de sandbox · 2 h<br>**HU17-T1** Obtener la clave gratuita de uno de los proveedores de LLM soportados y configurarla por… · 1 h<br>**HU19-T3** Registrar quién aprobó cada problema y en qué fecha · 1 h<br>**HU26-T2** Crear la migración Flyway del modelo Inscripcion (APP_INSCRIPCION) · 1 h<br>**HU07-T1** Levantar Judge0 autohospedado con docker-compose, agregándolo al docker-compose.yml único… · 3 h<br>**HU26-T1** Generar un código de invitación único para cada curso, que el docente comparte con sus… · 1 h<br>**HU20-T2** Archivar el problema descartado en lugar de borrarlo de la base de datos · 1 h<br>*Total: 10 h* | **HU05-T1** Integrar el editor Monaco en la web del estudiante como componente reutilizable · 2 h<br>*Total: 2 h* | — |
| **Ola 2** | **HU08-T4** Dejar operativo el proveedor mock, que devuelve resultados simulados, para poder trabajar y… · 1 h<br>**HU17-T2** Escribir y afinar el prompt de generación · 3 h<br>**HU07-T3** Implementar en Spring Boot el cliente HTTP que envía código, lenguaje y entrada a Judge0 y… · 2 h<br>**HU28-T1** Crear la consulta de estudiantes inscritos en un curso · 1 h<br>**HU07-T2** Configurar en Judge0 los límites de tiempo de CPU y de memoria por ejecución y bloquear el… · 2 h<br>**HU27-T1** Crear la consulta de los cursos en los que está inscrito el estudiante · 1 h<br>**HU08-T2** Implementar el segundo proveedor, Piston, cumpliendo esa interfaz, junto al de Judge0 · 2 h<br>**HU26-T3** Crear el endpoint de inscripción · 2 h<br>*Total: 14 h* | **HU05-T2** Configurar el resaltado de sintaxis para los lenguajes del proyecto · 1 h<br>**HU05-T3** Ajustar el editor para que se vea y funcione bien en pantallas pequeñas, sin desbordar la… · 1 h<br>*Total: 2 h* | **HU05-T4** Probar el editor con código muy largo y con caracteres especiales (con Carlos) · 1 h<br>**HU20-T4** Prueba (con Juan Diego) · 1 h<br>*Total: 2 h* |
| **Ola 3** | **HU17-T3** Completar el endpoint POST /generate de ai-service, apoyado en la capa de abstracción de… · 2 h<br>**HU06-T1** Crear el endpoint de «ejecución de prueba» · 2 h<br>**HU07-T4** Documentar en docs/ cómo se levanta y configura Judge0, para que cualquier compañero pueda… · 1 h<br>**HU08-T3** Permitir elegir el proveedor activo con una sola variable de entorno, sin tocar los routers · 1 h<br>**HU28-T5** Completar el borrado protegido de HU12 (CA-12.3) · 1 h<br>*Total: 7 h* | **HU28-T2** Construir la tabla de estudiantes inscritos dentro del panel del docente · 2 h<br>**HU27-T2** Construir la pantalla principal de la web con una tarjeta por curso inscrito · 2 h<br>*Total: 4 h* | **HU07-T5** Pruebas de seguridad (con Juan Diego) · 1 h<br>**HU26-T4** Pruebas (con Juan Diego) · 1 h<br>**HU27-T3** Construir la misma pantalla principal en la app Android con Jetpack Compose · 2 h<br>**HU28-T4** Prueba (con Juan Diego) · 1 h<br>*Total: 5 h* |
| **Ola 4** | **HU17-T5** Validar el esquema de la respuesta del LLM antes de guardarla · 1 h<br>**HU06-T4** Limitar cuántas ejecuciones seguidas puede hacer un estudiante por minuto, para no saturar… · 1 h<br>*Total: 2 h* | **HU06-T2** Agregar al editor el botón «Ejecutar» y un panel donde se muestra la salida o el error de… · 2 h<br>**HU28-T3** Agregar un botón que descargue la lista de inscritos como archivo CSV · 1 h<br>*Total: 3 h* | — |
| **Ola 5** | **HU18-T1** Crear la consulta de problemas pendientes de revisión del docente, para mostrarlos con la… · 1 h<br>**HU17-T6** Manejar el caso de servicio de IA caído o respuesta inválida, mostrando al docente un error… (con Carlos) · 1 h<br>*Total: 2 h* | **HU06-T3** Mostrar el estado «ejecutando…» y deshabilitar el botón mientras tanto, para evitar el… · 1 h<br>**HU17-T4** Construir el formulario de generación con un indicador de progreso mientras la IA responde · 2 h<br>*Total: 3 h* | — |
| **Ola 6** | — | **HU18-T2** Construir la pantalla «Bandeja de revisión» con la lista de problemas pendientes · 2 h<br>*Total: 2 h* | — |
| **Ola 7** | **HU20-T1** Implementar la acción «Descartar» con un cuadro de confirmación previo (con Carlos) · 1 h<br>*Total: 1 h* | **HU19-T1** Reutilizar el formulario de problema dentro de la pantalla de revisión, para que el docente… · 1 h<br>**HU18-T3** Agregar filtros por curso y por estado de revisión · 1 h<br>**HU18-T4** Mostrar el contador de pendientes y probar que ningún problema generado por IA se publica… (con Juan Diego) · 1 h<br>*Total: 3 h* | — |
| **Ola 8** | **HU19-T2** Implementar la acción «Aprobar» (con Carlos) · 2 h<br>*Total: 2 h* | **HU20-T3** Agregar el botón «Descartar» a la bandeja de revisión · 1 h<br>*Total: 1 h* | — |
| **Ola 9** | **HU31-T1** Filtrar por aprobado_por_docente = true en la consulta que alimenta el listado de problemas… · 1 h<br>**HU25-T1** Crear en admin-api el endpoint de cursos publicados · 1 h<br>*Total: 2 h* | — | **HU19-T4** Prueba (con Juan Diego) · 1 h<br>*Total: 1 h* |
| **Ola 10** | **HU29-T1** Crear la consulta de los problemas de un curso en el que el estudiante está inscrito · 1 h<br>**HU31-T2** Aplicar el mismo filtro al abrir un problema individual, para que no se pueda acceder a uno… · 1 h<br>**HU25-T2** Consumir ese endpoint desde user-api para ofrecer al estudiante la lista de cursos disponibles · 2 h<br>*Total: 4 h* | — | — |
| **Ola 11** | **HU32-T1** Crear el endpoint de detalle de un problema · 1 h<br>**HU30-T1** Agregar a la consulta de problemas los filtros de dificultad y de estado · 1 h<br>**HU25-T4** Filtrar por la institución del estudiante y mostrar «sin resultados» cuando no hay… (con Carlos) · 1 h<br>*Total: 3 h* | **HU29-T2** Construir el listado de problemas mostrando la dificultad y el tipo de ejercicio de cada uno · 2 h<br>**HU25-T3** Construir la pantalla de búsqueda de cursos en la web del estudiante · 2 h<br>*Total: 4 h* | **HU29-T4** Prueba (con Juan Diego) · 1 h<br>**HU31-T4** Pruebas con un problema aprobado (visible) y con uno sin aprobar (no visible) (con Juan Diego) · 1 h<br>*Total: 2 h* |
| **Ola 12** | **HU30-T2** Calcular el estado de cada problema para el estudiante a partir de sus envíos · 2 h<br>*Total: 2 h* | **HU32-T2** Construir la pantalla de detalle con el enunciado y los casos de ejemplo públicos · 2 h<br>**HU29-T3** Ordenar el listado por dificultad y fecha de publicación, y paginarlo · 1 h<br>**HU30-T3** Agregar los botones de filtro al listado y recordar el último filtro usado · 1 h<br>*Total: 4 h* | **HU31-T3** Auditar todas las rutas para comprobar que ninguna se salta el filtro de aprobados (con Juan Diego) · 1 h<br>**HU32-T4** Prueba (con Juan Diego) · 1 h<br>*Total: 2 h* |
| **Ola 13** | — | **HU32-T3** Agregar el botón que lleva al editor de código del problema · 1 h<br>*Total: 1 h* | **HU30-T4** Pruebas de combinaciones de filtros, incluida una combinación que no devuelve resultados (con Juan Diego) · 1 h<br>*Total: 1 h* |

---

## 4. Orden personal de cada integrante

«Necesita de otros» muestra solo lo que depende de otra persona (entre paréntesis, de quién); «—» significa que puedes seguir cuando termines la fila anterior. «Libera» es cuántas tareas del sprint se desbloquean al terminarla. Las tareas de Sprint 1 de las que dependen ya están hechas y no se listan.

### Juan Diego · 49 h · 35 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera |
|---|---|---|---|---|---|
| 1 | **HU08-T1** | Definir en ai-service la interfaz común que debe cumplir cualquier proveedor de sandbox | 2 h | — | 36 |
| 2 | **HU17-T1** | Obtener la clave gratuita de uno de los proveedores de LLM soportados y configurarla por… | 1 h | — | 34 |
| 3 | **HU19-T3** | Registrar quién aprobó cada problema y en qué fecha | 1 h | — | 22 |
| 4 | **HU26-T2** | Crear la migración Flyway del modelo Inscripcion (APP_INSCRIPCION) | 1 h | — | 18 |
| 5 | **HU07-T1** | Levantar Judge0 autohospedado con docker-compose, agregándolo al docker-compose.yml único… | 3 h | — | 8 |
| 6 | **HU26-T1** | Generar un código de invitación único para cada curso, que el docente comparte con sus… | 1 h | — | 2 |
| 7 | **HU20-T2** | Archivar el problema descartado en lugar de borrarlo de la base de datos | 1 h | — | 1 |
| 8 | **HU08-T4** | Dejar operativo el proveedor mock, que devuelve resultados simulados, para poder trabajar y… | 1 h | — | 33 |
| 9 | **HU17-T2** | Escribir y afinar el prompt de generación | 3 h | — | 33 |
| 10 | **HU07-T3** | Implementar en Spring Boot el cliente HTTP que envía código, lenguaje y entrada a Judge0 y… | 2 h | — | 5 |
| 11 | **HU28-T1** | Crear la consulta de estudiantes inscritos en un curso | 1 h | — | 4 |
| 12 | **HU07-T2** | Configurar en Judge0 los límites de tiempo de CPU y de memoria por ejecución y bloquear el… | 2 h | — | 2 |
| 13 | **HU27-T1** | Crear la consulta de los cursos en los que está inscrito el estudiante | 1 h | — | 2 |
| 14 | **HU08-T2** | Implementar el segundo proveedor, Piston, cumpliendo esa interfaz, junto al de Judge0 | 2 h | — | 1 |
| 15 | **HU26-T3** | Crear el endpoint de inscripción | 2 h | — | 1 |
| 16 | **HU17-T3** | Completar el endpoint POST /generate de ai-service, apoyado en la capa de abstracción de… | 2 h | — | 32 |
| 17 | **HU06-T1** | Crear el endpoint de «ejecución de prueba» | 2 h | — | 3 |
| 18 | **HU07-T4** | Documentar en docs/ cómo se levanta y configura Judge0, para que cualquier compañero pueda… | 1 h | — | 0 |
| 19 | **HU08-T3** | Permitir elegir el proveedor activo con una sola variable de entorno, sin tocar los routers | 1 h | — | 0 |
| 20 | **HU28-T5** | Completar el borrado protegido de HU12 (CA-12.3) | 1 h | — | 0 |
| 21 | **HU17-T5** | Validar el esquema de la respuesta del LLM antes de guardarla | 1 h | — | 31 |
| 22 | **HU06-T4** | Limitar cuántas ejecuciones seguidas puede hacer un estudiante por minuto, para no saturar… | 1 h | — | 0 |
| 23 | **HU18-T1** | Crear la consulta de problemas pendientes de revisión del docente, para mostrarlos con la… | 1 h | — | 28 |
| 24 | **HU17-T6** | Manejar el caso de servicio de IA caído o respuesta inválida, mostrando al docente un error… (con Carlos) | 1 h | — | 0 |
| 25 | **HU20-T1** | Implementar la acción «Descartar» con un cuadro de confirmación previo (con Carlos) | 1 h | HU18-T2 (CA) | 1 |
| 26 | **HU19-T2** | Implementar la acción «Aprobar» (con Carlos) | 2 h | HU19-T1 (CA) | 21 |
| 27 | **HU31-T1** | Filtrar por aprobado_por_docente = true en la consulta que alimenta el listado de problemas… | 1 h | — | 15 |
| 28 | **HU25-T1** | Crear en admin-api el endpoint de cursos publicados | 1 h | — | 3 |
| 29 | **HU29-T1** | Crear la consulta de los problemas de un curso en el que el estudiante está inscrito | 1 h | — | 7 |
| 30 | **HU31-T2** | Aplicar el mismo filtro al abrir un problema individual, para que no se pueda acceder a uno… | 1 h | — | 6 |
| 31 | **HU25-T2** | Consumir ese endpoint desde user-api para ofrecer al estudiante la lista de cursos disponibles | 2 h | — | 2 |
| 32 | **HU32-T1** | Crear el endpoint de detalle de un problema | 1 h | — | 4 |
| 33 | **HU30-T1** | Agregar a la consulta de problemas los filtros de dificultad y de estado | 1 h | — | 3 |
| 34 | **HU25-T4** | Filtrar por la institución del estudiante y mostrar «sin resultados» cuando no hay… (con Carlos) | 1 h | — | 0 |
| 35 | **HU30-T2** | Calcular el estado de cada problema para el estudiante a partir de sus envíos | 2 h | — | 1 |

### Carlos · 29 h · 20 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera |
|---|---|---|---|---|---|
| 1 | **HU05-T1** | Integrar el editor Monaco en la web del estudiante como componente reutilizable | 2 h | — | 6 |
| 2 | **HU05-T2** | Configurar el resaltado de sintaxis para los lenguajes del proyecto | 1 h | — | 0 |
| 3 | **HU05-T3** | Ajustar el editor para que se vea y funcione bien en pantallas pequeñas, sin desbordar la… | 1 h | — | 0 |
| 4 | **HU28-T2** | Construir la tabla de estudiantes inscritos dentro del panel del docente | 2 h | HU28-T1 (JD) | 1 |
| 5 | **HU27-T2** | Construir la pantalla principal de la web con una tarjeta por curso inscrito | 2 h | HU27-T1 (JD) | 0 |
| 6 | **HU06-T2** | Agregar al editor el botón «Ejecutar» y un panel donde se muestra la salida o el error de… | 2 h | HU06-T1 (JD) | 1 |
| 7 | **HU28-T3** | Agregar un botón que descargue la lista de inscritos como archivo CSV | 1 h | — | 0 |
| 8 | **HU06-T3** | Mostrar el estado «ejecutando…» y deshabilitar el botón mientras tanto, para evitar el… | 1 h | — | 0 |
| 9 | **HU17-T4** | Construir el formulario de generación con un indicador de progreso mientras la IA responde | 2 h | HU17-T3 (JD), HU17-T5 (JD) | 0 |
| 10 | **HU18-T2** | Construir la pantalla «Bandeja de revisión» con la lista de problemas pendientes | 2 h | HU18-T1 (JD) | 27 |
| 11 | **HU19-T1** | Reutilizar el formulario de problema dentro de la pantalla de revisión, para que el docente… | 1 h | — | 22 |
| 12 | **HU18-T3** | Agregar filtros por curso y por estado de revisión | 1 h | — | 0 |
| 13 | **HU18-T4** | Mostrar el contador de pendientes y probar que ningún problema generado por IA se publica… (con Juan Diego) | 1 h | — | 0 |
| 14 | **HU20-T3** | Agregar el botón «Descartar» a la bandeja de revisión | 1 h | HU20-T1 (JD) | 0 |
| 15 | **HU29-T2** | Construir el listado de problemas mostrando la dificultad y el tipo de ejercicio de cada uno | 2 h | HU29-T1 (JD) | 2 |
| 16 | **HU25-T3** | Construir la pantalla de búsqueda de cursos en la web del estudiante | 2 h | HU25-T2 (JD) | 0 |
| 17 | **HU32-T2** | Construir la pantalla de detalle con el enunciado y los casos de ejemplo públicos | 2 h | HU32-T1 (JD) | 1 |
| 18 | **HU29-T3** | Ordenar el listado por dificultad y fecha de publicación, y paginarlo | 1 h | — | 0 |
| 19 | **HU30-T3** | Agregar los botones de filtro al listado y recordar el último filtro usado | 1 h | HU30-T1 (JD) | 0 |
| 20 | **HU32-T3** | Agregar el botón que lleva al editor de código del problema | 1 h | — | 0 |

### Gael · 13 h · 12 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera |
|---|---|---|---|---|---|
| 1 | **HU05-T4** | Probar el editor con código muy largo y con caracteres especiales (con Carlos) | 1 h | HU05-T1 (CA) | 0 |
| 2 | **HU20-T4** | Prueba (con Juan Diego) | 1 h | HU20-T2 (JD) | 0 |
| 3 | **HU07-T5** | Pruebas de seguridad (con Juan Diego) | 1 h | HU07-T2 (JD), HU07-T3 (JD) | 0 |
| 4 | **HU26-T4** | Pruebas (con Juan Diego) | 1 h | HU26-T3 (JD) | 0 |
| 5 | **HU27-T3** | Construir la misma pantalla principal en la app Android con Jetpack Compose | 2 h | HU27-T1 (JD) | 0 |
| 6 | **HU28-T4** | Prueba (con Juan Diego) | 1 h | HU28-T1 (JD) | 0 |
| 7 | **HU19-T4** | Prueba (con Juan Diego) | 1 h | HU19-T2 (JD) | 0 |
| 8 | **HU29-T4** | Prueba (con Juan Diego) | 1 h | HU29-T1 (JD) | 0 |
| 9 | **HU31-T4** | Pruebas con un problema aprobado (visible) y con uno sin aprobar (no visible) (con Juan Diego) | 1 h | HU31-T2 (JD) | 0 |
| 10 | **HU31-T3** | Auditar todas las rutas para comprobar que ninguna se salta el filtro de aprobados (con Juan Diego) | 1 h | HU31-T2 (JD), HU32-T1 (JD) | 0 |
| 11 | **HU32-T4** | Prueba (con Juan Diego) | 1 h | HU32-T1 (JD) | 0 |
| 12 | **HU30-T4** | Pruebas de combinaciones de filtros, incluida una combinación que no devuelve resultados (con Juan Diego) | 1 h | HU30-T1 (JD), HU30-T2 (JD) | 0 |

---

## 5. Puntos de encuentro

| Cuando esté en `main`… | …queda libre |
|---|---|
| D4 (CORS de `user-api`) | Carlos puede probar cualquier pantalla de `user-web` contra `user-api` real. |
| HU07-T3 y HU06-T1 (sandbox y endpoint de ejecución) | Carlos: HU06-T2 y T3. Gael: HU07-T5. |
| HU17-T5 (guardar problemas generados) | Carlos: HU17-T4. Juan Diego: HU18-T1. |
| HU18-T2 (bandeja) | Carlos: HU18-T3, T4, HU19-T1; Juan Diego: HU20-T1. |
| HU19-T2 (aprobar) | Juan Diego: HU25-T1 y HU31-T1. Gael: HU19-T4. Es el nudo de todo el bloque de estudiantes. |
| HU26-T2 (tabla de inscripciones) | HU26-T3, HU27-T1, HU28-T1 y HU29-T1. |
| HU27-T1 (cursos inscritos) | Carlos: HU27-T2. Gael: HU27-T3 (Android). |
| HU31-T1 y HU29-T1 (aprobados y problemas del curso) | Carlos: HU29-T2 y T3. Gael: HU29-T4. Juan Diego: HU30-T1 y T2. |
| HU32-T1 (detalle del problema) | Carlos: HU32-T2 y T3. Gael: HU32-T4 y HU31-T3. |

**Mientras esperas lo de otro**:

- Carlos: empieza por HU05 (Monaco, sin dependencias) y deja las pantallas de inscripción y de problemas listas con respuestas simuladas del contrato JSON.
- Gael: escribe los casos de las pruebas desde los criterios de aceptación; HU27-T3 (Android) puede avanzar con datos de ejemplo.
- Juan Diego: si espera una revisión, sigue con la siguiente fila de su lista que no dependa de ella.

---

## 6. Decisiones abiertas, huecos y riesgos

### Decisiones que conviene cerrar el primer día

| # | Decisión | Mi recomendación |
|---|---|---|
| **D1** | ¿Quién llama al sandbox? HU07-T3 pone el cliente en `user-api`; HU08 y el ADR-2 ponen los proveedores en `ai-service`. Es la decisión abierta nº 1 de `registro-cambios.md`, y HU36-T2 (Sprint 3) depende de ella. | `user-api` llama a `ai-service`, que usa el proveedor activo. HU07-T3 pasa a ser el cliente hacia `ai-service` y el proveedor Judge0 en Python entra en HU08-T1. |
| **D3** | ¿Cómo se autentican `user-api` y `admin-api` en la API interna? Varias tareas la usan (HU25, HU26-T3, HU27, HU28, HU29, HU31, HU32) y ninguna la define. | Rutas `/internal/` con la cabecera `X-Internal-Key`, clave en una variable de entorno que solo reciben esos dos servicios. |
| **D4** | ¿Cómo llama `user-web` (3001) a `user-api` (8081)? En el repo `user-api` no tiene CORS; HU22-T7 solo lo hizo en `admin-api`. | CORS por variable de entorno, mismo patrón que HU22-T7. Va antes de HU06-T2. |

Ya resuelto en `docs/registro-cambios.md`: curso «publicado» = tiene al menos un problema aprobado; el código de invitación vence a los 30 días; el límite de ejecuciones es de 10 por minuto.

### Huecos en las tareas

1. **Endpoints internos de `admin-api` sin tarea propia.** HU26-T3, HU27-T1, HU29-T1 y HU32-T1 necesitan datos de `admin-api` (validar un código, datos del curso, problemas aprobados, detalle con solo casos públicos). Se hacen dentro de la tarea de `user-api` que los usa y cada una debe contarlos al estimarse.
2. **Campos nuevos en `Problema` sin migración propia.** `generado_por_ia`, `aprobado_por_docente`, quién y cuándo aprobó, y `archivado` no existen todavía en `admin-api`. HU17-T5 debería abrir con esa migración, porque HU18, HU19, HU20, HU25 y HU31 dependen de ella.
3. **HU17-T3 supone una capa de proveedores de LLM que no existe.** H7 solo dejó `/health`; las 2 h de la tarea deben incluir esa capa y un proveedor mock.
4. **HU17-T5 hace más de lo que dice.** Admin-api llama a `ai-service`, valida el JSON y guarda cada problema con sus casos reutilizando las reglas de HU15 y HU16. Es 1 h en el plan: probablemente 2 a 3 h.
5. **HU30-T2 no puede calcular «resuelto» todavía.** `app_envio` no tiene `resultado` hasta HU36-T1; se prueba con envíos sembrados (decisión abierta nº 5 del registro). *Resuelto el 28 de octubre:* la migración V4 de user-api agrega `resultado` antes de HU36-T1 (ADR-7, D6).
6. **HU26-T1 debe generar el código también para los cursos que ya existen** (30 días de vigencia), o los cursos creados en el Sprint 1 no se podrán usar.
7. **HU28-T1 solo puede listar el correo**: `app_estudiante` no guarda el nombre.

### Riesgos

- **Cadena crítica larga.** HU17 → HU18 → HU19 → HU25/HU31 → HU29 → HU30 pasa por casi todas las olas. Un retraso en la IA corre todo el bloque de estudiantes; por eso la siembra manual de problemas aprobados.
- **Judge0 en Docker Desktop (Windows).** Usa mecanismos del kernel que suelen fallar con WSL2. Prueba HU07-T1 al empezar; si en 1 h no arranca, usa Piston (HU08-T2) o el mock y no frenes HU06.
- **Clave gratuita del LLM (HU17-T1).** Tiene límite de peticiones por minuto. Desarrollo y CI con el proveedor mock; la clave real solo para afinar el prompt.
- **Reparto desigual.** Juan Diego 49 h, Carlos 29 h y Gael 13 h (decisión abierta nº 4 del registro). Si hay que rebalancear, Gael puede tomar HU07-T4 (documentar Judge0) y HU08-T4 (proveedor mock).

---

## 7. Gestión

- Crear en Trello las tarjetas del Sprint 2 con los responsables de la sección 4 y pasar a «Hecho» las del Sprint 1.
- Confirmar D1, D3 y D4 en el Planning y pasarlas de «propuesta» a «decidido» en el ADR-7 (ya decididas con las recomendaciones).
- Corregir el flujo de Git en los documentos (hecho en esta revisión) y avisar a Carlos y a Gael.

## 8. Cómo mantener esto al día

Al fusionar cada tarea a `main`, táchala aquí y anótala en `historial.md`. Si me pasas el historial actualizado, regenero las olas con lo que falte.
