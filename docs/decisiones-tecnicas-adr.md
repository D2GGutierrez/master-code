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
> - `user-api`: `GET /internal/cursos/{cursoId}/inscritos` (email y fecha de cada inscrito). La usa `admin-api` en la lista de inscritos del docente (HU28-T1), que además comprueba que el curso sea del docente (403 si es de otro).

### D4. CORS de `user-api`

> **Decidido.** lista de orígenes permitidos por variable de entorno (`CORS_ALLOWED_ORIGINS`), con el mismo patrón que `admin-api` (HU22-T7). Sin esto, `user-web` (puerto 3001) no puede llamar a `user-api` (8081) desde el navegador.

### Ya decidido en `registro-cambios.md`

>Curso «publicado» = tiene al menos un problema aprobado (HU25-T1). El código de invitación vence a los 30 días y se genera también para los cursos existentes (HU26-T1). Límite de ejecuciones de prueba: 10 por minuto, configurable (HU06-T4).
