import json
import urllib.error
import urllib.request

from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
    Resultado,
)

# Ids de lenguaje de Judge0 1.13.1.
LENGUAJES = {"python": 71, "java": 62, "javascript": 63}

# Ids de estado de Judge0: 3 = aceptado, 5 = tiempo agotado, 6 = error de compilación, 7 a 12 = error al correr.
# Cualquier otro (por ejemplo 13, error interno) se trata como falla del servicio.
_ESTADO_POR_ID = {3: Estado.OK, 5: Estado.TIEMPO_AGOTADO, 6: Estado.ERROR_COMPILACION}
_ERRORES_DE_EJECUCION = range(7, 13)


def _post_json(url: str, cuerpo: dict) -> dict:
    peticion = urllib.request.Request(
        url, data=json.dumps(cuerpo).encode(), headers={"Content-Type": "application/json"}, method="POST"
    )
    # Tiempo máximo para no dejar colgada la petición si el motor no responde.
    with urllib.request.urlopen(peticion, timeout=30) as respuesta:
        return json.loads(respuesta.read())


# Judge0 informa el tiempo en segundos, como texto ("0.012"), o null.
def _a_milisegundos(segundos) -> int | None:
    try:
        return round(float(segundos) * 1000)
    except (TypeError, ValueError):
        return None


class Judge0(ProveedorSandbox):
    # `enviar` se recibe para poder probar sin un Judge0 real.
    def __init__(self, url: str, enviar=_post_json):
        self._url = url.rstrip("/")
        self._enviar = enviar

    def ejecutar(self, ejecucion: Ejecucion) -> Resultado:
        id_lenguaje = LENGUAJES.get(ejecucion.lenguaje.lower())
        if id_lenguaje is None:
            raise LenguajeNoSoportado(ejecucion.lenguaje)

        cuerpo = {"language_id": id_lenguaje, "source_code": ejecucion.codigo, "stdin": ejecucion.entrada}
        try:
            datos = self._enviar(f"{self._url}/submissions?wait=true", cuerpo)
        except (urllib.error.URLError, TimeoutError, ValueError):
            return Resultado(Estado.ERROR_SERVICIO, error="El motor de ejecución no respondió.")

        id_estado = datos.get("status", {}).get("id")
        if id_estado in _ERRORES_DE_EJECUCION:
            estado = Estado.ERROR_EJECUCION
        else:
            estado = _ESTADO_POR_ID.get(id_estado, Estado.ERROR_SERVICIO)

        return Resultado(
            estado,
            salida=datos.get("stdout") or "",
            error=datos.get("stderr") or datos.get("compile_output") or datos.get("message") or "",
            tiempo_ms=_a_milisegundos(datos.get("time")),
        )
