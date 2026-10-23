import pytest
from fastapi.testclient import TestClient

from app import config
from app.main import app
from app.providers.sandbox.judge0 import Judge0
from app.providers.sandbox.mock import Mock
from app.providers.sandbox.piston import Piston
from app.providers.sandbox.seleccion import proveedor_activo


def test_sin_variable_el_proveedor_es_mock(monkeypatch):
    monkeypatch.delenv("SANDBOX_PROVIDER", raising=False)

    assert isinstance(proveedor_activo(), Mock)


@pytest.mark.parametrize(("valor", "clase"), [("mock", Mock), ("judge0", Judge0), ("piston", Piston), ("PISTON", Piston)])
def test_una_sola_variable_cambia_el_proveedor(monkeypatch, valor, clase):
    monkeypatch.setenv("SANDBOX_PROVIDER", valor)

    assert isinstance(proveedor_activo(), clase)


def test_las_urls_salen_de_variables_con_valor_por_defecto(monkeypatch):
    monkeypatch.delenv("JUDGE0_URL", raising=False)
    monkeypatch.delenv("PISTON_URL", raising=False)
    assert config.url_judge0() == "http://judge0-server:2358"
    assert config.url_piston() == "http://piston:2000"

    monkeypatch.setenv("PISTON_URL", "http://otro:9000")
    assert config.url_piston() == "http://otro:9000"


def test_un_proveedor_desconocido_falla_con_mensaje_claro(monkeypatch):
    monkeypatch.setenv("SANDBOX_PROVIDER", "docker")

    with pytest.raises(ValueError, match="SANDBOX_PROVIDER"):
        proveedor_activo()
    # También al arrancar: así el error aparece al iniciar el servicio y no con la primera ejecución.
    with pytest.raises(ValueError, match="SANDBOX_PROVIDER"):
        config.problema_de_configuracion()


# --- POST /execute: el router solo conoce la interfaz, nunca a un motor concreto ---

cliente = TestClient(app)


def test_execute_con_mock_devuelve_el_contrato(monkeypatch):
    monkeypatch.setenv("SANDBOX_PROVIDER", "mock")

    respuesta = cliente.post("/execute", json={"codigo": "print(input())", "lenguaje": "python", "entrada": "hola"})

    assert respuesta.status_code == 200
    assert respuesta.json() == {"estado": "ok", "salida": "hola", "error": "", "tiempo_ms": 1}


def test_execute_sin_entrada_usa_cadena_vacia(monkeypatch):
    monkeypatch.setenv("SANDBOX_PROVIDER", "mock")

    respuesta = cliente.post("/execute", json={"codigo": "x", "lenguaje": "python"})

    assert respuesta.json()["salida"] == ""


def test_execute_con_codigo_que_falla_es_200_con_su_estado(monkeypatch):
    monkeypatch.setenv("SANDBOX_PROVIDER", "mock")

    respuesta = cliente.post("/execute", json={"codigo": "# MOCK_TIEMPO_AGOTADO", "lenguaje": "python"})

    assert respuesta.status_code == 200
    assert respuesta.json()["estado"] == "tiempo_agotado"


def test_execute_con_lenguaje_no_soportado_es_400(monkeypatch):
    monkeypatch.setenv("SANDBOX_PROVIDER", "mock")

    respuesta = cliente.post("/execute", json={"codigo": "x", "lenguaje": "cobol"})

    assert respuesta.status_code == 400


@pytest.mark.parametrize(
    "cuerpo",
    [
        {"lenguaje": "python"},
        {"codigo": "", "lenguaje": "python"},
        {"codigo": "x"},
        {"codigo": "x" * 50_001, "lenguaje": "python"},
        {"codigo": "x", "lenguaje": "python", "entrada": "y" * 10_001},
    ],
)
def test_execute_con_cuerpo_invalido_es_422(monkeypatch, cuerpo):
    monkeypatch.setenv("SANDBOX_PROVIDER", "mock")

    assert cliente.post("/execute", json=cuerpo).status_code == 422


def test_el_router_usa_el_proveedor_que_se_le_inyecta():
    from app.providers.sandbox.base import Estado, Resultado

    class Falso:
        def ejecutar(self, ejecucion):
            return Resultado(Estado.OK, salida=f"{ejecucion.lenguaje}:{ejecucion.codigo}", tiempo_ms=None)

    app.dependency_overrides[proveedor_activo] = Falso
    try:
        respuesta = cliente.post("/execute", json={"codigo": "x", "lenguaje": "java"})
    finally:
        app.dependency_overrides.clear()

    assert respuesta.json() == {"estado": "ok", "salida": "java:x", "error": "", "tiempo_ms": None}
