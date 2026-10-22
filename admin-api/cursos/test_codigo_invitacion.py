# CA-26.1: cada curso, nuevo o ya existente, tiene un código único con fecha de vencimiento.
from datetime import timedelta

from django.db import connection
from django.db.migrations.executor import MigrationExecutor
from django.test import TransactionTestCase, override_settings
from django.utils import timezone

from cursos.models import ALFABETO_CODIGO, Curso
from cursos.tests import CursosTestCase


class CodigoDeInvitacionTests(CursosTestCase):
    def crear_curso(self, nombre="Algoritmos I"):
        return Curso.objects.create(docente=self.crear_docente(f"docente-{nombre}"), nombre=nombre, lenguaje="python")

    def test_un_curso_nuevo_recibe_codigo_de_8_caracteres_legibles(self):
        curso = self.crear_curso()

        self.assertEqual(len(curso.codigo_invitacion), 8)
        self.assertTrue(set(curso.codigo_invitacion) <= set(ALFABETO_CODIGO))

    def test_dos_cursos_tienen_codigos_distintos(self):
        self.assertNotEqual(self.crear_curso("A").codigo_invitacion, self.crear_curso("B").codigo_invitacion)

    def test_vence_a_los_30_dias_por_defecto(self):
        antes = timezone.now()
        curso = self.crear_curso()

        self.assertAlmostEqual(
            curso.codigo_vence, antes + timedelta(days=30), delta=timedelta(minutes=1)
        )

    @override_settings(INVITACION_DIAS=7)
    def test_los_dias_de_vigencia_se_configuran(self):
        antes = timezone.now()
        curso = self.crear_curso()

        self.assertAlmostEqual(curso.codigo_vence, antes + timedelta(days=7), delta=timedelta(minutes=1))


class MigracionDeCursosExistentesTests(TransactionTestCase):
    # Vuelve a la migración 0002 (sin códigos), crea cursos "viejos" y aplica las siguientes.
    ANTES = [("cursos", "0002_curso_codigo_invitacion_codigo_vence")]

    def tearDown(self):
        # Deja la base en la última migración para no afectar a otras pruebas.
        ejecutor = MigrationExecutor(connection)
        ejecutor.migrate(ejecutor.loader.graph.leaf_nodes())

    def test_los_cursos_ya_creados_reciben_codigo_unico_y_vencimiento(self):
        ejecutor = MigrationExecutor(connection)
        ejecutor.migrate(self.ANTES)
        apps = ejecutor.loader.project_state(self.ANTES).apps
        User = apps.get_model("auth", "User")
        Docente = apps.get_model("cuentas", "Docente")
        CursoAntiguo = apps.get_model("cursos", "Curso")
        docente = Docente.objects.create(
            user=User.objects.create(username="docente-migracion"),
            nombre="Docente",
            email="docente@example.com",
            institucion="example.com",
        )
        for nombre in ("A", "B", "C"):
            CursoAntiguo.objects.create(docente=docente, nombre=nombre, lenguaje="python")

        ejecutor = MigrationExecutor(connection)
        ejecutor.migrate(ejecutor.loader.graph.leaf_nodes())

        codigos = list(Curso.objects.values_list("codigo_invitacion", flat=True))
        self.assertEqual(len(set(codigos)), 3)
        self.assertTrue(all(len(c) == 8 for c in codigos))
        self.assertFalse(Curso.objects.filter(codigo_vence__isnull=True).exists())
