import logging
import urllib.error

from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
    Resultado,
)
from app.providers.sandbox.http import post_json

_log = logging.getLogger("uvicorn.error")

# lenguaje -> (nombre en Piston, nombre del archivo). Java exige que el archivo se llame como su
# clase pública, por eso Main.java (igual que con Judge0).
LENGUAJES = {
    "python": ("python", "main.py"),
    "java": ("java", "Main.java"),
    "javascript": ("javascript", "main.js"),
}

# Códigos de estado de Piston en run.status y compile.status (solo aparecen si algo salió mal):
# TO = tiempo agotado, RE = error al correr, SG = el proceso murió por una señal,
# OL / EL = salida o errores demasiado largos, XX = falla interna del motor.
_ESTADO_POR_CODIGO = {
    "TO": Estado.TIEMPO_AGOTADO,
    "RE": Estado.ERROR_EJECUCION,
    "SG": Estado.ERROR_EJECUCION,
    "OL": Estado.ERROR_EJECUCION,
    "EL": Estado.ERROR_EJECUCION,
    "XX": Estado.ERROR_SERVICIO,
}


# El Java que instala Piston (15) ejecuta el .java directo, sin una etapa de compilación aparte: un error de
# compilación llega como un fallo de la ejecución (status RE, código 1) con esta línea al final de stderr.
_JAVA_NO_COMPILO = "error: compilation failed"


def _fallo_la_compilacion(resultado: Resultado) -> bool:
    return resultado.estado == Estado.ERROR_EJECUCION and resultado.error.rstrip().endswith(_JAVA_NO_COMPILO)


def _milisegundos(etapa: dict) -> int | None:
    # Piston informa cpu_time y wall_time en milisegundos; se prefiere el de CPU, como Judge0.
    valor = etapa.get("cpu_time", etapa.get("wall_time"))
    return round(valor) if isinstance(valor, (int, float)) else None


class Piston(ProveedorSandbox):
    # `enviar` se recibe para poder probar sin un Piston real. Los límites de tiempo y memoria y el
    # bloqueo de red se configuran en el servidor Piston (docker-compose.yml), no en cada petición.
    def __init__(self, url: str, enviar=post_json):
        self._url = url.rstrip("/")
        self._enviar = enviar

    def ejecutar(self, ejecucion: Ejecucion) -> Resultado:
        destino = LENGUAJES.get(ejecucion.lenguaje.lower())
        if destino is None:
            raise LenguajeNoSoportado(ejecucion.lenguaje)
        lenguaje, archivo = destino

        cuerpo = {
            "language": lenguaje,
            # "*" = la versión instalada más reciente de ese lenguaje.
            "version": "*",
            "files": [{"name": archivo, "content": ejecucion.codigo}],
            "stdin": ejecucion.entrada,
        }
        resultado = self._pedir(cuerpo, ejecucion.lenguaje)
        if lenguaje == "java" and _fallo_la_compilacion(resultado):
            return self._confirmar_compilacion(cuerpo, resultado)
        return resultado

    def _pedir(self, cuerpo: dict, lenguaje: str) -> Resultado:
        try:
            datos = self._enviar(f"{self._url}/api/v2/execute", cuerpo)
        except urllib.error.HTTPError as error:
            # Por ejemplo 400 si el lenguaje no está instalado en Piston: es un problema del servicio.
            _log.warning("Piston respondió HTTP %s al ejecutar %s", error.code, lenguaje)
            return Resultado(Estado.ERROR_SERVICIO, error="El motor de ejecución no pudo ejecutar el código.")
        except (urllib.error.URLError, TimeoutError, ValueError):
            return Resultado(Estado.ERROR_SERVICIO, error="El motor de ejecución no respondió.")
        return _traducir(datos)

    def _confirmar_compilacion(self, cuerpo: dict, resultado: Resultado) -> Resultado:
        # user-api le muestra al estudiante el texto de un error de compilación porque no depende de la entrada (un
        # caso oculto no se filtra). Aquí el programa sí corrió con la entrada: uno que imprima su stdin por stderr
        # y termine con el mensaje de arriba se haría pasar por un error de compilación y dejaría ver un caso oculto.
        # Por eso se repite sin entrada: el texto de esa corrida no puede llevar nada de ella.
        repetido = self._pedir({**cuerpo, "stdin": ""}, "java")
        if not _fallo_la_compilacion(repetido):
            return resultado
        # Piston guarda el archivo como Main.java.java, y ese nombre saldría en el mensaje.
        error = repetido.error.replace("Main.java.java", "Main.java")
        return Resultado(Estado.ERROR_COMPILACION, error=error, tiempo_ms=repetido.tiempo_ms)


def _traducir(datos: dict) -> Resultado:
    compilacion = datos.get("compile") or {}
    ejecucion = datos.get("run") or {}

    # La compilación va primero: si falló, el programa nunca llegó a correr.
    if compilacion and (compilacion.get("code") not in (0, None) or compilacion.get("status")):
        estado = _ESTADO_POR_CODIGO.get(compilacion.get("status"), Estado.ERROR_COMPILACION)
        if estado == Estado.ERROR_EJECUCION:
            estado = Estado.ERROR_COMPILACION
        error = compilacion.get("stderr") or compilacion.get("output") or compilacion.get("message") or ""
        return Resultado(estado, error=error, tiempo_ms=_milisegundos(compilacion))

    codigo_estado = ejecucion.get("status")
    if codigo_estado in _ESTADO_POR_CODIGO:
        estado = _ESTADO_POR_CODIGO[codigo_estado]
    elif ejecucion.get("code") == 0:
        estado = Estado.OK
    elif ejecucion.get("code") is None and not ejecucion.get("signal"):
        # Sin código de salida ni señal: la respuesta no dice qué pasó con el programa.
        estado = Estado.ERROR_SERVICIO
    else:
        estado = Estado.ERROR_EJECUCION

    return Resultado(
        estado,
        salida=ejecucion.get("stdout") or "",
        error=ejecucion.get("stderr") or ejecucion.get("message") or "",
        tiempo_ms=_milisegundos(ejecucion),
    )
