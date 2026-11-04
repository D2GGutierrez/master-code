import logging
import urllib.error

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.providers.sandbox.base import Ejecucion, Estado
from app.providers.sandbox.judge0 import Judge0
from app.providers.sandbox.mock import MARCADORES
from app.providers.sandbox.piston import Piston

# HU39-T3: ai-service recibe la entrada de los casos ocultos (user-api la manda para calificar) y no debe escribirla en
# ningún log. Marca inventada: si aparece en lo capturado, algún log la escribió.
MARCA = "MARCA-OCULTA-123"

cliente = TestClient(app)


# Sin marcador (programa que imprime lo que lee) y con cada estado simulado del mock.
@pytest.mark.parametrize("codigo", ["print(input())", *(f"# {marcador}" for marcador in MARCADORES)])
def test_execute_no_registra_la_entrada(monkeypatch, caplog, codigo):
    monkeypatch.setenv("SANDBOX_PROVIDER", "mock")
    caplog.set_level(logging.DEBUG)

    respuesta = cliente.post("/execute", json={"codigo": codigo, "lenguaje": "python", "entrada": MARCA})

    assert respuesta.status_code == 200
    assert MARCA not in caplog.text


def _falla_con(error):
    def enviar(url, cuerpo):
        raise error

    return enviar


# Los caminos de error de los motores reales son los únicos que hoy escriben un log.
@pytest.mark.parametrize(
    "proveedor",
    [
        Piston("http://piston:2000", _falla_con(urllib.error.HTTPError("u", 400, "bad", {}, None))),
        Piston("http://piston:2000", _falla_con(urllib.error.URLError("sin conexión"))),
        Judge0("http://judge0:2358", _falla_con(urllib.error.HTTPError("u", 500, "bad", {}, None))),
        Judge0("http://judge0:2358", _falla_con(urllib.error.URLError("sin conexión"))),
    ],
)
def test_un_motor_que_falla_no_registra_la_entrada(caplog, proveedor):
    caplog.set_level(logging.DEBUG)

    resultado = proveedor.ejecutar(Ejecucion("print(input())", "python", MARCA))

    assert resultado.estado == Estado.ERROR_SERVICIO
    assert MARCA not in caplog.text
    assert MARCA not in resultado.error
