import logging

from fastapi import FastAPI

from app.config import problema_de_configuracion
from app.routers import ejecucion

app = FastAPI(title="ai-service")
app.include_router(ejecucion.router)

# Si falta la clave, el servicio arranca igual (/health y el mock siguen sirviendo) pero lo avisa.
_problema = problema_de_configuracion()
if _problema:
    logging.getLogger("uvicorn.error").warning(_problema)


# Mismo cuerpo que el /health de admin-api y user-api.
@app.get("/health")
def health():
    return {"status": "ok"}
