# Auditoría de casos de prueba ocultos (HU39-T3)

- **Fecha:** 2026-11-09
- **Actualización (2026-11-09):** se corrigieron los hallazgos de esta auditoría y de HU41-T4. La fuga de Judge0 quedó cerrada, ai-service quedó publicado solo en local y admin-api ahora limita la entrada de un caso. Ver «Hallazgos».
- **Rama auditada:** `gael-lajara`
- **Pregunta:** ¿algún caso de prueba oculto (`es_publico=False`) sale hacia el estudiante por una ruta, un mensaje de error o un log? Se revisan su entrada, su salida esperada y la salida que obtuvo el programa.

## Alcance

- **user-api, admin-api y ai-service:** el código de las rutas, los serializers, los mensajes de error y todas las llamadas de log.
- **Prueba con el stack levantado:** un docente crea un problema con un caso oculto cuya entrada y salida son la marca inventada `MARCA-OCULTA-123`. Un estudiante hace seis envíos que cubren eco correcto, error que repite la entrada, error de sintaxis, tiempo agotado, memoria excedida y fallo del motor. Después se consultan el historial y el detalle de cada envío, y se busca la marca en `docker compose logs` de los 11 servicios.
- **Los motores Judge0 y Piston:** se revisan aparte. ai-service les pasa la entrada de cada caso, y el stack tiene `SANDBOX_PROVIDER=mock`.
- **Fuera de alcance:** user-web, admin-web y mobile.

## Rutas que pueden devolver casos o envíos

| Servicio | Ruta | Archivo | Qué devuelve |
|---|---|---|---|
| user-api | `POST /api/problemas/{problemaId}/envios` | `envio/EnviosController.java:114` | El envío calificado. De un caso público: entrada, salida esperada, salida obtenida, error y estado. De uno oculto: solo `publico` y `paso`. |
| user-api | `GET /api/problemas/{problemaId}/envios` | `envio/EnviosController.java:83` | El historial (HU41): id, intento, fecha, resultado y puntaje. No lee `app_resultado_caso_prueba`. |
| user-api | `GET /api/envios/{envioId}` | `envio/EnviosController.java:104` | El detalle (HU42): lo mismo que el historial más el código enviado. No lee `app_resultado_caso_prueba`. |
| user-api | `GET /api/problemas/{problemaId}` | `problema/ProblemasController.java:172` | El problema con sus casos públicos, tal como llegan de la ruta interna de admin-api. |
| user-api | `POST /api/ejecuciones` | `ejecucion/EjecucionController.java:46` | «Ejecutar» con la entrada que escribe el estudiante. No toca ningún caso. |
| user-api | `GET /internal/envios/resumen` | `envio/ResumenEnviosInternoController.java:29` | Cifras por problema (intentos y correctos). Requiere `X-Internal-Key`. |
| admin-api | `GET /internal/problemas/{id}/calificacion/` | `problemas/interno.py:46` | **Todos los casos, ocultos incluidos.** Solo para que user-api califique. Requiere `X-Internal-Key`. |
| admin-api | `GET /internal/problemas/{id}/` | `problemas/interno.py:38` | El problema con los casos públicos solamente (`serializers.py:187`). |
| admin-api | `GET /internal/cursos/{id}/problemas/` | `problemas/interno.py:22` | El listado del curso, sin casos. |
| admin-api | `POST /api/cursos/{id}/problemas/`, `…/generar/`, `GET /api/problemas/pendientes/`, `GET/PUT /api/problemas/{id}/`, `POST …/aprobar/`, `POST …/descartar/` | `problemas/views.py` | Para el docente dueño del curso, con todos los casos. Un estudiante recibe 403 y un docente ajeno, 404. |
| admin-api | `/admin/` | `config/urls.py:8` | El sitio de administración de Django. No registra ningún modelo de problemas (no existe ningún `admin.py`). |
| ai-service | `POST /execute` | `app/routers/ejecucion.py:33` | El resultado de una ejecución: estado, salida, error y tiempo. Recibe la entrada de los casos ocultos y no guarda nada. |
| ai-service | `POST /generate` | `app/routers/generacion.py:21` | Problemas generados por la IA, que todavía no tienen id ni dueño. |

ai-service no tiene rutas `/internal/` ni `/admin/`.

## Resultado por canal

| Canal | user-api | admin-api | ai-service | Motor (Judge0 / Piston) |
|---|---|---|---|---|
| 1. Rutas al estudiante | **OK** | **OK** | **OK** | — |
| 2. Errores (4xx, 5xx, timeout, sandbox) | **OK** | **OK** | **OK**, con una observación | — |
| 3. Logs | **OK** | **OK** | **OK** | **FUGA en Judge0, CERRADA:** su log está desactivado en el compose. Piston sin verificar. |
| 4. Rutas internas | **OK** | **OK** | **OK** | — |

### 1. Rutas

- **user-api, envío:** un caso oculto se construye con `new CasoCalificado(false, paso, null, null, null, null, null)` (`EnviosController.java:176-177`), y `@JsonInclude(NON_NULL)` (`:56`) quita del JSON los campos nulos. La salida oculta se guarda en `app_resultado_caso_prueba` (HU37-T1, `:174`), pero no viaja en la respuesta.
- **user-api, historial y detalle:** las consultas `delEstudiante` (`EnvioRepository.java:136`) y `detalle` (`:105`) solo leen `app_envio`. Ninguna ruta del estudiante lee `app_resultado_caso_prueba`.
- **admin-api:** la ruta que alimenta la vista del estudiante filtra con `es_publico=True` (`serializers.py:187`). La única que entrega los ocultos es `/internal/…/calificacion/` (`serializers.py:208-216`).
- **Prueba en vivo:** la marca aparece 0 veces en las 20 respuestas, que incluyen los 6 envíos, el historial, los 5 detalles, la vista del problema y tres 4xx. Como control, la base sí guarda la marca como salida del caso oculto del envío 12.

### 2. Errores

- **user-api:** los avisos por tiempo agotado, memoria excedida y error de ejecución son textos fijos (`EnviosController.java:203-226`). Con un 503 (`:239`), un 400 o un 404 el mensaje también es fijo y no incluye el texto de la excepción. El único aviso que muestra el texto del motor es el de compilación (`:205-208`). Ese texto no depende de la entrada, y Piston confirma un error de compilación de Java repitiendo la ejecución sin entrada (`piston.py`, `_confirmar_compilacion`).
- **user-api, 500:** no hay ningún `@ControllerAdvice`. Spring Boot no incluye el mensaje ni la traza en el cuerpo (`server.error.include-message` está en su valor por defecto, `never`).
- **admin-api:** una petición rechazada a `/internal/` no lleva ningún caso (lo comprueba una prueba). Los 404 son iguales para un problema inexistente y para uno no visible.
- **ai-service:** los errores de un motor devuelven textos genéricos.
- **Observación sobre ai-service:** si la entrada de un caso pasa de 10 000 caracteres, ai-service responde 422, y FastAPI repite la entrada en ese cuerpo. Solo lo recibe user-api, que lo convierte en el 503 fijo, así que el estudiante no ve la entrada. Antes, admin-api no limitaba el tamaño de la entrada (`models.py`, `TextField`), así que un problema con un caso así nunca se podía calificar. **Resuelto:** `CasoPruebaSerializer` rechaza con 400 una entrada de más de `MAX_ENTRADA` = 10 000 caracteres (`problemas/serializers.py`), el mismo tope de ai-service. Lo hace al crear o editar un problema, también uno generado por la IA, y los casos que ya existen no se tocan.
- **Límite de cobertura:** el 500 genérico de user-api se respalda solo con la configuración por defecto de Spring Boot. No hay una prueba que lo provoque, porque MockMvc no pasa por el controlador de errores de Spring Boot.

### 3. Logs

- **user-api:** no hay ningún logger, `System.out` ni `printStackTrace` en `src/main`.
- **admin-api:** hay 4 llamadas de log (`cuentas/views.py:165,205` y `problemas/ia.py:34,42`). Solo escriben un código HTTP o el nombre de la excepción.
- **ai-service:** hay 4 llamadas de log (`main.py:15`, `providers/llm/envio.py:21,25` y `providers/sandbox/piston.py:90`). Solo escriben un código HTTP, un lenguaje o el nombre de la excepción.
- **`docker compose logs`, después de la prueba en vivo:** la marca aparece 0 veces en los 11 servicios (admin-api, admin-web, ai-service, db, judge0-db, judge0-redis, judge0-server, judge0-worker, piston, user-api y user-web).
- **FUGA en Judge0:** al mandar la marca directamente a `judge0-server` (lo mismo que hace ai-service con `SANDBOX_PROVIDER=judge0`), Rails la escribe en su log como parte de `Parameters: {… "stdin"=>"…" …}`, junto con el código del estudiante. **CERRADA:** `judge0-server` y `judge0-worker` tienen `logging: driver: none` en `docker-compose.yml`, así que Docker no guarda su salida y `docker compose logs` no tiene nada que mostrar. `infra/scripts/fuga_judge0_logs.py` lee el driver con `docker inspect`. Si es `none`, termina con «OK: log de Judge0 desactivado» y código 0. Si no, busca la marca en el log como antes y termina con código 1 si aparece.
- **Piston:** no tiene runtimes instalados (`/api/v2/runtimes` devuelve `[]`). Una petición con la marca dio 400 y la marca no quedó en su log, pero no se pudo comprobar con una ejecución real.

### 4. Rutas internas

- `/internal/problemas/{id}/calificacion/` devuelve los casos ocultos. user-api los usa para ejecutar y comparar (`EnviosController.java:153-178`) y nunca los copia en la respuesta ni en el historial o el detalle.
- ai-service recibe la entrada de cada caso en `POST /execute` y no la registra. Lo comprueban la revisión de código, la prueba nueva y los logs del stack.

## Pruebas que respaldan cada fila

| Canal | Pruebas existentes | Pruebas nuevas (HU39-T3) |
|---|---|---|
| 1. user-api | `EnviosApiTest.de_un_caso_oculto_solo_viaja_si_paso_nunca_su_entrada_ni_su_salida`, `EnviosApiTest.un_envio_guarda_una_fila_por_cada_caso_con_si_paso_y_lo_que_imprimio`, `HistorialEnviosApiTest.la_respuesta_no_incluye_el_codigo_del_envio` | `CasosOcultosAuditoriaTest.la_salida_oculta_guardada_no_sale_por_el_historial_ni_por_el_detalle` |
| 1. admin-api | `test_casos_ocultos.CasosOcultosNoSeExponenAlEstudianteTests` (6 pruebas), `test_casos_ocultos_internos.CasosOcultosEnLaRutaInternaTests`, `test_calificacion_interna.CalificacionDeUnProblemaTests.test_la_ruta_publica_del_estudiante_sigue_sin_traer_los_casos_ocultos` | — |
| 2. user-api | `EnviosApiTest.un_error_del_programa_en_un_caso_oculto_no_filtra_su_entrada_por_el_aviso`, `EnviosApiTest.si_el_motor_falla_a_mitad_de_los_casos_no_queda_ninguna_fila` | `CasosOcultosAuditoriaTest`: `un_tiempo_agotado_en_el_caso_oculto_no_filtra_su_entrada`, `una_memoria_excedida_en_el_caso_oculto_no_filtra_su_entrada`, `un_error_del_servicio_en_el_caso_oculto_da_503_sin_su_entrada`, `un_sandbox_caido_en_el_caso_oculto_da_503_sin_el_mensaje_de_la_excepcion`, `un_400_en_el_caso_oculto_no_filtra_su_entrada` |
| 2. admin-api | `test_calificacion_interna.ClaveDeCalificacionTests.test_el_rechazo_no_filtra_ningun_caso`, `…test_sin_aprobar_descartado_o_inexistente_da_la_misma_404` | `test_limite_entrada.LimiteDeEntradaTests` (una entrada de 10 001 caracteres da 400; una de 10 000 se acepta) |
| 2. ai-service | `test_sandbox_piston.test_un_http_de_error_es_error_de_servicio_con_mensaje_generico`, `test_sandbox_piston.test_el_error_de_compilacion_de_java_se_confirma_repitiendo_sin_entrada` | `test_logs_casos_ocultos.test_un_motor_que_falla_no_registra_la_entrada` (también revisa el mensaje de error) |
| 3. ai-service | — | `test_logs_casos_ocultos.test_execute_no_registra_la_entrada` (6 casos), `test_logs_casos_ocultos.test_un_motor_que_falla_no_registra_la_entrada` (4 casos) |
| 3. Judge0 | — | `infra/scripts/fuga_judge0_logs.py` (hoy «OK: log de Judge0 desactivado», código 0) |
| 4. | `test_guardian_rutas_internas.GuardianRutasInternasTests`, `test_calificacion_interna.ClaveDeCalificacionTests`, `ClaveInternaFiltroTest`, `GuardianRutasTest` | Las mismas del canal 1 para user-api |

**Comprobación por mutación:**
- `test_logs_casos_ocultos`, primera mutación: el log de `piston.py:90` pasó a escribir el `stdin`, y la prueba dio 1 fallo.
- `test_logs_casos_ocultos`, segunda mutación: la ruta `/execute` pasó a registrar la entrada, y la prueba dio 6 fallos.
- `test_limite_entrada`: con `extra_kwargs` vacío en `CasoPruebaSerializer`, falló la prueba de la entrada demasiado larga. La del tope exacto siguió pasando, como debe.
- En los tres casos el archivo quedó restaurado.
- `CasosOcultosAuditoriaTest` (user-api) pasa 6 de 6, en una corrida manual con las variables `POSTGRES_*`.

## Hallazgos

**Resueltos**

1. **Fuga de Judge0: CERRADA.** Judge0 escribía en el log de `judge0-server` el stdin de cada submission, con el código del estudiante: con `SANDBOX_PROVIDER=judge0` habrían quedado ahí las entradas de los casos ocultos. Ahora `judge0-server` y `judge0-worker` tienen el log desactivado en el compose (`logging: driver: none`). Ver el canal 3.
2. **ai-service, publicado solo en local.** El compose publica `127.0.0.1:8002:8002`. Los servicios del compose lo siguen llamando por `http://ai-service:8002`, y las herramientas de este PC (`humo.py`, `probar_sandbox.py`, `datos_demo.py`) por `localhost:8002`.
3. **Tamaño de la entrada de un caso: resuelto.** admin-api aplica el mismo tope que ai-service (10 000 caracteres). Ver el canal 2.

**Pendientes**

1. **Autenticación de ai-service.** Sigue sin autenticación. Hoy solo es accesible desde el propio PC y desde la red de Docker. Necesita una decisión de Juan Diego antes de usarlo fuera del desarrollo.
2. **Judge0 guarda cada submission en su base (`judge0-db`), stdin incluido.** No es un log, pero es una copia persistente de las entradas ocultas fuera de `admin_schema`. Solo es accesible dentro de la red de Docker. No se cambió.
3. **Verificación con motores reales.** La prueba en vivo usó el motor `mock`. Falta repetirla con Judge0 (Linux con cgroup v1, `docs/judge0.md`) y con Piston (necesita runtimes instalados, `docs/piston.md`).

## Nota: estudiante dado de baja de un curso (HU41-T4)

Un estudiante que ya no está inscrito en un curso sigue abriendo el detalle de sus propios envíos (`GET /api/envios/{envioId}`: 200 con su código). En cambio, ya no ve el historial de ese problema (`GET /api/problemas/{problemaId}/envios`: 404, el mismo que un problema de un curso ajeno). Es intencional según HU42-T2: el detalle es un dato del estudiante y no consulta inscripciones ni a admin-api. Por eso no se cambió. Lo fija `EnviosAislamientoTest.sin_inscripcion_el_estudiante_sigue_abriendo_su_propio_envio`; si se decide otra cosa, esa prueba debe cambiar junto con la decisión.

## Observaciones de exposición (canal 5)

- **ai-service está publicado solo en `127.0.0.1:8002`** (`docker-compose.yml`), pero sigue sin autenticación (ver «Pendientes»). Antes estaba en `0.0.0.0:8002`, y desde otra máquina de la red se podía llamar a `POST /execute`, que ejecuta código con el motor, y a `POST /generate`, que gasta la cuota del LLM. No expone casos ocultos porque ai-service no los guarda.
- **En Windows, `http://localhost:8002` tarda unos 2 s por petición.** `localhost` se resuelve primero a `::1`, que ya no está publicado, y espera antes de caer a IPv4. Con `http://127.0.0.1:8002` responde en milisegundos. Nada se rompe, pero cada llamada desde este PC (los scripts, y user-api cuando corre fuera de Docker) tarda más.
- **Las rutas `/internal/` de admin-api (`0.0.0.0:8000`) y de user-api (`0.0.0.0:8081`)** rechazan las peticiones sin `X-Internal-Key` (403 en la prueba en vivo). Están publicadas en todas las interfaces, pero protegidas por la clave.
- **El panel `/admin/` de Django responde en `0.0.0.0:8000`.** Pide inicio de sesión y no tiene modelos de problemas registrados.
- **Judge0 y Piston** solo escuchan en `127.0.0.1`.
- **`.env.example` trae `DJANGO_DEBUG=true`.** Con DEBUG activo, un 500 de admin-api muestra la página de depuración con las variables locales. En las rutas `/internal/` solo puede verla quien ya tiene la clave, pero conviene confirmar que DEBUG esté apagado fuera del desarrollo.
