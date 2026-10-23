import json
import urllib.request


# Compartido por los proveedores que hablan HTTP con su motor (Judge0, Piston) y con el LLM (Gemini, Groq).
# Un fallo de red o un código HTTP de error sale como URLError/HTTPError: cada proveedor lo traduce.
# `cabeceras` sirve para la clave del LLM (va en una cabecera, nunca en la URL, para que no quede en logs).
def post_json(url: str, cuerpo: dict, cabeceras: dict | None = None, timeout: float = 30) -> dict:
    # Algunos servicios (Groq, detrás de Cloudflare) rechazan el User-Agent por defecto de urllib.
    encabezados = {"Content-Type": "application/json", "User-Agent": "master-code-ai-service/1.0"}
    encabezados.update(cabeceras or {})
    peticion = urllib.request.Request(url, data=json.dumps(cuerpo).encode(), headers=encabezados, method="POST")
    # Tiempo máximo para no dejar colgada la petición si el motor no responde.
    with urllib.request.urlopen(peticion, timeout=timeout) as respuesta:
        return json.loads(respuesta.read())
