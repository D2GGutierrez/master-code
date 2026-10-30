>Se documentaron cuatro decisiones de arquitectura (Architecture Decision Records) antes de iniciar la construcción de los módulos de negocio.

## **ADR-1: Base de datos**

### Contexto

>Administración (Django/ORM) y Usuario (Spring Boot/JPA) son dos backends independientes que necesitan compartir información: los problemas que crea un docente deben ser visibles para el estudiante.

### Decisión

>Una sola instancia de PostgreSQL, con dos esquemas separados: admin_schema (migrado por Django) y app_schema (migrado por Spring Boot/Flyway). Ningún servicio escribe directamente en el esquema del otro; si Usuario necesita datos de Admin (por ejemplo, el catálogo de problemas), se expone vía una API interna de Django en lugar de leer la tabla directamente. Esto evita que dos ORMs distintos compitan por el mismo esquema y evita construir sincronización entre dos bases separadas.

#### Alternativa descartada

>Dos bases de datos independientes con sincronización: más "puro" a nivel de microservicios, pero para un equipo de 2 a 5 personas construyendo un MVP añade una complejidad de integración que no se justifica todavía.

## **ADR-2: Motor de IA (generación y evaluación de problemas)**

>La plataforma necesita (a) generar problemas de programación a partir del contexto que da el docente, y (b) calificar las soluciones que envían los estudiantes, ahora con dos formatos distintos según la plataforma (ver sección 5.1). 

|                                                                                                                                |
| ------------------------------------------------------------------------------------------------------------------------------ |
| Idea central: separar "calificar" (gratis, no necesita IA) de "generar contenido" (sí necesita IA, pero con muy poco volumen). |

### Calificar (evaluación) sin IA

>Para Web (escritura_codigo), el sandbox de ejecución (Judge0 o Piston, ambos open-source y gratuitos de autohospedar solo se paga el servidor que de todas formas se necesita) corre el código del estudiante contra los casos de prueba y devuelve pase/falla por cada uno. Esa es la nota; no hace falta ningún LLM para calificar. Para Móvil, la evaluación ya era gratuita desde el diseño original: comparación directa de texto/opción (ver tabla en 5.1).

#### Feedback cualitativo de IA sobre el código pausado, fuera del MVP

>Comentarios de estilo/legibilidad del tipo "tu lógica funciona pero podrías usar una lista" son la única parte que realmente escala con la cantidad de estudiantes (cada envío costaría una llamada a un LLM), así que con presupuesto cero se deja como mejora futura, no como requisito para lanzar. La plataforma funciona sin ella: el estudiante ya sabe si pasó o no cada caso de prueba.

### Generar problemas (lado del docente) — sí usa IA, pero cabe en un nivel gratuito

>El volumen aquí lo pone el docente, no el estudiante: unos pocos docentes generando problemas para sus cursos, no miles de estudiantes enviando soluciones. Con eso, un proveedor de LLM con nivel gratuito alcanza sin pagar nada:

- Google Gemini (AI Studio): 1,500 solicitudes/día gratis en Gemini 2.5 Flash, sin tarjeta, sin vencimiento.

- GitHub Models: acceso gratis a modelos como GPT-4o / Claude 3.5 Sonnet vía cuenta de GitHub (revisar si Tecsup ya provee el GitHub Student Developer Pack).

- Groq: modelos open-source (Llama 3.3 70B) gratis y rápidos, 1,000 solicitudes/día.

>Actualización del 27 de octubre de 2026 (HU17-T3): GitHub Models se retiró, así que ai-service ya no lo ofrece (`LLM_PROVIDER` acepta `mock`, `gemini` o `groq`). Las cuotas gratuitas y los nombres de modelo cambian seguido y Google ya no publica los límites en su documentación: se revisan en AI Studio antes de depender de un número. Por eso el modelo se elige con `LLM_MODEL` (por defecto `gemini-flash-latest` para Gemini y `llama-3.3-70b-versatile` para Groq) y retirar un modelo no obliga a cambiar código.


### Diseño técnico

>Un servicio dedicado (ai-service, Python + FastAPI) expone dos endpoints:

- POST /generate: recibe el contexto del docente (tema, nivel, lenguaje, objetivos, cantidad) más la plataforma destino, y devuelve el contenido en el formato correspondiente: para Web, enunciado + starter code + casos de prueba; para Móvil, el snippet + tipo de pregunta + respuesta correcta ya calculada. Lo llama Administración (Django). El docente revisa y aprueba antes de publicar — obligatorio, porque con un LLM gratuito hay más probabilidad de que se le escape un error.

>**Contrato de `POST /generate`** (HU17-T3). Cuerpo: `{"tema", "nivel", "lenguaje", "cantidad", "plataforma"}` (`cantidad` de 1 a 10; `nivel`: facil, medio o dificil; `lenguaje`: python, java o javascript; `plataforma`: web, movil o ambas). Respuesta 200: `{"problemas": [{"titulo", "enunciado", "tipo_ejercicio", "casos_prueba", "opciones", "respuesta_esperada"}]}`, siempre validada contra el formato fijo y con exactamente `cantidad` problemas de un tipo que sirva para la plataforma. Errores: 422 si el cuerpo es inválido; 502 si la IA respondió algo que no cumple el formato (el `detail` dice qué); 503 si el proveedor no se pudo usar (sin clave, sin red o la petición fue rechazada). ai-service no reintenta: reintentar es decisión de admin-api (HU17-T6). Puede tardar hasta 90 segundos, así que el cliente debe esperar al menos eso. Con `LLM_PROVIDER=mock` responde problemas de relleno marcados con «[Mock]», sin clave ni red.

- POST /evaluate: no llama a ningún LLM en el MVP. Solo orquesta la llamada al sandbox (Judge0/Piston) para escritura_codigo y devuelve pase/falla por caso de prueba. El Feedback cualitativo queda como un parámetro opcional que hoy simplemente no se activa.

==¿Por qué un servicio aparte?== Aunque en el MVP no llame a un LLM en /evaluate, mantenerlo como servicio propio (en vez de meterlo en Django o Spring Boot) deja la puerta abierta para activar el Feedback cualitativo más adelante sin tocar los otros dos backends, y centraliza la integración con el sandbox.

>Proveedor de LLM: se usará una capa de abstracción para no acoplarse a Gemini/GitHub Models/Groq específicamente — el día que haya presupuesto (o un auspicio institucional), cambiar de proveedor o activar el Feedback cualitativo de pago será un cambio de configuración, no de arquitectura.

Ejecución de código de estudiantes: nunca se ejecuta código sin sandbox; límites estrictos de tiempo/CPU/memoria; sin acceso a red desde el sandbox.

|   |
|---|
|Costo total estimado del MVP: $0/mes (solo el servidor que ya se necesita para los demás servicios).|

## **ADR-3: Estrategia de repositorios**

>**Estado: reemplazado por ADR-5 (monorepo).**

### Decisión

>Multi-repo (un repositorio por servicio desplegable), porque cada stack tiene un ciclo de build/deploy y versionado independiente:

- masters-code-admin-web (React)

- masters-code-admin-api (Django)

- masters-code-user-web (React)

- masters-code-user-api (Spring Boot)

- masters-code-mobile (Kotlin/Android)

- masters-code-ai-service (Python/FastAPI)

>Las decisiones de arquitectura, como este documento, viven en el Proyecto de Claude "MASTER CODE" o en un repositorio ligero masters-code-docs, para que no se pierdan entre los seis repositorios.

## **ADR-4: Autenticación compartida**

### Contexto

>Docentes y estudiantes son usuarios de sistemas distintos (Django vs. Spring Boot), pero conviene no construir autenticación dos veces.

### Decisión

>Contraseñas: Django las guarda con PBKDF2-SHA256 (el hash por defecto de `auth_user`, con sal y miles de iteraciones), nunca en texto plano. Confirmado en HU04-T1.

>Django (que trae autenticación robusta de fábrica) actúa como emisor de identidad para ambos lados vía JWT firmado; Spring Boot valida esos tokens con la clave pública de Django (no necesita su propio sistema de Login). El Frontend de Usuario (React/Kotlin) obtiene el token contra el backend de Administración o contra un endpoint de autenticación expuesto por Django, reutilizado para estudiantes.

>Detalle concreto del contrato (claims, duraciones, rutas, llave pública): [`contrato-jwt.md`](contrato-jwt.md).

## **ADR-5: Monorepo**

### Decisión

>Todo el proyecto vive en un solo repositorio, con una carpeta por servicio o aplicación en la raíz (`admin-api`, `admin-web`, `user-api`, `user-web`, `mobile`, `ai-service`, `docs`, `infra`) y un solo `docker-compose.yml`. Reemplaza al ADR-3 (varios repositorios).

### Motivo

>Con tres integrantes, un solo repositorio simplifica el tablero, las revisiones y el `docker compose up` que levanta todos los servicios en la misma red.

## **ADR-6: Flujo de trabajo con ramas**

### Decisión

>Una rama permanente por persona (`Gutierrez`, `carlos-junco`, `gael-lajara`) y `main`, que siempre funciona y es lo que se presenta. Cada quien trabaja a diario en su rama, con un commit por tarea. **Solo se lleva algo a `main` cuando ya funciona en la rama personal** (CI en verde y probado a mano), mediante un Pull Request revisado por otra persona. Al cerrar cada sprint se marca con una etiqueta (`sprint-1`, `sprint-2`...). Los commits se hacen manualmente y conservan su fecha real. Antes del Pull Request, quien hizo la tarea debe poder explicar cada archivo que cambió. Reglas completas en [`flujo-git.md`](flujo-git.md).

### Motivo

>Es la regla que pide la institución y evita que `main` quede roto. Reemplaza la decisión anterior de subir directo a `main` y la de una rama por tarea, que no se usó en la práctica.

## **ADR-7: Detalles del Sprint 2 (decidido el 24 de octubre de 2026)**

### D1. Quién llama al sandbox

> **Decidido.** `user-api` llama a `ai-service` (`POST /execute`), y `ai-service` usa el proveedor activo (Judge0, Piston o mock, ver HU08). HU07-T3 pasa a ser el cliente Spring hacia `ai-service`, y el proveedor Judge0 en Python se agrega dentro de HU08-T1. Motivo: es lo que pide ADR-2 y permite cambiar de motor solo con una variable de entorno. Alternativa: `user-api` llama a Judge0 directo y los proveedores de `ai-service` no se usan.
>
> **Contrato de `POST /execute`** (el cliente de `user-api` ya lo usa; el endpoint en `ai-service` se construye en HU08-T3). Cuerpo: `{"codigo", "lenguaje", "entrada"}`. Respuesta 200: `{"estado", "salida", "error", "tiempo_ms"}`, con `estado` = `ok`, `error_ejecucion`, `error_compilacion`, `tiempo_agotado` o `error_servicio`; `tiempo_ms` puede ser `null`. Un código que falla es una respuesta 200 con su estado, no un error HTTP. Respuesta 400: lenguaje no soportado por el motor activo.

### D3. Autenticación entre servicios (API interna)

> **Decidido.** rutas bajo `/internal/` en `admin-api` y `user-api`, solo alcanzables por la red de Docker y protegidas con la cabecera `X-Internal-Key`. La clave vive en una variable de entorno que solo reciben esos dos servicios. No se reenvía el JWT del usuario, porque los datos que pide el estudiante no le pertenecen a su rol.
>
> **Rutas internas construidas** (la clave es la variable `INTERNAL_API_KEY`; sin ella, las dos APIs rechazan todo con 403):
>
> - `admin-api`: `GET /internal/cursos/por-codigo/<codigo>/` (200 con el curso; 404 si no existe; 410 si venció) y `GET /internal/cursos/?ids=1,2` (los cursos que existan). Las usa `user-api` en la inscripción (HU26-T3) y en «Mis cursos» (HU27-T1).
> - `admin-api`, para el estudiante (HU25-T1, HU31, HU32-T1): `GET /internal/cursos/publicados/` (los cursos con al menos un problema visible, con su institución; filtros opcionales `?institucion=` y `?buscar=`), `GET /internal/cursos/<id>/problemas/` (los problemas visibles del curso, sin enunciado ni casos; 404 si el curso no existe; filtro opcional `?plataforma=web|movil`, que deja también los de `ambas`) y `GET /internal/problemas/<id>/` (un problema con solo sus casos públicos y su `curso_id`; 404 si no existe, no está aprobado o fue descartado, sin distinguir cuál). Las tres parten de `Problema.objects.visibles_para_estudiante()` (D5): es la única puerta por la que un problema llega a un estudiante, y «publicado» significa que el curso tiene al menos un problema así. No entregan casos ocultos, respuesta esperada ni cuál alternativa es correcta; las alternativas llegarán con HU46-T2.
> - `admin-api`, para calificar (HU36-T2): `GET /internal/problemas/<id>/calificacion/` (el problema con **todos** sus casos, los ocultos también, cada uno con su `es_publico`, y su `curso_id`; 404 con la misma regla que la anterior). Es la única ruta que entrega casos ocultos y solo la usa `POST /api/problemas/{id}/envios` de `user-api` (D7); el estudiante nunca recibe ese contenido.
> - `user-api`: `GET /internal/cursos/{cursoId}/inscritos` (email y fecha de cada inscrito). La usa `admin-api` en la lista de inscritos del docente (HU28-T1), que además comprueba que el curso sea del docente (403 si es de otro).
>
> **Quién las consume en `user-api`** (HU25-T2 y T4, HU29-T1, HU30, HU31-T2):
>
> - `GET /api/cursos`: el catálogo. Los cursos publicados de la institución del claim `institucion` del token, con `?buscar=` opcional (parte del nombre). La institución nunca se acepta por parámetro y `user-api` la vuelve a aplicar sobre lo que entrega `admin-api`. Sin coincidencias responde 200 con lista vacía (la pantalla muestra «sin resultados»).
> - `GET /api/cursos/{id}/problemas`: 403 si el estudiante no está inscrito. Filtros opcionales y combinables `?plataforma=web|movil`, `?dificultad=facil|medio|dificil` y `?estado=sin_abrir|intentado|resuelto` (ver D6). Un valor de filtro que no existe es 400; una combinación sin coincidencias es 200 con lista vacía. Cada fila trae su `estado`.
> - `GET /api/problemas/{id}`: 404 si el problema no es visible o el estudiante no está inscrito en su curso, con el mismo cuerpo que un id inexistente.
> - `POST /api/problemas/{id}/envios`: el envío oficial con su calificación (ver D7).
>
> `user-api` comprueba la inscripción porque es quien la tiene (ADR-1); `admin-api` decide qué problemas son visibles.

### D4. CORS de `user-api`

> **Decidido.** lista de orígenes permitidos por variable de entorno (`CORS_ALLOWED_ORIGINS`), con el mismo patrón que `admin-api` (HU22-T7). Sin esto, `user-web` (puerto 3001) no puede llamar a `user-api` (8081) desde el navegador.

### D5. Revisión de los problemas generados por la IA (HU17 a HU20; decidido el 27 de octubre de 2026)

> **Decidido.** En `admin-api`, `Problema` suma `generado_por_ia`, `aprobado_por_docente` y `respuesta_esperada`, y se crea `OpcionRespuesta` (`ADMIN_OPCION_RESPUESTA`, que era HU46-T1) porque la IA también genera ejercicios de opción múltiple y hay que guardar sus alternativas. Lo generado nace con `generado_por_ia=true` y `aprobado_por_docente=false`: la IA nunca publica sola (ADR-2). Un estudiante solo ve lo que cumple `Problema.objects.visibles_para_estudiante()` (aprobado y no archivado); HU31 debe partir de esa consulta.
>
> - **Los problemas creados a mano nacen aprobados si están completos**, con el docente que los creó como aprobador. Si les falta algo (por ejemplo, un `opcion_multiple` sin alternativas) se guardan como borrador sin aprobar. La migración aprueba los problemas de escritura de código que ya existían y tienen al menos un caso de prueba.
> - **«Completo»** (`problemas/completitud.py`): `escritura_codigo` con al menos un caso de prueba; `opcion_multiple` con entre 2 y 6 alternativas y exactamente una correcta; `prediccion_salida` y `completar_espacio` con `respuesta_esperada`. La misma regla decide el guardado de lo que genera la IA, el 400 de Aprobar y si un problema manual nace aprobado.
> - **La plataforma de cada problema generado sale de su tipo** (`escritura_codigo` va a `web`; los otros tres a `movil`). Pedir «ambas» significa «genera para las dos», no que cada problema se publique en las dos.
>
> **Rutas de `admin-api`** (todas exigen sesión de docente; un problema o curso ajeno da 404):
>
> - `POST /api/cursos/<id>/problemas/generar/`, cuerpo `{"tema", "nivel", "cantidad", "lenguaje"?, "plataforma"?}`. `lenguaje` por defecto es el del curso; `plataforma` por defecto es `ambas`; `cantidad` de 1 a 10. Responde 201 con `{"problemas": [...]}`. Todo o nada: si un solo problema no cumple el formato, no se guarda ninguno. Con ai-service caído o sin tiempo: 503; con una respuesta inservible: 502. Ambos con `{"detail": "...", "reintentar": true}` y sin guardar nada. Puede tardar hasta 100 segundos (`AI_SERVICE_TIMEOUT_SEGUNDOS`).
> - `GET /api/problemas/pendientes/` (`?curso=<id>` opcional): lista de problemas de IA sin aprobar ni descartar de los cursos del docente, del más nuevo al más antiguo, con `generado_por_ia`, `curso_nombre`, casos, alternativas y respuesta esperada.
> - `POST /api/problemas/<id>/aprobar/`: 200 con el problema aprobado y registrado quién y cuándo. 400 si está incompleto, con el motivo bajo el nombre del campo (`casos_prueba`, `opciones` o `respuesta_esperada`). 409 si fue descartado. Repetirlo es 200 y no cambia nada.
> - `POST /api/problemas/<id>/descartar/`, cuerpo `{"confirmar": true}`: archiva el problema, no lo borra. Sin `confirmar: true` responde 400 y no cambia nada (el cuadro de confirmación es de la pantalla; esto evita descartar por una llamada suelta). 409 si ya está aprobado. Repetirlo es 200.
>
> **Pendiente:** editar un problema antes de aprobarlo (CA-19.1 a CA-19.3) necesita un `PATCH /api/problemas/<id>/` que todavía no existe en `admin-api`.

### D6. Estado de un problema para el estudiante (HU30; decidido el 28 de octubre de 2026)

> **Decidido.** El estado se calcula en `user-api` con los envíos del estudiante (`app_envio`) y no se guarda: `sin_abrir` = ningún envío; `intentado` = hay envíos, pero ninguno correcto; `resuelto` = al menos un envío con `resultado = 'correcto'` (basta uno, venga de web o de móvil: son de la misma cuenta). Abrir un problema no deja rastro, así que «sin abrir» significa «sin envíos». Un envío sin calificar (`resultado` vacío) cuenta como intento. La ejecución de prueba («Ejecutar») no crea envíos y no cambia el estado.
>
> **Migración V4 de `user-api`:** agrega `app_envio.resultado` (`'correcto'` o `'incorrecto'`; vacío si no se calificó; la V5 de D7 sumó `'parcial'`) y el índice `(estudiante_id, problema_id)`. Se adelantó desde HU36-T1 porque HU30-T2 no puede distinguir «resuelto» sin ella. **HU36-T1 no debe volver a crear la columna:** completa la tabla con código, puntaje y número de intento, y puede ampliar el CHECK de `resultado` si HU36-T2 necesita más valores.
>
> Los filtros de dificultad y de estado se aplican en `user-api` sobre los problemas visibles que entrega `admin-api` (un curso tiene pocos problemas), así que el contrato interno no cambia.

### D7. Enviar solución: envío oficial y calificación (HU36 y HU38, adelantadas al Sprint 2; decidido el 29 de octubre de 2026)

> **Decidido.** Sin un envío oficial el estudiante no puede resolver un problema desde la app, porque «Ejecutar» no deja rastro (D6). Por eso el backend de «Enviar solución» se adelantó desde el Sprint 3. `POST /api/problemas/{id}/envios` (`user-api`) ejecuta el código del estudiante contra **todos** los casos del problema, ocultos incluidos, llamando a `ai-service` (`POST /execute`, D1); califica sin IA (ADR-2) y guarda el intento en `app_envio`. La lista de casos sale de `GET /internal/problemas/<id>/calificacion/` (D3).
>
> - **Fórmula del puntaje (HU38-T1).** Casos superados × 100 / casos totales, con dos decimales: 3 de 4 casos son 75,00 %; 1 de 3, 33,33 %. `resultado` es `correcto` si pasan todos, `parcial` si pasan algunos e `incorrecto` si no pasa ninguno. Solo `correcto` deja el problema `resuelto`; `parcial` e `incorrecto` lo dejan `intentado` (D6).
> - **Cuándo pasa un caso.** El motor terminó con `ok` y la salida coincide con la esperada ignorando los espacios y saltos de línea del final (de cada línea y del texto completo, también `\r\n`). Las mayúsculas y los espacios del medio sí cuentan: el programa debe imprimir justo lo pedido. Un error de compilación, de ejecución o de tiempo cuenta como caso fallado.
> - **Lenguaje.** El del problema; el estudiante no lo elige. Solo los ejercicios `escritura_codigo` se envían por aquí (otro tipo es 400).
> - **Número de intento (CA-36.3).** Se cuenta por estudiante y por problema (1, 2, 3…). Lo calcula la misma sentencia que inserta, y un índice único `(estudiante_id, problema_id, numero_intento)` impide que dos envíos simultáneos compartan número: la base rechaza el segundo y el repositorio lo reintenta con el siguiente (hasta tres veces).
> - **Quién y desde dónde.** El estudiante sale del token, nunca del cuerpo. La plataforma sale del claim `cliente` (`mobile` → `movil`; cualquier otro valor → `web`), tampoco se acepta del cuerpo.
> - **Si falla el motor, el intento no se cuenta.** Si `ai-service` no responde o un caso termina en `error_servicio`, la respuesta es 503 («Tu envío no se contó») y no se guarda nada, ni siquiera los casos que ya habían corrido. Un código que falla sí se guarda como intento: compilación, ejecución o tiempo agotado son culpa del código, no del servicio.
> - **Casos ocultos (HU39).** De un caso oculto al estudiante solo le llegan `publico: false` y `paso`. De uno público, además, `entrada`, `salida_esperada`, `salida_obtenida`, `error` y `estado`; la salida y el error se recortan a 2 000 caracteres. El `aviso` general usa mensajes fijos («Tu programa tardó demasiado…», «Tu programa falló…») y solo copia el texto del motor cuando es un error de compilación, porque ese texto no depende de la entrada; el de un error de ejecución podría imprimir la entrada de un caso oculto.
> - **Qué se guarda (HU36-T1, HU42-T1).** La migración V5 de `user-api` agrega a `app_envio` el `codigo` completo, el `puntaje` (0 a 100) y el `numero_intento`; amplía el CHECK de `resultado` con `'parcial'` y numera por fecha los envíos que ya existían. **Todavía no se guarda el resultado de cada caso** (HU37-T1) ni hay historial de envíos (HU41).
>
> **Respuesta 201:** `{id, numero_intento, resultado, puntaje, casos_superados, casos_total, aviso, casos: [...]}`, donde `aviso` es `null` si no hubo errores del programa. **Errores:** 400 (código vacío o de más de 50 000 caracteres, ejercicio que no es de escribir código, lenguaje que el motor no soporta), 404 (problema no visible o de un curso donde el estudiante no está inscrito, con el mismo cuerpo que un id inexistente), 409 (problema sin casos de prueba), 503 (`admin-api` o `ai-service` no disponibles).
>
> **Pendiente:** un límite de envíos por minuto, como el de «Ejecutar» (HU06-T4). Por ahora el botón de la pantalla se bloquea mientras se evalúa.

### Ya decidido en `registro-cambios.md`

>Curso «publicado» = tiene al menos un problema aprobado (HU25-T1). El código de invitación vence a los 30 días y se genera también para los cursos existentes (HU26-T1). Límite de ejecuciones de prueba: 10 por minuto, configurable (HU06-T4).
