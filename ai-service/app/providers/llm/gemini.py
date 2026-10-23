from app.generacion.esquema import ErrorDeFormato, Solicitud
from app.generacion.prompt import construir_prompt
from app.providers.llm import envio
from app.providers.llm.base import LLMNoDisponible, ProveedorLLM
from app.providers.sandbox.http import post_json

URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models"


class Gemini(ProveedorLLM):
    # `enviar_http` se recibe para poder probar sin red ni clave real.
    def __init__(self, clave: str, modelo: str, enviar_http=post_json):
        self._clave = clave
        self._modelo = modelo
        self._enviar_http = enviar_http

    def generar(self, solicitud: Solicitud) -> str:
        if not self._clave:
            raise LLMNoDisponible("Gemini no está configurado: falta LLM_API_KEY.")
        cuerpo = {
            "contents": [{"parts": [{"text": construir_prompt(solicitud)}]}],
            # Pide JSON puro; igual se valida después porque ningún modelo lo garantiza.
            "generationConfig": {"responseMimeType": "application/json"},
        }
        datos = envio.enviar(
            "Gemini",
            f"{URL_BASE}/{self._modelo}:generateContent",
            cuerpo,
            # La clave va en una cabecera: en la URL quedaría en los logs de cualquier proxy.
            {"x-goog-api-key": self._clave},
            self._enviar_http,
        )
        return _texto(datos)


def _texto(datos: dict) -> str:
    try:
        partes = datos["candidates"][0]["content"]["parts"]
        # Los modelos que «piensan» pueden devolver también partes de razonamiento: no son la respuesta.
        texto = "".join(parte.get("text", "") for parte in partes if not parte.get("thought"))
    except (KeyError, IndexError, TypeError, AttributeError):
        texto = ""
    if not texto.strip():
        # Respondió, pero sin texto (por ejemplo, bloqueó la respuesta): no es un caso de «servicio caído».
        raise ErrorDeFormato("Gemini no devolvió ningún texto.")
    return texto
