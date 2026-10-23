# Piston: sandbox alternativo

Piston es el segundo motor que puede ejecutar el código de los estudiantes (HU08-T2). Cumple la misma interfaz que Judge0 (`ai-service/app/providers/sandbox/piston.py`), así que se elige solo con una variable. Su ventaja: pide **cgroup v2** (el de Docker Desktop con WSL2), justo lo que Judge0 1.13.1 no soporta. Por eso es la primera opción para probar ejecución real en un PC con Windows.

> **Estado.** Escrito y probado con respuestas simuladas; **todavía no se ha probado contra un Piston real**. Al hacerlo por primera vez, anota aquí qué pasó.

## 1. Levantarlo

No arranca con un `docker compose up` normal: se pide con un perfil.

```bash
docker compose --profile piston up -d piston
docker compose ps piston
```

La imagen es `ghcr.io/engineer-man/piston` (sin versión fija: el proyecto no publica etiquetas de versión; cuando se verifique, conviene fijarla por su `digest`). Necesita `privileged: true` para aislar los programas.

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

## 5. Límites y red

Se configuran en el servicio `piston` del `docker-compose.yml` (`PISTON_...`):

| Qué | Variable | Valor |
| :--- | :--- | :--- |
| Tiempo de ejecución | `PISTON_RUN_TIMEOUT` | 5000 ms |
| Tiempo de compilación | `PISTON_COMPILE_TIMEOUT` | 10000 ms |
| Memoria al ejecutar y al compilar | `PISTON_RUN_MEMORY_LIMIT`, `PISTON_COMPILE_MEMORY_LIMIT` | 268435456 bytes (256 MB) |
| Procesos por ejecución | `PISTON_MAX_PROCESS_COUNT` | 64 |
| Red | `PISTON_DISABLE_NETWORKING` | `true` |

## 6. Diferencias con Judge0 que ya maneja `ai-service`

- Los límites son del servidor, no de cada petición.
- Piston no distingue «memoria excedida»: el programa muere por una señal y aparece como error de ejecución.
- Java se guarda como `Main.java` (la clase pública debe llamarse `Main`), igual que con Judge0.
