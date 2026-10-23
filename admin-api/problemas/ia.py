import json
import logging
import urllib.error
import urllib.request

from django.conf import settings

_log = logging.getLogger(__name__)


class IaNoDisponible(Exception):
    """ai-service no respondió, tardó demasiado o está caído: no hay nada que revisar, se puede reintentar."""


class IaRespuestaInvalida(Exception):
    """ai-service respondió, pero lo que devolvió no sirve (formato roto o contrato roto)."""


# Pide a ai-service (POST /generate, ADR-2) los problemas de una solicitud y devuelve la lista tal cual,
# sin validar su contenido: eso lo hace guardar_generados. Solo biblioteca estándar, como cursos/inscritos.py.
# No lleva X-Internal-Key: esa clave es de la API interna entre admin-api y user-api (D3).
def generar_problemas(solicitud, timeout=None):
    peticion = urllib.request.Request(
        f"{settings.AI_SERVICE_URL.rstrip('/')}/generate",
        data=json.dumps(solicitud).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(peticion, timeout=timeout or settings.AI_SERVICE_TIMEOUT) as respuesta:
            cuerpo = respuesta.read()
    except urllib.error.HTTPError as error:
        # Del cuerpo del error solo se registra el código: el docente ve un mensaje fijo y claro.
        _log.warning("ai-service respondió HTTP %s al generar problemas", error.code)
        if error.code in (400, 422, 502):
            # 502: la IA contestó fuera de formato. 400 y 422: ai-service no entendió la solicitud,
            # que admin-api ya validó; sería un contrato roto entre los dos servicios, no un fallo de red.
            raise IaRespuestaInvalida(f"HTTP {error.code}") from None
        raise IaNoDisponible(f"HTTP {error.code}") from None
    except (urllib.error.URLError, OSError) as error:
        # Conexión rechazada (ai-service apagado), tiempo agotado, red caída.
        _log.warning("ai-service no respondió al generar problemas: %s", type(error).__name__)
        raise IaNoDisponible(type(error).__name__) from None

    try:
        problemas = json.loads(cuerpo)["problemas"]
    except (ValueError, KeyError, TypeError):
        raise IaRespuestaInvalida("la respuesta de ai-service no tiene el formato del contrato") from None
    if not isinstance(problemas, list):
        raise IaRespuestaInvalida("«problemas» no es una lista")
    return problemas
