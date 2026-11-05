# CA-061.1 a CA-061.3: la evaluación guarda fecha de inicio, fecha de fin y límite de tiempo.
from datetime import timedelta

from django.conf import settings
from django.contrib.auth.models import User
from django.db import IntegrityError, connection, transaction
from django.db.models import ProtectedError
from django.test import TestCase
from django.utils import timezone

from cuentas.models import Docente
from cursos.models import Curso
from evaluaciones.models import Evaluacion, EvaluacionProblema
from problemas.models import Problema


class EvaluacionModeloTests(TestCase):
    def test_guarda_el_curso_la_ventana_y_el_limite_y_vive_en_admin_schema(self):
        email = f"docente-a@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(username=email, email=email, password="Clave-Falsa-2026")
        docente = Docente.objects.create(
            user=user, nombre="docente-a", email=email, institucion=settings.DOMINIO_INSTITUCIONAL
        )
        curso = Curso.objects.create(docente=docente, nombre="Algoritmos I", lenguaje="python")
        inicio = timezone.now() + timedelta(days=1)
        fin = inicio + timedelta(hours=2)

        creada = Evaluacion.objects.create(
            curso=curso, fecha_inicio=inicio, fecha_fin=fin, limite_minutos=90
        )

        leida = Evaluacion.objects.get(pk=creada.pk)
        self.assertEqual(leida.curso, curso)
        self.assertEqual(leida.fecha_inicio, inicio)
        self.assertEqual(leida.fecha_fin, fin)
        self.assertEqual(leida.limite_minutos, 90)
        # ADR-1: la tabla está en admin_schema, no en public.
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT table_schema FROM information_schema.tables WHERE table_name = 'admin_evaluacion'"
            )
            self.assertEqual(cursor.fetchall(), [("admin_schema",)])


# CA-062.1 y CA-062.2: la evaluación guarda qué problemas contiene.
class EvaluacionProblemaModeloTests(TestCase):
    def setUp(self):
        email = f"docente-a@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(username=email, email=email, password="Clave-Falsa-2026")
        docente = Docente.objects.create(
            user=user, nombre="docente-a", email=email, institucion=settings.DOMINIO_INSTITUCIONAL
        )
        self.curso = Curso.objects.create(docente=docente, nombre="Algoritmos I", lenguaje="python")
        inicio = timezone.now() + timedelta(days=1)
        self.evaluacion = self.crear_evaluacion(inicio)
        self.suma = self.crear_problema("Suma")
        self.ordena = self.crear_problema("Ordena")

    def crear_evaluacion(self, inicio):
        return Evaluacion.objects.create(
            curso=self.curso, fecha_inicio=inicio, fecha_fin=inicio + timedelta(hours=2), limite_minutos=90
        )

    def crear_problema(self, titulo):
        return Problema.objects.create(
            curso=self.curso,
            titulo=titulo,
            enunciado="Resuelve.",
            lenguaje="python",
            dificultad="facil",
            plataforma=Problema.Plataforma.WEB,
            tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
        )

    def test_una_evaluacion_contiene_varios_problemas(self):
        EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.suma)
        EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.ordena)

        contenidos = EvaluacionProblema.objects.filter(evaluacion=self.evaluacion)

        self.assertEqual({fila.problema for fila in contenidos}, {self.suma, self.ordena})

    def test_un_problema_puede_estar_en_varias_evaluaciones(self):
        otra = self.crear_evaluacion(timezone.now() + timedelta(days=7))

        EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.suma)
        EvaluacionProblema.objects.create(evaluacion=otra, problema=self.suma)

        self.assertEqual(EvaluacionProblema.objects.filter(problema=self.suma).count(), 2)

    def test_el_mismo_problema_no_entra_dos_veces_en_la_misma_evaluacion(self):
        EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.suma)

        with self.assertRaises(IntegrityError), transaction.atomic():
            EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.suma)

    def test_al_borrar_la_evaluacion_se_borra_su_relacion_pero_no_los_problemas(self):
        EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.suma)

        self.evaluacion.delete()

        self.assertEqual(EvaluacionProblema.objects.count(), 0)
        self.assertTrue(Problema.objects.filter(pk=self.suma.pk).exists())

    def test_no_se_borra_un_problema_que_esta_en_una_evaluacion(self):
        EvaluacionProblema.objects.create(evaluacion=self.evaluacion, problema=self.suma)

        with self.assertRaises(ProtectedError):
            self.suma.delete()

        self.assertTrue(EvaluacionProblema.objects.filter(problema=self.suma).exists())

    def test_la_tabla_esta_en_admin_schema(self):
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT table_schema FROM information_schema.tables WHERE table_name = 'admin_evaluacion_problema'"
            )
            self.assertEqual(cursor.fetchall(), [("admin_schema",)])
