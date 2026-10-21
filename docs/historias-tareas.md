# Historias de Usuario — Tareas

> Versión 2 · 7 de octubre de 2026. Corrige la numeración, las dependencias entre tareas y los criterios sin cobertura. Los cambios y las decisiones pendientes están en «Registro de cambios y decisiones».

### Resumen

| Sprint | Fechas | Hito | HU | Tareas | Horas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Sprint 1 | 5 – 16 oct 2026 | Cursos y banco de problemas manual; cuenta única del estudiante (E1 + base ADR-4) | 16 | 68 | 94 h |
| Sprint 2 | 19 – 30 oct 2026 | Editor + sandbox, generación con IA, inscripción a cursos (E2 parcial, E3, base E4) | 16 | 67 | 91 h |
| Sprint 3 | 2 – 13 nov 2026 | Calificación de punta a punta y evaluaciones. Cierra el MVP web | 16 | 69 | 90 h |
| Sprint 4 | 16 – 27 nov 2026 | App móvil completa estilo Consoly. Cierra el MVP | 16 | 66 | 90 h |
| **Total** | | | **64** | **270** | **365 h** |

La capacidad del equipo es de 90 h por sprint (3 h por persona al día). Los sprints 1 y 2 la superan en 4 h y 1 h; ver la sección 7.

### 2. Mapa del repositorio
Todo el código vive en un único repositorio (monorepo) llamado master-code (ADR-5). Cada carpeta es un servicio o una aplicación independiente, y la columna «Servicio» de las tablas de tareas usa estos mismos nombres.

| Carpeta        | Tecnología                                 | Qué hace                                                                                                                                                                      |
| :------------- | :----------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **admin-api**  | Python · Django + Django REST Framework    | Backend del panel docente: cursos, problemas, evaluaciones, y registro e inicio de sesión de todos los usuarios. Es quien emite los tokens (JWT). Usa el schema admin_schema. |
| **admin-web**  | React                                      | Panel web del docente.                                                                                                                                                        |
| **user-api**   | Java 21 · Spring Boot 3.3.4 · JPA · Flyway | Backend del estudiante: inscripciones, envíos, resultados, progreso y mecánicas de juego. Usa el schema app_schema.                                                           |
| **user-web**   | React + Vite + TypeScript                  | Web del estudiante, con el editor de código.                                                                                                                                  |
| **mobile**     | Kotlin + Jetpack Compose                   | App Android del estudiante.                                                                                                                                                   |
| **ai-service** | Python · FastAPI                           | Servicio de IA: /generate crea problemas con un modelo de lenguaje (LLM) y /evaluate coordina el sandbox (ADR-2).                                                             |
| **docs**       | Markdown                                   | Documentación técnica: decisiones, fórmulas, guías de despliegue.                                                                                                             |
| **infra**      | Docker                                     | En las tablas, «infra» se refiere al docker-compose.yml único del monorepo, que levanta los servicios de apoyo (por ejemplo el sandbox) con un solo docker compose up.        |

### 3. Reglas de arquitectura que debes conocer
* **Una base de datos, dos schemas (ADR-1).** Hay un solo PostgreSQL con dos schemas: admin_schema (lo maneja Django con sus migraciones) y app_schema (lo maneja Spring con migraciones Flyway). Ningún servicio lee las tablas del otro y no hay claves foráneas entre schemas: user-api guarda ids como curso_id (referencias lógicas) y pide los datos del curso a admin-api por una API interna.
* **Una sola cuenta para todo (ADR-4).** El registro y el inicio de sesión pasan siempre por admin-api, que firma un JWT con RS256. user-api no guarda contraseñas: verifica la firma con la clave pública publicada en /.well-known/jwks.json (variable de entorno JWKS_URI). Por eso la misma cuenta sirve en el panel docente, en la web del estudiante y en el móvil.
* **La IA nunca publica sola (ADR-2).** Los problemas generados quedan con generado_por_ia = true y aprobado_por_docente = false. Los estudiantes solo ven los problemas aprobados por un docente.
* **Web y móvil se evalúan distinto.** Cada problema tiene una plataforma (web, móvil o ambas) y un tipo_ejercicio. Solo escritura_codigo (web) usa el sandbox, que ejecuta el código del estudiante contra los casos de prueba. Los otros tres tipos (prediccion_salida, opcion_multiple y completar_espacio) son móviles y user-api los corrige comparando respuestas, sin sandbox ni IA.
* **Los casos de prueba ocultos no se filtran.** Del caso oculto el estudiante solo ve si pasó o falló; nunca su entrada ni su salida esperada.

### 4. Glosario

| Término | Significado |
| :--- | :--- |
| **HU (historia de usuario)** | Un requisito escrito desde el punto de vista de quien lo usa («Como docente, quiero…»). Cada HU se divide en tareas. |
| **Sprint** | Ciclo de trabajo de dos semanas. El MVP tiene cuatro sprints, de 90 h de equipo cada uno. |
| **ADR** | Registro de una decisión técnica, en docs/decisiones-tecnicas-adr.md. Los que se citan aquí: ADR-1 (base de datos), ADR-2 (IA y sandbox), ADR-4 (autenticación compartida) y ADR-5 (monorepo). |
| **Migración** | Archivo que crea o cambia tablas. En admin-api la genera Django (makemigrations); en user-api se escribe como archivo SQL versionado con Flyway. |
| **JWT, access token y refresh token** | El JWT es una credencial firmada que prueba quién es el usuario. El access token dura poco (15 min) y se envía en cada petición; el refresh token dura más y sirve para pedir un access nuevo. Con «rotación», cada renovación entrega un refresh nuevo e invalida el anterior. |
| **JWKS** | Dirección pública donde admin-api publica la clave con la que se verifica la firma de los tokens. |
| **Sandbox** | Entorno aislado que ejecuta el código escrito por un estudiante con límites de tiempo y de memoria, y sin acceso a la red. Se usa Judge0 o Piston, intercambiables. |
| **Caso de prueba (público u oculto)** | Par entrada y salida esperada con el que se califica un problema. Los públicos se muestran como ejemplo; los ocultos solo sirven para calificar. |
| **Envío y ejecución de prueba** | Un envío es un intento oficial: se califica y queda guardado en APP_ENVIO. Una ejecución de prueba solo muestra la salida del código y no se guarda. |
| **generado_por_ia y aprobado_por_docente** | Marcas que indican que un problema lo creó la IA y que un docente ya lo revisó y lo aprobó. |
| **Proveedor mock** | Proveedor simulado (de IA o de sandbox) que devuelve respuestas fijas, para trabajar y probar sin servicios externos. |
| **Racha** | Cantidad de días seguidos en que el estudiante practicó. |
| **Cinturón** | Nivel por lenguaje, de blanco a negro, que sube al acumular puntos. |
| **Dojo** | Modo de práctica libre de la app móvil: sesiones de ejercicios cortos con dificultad adaptativa. |
| **Partida o reto** | Duelo entre dos estudiantes del mismo curso con los mismos ejercicios y un marcador en vivo. |
| **Evaluación** | Examen de un curso con fecha de inicio y de fin, límite de tiempo y puntos por problema. |
| **Product Owner (PO)** | Quien prioriza el backlog. Además, activa a mano desde el admin de Django las cuentas de docentes que quedan pendientes. |
| **CA (criterio de aceptación)** | Condición concreta que se comprueba en el Sprint Review para dar una HU por terminada. Se escribe en `historias-criterios.md` con el formato CA-09.1 (HU 9, criterio 1). |
| **Hecho cuando** | Comprobación observable que dice si una tarea está terminada (un `curl`, una prueba, una pantalla). |
| **Diferido** | Criterio de una HU que no puede cumplirse todavía porque depende de algo de un sprint posterior; queda anotado con la tarea que lo cierra. |

### 5. Cómo leer las tablas y cómo reportar el avance

**Identificadores.** Cada historia se identifica con su número de HU, que no cambia nunca (HU 1 a HU 64). Cada tarea se identifica como `HU09-T1`, y cada criterio como `CA-09.1`. Ese mismo ID se usa en Trello, en el nombre de la rama (`feat/hu09-t1-modelo-curso`), en el commit (`admin-api: modelo Curso y migración (HU9 T1)`) y en el Pull Request. Las cuatro historias de evaluaciones, que antes no tenían número, son HU 61 a HU 64; el resto conserva el suyo.

**Columnas de las tablas.**

- **Servicio**: carpeta del monorepo donde se hace la tarea.
- **Resp.**: la persona que responde por la tarea y la cierra. Si dice «(+ otra persona)», la tarea toca también una carpeta de esa persona, que entrega su parte en un PR propio dentro de su carpeta.
- **Depende de**: tareas que deben estar integradas en `main` antes de empezar. Si una dependencia no está lista, la tarea no se empieza: se avisa al Scrum Master.
- **Cubre CA**: criterios de aceptación que esta tarea ayuda a cumplir.
- **Hecho cuando**: lo que se comprueba antes de mover la tarjeta a «Hecho».
- **Hrs.**: horas estimadas.

**Cuándo está terminada una tarea.** Cuando se cumplen las cinco cosas: el Pull Request está aprobado por otra persona y integrado en `main`; el CI está en verde (lint, pruebas y build); se comprobó el «hecho cuando» de la tarea; quien la hizo puede explicar cada archivo que cambió (control de entendimiento de `docs/flujo-git.md`); y no hay credenciales en el cambio ni queda configuración sin documentar en el README.

**Cuándo está terminada una historia.** Cuando todas sus tareas, pruebas incluidas, están terminadas y todos sus criterios de aceptación se verificaron en el Sprint Review con `docker compose up --build` y los otros servicios encendidos. Un criterio marcado como diferido deja la historia como «parcial» hasta que la tarea que lo cierra esté hecha.

**Dónde vive cada cosa.** Este documento y el de criterios son la especificación y solo cambian mediante un Pull Request a `docs/`, anotando el cambio en el registro. El estado de cada tarea (por hacer, en progreso, en revisión, hecho), las horas reales y el enlace al PR viven en Trello; no se copian aquí.

**Reparto.** Las tareas de pruebas, de auditoría y de verificación son de Gael en todas las historias. El Sprint 1 sigue la asignación acordada; los sprints 2 a 4 son una propuesta (la carpeta decide quién) y se confirman en el Planning de cada sprint.

**Horas.** Las horas con medio punto se escriben con punto decimal (0.5).

### 6. Equivalencias de numeración

El documento de criterios usaba otra numeración (HU-001 a HU-064, correlativa por sprint). Desde esta versión los dos documentos usan el número de HU de este documento. La tabla permite encontrar cualquier referencia antigua.

| HU | Antes en criterios | Backlog | Sprint | Historia |
| :--- | :--- | :--- | :--- | :--- |
| HU 1 | HU-001 |  | 1 | Registro de docente |
| HU 2 | HU-002 |  | 1 | Inicio de sesión del docente |
| HU 3 | HU-003 |  | 1 | Recuperación de contraseña |
| HU 4 | HU-004 |  | 1 | Protección de cuentas |
| HU 5 | HU-017 |  | 2 | Editor de programación |
| HU 6 | HU-018 |  | 2 | Ejecutar sin enviar |
| HU 7 | HU-019 |  | 2 | Sandbox |
| HU 8 | HU-020 |  | 2 | Motor configurable |
| HU 9 | HU-005 |  | 1 | Crear curso |
| HU 10 | HU-006 |  | 1 | Editar curso |
| HU 11 | HU-007 |  | 1 | Listado de cursos |
| HU 12 | HU-008 |  | 1 | Protección de eliminación de curso |
| HU 13 | HU-009 |  | 1 | Crear problema |
| HU 14 | HU-010 |  | 1 | Plataforma y tipo de ejercicio |
| HU 15 | HU-011 |  | 1 | Validación de combinaciones |
| HU 16 | HU-012 |  | 1 | Casos de prueba |
| HU 17 | HU-021 |  | 2 | Generar problemas |
| HU 18 | HU-022 |  | 2 | Bandeja de problemas generados |
| HU 19 | HU-023 |  | 2 | Revisar y aprobar problema |
| HU 20 | HU-024 |  | 2 | Descartar problema generado |
| HU 21 | HU-013 |  | 1 | Registro del estudiante |
| HU 22 | HU-014 |  | 1 | Inicio de sesión multiplataforma |
| HU 23 | HU-015 |  | 1 | Recuperación de contraseña del estudiante |
| HU 24 | HU-016 |  | 1 | Sincronización del progreso |
| HU 25 | HU-025 |  | 2 | Buscar cursos publicados |
| HU 26 | HU-026 |  | 2 | Inscribirse en un curso |
| HU 27 | HU-027 |  | 2 | Mis cursos |
| HU 28 | HU-028 |  | 2 | Lista de inscritos |
| HU 29 | HU-029 |  | 2 | Problemas disponibles |
| HU 30 | HU-030 |  | 2 | Filtrar problemas |
| HU 31 | HU-031 |  | 2 | Problemas aprobados |
| HU 32 | HU-032 |  | 2 | Detalle del problema |
| HU 33 | HU-033 | 29 | 3 | Enunciado junto al editor |
| HU 34 | HU-034 | 30 | 3 | Plantilla inicial de código |
| HU 35 | HU-035 | 31 | 3 | Borrador automático |
| HU 36 | HU-036 | 32 | 3 | Enviar solución |
| HU 37 | HU-037 | 33 | 3 | Detalle de casos evaluados |
| HU 38 | HU-038 | 34 | 3 | Calificación parcial |
| HU 39 | HU-039 | 35 | 3 | Ocultamiento de datos de prueba |
| HU 40 | HU-040 | 36 | 3 | Errores comprensibles |
| HU 41 | HU-041 | 37 | 3 | Consultar intentos |
| HU 42 | HU-042 | 38 | 3 | Recuperar una solución anterior |
| HU 43 | HU-043 | 39 | 3 | Resumen de progreso |
| HU 44 | HU-044 | 40 | 3 | Historial de envíos del grupo |
| HU 45 | HU-049 | 41 | 4 | Predecir salida en consola |
| HU 46 | HU-050 | 42 | 4 | Responder alternativa |
| HU 47 | HU-051 | 43 | 4 | Completar código |
| HU 48 | HU-052 | 44 | 4 | Feedback inmediato |
| HU 49 | HU-053 | 45 | 4 | Mantener racha |
| HU 50 | HU-054 | 46 | 4 | Subir de cinturón |
| HU 51 | HU-055 | 47 | 4 | Puntos restantes |
| HU 52 | HU-056 | 48 | 4 | Recordatorio de racha |
| HU 53 | HU-057 | 49 | 4 | Iniciar Dojo |
| HU 54 | HU-058 | 50 | 4 | Ajuste automático de dificultad |
| HU 55 | HU-059 | 51 | 4 | Salir y reanudar Dojo |
| HU 56 | HU-060 | 52 | 4 | Resultado de sesión |
| HU 57 | HU-061 | 53 | 4 | Retar a un compañero |
| HU 58 | HU-062 | 54 | 4 | Misma secuencia de ejercicios |
| HU 59 | HU-063 | 55 | 4 | Marcador de partida |
| HU 60 | HU-064 | 56 | 4 | Historial y clasificación |
| HU 61 | HU-045 | 57 | 3 | Programar una evaluación |
| HU 62 | HU-046 | 58 | 3 | Agregar problemas a una evaluación |
| HU 63 | HU-047 | 59 | 3 | Asignación de puntos |
| HU 64 | HU-048 | 60 | 3 | Control automático de acceso |

### 7. Carga por integrante

Horas (y tareas) por responsable, contando solo a quien responde por la tarea.

| Sprint | Juan Diego | Carlos | Gael | Equipo |
| :--- | :--- | :--- | :--- | :--- |
| 1 | 43 h (29) | 31 h (21) | 20 h (18) | 94 h (68) |
| 2 | 49 h (35) | 29 h (20) | 13 h (12) | 91 h (67) |
| 3 | 41 h (30) | 37 h (27) | 12 h (12) | 90 h (69) |
| 4 | 45 h (32) | 0 h (0) | 45 h (34) | 90 h (66) |

La capacidad es de 30 h por persona y por sprint. El reparto de los sprints 2 a 4 debe rebalancearse en cada Planning.

---

## Sprint 1   5 – 16 oct 2026
**Hito: Cursos y banco de problemas manual; cuenta única del estudiante (E1 + base ADR-4)**
16 historias · 68 tareas · 94 h

### HU 1

Como docente, quiero registrarme con mi correo institucional para que solo el personal autorizado pueda acceder al panel.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU01-T1 | Crear el modelo Docente en admin-api (tabla ADMIN_DOCENTE del modelo de datos): perfil con nombre, correo e institución, ligado al usuario de Django (auth_user). Generar la migración con makemigrations para que cree la tabla en el schema admin_schema. Está lista cuando la migración corre sin errores sobre una base vacía. | admin-api | Juan Diego | — | 01.1 | La migración corre sin errores sobre una base vacía y crea ADMIN_DOCENTE en admin_schema. | 1 |
| HU01-T2 | Crear el endpoint registro/docente/ (método POST). Debe validar que el correo pertenezca al dominio institucional permitido y crear la cuenta inactiva (is_active = false): el Product Owner la activa a mano desde el admin de Django. Esta validación de dominio se reutiliza después en el registro de estudiantes (HU21). | admin-api | Juan Diego | HU01-T1 | 01.1, 01.2, 01.3, 01.4 | POST registro/docente/ con correo del dominio crea la cuenta con is_active=false (201); dominio ajeno y correo repetido devuelven 400 con un mensaje por campo. | 2 |
| HU01-T3 | Construir en React la pantalla de registro del docente (nombre, correo y contraseña) y conectarla con registro/docente/. Al hacerlo, crear un componente base de formulario (campos, mensajes de error y botón de envío) pensado para reutilizarse en las demás pantallas del sprint, como el formulario de cursos (HU9) y el de problemas (HU13). | admin-web | Carlos | HU01-T2 | 01.1, 01.2, 01.3, 01.4 | El formulario envía a registro/docente/, muestra cada error junto a su campo y avisa que la cuenta queda pendiente; otra pantalla puede importar el componente base sin cambiarlo. | 3 |
| HU01-T4 | Escribir las pruebas automáticas del registro: correo institucional válido (crea la cuenta inactiva), correo de otro dominio (rechazado), correo ya registrado (rechazado) y un intento de iniciar sesión con la cuenta aún inactiva (debe fallar con un mensaje claro de cuenta pendiente de activación). | admin-api | Gael (+ Juan Diego) | HU01-T2, HU02-T2 | 01.1, 01.2, 01.3, 01.4 | Las cuatro pruebas (válido, ajeno, repetido, inicio de sesión con cuenta inactiva) pasan en el CI. | 2 |
| **Subtotal** | | | | | | | **8 h** |

### HU 2

Como docente, quiero iniciar sesión y que la sesión se mantenga activa durante la clase.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU02-T1 | Configurar djangorestframework-simplejwt en admin-api para firmar el token con RS256 (clave privada en una variable de entorno, nunca en el repositorio). El token debe llevar los claims sub, email, rol, institucion, cliente (desde qué aplicación inició sesión), iss, iat y exp. Es el mismo formato que luego verificará user-api (ADR-4). | admin-api | Juan Diego | HU22-T6 | 02.1 | Un token emitido tiene alg RS256 y los claims sub, email, rol, institucion, cliente, iss, iat y exp; la clave privada sale de una variable de entorno. | 2 |
| HU02-T2 | Implementar el inicio de sesión (POST con correo y contraseña que devuelve un access token y un refresh token) y la renovación del token. El access token dura 15 minutos y el refresh 30 minutos en la web, con rotación: cada renovación entrega un refresh nuevo e invalida el anterior. Incluye el cierre de sesión (logout), que invalida el refresh al instante, y la duración por cliente: el refresh dura 30 días en el móvil (claim cliente). | admin-api | Juan Diego | HU02-T1, HU01-T2 | 02.1, 02.2, 02.5 | login devuelve access y refresh (200); correo inexistente y contraseña errónea dan el mismo 401; el refresh rota y el anterior da 401; logout invalida el refresh; el refresh dura 30 min en web y 30 días en móvil. | 2 |
| HU02-T3 | Crear el interceptor HTTP del panel docente: guarda los tokens al iniciar sesión, agrega Authorization: Bearer <access> a cada petición hacia admin-api y, si recibe un 401, pide un access nuevo con el refresh y repite la petición sin que el docente lo note. | admin-web | Carlos | HU02-T2 | 02.2, 02.3 | Cada petición a admin-api lleva Authorization: Bearer; ante un 401 el interceptor pide un access nuevo y repite la petición sin que el docente lo note. | 2 |
| HU02-T4 | Cuando la sesión expira del todo (el refresh también venció), llevar al docente a la pantalla de inicio de sesión recordando la ruta en la que estaba, para devolverlo a ella después de volver a entrar. | admin-web | Carlos | HU02-T3 | 02.4 | Con el refresh vencido el docente llega a la pantalla de inicio de sesión y, tras entrar, vuelve a la ruta que intentaba abrir. | 1 |
| HU02-T5 | Pruebas de expiración y renovación: un access token vencido se renueva solo con el refresh; un refresh vencido obliga a iniciar sesión y, al hacerlo, vuelve a la ruta original. | admin-api + admin-web | Gael (+ Carlos, Juan Diego) | HU02-T2, HU02-T3, HU02-T4 | 02.2, 02.4 | Pasan las pruebas: un access vencido se renueva solo; un refresh vencido lleva al inicio de sesión y después a la ruta original. | 1 |
| **Subtotal** | | | | | | | **8 h** |

### HU 3

Como docente, quiero recuperar mi contraseña mediante un enlace enviado al correo.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU03-T1 | Configurar el servicio gratuito de envío de correo (SMTP) en admin-api mediante variables de entorno y comprobar en local que llega un correo de prueba. Es la base del correo de recuperación de contraseña. | admin-api | Juan Diego | — | 03.2 | Con las variables de entorno de SMTP llega un correo de prueba (o, en desarrollo, el enlace se imprime en consola). | 2 |
| HU03-T2 | Generar el token de recuperación de contraseña: de un solo uso y con tiempo de vida limitado (usar el generador de tokens de Django). Se envía dentro del enlace del correo. Incluye los dos endpoints del flujo (pedir el enlace y fijar la contraseña nueva). El enlace vence a los 30 minutos. | admin-api | Juan Diego | HU03-T1 | 03.1, 03.2, 03.3, 03.4 | Los dos endpoints (pedir enlace y fijar contraseña nueva) funcionan; el token es de un solo uso y vence a los 30 minutos. | 1 |
| HU03-T3 | Construir las dos pantallas del flujo: «Olvidé mi contraseña» (el docente escribe su correo y se le envía el enlace) y «Nueva contraseña» (se abre desde el enlace, pide la contraseña dos veces y la guarda). | admin-web | Carlos | HU03-T2 | 03.1, 03.4 | Desde «Olvidé mi contraseña» el docente pide el enlace; «Nueva contraseña» pide la clave dos veces y la guarda. | 2 |
| HU03-T4 | Prueba: un enlace de recuperación vencido, o que ya fue usado una vez, no permite cambiar la contraseña y devuelve un mensaje claro. | admin-api | Gael (+ Juan Diego) | HU03-T2 | 03.3, 03.4 | Un enlace vencido y uno ya usado devuelven un mensaje claro y no cambian la contraseña. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 4

Como administrador, quiero contraseñas cifradas y sesión que caduque por inactividad, para proteger las cuentas en las PC compartidas.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU04-T1 | Confirmar que admin-api guarda las contraseñas con el hash que Django aplica por defecto sobre auth_user (nunca en texto plano) y dejar escrito en el ADR de autenticación compartida (ADR-4) qué algoritmo se usa. | admin-api + docs | Gael (+ Juan Diego) | HU01-T2 | 04.1 | Una cuenta de prueba tiene en auth_user un hash (no texto plano) y el ADR-4 nombra el algoritmo. | 1 |
| HU04-T2 | Hacer que la sesión caduque por inactividad: con el refresh de 30 minutos de la web, si el docente no hace nada durante ese tiempo no puede renovar su token y debe iniciar sesión otra vez. Comprobarlo con un token realmente vencido. | admin-api + admin-web | Juan Diego (+ Carlos) | HU02-T2 | 04.2 | Un refresh vencido (30 min) no permite renovar y el docente debe iniciar sesión otra vez, comprobado con un token realmente vencido. | 1 |
| HU04-T3 | Agregar el botón «Cerrar sesión» al panel docente: borra los tokens del navegador e invalida el refresh token en el servidor (lista de tokens revocados de simplejwt) para que no pueda volver a usarse. | admin-web | Carlos | HU02-T2, HU02-T3 | 04.3 | El botón borra los tokens del navegador y el refresh invalidado da 401 en /refresh/. | 1 |
| HU04-T4 | Revisar admin-api para que ninguna contraseña (en el registro, en el login ni en mensajes de error) se escriba en los logs. Buscar print o logger que impriman el cuerpo completo de una petición. | admin-api | Gael (+ Juan Diego) | HU02-T2, HU03-T2 | 04.4 | Se buscó print y logger en admin-api: ningún log contiene contraseñas ni el cuerpo completo de peticiones de login o registro. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 9

Como docente, quiero crear un curso indicando nombre y lenguaje de programación.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU09-T1 | Crear el modelo Curso en admin-api (tabla ADMIN_CURSO): docente propietario, nombre, lenguaje de programación principal y fecha de creación. Generar la migración en admin_schema. | admin-api | Juan Diego | — | 09.1 | La migración crea ADMIN_CURSO en admin_schema con docente, nombre, lenguaje y fecha de creación. | 1 |
| HU09-T2 | Crear el endpoint REST (con Django REST Framework) para crear cursos. El docente propietario se toma del token del usuario autenticado, nunca del cuerpo de la petición. | admin-api | Juan Diego | HU09-T1, HU02-T2 | 09.1, 09.2, 09.3, 09.4 | POST crea el curso con el docente tomado del token (201); sin nombre o sin lenguaje devuelve 400. | 2 |
| HU09-T3 | Construir el formulario «Nuevo curso» (nombre y lenguaje) en React reutilizando el componente base de formulario creado en HU1. | admin-web | Carlos | HU09-T2, HU01-T3 | 09.1, 09.2 | El formulario «Nuevo curso» usa el componente base y crea el curso. | 2 |
| HU09-T4 | Pruebas: crear un curso sin nombre o sin lenguaje devuelve un error de validación; crearlo correctamente lo deja asociado al docente que lo creó. | admin-api | Gael (+ Juan Diego) | HU09-T2 | 09.3, 09.4 | Pasan las pruebas: sin nombre 400, sin lenguaje 400 y el curso creado queda asociado a su docente. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 10

Como docente, quiero editar los datos de un curso ya creado.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU10-T1 | Crear el endpoint de edición de curso (PUT o PATCH) con permiso de propietario: solo el docente dueño del curso puede modificarlo; cualquier otro recibe un 403. | admin-api | Juan Diego | HU09-T1, HU02-T2 | 10.1, 10.3 | El propietario edita su curso (200); otro docente recibe 403 y el curso no cambia. | 2 |
| HU10-T2 | Reutilizar el formulario de creación de curso (HU9) en modo edición: se abre con los datos actuales del curso ya cargados. | admin-web | Carlos | HU10-T1, HU09-T3 | 10.1 | El formulario se abre con los datos actuales y guarda los cambios. | 1 |
| HU10-T3 | Mostrar un cuadro de confirmación antes de guardar los cambios del curso. | admin-web | Carlos | HU10-T2 | 10.2 | Antes de guardar aparece un cuadro de confirmación; cancelar no guarda. | 1 |
| HU10-T4 | Prueba: un docente que intenta modificar el curso de otro docente recibe un 403 y el curso no cambia. | admin-api | Gael (+ Juan Diego) | HU10-T1 | 10.3 | La prueba pasa: un docente que edita el curso de otro recibe 403 y el curso no cambia. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 11

Como docente, quiero ver el listado de mis cursos con la cantidad de problemas de cada uno.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU11-T1 | Crear la consulta que devuelve los cursos del docente autenticado junto con la cantidad de problemas de cada uno, calculada en una sola consulta a la base de datos (no una por curso). | admin-api | Juan Diego | HU09-T1, HU13-T1 | 11.1, 11.2 | Una sola consulta devuelve los cursos del docente con su cantidad de problemas (sin una consulta por curso). | 1 |
| HU11-T2 | Construir la tabla «Mis cursos» con las columnas nombre, lenguaje y cantidad de problemas. | admin-web | Carlos | HU11-T1 | 11.1, 11.2 | La tabla muestra nombre, lenguaje y cantidad de problemas de cada curso. | 2 |
| HU11-T3 | Agregar paginación a la tabla y un estado vacío con un mensaje que guíe al docente a crear su primer curso. | admin-web | Carlos | HU11-T2 | 11.4, 11.5 | Con más de una página se puede paginar; sin cursos aparece un mensaje que invita a crear el primero. | 1 |
| HU11-T4 | Agregar el buscador de cursos por nombre: un parámetro de búsqueda en el endpoint y un campo de búsqueda sobre la tabla. | admin-web + admin-api | Carlos (+ Juan Diego) | HU11-T1, HU11-T2 | 11.3 | Escribir en el buscador filtra la tabla por nombre (parámetro de búsqueda en el endpoint). | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 12

Como docente, quiero que el sistema impida eliminar un curso con problemas o inscritos.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU12-T1 | Proteger el borrado: en la relación entre Problema y Curso usar on_delete=PROTECT, de modo que Django no deje eliminar un curso que ya tiene problemas. La comprobación de estudiantes inscritos se completa en el Sprint 2, cuando exista la inscripción (HU28 T5). | admin-api | Juan Diego | HU13-T1 | 12.2 | Borrar un curso con problemas falla por la restricción PROTECT. | 1 |
| HU12-T2 | Cuando se bloquea el borrado, devolver un error de negocio (por ejemplo un 409) con un mensaje que explique el motivo, en lugar de un error genérico 500. | admin-api | Juan Diego | HU12-T1 | 12.2, 12.4 | El borrado bloqueado devuelve 409 con un mensaje que explica el motivo, no un 500. | 1 |
| HU12-T3 | Mostrar ese mensaje al docente como un aviso informativo, de modo que no parezca una falla del sistema. | admin-web | Carlos | HU12-T2 | 12.4 | El docente ve el mensaje como aviso informativo, con un estilo distinto al de error. | 1 |
| HU12-T4 | Pruebas: eliminar un curso vacío funciona; eliminar un curso con problemas es bloqueado y muestra el mensaje. | admin-api | Gael (+ Juan Diego) | HU12-T2 | 12.1, 12.2 | Pasan las pruebas: curso vacío se elimina (204) y curso con problemas devuelve 409 con mensaje. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 13

Como docente, quiero crear un problema con título, enunciado, lenguaje y dificultad dentro de uno de mis cursos.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU13-T1 | Crear el modelo Problema en admin-api (tabla ADMIN_PROBLEMA): curso, título, enunciado, lenguaje y dificultad. Generar la migración en admin_schema. | admin-api | Juan Diego | HU09-T1 | 13.2 | La migración crea ADMIN_PROBLEMA con curso, título, enunciado, lenguaje y dificultad. | 1 |
| HU13-T2 | Crear el endpoint para crear problemas dentro de un curso. Solo el docente propietario del curso puede hacerlo. | admin-api | Juan Diego | HU13-T1, HU02-T2 | 13.1, 13.2, 13.3 | POST crea el problema en un curso propio (201); en un curso ajeno devuelve 403; sin enunciado devuelve 400. | 2 |
| HU13-T3 | Construir el formulario de problema en React, con un editor de texto enriquecido para el enunciado. Reutiliza el componente base de formulario de HU1. | admin-web | Carlos | HU13-T2, HU01-T3 | 13.1, 13.2 | El formulario con editor de texto enriquecido crea el problema y conserva el formato del enunciado. | 2 |
| HU13-T4 | Prueba: no se puede guardar un problema sin enunciado. | admin-api | Gael (+ Juan Diego) | HU13-T2 | 13.3 | La prueba pasa: no se puede guardar un problema sin enunciado. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 14

Como docente, quiero indicar la plataforma y el tipo de ejercicio de cada problema.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU14-T1 | En el modelo Problema definir dos campos con opciones fijas: plataforma (web, movil o ambas: dónde se publica el problema) y tipo_ejercicio (escritura_codigo, prediccion_salida, opcion_multiple o completar_espacio: cómo se responde y se corrige). Generar la migración. | admin-api | Juan Diego | HU13-T1 | 14.1, 14.2 | plataforma y tipo_ejercicio solo aceptan los valores definidos y la migración corre sin errores. | 1 |
| HU14-T2 | Agregar al formulario de problema los selectores de plataforma y de tipo de ejercicio. | admin-web | Carlos | HU14-T1, HU13-T3 | 14.1, 14.2 | El formulario tiene los dos selectores y envía sus valores. | 1 |
| HU14-T3 | Hacer que el formulario muestre solo los campos de respuesta que corresponden al tipo elegido: casos de prueba para escritura_codigo, alternativas para opcion_multiple y respuesta esperada para prediccion_salida y completar_espacio. | admin-web | Carlos | HU14-T2 | 14.3 | Al elegir cada tipo cambian los campos: casos de prueba (escritura_codigo), alternativas (opcion_multiple) o respuesta esperada (los otros dos). | 2 |
| HU14-T4 | Pruebas de las combinaciones válidas de plataforma y tipo de ejercicio (por ejemplo escritura_codigo con web, y opcion_multiple con movil). | admin-api | Gael (+ Juan Diego) | HU14-T1 | 14.4 | Pasan las pruebas: escritura_codigo con web y opcion_multiple con movil se guardan y se recuperan igual. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 15

Como docente, quiero que el sistema rechace las combinaciones imposibles, como pedir escritura de código en el celular.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU15-T1 | Escribir en el serializer de Problema la regla que rechaza combinaciones imposibles: escritura_codigo en movil (el celular no ejecuta código en un sandbox) y cualquiera de prediccion_salida, opcion_multiple o completar_espacio en web. | admin-api | Juan Diego | HU14-T1 | 15.1, 15.2, 15.3 | El serializer rechaza con 400 escritura_codigo en movil y los tres tipos móviles en web. | 2 |
| HU15-T2 | Reforzar la misma regla con un CheckConstraint en la base de datos de admin_schema, para que ni siquiera el admin de Django pueda guardar una combinación imposible. | admin-api | Juan Diego | HU15-T1 | 15.2 | Guardar una combinación imposible desde el admin de Django o con SQL falla por el CheckConstraint. | 1 |
| HU15-T3 | Mostrar el mensaje de error de esa regla junto al campo que está mal (por ejemplo, debajo de «Plataforma»). | admin-web | Carlos | HU15-T1, HU14-T2 | 15.4 | El mensaje aparece debajo del campo equivocado (por ejemplo «Plataforma»). | 1 |
| HU15-T4 | Pruebas que recorran todas las combinaciones de plataforma y tipo de ejercicio: las válidas se guardan y las imposibles devuelven error. | admin-api | Gael (+ Juan Diego) | HU15-T1, HU15-T2 | 15.1, 15.2, 15.3 | Una prueba recorre todas las combinaciones: las válidas se guardan y las imposibles dan error. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 16

Como docente, quiero registrar los casos de prueba de un problema, distinguiendo los públicos de los ocultos.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU16-T1 | Crear el modelo CasoPrueba (tabla ADMIN_CASO_PRUEBA): problema, entrada, salida esperada y es_publico. Un caso público se muestra al estudiante como ejemplo; uno oculto solo sirve para calificar. Generar la migración. | admin-api | Juan Diego | HU13-T1 | 16.2 | La migración crea ADMIN_CASO_PRUEBA con problema, entrada, salida esperada y es_publico. | 1 |
| HU16-T2 | Construir el formulario anidado dentro del formulario de problema para agregar y quitar casos de prueba sin salir de la pantalla. | admin-web | Carlos | HU16-T1, HU14-T3 | 16.1, 16.2 | Dentro del formulario del problema se agregan y quitan casos sin salir de la pantalla, y cada uno se marca público u oculto. | 2 |
| HU16-T3 | Exigir al menos un caso de prueba en los problemas de tipo escritura_codigo (validación en el serializer y mensaje en el formulario) y permitir marcar cada caso como público u oculto. Exponer además la vista pública de los casos de prueba: devuelve completos solo los públicos y de los ocultos nada; es la que más adelante consumirá user-api. | admin-api + admin-web | Juan Diego (+ Carlos) | HU16-T1, HU14-T3 | 16.2, 16.3 | Un problema escritura_codigo sin casos devuelve 400 y el formulario lo avisa; la vista pública de casos devuelve solo los públicos. | 2 |
| HU16-T4 | Prueba: la vista pública de casos de prueba no incluye la entrada ni la salida de ningún caso oculto. La API del estudiante se vuelve a probar en HU32 T4 y HU39 T4. | admin-api | Gael (+ Juan Diego) | HU16-T3 | 16.4 | La prueba pasa: la vista pública de casos no contiene entrada ni salida de ningún caso oculto. La API del estudiante se vuelve a probar en HU32-T4 y HU39-T4. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 21

Como estudiante, quiero registrarme con mi correo institucional.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU21-T1 | Crear la migración Flyway (archivo SQL versionado) de la tabla APP_ESTUDIANTE en el schema app_schema: perfil del estudiante, identificado con el email que viene en el token (el email es el identificador común entre los dos esquemas). El perfil se crea la primera vez que llega una petición con un token válido. | user-api | Juan Diego | — | 21.1 | Flyway aplica la migración sobre app_schema sin errores y la tabla APP_ESTUDIANTE existe, identificada por el email. | 1 |
| HU21-T2 | Crear el endpoint registro/estudiante/ en admin-api. Reutiliza la validación de dominio institucional del registro de docentes (HU1), pero crea la cuenta ya activa y con rol = estudiante. | admin-api | Juan Diego | HU01-T2 | 21.1, 21.2, 21.3 | POST registro/estudiante/ crea la cuenta activa con rol estudiante (201); dominio ajeno y correo repetido devuelven 400. | 1 |
| HU21-T3 | Construir la pantalla de registro de estudiantes en la web del estudiante (React + Vite), siguiendo el mismo componente base de formulario de HU1. | user-web | Carlos | HU21-T2, HU01-T3 | 21.1, 21.2, 21.3 | La pantalla registra a un estudiante y muestra cada error junto a su campo, con el componente base. | 2 |
| HU21-T4 | Pruebas: registro de estudiante válido (cuenta activa con rol estudiante) y registro con un correo ya existente (rechazado). | admin-api | Gael (+ Juan Diego) | HU21-T2 | 21.1, 21.3 | Pasan las pruebas: registro válido (cuenta activa, rol estudiante) y correo existente rechazado. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 22

Como estudiante, quiero iniciar sesión con la misma cuenta en la web y en la app móvil.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU22-T1 | Configurar user-api (Spring Boot) como resource server OAuth2: debe verificar la firma RS256 de cada JWT con la clave pública que descarga desde la URL de la variable JWKS_URI. Así user-api confía en los tokens que emite admin-api sin compartir contraseñas ni base de datos (ADR-4). Incluye el endpoint protegido mínimo GET /api/yo (devuelve el email y el rol del token), la regla de que solo el rol estudiante entra a /api/** (un token de docente recibe 403) y la creación del perfil en APP_ESTUDIANTE la primera vez que llega un token válido. | user-api | Juan Diego | HU22-T2, HU22-T6, HU21-T1 | 22.3, 22.4, 22.5, 22.6 | Con curl: token válido 200 en /api/yo con email y rol; sin token 401; firma alterada 401; vencido 401; token de docente 403; la primera petición crea el perfil en app_schema. | 3 |
| HU22-T2 | Publicar en admin-api la clave pública en GET /.well-known/jwks.json (formato JWKS), que es lo que consulta user-api para verificar los tokens. | admin-api | Juan Diego | HU22-T6, HU02-T1 | 22.3 | GET /.well-known/jwks.json responde 200 sin autenticación con un JWK RS256, y un token real de admin-api se verifica con él. | 1 |
| HU22-T3 | Implementar el inicio de sesión en la web del estudiante: llama al login de admin-api, guarda access y refresh (refresh de 30 minutos con rotación) y renueva el token antes de que venza. | user-web | Carlos | HU02-T2, HU21-T3, HU22-T7 | 22.1 | El estudiante entra desde user-web, los tokens se guardan, el access se renueva antes de vencer y el navegador no muestra error de CORS. | 1 |
| HU22-T4 | Implementar el inicio de sesión en la app Android (Kotlin + Jetpack Compose): guardar el refresh token cifrado en el teléfono (refresh de 30 días con rotación). | mobile | Gael | HU02-T2 | 22.2 | En la app el inicio de sesión funciona con la misma cuenta; el refresh se guarda cifrado y se renueva con rotación (30 días). | 2 |
| HU22-T5 | Prueba de punta a punta: la misma cuenta inicia sesión en web y en móvil; user-api responde 200 con un token válido y 401 si falta, fue alterado o está vencido. | user-api + mobile | Gael (+ Juan Diego) | HU22-T1, HU22-T3, HU22-T4 | 22.1, 22.2, 22.3, 22.4 | La misma cuenta inicia sesión en web y móvil; user-api responde 200 con token válido y 401 si falta, está alterado o vencido. | 1 |
| HU22-T6 | Generar el par de claves RSA, guardar la privada en .env (nunca en el repositorio) y documentar cómo regenerarlas. Si se regeneran, los tokens vigentes dejan de servir. *(nueva)* | infra | Juan Diego | — | 22.3 | make claves-jwt genera el par y deja la privada en .env (ignorado por git) sin imprimirla; docs/claves-jwt.md explica cómo regenerarla. | 2 |
| HU22-T7 | Configurar CORS en admin-api para que el navegador permita las peticiones de la web del estudiante (orígenes http://localhost:3001 y http://localhost:3000, definidos por variable de entorno). La app Kotlin no lo necesita. *(nueva)* | admin-api | Juan Diego | — | 22.1 | Una petición desde http://localhost:3001 recibe las cabeceras CORS y el navegador no la bloquea; los orígenes salen de una variable de entorno. | 2 |
| **Subtotal** | | | | | | | **12 h** |

### HU 23

Como estudiante, quiero recuperar mi contraseña desde la pantalla de inicio de sesión.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU23-T1 | Reutilizar para estudiantes el flujo de recuperación de contraseña hecho en HU3 (mismos endpoints y token); solo puede cambiar el texto del correo. | admin-api | Juan Diego | HU03-T2, HU21-T2 | 23.4 | Un estudiante pide y completa la recuperación con los mismos endpoints de HU 3; solo cambia el texto del correo. | 1 |
| HU23-T2 | Agregar el enlace «Olvidé mi contraseña» en la pantalla de inicio de sesión de la web del estudiante y de la app móvil. | user-web + mobile | Carlos (+ Gael) | HU03-T3, HU22-T3, HU22-T4 | 23.1, 23.2 | «Olvidé mi contraseña» aparece en el inicio de sesión de la web y de la app. | 1 |
| HU23-T3 | Verificar que el enlace que llega por correo se abre bien desde el celular (hacia la app o hacia una página que funcione en móvil). | mobile | Gael | HU23-T2 | 23.3 | El enlace del correo se abre en el navegador del celular y lleva a la pantalla de nueva contraseña (o a la app). | 1 |
| HU23-T4 | Prueba del proceso completo: pedir el enlace, abrirlo, cambiar la contraseña e iniciar sesión con la nueva. | admin-api + mobile | Gael (+ Juan Diego) | HU23-T1, HU23-T2, HU23-T3 | 23.4 | Proceso completo comprobado: pedir enlace, abrirlo, cambiar la contraseña e iniciar sesión con la nueva. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 24

Como estudiante, quiero que mi perfil y progreso estén asociados a mi cuenta y no al dispositivo.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU24-T1 | Crear la tabla de envíos APP_ENVIO con una migración Flyway mínima: id, estudiante_id (clave foránea a APP_ESTUDIANTE), problema_id, plataforma (web o movil) y fecha. HU36 T1 la completa con código, resultado, puntaje y número de intento. | user-api | Juan Diego | HU21-T1 | 24.1, 24.2 | La migración corre sin errores y APP_ENVIO rechaza una plataforma distinta de web o movil. | 1 |
| HU24-T2 | Crear la consulta agregada del avance del estudiante: junta lo resuelto desde web y móvil bajo la misma cuenta, sin importar el dispositivo. | user-api | Juan Diego | HU24-T1, HU22-T1 | 24.1, 24.3 | Con envíos de prueba de web y de móvil de la misma cuenta, la consulta devuelve el avance junto. | 2 |
| HU24-T3 | Mostrar ese avance en la web del estudiante. | user-web | Carlos | HU24-T2, HU22-T3 | 24.3, 24.4 | La web muestra el avance que devuelve user-api. | 1 |
| HU24-T4 | Prueba: un envío de móvil sembrado por la prueba (el envío real llega en HU45 T3) aparece en el avance que se ve en la web. | user-api | Gael (+ Juan Diego) | HU24-T2 | 24.4 | Con un envío de móvil sembrado por la prueba, el avance que ve la web lo incluye. La comprobación con un envío real queda en HU45-T3. | 1 |
| **Subtotal** | | | | | | | **5 h** |

---

## Sprint 2   19 – 30 oct 2026
**Hito: Editor + sandbox, generación con IA, inscripción a cursos (E2 parcial, E3, base E4)**
16 historias · 67 tareas · 91 h

### HU 5

Como estudiante, quiero escribir mi solución en un editor con resaltado de sintaxis y numeración de líneas, para programar con comodidad desde el navegador.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU05-T1 | Integrar el editor Monaco (el mismo editor de VS Code, paquete @monaco-editor/react) en la web del estudiante como componente reutilizable. Es la base de todos los ejercicios de escritura de código. | user-web | Carlos | — | 05.1 | El editor Monaco se importa como componente reutilizable y muestra numeración de líneas. | 2 |
| HU05-T2 | Configurar el resaltado de sintaxis para los lenguajes del proyecto: Python, Java y JavaScript. | user-web | Carlos | HU05-T1 | 05.2 | Python, Java y JavaScript se resaltan al elegir cada lenguaje. | 1 |
| HU05-T3 | Ajustar el editor para que se vea y funcione bien en pantallas pequeñas, sin desbordar la página. | user-web | Carlos | HU05-T1 | 05.4 | Con 360 px de ancho el editor no desborda la página. | 1 |
| HU05-T4 | Probar el editor con código muy largo y con caracteres especiales (tildes, emojis, tabulaciones). | user-web | Gael (+ Carlos) | HU05-T1 | 05.3 | Un código de 2000 líneas y un texto con tildes, emojis y tabulaciones se editan sin errores. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 6

Como estudiante, quiero ejecutar mi código y ver la salida sin registrarlo como intento oficial, para probar ideas antes de comprometer una calificación.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU06-T1 | Crear el endpoint de «ejecución de prueba»: recibe el código y una entrada, lo ejecuta en el sandbox y devuelve la salida. No crea ningún APP_ENVIO, así que no cuenta como intento oficial. | user-api | Juan Diego | HU07-T3 | 06.2, 06.3 | El endpoint devuelve la salida o el error de la ejecución y no crea ninguna fila en APP_ENVIO. | 2 |
| HU06-T2 | Agregar al editor el botón «Ejecutar» y un panel donde se muestra la salida o el error de la ejecución. | user-web | Carlos | HU06-T1, HU05-T1 | 06.1, 06.3 | El botón Ejecutar muestra la salida o el error en un panel. | 2 |
| HU06-T3 | Mostrar el estado «ejecutando…» y deshabilitar el botón mientras tanto, para evitar el doble clic. | user-web | Carlos | HU06-T2 | 06.4 | Mientras ejecuta, el botón queda deshabilitado y muestra «ejecutando…». | 1 |
| HU06-T4 | Limitar cuántas ejecuciones seguidas puede hacer un estudiante por minuto (rate limit), para no saturar el sandbox. Por defecto 10 por minuto, configurable por variable de entorno. | user-api | Juan Diego | HU06-T1 | 06.5 | Superar el límite de ejecuciones por minuto (por defecto 10, en una variable de entorno) devuelve 429. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 7

Como administrador, quiero que el código de los estudiantes se ejecute en un entorno aislado con límites de tiempo, memoria y sin acceso a la red.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU07-T1 | Levantar Judge0 (motor que ejecuta código ajeno en un entorno aislado, el «sandbox») autohospedado con docker-compose, agregándolo al docker-compose.yml único del monorepo para que docker compose up lo inicie junto al resto de servicios. | infra | Juan Diego | — | 07.1 | docker compose up levanta Judge0 junto al resto de servicios y un «hola mundo» se ejecuta. | 3 |
| HU07-T2 | Configurar en Judge0 los límites de tiempo de CPU y de memoria por ejecución y bloquear el acceso a la red, porque va a ejecutar código escrito por estudiantes. | infra | Juan Diego | HU07-T1 | 07.2, 07.3, 07.4 | Los límites de CPU y memoria están configurados y un intento de abrir una conexión de red falla. | 2 |
| HU07-T3 | Implementar en Spring Boot el cliente HTTP que envía código, lenguaje y entrada a Judge0 y recupera salida, errores y tiempos de ejecución. | user-api | Juan Diego | HU07-T1 | 07.1 | El cliente de user-api envía código, lenguaje y entrada y recupera salida, errores y tiempos. | 2 |
| HU07-T4 | Documentar en docs/ cómo se levanta y configura Judge0, para que cualquier compañero pueda repetirlo. | docs | Juan Diego | HU07-T2 | 07.1 | Otro integrante levanta y configura Judge0 siguiendo solo lo que dice docs/. | 1 |
| HU07-T5 | Pruebas de seguridad: un programa con bucle infinito debe cortarse por el límite de tiempo, y uno que intenta leer archivos del sistema debe quedar bloqueado. | user-api | Gael (+ Juan Diego) | HU07-T2, HU07-T3 | 07.2, 07.5 | Un bucle infinito se corta por el límite de tiempo y un programa que lee archivos del sistema queda bloqueado. | 1 |
| **Subtotal** | | | | | | | **9 h** |

### HU 8

Como responsable del proyecto, quiero poder cambiar de motor de ejecución por configuración, para no depender de un proveedor de pago.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU08-T1 | Definir en ai-service (carpeta app/providers/sandbox/) la interfaz común que debe cumplir cualquier proveedor de sandbox: recibe código, lenguaje y entrada, y devuelve salida y estado. Así el resto del código no depende de Judge0. | ai-service | Juan Diego | — | 08.2 | El resto del código solo depende de la interfaz común, no de Judge0. | 2 |
| HU08-T2 | Implementar el segundo proveedor, Piston (otro motor de ejecución de código), cumpliendo esa interfaz, junto al de Judge0. | ai-service | Juan Diego | HU08-T1 | 08.3 | El proveedor Piston devuelve salida y estado con la misma interfaz que Judge0. | 2 |
| HU08-T3 | Permitir elegir el proveedor activo con una sola variable de entorno, sin tocar los routers. | ai-service | Juan Diego | HU08-T1, HU08-T2 | 08.1, 08.2, 08.3 | Cambiar una variable de entorno cambia el proveedor activo sin tocar los routers. | 1 |
| HU08-T4 | Dejar operativo el proveedor mock, que devuelve resultados simulados, para poder trabajar y correr la integración continua (CI) sin tener ningún motor instalado. | ai-service | Juan Diego | HU08-T1 | 08.4 | Con el proveedor mock el CI corre sin ningún motor instalado. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 17

Como docente, quiero generar varios problemas indicando tema, nivel, lenguaje y cantidad, para preparar una práctica nueva rápidamente.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU17-T1 | Obtener la clave gratuita de uno de los proveedores de LLM soportados (Gemini, GitHub Models o Groq) y configurarla por variable de entorno en ai-service. La clave nunca se sube al repositorio. | ai-service | Juan Diego | — | 17.2 | ai-service lee la clave de una variable de entorno y git grep no la encuentra en el repositorio. | 1 |
| HU17-T2 | Escribir y afinar el prompt de generación: a partir de tema, nivel, lenguaje, cantidad y plataforma destino debe producir problemas con enunciado, casos de prueba y respuesta esperada en un formato JSON fijo. Probar con varios temas hasta que el formato sea estable. | ai-service | Juan Diego | HU17-T1 | 17.1, 17.4 | Con cinco temas distintos las cinco respuestas del LLM validan contra el formato JSON fijo. | 3 |
| HU17-T3 | Completar el endpoint POST /generate de ai-service, apoyado en la capa de abstracción de proveedores de LLM (el proveedor se elige por configuración; existe también mock para pruebas). | ai-service | Juan Diego | HU17-T2, HU08-T4 | 17.2 | POST /generate devuelve problemas con el proveedor elegido por configuración y con el mock en las pruebas. | 2 |
| HU17-T4 | Construir el formulario de generación (tema, nivel, lenguaje y cantidad) con un indicador de progreso mientras la IA responde. | admin-web | Carlos | HU17-T3, HU17-T5 | 17.1, 17.3 | El formulario envía tema, nivel, lenguaje y cantidad y muestra un indicador de progreso hasta la respuesta. | 2 |
| HU17-T5 | Validar el esquema de la respuesta del LLM antes de guardarla. Los problemas se guardan con generado_por_ia = true y aprobado_por_docente = false: nunca se publican sin que un docente los revise. | admin-api | Juan Diego | HU17-T3, HU13-T1 | 17.4 | Una respuesta con esquema inválido no se guarda; una válida se guarda con generado_por_ia=true y aprobado_por_docente=false. | 1 |
| HU17-T6 | Manejar el caso de servicio de IA caído o respuesta inválida, mostrando al docente un error claro y la opción de reintentar. | admin-api + admin-web | Juan Diego (+ Carlos) | HU17-T5 | 17.5 | Con ai-service apagado o una respuesta inválida el docente ve un error claro con botón Reintentar y no se guarda nada. | 1 |
| **Subtotal** | | | | | | | **10 h** |

### HU 18

Como docente, quiero ver los problemas generados en una bandeja de pendientes, claramente marcados como creados por IA, para revisarlos antes de que lleguen a mis estudiantes.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU18-T1 | Crear la consulta de problemas pendientes de revisión (generado_por_ia = true y aprobado_por_docente = false) del docente, para mostrarlos con la marca «generado por IA». | admin-api | Juan Diego | HU17-T5 | 18.1, 18.2 | La consulta devuelve solo los problemas del docente con generado_por_ia=true y aprobado_por_docente=false. | 1 |
| HU18-T2 | Construir la pantalla «Bandeja de revisión» con la lista de problemas pendientes. | admin-web | Carlos | HU18-T1 | 18.1, 18.2 | La pantalla lista los pendientes con la marca «generado por IA». | 2 |
| HU18-T3 | Agregar filtros por curso y por estado de revisión. | admin-web | Carlos | HU18-T2 | 18.3, 18.4 | Los filtros por curso y por estado de revisión cambian la lista. | 1 |
| HU18-T4 | Mostrar el contador de pendientes y probar que ningún problema generado por IA se publica automáticamente. | admin-web + admin-api | Carlos (+ Juan Diego) | HU18-T2 | 18.5, 18.6 | El contador coincide con la lista y una prueba confirma que un problema generado no es visible para el estudiante sin aprobarse. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 19

Como docente, quiero editar el enunciado, los casos de prueba o la respuesta esperada antes de aprobar un problema, para corregir lo que la IA haya generado mal.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU19-T1 | Reutilizar el formulario de problema (enunciado, casos de prueba y respuesta esperada) dentro de la pantalla de revisión, para que el docente pueda corregir antes de aprobar. | admin-web | Carlos | HU13-T3, HU16-T2, HU18-T2 | 19.1, 19.2, 19.3 | En la pantalla de revisión se edita enunciado, casos de prueba y respuesta esperada con el mismo formulario. | 1 |
| HU19-T2 | Implementar la acción «Aprobar»: marca aprobado_por_docente = true y deja el problema visible para los estudiantes. | admin-api + admin-web | Juan Diego (+ Carlos) | HU19-T1, HU19-T3 | 19.4 | Aprobar marca aprobado_por_docente=true y el problema aparece en la API pública para el estudiante. | 2 |
| HU19-T3 | Registrar quién aprobó cada problema y en qué fecha. | admin-api | Juan Diego | HU13-T1 | 19.5 | Cada aprobación guarda el docente que aprobó y la fecha. | 1 |
| HU19-T4 | Prueba: no se puede aprobar un problema incompleto (por ejemplo, uno sin casos de prueba). | admin-api | Gael (+ Juan Diego) | HU19-T2 | 19.6 | Aprobar un problema sin casos de prueba devuelve 400. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 20

Como docente, quiero descartar un problema generado que no me sirve, para que no ocupe espacio en el catálogo de mi curso.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU20-T1 | Implementar la acción «Descartar» con un cuadro de confirmación previo. | admin-api + admin-web | Juan Diego (+ Carlos) | HU18-T2 | 20.2 | Al descartar aparece un cuadro de confirmación y cancelar no cambia nada. | 1 |
| HU20-T2 | Archivar el problema descartado (marcarlo como archivado) en lugar de borrarlo de la base de datos. | admin-api | Juan Diego | HU13-T1 | 20.3 | El problema descartado queda marcado como archivado y sigue existiendo en la base. | 1 |
| HU20-T3 | Agregar el botón «Descartar» a la bandeja de revisión. | admin-web | Carlos | HU20-T1, HU18-T2 | 20.1 | La bandeja tiene el botón Descartar en cada problema. | 1 |
| HU20-T4 | Prueba: un problema descartado no vuelve a aparecer en la bandeja ni en el catálogo del curso. | admin-api | Gael (+ Juan Diego) | HU20-T2 | 20.4 | Un problema descartado no aparece en la bandeja ni en el catálogo del curso. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 25

Como estudiante, quiero buscar los cursos publicados de mi institución, para encontrar el que corresponda a mi sección.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU25-T1 | Crear en admin-api el endpoint de cursos publicados. user-api no puede leer las tablas de admin_schema (ADR-1); consulta los cursos mediante esta API interna. Un curso está publicado cuando tiene al menos un problema aprobado (regla propuesta, a confirmar en el planning del Sprint 2). | admin-api | Juan Diego | HU19-T2 | 25.1 | El endpoint interno devuelve solo los cursos publicados (con al menos un problema aprobado) con su institución. | 1 |
| HU25-T2 | Consumir ese endpoint desde user-api para ofrecer al estudiante la lista de cursos disponibles. | user-api | Juan Diego | HU25-T1, HU22-T1 | 25.1, 25.2 | user-api lista los cursos consumiendo el endpoint de admin-api con un token válido de estudiante. | 2 |
| HU25-T3 | Construir la pantalla de búsqueda de cursos en la web del estudiante. | user-web | Carlos | HU25-T2 | 25.1, 25.2 | La pantalla muestra la lista de cursos y un campo de búsqueda. | 2 |
| HU25-T4 | Filtrar por la institución del estudiante (viene en el claim institucion del token) y mostrar «sin resultados» cuando no hay coincidencias. | user-api + user-web | Juan Diego (+ Carlos) | HU25-T2 | 25.3, 25.4 | Solo salen cursos de la institución del claim institucion; sin coincidencias aparece «sin resultados». | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 26

Como estudiante, quiero inscribirme usando el código de invitación que me da mi docente, para asegurar que entro al curso correcto.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU26-T1 | Generar un código de invitación único para cada curso, que el docente comparte con sus estudiantes, y guardarlo en el curso. El código tiene fecha de vencimiento (30 días desde que se genera, configurable por variable de entorno) y la migración también lo genera para los cursos ya creados. | admin-api | Juan Diego | HU09-T1 | 26.1, 26.4 | Cada curso, nuevo o ya existente, tiene un código único con fecha de vencimiento (30 días por defecto, configurable). | 1 |
| HU26-T2 | Crear la migración Flyway del modelo Inscripcion (APP_INSCRIPCION): estudiante, curso_id y fecha. curso_id es solo una referencia lógica, sin clave foránea, porque el curso vive en otro schema (ADR-1). | user-api | Juan Diego | HU21-T1 | 26.2, 26.5 | La migración Flyway crea APP_INSCRIPCION con restricción única (estudiante, curso_id) y sin clave foránea al otro esquema. | 1 |
| HU26-T3 | Crear el endpoint de inscripción: recibe el código de invitación, lo valida contra admin-api y registra la inscripción. | user-api | Juan Diego | HU26-T1, HU26-T2, HU22-T1 | 26.2, 26.3, 26.4 | Código válido devuelve 201 con la inscripción; código incorrecto o vencido devuelve 400 con mensaje; consulta a admin-api, no a sus tablas. | 2 |
| HU26-T4 | Pruebas: código errado, código vencido e inscripción repetida al mismo curso. | user-api | Gael (+ Juan Diego) | HU26-T3 | 26.3, 26.4, 26.5 | Pasan las pruebas: código errado, código vencido e inscripción repetida. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 27

Como estudiante, quiero ver mis cursos inscritos en la pantalla principal, para entrar directamente a practicar.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU27-T1 | Crear la consulta de los cursos en los que está inscrito el estudiante. | user-api | Juan Diego | HU26-T2 | 27.1 | La consulta devuelve solo los cursos del estudiante autenticado. | 1 |
| HU27-T2 | Construir la pantalla principal de la web con una tarjeta por curso inscrito. | user-web | Carlos | HU27-T1 | 27.1, 27.2, 27.4 | La pantalla principal muestra una tarjeta por curso y cada una abre el curso. | 2 |
| HU27-T3 | Construir la misma pantalla principal en la app Android con Jetpack Compose. | mobile | Gael | HU27-T1, HU22-T4 | 27.1, 27.3, 27.4 | La app muestra la misma pantalla con una tarjeta por curso y cada una abre el curso. | 2 |
| **Subtotal** | | | | | | | **5 h** |

### HU 28

Como docente, quiero ver la lista de estudiantes inscritos en mi curso, para confirmar que toda mi sección se registró.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU28-T1 | Crear la consulta de estudiantes inscritos en un curso. La inscripción vive en app_schema (user-api), así que admin-api la obtiene por API interna y no leyendo tablas del otro servicio (ADR-1). | user-api + admin-api | Juan Diego | HU26-T2 | 28.1 | admin-api obtiene los inscritos de un curso por la API interna de user-api, sin leer sus tablas. | 1 |
| HU28-T2 | Construir la tabla de estudiantes inscritos dentro del panel del docente. | admin-web | Carlos | HU28-T1 | 28.1, 28.2 | El panel muestra la tabla de inscritos de un curso propio. | 2 |
| HU28-T3 | Agregar un botón que descargue la lista de inscritos como archivo CSV. | admin-web | Carlos | HU28-T2 | 28.3 | El botón descarga un CSV con la lista de inscritos. | 1 |
| HU28-T4 | Prueba: cada docente ve únicamente los inscritos de sus propios cursos. | admin-api | Gael (+ Juan Diego) | HU28-T1 | 28.4 | La prueba pasa: cada docente ve solo los inscritos de sus cursos y recibe 403 con los de otro. | 1 |
| HU28-T5 | Completar el borrado protegido de HU12 (CA-12.3): antes de eliminar un curso, admin-api pregunta a user-api, por la API interna de T1, si tiene estudiantes inscritos; si los tiene, responde el mismo 409 con un mensaje que lo explica. *(nueva)* | admin-api + user-api | Juan Diego | HU12-T2, HU28-T1 | 12.3 | Eliminar un curso con inscritos devuelve el mismo 409 de HU 12 con un mensaje que lo explica; un curso sin inscritos ni problemas se sigue eliminando. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 29

Como estudiante, quiero ver la lista de problemas de un curso con su dificultad y tipo de ejercicio, para elegir cuál resolver.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU29-T1 | Crear la consulta de los problemas de un curso en el que el estudiante está inscrito. | user-api | Juan Diego | HU26-T2, HU31-T1 | 29.1 | La consulta devuelve los problemas del curso solo si el estudiante está inscrito. | 1 |
| HU29-T2 | Construir el listado de problemas mostrando la dificultad y el tipo de ejercicio de cada uno. | user-web | Carlos | HU29-T1 | 29.2, 29.3 | El listado muestra la dificultad y el tipo de ejercicio de cada problema. | 2 |
| HU29-T3 | Ordenar el listado por dificultad y fecha de publicación, y paginarlo. | user-web | Carlos | HU29-T2 | 29.4, 29.5 | El listado se ordena por dificultad y fecha y se pagina. | 1 |
| HU29-T4 | Prueba: un estudiante que no está inscrito en el curso no puede ver el listado (recibe un 403). | user-api | Gael (+ Juan Diego) | HU29-T1 | 29.1 | Un estudiante no inscrito recibe 403 al pedir el listado. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 30

Como estudiante, quiero filtrar los problemas por dificultad y por estado, para enfocarme en lo que todavía me falta.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU30-T1 | Agregar a la consulta de problemas los filtros de dificultad y de estado. | user-api | Juan Diego | HU29-T1 | 30.1, 30.2 | La consulta acepta los filtros de dificultad y de estado. | 1 |
| HU30-T2 | Calcular el estado de cada problema para el estudiante a partir de sus envíos: resuelto, intentado o sin abrir. | user-api | Juan Diego | HU24-T1, HU30-T1 | 30.3 | Con envíos sembrados, cada problema sale como resuelto, intentado o sin abrir. | 2 |
| HU30-T3 | Agregar los botones de filtro al listado y recordar el último filtro usado. | user-web | Carlos | HU30-T1, HU29-T2 | 30.1, 30.2, 30.4 | Los botones filtran el listado y al volver se recuerda el último filtro. | 1 |
| HU30-T4 | Pruebas de combinaciones de filtros, incluida una combinación que no devuelve resultados. | user-api | Gael (+ Juan Diego) | HU30-T1, HU30-T2 | 30.5 | Pasan las pruebas de combinaciones de filtros, incluida una sin resultados. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 31

Como estudiante, quiero que solo aparezcan los problemas aprobados por el docente, para no toparme con ejercicios a medio revisar.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU31-T1 | Filtrar por aprobado_por_docente = true en la consulta que alimenta el listado de problemas del estudiante. | admin-api + user-api | Juan Diego | HU19-T2 | 31.1 | El listado de user-api solo incluye problemas con aprobado_por_docente=true. | 1 |
| HU31-T2 | Aplicar el mismo filtro al abrir un problema individual, para que no se pueda acceder a uno sin aprobar adivinando su id. | user-api | Juan Diego | HU31-T1 | 31.2 | Pedir por id un problema sin aprobar devuelve 404. | 1 |
| HU31-T3 | Auditar todas las rutas (web, móvil y API interna) para comprobar que ninguna se salta el filtro de aprobados. | user-api + admin-api | Gael (+ Juan Diego) | HU31-T2, HU32-T1 | 31.3 | Se revisaron todas las rutas (web, móvil e interna): ninguna devuelve problemas sin aprobar y la lista queda escrita en docs/. | 1 |
| HU31-T4 | Pruebas con un problema aprobado (visible) y con uno sin aprobar (no visible). | user-api | Gael (+ Juan Diego) | HU31-T2 | 31.1, 31.2 | Pasan las pruebas con un problema aprobado (visible) y uno sin aprobar (no visible). | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 32

Como estudiante, quiero abrir el detalle de un problema antes de empezar, para leer el enunciado y decidir si lo intento ahora.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU32-T1 | Crear el endpoint de detalle de un problema: enunciado, dificultad y tipo de ejercicio. | user-api | Juan Diego | HU31-T2, HU16-T3 | 32.1, 32.2, 32.4 | El endpoint devuelve enunciado, dificultad, tipo y solo los casos públicos. | 1 |
| HU32-T2 | Construir la pantalla de detalle con el enunciado y los casos de ejemplo públicos. | user-web | Carlos | HU32-T1 | 32.1, 32.2 | La pantalla muestra el enunciado y los casos de ejemplo. | 2 |
| HU32-T3 | Agregar el botón que lleva al editor de código del problema. | user-web | Carlos | HU32-T2, HU05-T1 | 32.3 | El botón abre el editor de código del problema. | 1 |
| HU32-T4 | Prueba: los casos de prueba ocultos no viajan en la respuesta. | user-api | Gael (+ Juan Diego) | HU32-T1 | 32.4 | La respuesta del detalle no contiene entrada ni salida de casos ocultos. | 1 |
| **Subtotal** | | | | | | | **5 h** |

---

## Sprint 3   2 – 13 nov 2026
**Hito: Calificación de punta a punta y evaluaciones. Cierra el MVP web**
16 historias · 69 tareas · 90 h

### HU 33 · backlog 29

Como estudiante, quiero ver el enunciado y los casos de ejemplo junto al editor, para no perder de vista lo que me piden mientras escribo la solución.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU33-T1 | Construir en user-web el layout de dos paneles redimensionables: el enunciado a la izquierda y el editor Monaco a la derecha, con un divisor que se arrastra. | user-web | Carlos | HU05-T1 | 33.1, 33.2 | La pantalla tiene el enunciado a la izquierda y el editor a la derecha, con un divisor que se arrastra. | 2 |
| HU33-T2 | Construir el panel del enunciado con formato de texto (títulos, listas y bloques de código) a partir del enunciado guardado. | user-web | Carlos | HU33-T1, HU32-T1 | 33.1 | El enunciado se muestra con títulos, listas y bloques de código. | 2 |
| HU33-T3 | Mostrar bajo el enunciado los ejemplos de entrada y salida (los casos de prueba públicos). | user-web | Carlos | HU33-T1, HU32-T1 | 33.3 | Bajo el enunciado aparecen los ejemplos de entrada y salida públicos. | 1 |
| HU33-T4 | Recordar la posición del divisor entre los dos paneles para la próxima vez que el estudiante abra un problema. | user-web | Carlos | HU33-T1 | 33.4 | Al reabrir un problema el divisor queda donde se dejó. | 1 |
| HU33-T5 | Probar el layout en una laptop pequeña y en un monitor grande. | user-web | Gael (+ Carlos) | HU33-T1, HU33-T4 | 33.5 | El layout se revisa en una laptop pequeña (1366 px) y en un monitor grande (1920 px) sin desbordes. | 1 |
| **Subtotal** | | | | | | | **7 h** |

### HU 34 · backlog 30

Como estudiante, quiero que el editor cargue un código base con la firma de la función, para empezar a resolver sin perder tiempo en la estructura.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU34-T1 | Agregar el campo codigo_base al modelo Problema y su migración. Es el «starter code»: código que se le entrega al estudiante ya escrito, por ejemplo la firma de la función que debe completar. | admin-api | Juan Diego | HU13-T1 | 34.1 | La migración agrega codigo_base a ADMIN_PROBLEMA y el problema lo guarda y lo devuelve. | 1 |
| HU34-T2 | Incluir ese campo en el formulario de problema del docente. | admin-web | Carlos | HU34-T1, HU13-T3 | 34.1 | El docente escribe el código base en el formulario y se guarda. | 1 |
| HU34-T3 | Cargar el código base en el editor cuando el estudiante abre el problema por primera vez. | user-web | Carlos | HU34-T1, HU32-T1, HU05-T1 | 34.2 | Al abrir el problema por primera vez el editor trae el código base. | 1 |
| HU34-T4 | Agregar el botón «Volver al código base original» con confirmación y probarlo. | user-web | Carlos | HU34-T3 | 34.3 | El botón pide confirmación y restaura el código base; cancelar no cambia el código. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 35 · backlog 31

Como estudiante, quiero que mi código se guarde automáticamente como borrador, para no perder el avance si se cierra el navegador.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU35-T1 | Crear la migración Flyway del modelo Borrador: el código guardado por cada estudiante en cada problema. | user-api | Juan Diego | HU22-T1 | 35.1, 35.4 | La migración crea APP_BORRADOR (estudiante, problema_id, código) con un borrador por estudiante y problema. | 1 |
| HU35-T2 | Crear el endpoint que guarda y recupera el borrador del estudiante para un problema. | user-api | Juan Diego | HU35-T1 | 35.1, 35.4 | El endpoint guarda y devuelve el borrador del estudiante autenticado. | 2 |
| HU35-T3 | Implementar el guardado periódico con antirrebote (debounce): se guarda cuando el estudiante deja de escribir 3 segundos, para no llamar al servidor en cada tecla. | user-web | Carlos | HU35-T2, HU05-T1 | 35.1, 35.2 | El editor guarda 3 segundos después de la última tecla y no envía una solicitud por tecla. | 1 |
| HU35-T4 | Mostrar en el editor un indicador de «último guardado». | user-web | Carlos | HU35-T3 | 35.3 | El editor muestra «Último guardado» con la hora. | 1 |
| HU35-T5 | Prueba: al cerrar y volver a abrir el problema aparece lo último que el estudiante escribió. | user-api + user-web | Gael (+ Carlos, Juan Diego) | HU35-T2, HU35-T3 | 35.4 | Al cerrar y volver a abrir el problema aparece lo último que se escribió. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 36 · backlog 32

Como estudiante, quiero enviar mi solución como intento oficial desde el mismo editor y recibir el resultado ahí mismo.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU36-T1 | Crear la migración Flyway que completa la tabla APP_ENVIO (creada en HU24 T1 con estudiante, problema_id, plataforma y fecha): agregar código, resultado, puntaje y número de intento. | user-api | Juan Diego | HU24-T1 | 36.3 | La migración completa APP_ENVIO con código, resultado, puntaje y número de intento. | 1 |
| HU36-T2 | Implementar el endpoint de envío: recibe el código, lo evalúa contra los casos de prueba usando el sandbox (ver HU7 y HU8) y guarda el resultado en APP_ENVIO. A diferencia de «Ejecutar» (HU6), un envío sí cuenta como intento oficial. | user-api | Juan Diego | HU36-T1, HU07-T3, HU08-T3, HU32-T1 | 36.1, 36.3, 36.4 | Un envío se evalúa contra los casos de prueba y se guarda en APP_ENVIO con su número de intento; «Ejecutar» sigue sin guardar nada. | 3 |
| HU36-T3 | Agregar el botón «Enviar» con una confirmación que muestre el número de intento. | user-web | Carlos | HU36-T2 | 36.2 | Al pulsar Enviar aparece una confirmación con el número de intento. | 1 |
| HU36-T4 | Mostrar el resultado en el mismo editor, sin recargar la página. | user-web | Carlos | HU36-T2 | 36.5 | El resultado aparece en el mismo editor sin recargar la página. | 2 |
| HU36-T5 | Bloquear el botón «Enviar» mientras el envío se evalúa, para evitar envíos duplicados. | user-web | Carlos | HU36-T3 | 36.6 | Mientras se evalúa, el botón Enviar está deshabilitado y un doble clic no crea dos envíos. | 1 |
| HU36-T6 | Prueba del flujo completo: escribir código, enviarlo y ver el resultado guardado. | user-api + user-web | Gael (+ Carlos, Juan Diego) | HU36-T4, HU36-T5 | 36.1, 36.5 | Flujo completo comprobado: escribir código, enviarlo y ver el resultado guardado en APP_ENVIO. | 1 |
| **Subtotal** | | | | | | | **9 h** |

### HU 37 · backlog 33

Como estudiante, quiero ver qué casos de prueba pasaron y cuáles no, comparando la salida esperada con la obtenida en los casos públicos.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU37-T1 | Crear el modelo ResultadoCasoPrueba (APP_RESULTADO_CASO_PRUEBA): envío, caso_prueba_id, si pasó o no y la salida obtenida. Se guarda una fila por cada caso evaluado al procesar un envío. | user-api | Juan Diego | HU36-T1 | 37.1, 37.2 | La migración crea APP_RESULTADO_CASO_PRUEBA y un envío guarda una fila por cada caso evaluado. | 2 |
| HU37-T2 | Construir la tabla que muestra qué casos pasaron y cuáles no. | user-web | Carlos | HU37-T1 | 37.1, 37.2 | La tabla marca cada caso público como pasó o falló. | 2 |
| HU37-T3 | En los casos públicos que fallaron, resaltar la diferencia entre la salida esperada y la obtenida. | user-web | Carlos | HU37-T2 | 37.3, 37.4 | En un caso público fallido se ven la salida esperada y la obtenida, con la diferencia resaltada. | 2 |
| HU37-T4 | En los casos ocultos mostrar solo «pasa» o «falla», sin revelar su entrada ni su salida. | user-api + user-web | Juan Diego (+ Carlos) | HU37-T1 | 37.5 | Un caso oculto muestra solo «pasa» o «falla». | 1 |
| HU37-T5 | Pruebas de los tres escenarios: todos los casos pasan, algunos pasan y ninguno pasa. | user-api | Gael (+ Juan Diego) | HU37-T1 | 37.1, 37.2 | Pasan las pruebas: todos los casos pasan, algunos pasan y ninguno pasa. | 1 |
| **Subtotal** | | | | | | | **8 h** |

### HU 38 · backlog 34

Como estudiante, quiero recibir un puntaje proporcional a la cantidad de casos superados, para saber si mi solución está parcialmente correcta.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU38-T1 | Definir y escribir en docs/ la fórmula del puntaje: proporcional a los casos de prueba superados (por ejemplo, 3 de 4 casos equivalen a 75 %). | docs | Juan Diego | — | 38.1 | docs/ contiene la fórmula con un ejemplo (3 de 4 casos equivalen a 75 %). | 1 |
| HU38-T2 | Calcular el puntaje al terminar la evaluación del envío y guardarlo en APP_ENVIO. | user-api | Juan Diego | HU38-T1, HU36-T2, HU37-T1 | 38.1, 38.2, 38.3, 38.4 | El puntaje se calcula con la fórmula y se guarda en APP_ENVIO. | 1 |
| HU38-T3 | Mostrar el puntaje y el resultado (correcto, parcial o incorrecto) en pantalla. | user-web | Carlos | HU38-T2 | 38.5 | La pantalla muestra el puntaje y el resultado (correcto, parcial o incorrecto). | 1 |
| HU38-T4 | Pruebas: solución totalmente correcta, parcial y totalmente incorrecta. | user-api | Gael (+ Juan Diego) | HU38-T2 | 38.2, 38.3, 38.4 | Pasan las pruebas: solución totalmente correcta, parcial y totalmente incorrecta. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 39 · backlog 35

Como docente, quiero que los casos de prueba ocultos no revelen su entrada al estudiante, para que la calificación mida la solución y no la capacidad de adivinar los ejemplos.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU39-T1 | Separar en user-api dos respuestas: la pública (la que ve el estudiante) y la interna (que incluye entrada y salida de los casos ocultos, solo para calificar). | user-api | Juan Diego | HU32-T1, HU37-T1 | 39.1, 39.2 | Existen la respuesta pública (sin datos ocultos) y la interna (solo para calificar), y las rutas del estudiante usan la pública. | 2 |
| HU39-T2 | Devolver de cada caso oculto únicamente si pasó o falló, nunca su entrada ni su salida. | user-api | Juan Diego | HU39-T1 | 39.3 | De cada caso oculto solo viaja si pasó o falló. | 1 |
| HU39-T3 | Auditar user-api, admin-api y ai-service para comprobar que ninguna ruta, mensaje de error o log expone el contenido de un caso oculto. | user-api + admin-api | Gael (+ Juan Diego) | HU39-T2 | 39.4 | Se revisaron rutas, mensajes de error y logs de user-api, admin-api y ai-service: nada expone un caso oculto, y queda anotado en docs/. | 1 |
| HU39-T4 | Prueba: el estudiante no puede conocer un caso oculto de ninguna forma. | user-api | Gael (+ Juan Diego) | HU39-T2 | 39.1, 39.2, 39.3, 39.4 | La prueba pasa: ninguna ruta del estudiante contiene entrada o salida de un caso oculto. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 40 · backlog 36

Como estudiante, quiero un mensaje claro cuando mi código exceda el tiempo límite o falle al compilar, para distinguir un error mío de una falla de la plataforma.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU40-T1 | Traducir los errores que devuelve el sandbox (tiempo agotado, memoria excedida, error de compilación) a mensajes comprensibles en español. | user-api | Juan Diego | HU36-T2, HU07-T3 | 40.1, 40.2 | Tiempo agotado, memoria excedida y error de compilación salen con mensajes comprensibles en español. | 2 |
| HU40-T2 | Indicar en qué línea está el error de sintaxis cuando el motor la informa. | user-api + user-web | Juan Diego (+ Carlos) | HU40-T1 | 40.3 | Cuando el motor informa la línea del error de sintaxis, el mensaje la muestra. | 2 |
| HU40-T3 | Diferenciar en pantalla un error del código del estudiante («tu programa falló») de una falla del servicio («inténtalo más tarde»). | user-web | Carlos | HU40-T1 | 40.4 | La pantalla distingue «tu programa falló» de «inténtalo más tarde». | 1 |
| HU40-T4 | Guardar el mensaje de error junto al envío, para poder revisarlo después en el historial. | user-api | Juan Diego | HU40-T1, HU36-T1 | 40.5 | El mensaje de error queda guardado con el envío y se ve en el historial. | 1 |
| HU40-T5 | Pruebas: tiempo agotado, error de sintaxis y servicio de sandbox caído. | user-api | Gael (+ Juan Diego) | HU40-T1, HU40-T2 | 40.1, 40.2, 40.4 | Pasan las pruebas: tiempo agotado, error de sintaxis y sandbox caído. | 1 |
| **Subtotal** | | | | | | | **7 h** |

### HU 41 · backlog 37

Como estudiante, quiero ver la lista de mis envíos de un problema con su fecha, resultado y puntaje, para saber cuántas veces lo he intentado.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU41-T1 | Crear la consulta de los envíos de un estudiante en un problema, del más reciente al más antiguo. | user-api | Juan Diego | HU36-T1 | 41.1, 41.3 | La consulta devuelve los envíos del estudiante en un problema, del más reciente al más antiguo. | 1 |
| HU41-T2 | Construir la tabla del historial con fecha, resultado y puntaje de cada intento. | user-web | Carlos | HU41-T1 | 41.1, 41.2, 41.3 | La tabla muestra fecha, resultado y puntaje de cada intento. | 2 |
| HU41-T3 | Paginar el historial cuando hay muchos intentos. | user-web | Carlos | HU41-T2 | 41.4 | Con muchos intentos el historial se pagina. | 1 |
| HU41-T4 | Prueba: un estudiante solo puede ver sus propios envíos. | user-api | Gael (+ Juan Diego) | HU41-T1 | 41.5 | La prueba pasa: un estudiante recibe 403 al pedir los envíos de otro. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 42 · backlog 38

Como estudiante, quiero abrir un envío anterior y ver el código exacto que mandé, para recuperar una solución que funcionaba mejor.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU42-T1 | Guardar el código completo de cada envío en APP_ENVIO (si todavía no se guarda completo). | user-api | Juan Diego | HU36-T1 | 42.1 | Cada envío guarda el código completo. | 1 |
| HU42-T2 | Crear el endpoint de detalle de un envío. | user-api | Juan Diego | HU42-T1 | 42.2 | El endpoint devuelve el detalle de un envío del propio estudiante. | 1 |
| HU42-T3 | Mostrar el código del envío en el editor, en modo solo lectura. | user-web | Carlos | HU42-T2, HU05-T1 | 42.2, 42.3 | El código del envío se abre en el editor en modo solo lectura. | 1 |
| HU42-T4 | Agregar el botón para copiar ese código al editor actual. | user-web | Carlos | HU42-T3 | 42.4 | El botón copia ese código al editor actual. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 43 · backlog 39

Como estudiante, quiero ver un resumen de mi progreso en cada curso, con los problemas resueltos, intentados y pendientes.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU43-T1 | Calcular el avance del estudiante en cada curso: problemas resueltos, intentados y pendientes. | user-api | Juan Diego | HU36-T1, HU30-T2 | 43.1, 43.2, 43.3 | El avance de un curso devuelve resueltos, intentados y pendientes. | 2 |
| HU43-T2 | Construir la tarjeta de progreso con una barra de avance. | user-web | Carlos | HU43-T1 | 43.4 | La tarjeta muestra el avance con una barra. | 2 |
| HU43-T3 | Guardar en caché el resultado del cálculo para no recalcularlo en cada visita, e invalidar el caché cuando el estudiante hace un envío nuevo. | user-api | Juan Diego | HU43-T1, HU36-T2 | 43.1 | El resultado se guarda en caché y se invalida cuando el estudiante hace un envío nuevo. | 1 |
| HU43-T4 | Definir qué problemas cuentan para el avance (solo los aprobados y publicados) y probar el cálculo con un curso vacío y con uno terminado. | user-api | Juan Diego | HU43-T1 | 43.5, 43.6 | Solo cuentan los problemas aprobados y publicados; el cálculo funciona con un curso vacío y con uno terminado. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 44 · backlog 40

Como docente, quiero revisar el historial de envíos de mis estudiantes en un problema, para detectar en qué tema está trabando el grupo.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU44-T1 | Crear la consulta que resume los envíos de todo el curso agrupados por problema (cuántos intentos y cuántos correctos). | user-api | Juan Diego | HU36-T1 | 44.1, 44.2 | La consulta agrupa los envíos del curso por problema con intentos y correctos. | 2 |
| HU44-T2 | Construir la vista con el porcentaje de acierto de cada problema. | admin-web | Carlos | HU44-T1 | 44.2 | La vista muestra el porcentaje de acierto de cada problema. | 2 |
| HU44-T3 | Ordenar los problemas por porcentaje de acierto, para ver primero los más difíciles. | admin-web | Carlos | HU44-T2 | 44.3 | Los problemas se ordenan por porcentaje de acierto. | 1 |
| HU44-T4 | Prueba: el docente solo ve los datos de sus propios cursos. | admin-api + user-api | Gael (+ Juan Diego) | HU44-T1 | 44.1, 44.4 | La prueba pasa: un docente solo ve datos de sus propios cursos. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 61 · backlog 57

Como docente, quiero crear una evaluación con fecha de inicio, fecha de fin y límite de tiempo, para programar un examen o una práctica calificada.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU61-T1 | Crear el modelo Evaluacion (examen de un curso con ventana de tiempo): curso, fecha de inicio, fecha de fin y límite de tiempo. Generar la migración en admin_schema. | admin-api | Juan Diego | HU09-T1 | 61.1, 61.2, 61.3 | La migración crea ADMIN_EVALUACION con curso, fecha de inicio, fecha de fin y límite de tiempo. | 1 |
| HU61-T2 | Crear el endpoint de creación de evaluaciones. | admin-api | Juan Diego | HU61-T1, HU02-T2 | 61.1, 61.2, 61.3 | POST crea la evaluación de un curso propio (201). | 1 |
| HU61-T3 | Construir el formulario con fecha de inicio, fecha de fin y límite de tiempo (en minutos). | admin-web | Carlos | HU61-T2, HU01-T3 | 61.1, 61.2, 61.3 | El formulario envía fecha de inicio, fecha de fin y límite en minutos. | 2 |
| HU61-T4 | Validar que la fecha de fin sea posterior a la de inicio, y probarlo. | admin-api | Juan Diego | HU61-T2 | 61.4 | Una fecha de fin anterior a la de inicio devuelve 400 con mensaje, y hay una prueba que lo comprueba. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 62 · backlog 58

Como docente, quiero seleccionar qué problemas de mis cursos formarán parte de la evaluación.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU62-T1 | Crear la relación entre Evaluacion y Problema (qué problemas contiene cada evaluación) y su migración. | admin-api | Juan Diego | HU61-T1, HU13-T1 | 62.1, 62.2 | La relación evaluación-problema existe con su migración. | 1 |
| HU62-T2 | Construir el selector múltiple para elegir problemas del banco del curso. | admin-web | Carlos | HU62-T1 | 62.1, 62.2 | El selector múltiple elige problemas del banco del curso. | 2 |
| HU62-T3 | Validar que la evaluación tenga al menos un problema, y probarlo. | admin-api | Juan Diego | HU62-T1 | 62.3 | Una evaluación sin problemas devuelve 400, y hay una prueba que lo comprueba. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 63 · backlog 59

Como docente, quiero definir el valor en puntos de cada problema dentro de la evaluación, para dar mayor peso a los ejercicios más complejos.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU63-T1 | Agregar el campo de puntaje a la relación evaluación-problema: cuántos puntos vale cada problema dentro de esa evaluación. | admin-api | Juan Diego | HU62-T1 | 63.1 | Cada problema de la evaluación tiene su puntaje en la relación. | 1 |
| HU63-T2 | Permitir editar los puntos de cada problema desde el selector. | admin-web | Carlos | HU63-T1, HU62-T2 | 63.1, 63.2 | Los puntos de cada problema se editan desde el selector. | 1 |
| HU63-T3 | Mostrar el total de puntos de la evaluación. | admin-web | Carlos | HU63-T2 | 63.3, 63.4 | El total mostrado es igual a la suma de los puntos asignados. | 1 |
| HU63-T4 | Prueba de la suma de puntos y de su validación (por ejemplo, que no sean negativos). | admin-api | Gael (+ Juan Diego) | HU63-T1 | 63.4 | Pasan las pruebas de la suma y de que no haya puntos negativos. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 64 · backlog 60

Como docente, quiero que la evaluación se active y se cierre automáticamente según el horario programado, para no controlar el acceso manualmente.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU64-T1 | Calcular el estado de la evaluación (programada, activa o cerrada) comparando sus fechas con la fecha y hora actual. | admin-api | Juan Diego | HU61-T1 | 64.1, 64.2, 64.3 | El estado programada, activa o cerrada sale de comparar las fechas con la hora actual. | 2 |
| HU64-T2 | Bloquear el acceso del estudiante a la evaluación fuera de la ventana programada. | user-api | Juan Diego | HU64-T1, HU22-T1 | 64.1, 64.2, 64.3 | Antes o después de la ventana el estudiante no entra; durante la ventana sí. | 2 |
| HU64-T3 | Cerrar automáticamente la evaluación del estudiante cuando vence el límite de tiempo. | user-api | Juan Diego | HU64-T2, HU36-T2 | 64.4 | Al vencer el límite de tiempo el intento se cierra solo. | 1 |
| HU64-T4 | Pruebas: intentar entrar antes, durante y después de la ventana. | user-api | Gael (+ Juan Diego) | HU64-T2, HU64-T3 | 64.1, 64.2, 64.3, 64.4 | Pasan las pruebas de entrar antes, durante y después de la ventana. | 1 |
| **Subtotal** | | | | | | | **6 h** |

---

## Sprint 4   16 – 27 nov 2026
**Hito: App móvil completa estilo Consoly. Cierra el MVP**
16 historias · 66 tareas · 90 h

### HU 45 · backlog 41

Como estudiante, quiero ver un fragmento de código y escribir lo que imprime en consola, para entrenar la lectura de código sin necesidad de un teclado.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU45-T1 | Implementar la corrección de ejercicios prediccion_salida por comparación directa: se compara el texto que escribió el estudiante con la respuesta esperada, normalizando espacios y mayúsculas. No usa sandbox ni IA. | user-api | Juan Diego | HU22-T1, HU24-T1 | 45.3, 45.4 | La respuesta se compara normalizada con la esperada y el resultado es correcto o incorrecto, sin sandbox ni IA. | 2 |
| HU45-T2 | Construir en la app Android la pantalla del ejercicio «predecir la salida»: muestra un fragmento de código y un campo para escribir lo que imprimiría. | mobile | Gael | HU22-T4, HU45-T1 | 45.1, 45.2 | La pantalla muestra el fragmento de código y un campo para escribir la salida. | 2 |
| HU45-T3 | Controlar cuántos intentos permite cada ejercicio y mostrar al estudiante los que le quedan. Cada intento se guarda en APP_ENVIO con plataforma movil, de modo que aparezca en el avance de la web (HU24). | mobile + user-api | Gael (+ Juan Diego) | HU45-T1, HU24-T1 | 45.5, 24.4 | Se muestran los intentos restantes, cada intento se guarda en APP_ENVIO con plataforma movil y aparece en el avance de la web (cierra CA-24.4). | 1 |
| HU45-T4 | Pruebas: respuesta correcta, incorrecta y correcta pero con espacios de más. | user-api | Gael (+ Juan Diego) | HU45-T1 | 45.3, 45.4 | Pasan las pruebas: respuesta correcta, incorrecta y correcta con espacios de más. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 46 · backlog 42

Como estudiante, quiero responder ejercicios de opción múltiple tocando una alternativa, para practicar con una sola mano mientras me traslado.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU46-T1 | Crear el modelo de alternativas de respuesta (ADMIN_OPCION_RESPUESTA): problema, texto y es_correcta. Generar la migración en admin_schema. | admin-api | Juan Diego | HU13-T1 | 46.1 | La migración crea ADMIN_OPCION_RESPUESTA en admin_schema con problema, texto y es_correcta. | 1 |
| HU46-T2 | Entregar las alternativas al celular sin indicar cuál es la correcta y mezclando su orden en cada intento. | user-api | Juan Diego | HU46-T1, HU22-T1 | 46.3, 46.4 | Las alternativas llegan mezcladas en cada intento y sin indicar cuál es la correcta. | 2 |
| HU46-T3 | Construir la pantalla de opción múltiple con botones grandes (al menos 48 dp de alto), cómodos para el pulgar. | mobile | Gael | HU46-T2 | 46.1, 46.2, 46.5 | La pantalla muestra las alternativas como botones de al menos 48 dp de alto. | 2 |
| HU46-T4 | Prueba: la alternativa correcta no viaja al celular antes de que el estudiante responda. | user-api | Gael (+ Juan Diego) | HU46-T2 | 46.4 | La prueba pasa: la alternativa correcta no viaja al celular antes de responder. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 47 · backlog 43

Como estudiante, quiero completar el espacio en blanco de un fragmento de código, para reforzar la sintaxis que más se me olvida.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU47-T1 | Definir cómo se marca el espacio en blanco dentro del fragmento de código de un problema completar_espacio (por ejemplo con un marcador ___) y cómo se guarda la respuesta esperada. | admin-api | Juan Diego | HU13-T1 | 47.1 | El marcador ___ y la forma de guardar la respuesta esperada quedan documentados y funcionan en admin-api. | 1 |
| HU47-T2 | Implementar la corrección con normalización simple: ignorar espacios sobrantes y diferencias de mayúsculas. | user-api | Juan Diego | HU47-T1 | 47.3, 47.4 | La corrección ignora espacios sobrantes y diferencias de mayúsculas. | 2 |
| HU47-T3 | Construir la pantalla con el campo de texto dentro del fragmento de código. | mobile | Gael | HU47-T1, HU22-T4 | 47.1, 47.2 | La pantalla muestra el fragmento con un campo de texto en el espacio en blanco. | 1 |
| HU47-T4 | Prueba con respuestas escritas de forma distinta pero igualmente válidas. | user-api | Gael (+ Juan Diego) | HU47-T2 | 47.3, 47.4 | La prueba acepta respuestas escritas de forma distinta pero equivalentes. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 48 · backlog 44

Como estudiante, quiero que la corrección sea inmediata al responder, para aprovechar los ratos cortos sin esperar a que se ejecute nada.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU48-T1 | Verificar que ningún ejercicio móvil (prediccion_salida, opcion_multiple, completar_espacio) llame al sandbox ni a ai-service al evaluarse: se corrige directamente en user-api. | user-api | Gael (+ Juan Diego) | HU45-T1, HU46-T2, HU47-T2 | 48.1 | Ningún ejercicio móvil llama al sandbox ni a ai-service al evaluarse, verificado en el código y en los logs. | 1 |
| HU48-T2 | Mostrar de inmediato si el estudiante acertó y, si falló, cuál era la respuesta correcta. | mobile | Gael | HU45-T2 | 48.2, 48.3 | Tras responder se ve de inmediato si acertó y, si falló, cuál era la respuesta correcta. | 1 |
| HU48-T3 | Manejar la pérdida de señal: guardar la respuesta en el teléfono y reintentar el envío cuando vuelva la conexión. | mobile | Gael | HU48-T2 | 48.4, 48.5 | Sin conexión la respuesta queda guardada en el teléfono y se reenvía al volver la señal. | 2 |
| HU48-T4 | Prueba sin conexión: responder en modo avión, recuperar la señal y comprobar que la respuesta se envía una sola vez. | mobile | Gael | HU48-T3 | 48.4, 48.5 | En modo avión se responde, se recupera la señal y la respuesta se envía una sola vez. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 49 · backlog 45

Como estudiante, quiero mantener una racha de días consecutivos practicando, para darme una razón de volver a la aplicación todos los días.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU49-T1 | Crear la migración Flyway del modelo Racha (APP_RACHA): estudiante, lenguaje, racha actual, racha máxima y fecha de última actividad. La racha es la cantidad de días seguidos en que el estudiante practicó. | user-api | Juan Diego | HU22-T1 | 49.1 | La migración crea APP_RACHA con estudiante, lenguaje, racha actual, racha máxima y última actividad. | 1 |
| HU49-T2 | Implementar el conteo de días seguidos considerando la hora local del estudiante, no la del servidor. | user-api | Juan Diego | HU49-T1 | 49.1, 49.2 | El conteo de días seguidos usa la hora local del estudiante, no la del servidor. | 2 |
| HU49-T3 | Mostrar la racha en la pantalla principal de la app. | mobile | Gael | HU49-T2 | 49.3 | La racha actual aparece en la pantalla principal. | 2 |
| HU49-T4 | Definir qué actividad cuenta para mantener la racha del día (resolver al menos un ejercicio ese día) y probar que la racha se corta al saltarse un día. | user-api | Juan Diego | HU49-T2 | 49.1, 49.4 | Resolver un ejercicio cuenta como actividad del día y la prueba comprueba que saltarse un día corta la racha. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 50 · backlog 46

Como estudiante, quiero subir de cinturón en un lenguaje conforme acumulo puntos, para tener una medida visible de mi avance.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU50-T1 | Crear el modelo Cinturon (APP_CINTURON): estudiante, lenguaje, nivel (de blanco a negro) y puntos acumulados, con la cantidad de puntos que exige cada nivel. | user-api | Juan Diego | HU22-T1 | 50.1, 50.3 | La tabla APP_CINTURON existe con los puntos que exige cada nivel. | 1 |
| HU50-T2 | Sumar puntos al cinturón del lenguaje correspondiente cada vez que el estudiante resuelve un ejercicio. | user-api | Juan Diego | HU50-T1 | 50.1 | Resolver un ejercicio suma puntos al cinturón de su lenguaje. | 1 |
| HU50-T3 | Diseñar la imagen que representa cada cinturón en la app. | mobile | Gael | HU50-T1 | 50.4 | Hay una imagen por cada nivel de cinturón en la app. | 2 |
| HU50-T4 | Definir cuántos puntos otorga cada tipo de ejercicio y probar la subida de nivel. | user-api | Juan Diego | HU50-T2 | 50.2, 50.3 | docs/puntos-cinturon.md define los puntos por tipo de ejercicio y una prueba comprueba la subida de nivel. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 51 · backlog 47

Como estudiante, quiero ver cuántos puntos me faltan para el siguiente cinturón, para saber cuánto esfuerzo me queda por delante.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU51-T1 | Calcular, en el perfil del estudiante, cuántos puntos le faltan para el siguiente cinturón. | user-api | Juan Diego | HU50-T1 | 51.1, 51.2 | El perfil devuelve cuántos puntos faltan para el siguiente cinturón. | 1 |
| HU51-T2 | Construir la barra de avance hacia el siguiente cinturón. | mobile | Gael | HU51-T1 | 51.3 | La barra de avance hacia el siguiente cinturón se muestra en el perfil. | 2 |
| HU51-T3 | Mostrar un mensaje especial a quien ya tiene el cinturón más alto (negro), en lugar de la barra. | mobile | Gael | HU51-T2 | 51.4 | Con el cinturón negro se muestra el mensaje especial en lugar de la barra. | 0.5 |
| HU51-T4 | Probar el cálculo en todos los niveles de cinturón. | user-api | Gael (+ Juan Diego) | HU51-T1 | 51.1, 51.4 | La prueba cubre el cálculo en todos los niveles de cinturón. | 0.5 |
| **Subtotal** | | | | | | | **4 h** |

### HU 52 · backlog 48

Como estudiante, quiero recibir un aviso cuando esté por perder mi racha, para no romperla por olvido.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU52-T1 | Configurar el servicio gratuito de notificaciones push (por ejemplo Firebase Cloud Messaging) en la app y guardar en user-api el token del dispositivo de cada estudiante. | mobile + user-api | Gael (+ Juan Diego) | HU49-T1, HU22-T4 | 52.2 | El token del dispositivo se guarda en user-api y llega una notificación de prueba. | 2 |
| HU52-T2 | Programar una tarea diaria que detecte a los estudiantes que están por perder su racha (no han practicado hoy) y les envíe un aviso. | user-api | Juan Diego | HU49-T2, HU52-T1 | 52.1, 52.2 | La tarea diaria envía el aviso a quien tiene racha activa y aún no practicó ese día. | 2 |
| HU52-T3 | Permitir desactivar los avisos y elegir la hora en que se envían. | mobile | Gael | HU52-T1 | 52.3, 52.4 | El estudiante puede desactivar el aviso y elegir la hora. | 1 |
| HU52-T4 | Prueba: el aviso llega a un celular real. | mobile | Gael | HU52-T2 | 52.2 | El aviso llega a un celular real. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 53 · backlog 49

Como estudiante, quiero iniciar una sesión de práctica continua sin tener que elegir un problema, para ponerme a entrenar de inmediato.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU53-T1 | Crear la migración Flyway del modelo SesionDojo: una sesión de práctica libre del Dojo (el modo de ejercicios cortos de la app) con sus ejercicios entregados y sus aciertos. | user-api | Juan Diego | HU22-T1 | 53.1 | La migración crea SesionDojo con sus ejercicios entregados y sus aciertos. | 1 |
| HU53-T2 | Implementar la entrega del siguiente ejercicio de la sesión, evitando repetir los ya entregados. | user-api | Juan Diego | HU53-T1 | 53.2, 53.3 | El siguiente ejercicio de la sesión no repite ninguno de los ya entregados. | 2 |
| HU53-T3 | Construir la pantalla del Dojo con un contador de aciertos. | mobile | Gael | HU53-T2, HU22-T4 | 53.1, 53.4 | La pantalla del Dojo inicia la sesión y muestra el contador de aciertos. | 2 |
| HU53-T4 | Prueba: dentro de una misma sesión no se repite ningún ejercicio. | user-api | Gael (+ Juan Diego) | HU53-T2 | 53.3 | La prueba pasa: dentro de una misma sesión no se repite ningún ejercicio. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 54 · backlog 50

Como estudiante, quiero que la dificultad suba o baje según mis aciertos, para practicar siempre en un nivel que me exija sin frustrarme.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU54-T1 | Documentar en docs/ la regla que sube o baja la dificultad según los aciertos (dos aciertos seguidos suben un nivel y dos errores seguidos lo bajan). | docs | Juan Diego | — | 54.1 | docs/ describe la regla: dos aciertos seguidos suben un nivel y dos errores seguidos lo bajan. | 1 |
| HU54-T2 | Implementar la elección del siguiente ejercicio según esa regla. | user-api | Juan Diego | HU54-T1, HU53-T2 | 54.1, 54.2 | El siguiente ejercicio respeta el nivel resultante de la regla. | 2 |
| HU54-T3 | Guardar el nivel de dificultad alcanzado al terminar la sesión. | user-api | Juan Diego | HU54-T2 | 54.3 | Al terminar la sesión se guarda el nivel alcanzado. | 1 |
| HU54-T4 | Definir el nivel de dificultad inicial de un estudiante nuevo. | user-api | Juan Diego | HU54-T3 | 54.4 | Un estudiante nuevo empieza en el nivel inicial documentado. | 1 |
| HU54-T5 | Prueba simulando rachas de aciertos y de errores. | user-api | Gael (+ Juan Diego) | HU54-T2 | 54.5 | Pasan las pruebas con rachas de aciertos y de errores. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 55 · backlog 51

Como estudiante, quiero salir del Dojo en cualquier momento y conservar lo avanzado, para poder practicar en sesiones cortas.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU55-T1 | Guardar el estado de la sesión del Dojo (ejercicio actual y aciertos) al salir de la pantalla. | mobile | Gael | HU53-T3 | 55.1, 55.5 | Al salir de la pantalla se guardan el ejercicio actual y los aciertos. | 1 |
| HU55-T2 | Implementar la reanudación de una sesión interrumpida: al volver al Dojo se ofrece continuar donde quedó. | mobile + user-api | Gael (+ Juan Diego) | HU55-T1 | 55.2 | Al volver al Dojo se ofrece continuar donde quedó. | 1.5 |
| HU55-T3 | Agregar el botón «Terminar sesión» con confirmación. | mobile | Gael | HU55-T1 | 55.3, 55.4 | El botón Terminar sesión pide confirmación y cierra la sesión definitivamente. | 0.5 |
| HU55-T4 | Prueba: cerrar la aplicación a mitad de sesión y volver a abrirla. | mobile | Gael | HU55-T2 | 55.5 | La prueba cierra la aplicación a mitad de sesión, la vuelve a abrir y el estado se conserva. | 1 |
| **Subtotal** | | | | | | | **4 h** |

### HU 56 · backlog 52

Como estudiante, quiero ver al terminar la sesión un resumen de mis aciertos y los temas en los que más fallé, para saber qué repasar.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU56-T1 | Calcular los temas en los que el estudiante más falló durante la sesión. | user-api | Juan Diego | HU53-T2 | 56.2 | La consulta devuelve los temas con más errores de la sesión (el origen del tema está pendiente de definir, ver registro de decisiones). | 1.5 |
| HU56-T2 | Construir la pantalla de resumen de la sesión: aciertos y temas a repasar. | mobile | Gael | HU56-T1 | 56.1, 56.2, 56.4, 56.5 | La pantalla de resumen muestra los aciertos y los temas a repasar. | 2 |
| HU56-T3 | Sumar los puntos de la sesión al cinturón del lenguaje correspondiente. | user-api | Juan Diego | HU50-T2, HU53-T2 | 56.3 | Los puntos de la sesión se suman al cinturón del lenguaje. | 0.5 |
| HU56-T4 | Pruebas: una sesión perfecta y una sesión sin aciertos. | user-api | Gael (+ Juan Diego) | HU56-T1, HU56-T3 | 56.4, 56.5 | Pasan las pruebas de una sesión perfecta y de una sesión sin aciertos. | 1 |
| **Subtotal** | | | | | | | **5 h** |

### HU 57 · backlog 53

Como estudiante, quiero retar a un compañero de mi curso a una partida de ejercicios cortos, para practicar compitiendo en lugar de solo.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU57-T1 | Crear el modelo Partida (un reto entre dos estudiantes del mismo curso) con sus jugadores, y su migración Flyway en app_schema. | user-api | Juan Diego | HU22-T1 | 57.2 | La migración crea Partida con sus jugadores en app_schema. | 2 |
| HU57-T2 | Implementar la creación de la partida y la invitación al rival. | user-api | Juan Diego | HU57-T1, HU26-T2 | 57.1, 57.2 | Se crea la partida y se invita a un compañero del mismo curso. | 2 |
| HU57-T3 | Construir la pantalla para retar a un compañero del curso. | mobile | Gael | HU57-T2 | 57.1, 57.3 | La pantalla para retar muestra los compañeros del curso y permite invitar a uno. | 2 |
| HU57-T4 | Implementar la caducidad del reto si el rival no responde en 24 horas (configurable). | user-api | Juan Diego | HU57-T2 | 57.4 | Un reto sin respuesta caduca a las 24 horas (probado con un valor corto en las pruebas). | 1 |
| **Subtotal** | | | | | | | **7 h** |

### HU 58 · backlog 54

Como estudiante, quiero que ambos jugadores reciban la misma secuencia de ejercicios, para que la comparación del resultado sea justa.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU58-T1 | Generar y guardar la secuencia de ejercicios de la partida al crearla, para que ambos jugadores reciban los mismos. | user-api | Juan Diego | HU57-T1 | 58.1 | La secuencia de ejercicios se guarda al crear la partida. | 2 |
| HU58-T2 | Entregar el mismo ejercicio a ambos jugadores en cada turno. | user-api | Juan Diego | HU58-T1 | 58.2, 58.3 | Ambos jugadores reciben el mismo ejercicio en cada turno y en el mismo orden. | 2 |
| HU58-T3 | Impedir que un jugador se adelante en la secuencia. | user-api | Juan Diego | HU58-T2 | 58.4 | Un jugador no puede pedir el ejercicio siguiente antes de que le corresponda. | 1 |
| HU58-T4 | Prueba: una partida completa jugada con dos cuentas. | user-api + mobile | Gael (+ Juan Diego) | HU58-T3 | 58.1, 58.2, 58.3, 58.4 | Una partida completa se juega con dos cuentas. | 1 |
| **Subtotal** | | | | | | | **6 h** |

### HU 59 · backlog 55

Como estudiante, quiero ver el marcador en vivo durante la partida, para saber si voy ganando y mantener la tensión del reto.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU59-T1 | Elegir el mecanismo de tiempo real (WebSocket, Server-Sent Events o consulta periódica) y registrar la decisión en un nuevo ADR. | docs | Juan Diego | — | 59.2, 59.3, 59.4 | Un ADR nuevo registra el mecanismo elegido (WebSocket, SSE o consulta periódica) y la regla de desconexión. | 1 |
| HU59-T2 | Implementar en el servidor la actualización del marcador de la partida. | user-api | Juan Diego | HU59-T1, HU58-T2 | 59.2 | El servidor actualiza el marcador con cada respuesta. | 2 |
| HU59-T3 | Mostrar el marcador en vivo en la pantalla de la partida. | mobile | Gael | HU59-T2 | 59.1, 59.2 | La pantalla de la partida muestra el marcador en vivo. | 2 |
| HU59-T4 | Manejar la desconexión de un jugador y el caso en que no vuelve. Regla propuesta: el jugador tiene 30 segundos para volver; si no vuelve, pierde la partida (se fija en el ADR de HU59 T1). | user-api + mobile | Juan Diego (+ Gael) | HU59-T2 | 59.3, 59.4 | Un jugador desconectado tiene 30 segundos para volver; si no vuelve, pierde la partida (regla del ADR). | 1 |
| HU59-T5 | Prueba con conexión lenta. | mobile | Gael | HU59-T3 | 59.5 | La prueba con conexión lenta (red limitada) muestra el marcador sin errores. | 1 |
| **Subtotal** | | | | | | | **7 h** |

### HU 60 · backlog 56

Como estudiante, quiero consultar el historial de mis partidas y el ranking de mi curso, para medir mi desempeño frente al de mis compañeros.

| ID | Descripción | Servicio | Resp. | Depende de | Cubre CA | Hecho cuando | Hrs. |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| HU60-T1 | Crear la consulta del historial de partidas del estudiante. | user-api | Juan Diego | HU57-T1 | 60.1 | La consulta devuelve el historial de partidas del estudiante. | 1 |
| HU60-T2 | Calcular el ranking del curso a partir de las partidas ganadas, definiendo el criterio de desempate. Devolver también la posición del propio estudiante, aunque no aparezca en la página que se muestra. | user-api | Juan Diego | HU60-T1 | 60.2, 60.3 | El ranking del curso aplica el desempate definido y devuelve también la posición del propio estudiante. | 2 |
| HU60-T3 | Construir las pantallas de historial y de ranking. | mobile | Gael | HU60-T1, HU60-T2 | 60.1, 60.2 | Las pantallas de historial y de ranking muestran los datos de user-api. | 2 |
| HU60-T4 | Prueba del ranking con empates, con estudiantes sin partidas y con un estudiante que no está inscrito en el curso (recibe un 403). | user-api | Gael (+ Juan Diego) | HU60-T2 | 60.2, 60.4 | Pasan las pruebas del ranking con empates, con estudiantes sin partidas y con un estudiante no inscrito (403). | 1 |
| **Subtotal** | | | | | | | **6 h** |

