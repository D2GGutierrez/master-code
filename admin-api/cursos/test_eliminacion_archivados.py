# CA-008.2: un curso con problemas no se elimina, aunque estén descartados (PROTECT).
# CA-008.4: el bloqueo explica la causa; si solo quedan descartados, lo dice.
from unittest.mock import patch

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from cursos.views import MENSAJE_CON_PROBLEMAS
from problemas.models import Problema
from problemas.serializers import DIFICULTADES


class EliminarCursoConProblemasArchivadosTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        # user-api no existe en las pruebas: el curso no tiene inscritos.
        parche = patch("cursos.views.inscritos_de", return_value=[])
        self.inscritos_de = parche.start()
        self.addCleanup(parche.stop)
        self.docente = self.crear_docente("docente-a")
        self.autenticar(self.docente)

    def crear_curso(self):
        return Curso.objects.create(
            docente=self.docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0]
        )

    def crear_problema(self, curso, archivado=False):
        return Problema.objects.create(
            curso=curso,
            titulo="Suma de dos números",
            enunciado="Lee dos enteros e imprime su suma.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.WEB,
            tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
            archivado=archivado,
        )

    def borrar(self, curso):
        return self.client.delete(f"/api/cursos/{curso.pk}/")

    def test_con_solo_problemas_descartados_no_se_elimina_y_explica_que_estan_archivados(
        self,
    ):
        curso = self.crear_curso()
        problemas = [self.crear_problema(curso, archivado=True) for _ in range(2)]

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 409)
        mensaje = response.data["detail"].lower()
        self.assertIn("problemas", mensaje)
        self.assertIn("descartados", mensaje)
        self.assertIn("archivados", mensaje)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())
        self.assertEqual(
            Problema.objects.filter(pk__in=[p.pk for p in problemas]).count(), 2
        )

    def test_con_un_problema_activo_y_uno_descartado_el_mensaje_es_el_de_siempre(self):
        curso = self.crear_curso()
        self.crear_problema(curso)
        self.crear_problema(curso, archivado=True)

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.data["detail"], MENSAJE_CON_PROBLEMAS)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())

    def test_con_solo_problemas_activos_el_mensaje_es_el_de_siempre(self):
        curso = self.crear_curso()
        self.crear_problema(curso)
        self.crear_problema(curso)

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 409)
        self.assertEqual(response.data["detail"], MENSAJE_CON_PROBLEMAS)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())

    def test_un_curso_vacio_se_elimina(self):
        curso = self.crear_curso()

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 204)
        self.assertFalse(Curso.objects.filter(pk=curso.pk).exists())

    def test_los_dos_mensajes_son_distintos_con_el_mismo_409_y_la_misma_clave(self):
        solo_archivados = self.crear_curso()
        self.crear_problema(solo_archivados, archivado=True)
        con_activos = self.crear_curso()
        self.crear_problema(con_activos)

        respuesta_archivados = self.borrar(solo_archivados)
        respuesta_activos = self.borrar(con_activos)

        self.assertEqual(respuesta_archivados.status_code, 409)
        self.assertEqual(respuesta_activos.status_code, 409)
        self.assertEqual(set(respuesta_archivados.data), {"detail"})
        self.assertEqual(set(respuesta_activos.data), {"detail"})
        self.assertTrue(respuesta_archivados.data["detail"].strip())
        self.assertNotEqual(
            respuesta_archivados.data["detail"], respuesta_activos.data["detail"]
        )

    def test_un_docente_ajeno_no_puede_borrar_un_curso_con_problemas_descartados(self):
        curso = self.crear_curso()
        problema = self.crear_problema(curso, archivado=True)
        self.autenticar(self.crear_docente("docente-b"))

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 403)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())
        problema.refresh_from_db()
        self.assertTrue(problema.archivado)
        self.inscritos_de.assert_not_called()
