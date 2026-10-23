import json
import urllib.request


# Compartido por los proveedores que hablan HTTP con su motor (Judge0, Piston).
# Un fallo de red o un código HTTP de error sale como URLError/HTTPError: cada proveedor lo traduce.
def post_json(url: str, cuerpo: dict) -> dict:
    peticion = urllib.request.Request(
        url, data=json.dumps(cuerpo).encode(), headers={"Content-Type": "application/json"}, method="POST"
    )
    # Tiempo máximo para no dejar colgada la petición si el motor no responde.
    with urllib.request.urlopen(peticion, timeout=30) as respuesta:
        return json.loads(respuesta.read())
