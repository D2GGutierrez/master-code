# ai-service

Servicio FastAPI de MASTER CODE en el puerto 8002. Por ahora solo responde `GET /health` con `{"status": "ok"}`; la documentación automática está en `/docs`.

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
