import urllib.error

import pytest

from app.generacion.esquema import ErrorDeFormato, Solicitud, validar_respuesta
from app.providers.llm.base import LLMNoDisponible
from app.providers.llm.envio import TIEMPO_MAXIMO_S
from app.providers.llm.mock import Mock
from app.providers.llm.openai_compatible import CompatibleOpenAI

CLAVE = "clave-secreta-de-prueba"
URL = "https://api.ejemplo.test/v1/chat/completions"
SOLICITUD = Solicitud(tema="ordenamiento", nivel="dificil", lenguaje="python", cantidad=3, plataforma="movil")


class Servidor:
    def __init__(self, respuesta=None, error=None):
        self.respuesta = respuesta
        self.error = error
        self.llamadas = []

    def __call__(self, url, cuerpo, cabeceras=None, timeout=30):
        self.llamadas.append({"url": url, "cuerpo": cuerpo, "cabeceras": cabeceras or {}, "timeout": timeout})
        if self.error:
            raise self.error
        return self.respuesta


def _respuesta(texto):
    return {"choices": [{"message": {"role": "assistant", "content": texto}}]}


def _proveedor(servidor, clave=CLAVE):
    return CompatibleOpenAI("Groq", URL, clave, "modelo-de-prueba", servidor)


def test_envia_la_peticion_con_la_clave_como_bearer():
    servidor = Servidor(_respuesta("{}"))

    _proveedor(servidor).generar(SOLICITUD)

    llamada = servidor.llamadas[0]
    assert llamada["url"] == URL
    assert llamada["cabeceras"] == {"Authorization": f"Bearer {CLAVE}"}
    assert llamada["timeout"] == TIEMPO_MAXIMO_S
    cuerpo = llamada["cuerpo"]
    assert cuerpo["model"] == "modelo-de-prueba"
    assert cuerpo["response_format"] == {"type": "json_object"}
    assert cuerpo["messages"][0]["role"] == "user"
    assert "ordenamiento" in cuerpo["messages"][0]["content"]
    # El modo JSON de estos servicios falla si el prompt no menciona «JSON».
    assert "JSON" in cuerpo["messages"][0]["content"]


def test_devuelve_el_texto_y_este_pasa_la_validacion():
    texto = Mock().generar(SOLICITUD)

    resultado = _proveedor(Servidor(_respuesta(texto))).generar(SOLICITUD)

    assert len(validar_respuesta(resultado, SOLICITUD)) == 3


def test_sin_clave_no_envia_nada():
    servidor = Servidor(_respuesta("{}"))

    with pytest.raises(LLMNoDisponible, match="Groq.*LLM_API_KEY"):
        _proveedor(servidor, clave="").generar(SOLICITUD)

    assert servidor.llamadas == []


def test_un_error_http_no_filtra_la_clave():
    error = urllib.error.HTTPError(URL, 401, f"clave invalida {CLAVE}", {}, None)

    with pytest.raises(LLMNoDisponible) as capturado:
        _proveedor(Servidor(error=error)).generar(SOLICITUD)

    assert "401" in str(capturado.value)
    assert CLAVE not in str(capturado.value)


@pytest.mark.parametrize("error", [urllib.error.URLError("sin red"), TimeoutError(), ValueError("no es JSON")])
def test_fallos_de_red_son_llm_no_disponible(error):
    with pytest.raises(LLMNoDisponible, match="Groq no respondió"):
        _proveedor(Servidor(error=error)).generar(SOLICITUD)


@pytest.mark.parametrize(
    "datos",
    [{}, {"choices": []}, {"choices": [{"message": {}}]}, _respuesta(""), _respuesta(None), {"choices": [{}]}],
)
def test_una_respuesta_sin_texto_es_error_de_formato(datos):
    with pytest.raises(ErrorDeFormato):
        _proveedor(Servidor(datos)).generar(SOLICITUD)
