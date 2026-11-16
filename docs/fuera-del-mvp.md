# Qué quedó fuera del MVP (C3)

Es el último sprint y no hay un Sprint 5 (ADR-9 D12): lo que no esté terminado el viernes 27 de noviembre de 2026 **se declara no entregado**, aquí y en la memoria, en lugar de entregarse a medias. Este documento dice la verdad de la entrega: qué historias están completas, cuáles a medias y cuáles no existen, con los criterios de aceptación que por eso no se cumplen.

> **Estado al 20 de noviembre de 2026**, sacado del código del repositorio, no de Trello. Es una foto: el 27 se repasa la tabla, se cambia lo que avanzó y se borra esta nota. Si Trello y este documento no coinciden, manda el código: revisa antes de decirlo en voz alta.

## Resumen

| Parte | Estado |
| :--- | :--- |
| MVP web (docente y estudiante, sprints 1 a 3: cursos, generación con IA, editor, envíos y calificación, avance, evaluaciones) | Entregado. Ver «Pendientes conocidos de la web» más abajo. |
| Corrección de ejercicios del celular (HU45 a HU48) | El servidor está completo; la app tiene las pantallas de predecir la salida y de completar el espacio, **sin enviar todavía la respuesta**. |
| Dojo (HU53, HU54, HU55) | El servidor entrega ejercicios sin repetir; **no hay pantalla** y no hay ajuste de dificultad. |
| Cinturones (HU50, HU51) | Modelo, puntos y siete imágenes; **sin pantalla** ni barra de avance. |
| Racha (HU49), resumen de sesión (HU56), duelos (HU57 a HU60) | No empezados (solo existen decisiones y documentos). |

## Historias del Sprint 4, una por una

«Hecho» quiere decir que existe y tiene pruebas. Las tareas se identifican como en `docs/historias-tareas.md`.

| Historia | Estado | Qué hay | Qué falta |
| :--- | :--- | :--- | :--- |
| **HU45** Predecir la salida | Parcial | Corrección en `user-api` (T1) y pantalla de la app (T2), con pruebas (T4). | **HU45-T3**: guardar cada intento en `app_envio` y limitar cuántos hay. La pantalla no envía la respuesta. |
| **HU46** Opción múltiple | Parcial | Modelo de alternativas (T1), entrega mezclada y sin la marca de correcta (T2), prueba de que la correcta no viaja (T4). | **HU46-T3**: la pantalla de la app con botones de 48 dp. |
| **HU47** Completar el espacio | Parcial | Marcador `___` (T1, ADR-9 D17), corrección con normalización (T2), pantalla (T3). | Enviar la respuesta desde la pantalla (con HU45-T3 y HU48-T2). |
| **HU48** Retroalimentación y respuesta sin señal | Parcial | El servidor responde si acertó y, si falló, cuál era la correcta. | **T2** mostrarlo en la app, **T3** y **T4** guardar la respuesta sin señal y reenviarla una sola vez. |
| **HU49** Racha | No empezado | — | Todas (T1 a T4): migración, cuenta de días con la hora local (H6), pantalla y qué actividad mantiene la racha. |
| **HU50** Cinturones | Parcial | Modelo con 7 niveles (T1, ADR-9 D18), suma de puntos al resolver (T2), imágenes de los siete cinturones (T3). | **HU50-T4**: `docs/puntos-cinturon.md` con los puntos definitivos por tipo (los de hoy son provisionales). |
| **HU51** Puntos para el siguiente cinturón | No empezado | — | Todas (T1 a T4): el cálculo, la barra de avance y el mensaje del cinturón negro. |
| **HU52** Avisos push | Parcial | Guardar el token del dispositivo en `user-api` (T1, D16). | Firebase en la app (H5), la tarea diaria que avisa (T2), desactivar y elegir la hora (T3), prueba en un celular real (T4). |
| **HU53** Dojo | Parcial | Sesión y entrega del siguiente ejercicio sin repetir (T1, T2, ADR-9 D19), con pruebas (T4). | **HU53-T3**: la pantalla del Dojo con el contador de aciertos. |
| **HU54** Ajuste de dificultad | Parcial | La regla documentada (T1, `docs/regla-dificultad.md`). | Elegir el ejercicio según la regla (T2), guardar el nivel (T3), nivel inicial (T4), prueba con rachas (T5). |
| **HU55** Salir y reanudar el Dojo | No empezado | — | Todas (T1 a T4). |
| **HU56** Resumen de la sesión | No empezado | El campo `tema` ya existe (H4, D13) y es lo único que HU56-T1 necesitaba. | Todas (T1 a T4). |
| **HU57** Retar a un compañero | No empezado | — | Todas (T1 a T4). |
| **HU58** Misma secuencia para los dos | No empezado | — | Todas (T1 a T4). |
| **HU59** Marcador en vivo | Parcial | La decisión de tiempo real y la regla de desconexión (T1, ADR-10 D20). | T2 a T5: marcador, pantalla, desconexión y prueba de conexión lenta. |
| **HU60** Historial y ranking | No empezado | — | Todas (T1 a T4). |

Piezas de cierre y huecos que no están en `historias-tareas.md` (los define `docs/roadmap-sprint-4.md`): hechos H1, H2, H3 (formulario del docente), H4 (campo `tema`) y C3 (este documento, el README, la guía de demo y las etiquetas de Git); pendientes H5 a H9, C1 (datos de demo del celular), C2 (recorrido de aceptación en un celular) y C4 (SonarQube y rotación de secretos).

## Criterios de aceptación que no se cumplen

Un criterio de aceptación no se cumple mientras esté pendiente alguna de las tareas que lo cubren. `docs/historias-criterios.md` trae, bajo cada criterio, las tareas que lo cubren («Tareas: HU45-T3…»): cruza esa lista con la columna «Qué falta» de la tabla de arriba. En concreto, hoy no se pueden demostrar los criterios de las historias **no empezadas** (HU49, HU51, HU55, HU56, HU57, HU58 y HU60), CA-50.4 (el cinturón con su imagen: la imagen existe, la pantalla no), CA-53.1 y CA-53.4 (pantalla del Dojo y su contador), CA-54.2 a CA-54.5 y CA-59.1 a CA-59.5, además de los que dependen de HU45-T3, HU46-T3, HU48 y HU52 (T2 a T4). Los que sí se cumplen están cubiertos por las pruebas de `user-api` (`./mvnw test`), de `admin-api` (`python manage.py test`) y de `ai-service` (`pytest`).

## Pendientes conocidos de la web

Verificados en el código el 20 de noviembre; son huecos de las historias HU19 y HU20 y de otras de los sprints anteriores:

- La **bandeja de revisión** de `admin-web` solo tiene «Descartar»: **no tiene el botón «Aprobar»** ni edita el problema, aunque la ruta de `admin-api` (`POST /api/problemas/{id}/aprobar/` y `PUT /api/problemas/{id}/`) existe. Un problema que genera la IA solo se puede publicar llamando a la API. Es lo más visible de la web y conviene resolverlo o decirlo antes de la demostración.
- Un problema con el **tema** o las alternativas que propone la IA se corrige por la API, no desde una pantalla (ADR-9 D13).
- Según la guía de demo del 29 de octubre (comprobar si sigue igual): no hay una ruta para renovar el código de invitación de un curso (vence a los 30 días) y Groq no se probó con clave real.
- Judge0 no funciona en Windows ni en WSL2: el motor de ejecución de la demo es Piston (`docs/piston.md`).

## Cómo se actualiza el día del cierre

1. Repasa la tabla de «Historias del Sprint 4»: cambia el estado de cada fila según lo que está en `main` ese día.
2. Mueve a Trello las tarjetas que no se terminaron a una lista «Fuera del MVP» (no a un sprint siguiente, que no existe).
3. Cambia la nota de la cabecera por «Estado al 27 de noviembre de 2026» y quita las filas de los criterios que ya se cumplen.
4. Copia el resumen a la memoria y a la presentación: lo que no está aquí, está entregado.
