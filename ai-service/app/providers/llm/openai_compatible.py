from app.generacion.esquema import ErrorDeFormato, Solicitud
from app.generacion.prompt import construir_prompt
from app.providers.llm import envio
from app.providers.llm.base import LLMNoDisponible, ProveedorLLM
from app.providers.sandbox.http import post_json


# Sirve para cualquier servicio con la API de «chat completions» de OpenAI. Hoy se usa con Groq;
# otro (OpenRouter, por ejemplo) solo necesitaría su URL y un nombre en seleccion.py.
class CompatibleOpenAI(ProveedorLLM):
    def __init__(self, nombre: str, url: str, clave: str, modelo: str, enviar_http=post_json):
        self._nombre = nombre
        self._url = url
        self._clave = clave
        self._modelo = modelo
        self._enviar_http = enviar_http

    def generar(self, solicitud: Solicitud) -> str:
        if not self._clave:
            raise LLMNoDisponible(f"{self._nombre} no está configurado: falta LLM_API_KEY.")
        cuerpo = {
            "model": self._modelo,
            "messages": [{"role": "user", "content": construir_prompt(solicitud)}],
            # Modo JSON: exige que el prompt mencione «JSON», cosa que construir_prompt ya hace.
            "response_format": {"type": "json_object"},
        }
        datos = envio.enviar(
            self._nombre, self._url, cuerpo, {"Authorization": f"Bearer {self._clave}"}, self._enviar_http
        )
        try:
            texto = datos["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError):
            texto = ""
        if not isinstance(texto, str) or not texto.strip():
            raise ErrorDeFormato(f"{self._nombre} no devolvió ningún texto.")
        return texto
