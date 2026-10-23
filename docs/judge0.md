# Judge0: cómo se levanta y se configura

Judge0 es el motor («sandbox») que ejecuta el código que escriben los estudiantes dentro de un entorno aislado, con límites de tiempo y memoria y sin red. Es un servicio más del `docker-compose.yml`. Solo `ai-service` lo llama (ADR-7, D1): `user-api` le pide a `ai-service` que ejecute (`POST /execute`) y `ai-service` usa el motor que diga la variable `SANDBOX_PROVIDER`.

```text
user-api  --POST /execute-->  ai-service  --(SANDBOX_PROVIDER)-->  Judge0 | Piston | mock
```

> **Requisito importante.** Judge0 1.13.1 necesita un kernel Linux con **cgroup v1**. En Docker Desktop con WSL2 (el caso de Windows) el contenedor arranca, pero al ejecutar código falla (ver la sección 6). Si tu PC no tiene cgroup v1, usa el proveedor `mock` para desarrollar o Piston ([`piston.md`](piston.md)), que sí funciona con cgroup v2.

## 1. Qué levanta el compose

| Servicio | Para qué sirve |
| :--- | :--- |
| `judge0-db` | PostgreSQL propio de Judge0. Está separado de la base del proyecto (ADR-1): no toca `admin_schema` ni `app_schema`. |
| `judge0-redis` | Cola de trabajos de Judge0, con contraseña. |
| `judge0-server` | La API de Judge0 (puerto 2358, publicado solo en `127.0.0.1` para probarlo a mano). |
| `judge0-worker` | Quien ejecuta los programas. Es el único con `privileged: true`, porque aísla cada programa con mecanismos del kernel. |

La imagen es `judge0/judge0:1.13.1`, con versión fija para que se comporte igual en las tres máquinas.

Hoy un `docker compose up` sin nombres también levanta estos cuatro servicios. Si no los necesitas, pide solo los que quieres (por ejemplo `docker compose up -d db admin-api user-api ai-service`).

## 2. Primera vez

1. En tu `.env` local (nunca se sube al repositorio) agrega dos contraseñas distintas, inventadas por ti:

   ```env
   JUDGE0_DB_PASSWORD=una_clave_larga_para_la_base
   JUDGE0_REDIS_PASSWORD=otra_clave_larga_para_redis
   ```

   `.env.example` trae las dos líneas con valores falsos.

2. Levanta solo Judge0:

   ```bash
   docker compose up -d judge0-db judge0-redis judge0-server judge0-worker
   docker compose ps
   ```

   `judge0-db` y `judge0-redis` deben quedar `healthy` y `judge0-server` y `judge0-worker`, `Up`.

3. Comprueba que responde:

   ```bash
   curl http://localhost:2358/version
   ```

   Debe devolver `1.13.1`.

## 3. Probar que ejecuta («hola mundo»)

En Git Bash, desde cualquier carpeta:

```bash
echo '{"source_code":"print(\"hola mundo\")","language_id":71}' > hola.json
curl -s -X POST "http://localhost:2358/submissions?wait=true" -H "Content-Type: application/json" -d @hola.json
```

La respuesta debe traer `"stdout":"hola mundo\n"` y `"status":{"id":3,"description":"Accepted"}`. Borra `hola.json` al terminar.

Los ids de lenguaje que usa el proyecto (`ai-service`, `app/providers/sandbox/judge0.py`) son 71 (Python), 62 (Java) y 63 (JavaScript). La lista completa sale de `curl http://localhost:2358/languages`.

## 4. Límites y red (HU07-T2)

Se configuran con variables de entorno en `judge0-server` (el `judge0-worker` usa las mismas). El valor máximo (`MAX_...`) es igual al valor por defecto: así tampoco un envío puede pedir más de lo permitido.

| Qué | Variable | Valor |
| :--- | :--- | :--- |
| Tiempo de CPU por ejecución | `CPU_TIME_LIMIT` / `MAX_CPU_TIME_LIMIT` | 5 s |
| Tiempo total por ejecución | `WALL_TIME_LIMIT` / `MAX_WALL_TIME_LIMIT` | 10 s |
| Memoria por ejecución | `MEMORY_LIMIT` / `MAX_MEMORY_LIMIT` | 256000 KB (250 MB; Java necesita margen por la JVM) |
| Pila | `STACK_LIMIT` / `MAX_STACK_LIMIT` | 64000 KB |
| Procesos e hilos | `MAX_PROCESSES_AND_OR_THREADS` / `MAX_MAX_PROCESSES_AND_OR_THREADS` | 60 (frena una bomba de procesos) |
| Tamaño de un archivo escrito | `MAX_FILE_SIZE` / `MAX_MAX_FILE_SIZE` | 1024 KB |
| Red | `ENABLE_NETWORK` / `ALLOW_ENABLE_NETWORK` | `false` / `false` (sin red, y un envío no puede activarla) |

Para cambiar un valor se edita `docker-compose.yml` y se recrea el servicio: `docker compose up -d judge0-server judge0-worker`.

**Comprobar que la configuración llegó** (no necesita ejecutar código, funciona aunque no tengas cgroup v1):

```bash
curl -s http://localhost:2358/config_info
```

Debe mostrar `cpu_time_limit` en 5, `wall_time_limit` en 10, `memory_limit` en 256000 y `enable_network` y `allow_enable_network` en `false`.

**Comprobar que un envío no puede pasarse del máximo** (tampoco ejecuta código):

```bash
echo '{"source_code":"print(1)","language_id":71,"cpu_time_limit":30}' > pasarse.json
curl -s -X POST "http://localhost:2358/submissions" -H "Content-Type: application/json" -d @pasarse.json
```

Judge0 responde `422` diciendo que el valor debe ser menor o igual que el máximo. Las pruebas de que un bucle infinito se corta y de que no se pueden leer archivos del sistema son la tarea HU07-T5.

## 5. Usarlo desde la aplicación

1. En tu `.env`: `SANDBOX_PROVIDER=judge0`.
2. `docker compose up -d ai-service` (lee la variable al arrancar).
3. Probar el contrato de `ai-service`:

   ```bash
   echo '{"codigo":"print(input())","lenguaje":"python","entrada":"hola"}' > ejecutar.json
   curl -s -X POST http://localhost:8002/execute -H "Content-Type: application/json" -d @ejecutar.json
   ```

   Respuesta esperada: `{"estado":"ok","salida":"hola\n","error":"","tiempo_ms":...}`.

Con `SANDBOX_PROVIDER=mock` (el valor por defecto) `ai-service` no ejecuta nada: devuelve resultados simulados. Sirve para el CI y para desarrollar sin motor.

## 6. Problemas conocidos

| Qué ves | Por qué | Qué hacer |
| :--- | :--- | :--- |
| `docker compose up` dice que falta `JUDGE0_DB_PASSWORD` | No están las dos contraseñas en tu `.env`. | Agrégalas (paso 2.1). |
| `judge0-db` no arranca después de cambiar `JUDGE0_DB_PASSWORD` | Postgres guarda la contraseña en su volumen la primera vez. | `docker compose rm -sf judge0-db` y `docker volume rm <proyecto>_judge0_db_data` (el nombre exacto sale de `docker volume ls`), y vuelve a levantar. Solo borra datos de Judge0. |
| Todo arranca, pero ejecutar da `status.id` 13 (`Internal Error`) con `/box/script.py` o `Cannot run proxy, clone failed: Operation not permitted` | El kernel no tiene cgroup v1 (Docker Desktop con WSL2 usa cgroup v2). No se arregla con configuración de Docker ni con `.wslconfig`. | Usa otra máquina (siguiente apartado), el proveedor `mock` o Piston. |

**Saber qué cgroup tiene tu Docker:**

```bash
MSYS_NO_PATHCONV=1 docker run --rm alpine ls /sys/fs/cgroup
```

Si aparece el archivo `cgroup.controllers`, es cgroup v2 (Judge0 1.13.1 no ejecutará). Si ves carpetas como `cpu`, `memory` y `pids`, es cgroup v1.

**Dónde sí corre Judge0 1.13.1:** una máquina Linux con cgroup v1, por ejemplo Ubuntu 20.04 (lo trae por defecto) o una más nueva con el parámetro de arranque `systemd.unified_cgroup_hierarchy=0`. Puede ser una máquina virtual o un servidor. Allí el procedimiento es el mismo de las secciones 2 a 5.

## 7. Estado de verificación

- HU07-T1 (compose): arranca completo en Docker Desktop con WSL2; **la ejecución real de código quedó pendiente de probar en Linux con cgroup v1**.
- HU07-T2 (límites y red): configurados; la comprobación con `/config_info` de la sección 4 funciona sin ejecutar código. Las pruebas de seguridad que sí ejecutan (HU07-T5) también necesitan ese Linux.
- Hace falta conseguir un motor real antes de HU07-T5: una máquina con cgroup v1 o Piston verificado.
