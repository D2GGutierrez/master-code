from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum


class Estado(str, Enum):
    OK = "ok"
    ERROR_EJECUCION = "error_ejecucion"  # el programa del estudiante falló al correr
    ERROR_COMPILACION = "error_compilacion"
    TIEMPO_AGOTADO = "tiempo_agotado"
    ERROR_SERVICIO = "error_servicio"  # falló el motor, no el código del estudiante


@dataclass(frozen=True)
class Ejecucion:
    codigo: str
    lenguaje: str
    entrada: str = ""


@dataclass(frozen=True)
class Resultado:
    estado: Estado
    salida: str = ""
    error: str = ""
    # Milisegundos que tardó el programa; None si el motor no lo informa.
    tiempo_ms: int | None = None


class LenguajeNoSoportado(ValueError):
    pass


# Contrato común de todo motor de ejecución (Judge0, Piston, mock). El resto del código solo
# conoce esta clase: así cambiar de motor es cambiar una variable y no tocar los routers (HU08).
class ProveedorSandbox(ABC):
    @abstractmethod
    def ejecutar(self, ejecucion: Ejecucion) -> Resultado:
        """Corre el código con su entrada y devuelve la salida y el estado.

        Un fallo del motor se devuelve como Estado.ERROR_SERVICIO, no como excepción.
        Un lenguaje que el motor no tiene lanza LenguajeNoSoportado.
        """
