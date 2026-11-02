# CA-061.1 a CA-061.3: la evaluación guarda fecha de inicio, fecha de fin y límite de tiempo.
from datetime import timedelta

from django.conf import settings
from django.contrib.auth.models import User
from django.db import connection
from django.test import TestCase
from django.utils import timezone

from cuentas.models import Docente
from cursos.models import Curso
from evaluaciones.models import Evaluacion


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
