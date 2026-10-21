# Registro de cambios y decisiones — documentación de tareas y criterios

Versión 2 · 7 de octubre de 2026. Explica qué se corrigió en `historias-tareas.md`, `historias-criterios.md` y «Tareas Urgentes», qué valores se propusieron y qué decisiones siguen abiertas. Cualquier cambio futuro a esos documentos se anota aquí en una línea, con fecha y motivo.

## 1. Qué se corrigió

| Problema encontrado | Corrección |
| :--- | :--- |
| Dos numeraciones de HU distintas. En los sprints 3 y 4 el mismo número era dos historias (HU 45 era «Predecir salida» en un documento y «Programar una evaluación» en el otro). | Un solo número por HU, del 1 al 64, que no cambia nunca. Las cuatro historias de evaluaciones, que no tenían número, son HU 61 a HU 64. Los criterios se renumeraron al mismo esquema y la equivalencia con los números antiguos está en la sección 6 del documento de tareas. |
| Las tareas no tenían ID, responsable, dependencias ni criterios que cubren. | Cada tarea tiene un ID (`HU09-T1`), un responsable, las tareas de las que depende, los criterios de aceptación que cubre y un «hecho cuando». Cada criterio indica a su vez qué tareas lo cubren. |
| Tareas que dependían de algo que todavía no existe (ver sección 2). | Dependencias explícitas y cuatro tareas reescritas. |
| Criterios sin tarea, o con reglas sin valor concreto («el mecanismo definido en el documento»). | 23 criterios con valor concreto y 5 criterios agregados; los 280 criterios tienen al menos una tarea (ver sección 3). |
| El Sprint 1 decía 66 tareas y 90 h, pero el trabajo real acordado es de 68 tareas y 94 h (las claves RSA y CORS del contrato JWT). | Las dos tareas están en el documento (HU22-T6 y HU22-T7). Los totales pasan a 270 tareas y 365 h. |
| El documento no decía cuándo una tarea o una historia está terminada, ni dónde se registra el estado. | La sección 5 define cuándo está terminada una tarea y una historia y dónde vive cada cosa (especificación en `docs/`, estado en Trello). |
| Errores de forma: «cada caso cada como», «se adelanta», signos «¿» en lugar de guiones, medias horas con coma. | Corregidos (las horas usan punto decimal). |

## 2. Tareas reescritas o agregadas

Todas las tareas conservan su ID original. Las nuevas se numeran a continuación de las existentes.

| Tarea | Qué cambió | Motivo |
| :--- | :--- | :--- |
| HU02-T2 | Incluye el cierre de sesión (logout) y la duración del refresh por cliente. | Estaba decidido en el contrato JWT pero ninguna tarea lo contenía, y HU04-T3 necesita el logout. |
| HU03-T2 | Incluye los dos endpoints del flujo (pedir enlace y fijar contraseña nueva) y el vencimiento de 30 minutos. | Los endpoints no estaban en ninguna tarea. |
| HU12-T1 | La comprobación de inscritos ahora apunta a HU28-T5. | Decía «se completa en el Sprint 2 (HU26)», pero HU26 no tenía esa tarea. |
| HU16-T3 | Agrega la vista pública de casos de prueba. | La prueba T4 necesita algo que probar en el Sprint 1. |
| HU16-T4 | Se prueba la vista pública; la API del estudiante se vuelve a probar en HU32-T4 y HU39-T4. | Esa API no existe hasta el Sprint 2. |
| HU21-T1 | El perfil se identifica con el email, no con el id. | El contrato JWT y el modelo de datos dicen que el email es el identificador común entre esquemas. |
| HU22-T1 | Incluye GET /api/yo, el 403 para docentes y la creación del perfil en el primer acceso. | Estaba en el sprint backlog y en los criterios del contrato, no en la tarea. |
| HU24-T1 | Crea APP_ENVIO con una migración mínima (id, estudiante_id, problema_id, plataforma, fecha). | Pedía agregar un campo a una tabla que no existe hasta HU36-T1 (Sprint 3). |
| HU24-T4 | Se prueba con un envío sembrado. | No hay envíos reales hasta el Sprint 4. La prueba con envío real queda en HU45-T3. |
| HU36-T1 | Completa APP_ENVIO en lugar de crearla. | Evita crear la tabla dos veces. |
| HU45-T3 | Cada intento móvil se guarda en APP_ENVIO con plataforma movil. | Es donde se verifica de punta a punta CA-24.4. |
| HU25-T1 | Define «publicado»: curso con al menos un problema aprobado. | El término no estaba definido en HU9. |
| HU26-T1 | El código de invitación vence a los 30 días y se genera también para los cursos existentes. | CA-26.4 pedía rechazar códigos vencidos y ninguna tarea los hacía vencer. |
| HU06-T4, HU35-T3, HU46-T3, HU49-T4, HU54-T1, HU57-T4, HU58-T3, HU59-T4 | Valores concretos en lugar de «unos segundos», «un tiempo definido», «cómodos para el pulgar», etc. | Se pueden probar. |
| HU60-T2, HU60-T4 | Posición del propio estudiante en el ranking y 403 para un estudiante no inscrito. | Cubren CA-60.3 y CA-60.4, que ninguna tarea cubría. |
| **HU22-T6** (nueva, 2 h) | Generar el par de claves RSA y documentar cómo regenerarlo. | Del contrato JWT; estaba en la asignación pero no en el documento. |
| **HU22-T7** (nueva, 2 h) | Configurar CORS en admin-api para la web del estudiante. | Del contrato JWT; evita que el navegador bloquee el login. |
| **HU28-T5** (nueva, 1 h) | Bloquear el borrado de un curso con inscritos. | Cierra CA-12.3, que HU12 no puede cumplir en el Sprint 1. |

## 3. Criterios modificados y agregados

Criterios modificados (el criterio ya existía y ahora tiene un valor concreto):

- CA-03.3: el enlace de recuperación vence a los 30 minutos (decisión que estaba en el sprint backlog).
- CA-04.2: 30 minutos sin actividad en la web obligan a iniciar sesión de nuevo.
- CA-04.3: cerrar sesión invalida el refresh al instante; el access vence solo (15 min como máximo).
- CA-12.3: diferido al Sprint 2 y cerrado por HU28-T5.
- CA-24.4: se prueba con datos sembrados en el Sprint 1 y con un envío real en HU45-T3.
- CA-25.3: los resultados se limitan a la institución del claim `institucion` (antes decía «poder filtrarse por institución», pero la tarea los limita siempre).
- CA-26.4: un código vencido es el de más de 30 días.
- CA-35.2: el guardado espera 3 segundos sin escribir.
- CA-45.3, CA-47.3: normalización de respuestas (espacios y mayúsculas) escrita con la regla exacta.
- CA-46.5: botones de al menos 48 dp.
- CA-48.3, CA-48.5: se muestra la respuesta correcta al fallar; la respuesta pendiente se reenvía sola y se registra una sola vez.
- CA-49.1, CA-49.4: la racha suma un día por resolver al menos un ejercicio y vuelve a cero al saltarse un día.
- CA-50.2: los puntos por tipo de ejercicio salen de `docs/puntos-cinturon.md`.
- CA-51.4: el cinturón negro muestra un mensaje especial en lugar de la barra.
- CA-52.1, CA-52.4: el sistema identifica cada día a quien tiene racha activa y no practicó; el estudiante elige la hora del aviso.
- CA-54.1: dos aciertos seguidos suben un nivel y dos errores seguidos lo bajan.
- CA-57.4: un reto sin respuesta caduca a las 24 horas.
- CA-59.3, CA-59.4: 30 segundos para volver tras una desconexión; si no vuelve, pierde la partida.

Criterios agregados (ya estaban decididos en el contrato JWT o en el sprint backlog y faltaban en este documento):

- CA-01.4: la cuenta de docente queda pendiente y el inicio de sesión la rechaza con un mensaje claro.
- CA-02.5: el mensaje de credenciales incorrectas no revela si falló el correo o la contraseña.
- CA-22.4: una credencial ausente, vencida, alterada o firmada con otra clave recibe 401.
- CA-22.5: un token de docente no abre la API del estudiante (403).
- CA-22.6: la primera petición con un token válido crea el perfil del estudiante.

## 4. Valores propuestos que hay que confirmar

Estos valores los puse yo para poder escribir criterios que se puedan probar. Se confirman en el Planning del sprint correspondiente; si cambian, se editan en el criterio, en la tarea y aquí.

| Valor | Propuesta | Dónde aparece |
| :--- | :--- | :--- |
| Vigencia del código de invitación | 30 días, configurable | CA-26.4, HU26-T1 |
| Qué es un curso «publicado» | Tiene al menos un problema aprobado | HU25-T1 |
| Límite de ejecuciones de prueba | 10 por minuto, configurable | HU06-T4 |
| Espera del guardado automático | 3 segundos | CA-35.2, HU35-T3 |
| Normalización de respuestas móviles | Sin espacios al inicio ni al final, espacios repetidos reducidos a uno, sin distinguir mayúsculas | CA-45.3, CA-47.3 |
| Tamaño mínimo de botones | 48 dp | CA-46.5 |
| Caducidad de un reto | 24 horas | CA-57.4, HU57-T4 |
| Tiempo para volver tras desconectarse | 30 segundos | CA-59.3, CA-59.4 |
| Regla de dificultad del Dojo | Dos aciertos suben un nivel, dos errores lo bajan | CA-54.1 |
| Identificador del perfil de estudiante | El email del token | HU21-T1 |
| Tareas del contrato JWT dentro del sprint | HU22-T6 y HU22-T7 (Sprint 1 de 94 h) | Resumen |

## 5. Decisiones abiertas

Estas las detecté, pero no las resolví porque cambian el diseño y las decide el equipo.

1. **Quién llama al sandbox.** HU06-T1 y HU07-T3 ponen el cliente de Judge0 en user-api (Spring). El ADR-2 y HU08 ponen la evaluación en ai-service (`/evaluate` y los proveedores Judge0, Piston y mock). Hay que elegir uno antes del Sprint 2, porque HU36-T2 depende de esa decisión.
2. **De dónde sale el «tema» de un problema.** HU56 muestra «los temas en los que más falló», pero ADMIN_PROBLEMA no tiene un campo `tema` (HU13-T1 define título, enunciado, lenguaje y dificultad). Hay que agregarlo al modelo o cambiar el criterio.
3. **Si los ejercicios móviles se guardan como envíos.** HU24 y HU45-T3 asumen que cada intento móvil se guarda en APP_ENVIO. Si no es así, CA-24.4 y el progreso por cuenta cambian.
4. **Capacidad.** El Sprint 1 tiene 94 h y el Sprint 2 tiene 91 h frente a 90 h de capacidad. El reparto es muy desigual en los sprints 2 a 4: Juan Diego 49 h, Carlos 29 h y Gael 13 h en el Sprint 2; Juan Diego 45 h, Carlos 0 h y Gael 45 h en el Sprint 4 (el Sprint 4 es casi todo móvil y backend, y Carlos, que es frontend web, queda sin tareas). Conviene rebalancear antes de cada Planning.
5. **Estados de HU30.** «Resuelto» e «intentado» se calculan con los envíos (APP_ENVIO), que solo existen de verdad desde el Sprint 3. En el Sprint 2 se prueba con datos sembrados (HU30-T2).

## 6. Cambios de proceso que quedan fuera de estos documentos

- Flujo con ramas y Pull Requests: sustituye la decisión «todos suben directo a `main`» del Sprint 1. Hay que actualizar `CONTRIBUTING.md`, la plantilla de PR y las protecciones de rama (ver `flujo-git.md`).
- Estado de las tareas en Trello: el sprint backlog anterior mencionaba un tablero en GitHub Projects (habilitador H2). Hay que decidir que sea solo Trello y cerrar H2.
- Correcciones pendientes al GLAB-S07: «68 tareas» del Sprint 1 y «68» del Sprint 3 (son 69), y la línea de capacidad de la sección 5 (1 h al día debe ser 3 h).
- La descripción del Project dice «SCRAM»; es «Scrum».
- «Introducción» no cambió. Su tabla de equipo (43 h / 29 tareas, 31 h / 21 y 20 h / 18) coincide con el Sprint 1 de esta versión.

## 7. Cómo se comprobó

Las dos tablas se generaron con un script y se validó que: las 267 tareas originales y las 3 nuevas tienen metadatos y ninguna sobra; los IDs no se repiten; las 64 historias de los dos documentos coinciden una a una; la carga del Sprint 1 da exactamente 43 h, 31 h y 20 h (29, 21 y 18 tareas) como en la asignación; cada dependencia existe, no apunta a un sprint posterior y no forma ciclos; y cada uno de los 280 criterios tiene al menos una tarea que lo cubre.
