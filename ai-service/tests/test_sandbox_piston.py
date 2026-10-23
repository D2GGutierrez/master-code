import urllib.error

import pytest

from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
    Resultado,
)
from app.providers.sandbox.piston import Piston


def piston_que_responde(datos):
    enviados = []

    def enviar(url, cuerpo):
        enviados.append((url, cuerpo))
        return datos

    return Piston("http://piston:2000/", enviar), enviados


def ejecutar_en(proveedor: ProveedorSandbox, ejecucion: Ejecucion) -> Resultado:
    # Igual que con Judge0: el resto del código solo conoce la interfaz común.
    return proveedor.ejecutar(ejecucion)


OK = {"language": "python", "run": {"stdout": "hola\n", "stderr": "", "code": 0, "signal": None, "cpu_time": 12}}


def test_codigo_correcto_devuelve_salida_estado_ok_y_tiempo():
    proveedor, enviados = piston_que_responde(OK)

    resultado = ejecutar_en(proveedor, Ejecucion("print('hola')", "python"))

    assert resultado == Resultado(Estado.OK, salida="hola\n", tiempo_ms=12)
    url, cuerpo = enviados[0]
    assert url == "http://piston:2000/api/v2/execute"
    assert cuerpo["language"] == "python"
    assert cuerpo["version"] == "*"
    assert cuerpo["files"] == [{"name": "main.py", "content": "print('hola')"}]


def test_la_entrada_viaja_como_stdin():
    proveedor, enviados = piston_que_responde(OK)

    ejecutar_en(proveedor, Ejecucion("x", "python", entrada="5"))

    assert enviados[0][1]["stdin"] == "5"


@pytest.mark.parametrize(
    ("lenguaje", "nombre"), [("python", "main.py"), ("java", "Main.java"), ("javascript", "main.js")]
)
def test_cada_lenguaje_usa_su_archivo(lenguaje, nombre):
    proveedor, enviados = piston_que_responde(OK)

    ejecutar_en(proveedor, Ejecucion("x", lenguaje))

    assert enviados[0][1]["files"][0]["name"] == nombre


def test_usa_wall_time_si_no_hay_cpu_time():
    proveedor, _ = piston_que_responde({"run": {"stdout": "", "code": 0, "wall_time": 40}})

    assert ejecutar_en(proveedor, Ejecucion("x", "python")).tiempo_ms == 40


def test_sin_tiempos_queda_en_none():
    proveedor, _ = piston_que_responde({"run": {"stdout": "", "code": 0}})

    assert ejecutar_en(proveedor, Ejecucion("x", "python")).tiempo_ms is None


@pytest.mark.parametrize(
    ("datos", "esperado"),
    [
        ({"run": {"status": "TO", "signal": "SIGKILL", "code": None}}, Estado.TIEMPO_AGOTADO),
        ({"run": {"status": "RE", "code": 1, "stderr": "ZeroDivisionError"}}, Estado.ERROR_EJECUCION),
        ({"run": {"status": "SG", "signal": "SIGSEGV", "code": None}}, Estado.ERROR_EJECUCION),
        ({"run": {"status": "OL", "code": None, "signal": "SIGKILL"}}, Estado.ERROR_EJECUCION),
        ({"run": {"status": "XX", "message": "fallo interno"}}, Estado.ERROR_SERVICIO),
        # Sin campo status: se decide por el código de salida.
        ({"run": {"code": 1, "stderr": "boom"}}, Estado.ERROR_EJECUCION),
        ({"run": {"code": None, "signal": "SIGKILL"}}, Estado.ERROR_EJECUCION),
        ({"run": {"code": None, "signal": None}}, Estado.ERROR_SERVICIO),
        ({}, Estado.ERROR_SERVICIO),
    ],
)
def test_cada_resultado_de_piston_se_traduce(datos, esperado):
    proveedor, _ = piston_que_responde(datos)

    assert ejecutar_en(proveedor, Ejecucion("x", "python")).estado == esperado


def test_un_error_de_compilacion_se_reconoce_aunque_no_haya_run():
    proveedor, _ = piston_que_responde(
        {"compile": {"stdout": "", "stderr": "Main.java:1: error: ';' expected", "code": 1, "signal": None}}
    )

    resultado = ejecutar_en(proveedor, Ejecucion("x", "java"))

    assert resultado.estado == Estado.ERROR_COMPILACION
    assert "expected" in resultado.error


def test_una_compilacion_correcta_deja_pasar_a_la_ejecucion():
    proveedor, _ = piston_que_responde(
        {"compile": {"code": 0, "stderr": ""}, "run": {"stdout": "7\n", "code": 0, "cpu_time": 80}}
    )

    resultado = ejecutar_en(proveedor, Ejecucion("x", "java"))

    assert resultado == Resultado(Estado.OK, salida="7\n", tiempo_ms=80)


def test_el_mensaje_de_error_se_conserva():
    proveedor, _ = piston_que_responde({"run": {"code": 1, "stderr": "ZeroDivisionError"}})

    assert ejecutar_en(proveedor, Ejecucion("1/0", "python")).error == "ZeroDivisionError"


def test_si_el_motor_no_responde_es_error_de_servicio_y_no_excepcion():
    def enviar(url, cuerpo):
        raise urllib.error.URLError("sin conexión")

    resultado = ejecutar_en(Piston("http://piston:2000", enviar), Ejecucion("x", "python"))

    assert resultado.estado == Estado.ERROR_SERVICIO


def test_un_http_de_error_es_error_de_servicio_con_mensaje_generico():
    def enviar(url, cuerpo):
        raise urllib.error.HTTPError(url, 400, "python-* runtime is unknown", {}, None)

    resultado = ejecutar_en(Piston("http://piston:2000", enviar), Ejecucion("x", "python"))

    assert resultado.estado == Estado.ERROR_SERVICIO
    # El mensaje interno del motor no llega al estudiante.
    assert "runtime" not in resultado.error


def test_lenguaje_no_soportado_lanza_error():
    proveedor, _ = piston_que_responde({})

    with pytest.raises(LenguajeNoSoportado):
        ejecutar_en(proveedor, Ejecucion("x", "cobol"))
