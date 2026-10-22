import urllib.error

import pytest

from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
    Resultado,
)
from app.providers.sandbox.judge0 import Judge0


def judge0_que_responde(datos):
    enviados = []

    def enviar(url, cuerpo):
        enviados.append((url, cuerpo))
        return datos

    return Judge0("http://judge0-server:2358/", enviar), enviados


def ejecutar_en(proveedor: ProveedorSandbox, ejecucion: Ejecucion) -> Resultado:
    # El resto del código solo conoce la interfaz, nunca a Judge0.
    return proveedor.ejecutar(ejecucion)


def test_codigo_correcto_devuelve_salida_y_estado_ok():
    proveedor, enviados = judge0_que_responde({"status": {"id": 3}, "stdout": "hola mundo\n"})

    resultado = ejecutar_en(proveedor, Ejecucion("print('hola mundo')", "python"))

    assert resultado == Resultado(Estado.OK, salida="hola mundo\n")
    url, cuerpo = enviados[0]
    assert url == "http://judge0-server:2358/submissions?wait=true"
    assert cuerpo["language_id"] == 71


def test_la_entrada_viaja_como_stdin():
    proveedor, enviados = judge0_que_responde({"status": {"id": 3}, "stdout": "5\n"})

    ejecutar_en(proveedor, Ejecucion("print(int(input()))", "python", entrada="5"))

    assert enviados[0][1]["stdin"] == "5"


@pytest.mark.parametrize(
    ("datos", "esperado"),
    [
        ({"status": {"id": 5}}, Estado.TIEMPO_AGOTADO),
        ({"status": {"id": 6}, "compile_output": "error de sintaxis"}, Estado.ERROR_COMPILACION),
        ({"status": {"id": 11}, "stderr": "ZeroDivisionError"}, Estado.ERROR_EJECUCION),
        ({"status": {"id": 13}, "message": "Internal Error"}, Estado.ERROR_SERVICIO),
    ],
)
def test_cada_estado_de_judge0_se_traduce(datos, esperado):
    proveedor, _ = judge0_que_responde(datos)

    assert ejecutar_en(proveedor, Ejecucion("x", "python")).estado == esperado


def test_el_mensaje_de_error_se_conserva():
    proveedor, _ = judge0_que_responde({"status": {"id": 11}, "stderr": "ZeroDivisionError"})

    assert ejecutar_en(proveedor, Ejecucion("1/0", "python")).error == "ZeroDivisionError"


def test_si_el_motor_no_responde_es_error_de_servicio_y_no_excepcion():
    def enviar(url, cuerpo):
        raise urllib.error.URLError("sin conexión")

    resultado = ejecutar_en(Judge0("http://judge0-server:2358", enviar), Ejecucion("x", "python"))

    assert resultado.estado == Estado.ERROR_SERVICIO


def test_lenguaje_no_soportado_lanza_error():
    proveedor, _ = judge0_que_responde({})

    with pytest.raises(LenguajeNoSoportado):
        ejecutar_en(proveedor, Ejecucion("x", "cobol"))
