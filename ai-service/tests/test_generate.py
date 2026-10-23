import pytest
from fastapi.testclient import TestClient

from app import config
from app.generacion.esquema import TIPOS_POR_PLATAFORMA, Solicitud
from app.main import app
from app.providers.llm.base import LLMNoDisponible, ProveedorLLM
from app.providers.llm.gemini import Gemini
from app.providers.llm.mock import Mock
from app.providers.llm.openai_compatible import CompatibleOpenAI
from app.providers.llm.seleccion import proveedor_activo

cliente = TestClient(app)

CUERPO = {"tema": "bucles for", "nivel": "facil", "lenguaje": "python", "cantidad": 3, "plataforma": "ambas"}


class Falso(ProveedorLLM):
    def __init__(self, texto=None, error=None):
        self.texto = texto
        self.error = error

    def generar(self, solicitud):
        if self.error:
            raise self.error
        return self.texto


def _con_proveedor(proveedor):
    app.dependency_overrides[proveedor_activo] = lambda: proveedor


@pytest.fixture(autouse=True)
def _sin_reemplazos():
    yield
    app.dependency_overrides.clear()


# --- POST /generate ---


def test_generate_con_mock_devuelve_los_problemas_pedidos(monkeypatch):
    monkeypatch.delenv("LLM_PROVIDER", raising=False)

    respuesta = cliente.post("/generate", json=CUERPO)

    assert respuesta.status_code == 200
    problemas = respuesta.json()["problemas"]
    assert len(problemas) == 3
    assert {p["tipo_ejercicio"] for p in problemas} <= set(TIPOS_POR_PLATAFORMA["ambas"])
    assert set(problemas[0]) == {"titulo", "enunciado", "tipo_ejercicio", "casos_prueba", "opciones", "respuesta_esperada"}


def test_generate_solo_devuelve_tipos_de_la_plataforma(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "mock")

    respuesta = cliente.post("/generate", json={**CUERPO, "plataforma": "web", "cantidad": 4})

    assert {p["tipo_ejercicio"] for p in respuesta.json()["problemas"]} == {"escritura_codigo"}


@pytest.mark.parametrize(
    "cambio",
    [
        {"cantidad": 0},
        {"cantidad": 11},
        {"tema": ""},
        {"tema": "x" * 201},
        {"nivel": "experto"},
        {"lenguaje": "cobol"},
        {"plataforma": "escritorio"},
    ],
)
def test_generate_con_cuerpo_invalido_es_422(cambio):
    assert cliente.post("/generate", json={**CUERPO, **cambio}).status_code == 422


def test_generate_sin_algun_dato_es_422():
    cuerpo = {clave: valor for clave, valor in CUERPO.items() if clave != "tema"}

    assert cliente.post("/generate", json=cuerpo).status_code == 422


def test_una_respuesta_que_no_es_json_es_502():
    _con_proveedor(Falso("esto no es JSON"))

    respuesta = cliente.post("/generate", json=CUERPO)

    assert respuesta.status_code == 502
    assert "inválida" in respuesta.json()["detail"]


def test_una_respuesta_con_otra_cantidad_es_502():
    una_sola = Mock().generar(Solicitud(**{**CUERPO, "cantidad": 1}))
    _con_proveedor(Falso(una_sola))

    respuesta = cliente.post("/generate", json=CUERPO)

    assert respuesta.status_code == 502
    assert "3" in respuesta.json()["detail"]


def test_un_proveedor_caido_es_503_con_su_mensaje():
    _con_proveedor(Falso(error=LLMNoDisponible("Gemini no respondió.")))

    respuesta = cliente.post("/generate", json=CUERPO)

    assert respuesta.status_code == 503
    assert respuesta.json() == {"detail": "Gemini no respondió."}


def test_con_gemini_sin_clave_es_503_y_no_toca_la_red(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "gemini")
    monkeypatch.delenv("LLM_API_KEY", raising=False)

    respuesta = cliente.post("/generate", json=CUERPO)

    assert respuesta.status_code == 503
    assert "LLM_API_KEY" in respuesta.json()["detail"]


# --- elección del proveedor por variable (ADR-2) ---


def test_sin_variable_el_proveedor_es_mock(monkeypatch):
    monkeypatch.delenv("LLM_PROVIDER", raising=False)

    assert isinstance(proveedor_activo(), Mock)


@pytest.mark.parametrize(
    ("valor", "clase"), [("mock", Mock), ("gemini", Gemini), ("groq", CompatibleOpenAI), ("GROQ", CompatibleOpenAI)]
)
def test_una_sola_variable_cambia_el_proveedor(monkeypatch, valor, clase):
    monkeypatch.setenv("LLM_PROVIDER", valor)
    monkeypatch.setenv("LLM_API_KEY", "clave-de-prueba")

    assert isinstance(proveedor_activo(), clase)


def test_el_modelo_por_defecto_depende_del_proveedor_y_se_puede_cambiar(monkeypatch):
    monkeypatch.delenv("LLM_MODEL", raising=False)
    monkeypatch.setenv("LLM_PROVIDER", "gemini")
    assert config.modelo_llm() == "gemini-flash-latest"
    monkeypatch.setenv("LLM_PROVIDER", "groq")
    assert config.modelo_llm() == "llama-3.3-70b-versatile"

    monkeypatch.setenv("LLM_MODEL", "otro-modelo")
    assert config.modelo_llm() == "otro-modelo"


def test_github_models_ya_no_es_un_proveedor(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "github")

    with pytest.raises(ValueError, match="GitHub Models"):
        proveedor_activo()
