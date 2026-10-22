import json

import pytest

from app.generacion.esquema import ErrorDeFormato, Solicitud, validar_respuesta
from app.generacion.prompt import construir_prompt


def solicitud(**cambios):
    datos = {"tema": "bucles for", "nivel": "facil", "lenguaje": "python", "cantidad": 1, "plataforma": "web"}
    return Solicitud(**{**datos, **cambios})


ESCRITURA = {
    "titulo": "Suma",
    "enunciado": "Lee dos enteros e imprime su suma.",
    "tipo_ejercicio": "escritura_codigo",
    "casos_prueba": [
        {"entrada": "1 2", "salida_esperada": "3", "es_publico": True},
        {"entrada": "0 0", "salida_esperada": "0", "es_publico": False},
    ],
}
OPCION = {
    "titulo": "Bucle",
    "enunciado": "¿Qué hace range(3)?",
    "tipo_ejercicio": "opcion_multiple",
    "opciones": [{"texto": "0,1,2", "es_correcta": True}, {"texto": "1,2,3", "es_correcta": False}],
}
PREDICCION = {
    "titulo": "Salida",
    "enunciado": "print(2 + 3)",
    "tipo_ejercicio": "prediccion_salida",
    "respuesta_esperada": "5",
}


def respuesta(*problemas):
    return json.dumps({"problemas": list(problemas)})


def test_una_respuesta_correcta_valida():
    problemas = validar_respuesta(respuesta(ESCRITURA), solicitud())

    assert problemas[0].titulo == "Suma"
    assert len(problemas[0].casos_prueba) == 2


def test_acepta_el_json_envuelto_en_bloque_markdown():
    texto = f"```json\n{respuesta(ESCRITURA)}\n```"

    assert len(validar_respuesta(texto, solicitud())) == 1


@pytest.mark.parametrize(
    ("texto", "mensaje"),
    [
        ("esto no es json", "JSON válido"),
        (json.dumps({"otra_cosa": []}), "problemas"),
        (respuesta({**ESCRITURA, "casos_prueba": []}), "caso de prueba"),
        (respuesta({**ESCRITURA, "titulo": ""}), "titulo"),
        (respuesta({**ESCRITURA, "opciones": [{"texto": "a", "es_correcta": True}]}), "no lleva opciones"),
    ],
)
def test_respuestas_con_formato_invalido_se_rechazan(texto, mensaje):
    with pytest.raises(ErrorDeFormato, match=mensaje):
        validar_respuesta(texto, solicitud())


def test_opcion_multiple_exige_una_sola_correcta():
    dos_correctas = {**OPCION, "opciones": [{"texto": "a", "es_correcta": True}, {"texto": "b", "es_correcta": True}]}

    with pytest.raises(ErrorDeFormato, match="una opción correcta"):
        validar_respuesta(respuesta(dos_correctas), solicitud(plataforma="movil"))


def test_prediccion_exige_respuesta_esperada():
    with pytest.raises(ErrorDeFormato, match="respuesta_esperada"):
        validar_respuesta(respuesta({**PREDICCION, "respuesta_esperada": " "}), solicitud(plataforma="movil"))


def test_la_cantidad_debe_coincidir():
    with pytest.raises(ErrorDeFormato, match="Se pidieron 2"):
        validar_respuesta(respuesta(ESCRITURA), solicitud(cantidad=2))


def test_el_tipo_debe_servir_para_la_plataforma():
    with pytest.raises(ErrorDeFormato, match="plataforma web"):
        validar_respuesta(respuesta(OPCION), solicitud(plataforma="web"))
    with pytest.raises(ErrorDeFormato, match="plataforma movil"):
        validar_respuesta(respuesta(ESCRITURA), solicitud(plataforma="movil"))


def test_ambas_acepta_cualquier_tipo():
    assert len(validar_respuesta(respuesta(ESCRITURA, OPCION, PREDICCION), solicitud(plataforma="ambas", cantidad=3))) == 3


@pytest.mark.parametrize("cambio", [{"cantidad": 0}, {"cantidad": 11}, {"nivel": "experto"}, {"tema": " "}])
def test_la_solicitud_invalida_se_rechaza(cambio):
    with pytest.raises(ValueError):
        solicitud(**cambio)


def test_el_prompt_lleva_los_datos_y_solo_los_tipos_de_la_plataforma():
    prompt = construir_prompt(solicitud(tema="recursividad", cantidad=3, lenguaje="java", plataforma="movil"))

    assert "<tema>recursividad</tema>" in prompt
    assert "exactamente 3 problemas" in prompt
    assert "java" in prompt
    assert '"opcion_multiple"' in prompt
    assert '"escritura_codigo":' not in prompt
