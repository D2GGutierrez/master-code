# HU44-T4 (CA-44.1, CA-44.4): el docente solo ve datos de sus propios cursos en el resumen de envíos. Lo que ya cubre
# test_resumen_envios (curso ajeno 403 sin consultar a user-api, inexistente 404, sin token 401, estudiante 403) no se
# repite aquí.
from unittest.mock import patch

from cursos.models import Curso
from problemas.models import CasoPrueba
from problemas.test_interno import InternoBase

RESUMEN = "cursos.views.resumen_envios_de"
# Marca inventada: solo aparecería en la respuesta si viajara un caso de prueba, el código base o el de un estudiante.
MARCA = "MARCA-RESUMEN-731"


def url(curso):
    return f"/api/cursos/{curso.pk}/resumen-envios/"


class AislamientoDelResumenDeEnviosTests(InternoBase):
    def test_filas_de_problemas_ajenos_que_devuelva_user_api_no_llegan_al_docente(self):
        propio = self.crear_problema(titulo="Propio")
        docente_b = self.crear_docente("docente-b")
        curso_b = Curso.objects.create(docente=docente_b, nombre="Curso de B", lenguaje=self.curso.lenguaje)
        ajeno = self.crear_problema(curso=curso_b, titulo="De B")
        self.autenticar(self.docente)

        # user-api cuenta lo que se le pida; si devolviera de más, admin-api solo arma filas con los problemas del curso.
        filas = [
            {"problema_id": propio.pk, "intentos": 2, "correctos": 1},
            {"problema_id": ajeno.pk, "intentos": 9, "correctos": 9},
        ]
        with patch(RESUMEN, return_value=filas) as pregunta:
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            respuesta.json(),
            [{"id": propio.pk, "titulo": "Propio", "dificultad": propio.dificultad, "intentos": 2, "correctos": 1}],
        )
        pregunta.assert_called_once_with([propio.pk])

    def test_el_resumen_no_trae_casos_de_prueba_ni_codigo(self):
        problema = self.crear_problema(codigo_base=MARCA)
        CasoPrueba.objects.create(problema=problema, entrada=MARCA, salida_esperada=MARCA, es_publico=False)
        CasoPrueba.objects.create(problema=problema, entrada=MARCA, salida_esperada=MARCA, es_publico=True)
        self.autenticar(self.docente)

        # Aunque user-api mandara campos de más (por ejemplo el código de un envío), no se reenvían.
        fila = {"problema_id": problema.pk, "intentos": 1, "correctos": 0, "codigo": MARCA}
        with patch(RESUMEN, return_value=[fila]):
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 200)
        self.assertNotIn(MARCA, respuesta.content.decode())
