# CA-20.3: el problema descartado queda archivado en lugar de eliminarse.
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.models import Problema
from problemas.serializers import DIFICULTADES


class ArchivadoDeProblemaTests(CursosTestCase):
    def crear_problema(self):
        curso = Curso.objects.create(docente=self.crear_docente("docente-a"), nombre="Algoritmos I", lenguaje=LENGUAJES[0])
        return Problema.objects.create(
            curso=curso,
            titulo="Suma",
            enunciado="Suma dos números.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.AMBAS,
            tipo_ejercicio=Problema.TipoEjercicio.OPCION_MULTIPLE,
        )

    def test_un_problema_nuevo_no_esta_archivado(self):
        self.assertFalse(self.crear_problema().archivado)

    def test_archivar_marca_el_problema_y_sigue_en_la_base(self):
        problema = self.crear_problema()

        problema.archivar()

        self.assertTrue(Problema.objects.get(pk=problema.pk).archivado)
