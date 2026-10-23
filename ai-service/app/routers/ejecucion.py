from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
)
from app.providers.sandbox.seleccion import proveedor_activo

router = APIRouter()


# Contrato de POST /execute: ver ADR-7 (D1) en docs/decisiones-tecnicas-adr.md.
# Los topes evitan que una petición enorme llegue al motor; user-api valida lo mismo antes de llamar.
class EjecucionIn(BaseModel):
    codigo: str = Field(min_length=1, max_length=50_000)
    lenguaje: str = Field(min_length=1, max_length=30)
    entrada: str = Field(default="", max_length=10_000)


class ResultadoOut(BaseModel):
    estado: Estado
    salida: str
    error: str
    tiempo_ms: int | None


# El proveedor llega por Depends: el router no sabe si es Judge0, Piston o mock, y las pruebas lo reemplazan.
@router.post("/execute", response_model=ResultadoOut)
def ejecutar(solicitud: EjecucionIn, proveedor: Annotated[ProveedorSandbox, Depends(proveedor_activo)]):
    try:
        resultado = proveedor.ejecutar(Ejecucion(solicitud.codigo, solicitud.lenguaje, solicitud.entrada))
    except LenguajeNoSoportado:
        raise HTTPException(status_code=400, detail="El motor de ejecución no soporta ese lenguaje.") from None
    return resultado
