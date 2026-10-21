# CA-019.5: el sistema registra quién aprobó el problema y la fecha de aprobación.
from django.utils import timezone

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.models import Problema
from problemas.serializers import DIFICULTADES


class RegistroDeAprobacionTests(CursosTestCase):
    def crear_problema(self, docente):
        curso = Curso.objects.create(docente=docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0])
        return Problema.objects.create(
            curso=curso,
            titulo="Suma",
            enunciado="Suma dos números.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.AMBAS,
            tipo_ejercicio=Problema.TipoEjercicio.OPCION_MULTIPLE,
        )

    def test_un_problema_nuevo_no_tiene_aprobacion(self):
        problema = self.crear_problema(self.crear_docente("docente-a"))

        self.assertIsNone(problema.aprobado_por)
        self.assertIsNone(problema.fecha_aprobacion)

    def test_registrar_aprobacion_guarda_el_docente_y_la_fecha(self):
        autor = self.crear_docente("docente-a")
        revisor = self.crear_docente("docente-b")
        problema = self.crear_problema(autor)
        antes = timezone.now()

        problema.registrar_aprobacion(revisor)

        problema.refresh_from_db()
        self.assertEqual(problema.aprobado_por, revisor)
        self.assertGreaterEqual(problema.fecha_aprobacion, antes)

    def test_si_se_elimina_el_docente_se_conserva_la_fecha(self):
        autor = self.crear_docente("docente-a")
        revisor = self.crear_docente("docente-b")
        problema = self.crear_problema(autor)
        problema.registrar_aprobacion(revisor)

        revisor.delete()

        problema.refresh_from_db()
        self.assertIsNone(problema.aprobado_por)
        self.assertIsNotNone(problema.fecha_aprobacion)
