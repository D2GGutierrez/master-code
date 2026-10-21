import pytest

from app import config


def test_sin_variables_usa_mock_y_no_hay_problema(monkeypatch):
    monkeypatch.delenv("LLM_PROVIDER", raising=False)
    monkeypatch.delenv("LLM_API_KEY", raising=False)

    assert config.proveedor_llm() == "mock"
    assert config.problema_de_configuracion() is None


def test_proveedor_real_sin_clave_lo_senala(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "gemini")
    monkeypatch.delenv("LLM_API_KEY", raising=False)

    assert "LLM_API_KEY" in config.problema_de_configuracion()


def test_proveedor_real_con_clave_esta_completo(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "groq")
    monkeypatch.setenv("LLM_API_KEY", "clave-de-prueba")

    assert config.proveedor_llm() == "groq"
    assert config.problema_de_configuracion() is None


def test_el_aviso_no_contiene_la_clave(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "github")
    monkeypatch.setenv("LLM_API_KEY", "")

    assert "clave-secreta" not in config.problema_de_configuracion()


def test_proveedor_desconocido_falla(monkeypatch):
    monkeypatch.setenv("LLM_PROVIDER", "otro")

    with pytest.raises(ValueError):
        config.proveedor_llm()
