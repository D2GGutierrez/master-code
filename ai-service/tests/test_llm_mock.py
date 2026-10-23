import json

import pytest

from app.generacion.esquema import (
    TIPOS,
    TIPOS_POR_PLATAFORMA,
    Solicitud,
    validar_respuesta,
)
from app.providers.llm.mock import Mock


def _solicitud(**cambios):
    datos = {"tema": "bucles for", "nivel": "facil", "lenguaje": "python", "cantidad": 3, "plataforma": "web"}
    return Solicitud(**{**datos, **cambios})


# El mock es el único proveedor que corre en el CI: tiene que cumplir el formato en toda combinación.
@pytest.mark.parametrize("plataforma", ["web", "movil", "ambas"])
@pytest.mark.parametrize("lenguaje", ["python", "java", "javascript"])
@pytest.mark.parametrize("cantidad", [1, 2, 4, 5, 10])
def test_la_respuesta_del_mock_pasa_la_validacion(plataforma, lenguaje, cantidad):
    solicitud = _solicitud(plataforma=plataforma, lenguaje=lenguaje, cantidad=cantidad)

    problemas = validar_respuesta(Mock().generar(solicitud), solicitud)

    assert len(problemas) == cantidad
    assert {p.tipo_ejercicio for p in problemas} <= set(TIPOS_POR_PLATAFORMA[plataforma])


def test_con_plataforma_ambas_reparte_los_cuatro_tipos():
    solicitud = _solicitud(plataforma="ambas", cantidad=4)

    problemas = validar_respuesta(Mock().generar(solicitud), solicitud)

    assert [p.tipo_ejercicio for p in problemas] == list(TIPOS)


def test_la_misma_solicitud_da_siempre_la_misma_respuesta():
    solicitud = _solicitud(cantidad=5, plataforma="ambas")

    assert Mock().generar(solicitud) == Mock().generar(solicitud)


def test_el_titulo_respeta_el_tope_aunque_el_tema_sea_larguisimo():
    solicitud = _solicitud(tema="a" * 200)

    problemas = validar_respuesta(Mock().generar(solicitud), solicitud)

    assert all(len(p.titulo) <= 150 for p in problemas)


def test_escritura_de_codigo_trae_casos_publicos_y_ocultos():
    solicitud = _solicitud(cantidad=1)

    problema = validar_respuesta(Mock().generar(solicitud), solicitud)[0]

    assert sum(c.es_publico for c in problema.casos_prueba) == 2
    assert sum(not c.es_publico for c in problema.casos_prueba) >= 1


def test_los_problemas_del_mock_se_distinguen_de_los_de_un_llm():
    solicitud = _solicitud(cantidad=2)

    problemas = json.loads(Mock().generar(solicitud))["problemas"]

    assert all(p["titulo"].startswith("[Mock]") and p["enunciado"].startswith("[Mock]") for p in problemas)
