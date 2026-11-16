# H4 (D13): el tema del problema. La IA lo propone al generar, el docente lo corrige al revisar y le llega a
# user-api por la ruta interna (HU56 lo necesita para el resumen de la sesión).
from copy import deepcopy
from unittest import mock

from problemas.models import CasoPrueba, Problema
from problemas.test_generacion import ESCRITURA, PREDICCION
from problemas.test_interno import InternoBase

CLIENTE = "problemas.views.generar_problemas"


class TemaTests(InternoBase):
    def datos(self, **cambios):
        datos = {
            "titulo": "Suma",
            "enunciado": "Suma dos números.",
            "lenguaje": self.curso.lenguaje,
            "dificultad": "facil",
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
            "casos_prueba": [{"entrada": "2 3", "salida_esperada": "5", "es_publico": True}],
        }
        datos.update(cambios)
        return datos

    def test_un_problema_nuevo_nace_sin_tema(self):
        self.assertEqual(self.crear_problema().tema, "")

    def test_el_docente_crea_un_problema_con_tema_y_lo_recibe_de_vuelta(self):
        self.autenticar(self.docente)

        response = self.client.post(
            f"/api/cursos/{self.curso.pk}/problemas/", self.datos(tema="bucles"), format="json"
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data["tema"], "bucles")
        self.assertEqual(Problema.objects.get(pk=response.data["id"]).tema, "bucles")

    def test_sin_tema_en_el_cuerpo_queda_vacio(self):
        self.autenticar(self.docente)

        response = self.client.post(f"/api/cursos/{self.curso.pk}/problemas/", self.datos(), format="json")

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data["tema"], "")

    def test_un_tema_de_mas_de_60_caracteres_da_400(self):
        self.autenticar(self.docente)

        response = self.client.post(
            f"/api/cursos/{self.curso.pk}/problemas/", self.datos(tema="x" * 61), format="json"
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("tema", response.data)
        self.assertEqual(Problema.objects.count(), 0)

    def test_el_docente_corrige_el_tema_y_un_put_sin_el_lo_vacia(self):
        problema = self.crear_problema(generado_por_ia=True, estado="pendiente", tema="listas")
        CasoPrueba.objects.create(problema=problema, entrada="2 3", salida_esperada="5", es_publico=True)
        self.autenticar(self.docente)

        nuevo = self.client.put(f"/api/problemas/{problema.pk}/", self.datos(tema="diccionarios"), format="json")
        self.assertEqual(nuevo.status_code, 200)
        problema.refresh_from_db()
        self.assertEqual(problema.tema, "diccionarios")

        sin_tema = self.client.put(f"/api/problemas/{problema.pk}/", self.datos(), format="json")
        self.assertEqual(sin_tema.status_code, 200)
        problema.refresh_from_db()
        self.assertEqual(problema.tema, "")

    def test_la_bandeja_de_revision_muestra_el_tema_que_propuso_la_ia(self):
        self.crear_problema(generado_por_ia=True, estado="pendiente", tema="recursividad")
        self.autenticar(self.docente)

        response = self.client.get(f"/api/problemas/{Problema.objects.get().pk}/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["tema"], "recursividad")


class TemaGeneradoTests(InternoBase):
    def generar(self, *problemas):
        self.autenticar(self.docente)
        with mock.patch(CLIENTE, return_value=deepcopy(list(problemas))):
            return self.client.post(
                f"/api/cursos/{self.curso.pk}/problemas/generar/",
                {"tema": "operadores", "nivel": "facil", "cantidad": len(problemas)},
                format="json",
            )

    def test_el_tema_que_propone_la_ia_se_guarda(self):
        response = self.generar({**ESCRITURA, "tema": "operadores aritméticos"}, PREDICCION)

        self.assertEqual(response.status_code, 201)
        self.assertEqual(
            sorted(Problema.objects.values_list("tema", flat=True)), ["", "operadores aritméticos"]
        )

    def test_un_tema_demasiado_largo_se_recorta_y_no_tumba_la_tanda(self):
        response = self.generar({**ESCRITURA, "tema": "t" * 100})

        self.assertEqual(response.status_code, 201)
        self.assertEqual(Problema.objects.get().tema, "t" * 60)

    def test_un_tema_que_no_es_texto_no_tumba_la_tanda(self):
        response = self.generar({**ESCRITURA, "tema": 5})

        self.assertEqual(response.status_code, 201)
        self.assertEqual(Problema.objects.get().tema, "5")


class TemaInternoTests(InternoBase):
    def test_el_detalle_y_el_listado_internos_traen_el_tema(self):
        problema = self.crear_problema(tema="bucles")

        detalle = self.pedir(f"/internal/problemas/{problema.pk}/")
        listado = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/")

        self.assertEqual(detalle.status_code, 200)
        self.assertEqual(detalle.data["tema"], "bucles")
        self.assertEqual(listado.data[0]["tema"], "bucles")

    def test_un_problema_sin_tema_llega_con_el_texto_vacio(self):
        problema = self.crear_problema()

        self.assertEqual(self.pedir(f"/internal/problemas/{problema.pk}/").data["tema"], "")
