import pytest

from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
    Resultado,
)
from app.providers.sandbox.mock import MARCADORES, Mock


def ejecutar(codigo, entrada="", lenguaje="python") -> Resultado:
    proveedor: ProveedorSandbox = Mock()
    return proveedor.ejecutar(Ejecucion(codigo, lenguaje, entrada))


def test_sin_marcador_imprime_lo_que_recibe():
    resultado = ejecutar("print(input())", entrada="hola")

    assert resultado == Resultado(Estado.OK, salida="hola", tiempo_ms=1)


def test_es_determinista():
    assert ejecutar("x", "5") == ejecutar("x", "5")


@pytest.mark.parametrize("marcador", list(MARCADORES))
def test_cada_marcador_simula_su_estado(marcador):
    estado, mensaje = MARCADORES[marcador]

    resultado = ejecutar(f"# {marcador}\nprint(1)")

    assert resultado.estado == estado
    assert resultado.error == mensaje
    assert resultado.salida == ""


def test_el_marcador_sirve_en_cualquier_lenguaje():
    assert ejecutar("// MOCK_TIEMPO_AGOTADO", lenguaje="javascript").estado == Estado.TIEMPO_AGOTADO


def test_el_lenguaje_no_distingue_mayusculas():
    assert ejecutar("x", lenguaje="Python").estado == Estado.OK


def test_un_lenguaje_no_soportado_lanza_error():
    with pytest.raises(LenguajeNoSoportado):
        ejecutar("x", lenguaje="cobol")
