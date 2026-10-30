# ai-service

Servicio FastAPI de MASTER CODE en el puerto 8002. La documentación automática está en `/docs`.

| Ruta | Para qué sirve |
|---|---|
| `GET /health` | Responde `{"status": "ok"}`. |
| `POST /execute` | Ejecuta código de un estudiante en el motor activo (`SANDBOX_PROVIDER`: mock, judge0 o piston). Contrato en ADR-7. |
| `POST /generate` | Genera problemas con el LLM activo (`LLM_PROVIDER`: mock, gemini o groq). Contrato en ADR-2. |

## Variables de entorno

| Variable | Valor por defecto | Notas |
|---|---|---|
| `LLM_PROVIDER` | `mock` | `mock` no necesita clave ni red. |
| `LLM_API_KEY` | vacía | Solo para gemini o groq. Va en tu `.env` local, nunca en el repositorio. Nunca se imprime ni se devuelve. |
| `LLM_MODEL` | el de cada proveedor | `gemini-flash-latest` o `llama-3.3-70b-versatile`. Cámbiala si el proveedor retira ese modelo. |
| `SANDBOX_PROVIDER` | `mock` | `mock`, `judge0` o `piston` (ver `docs/judge0.md` y `docs/piston.md`). |

## Probar la generación

Con `mock`, `curl -X POST http://localhost:8002/generate -H "Content-Type: application/json" -d '{"tema":"bucles","nivel":"facil","lenguaje":"python","cantidad":2,"plataforma":"ambas"}'`.

Con un LLM real, `python -m scripts.probar_prompt` pide problemas de cinco temas y valida cada respuesta (ver el encabezado del script).

## Probar la ejecución

Con `SANDBOX_PROVIDER=piston` (o `judge0`) y el motor levantado, `python ai-service/scripts/probar_sandbox.py` ejecuta programas buenos y malos (bucle infinito, memoria, red) a través de `POST /execute` y dice si el motor los trata como se espera (ver `docs/piston.md`, sección 5).

## Local

```bash
py -3.13 -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m uvicorn app.main:app --port 8002
```

## Docker

```bash
docker build -t ai-service .
docker run --rm -p 8002:8002 ai-service
```

Comprobar: `curl http://localhost:8002/health`
