import urllib.error

import pytest

from app.generacion.esquema import ErrorDeFormato, Solicitud, validar_respuesta
from app.providers.llm.base import LLMNoDisponible
from app.providers.llm.envio import TIEMPO_MAXIMO_S
from app.providers.llm.gemini import Gemini
from app.providers.llm.mock import Mock

CLAVE = "clave-secreta-de-prueba"
SOLICITUD = Solicitud(tema="recursividad", nivel="medio", lenguaje="java", cantidad=2, plataforma="ambas")


class Servidor:
    """Registra lo que se envía y responde con lo que se le indique."""

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
    return {"candidates": [{"content": {"parts": [{"text": texto}]}}]}


def test_envia_la_peticion_con_la_clave_en_una_cabecera():
    servidor = Servidor(_respuesta("{}"))

    Gemini(CLAVE, "gemini-de-prueba", servidor).generar(SOLICITUD)

    llamada = servidor.llamadas[0]
    assert llamada["url"] == (
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-de-prueba:generateContent"
    )
    assert llamada["cabeceras"] == {"x-goog-api-key": CLAVE}
    assert CLAVE not in llamada["url"]
    assert llamada["timeout"] == TIEMPO_MAXIMO_S
    assert llamada["cuerpo"]["generationConfig"] == {"responseMimeType": "application/json"}
    prompt = llamada["cuerpo"]["contents"][0]["parts"][0]["text"]
    assert "recursividad" in prompt and "java" in prompt


def test_devuelve_el_texto_y_este_pasa_la_validacion():
    texto = Mock().generar(SOLICITUD)

    resultado = Gemini(CLAVE, "m", Servidor(_respuesta(texto))).generar(SOLICITUD)

    assert len(validar_respuesta(resultado, SOLICITUD)) == 2


def test_junta_las_partes_y_descarta_las_de_razonamiento():
    datos = {
        "candidates": [
            {
                "content": {
                    "parts": [{"text": "pensando...", "thought": True}, {"text": '{"problemas":'}, {"text": " []}"}]
                }
            }
        ]
    }

    assert Gemini(CLAVE, "m", Servidor(datos)).generar(SOLICITUD) == '{"problemas": []}'


def test_sin_clave_no_envia_nada():
    servidor = Servidor(_respuesta("{}"))

    with pytest.raises(LLMNoDisponible, match="LLM_API_KEY"):
        Gemini("", "m", servidor).generar(SOLICITUD)

    assert servidor.llamadas == []


def test_un_error_http_no_filtra_la_clave():
    error = urllib.error.HTTPError(f"https://x/?key={CLAVE}", 403, f"prohibido {CLAVE}", {}, None)

    with pytest.raises(LLMNoDisponible) as capturado:
        Gemini(CLAVE, "m", Servidor(error=error)).generar(SOLICITUD)

    assert "403" in str(capturado.value)
    assert CLAVE not in str(capturado.value)
    assert capturado.value.__cause__ is None and capturado.value.__suppress_context__


@pytest.mark.parametrize(
    "error",
    [urllib.error.URLError("sin red"), TimeoutError(), ConnectionResetError(), ValueError("no es JSON")],
)
def test_fallos_de_red_o_cuerpo_ilegible_son_llm_no_disponible(error):
    with pytest.raises(LLMNoDisponible, match="no respondió"):
        Gemini(CLAVE, "m", Servidor(error=error)).generar(SOLICITUD)


@pytest.mark.parametrize(
    "datos",
    [{}, {"candidates": []}, {"candidates": [{"content": {"parts": []}}]}, _respuesta("   "), {"candidates": [{}]}],
)
def test_una_respuesta_sin_texto_es_error_de_formato(datos):
    with pytest.raises(ErrorDeFormato):
        Gemini(CLAVE, "m", Servidor(datos)).generar(SOLICITUD)
