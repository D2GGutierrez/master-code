import json
import urllib.error
import urllib.request

from django.conf import settings


class UserApiNoDisponible(Exception):
    """user-api no respondió o respondió algo que no se puede leer."""


# Cliente de la API interna de user-api (D3, D10). La inscripción y los envíos viven en app_schema: aquí se pregunta por
# su API interna y no se leen sus tablas (ADR-1). Solo biblioteca estándar: admin-api no tiene otro cliente HTTP.
def _consultar(ruta):
    peticion = urllib.request.Request(
        f"{settings.USER_API_URL.rstrip('/')}{ruta}",
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


def inscritos_de(curso_id):
    return _consultar(f"/internal/cursos/{curso_id}/inscritos")


# HU44-T1: por cada problema que tenga envíos, cuántos intentos y cuántos correctos
# ([{"problema_id", "intentos", "correctos"}]). Un problema sin envíos no viene en la lista.
def resumen_envios_de(problema_ids):
    ids = ",".join(str(problema_id) for problema_id in problema_ids)
    return _consultar(f"/internal/envios/resumen?problemas={ids}")
