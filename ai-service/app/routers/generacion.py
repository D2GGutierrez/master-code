from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException

from app.generacion.esquema import (
    ErrorDeFormato,
    RespuestaIA,
    Solicitud,
    validar_respuesta,
)
from app.providers.llm.base import LLMNoDisponible, ProveedorLLM
from app.providers.llm.seleccion import proveedor_activo

router = APIRouter()


# POST /generate (HU17-T3). Lo llama admin-api; el docente revisa todo antes de publicar (ADR-2).
# El cuerpo es `Solicitud` y la respuesta `RespuestaIA`: el formato fijo de HU17-T2.
# No reintenta: si la respuesta no sirve, admin-api decide si vuelve a pedir (HU17-T6).
# Es una ruta síncrona a propósito: FastAPI la corre en un hilo y la espera al LLM no bloquea el servicio.
@router.post("/generate", response_model=RespuestaIA)
def generar(solicitud: Solicitud, proveedor: Annotated[ProveedorLLM, Depends(proveedor_activo)]):
    try:
        texto = proveedor.generar(solicitud)
        problemas = validar_respuesta(texto, solicitud)
    except LLMNoDisponible as error:
        # 503: el proveedor no se pudo usar. El mensaje nunca lleva la clave (ver LLMNoDisponible).
        raise HTTPException(status_code=503, detail=str(error)) from None
    except ErrorDeFormato as error:
        # 502: el proveedor respondió, pero algo que no cumple el formato.
        raise HTTPException(status_code=502, detail=f"La IA devolvió una respuesta inválida: {error}") from None
    return RespuestaIA(problemas=problemas)
