import json
import urllib.error
import urllib.request

from django.conf import settings


class UserApiNoDisponible(Exception):
    """user-api no respondió o respondió algo que no se puede leer."""


# La inscripción vive en app_schema (user-api): aquí se pregunta por su API interna y no se lee
# su tabla (ADR-1, D3). Solo biblioteca estándar: admin-api no tiene otro cliente HTTP.
def inscritos_de(curso_id):
    peticion = urllib.request.Request(
        f"{settings.USER_API_URL.rstrip('/')}/internal/cursos/{curso_id}/inscritos",
        headers={"X-Internal-Key": settings.INTERNAL_API_KEY},
    )
    try:
        # Tiempo máximo: si user-api está caído, el docente ve el error en segundos y no una pantalla colgada.
        with urllib.request.urlopen(peticion, timeout=5) as respuesta:
            datos = json.loads(respuesta.read())
    except (urllib.error.URLError, TimeoutError, ValueError) as error:
        raise UserApiNoDisponible(str(error)) from error
    if not isinstance(datos, list):
        raise UserApiNoDisponible("respuesta inesperada de user-api")
    return datos
