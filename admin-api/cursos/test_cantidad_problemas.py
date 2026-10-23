# CA-007.2: cada curso muestra cuántos problemas tiene. CA-024.3/024.4: un problema descartado
# queda archivado y ya no cuenta como problema del curso.
from django.db import connection
from django.test.utils import CaptureQueriesContext

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CURSOS_URL, CursosTestCase
from problemas.models import Problema
from problemas.serializers import DIFICULTADES


class CantidadProblemasDeCursoTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.autenticar(self.docente)

    def crear_curso(self, nombre, docente=None):
        return Curso.objects.create(
            docente=docente or self.docente, nombre=nombre, lenguaje=LENGUAJES[0]
        )

    def crear_problemas(self, curso, cantidad, **extra):
        return [
            Problema.objects.create(
                curso=curso,
                titulo=f"Problema {i}",
                enunciado="Suma dos números.",
                lenguaje=LENGUAJES[0],
                dificultad=DIFICULTADES[0],
                plataforma=Problema.Plataforma.WEB,
                tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
                **extra,
            )
            for i in range(cantidad)
        ]

    def descartar(self, problema):
        # La ruta real de «Descartar», con la confirmación que exige.
        response = self.client.post(
            f"/api/problemas/{problema.pk}/descartar/",
            {"confirmar": True},
            format="json",
        )
        self.assertEqual(response.status_code, 200)

    def cantidades(self, **parametros):
        response = self.client.get(CURSOS_URL, parametros)
        self.assertEqual(response.status_code, 200)
        return {curso["nombre"]: curso["cantidad_problemas"] for curso in response.data}

    def test_descartar_un_problema_baja_el_conteo(self):
        curso = self.crear_curso("Algoritmos I")
        problemas = self.crear_problemas(curso, 3)
        self.assertEqual(self.cantidades()["Algoritmos I"], 3)

        self.descartar(problemas[0])

        self.assertEqual(self.cantidades()["Algoritmos I"], 2)

    def test_con_todos_los_problemas_descartados_el_curso_muestra_cero(self):
        curso = self.crear_curso("Algoritmos I")
        for problema in self.crear_problemas(curso, 2):
            self.descartar(problema)

        self.assertEqual(self.cantidades()["Algoritmos I"], 0)

    def test_sin_problemas_muestra_cero_y_sin_descartes_muestra_todos(self):
        self.crear_curso("Vacío")
        self.crear_problemas(self.crear_curso("Completo"), 4)

        self.assertEqual(self.cantidades(), {"Vacío": 0, "Completo": 4})

    def test_un_problema_pendiente_de_revision_si_cuenta(self):
        curso = self.crear_curso("Algoritmos I")
        self.crear_problemas(curso, 1, generado_por_ia=True)

        self.assertEqual(self.cantidades()["Algoritmos I"], 1)

    def test_descartar_en_un_curso_no_cambia_otros_cursos(self):
        curso = self.crear_curso("Algoritmos I")
        otro_curso = self.crear_curso("Estructuras")
        problema = self.crear_problemas(curso, 2)[0]
        self.crear_problemas(otro_curso, 2)
        docente_b = self.crear_docente("docente-b")
        curso_ajeno = self.crear_curso("Ajeno", docente=docente_b)
        self.crear_problemas(curso_ajeno, 2)

        self.descartar(problema)

        self.assertEqual(self.cantidades(), {"Algoritmos I": 1, "Estructuras": 2})
        self.autenticar(docente_b)
        self.assertEqual(self.cantidades(), {"Ajeno": 2})

    def test_las_consultas_no_crecen_con_la_cantidad_de_cursos_ni_de_problemas(self):
        self.crear_problemas(self.crear_curso("Curso 0"), 1)
        with CaptureQueriesContext(connection) as con_uno:
            self.cantidades()
        for i in range(1, 5):
            problemas = self.crear_problemas(self.crear_curso(f"Curso {i}"), 3)
            problemas[0].archivar()

        with CaptureQueriesContext(connection) as con_cinco:
            self.assertEqual(len(self.cantidades()), 5)

        self.assertEqual(len(con_cinco), len(con_uno))

    def test_el_conteo_sigue_bien_al_buscar_por_nombre(self):
        python = self.crear_curso("Python básico")
        problemas = self.crear_problemas(python, 3)
        self.crear_problemas(self.crear_curso("Java"), 1)

        self.descartar(problemas[0])

        self.assertEqual(self.cantidades(search="python"), {"Python básico": 2})
