# Piston: sandbox alternativo

Piston es el segundo motor que puede ejecutar el código de los estudiantes (HU08-T2). Cumple la misma interfaz que Judge0 (`ai-service/app/providers/sandbox/piston.py`), así que se elige solo con una variable. Su ventaja: pide **cgroup v2** (el de Docker Desktop con WSL2), justo lo que Judge0 1.13.1 no soporta. Por eso es la primera opción para probar ejecución real en un PC con Windows.

> **Estado.** Verificado contra un Piston real el 29 de oct. de 2026 (python 3.12.0, node 20.11.1, java 15.0.2): `probar_sandbox.py` pasa sus 16 casos y `infra/scripts/humo.py` sus 25 pasos con este motor. En la primera corrida falló un caso: el error de compilación de Java, porque ese Java ejecuta el `.java` directo, sin etapa de compilación, y Piston lo reportaba como fallo de ejecución; se corrigió en `piston.py` (ver la tabla de la sección 5). La imagen quedó fijada por su digest en el `docker-compose.yml`.

## 1. Levantarlo

No arranca con un `docker compose up` normal: se pide con un perfil.

```bash
docker compose --profile piston up -d piston
docker compose ps piston
```

La imagen es `ghcr.io/engineer-man/piston` (el proyecto no publica etiquetas de versión, así que el `docker-compose.yml` la fija por su `digest`; para actualizarla, repite la sección 5 con la imagen nueva y cambia el digest). Necesita `privileged: true` para aislar los programas.

## 2. Instalar los lenguajes (una sola vez)

Piston arranca sin lenguajes. Se instalan por su API y quedan en el volumen `piston_packages`:

```bash
curl -s http://localhost:2000/api/v2/packages | head -c 600
curl -s -X POST http://localhost:2000/api/v2/packages -H "Content-Type: application/json" -d '{"language":"python","version":"*"}'
curl -s -X POST http://localhost:2000/api/v2/packages -H "Content-Type: application/json" -d '{"language":"java","version":"*"}'
curl -s -X POST http://localhost:2000/api/v2/packages -H "Content-Type: application/json" -d '{"language":"node","version":"*"}'
```

El paquete de JavaScript se llama `node`. Si alguno falla con «no encontrado», busca el nombre exacto en el listado del primer comando. La instalación descarga el lenguaje de internet y tarda unos minutos. Comprueba:

```bash
curl -s http://localhost:2000/api/v2/runtimes
```

Deben aparecer `python`, `java` y `javascript`.

## 3. Probar que ejecuta

```bash
echo '{"language":"python","version":"*","files":[{"name":"main.py","content":"print(\"hola mundo\")"}]}' > hola.json
curl -s -X POST http://localhost:2000/api/v2/execute -H "Content-Type: application/json" -d @hola.json
```

En `run.stdout` debe venir `hola mundo`. Borra `hola.json` al terminar.

## 4. Usarlo desde la aplicación

1. En tu `.env`: `SANDBOX_PROVIDER=piston`.
2. `docker compose up -d ai-service`.
3. Probar `POST http://localhost:8002/execute` como en la sección 5 de [`judge0.md`](judge0.md).

Con este motor, «Ejecutar» y «Enviar solución» (ADR-7 D7) ejecutan código de verdad. Con `mock` (el valor por defecto) no se ejecuta nada y todos los envíos salen incorrectos.

## 5. Verificarlo de punta a punta

Con Piston levantado, los lenguajes instalados (sección 2) y `ai-service` usando `piston` (sección 4), desde la raíz del repositorio:

```bash
python ai-service/scripts/probar_sandbox.py
```

Solo usa la biblioteca estándar de Python (no necesita el entorno virtual). Si Piston solo tiene algunos lenguajes: `--lenguajes python,javascript`. Pasa por `POST /execute`, o sea por el mismo camino que `user-api`. Tarda cerca de un minuto porque los programas que el motor debe frenar esperan el límite de tiempo. Termina con código 0 si los 16 casos salen como se espera.

| Caso | Qué comprueba | Si sale mal |
| :--- | :--- | :--- |
| Hola mundo y «lee la entrada» (los tres lenguajes) | Que el lenguaje está instalado y el programa recibe su entrada. | `error_servicio`: el lenguaje no está instalado en Piston (sección 2), o `ai-service` no llega a él (`PISTON_URL`). Mira `docker compose logs ai-service`. |
| Salida de 5000 caracteres | Que una salida normal no se corta. | Piston mata el programa si imprime más que `PISTON_OUTPUT_MAX_SIZE` (por defecto 1024; en el compose son 65536). |
| Error de ejecución | Que el error llega con su mensaje (`error_ejecucion`). | |
| Error de compilación (Java) | `error_compilacion` con el mensaje del compilador. | Piston no tiene etapa de compilación para Java 15: el error llega como `RE` con `error: compilation failed` al final de stderr. `piston.py` lo reconoce y repite la corrida sin entrada, para que un programa que imprima su entrada por stderr no se haga pasar por un error de compilación y filtre un caso oculto. |
| Bucle infinito (los tres lenguajes) | `tiempo_agotado` en unos 5 s. | Si dice `error_ejecucion`, esa versión de Piston no informa el estado `TO`: avísalo, la traducción está en `piston.py`. Si tarda más de 30 s, el límite no se aplica. |
| Memoria sin límite | Que el programa muere sin tumbar el motor. | Si sale `ok` o no termina, el límite de memoria no funciona. |
| Conexión a internet | Que el programa no sale a la red (`PISTON_DISABLE_NETWORKING`). Intenta conectarse a dos IP públicas. | Si imprime `CONECTO`, la red no está bloqueada. Si el PC no tiene internet, este caso no demuestra nada. |
| Bomba de procesos | Que `PISTON_MAX_PROCESS_COUNT` la frena. | Si no termina, el límite no funciona. |
| Lenguaje que el motor no tiene | `POST /execute` responde 400. | |

Si cambias la imagen de Piston, repite esta sección y anota arriba, en **Estado**, la fecha y los casos que fallaron antes de cambiar el digest del `docker-compose.yml`.

## 6. Límites y red

Se configuran en el servicio `piston` del `docker-compose.yml` (`PISTON_...`):

| Qué | Variable | Valor |
| :--- | :--- | :--- |
| Tiempo de ejecución (reloj y CPU) | `PISTON_RUN_TIMEOUT`, `PISTON_RUN_CPU_TIME` | 5000 ms |
| Tiempo de compilación (reloj y CPU) | `PISTON_COMPILE_TIMEOUT`, `PISTON_COMPILE_CPU_TIME` | 10000 ms |
| Memoria al ejecutar y al compilar | `PISTON_RUN_MEMORY_LIMIT`, `PISTON_COMPILE_MEMORY_LIMIT` | 268435456 bytes (256 MB) |
| Salida máxima de cada ejecución | `PISTON_OUTPUT_MAX_SIZE` | 65536 caracteres (el valor por defecto de Piston es 1024) |
| Procesos por ejecución | `PISTON_MAX_PROCESS_COUNT` | 64 |
| Red | `PISTON_DISABLE_NETWORKING` | `true` |

Cambiar un valor es editarlo en el compose y recrear el servicio: `docker compose --profile piston up -d --force-recreate piston`.

## 7. Diferencias con Judge0 y problemas conocidos

- Los límites son del servidor, no de cada petición.
- Piston no distingue «memoria excedida»: el programa muere por una señal y aparece como error de ejecución.
- Java se guarda como `Main.java` (la clase pública debe llamarse `Main`), igual que con Judge0.
- Si en Java «hola mundo» falla por memoria (mensajes como «Could not reserve enough space» o `error_ejecucion` con un compilador que muere), la máquina virtual de Java necesita más que los 256 MB: sube `PISTON_COMPILE_MEMORY_LIMIT` y `PISTON_RUN_MEMORY_LIMIT` a 536870912 (512 MB) y recrea el servicio.
- Cada caso de prueba de un envío es una llamada a `POST /execute`, una tras otra. Un programa que no termina agota los 5 s en cada caso: con 5 casos, el resultado tarda unos 25 s. `user-api` espera hasta 40 s por llamada (`SANDBOX_TIMEOUT_SEGUNDOS`).
