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
> **Contrato de `POST /execute`** (el cliente de `user-api` ya lo usa; el endpoint en `ai-service` se construye en HU08-T3). Cuerpo: `{"codigo", "lenguaje", "entrada"}`. Respuesta 200: `{"estado", "salida", "error", "tiempo_ms"}`, con `estado` = `ok`, `error_ejecucion`, `error_compilacion`, `tiempo_agotado`, `memoria_excedida` (HU40-T1) o `error_servicio`; `tiempo_ms` puede ser `null`. Un código que falla es una respuesta 200 con su estado, no un error HTTP. Respuesta 400: lenguaje no soportado por el motor activo.

### D3. Autenticación entre servicios (API interna)

> **Decidido.** rutas bajo `/internal/` en `admin-api` y `user-api`, solo alcanzables por la red de Docker y protegidas con la cabecera `X-Internal-Key`. La clave vive en una variable de entorno que solo reciben esos dos servicios. No se reenvía el JWT del usuario, porque los datos que pide el estudiante no le pertenecen a su rol.
>
> **Rutas internas construidas** (la clave es la variable `INTERNAL_API_KEY`; sin ella, las dos APIs rechazan todo con 403):
>
> - `admin-api`: `GET /internal/cursos/por-codigo/<codigo>/` (200 con el curso; 404 si no existe; 410 si venció) y `GET /internal/cursos/?ids=1,2` (los cursos que existan). Las usa `user-api` en la inscripción (HU26-T3) y en «Mis cursos» (HU27-T1).
> - `admin-api`, para el estudiante (HU25-T1, HU31, HU32-T1): `GET /internal/cursos/publicados/` (los cursos con al menos un problema visible, con su institución; filtros opcionales `?institucion=` y `?buscar=`), `GET /internal/cursos/<id>/problemas/` (los problemas visibles del curso, sin enunciado ni casos; 404 si el curso no existe; filtro opcional `?plataforma=web|movil`, que deja también los de `ambas`) y `GET /internal/problemas/<id>/` (un problema con solo sus casos públicos y su `curso_id`; 404 si no existe, no está aprobado o fue descartado, sin distinguir cuál). Las tres parten de `Problema.objects.visibles_para_estudiante()` (D5): es la única puerta por la que un problema llega a un estudiante, y «publicado» significa que el curso tiene al menos un problema así. No entregan casos ocultos, respuesta esperada ni cuál alternativa es correcta; las alternativas llegarán con HU46-T2.
> - `admin-api`, para calificar (HU36-T2): `GET /internal/problemas/<id>/calificacion/` (el problema con **todos** sus casos, los ocultos también, cada uno con su `id` (HU37-T1, para guardar su resultado) y su `es_publico`, y su `curso_id`; 404 con la misma regla que la anterior). Es la única ruta que entrega casos ocultos y solo la usa `POST /api/problemas/{id}/envios` de `user-api` (D7); el estudiante nunca recibe ese contenido.
> - `user-api`: `GET /internal/cursos/{cursoId}/inscritos` (email y fecha de cada inscrito). La usa `admin-api` en la lista de inscritos del docente (HU28-T1), que además comprueba que el curso sea del docente (403 si es de otro).
> - `user-api`: `GET /internal/envios/resumen?problemas=1,2,3` (por cada problema con envíos, `problema_id`, `intentos` y `correctos`; hasta 200 ids; un problema sin envíos no viene). La usa `admin-api` en `GET /api/cursos/<id>/resumen-envios/` (HU44-T1, D10), que comprueba que el curso sea del docente (403 si es de otro), pide solo los problemas que ve el estudiante y completa con ceros los que no tienen envíos. Solo cifras: ni estudiantes ni código.
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
> - **«Completo»** (`problemas/completitud.py`): `escritura_codigo` con al menos un caso de prueba; `opcion_multiple` con entre 2 y 6 alternativas y exactamente una correcta; `prediccion_salida` con `respuesta_esperada`; `completar_espacio` con `respuesta_esperada` y exactamente un hueco en el enunciado (D17). La misma regla decide el guardado de lo que genera la IA, el 400 de Aprobar y si un problema manual nace aprobado.
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
> - **Casos ocultos (HU39).** De un caso oculto al estudiante solo le llegan `publico: false` y `paso`. De uno público, además, `entrada`, `salida_esperada`, `salida_obtenida`, `error` y `estado`; la salida y el error se recortan a 2 000 caracteres. El `aviso` general usa mensajes fijos («Tu programa tardó demasiado…», «Tu programa falló…») y solo copia el texto del motor cuando es un error de compilación, porque ese texto no depende de la entrada; el de un error de ejecución podría imprimir la entrada de un caso oculto. Cuando ese texto trae la línea del error (`Main.java:3:` o `line 3`), el aviso la dice aparte: «Tu código no compiló. Revisa la línea 3.» (HU40-T2). Ese mismo aviso se guarda con el envío en `app_envio.mensaje_error` (V8, HU40-T4; NULL si no hubo error) y el historial lo devuelve como `mensaje_error`.
> - **Qué se guarda (HU36-T1, HU42-T1).** La migración V5 de `user-api` agrega a `app_envio` el `codigo` completo, el `puntaje` (0 a 100) y el `numero_intento`; amplía el CHECK de `resultado` con `'parcial'` y numera por fecha los envíos que ya existían. **Todavía no se guarda el resultado de cada caso** (HU37-T1) ni hay historial de envíos (HU41).
>
> **Respuesta 201:** `{id, numero_intento, resultado, puntaje, casos_superados, casos_total, aviso, casos: [...]}`, donde `aviso` es `null` si no hubo errores del programa. **Errores:** 400 (código vacío o de más de 50 000 caracteres, ejercicio que no es de escribir código, lenguaje que el motor no soporta), 404 (problema no visible o de un curso donde el estudiante no está inscrito, con el mismo cuerpo que un id inexistente), 409 (problema sin casos de prueba), 503 (`admin-api` o `ai-service` no disponibles).
>
> **Pendiente:** un límite de envíos por minuto, como el de «Ejecutar» (HU06-T4). Por ahora el botón de la pantalla se bloquea mientras se evalúa.

### Ya decidido en `registro-cambios.md`

>Curso «publicado» = tiene al menos un problema aprobado (HU25-T1). El código de invitación vence a los 30 días y se genera también para los cursos existentes (HU26-T1). Límite de ejecuciones de prueba: 10 por minuto, configurable (HU06-T4).


## **ADR-8: Detalles del Sprint 3 (decidido el 3 de noviembre de 2026)**

### D8. Qué manda al abrir el editor (HU34, HU35 y HU42)

> **Decidido.** Orden de preferencia: **borrador guardado** (HU35), luego **código base** (HU34, solo la primera vez, cuando aún no hay borrador) y, si no hay ninguno, **editor vacío**. «Volver al código base original» (HU34-T4) y «copiar al editor» (HU42-T4) reemplazan el contenido del editor con confirmación, y el siguiente guardado automático lo toma como borrador. El código de un envío anterior (HU42-T3) se abre en otro editor de solo lectura y nunca toca el borrador.
>
> **Alternativas descartadas:** que el código base mande siempre (el estudiante perdería lo escrito al reabrir, lo contrario de HU35) y que el estudiante elija al abrir (agrega una pantalla y una tarea que no existen).

### D9. Dónde se calcula el avance y cómo se guarda en caché (HU43)

> **Decidido.** El servidor calcula el avance (HU43-T1) y la tarjeta de progreso lo lee (HU43-T2). El listado de un curso sigue contando en el navegador porque ya tiene los datos. La caché es **en memoria del proceso** (Spring Cache con Caffeine), con clave estudiante y curso, invalidada en cada envío nuevo y con vencimiento de pocos minutos, porque el docente puede aprobar o archivar problemas. **Sin Redis:** el único Redis es el de Judge0 y no se comparte (ADR-1).
>
> **Qué problemas cuentan (HU43-T4).** Cuenta exactamente la lista que `admin-api` entrega al estudiante en ese curso: los problemas **aprobados por un docente y no descartados** (`visibles_para_estudiante()`, D5 y HU31), filtrados por plataforma si se pidió. No existe un estado «publicado» aparte: aprobar es publicar (`fecha_publicacion` es la fecha de aprobación). Un borrador, uno sin aprobar o uno descartado no cuenta en el total, y el envío de un problema que no está en la lista tampoco suma a resueltos ni a intentados. Si el docente archiva un problema, baja el total y deja de contar el envío que el estudiante ya tenía; si aprueba uno, entra como pendiente. De ahí salen los dos bordes: **curso vacío** (sin problemas aprobados) da todo en 0, sin error, y **curso terminado** (todos los de la lista resueltos) da `resueltos = total` y `pendientes = 0`. Las tres cuentas siempre suman el total.
>
> **Hecho en HU43-T3.** `AvanceDelCursoServicio.calcular` lleva `@Cacheable` con la clave `estudiante:curso:plataforma` (cada filtro de plataforma es una entrada); vence a los 5 minutos y guarda como máximo 10 000 entradas (`spring.cache` en `application.yml`). Un error (admin-api caído, curso inexistente) no se guarda. Cada envío guardado llama a `invalidar(estudiante, curso)`, que descarta los tres filtros. La inscripción y la plataforma se comprueban siempre antes de leer la caché. Si el docente aprueba o archiva un problema, el avance puede tardar hasta 5 minutos en reflejarlo.
>
> **Alternativas descartadas:** no usar caché (deja HU43-T3 sin cumplir) y un Redis propio de la aplicación (contenedor y configuración nuevos, excesivo para el MVP).

### D10. Cómo llega el docente a los envíos de sus estudiantes (HU44)

> **Decidido.** `admin-web` solo habla con `admin-api`. Para HU44, `admin-api` verifica que el curso es del docente y consulta a `user-api` por una ruta `/internal/` protegida con `X-Internal-Key` (el mecanismo de D3, en sentido contrario). `user-api` no acepta tokens de docentes, y HU44-T4 prueba una sola frontera.
>
> **Costo no contado:** la ruta de `admin-api`, su cliente, la ruta interna de `user-api` y la URL de `user-api` en `docker-compose.yml` y `.env.example` (la clave `INTERNAL_API_KEY` ya llega a los dos servicios). Va dentro de HU44-T1 (unas 2 h más).
>
> **Alternativas descartadas:** que `admin-web` llame directo a `user-api` (mezcla las dos fronteras de seguridad) y duplicar los envíos en `admin-api` (dos copias que sincronizar).

### D11. Cómo se cierra un intento de evaluación por tiempo (HU64)

> **Decidido.** **Cierre perezoso:** al entrar o enviar, `user-api` compara la hora actual con la hora de inicio del estudiante más el límite de minutos (y con el fin de la ventana); si venció, el intento figura como cerrado. No hay tarea programada. Hay que guardar la hora de inicio del intento (migración y columna nuevas, unas 1 h más dentro de HU64-T3) y una ruta `/internal/evaluaciones/{id}/` de `admin-api` con la ventana y el límite (1 h más dentro de HU64-T2).
>
> **Hecho en HU64-T2 (entrar a la evaluación).** `admin-api` expone `GET /internal/evaluaciones/{id}/` con la ventana, el límite, el estado calculado (HU64-T1) y los ids de los problemas visibles; así hay una sola regla de ventana (incluye la hora de inicio y excluye la de fin). `user-api` responde `GET /api/evaluaciones/{id}`: **200** con la ventana, el límite y los ids solo si el estado es `activa`; **403** antes (`programada`, «todavía no comienza») y después (`cerrada`, «ya terminó»), con las fechas pero sin problemas; **404**, igual que una evaluación inexistente, si el estudiante no está inscrito en el curso; **503** si `admin-api` no responde. Cualquier estado que no sea `activa` cierra la puerta. Esta ruta es la entrada: los problemas de la evaluación siguen siendo problemas normales del curso y se abren por las rutas de siempre; lo que se cierra por tiempo dentro de la evaluación (el intento y su límite de minutos) es HU64-T3.
>
> **Hecho en HU64-T3 (cierre por tiempo).** `user-api` guarda cuándo empezó cada estudiante su intento en `app_schema.app_intento_evaluacion` (migración V9: `estudiante_id`, `evaluacion_id` sin llave foránea porque la evaluación vive en `admin_schema`, `fecha_inicio`, y un solo intento por estudiante y evaluación). El reloj empieza la primera vez que el estudiante entra o envía código dentro de la ventana, y no se reinicia al volver a entrar. El cierre es perezoso, sin tarea programada: cada vez que entra o envía se compara la hora actual con el **menor** entre `inicio del intento + limite_minutos` y el fin de la ventana (`PlazoDelIntento`). Si ya pasó, `GET /api/evaluaciones/{id}` y `POST /api/problemas/{id}/envios` con `evaluacion_id` responden **403** `tiempo_agotado` («Se acabó el tiempo de tu evaluación.»), sin problemas, sin ejecutar el código y sin guardar el envío. El envío con `evaluacion_id` pasa por la misma puerta (`AccesoEvaluacionServicio`) y además exige que el problema sea de esa evaluación (404 si no); sin `evaluacion_id` es un envío de práctica y no depende de ningún reloj. Entrar fuera de la ventana o sin estar inscrito no crea ningún intento. Pendiente: el envío todavía no guarda a qué evaluación pertenece (hará falta para calificar la evaluación, HU65).
>
> **Alternativas descartadas:** una tarea programada que cierre los intentos vencidos (scheduler, concurrencia entre instancias y pruebas con tiempo simulado) y el híbrido de cierre perezoso más barrido de limpieza (solo tendría sentido con reportes que lean la columna directamente).

## **ADR-9: Detalles del último sprint (decidido el 20 de noviembre de 2026)**

### D12. Alcance del Sprint 4

> **Decidido.** Es el último sprint: nada «pasa al Sprint 5». El plan completo se trabaja **en orden de niveles** (0, 1, 2, …), sin un corte acordado de antemano. Lo que no esté terminado al cierre se declara **no entregado** en la memoria y en la demostración, en lugar de entregarse a medias. El alcance mínimo recomendado en `docs/roadmap-sprint-4.md` (niveles 0 a 3) sirve de referencia para saber qué no puede faltar.
>
> **Alternativas descartadas:** fijar el corte antes de empezar (el equipo prefirió decidirlo con el avance real) y arrastrar lo pendiente a un sprint posterior (no existe).

### D13. Tema de un problema (HU56 y siguientes)

> **Decidido.** Un campo opcional `tema` en `Problema` (texto corto, hasta 60 caracteres). La IA lo propone al generar el problema y el docente puede cambiarlo; vacío se muestra como «General».
>
> **Hecho (H4).** `Problema.tema` (migración `0011_problema_tema`), en el serializador del docente y en las dos rutas internas que consume `user-api` (el listado del curso y el detalle del problema); `user-api` ya ignora los campos que no conoce, así que lo usará HU56-T1 cuando lo necesite. `ai-service` lo pide en el prompt (`"tema"`: una a tres palabras) y lo devuelve en cada problema generado; si la IA no lo manda, queda vacío, y si se pasa de 60 caracteres se **recorta** en lugar de rechazar la tanda. El docente lo escribe, opcional, en el formulario de «Nuevo problema». **Límite conocido:** la bandeja de revisión de `admin-web` todavía no edita problemas, así que el tema que propone la IA se corrige por la API (`PUT /api/problemas/{id}/`, que ya lo admite) y no desde una pantalla.
>
> **Alternativas descartadas:** una tabla de temas por curso (migraciones, pantallas de administración y llaves foráneas que el último sprint no puede pagar) y deducir el tema del título o de las etiquetas (poco fiable).

### D14. Dónde se guardan los intentos de los ejercicios móviles (HU45-T3)

> **Decidido.** Cada intento es **una fila de `app_envio`** con `plataforma = 'movil'`: la respuesta escrita (o el texto de la alternativa elegida) va en `codigo`, `resultado` es `correcto` o `incorrecto` y `puntaje` es 100 o 0. Así el historial, el avance (D9) y el resumen del docente (D10) leen una sola tabla y no hay otra que sincronizar. **Todavía no se guardan:** H2 solo corrige; guardar el intento y limitar cuántos hay es HU45-T3.
>
> **Alternativas descartadas:** una tabla aparte para intentos móviles (duplica las consultas de avance, historial y resumen).

### D15. Cómo se corrigen los ejercicios móviles (H1, H2, HU45-T1 y HU46-T2)

> **Decidido.** Se corrige en `user-api`, sin IA y sin sandbox (ADR-2). Como `user-api` no puede leer `admin_schema` (ADR-1) y la ruta del estudiante nunca trae la respuesta, `admin-api` ofrece una ruta interna nueva, `GET /internal/problemas/{id}/respuesta/` (con `X-Internal-Key`, mecanismo de D3), que devuelve `id`, `curso_id`, `tipo_ejercicio`, `respuesta_esperada` y las alternativas con su `es_correcta`. Solo responde por problemas visibles para el estudiante (`visibles_para_estudiante()`, D5): uno sin aprobar, descartado o inexistente da el mismo 404.
>
> **Ruta única.** `POST /api/problemas/{id}/respuesta` con `{respuesta}` (predicción de salida) o `{opcion_id}` (opción múltiple) y respuesta `{resultado: "correcto" | "incorrecto", respuesta_correcta}`. `respuesta_correcta` aparece solo si falló. El acceso es el de abrir un problema (HU31-T2): no visible o curso sin inscripción da 404, y la respuesta esperada solo se pide a `admin-api` cuando el estudiante ya puede responder. Errores: 400 (falta la respuesta o la alternativa, alternativa de otro ejercicio, respuesta de más de 1 000 caracteres, ejercicio que no se resuelve por aquí), 404, 503 (`admin-api` no disponible). `completar_espacio` (HU47-T2) se corrige con la misma normalización que la predicción de salida: lo que el estudiante escribe es lo que llena el hueco, así que «  RANGE(10) » acierta con `range(10)`, pero `range (10)` no (los espacios de en medio se reducen, no se quitan).
>
> **Normalización (HU45-T1).** Se compara el texto sin espacios al inicio y al final, con los espacios repetidos reducidos a uno, sin distinguir mayúsculas de minúsculas; un salto de línea cuenta como espacio. Los signos importan: «Hola, mundo» no es «Hola mundo». Una respuesta en blanco, o un ejercicio sin respuesta esperada, nunca acierta.
>
> **Alternativas (HU46-T2, CA-46.4).** El detalle del problema trae las alternativas con `id` y `texto`, **mezcladas en cada apertura**, y nunca `es_correcta`: la correcta no se puede identificar antes de responder. Los textos de todas las alternativas sí viajan (hace falta para mostrarlas). Se cambió una prueba de `admin-api` que exigía que el texto de la correcta no viajara: lo que no viaja es la marca.
>
> **Alternativas descartadas:** meter `respuesta_esperada` en la ruta del estudiante (la expone a cualquiera que abra la red del navegador), que `user-api` lea `admin_schema` (rompe ADR-1) y una ruta por tipo de ejercicio (la app tendría que conocer el tipo antes de enviar).

### D16. Token de notificaciones del dispositivo (HU52-T1)

> **Decidido.** La app móvil registra el token de Firebase Cloud Messaging con `PUT /api/dispositivo` y `{token}` (204; 400 si falta o pasa de 1 024 caracteres). `user-api` lo guarda en `app_schema.app_dispositivo` (migración V10: `estudiante_id` con llave foránea a `app_estudiante`, `token` único, `fecha_registro`). El estudiante sale del token de sesión, nunca del cuerpo. Es un PUT porque repetirlo no cambia nada, y si otro estudiante inicia sesión en el mismo teléfono el token **cambia de dueño** (upsert por token); un estudiante puede tener varios teléfonos.
>
> **Alcance.** Esta entrega es solo la parte de `user-api`. Obtener el token en la app, las dependencias de Firebase y enviar una notificación de prueba desde la consola de Firebase quedan para la app móvil (guía en `docs/notificaciones-push.md`). Formalmente HU52-T1 depende de HU49-T1 y de H5.
>
> **Alternativas descartadas:** una columna `token` en `app_estudiante` (un solo teléfono por persona y un token que sobrevive al cambio de dueño) y enviar las notificaciones desde `user-api` en este sprint (necesita credenciales de servicio de Firebase que el repositorio no debe guardar).

### D17. Cómo se marca el espacio en blanco de «completar el espacio» (HU47-T1)

> **Decidido.** El **fragmento de código va en el `enunciado`**, igual que ya lo genera la IA, y el espacio en blanco es **una racha de tres o más guiones bajos** (`___`, `____`…). La **respuesta esperada va en `respuesta_esperada`**: el texto exacto que completa el hueco. Un problema `completar_espacio` es «completo» solo si su enunciado trae **exactamente un** hueco y tiene `respuesta_esperada`; la regla es la de `problemas/completitud.py`, la misma al guardar lo que genera la IA (se rechaza la tanda entera), al crear a mano (nace sin aprobar) y al aprobar (400 con el error en `enunciado`). Dos guiones bajos (`__init__`) no son un hueco. En los demás tipos el enunciado puede llevar guiones bajos sin restricción.
>
> **Para la app (HU47-T3).** Parte el enunciado en la racha de guiones bajos y pone el campo de texto en ese lugar; la respuesta esperada nunca viaja al estudiante (se corrige en `user-api`, D15; la normalización simple es HU47-T2).
>
> **Alternativas descartadas:** `codigo_base` para el fragmento (obliga a cambiar el prompt y el esquema de `ai-service` y deja sin fragmento los problemas ya generados) y varios huecos con varias respuestas (HU47-T2 y HU47-T3 asumen una sola comparación).

### D18. Cinturones, niveles y puntos (HU50-T1, HU50-T2)

> **Decidido.** Hay **un cinturón por estudiante y lenguaje** (`app_schema.app_cinturon`, migración V11: `estudiante_id` con llave foránea a `app_estudiante`, `lenguaje`, `nivel`, `puntos`, `fecha_actualizacion`, único por estudiante y lenguaje). Los niveles viven en una tabla de referencia, `app_nivel_cinturon`, para que los umbrales se cambien con una migración y no con código: 1 blanco (0 puntos), 2 amarillo (100), 3 naranja (250), 4 verde (500), 5 azul (900), 6 marrón (1 400) y 7 negro (2 000). El nivel es el más alto cuyo umbral ya se alcanzó, se calcula al sumar y nunca baja; el negro es el tope. El cinturón nace al primer acierto del estudiante en ese lenguaje, en blanco y con los puntos de ese ejercicio.
>
> **Puntos (HU50-T2).** Solo el **primer acierto** de cada ejercicio suma, y por eso existe `app_puntos_ejercicio` (V13: único por estudiante y ejercicio, guarda el lenguaje y los puntos que se dieron). Repetir un ejercicio ya resuelto, equivocarse o un envío parcial no suman. Se suma tanto en la ruta de envíos (`escritura_codigo`, cuando el resultado es `correcto`) como en `POST /api/problemas/{id}/respuesta` (ejercicios móviles). La tabla aparte sirve porque los intentos móviles todavía no se guardan en `app_envio` (D14, HU45-T3 pendiente).
>
> **Valores provisionales** (los confirma HU50-T4, `docs/puntos-cinturon.md`; están en `PuntosPorEjercicio`, un solo lugar): `escritura_codigo` 30, `prediccion_salida` 10, `opcion_multiple` 10, `completar_espacio` 10.
>
> **Alternativas descartadas:** sumar por cada acierto (se podría repetir el mismo ejercicio para subir de cinturón), derivar los puntos de `app_envio` en cada consulta (los ejercicios móviles no están ahí) y un cinturón único sin lenguaje (el requisito es «del lenguaje correspondiente»).

### D19. Sesión del Dojo y siguiente ejercicio (HU53-T1, HU53-T2)

> **Decidido.** Una sesión del Dojo es una fila de `app_sesion_dojo` (V12: estudiante, lenguaje, `fecha_inicio`, `fecha_fin` nula mientras está abierta); cada ejercicio entregado queda en `app_sesion_dojo_ejercicio` (`sesion_id`, `problema_id` sin llave foránea porque el problema vive en `admin_schema` (ADR-1), `orden`, `fecha_entrega`, `acerto` vacío hasta que se responda). Una restricción única por sesión y ejercicio es lo que impide repetir.
>
> **Rutas.** `POST /api/dojo/sesiones` con `{lenguaje}` (`python`, `java` o `javascript`) crea la sesión y responde 201 `{id, lenguaje}`. `POST /api/dojo/sesiones/{sesionId}/siguiente` entrega un ejercicio con el mismo detalle de `GET /api/problemas/{id}` (alternativas sin `es_correcta`, sin respuesta esperada), de modo que la app lo dibuja con las pantallas que ya tiene. Si ya no quedan ejercicios responde **204** sin cuerpo; sesión ajena o inexistente 404; sesión terminada 409; `admin-api` caído 503.
>
> **Qué se entrega.** Ejercicios móviles (`plataforma` móvil o ambas, **nunca** `escritura_codigo`) y visibles de los cursos donde el estudiante está inscrito, del lenguaje de la sesión y que no se hayan entregado ya en esa sesión; se elige uno al azar. Otra sesión sí puede repetir ejercicios. Cerrar la sesión (`fecha_fin`) y guardar si acertó (`acerto`) quedan para HU53-T3 y HU53-T4.
>
> **Alternativas descartadas:** orden fijo (el Dojo sería siempre igual), guardar la lista de ejercicios al abrir la sesión (se queda vieja si el docente aprueba o archiva problemas) y una ruta propia para el detalle (la app ya sabe pintar el del problema).

## **ADR-10: Duelos y marcador en vivo (HU57 a HU60; decidido el 20 de noviembre de 2026)**

### D20. Mecanismo de tiempo real del marcador y regla de desconexión (HU59-T1)

> **Decidido.** El marcador se mantiene al día con **consulta periódica** (*polling*): mientras la pantalla de la partida está abierta, la app pide `GET /api/partidas/{id}/marcador` cada **2 segundos**, con el mismo JWT del resto de la API. La respuesta trae el marcador de los dos jugadores, de qué ejercicio va cada uno, el estado de la partida (`en_curso`, `terminada`) y el estado de conexión del rival. Se descartan Server-Sent Events y WebSocket.
>
> **Por qué.** Es lo único de las tres opciones que no pide nada nuevo: es una ruta más de `user-api` bajo la seguridad que ya existe, sin hilos abiertos por jugador, sin estado de conexión en memoria (que se perdería al reiniciar el servicio) y sin proxy que mantenga conexiones largas. Aguanta bien la conexión lenta (HU59-T5): una consulta que falla se reintenta sola y nada se queda a medias. Y 2 s se siente «en vivo» para un duelo de ejercicios cortos, donde cada respuesta tarda mucho más que eso.
>
> | Criterio | Consulta periódica (elegida) | Server-Sent Events | WebSocket |
> | :--- | :--- | :--- | :--- |
> | Cambios en el servidor | Una ruta GET | Un `SseEmitter` por jugador y su limpieza | Configuración del *handshake* con JWT, sesiones y mensajes |
> | Seguridad | La del resto de la API | La cabecera con el JWT hay que armarla a mano en el cliente | El JWT no viaja en cabecera en el *handshake* del navegador; hay que inventar otro camino |
> | Estado en el servidor | Ninguno (todo en la base) | Conexiones abiertas en memoria | Sesiones abiertas en memoria |
> | Conexión lenta o con cortes | Se reintenta sola | Hay que programar la reconexión y qué se perdió | Igual que SSE, más latido (*ping*) |
> | Cómo se prueba | Con `MockMvc`, como todo lo demás | Pruebas asíncronas con tiempos | Cliente WebSocket de prueba |
> | Lo que cuesta | Más peticiones (una cada 2 s por jugador, unas 60 por minuto en total para dos) | Menos peticiones | Menos peticiones |
>
> **Cuidado con la conexión lenta.** La app no lanza una consulta nueva hasta que termina la anterior (no se acumulan). Si una falla, espera el doble (2 → 4 → 8 s, tope 10 s) y vuelve a 2 s con la primera que responda. Si la respuesta no cambió, `user-api` contesta **304** sin cuerpo (ETag), para que una red mala solo mueva cabeceras.
>
> **Regla de desconexión (CA-59.3 y CA-59.4).** Un jugador tiene **30 segundos para volver**; si no vuelve, **pierde la partida**. Con consulta periódica no hay una conexión que se cierre y avise: lo que se observa es la **última señal**, que es la última petición del jugador sobre esa partida (consultar el marcador o responder un ejercicio). Por eso:
>
> 1. Cada petición de un jugador sobre su partida guarda `ultima_senal` en su fila de la partida.
> 2. A los **10 segundos** sin señal el rival ve «tu rival se desconectó» (el marcador trae `rival_conectado: false`); no pasa nada más.
> 3. Los 30 segundos se cuentan **desde la última señal** (con consulta cada 2 s, es la mejor estimación de cuándo se cayó). Si el jugador vuelve antes, la partida sigue donde estaba, porque todo el estado vive en el servidor.
> 4. **No hay un temporizador en el servidor.** La regla se evalúa cada vez que alguien consulta o responde en esa partida: si el rival lleva más de 30 s sin señal y la partida sigue `en_curso`, se cierra como ganada por quien sigue presente (motivo `abandono`). El que se había caído ve la partida perdida cuando vuelve a consultar. Si **los dos** pasan de 30 s sin señal, se cierra como empate por abandono; el criterio de los empates en general es de H9.
> 5. Una partida abandonada cuenta como ganada para el otro en el ranking (HU60).
>
> **Para las tareas que vienen.** HU57-T1 debe crear la columna `ultima_senal` (marca de tiempo, nula hasta la primera petición) en la fila de cada jugador de la partida; HU59-T2 hace la ruta del marcador y guarda la señal; HU59-T4 implementa el paso 4 y las pruebas con un plazo corto configurable (los 30 s y los 10 s van en `application.yml`, no escritos en el código, como la caducidad del reto de HU57-T4); HU59-T3 hace la consulta cada 2 s en la app y HU59-T5 prueba la conexión lenta.
>
> **Alternativas descartadas:** Server-Sent Events y WebSocket (más inmediatos, pero piden estado en memoria, pruebas asíncronas y una seguridad aparte que el último sprint no puede pagar; si el proyecto sigue después, se pueden cambiar sin tocar la regla de desconexión, que solo depende de `ultima_senal`), un temporizador en el servidor que cierre la partida a los 30 s (obliga a un proceso en segundo plano que sobreviva reinicios, y la evaluación al consultar da el mismo resultado visible) y contar los 30 s desde que se detecta la desconexión (con consulta periódica nunca se «detecta» nada: solo se mide el silencio).

## **ADR-11: Dificultad y resumen del Dojo (HU49, HU54 y HU56; decidido el 20 de noviembre de 2026)**

### D21. Dónde viven el nivel, las rachas y el resumen de la sesión (HU54-T2/T3/T4, HU56-T1/T3, HU49-T1)

> **Decidido.**
>
> 1. **La regla es una función pura** (`ReglaDificultad`, docs/regla-dificultad.md). Su estado (nivel y las dos rachas) vive **en la sesión** (`app_sesion_dojo.nivel`, `racha_aciertos`, `racha_errores`, migración V15), así sobrevive si la app se cierra y se reanuda.
> 2. **Una respuesta pertenece a una sesión si trae `sesion_id`.** La ruta es la de siempre (`POST /api/problemas/{id}/respuesta`, D15) con un campo opcional `sesion_id`. Con él, el ejercicio debe estar entregado y pendiente en esa sesión abierta del estudiante (404 si la sesión no es suya, 409 si terminó o el ejercicio ya se respondió); el resultado se apunta (`acerto`, `tema`) y mueve el nivel. Sin `sesion_id` la ruta se comporta como antes y no toca ninguna sesión. Una respuesta inválida (400) no cuenta.
> 3. **El siguiente ejercicio** sale del nivel de la sesión; si no queda ninguno ahí, del nivel más cercano (a igual distancia, el más fácil). La sustitución no cambia el nivel.
> 4. **Terminar la sesión** es `POST /api/dojo/sesiones/{id}/terminar`: cierra la sesión, **guarda el nivel alcanzado** en `app_nivel_dificultad` (uno por estudiante y lenguaje, HU54-T3) y devuelve el resumen. Una sesión que nunca se termina no guarda nivel: la siguiente parte del último nivel guardado.
> 5. **Nivel inicial (HU54-T4): `facil`.** Quien no tiene nivel guardado en ese lenguaje parte de `facil`; quien lo tiene, de ese nivel.
> 6. **Los puntos no se suman dos veces (HU56-T3).** HU50-T2 ya suma al cinturón al acertar (solo la primera vez por ejercicio), también dentro del Dojo. Esperar al final para sumarlos duplicaría la lógica y perdería los puntos de una sesión abandonada. Por eso «sumar los puntos de la sesión al cinturón» queda cubierto así: los puntos llegan al cinturón en cada acierto y el resumen **informa** cuántos sumó la sesión (`puntos_sumados`, leído del registro de puntos de ese ejercicio desde que empezó la sesión) y el cinturón que queda (`cinturon`, o `null` si todavía no tiene).
> 7. **Temas con más fallos (HU56-T1).** Se apunta el `tema` del problema (D13) al responder; el resumen trae hasta 3 temas con sus fallos, de más a menos y, a igual cantidad, por nombre. Sin tema cuenta como «General».
> 8. **Racha de práctica (HU49-T1):** tabla `app_racha` (V14), una fila por estudiante y lenguaje con racha actual, máxima y fecha del último día practicado. Esta tarea solo crea la tabla; quien la actualiza es HU49-T2.
>
> **Resumen (respuesta de `terminar`).** `{ "nivel_alcanzado", "respondidos", "aciertos", "puntos_sumados", "temas_mas_fallados": [{ "tema", "fallos" }], "cinturon": { "nivel", "puntos" } | null }`.
>
> **Para las tareas que vienen.** La app del celular debe mandar `sesion_id` al responder dentro del Dojo y llamar a `terminar` al salir (HU55 usa el resumen). HU54-T5 prueba la regla (ya hay pruebas de la función pura en `ReglaDificultadTest`; faltan los casos largos que pida esa tarea). `tema` llega de admin-api en el detalle y en el listado de cada curso; `user-api` no se lo muestra al estudiante.
>
> **Alternativas descartadas:** una ruta nueva solo para responder dentro de la sesión (duplica la corrección de D15); calcular el nivel releyendo todas las respuestas de la sesión (más lento y la regla ya es incremental); sumar los puntos del cinturón solo al terminar (ver el punto 6).

## **ADR-12: Intentos, respuestas sin conexión y Dojo en el celular (HU45, HU46, HU48, HU53, HU54 y HU55; decidido el 20 de noviembre de 2026)**

### D22. Cuántos intentos hay, cómo se evita contar una respuesta dos veces y qué guarda el celular (HU45-T3, HU48-T3, HU53-T3, HU55-T1)

> **Decidido.**
>
> 1. **Intentos: 3 por ejercicio y por estudiante, en total** (todas las sesiones juntas; constante `IntentosMoviles.MAXIMO`). Se cuentan las filas de `app_envio` con plataforma `movil` de ese estudiante y ese problema (D14). Con los 3 gastados, `POST /api/problemas/{id}/respuesta` responde **409** («Ya usaste los 3 intentos de este ejercicio.») y el Dojo deja de entregar ese ejercicio. El detalle del problema, la entrega del Dojo y la respuesta traen `intentos_restantes` (solo en ejercicios móviles; en `escritura_codigo` no sale). La pantalla muestra «Te quedan N intentos».
> 2. **Cada intento es una fila de `app_envio`** (plataforma `movil`, puntaje 100 si acertó y 0 si no, `codigo` = el texto escrito o `opcion:<id>` de la alternativa), por eso aparece en el avance de la web (CA-24.4) y los puntos del cinturón se siguen sumando solo en el primer acierto de cada ejercicio (D18).
> 3. **Una respuesta se registra una sola vez aunque se reenvíe (CA-48.5).** La app manda un `intento_id` (una UUID) con cada intento. Migración V16: columna `app_envio.clave_cliente` con un índice único parcial por `(estudiante_id, clave_cliente)`. Un reenvío con la misma clave **no gasta intento, no vuelve a sumar puntos ni a mover el nivel, no exige que la sesión siga abierta** (la respuesta original pudo haberse registrado sin que la app lo supiera) y devuelve otra vez el resultado corregido. Sin `intento_id` la ruta se comporta como antes. Más de 40 caracteres: 400.
> 4. **Sin conexión (HU48-T3), sin dependencias nuevas:** la app guarda las respuestas que no pudo enviar como JSON en `SharedPreferences` (en el orden en que se dieron) y las reenvía **al abrir la app y cuando Android avisa de que volvió la red** (`ConnectivityManager`, con el permiso normal `ACCESS_NETWORK_STATE`). Reenvía en orden y se detiene en la primera que no pueda (sin conexión, sesión vencida o error del servidor); lo que el servidor rechaza (400, 404, 409) se descarta porque reenviarlo no serviría. La pantalla de ese ejercicio pasa sola de «guardada» al resultado cuando llega.
> 5. **Estado de la sesión del Dojo (HU55-T1):** también en `SharedPreferences`, una sola sesión (la última): `sesionId`, lenguaje, ejercicio actual, aciertos y respondidos. Se guarda en cada cambio y al salir de la pantalla (atrás, segundo plano). Se borra si el servidor dice que la sesión ya no existe o terminó.
> 6. **Dentro del Dojo cada ejercicio se responde una vez** (D21, punto 2): tras fallar no hay «Intentar de nuevo». Fuera del Dojo sí, mientras queden intentos.
> 7. **La renovación del token es de una a la vez** (`AuthRepository.renovar`): el refresh rota, y el reenvío en segundo plano puede coincidir con «Mis cursos»; sin esto, el segundo pedido recibiría 401 y cerraría la sesión sin motivo.
>
> **Límites conocidos.** Si el teléfono pierde la señal y el sistema cierra la app, el reenvío espera a que el estudiante la abra otra vez (no hay `WorkManager`). Una respuesta guardada que llega cuando la pantalla ya se cerró no suma al contador de aciertos del Dojo de esa visita (sí queda registrada en el servidor). HU55-T2 (retomar la sesión guardada) y HU55-T3 (botón «Terminar» y resumen) se resolvieron después: ver D24.
>
> **Alternativas descartadas:** Room o WorkManager (dependencias nuevas que el equipo no ha aprobado; SharedPreferences alcanza para una lista corta), 3 intentos por sesión (permitiría repetir sin fin el mismo ejercicio en sesiones nuevas), reconocer el reenvío por el contenido de la respuesta (dos respuestas iguales de forma legítima se confundirían) y dejar el reenvío solo a una acción manual del estudiante (se olvidaría).

## **ADR-13: Zona horaria, racha, cinturón en el perfil y partidas (H6, HU49, HU50, HU51 y HU57; decidido el 21 de noviembre de 2026)**

### D23. Cómo se cuenta el día del estudiante, qué cuenta para la racha y cómo se guarda una partida (H6, HU49-T2/T4, HU50-T4, HU51-T1, HU57-T1)

> **Decidido.**
>
> 1. **Zona horaria (H6).** La app manda la zona del teléfono (por ejemplo `America/Lima`) con `PUT /api/yo/zona-horaria` y cuerpo `{ "zona_horaria": "..." }`; user-api responde **204**. Se guarda en `app_estudiante.zona_horaria` (migración V17, **nula** hasta que la app la manda). Solo se aceptan **nombres de región** de la base de zonas de Java (o `UTC`): un desfase fijo como `+05:00` o `GMT-5` se rechaza con 400, porque no sigue los cambios de hora de la región y el día local quedaría mal. Mientras no haya una guardada, se usa la de por defecto: **`America/Lima`**, configurable con `estudiante.zona-horaria-por-defecto` (variable `ZONA_HORARIA_POR_DEFECTO`). La app la manda **cada vez que se abre con la sesión iniciada**; si falla, no avisa y reintenta en la próxima apertura. Si el estudiante viaja, la siguiente apertura la actualiza.
> 2. **Qué cuenta para la racha (HU49-T4, CA-49.1).** **Un acierto**, en el celular o en la web (también dentro del Dojo): al registrarse un acierto se anota el día. Un intento fallido o parcial **no** mantiene la racha. Basta un acierto en el día; varios el mismo día cuentan como uno.
> 3. **La racha es por lenguaje** (`app_racha`, V14, una fila por estudiante y lenguaje; sin migración nueva). «Hoy» es el **día local del estudiante** (HU49-T2, CA-49.2): a las 23:30 en Lima ya es el día siguiente en UTC, pero para él sigue siendo hoy.
> 4. **Cuándo se corta (CA-49.4).** El último acierto fue hoy o ayer: la racha **sigue viva** (si fue ayer todavía puede sumar hoy). Fue antes de ayer: pasó un día completo sin acierto y la racha vale **0**. Eso se calcula **al leer**: la fila guardada solo se reinicia (a 1) en el siguiente acierto; no hay proceso en segundo plano. La racha máxima nunca baja. Si la fecha guardada es posterior a «hoy» (el estudiante viajó hacia el oeste y su día local retrocedió), se trata como el mismo día: la racha no se pierde por viajar.
> 5. **Una sola sentencia SQL** (`INSERT ... ON CONFLICT DO UPDATE` en `RachaRepository`) decide si el acierto no cambia nada, suma uno o reinicia: dos aciertos a la vez no se pisan.
> 6. **`GET /api/yo/racha`** (HU49-T3 y HU52-T2 la usan): `{ "racha_actual", "practico_hoy", "por_lenguaje": [{ "lenguaje", "racha_actual", "racha_maxima", "practico_hoy" }] }`. La `racha_actual` de la pantalla principal es la **más alta entre los lenguajes**; `practico_hoy` es verdadero si practicó hoy en cualquiera.
> 7. **Puntos por tipo de ejercicio (HU50-T4, CA-50.2), confirmados:** `escritura_codigo` 30; `prediccion_salida`, `opcion_multiple` y `completar_espacio` 10 cada uno. **Cinturones (D18) confirmados:** blanco 0, amarillo 100, naranja 250, verde 500, azul 900, marron 1400 y negro 2000 puntos. Quedan escritos en `docs/puntos-cinturon.md`; la prueba `PuntosCinturonDocTest` lee las tablas del documento y falla si no coinciden con `PuntosPorEjercicio` ni con `app_nivel_cinturon`. `CinturonSubidaDeNivelTest` comprueba el nivel un ejercicio antes de cada umbral y justo al llegar (CA-50.3).
> 8. **Lo que falta para el siguiente cinturón (HU51-T1, CA-51.1 y CA-51.2).** `GET /api/yo/cinturones` devuelve, por cada lenguaje en que el estudiante ya acertó: `{ "lenguaje", "nivel", "nombre", "puntos", "siguiente": { "nivel", "nombre", "puntos_requeridos" } | null, "puntos_faltantes" | null }`. Con el negro, `siguiente` y `puntos_faltantes` van en `null` (las claves siguen en la respuesta). Sin ningún acierto, la lista va vacía. Los umbrales salen de `app_nivel_cinturon`, no del código.
> 9. **Partida (HU57-T1, CA-57.2).** Migración V18, dos tablas. `app_partida`: curso (sin clave foránea, ADR-1), `estado` (`pendiente`, `en_curso`, `terminada`, `rechazada`, `caducada`), fechas de creación, inicio y fin, `ganador_id` y `motivo_fin` (`completada` o `abandono`, D20). Solo una partida **terminada** puede tener ganador y motivo; un empate termina con `ganador_id` nulo. `app_partida_jugador`: **una fila por jugador** con `es_retador`, `puntaje`, `respondidos` y **`ultima_senal`** (nula hasta la primera petición, como pide D20). Un estudiante aparece una sola vez por partida y hay un solo retador por partida.
> 10. **Lo que cuida el código y no la base.** Que los dos jugadores estén inscritos en el curso y que sean exactamente dos (HU57-T2, al crear el reto); la **caducidad a las 24 horas** (HU57-T4) se evalúa al leer, sin proceso en segundo plano y sin columna propia; el criterio de empate y el valor de cada respuesta son de H9.
>
> **Para las tareas que vienen.** HU49-T3 (racha en la pantalla principal) y HU52-T2 (aviso diario) usan `GET /api/yo/racha`; HU51-T2 y HU51-T3 (barra de avance y mensaje del negro) usan `GET /api/yo/cinturones`; HU57-T2 crea el reto sobre las tablas de V18.
>
> **Límites conocidos.** La zona solo se actualiza al abrir la app: quien viaja y no la abre sigue con la anterior hasta entonces. Los aciertos anteriores a esta versión no tienen racha (la tabla estaba vacía). Un ejercicio sin lenguaje no cuenta para ninguna racha.
>
> **Alternativas descartadas:** una racha global sin lenguaje (se decidió por lenguaje, como el cinturón); contar cualquier intento como actividad (premiaría fallar a propósito); deducir la zona de la IP o de una cabecera (no es fiable y la app ya la conoce); aceptar desfases fijos (rompen con el cambio de hora); un proceso que reinicie las rachas a medianoche (obliga a un segundo proceso y a una zona por estudiante); guardar a los dos jugadores como columnas de `app_partida` (D20 pide `ultima_senal` en la fila de cada jugador y las consultas por estudiante, HU60-T1, son más simples con una fila por jugador).

## **ADR-14: Retomar y terminar la sesión del Dojo, token del dispositivo y avisos en el celular (HU55, HU52; decidido el 22 de noviembre de 2026)**

### D24. Cómo se retoma una sesión, cómo se termina desde el celular y dónde viven el token y los ajustes de avisos (HU55-T2/T3/T4, HU52-T1/T3)

> **Decidido.**
>
> 1. **Retomar con una ruta de lectura, no con `siguiente`.** `GET /api/dojo/sesiones/{id}` devuelve `{ "id", "lenguaje", "nivel", "abierta", "aciertos", "respondidos", "ejercicio_actual" }`. `ejercicio_actual` es el **último ejercicio entregado que todavía no se respondió** (la última fila de `app_sesion_dojo_ejercicio` de esa sesión con `acerto` nulo), con el mismo formato y las mismas garantías de la entrega del Dojo: sin respuesta esperada ni marca de la alternativa correcta, con `intentos_restantes`. Solo lee: no entrega ni apunta nada, así que pedirla dos veces da lo mismo. Va en `null` si no hay ninguno pendiente, si la sesión ya cerró (`abierta` en `false`) o si ese ejercicio dejó de ser visible (entonces la app pide el siguiente). La sesión de otro estudiante o una que no existe: **404**. Si admin-api no responde: **503**.
> 2. **Por qué no reusar `siguiente`.** `POST .../siguiente` siempre entrega un ejercicio **nuevo** y lo apunta como entregado. Retomar con él haría saltar el ejercicio que se dejó a medias (y un ejercicio que la app omite por no saber mostrarlo se perdería en cada reapertura). Con la ruta de lectura la reanudación es idempotente.
> 3. **Qué hace la app al volver (HU55-T2, CA-55.2).** Si quedó una sesión guardada (HU55-T1), el Dojo ofrece **Continuar** o **Empezar de nuevo**, y mientras el estudiante decide no se pisa lo guardado. **Continuar** pregunta a user-api cómo va la sesión y toma de ahí los aciertos y respondidos (lo guardado en el teléfono solo dice cuál es la sesión: pudo quedar viejo), y muestra `ejercicio_actual` o pide el siguiente. Si la sesión ya cerró o no existe, se borra lo guardado y se avisa. **Empezar de nuevo = terminar la anterior** (`POST .../terminar`, que guarda el nivel al que llegó, D21) y volver a elegir lenguaje. Sin conexión no se descarta nada: se avisa y se puede reintentar. La respuesta que quedó guardada por falta de señal (D22) se sigue resolviendo en la pantalla del ejercicio.
> 4. **Terminar desde el celular (HU55-T3, CA-55.3 y CA-55.4).** Botón **Terminar sesión** con confirmación («¿Terminar la sesión?»). Al confirmar llama a `POST .../terminar`, borra lo guardado y muestra un **resumen mínimo** (aciertos de respondidos y puntos sumados) con «Volver a mis cursos». Si el servidor dice 404 o 409, se trata como una sesión que ya no existe. Sin conexión o con error, la sesión sigue donde estaba, con un aviso. El estado que sigue al cierre no lleva `sesionId` para que, al salir de la pantalla, no se vuelva a guardar una sesión cerrada. El resumen completo (temas más fallados, cinturón) es de HU56-T2: reemplaza este diálogo y puede usar `ResumenDeSesion`, que ya trae esos datos.
> 5. **Token de notificaciones sin Firebase todavía (HU52-T1, D16).** La app ya llama a `PUT /api/dispositivo` (al abrir con sesión y, cuando exista el servicio de Firebase, al renovarse el token). El token sale de una interfaz `ProveedorDeToken`; hoy es `SinFirebase` (no hay token y no se manda nada), porque agregar el SDK necesita aprobar dependencias y el proyecto de Firebase (H5). Un token que no pudo enviarse (sin conexión, sesión vencida o error 5xx) queda guardado en `SharedPreferences` y se manda en la próxima apertura; un 400 se descarta. Desde Android 13 se pide `POST_NOTIFICATIONS` al abrir «Avisos de práctica» o al activar el interruptor. Las líneas de Gradle y el servicio de Firebase que faltan están en `docs/notificaciones-push.md`.
> 6. **Ajustes de avisos solo en el teléfono (HU52-T3, CA-52.3 y CA-52.4).** `SharedPreferences`: avisos **activados** por defecto y hora **20:00**. Se cambian desde «Mis cursos» → «Avisos de práctica».
>
> **Límites conocidos.** (a) `user-api` **no conoce** los ajustes de avisos: desactivarlos en la app no detiene un aviso que mande el servidor hasta que HU52-T2 los lea. La opción prevista es una ruta nueva (por ejemplo `PUT /api/yo/avisos`) que la app llame al cambiarlos; es de HU52-T2. (b) Un ejercicio omitido por ser de un tipo que el celular no muestra sigue sin responder en el servidor, así que al retomar vuelve a aparecer (y se puede pasar al siguiente). (c) Las pantallas de Compose (`DojoScreen`, `AvisosScreen`) no se compilaron al escribirlas; la lógica que las maneja sí tiene pruebas en la JVM. (d) HU52-T4 (probar el aviso en un teléfono real) y el recorrido C2 en un celular son manuales: `docs/recorrido-aceptacion-movil.md`.
>
> **Alternativas descartadas:** reanudar con `siguiente` (salta o pierde el ejercicio pendiente); guardar en el teléfono el ejercicio entero para mostrarlo sin red (el servidor es la fuente de los aciertos y de qué ejercicio sigue, y lo guardado podría quedar viejo); terminar la sesión sin confirmar (un toque sin querer cerraría la sesión y guardaría el nivel); agregar Firebase y WorkManager sin aprobación del equipo; mandar los ajustes de avisos al servidor en esta tarea (es una ruta nueva que decide quien haga HU52-T2).
