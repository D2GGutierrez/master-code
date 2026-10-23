import http.client
import logging
import urllib.error

from app.providers.llm.base import LLMNoDisponible
from app.providers.sandbox.http import post_json

_log = logging.getLogger("uvicorn.error")

# Un LLM tarda más que un sandbox: generar varios problemas puede pasar de 30 s.
# El llamador (admin-api) debe esperar al menos esto antes de rendirse.
TIEMPO_MAXIMO_S = 90


# Envía la petición al LLM y traduce cualquier fallo de red a LLMNoDisponible con un mensaje fijo.
# Del error HTTP solo se registra el código: su cuerpo y su URL no se copian a logs ni a respuestas.
def enviar(nombre: str, url: str, cuerpo: dict, cabeceras: dict, enviar_http=post_json) -> dict:
    try:
        return enviar_http(url, cuerpo, cabeceras=cabeceras, timeout=TIEMPO_MAXIMO_S)
    except urllib.error.HTTPError as error:
        _log.warning("%s respondió HTTP %s al generar problemas", nombre, error.code)
        raise LLMNoDisponible(f"{nombre} rechazó la petición (HTTP {error.code}).") from None
    except (urllib.error.URLError, OSError, http.client.HTTPException, ValueError) as error:
        # ValueError cubre un cuerpo que no es JSON; OSError, el tiempo agotado y las conexiones caídas.
        _log.warning("%s no respondió al generar problemas: %s", nombre, type(error).__name__)
        raise LLMNoDisponible(f"{nombre} no respondió.") from None
