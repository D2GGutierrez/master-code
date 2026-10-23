import json
import threading
import urllib.error
from http.server import BaseHTTPRequestHandler, HTTPServer

import pytest

from app.providers.sandbox.http import post_json


# Servidor real en 127.0.0.1: los demás tests reemplazan post_json por un falso y no prueban la petición de verdad.
@pytest.fixture
def servidor():
    recibido = {}

    class Manejador(BaseHTTPRequestHandler):
        def do_POST(self):
            largo = int(self.headers["Content-Length"])
            recibido["cuerpo"] = json.loads(self.rfile.read(largo))
            recibido["cabeceras"] = self.headers  # Message ignora mayúsculas, como HTTP
            codigo = 429 if self.path == "/limite" else 200
            self.send_response(codigo)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"ok": codigo == 200}).encode())

        def log_message(self, *args):
            pass

    http = HTTPServer(("127.0.0.1", 0), Manejador)
    hilo = threading.Thread(target=http.serve_forever, daemon=True)
    hilo.start()
    yield f"http://127.0.0.1:{http.server_port}", recibido
    http.shutdown()
    http.server_close()


def test_envia_el_cuerpo_y_las_cabeceras_extra(servidor):
    url, recibido = servidor

    respuesta = post_json(url + "/x", {"a": 1}, cabeceras={"x-goog-api-key": "abc"}, timeout=5)

    assert respuesta == {"ok": True}
    assert recibido["cuerpo"] == {"a": 1}
    assert recibido["cabeceras"]["x-goog-api-key"] == "abc"
    assert recibido["cabeceras"]["Content-Type"] == "application/json"
    assert "Python-urllib" not in recibido["cabeceras"]["User-Agent"]


def test_sin_cabeceras_extra_sigue_funcionando(servidor):
    url, _ = servidor

    assert post_json(url + "/x", {}) == {"ok": True}


def test_un_codigo_http_de_error_sale_como_http_error(servidor):
    url, _ = servidor

    with pytest.raises(urllib.error.HTTPError) as capturado:
        post_json(url + "/limite", {})

    assert capturado.value.code == 429
