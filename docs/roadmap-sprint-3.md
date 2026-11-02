# Roadmap — Sprint 3 · MASTER CODE

**Sprint:** 2 – 13 de noviembre de 2026 · **Hito:** calificación de punta a punta y evaluaciones; cierra el MVP web

**Estado de partida:** martes 3 de noviembre, día 2 de 10. Sprint 2 cerrado: sus 67 tareas están en `main` (verificado en el historial de Git; HU07-T1 y HU08-T1 no llevan la etiqueta de tarea en el commit, pero su código está). Además, 17 tareas de este sprint ya están hechas (sección 1).

**Plan:** 16 historias, 69 tareas, 90 h, tomadas de `docs/historias-tareas.md` (IDs, responsables y dependencias son los del documento). **Quedan 52 tareas y 67 h:** Juan Diego 30 h, Carlos 28 h y Gael 9 h. Se agregan 4 decisiones sin horas (D8 a D11, sección 6) y se señalan 8 huecos; cuatro de ellos suman unas 6 h que ninguna tarea cuenta. Las dependencias marcadas con \* en las tablas no están en `historias-tareas.md`: las agrega este roadmap y se explican en la sección 6.

---

## 1. Qué ya está hecho

Todo el Sprint 2 (HU5 a HU8, HU17 a HU20 y HU25 a HU32), verificado en `main`, y además **17 tareas del propio Sprint 3 (23 h)**, que se adelantaron con el envío oficial y la calificación (ADR-7, D7). Sobre eso se apoya este sprint:

| Ya existe | Dónde | Lo usa el Sprint 3 en |
|---|---|---|
| Sandbox detrás de `POST /execute` de `ai-service` (proveedores mock, Piston y Judge0) y el botón «Ejecutar» del editor (`EjecutorCodigo`) | ai-service, user-api, user-web | HU33 (el editor pasa a dos paneles), HU40 |
| Envío oficial `POST /api/problemas/{id}/envios`: evalúa caso por caso, puntaje = superados × 100 / total, resultado correcto, parcial o incorrecto; guarda código, puntaje y número de intento en `app_envio` (migraciones V4 y V5) | user-api | HU41, HU42, HU43, HU44 |
| `EnvioSolucion`: confirmación, botón bloqueado mientras evalúa, tabla por caso con la diferencia resaltada, puntaje y resultado | user-web | HU33-T1 (se reubica en el panel del editor), HU36-T3 |
| Casos ocultos: la respuesta al estudiante nunca lleva su entrada ni su salida, solo «pasa» o «falla», con pruebas que lo comprueban | user-api, admin-api | HU37-T1, HU39-T3 |
| Detalle del problema con el enunciado (HTML saneado con DOMPurify) y los ejemplos públicos; listado de un curso con el estado de cada problema (resuelto, intentado o sin abrir) | user-web, user-api | HU33-T2 y T3, HU43 |
| Avance por curso calculado en el navegador (`progreso.ts`, `TarjetaCurso`) y `GET /api/avance` con los envíos por problema | user-web, user-api | HU43 |
| Rutas `/internal/` de `admin-api` con `X-Internal-Key` y su cliente `AdminCliente` en `user-api` (cursos, problemas, calificación) | admin-api, user-api | HU34, HU37-T1, HU64-T2 |
| `infra/scripts/humo.py` (recorrido de punta a punta) y `datos_demo.py` (datos de demostración) | infra | HU36-T6 y la aceptación del MVP web |

### Lo adelantado del Sprint 3

| Historia | Ya hecho | Falta |
|---|---|---|
| **HU36** Enviar solución | T1, T2, T4, T5 (7 h) | T3 (el número de intento en la confirmación) y T6 (la prueba del flujo completo, que hoy cubren `humo.py` y las pruebas automáticas pero no una prueba a mano) |
| **HU37** Detalle de casos evaluados | T2, T3, T4, T5 (6 h) | T1 (guardar el resultado de cada caso; hoy se devuelve en la respuesta pero no se guarda) |
| **HU38** Calificación parcial | T1 a T4 (4 h) | Nada: la historia está completa |
| **HU39** Ocultamiento de datos de prueba | T1, T2, T4 (4 h) | T3 (la auditoría, que conviene hacer al final: hueco 4) |
| **HU40** Errores comprensibles | T3 (1 h) | T1 (falta «memoria excedida»), T2, T4 y T5 (falta el error de sintaxis) |
| **HU42** Recuperar una solución anterior | T1 (1 h) | T2, T3 y T4 |

Las otras diez historias (HU33 a HU35, HU41, HU43, HU44 y HU61 a HU64) empiezan de cero. Cuentan como hechas solo las tareas que cumplen su «Hecho cuando» completo; las cuatro que quedaron a medias (HU36-T3, HU36-T6, HU40-T1 y HU40-T5) cuentan enteras en lo que queda. Evidencia: ADR-7 (D6 y D7), `docs/registro-cambios.md`, las migraciones V4 y V5, `EnviosController`, `Calificacion`, `EnvioSolucion` y sus pruebas (`EnviosApiTest`, `CalificacionTest`, `EnvioSolucion.test.tsx`).

## 2. La idea del roadmap en cuatro líneas

1. **Quedan 67 h, no 90.** El núcleo de HU36 a HU39 se adelantó y ya está en `main`. Falta el editor completo (HU33 a HU35), el historial (HU41 y HU42), el progreso y la vista del docente (HU43 y HU44), el resto de HU37 y HU40, y las cuatro historias de evaluaciones (HU61 a HU64). El equipo tiene 81 h de capacidad hasta el viernes 13 (9 días × 3 h × 3 personas): alcanza en total, pero no por persona.
2. **Una cadena larga y varias cortas.** Evaluaciones es la larga: HU61-T1 → HU62-T1 → HU63-T1 → HU63-T2 → HU63-T3, y HU61-T1 → HU64-T1 → HU64-T2 → HU64-T3 → HU64-T4, hasta la ola 5. HU61-T1 desbloquea 14 tareas y es de Juan Diego: **empieza hoy**. El editor (HU33 a HU35), el historial (HU41 y HU42), el progreso (HU43) y la vista del docente (HU44) son cadenas de dos a cuatro olas que no se esperan entre sí.
3. **Gael tiene 9 h y todas son pruebas o auditoría.** Casi todas esperan a Juan Diego: la primera llega en la ola 2 y la última en la ola 5. Para que no quede parado conviene pasarle parte de lo que hoy tiene Juan Diego (sección 6) y que escriba desde ya los casos de prueba con los criterios de aceptación.
4. **Lo que falta en el plan está en las costuras entre servicios.** Varias tareas necesitan un campo, una ruta o una tabla que ninguna tarea construye (huecos 1 a 6): `codigo_base` hasta el estudiante, el id de cada caso, la ruta del docente hacia `user-api`, la ventana de la evaluación y la hora en que empezó el intento. Suman unas 6 h, todas de Juan Diego, que ya es quien más tiene.

Reglas que no cambian: se trabaja en la rama personal, solo se lleva a `main` lo que ya funciona allí, por Pull Request con otro revisor; commit `carpeta: qué hace (HU# T#)`; control de entendimiento antes del commit.

---

## 3. Las olas: el orden global, mezclando a los tres

Una **ola** agrupa las tareas que pueden empezar cuando terminaron las de las que dependen (las 17 ya hechas cuentan como terminadas). Se lee de arriba hacia abajo; dentro de una celda van de la que más desbloquea a la que menos. Una tarea solo espera a sus dependencias, no a que termine toda su ola.

| Ola | Juan Diego | Carlos | Gael |
|---|---|---|---|
| **Ola 0<br>Decisiones** | **D8** qué manda al abrir el editor<br>**D9** dónde se calcula el avance y su caché<br>**D10** cómo llega el docente a los envíos<br>**D11** cómo se cierra un intento por tiempo | — | — |
| **Ola 1** | **HU61-T1** Modelo Evaluacion (ADMIN_EVALUACION) y su migración · 1 h<br>**HU41-T1** Consulta de los envíos del estudiante en un problema, del más reciente al más antiguo · 1 h<br>**HU35-T1** Migración Flyway de APP_BORRADOR (un borrador por estudiante y problema) · 1 h<br>**HU34-T1** Agregar el campo `codigo_base` a Problema y su migración · 1 h<br>**HU40-T1** Mensajes en español para tiempo agotado, memoria excedida y error de compilación; falta el de memoria · 2 h<br>**HU42-T2** Endpoint de detalle de un envío del propio estudiante · 1 h<br>**HU43-T1** Calcular el avance por curso: resueltos, intentados y pendientes · 2 h<br>**HU44-T1** Consulta de los envíos del curso agrupados por problema (intentos y correctos) · 2 h<br>**HU37-T1** Guardar una fila por cada caso evaluado en cada envío (APP_RESULTADO_CASO_PRUEBA) · 2 h<br>*Total: 13 h* | **HU33-T1** Layout de dos paneles redimensionables: enunciado a la izquierda, editor a la derecha · 2 h<br>*Total: 2 h* | — |
| **Ola 2** | **HU62-T1** Relación Evaluacion–Problema y su migración · 1 h<br>**HU35-T2** Endpoint que guarda y recupera el borrador del estudiante · 2 h<br>**HU64-T1** Calcular el estado de la evaluación (programada, activa o cerrada) · 2 h<br>**HU61-T2** Endpoint de creación de evaluaciones · 1 h<br>**HU40-T2** Indicar la línea del error de sintaxis cuando el motor la informa (con Carlos) · 2 h<br>**HU40-T4** Guardar el mensaje de error junto al envío · 1 h<br>**HU43-T3** Caché del avance, invalidada con cada envío nuevo · 1 h<br>**HU43-T4** Definir qué problemas cuentan (aprobados y publicados) y probar curso vacío y terminado · 1 h<br>*Total: 11 h* | **HU33-T4** Recordar la posición del divisor entre los paneles · 1 h<br>**HU34-T3** Cargar el código base en el editor la primera vez que se abre el problema · 1 h<br>**HU36-T3** Mostrar el número de intento en la confirmación de envío; el botón y la confirmación ya existen · 1 h<br>**HU41-T2** Tabla del historial con fecha, resultado y puntaje · 2 h<br>**HU42-T3** Abrir el código de un envío en el editor, en solo lectura · 1 h<br>**HU44-T2** Vista con el porcentaje de acierto de cada problema · 2 h<br>**HU33-T2** Panel del enunciado con formato (títulos, listas y bloques de código); el detalle del problema ya lo muestra y se reutiliza · 2 h<br>**HU33-T3** Ejemplos de entrada y salida bajo el enunciado; el detalle del problema ya los muestra y se reutilizan · 1 h<br>**HU34-T2** Campo del código base en el formulario de problema del docente · 1 h<br>**HU43-T2** Tarjeta de progreso con barra de avance; ya existe (`TarjetaCurso`) calculada en el navegador y pasa a leer el avance de HU43-T1 · 2 h<br>*Total: 14 h* | **HU39-T3** Auditar user-api, admin-api y ai-service: ningún caso oculto sale por rutas, errores o logs; anotarlo en docs/ (con Juan Diego) · 1 h<br>**HU41-T4** Prueba: un estudiante solo ve sus propios envíos (403 con los de otro) (con Juan Diego) · 1 h<br>**HU44-T4** Prueba: el docente solo ve datos de sus propios cursos (con Juan Diego) · 1 h<br>*Total: 3 h* |
| **Ola 3** | **HU63-T1** Campo de puntaje en la relación evaluación–problema · 1 h<br>**HU64-T2** Bloquear el acceso del estudiante fuera de la ventana programada · 2 h<br>**HU61-T4** Validar que la fecha de fin sea posterior a la de inicio, con su prueba · 1 h<br>**HU62-T3** Validar que la evaluación tenga al menos un problema, con su prueba · 1 h<br>*Total: 5 h* | **HU35-T3** Guardado automático 3 s después de la última tecla (antirrebote) · 1 h<br>**HU62-T2** Selector múltiple de problemas del banco del curso · 2 h<br>**HU34-T4** Botón «Volver al código base original» con confirmación · 1 h<br>**HU41-T3** Paginar el historial cuando hay muchos intentos · 1 h<br>**HU42-T4** Botón para copiar ese código al editor actual · 1 h<br>**HU44-T3** Ordenar los problemas por porcentaje de acierto · 1 h<br>**HU61-T3** Formulario de la evaluación: inicio, fin y límite en minutos · 2 h<br>*Total: 9 h* | **HU33-T5** Probar el layout en 1366 px y en 1920 px (con Carlos) · 1 h<br>**HU36-T6** Prueba del flujo completo: escribir, enviar y ver el resultado guardado; falta hacerla a mano, `humo.py` y las pruebas automáticas ya la cubren (con Carlos y Juan Diego) · 1 h<br>**HU40-T5** Pruebas: tiempo agotado, error de sintaxis y sandbox caído; falta la del error de sintaxis (con Juan Diego) · 1 h<br>*Total: 3 h* |
| **Ola 4** | **HU64-T3** Cerrar el intento cuando vence el límite de tiempo · 1 h<br>*Total: 1 h* | **HU63-T2** Editar los puntos de cada problema desde el selector · 1 h<br>**HU35-T4** Indicador de «último guardado» con la hora · 1 h<br>*Total: 2 h* | **HU35-T5** Prueba: al cerrar y volver a abrir aparece lo último que se escribió (con Carlos y Juan Diego) · 1 h<br>**HU63-T4** Pruebas de la suma de puntos y de que no haya puntos negativos (con Juan Diego) · 1 h<br>*Total: 2 h* |
| **Ola 5** | — | **HU63-T3** Mostrar el total de puntos de la evaluación · 1 h<br>*Total: 1 h* | **HU64-T4** Pruebas: entrar antes, durante y después de la ventana (con Juan Diego) · 1 h<br>*Total: 1 h* |

La ola 1 de Juan Diego suma 13 h, más de cuatro días a 3 h por día, así que su orden dentro de la sección 4 (de lo que más desbloquea a lo que menos) importa más que la ola: lo que él entrega primero es lo que deja trabajar a Carlos y a Gael.

---

## 4. Orden personal de cada integrante

«Necesita de otros» muestra solo lo que depende de otra persona (entre paréntesis, de quién); «—» significa que puedes seguir cuando termines la fila anterior. «Libera» es cuántas tareas del sprint se desbloquean al terminarla. Las tareas de los sprints anteriores y las 17 ya hechas de este sprint, de las que dependen, ya están hechas y no se listan. Una \* marca una dependencia que agrega este roadmap (sección 6).

### Juan Diego · 30 h · 22 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera |
|---|---|---|---|---|---|
| 1 | **HU61-T1** | Modelo Evaluacion (ADMIN_EVALUACION) y su migración | 1 h | — | 14 |
| 2 | **HU41-T1** | Consulta de los envíos del estudiante en un problema, del más reciente al más antiguo | 1 h | — | 6 |
| 3 | **HU35-T1** | Migración Flyway de APP_BORRADOR (un borrador por estudiante y problema) | 1 h | — | 4 |
| 4 | **HU34-T1** | Agregar el campo `codigo_base` a Problema y su migración | 1 h | — | 3 |
| 5 | **HU40-T1** | Mensajes en español para tiempo agotado, memoria excedida y error de compilación; falta el de memoria | 2 h | — | 3 |
| 6 | **HU42-T2** | Endpoint de detalle de un envío del propio estudiante | 1 h | — | 3 |
| 7 | **HU43-T1** | Calcular el avance por curso: resueltos, intentados y pendientes | 2 h | — | 3 |
| 8 | **HU44-T1** | Consulta de los envíos del curso agrupados por problema (intentos y correctos) | 2 h | — | 3 |
| 9 | **HU37-T1** | Guardar una fila por cada caso evaluado en cada envío (APP_RESULTADO_CASO_PRUEBA) | 2 h | — | 1 |
| 10 | **HU62-T1** | Relación Evaluacion–Problema y su migración | 1 h | — | 6 |
| 11 | **HU35-T2** | Endpoint que guarda y recupera el borrador del estudiante | 2 h | — | 3 |
| 12 | **HU64-T1** | Calcular el estado de la evaluación (programada, activa o cerrada) | 2 h | — | 3 |
| 13 | **HU61-T2** | Endpoint de creación de evaluaciones | 1 h | — | 2 |
| 14 | **HU40-T2** | Indicar la línea del error de sintaxis cuando el motor la informa (con Carlos) | 2 h | — | 1 |
| 15 | **HU40-T4** | Guardar el mensaje de error junto al envío | 1 h | — | 0 |
| 16 | **HU43-T3** | Caché del avance, invalidada con cada envío nuevo | 1 h | — | 0 |
| 17 | **HU43-T4** | Definir qué problemas cuentan (aprobados y publicados) y probar curso vacío y terminado | 1 h | — | 0 |
| 18 | **HU63-T1** | Campo de puntaje en la relación evaluación–problema | 1 h | — | 3 |
| 19 | **HU64-T2** | Bloquear el acceso del estudiante fuera de la ventana programada | 2 h | — | 2 |
| 20 | **HU61-T4** | Validar que la fecha de fin sea posterior a la de inicio, con su prueba | 1 h | — | 0 |
| 21 | **HU62-T3** | Validar que la evaluación tenga al menos un problema, con su prueba | 1 h | — | 0 |
| 22 | **HU64-T3** | Cerrar el intento cuando vence el límite de tiempo | 1 h | — | 1 |

### Carlos · 28 h · 21 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera |
|---|---|---|---|---|---|
| 1 | **HU33-T1** | Layout de dos paneles redimensionables: enunciado a la izquierda, editor a la derecha | 2 h | — | 4 |
| 2 | **HU33-T4** | Recordar la posición del divisor entre los paneles | 1 h | — | 1 |
| 3 | **HU34-T3** | Cargar el código base en el editor la primera vez que se abre el problema | 1 h | HU34-T1 (JD) | 1 |
| 4 | **HU36-T3** | Mostrar el número de intento en la confirmación de envío; el botón y la confirmación ya existen | 1 h | HU41-T1 (JD) \* | 1 |
| 5 | **HU41-T2** | Tabla del historial con fecha, resultado y puntaje | 2 h | HU41-T1 (JD) | 1 |
| 6 | **HU42-T3** | Abrir el código de un envío en el editor, en solo lectura | 1 h | HU42-T2 (JD) | 1 |
| 7 | **HU44-T2** | Vista con el porcentaje de acierto de cada problema | 2 h | HU44-T1 (JD) | 1 |
| 8 | **HU33-T2** | Panel del enunciado con formato (títulos, listas y bloques de código); el detalle del problema ya lo muestra y se reutiliza | 2 h | — | 0 |
| 9 | **HU33-T3** | Ejemplos de entrada y salida bajo el enunciado; el detalle del problema ya los muestra y se reutilizan | 1 h | — | 0 |
| 10 | **HU34-T2** | Campo del código base en el formulario de problema del docente | 1 h | HU34-T1 (JD) | 0 |
| 11 | **HU43-T2** | Tarjeta de progreso con barra de avance; ya existe (`TarjetaCurso`) calculada en el navegador y pasa a leer el avance de HU43-T1 | 2 h | HU43-T1 (JD) | 0 |
| 12 | **HU35-T3** | Guardado automático 3 s después de la última tecla (antirrebote) | 1 h | HU35-T2 (JD) | 2 |
| 13 | **HU62-T2** | Selector múltiple de problemas del banco del curso | 2 h | HU62-T1 (JD) | 2 |
| 14 | **HU34-T4** | Botón «Volver al código base original» con confirmación | 1 h | — | 0 |
| 15 | **HU41-T3** | Paginar el historial cuando hay muchos intentos | 1 h | — | 0 |
| 16 | **HU42-T4** | Botón para copiar ese código al editor actual | 1 h | — | 0 |
| 17 | **HU44-T3** | Ordenar los problemas por porcentaje de acierto | 1 h | — | 0 |
| 18 | **HU61-T3** | Formulario de la evaluación: inicio, fin y límite en minutos | 2 h | HU61-T2 (JD) | 0 |
| 19 | **HU63-T2** | Editar los puntos de cada problema desde el selector | 1 h | HU63-T1 (JD) | 1 |
| 20 | **HU35-T4** | Indicador de «último guardado» con la hora | 1 h | — | 0 |
| 21 | **HU63-T3** | Mostrar el total de puntos de la evaluación | 1 h | — | 0 |

### Gael · 9 h · 9 tareas

| # | Tarea | Qué es | Horas | Necesita de otros | Libera |
|---|---|---|---|---|---|
| 1 | **HU39-T3** | Auditar user-api, admin-api y ai-service: ningún caso oculto sale por rutas, errores o logs; anotarlo en docs/ (con Juan Diego) | 1 h | HU37-T1 (JD) \*, HU41-T1 (JD) \*, HU42-T2 (JD) \* | 0 |
| 2 | **HU41-T4** | Prueba: un estudiante solo ve sus propios envíos (403 con los de otro) (con Juan Diego) | 1 h | HU41-T1 (JD) | 0 |
| 3 | **HU44-T4** | Prueba: el docente solo ve datos de sus propios cursos (con Juan Diego) | 1 h | HU44-T1 (JD) | 0 |
| 4 | **HU33-T5** | Probar el layout en 1366 px y en 1920 px (con Carlos) | 1 h | HU33-T1 (CA), HU33-T4 (CA) | 0 |
| 5 | **HU36-T6** | Prueba del flujo completo: escribir, enviar y ver el resultado guardado; falta hacerla a mano, `humo.py` y las pruebas automáticas ya la cubren (con Carlos y Juan Diego) | 1 h | HU36-T3 (CA) \* | 0 |
| 6 | **HU40-T5** | Pruebas: tiempo agotado, error de sintaxis y sandbox caído; falta la del error de sintaxis (con Juan Diego) | 1 h | HU40-T1 (JD), HU40-T2 (JD) | 0 |
| 7 | **HU35-T5** | Prueba: al cerrar y volver a abrir aparece lo último que se escribió (con Carlos y Juan Diego) | 1 h | HU35-T2 (JD), HU35-T3 (CA) | 0 |
| 8 | **HU63-T4** | Pruebas de la suma de puntos y de que no haya puntos negativos (con Juan Diego) | 1 h | HU63-T1 (JD) | 0 |
| 9 | **HU64-T4** | Pruebas: entrar antes, durante y después de la ventana (con Juan Diego) | 1 h | HU64-T2 (JD), HU64-T3 (JD) | 0 |

---

## 5. Puntos de encuentro

| Cuando esté en `main`… | …queda libre |
|---|---|
| **D8 a D11** (decisiones) | Carlos puede empezar HU34-T3, HU35-T3 y HU42-T3 sabiendo qué gana al abrir el editor (D8); Juan Diego, HU43 y HU44 sin rehacerlos (D9 y D10) y HU64 sin rehacer el cierre (D11). |
| HU34-T1 (`codigo_base`, con el hueco 1) | Carlos: HU34-T2 y HU34-T3. |
| HU41-T1 (historial de envíos) | Carlos: HU41-T2 y HU36-T3\*. Gael: HU41-T4. También entra en la auditoría HU39-T3\*. |
| HU42-T2 (detalle de un envío) | Carlos: HU42-T3 y, después, HU42-T4. También entra en la auditoría HU39-T3\*. |
| HU37-T1 (resultado por caso) | Gael: HU39-T3\* (con HU41-T1 y HU42-T2, es lo último que la auditoría espera). |
| HU43-T1 (avance por curso) | Carlos: HU43-T2. Juan Diego: HU43-T3 y HU43-T4. |
| HU44-T1 (resumen del curso por problema, con el hueco 5) | Carlos: HU44-T2 y, después, HU44-T3. Gael: HU44-T4. |
| HU35-T2 (endpoint del borrador) | Carlos: HU35-T3 y, después, HU35-T4. Gael: HU35-T5 (junto con HU35-T3). |
| HU33-T4 (divisor que se recuerda) | Gael: HU33-T5 (junto con HU33-T1). |
| HU36-T3 (número de intento) | Gael: HU36-T6. |
| HU40-T1 (mensajes de error) | Juan Diego: HU40-T2 y HU40-T4. Gael: HU40-T5 (junto con HU40-T2). |
| HU61-T2 (crear una evaluación) | Carlos: HU61-T3. Juan Diego: HU61-T4. |
| HU62-T1 (relación evaluación–problema) | Carlos: HU62-T2. Juan Diego: HU62-T3 y HU63-T1. HU63-T1 es el siguiente eslabón de la cadena de puntos. |
| HU63-T1 (puntos por problema) | Carlos: HU63-T2 (junto con HU62-T2). Gael: HU63-T4. |
| HU64-T2 y HU64-T3 (acceso y cierre por tiempo) | Gael: HU64-T4. |

**Mientras esperas lo de otro**:

- Carlos: empieza por HU33-T1 (el layout no depende de nadie) y sigue con HU33-T2, T3 y T4, que reutilizan lo que ya hace el detalle del problema. Las pantallas que esperan a Juan Diego (historial, progreso, vista del docente y formulario de evaluaciones) se dejan listas con respuestas simuladas del contrato JSON, como en el Sprint 2.
- Gael: escribe desde ya los casos de las pruebas (HU41-T4, HU44-T4, HU33-T5, HU35-T5 y las demás) a partir de los criterios de aceptación; toma el tramo 1 del reparto (sección 6) en cuanto Juan Diego termine HU40-T1, HU43-T1, HU61-T2 y HU62-T1; y prepara el recorrido de aceptación del MVP web con `datos_demo.py` y `humo.py`, que es lo que pide el hito «cierra el MVP web».
- Juan Diego: entrega primero lo que desbloquea a otros (HU61-T1, HU41-T1, HU34-T1 y HU42-T2, 4 h en total) y sigue con la siguiente fila de su lista que no dependa de una espera.

---

## 6. Decisiones abiertas, huecos y riesgos

### Decisiones que conviene cerrar el primer día

| # | Decisión | Mi recomendación |
|---|---|---|
| **D8** | ¿Qué manda al abrir el editor? HU34-T3 carga el código base «la primera vez», HU35 recupera el borrador, HU34-T4 restaura el código base y HU42-T4 copia un envío viejo al editor. Nadie dice cuál gana. | Borrador guardado, luego código base (la primera vez, cuando aún no hay borrador), luego editor vacío. «Volver al código base» y «copiar al editor» reemplazan el contenido con confirmación, y el siguiente guardado automático lo toma como borrador. El código de un envío anterior se abre en otro editor de solo lectura y nunca toca el borrador. |
| **D9** | ¿Dónde se calcula el avance y cómo se guarda en caché? Hoy el navegador cuenta resueltos e intentados del listado de problemas (`progreso.ts`); HU43-T1 lo calcula en el servidor y HU43-T3 pide caché. `user-api` no tiene biblioteca de caché ni Redis (el único Redis es el de Judge0 y no se comparte, ADR-1). | El servidor calcula (HU43-T1) y la tarjeta lo lee (HU43-T2); el listado de un curso sigue contando en el navegador porque ya tiene los datos. Caché en memoria del proceso (Spring Cache con Caffeine), con clave estudiante y curso, invalidada en cada envío y con vencimiento de pocos minutos, porque el docente puede aprobar o archivar problemas. Sin Redis. |
| **D10** | ¿Cómo llega el docente a los envíos? Están en `user-api` (HU44-T1), pero `admin-web` solo habla con `admin-api`, que es quien sabe de quién es cada curso. Hoy solo `user-api` llama a `admin-api`, nunca al revés. | `admin-api` verifica que el curso es del docente y consulta a `user-api` por una ruta `/internal/` con `X-Internal-Key` (el mecanismo de D3, en sentido contrario). `admin-web` solo ve `admin-api`; así HU44-T4 prueba una sola frontera y `user-api` no tiene que aceptar tokens de docentes. |
| **D11** | ¿Cómo se cierra un intento de evaluación por tiempo? HU64-T3 pide cerrarlo «automáticamente», pero `user-api` no tiene tareas programadas ni guarda cuándo empezó el estudiante. | Cierre perezoso: al entrar o enviar se compara la hora actual con la hora de inicio más el límite (y con el fin de la ventana) y, si venció, el intento figura cerrado. Sin tarea programada; solo hay que guardar la hora de inicio (hueco 6). |

Ya resuelto en el ADR-7 y en `docs/registro-cambios.md`: el puntaje es superados × 100 / total y solo «correcto» cuenta como resuelto (D7); el estado de un problema sale de `app_envio` (D6); el estudiante solo ve problemas aprobados (D5); de un caso oculto solo viaja «pasa» o «falla».

### Huecos en las tareas

1. **`codigo_base` no llega al estudiante con solo HU34-T1.** La tarea lo agrega al modelo y a su respuesta, pero el estudiante lo recibe por otro camino: `ProblemaEstudianteSerializer` de `admin-api` (con campos explícitos), el `ProblemaDto` y el record `ProblemaDetalle` de `user-api`, y el tipo `ProblemaDetalle` de `user-web`. Hay que agregarlo en los cuatro sitios; los tres primeros cuentan dentro de HU34-T1 (1 h → unas 2 h) y el último dentro de HU34-T3. Los problemas generados por IA no traen código base (el esquema de HU17 no lo pide): se abren vacíos hasta que el docente lo complete al revisarlos.
2. **HU37-T1 necesita el id de cada caso y la API de calificación no lo entrega.** `CasoPruebaCalificacionSerializer` devuelve solo `entrada`, `salida_esperada` y `es_publico`, y la tabla nueva guarda `caso_prueba_id`. Hay que agregar `id` en `admin-api` y en `ProblemaCalificableDto.Caso` (HU37-T1: 2 h → unas 3 h). Además, la tabla guardará la salida obtenida también de los casos ocultos: HU41-T1, HU42-T2 y cualquier detalle nuevo no deben devolverla nunca (CA-39.1 a CA-39.4). Y ninguna tarea del sprint lee esas filas; conviene que HU42-T2 las use para mostrar el resultado por caso de un envío anterior.
3. **Mensaje de error y migraciones de `user-api`.** HU40-T4 guarda el mensaje de error «para verlo en el historial», pero `app_envio` no tiene dónde guardarlo (hace falta una migración) y el CA-41.2 solo pide fecha, resultado y puntaje: HU41-T2 debe mostrar también el mensaje, o se quita «y se ve en el historial» del «Hecho cuando» de HU40-T4. Este sprint trae cuatro migraciones de `user-api` (HU35-T1, HU37-T1, HU40-T4 y la del intento de evaluación del hueco 6) y Flyway no aplica una V7 si la V8 ya corrió en esa base: se numeran en el orden en que llegan a `main`, no en el que se escribieron.
4. **HU39-T3 en la ola 1 auditaría poco.** La auditoría revisa que ninguna ruta, error ni log revele un caso oculto, pero HU37-T1, HU41-T1 y HU42-T2 agregan una tabla y rutas nuevas con envíos y salidas. Por eso va después de ellas (dependencia agregada, marcada con \*) y debe cubrir las rutas de envíos en sus dos formas, el detalle de un envío y los logs.
5. **HU44 no tiene tarea para la ruta del docente.** HU44-T1 está en `user-api`, HU44-T2 y T3 en `admin-web`, y entre ellas falta la ruta de `admin-api` que verifica el curso y consulta a `user-api`, su cliente y la ruta `/internal/` de `user-api` (D10). HU44-T4 la prueba, pero nadie la construye. Es la primera llamada de `admin-api` a `user-api`: falta la URL de `user-api` en `docker-compose.yml` y en `.env.example` (la clave `INTERNAL_API_KEY` ya llega a los dos servicios). Cuenta dentro de HU44-T1 (2 h → unas 4 h).
6. **Las evaluaciones no tienen quién las entregue a `user-api`, ni pantalla del estudiante.** HU64-T2 bloquea el acceso en `user-api`, pero la evaluación vive en `admin-api` (HU61-T1): falta una ruta `/internal/evaluaciones/{id}/` con la ventana y el límite, y su método en `AdminCliente` (1 h más en HU64-T2). HU64-T3 necesita saber cuándo empezó el estudiante y no hay tabla ni columna para eso (1 h más en HU64-T3, D11). Y ninguna historia del Sprint 3 ni del 4 cubre la pantalla donde el estudiante rinde la evaluación: HU64-T2 y T3 se prueban solo por API. Es una decisión de producto: o esa historia entra al backlog, o el MVP web cierra sin que un estudiante pueda rendir una evaluación.
7. **El número de intento no sale de HU36-T2.** HU36-T3 muestra el número *antes* de enviar y el endpoint de envío solo lo devuelve *después*. Sale de contar los envíos del estudiante en ese problema (HU41-T1); por eso HU36-T3 espera a HU41-T1 (dependencia agregada, marcada con \*) y HU41-T1 queda entre las primeras filas de Juan Diego. HU36-T6 se prueba después de HU36-T3 (también agregada).
8. **HU33-T2 y T3 ya están hechas en parte.** El detalle del problema (HU32-T2) ya muestra el enunciado con formato (`limpiarEnunciado`, HTML saneado) y los ejemplos públicos, y `EditorProblema` ya pide el problema completo. Las dos tareas deben reutilizar eso en el panel izquierdo: probablemente 1 a 2 h menos de las 3 h estimadas, que es lo que absorbe la hora que le sobra a Carlos.

Los huecos 1, 2, 5 y 6 suman unas 6 h (estimación mía); el 3, el 4, el 7 y el 8 no cambian las horas.

### Reparto y ritmo

Con 3 h por persona y día, hasta el viernes 13 hay 27 h por persona. Juan Diego tiene 30 h en el plan, y más con los huecos; Gael tiene 9 h. La simulación respeta las dependencias y el orden de la sección 4 (una tarea de otra persona cuenta como lista al día siguiente de terminada):

| Escenario | Juan Diego | Carlos | Gael | Se sale del viernes 13 |
|---|---|---|---|---|
| Capacidad hasta el viernes 13 (9 días × 3 h) | 27 h | 27 h | 27 h | — |
| **A.** El plan de `historias-tareas.md` | 30 h · lun 16 | 28 h · lun 16 | 9 h · mar 17 | 7 tareas, todas de evaluaciones |
| **B.** A más los huecos de la sección 6 (≈ 6 h, estimación) | 36 h · mié 18 | 28 h · mar 17 | 9 h · jue 19 | 11 tareas, 9 de evaluaciones |
| **C.** B con el tramo 1 del reparto (7 h) | 29 h · lun 16 | 28 h · lun 16 | 16 h · mar 17 | HU63-T3, HU64-T3 y HU64-T4 |
| **D.** B con los tramos 1 y 2 (9 h) | 27 h · vie 13 | 28 h · lun 16 | 18 h · lun 16 | HU63-T3 y HU64-T4 |

Cómo leerlo:

- Con el plan tal cual (A), Juan Diego se pasa 3 h y 7 tareas, todas de evaluaciones, se salen del sprint.
- Con los huecos (B) se pasa 9 h y se salen 11 tareas.
- Con el tramo 1 (C) se salen solo las tres últimas piezas de la cadena de evaluaciones: HU63-T3, HU64-T3 y HU64-T4.
- Con los tramos 1 y 2 (D) Juan Diego cierra el viernes 13 en su última hora, sin margen, y solo HU63-T3 y HU64-T4 pasan al lunes 16, 1 h cada una. La primera es de Carlos y se absorbe si HU33-T2 y T3 salen más rápidas (hueco 8); la segunda es de Gael y entra el viernes si Juan Diego adelanta HU64-T3, la única tarea del final de su lista que otra persona espera.

**Reparto sugerido, de Juan Diego a Gael.** Son tareas casi todas sin nadie esperándolas, y Gael está libre desde el primer día. El tramo 1 se decide hoy; el tramo 2 se revisa el jueves 5, y solo se pasa si los huecos 1, 2, 5 y 6 se confirmaron. Si se acepta, hay que cambiar la columna «Resp.» de esas tareas en `docs/historias-tareas.md`.

| Tramo | Tarea | Horas | Por qué se puede pasar a Gael |
|---|---|---|---|
| 1 | **HU61-T4** Validar que la fecha de fin sea posterior a la de inicio, con su prueba | 1 h | Es una validación con su prueba y nadie la espera. |
| 1 | **HU62-T3** Validar que la evaluación tenga al menos un problema, con su prueba | 1 h | Es una validación con su prueba y nadie la espera. |
| 1 | **HU43-T4** Definir qué problemas cuentan (aprobados y publicados) y probar curso vacío y terminado | 1 h | Define qué cuenta y lo prueba; nadie la espera. |
| 1 | **HU43-T3** Caché del avance, invalidada con cada envío nuevo | 1 h | Sigue D9; nadie la espera y es lo primero que se recorta (más abajo). |
| 1 | **HU40-T4** Guardar el mensaje de error junto al envío | 1 h | No bloquea a nadie; HU41-T2 debe mostrar el mensaje (hueco 3). |
| 1 | **HU40-T2** Indicar la línea del error de sintaxis cuando el motor la informa (con Carlos) | 2 h | Carlos ayuda en la parte web; solo bloquea a HU40-T5, que ya es de Gael. |
| 2 | **HU63-T1** Campo de puntaje en la relación evaluación–problema | 1 h | Es un campo nuevo; lo esperan HU63-T2 y HU63-T4, así que Gael debe hacerlo apenas esté HU62-T1. |
| 2 | **HU61-T2** Endpoint de creación de evaluaciones | 1 h | Es el endpoint de creación; lo espera HU61-T3 (Carlos), así que debe salir temprano. |

Con ambos tramos, y con los huecos incluidos, quedan Juan Diego con 14 tareas y 27 h, Carlos con 21 tareas y 28 h, y Gael con 17 tareas y 18 h. Las horas que aún le sobren a Gael pueden ir al recorrido de aceptación del MVP web o a adelantar el Sprint 4 (app móvil), que es su especialidad.

### Riesgos

- **La cadena de evaluaciones queda al final y descansa en una sola persona.** Va de la ola 1 a la 5, HU61-T1 desbloquea 14 tareas y sus últimas piezas (HU63-T3, HU64-T3 y HU64-T4) son lo primero que se sale del viernes 13. Conviene empezar HU61-T1 hoy y no dejar HU64 para el último día: HU64-T4 es una prueba de Gael que necesita HU64-T3 terminada.
- **Reparto desigual y horas ocultas.** Juan Diego 30 h (36 h con los huecos) para 27 h de capacidad, Carlos 28 h y Gael 9 h. Es la decisión abierta nº 4 del registro de cambios, ya señalada en el Sprint 2.
- **Una sola pantalla concentra cinco historias.** `EditorProblema.tsx` lo tocan HU33, HU34, HU35, HU36-T3 y HU42, y Gael prueba dos de ellas (HU33-T5 y HU35-T5). Si cada una agrega lo suyo en el mismo archivo habrá conflictos y regresiones; HU33-T1 debería partir la pantalla en componentes (por ejemplo `PanelEnunciado` y `PanelEditor`) para que cada historia toque el suyo.
- **Primera llamada de `admin-api` a `user-api`.** Falta la URL, el cliente y la ruta del otro lado (hueco 5). Sin eso, HU44 se queda en `user-api` y sin pantalla.
- **El MVP web cierra sin evaluación rendible** (hueco 6). El docente podrá crearla y programarla, pero ningún estudiante podrá entrar a rendirla hasta que exista esa pantalla.
- **Datos ocultos en una tabla nueva.** HU37-T1 guarda la salida obtenida de los casos ocultos; cualquier ruta del historial o del detalle que devuelva la fila completa los filtra. Las pruebas de HU41-T4 y HU42-T2 deben comprobar que nada de un caso oculto sale por ahí.

### Orden de recorte

Si el jueves 5 los huecos se confirman y el reparto no alcanza, se recorta en este orden (primero lo que menos rompe). Es decisión del Product Owner:

| Orden | Se recorta | Horas | Criterios que quedan sin cubrir |
|---|---|---|---|
| 1 | **HU43-T3** Caché del avance, invalidada con cada envío nuevo | 1 h | Ninguno: el avance se calcula en cada visita en lugar de guardarse en caché. |
| 2 | **HU41-T3** Paginar el historial cuando hay muchos intentos | 1 h | CA-41.4 (paginar el historial). |
| 3 | **HU44-T3** Ordenar los problemas por porcentaje de acierto | 1 h | CA-44.3 (ordenar los problemas por acierto). |
| 4 | **HU42-T4** Botón para copiar ese código al editor actual | 1 h | CA-42.4 (copiar el código al editor actual); el estudiante aún puede copiarlo a mano. |
| 5 | **HU64-T3** Cerrar el intento cuando vence el límite de tiempo | 1 h | CA-64.4 (cerrar el intento al vencer el límite); HU64-T4 conserva las pruebas de entrar antes, durante y después. |
| 6 | **HU63-T1 a T4** Puntos por problema: el campo, su edición, el total y sus pruebas | 4 h | CA-63.1 a CA-63.4 (puntos por problema); sin HU63 los problemas de una evaluación no tienen puntos propios. |

---

## 7. Gestión

- Crear en Trello las tarjetas del Sprint 3 con los responsables de la sección 4 (52 pendientes) y pasar a «Hecho» las 17 adelantadas; las cuatro a medias (HU36-T3, HU36-T6, HU40-T1 y HU40-T5) quedan abiertas con una nota de lo que falta.
- Confirmar D8 a D11 en el Planning y registrarlas como ADR-8 en `docs/decisiones-tecnicas-adr.md` (propuestas con las recomendaciones de la sección 6).
- Decidir hoy el tramo 1 del reparto, revisar el tramo 2 el jueves 5 y, si se aceptan, cambiar la columna «Resp.» de `docs/historias-tareas.md`.
- Decidir qué se hace con la pantalla del estudiante para rendir evaluaciones (hueco 6): entra al backlog (el Product Owner dice en qué sprint) o queda fuera del MVP.
- Poner las etiquetas de Git `sprint-1` y `sprint-2`, que el ADR-6 pide y hoy no existen (`git tag -l` está vacío): por ejemplo `git tag sprint-2 3b7e4e7`, el último commit de `main` al cerrar el Sprint 2, y lo mismo para `sprint-1` con el último commit de ese sprint. Después, `git push origin sprint-1 sprint-2`.
- Anotar los huecos 1 a 8 en `docs/registro-cambios.md`.
- Antes de cerrar el sprint, recorrer con `datos_demo.py` y `humo.py` los criterios CA-33.x a CA-64.x en el navegador (Gael), porque el hito dice que cierra el MVP web.

## 8. Cómo mantener esto al día

Al fusionar cada tarea a `main`, táchala aquí y anótala en `historial.md`. Si me pasas el historial actualizado, regenero las olas con lo que falte.
