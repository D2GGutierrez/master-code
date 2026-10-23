from abc import ABC, abstractmethod

from app.generacion.esquema import Solicitud


class LLMNoDisponible(Exception):
    """El proveedor de LLM no se pudo usar: sin clave, sin red o rechazó la petición.

    El mensaje se devuelve tal cual al llamador: nunca lleva la clave ni el cuerpo de la respuesta.
    """


# Contrato común de todo proveedor de LLM (Gemini, Groq, mock). El router solo conoce esta clase:
# cambiar de proveedor es cambiar LLM_PROVIDER y no tocar el endpoint (ADR-2).
class ProveedorLLM(ABC):
    @abstractmethod
    def generar(self, solicitud: Solicitud) -> str:
        """Devuelve el texto crudo con el JSON de problemas; la validación la hace el router.

        No valida ni corrige: un JSON mal formado le llega tal cual a `validar_respuesta`.
        Si el proveedor no se puede usar lanza LLMNoDisponible; si responde pero sin texto,
        lanza ErrorDeFormato.
        """
