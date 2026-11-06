# HU64-T2: GET /internal/evaluaciones/<id>/, la ventana de una evaluación para user-api (D3, D11).
from datetime import timedelta

from django.utils import timezone
from django.utils.dateparse import parse_datetime

from evaluaciones.models import Evaluacion, EvaluacionProblema
from problemas.test_interno import InternoBase


class EvaluacionInternaTests(InternoBase):
    def crear_evaluacion(self, inicio, fin, minutos=60, problemas=()):
        evaluacion = Evaluacion.objects.create(
            curso=self.curso, fecha_inicio=inicio, fecha_fin=fin, limite_minutos=minutos
        )
        for problema in problemas:
            EvaluacionProblema.objects.create(evaluacion=evaluacion, problema=problema)
        return evaluacion

    def url(self, evaluacion):
        return f"/internal/evaluaciones/{evaluacion.pk}/"

    def test_sin_clave_se_rechaza(self):
        evaluacion = self.crear_evaluacion(timezone.now(), timezone.now() + timedelta(hours=1))

        self.assertEqual(self.pedir(self.url(evaluacion), clave=None).status_code, 403)

    def test_con_clave_equivocada_se_rechaza(self):
        evaluacion = self.crear_evaluacion(timezone.now(), timezone.now() + timedelta(hours=1))

        self.assertEqual(self.pedir(self.url(evaluacion), clave="otra").status_code, 403)

    def test_un_token_de_docente_no_reemplaza_la_clave(self):
        evaluacion = self.crear_evaluacion(timezone.now(), timezone.now() + timedelta(hours=1))
        self.autenticar(self.docente)

        self.assertEqual(self.client.get(self.url(evaluacion)).status_code, 403)

    def test_devuelve_la_ventana_el_limite_el_curso_y_los_problemas(self):
        suma = self.crear_problema(titulo="Suma")
        resta = self.crear_problema(titulo="Resta")
        inicio = timezone.now() + timedelta(days=1)
        evaluacion = self.crear_evaluacion(inicio, inicio + timedelta(hours=2), minutos=90, problemas=[suma, resta])

        respuesta = self.pedir(self.url(evaluacion))

        self.assertEqual(respuesta.status_code, 200)
        datos = respuesta.json()
        self.assertEqual(datos["id"], evaluacion.pk)
        self.assertEqual(datos["curso_id"], self.curso.pk)
        self.assertEqual(datos["limite_minutos"], 90)
        self.assertEqual(parse_datetime(datos["fecha_inicio"]), inicio)
        self.assertEqual(parse_datetime(datos["fecha_fin"]), inicio + timedelta(hours=2))
        self.assertEqual(datos["problemas"], [suma.pk, resta.pk])

    def test_el_estado_sale_de_comparar_la_ventana_con_la_hora_actual(self):
        ahora = timezone.now()
        casos = {
            "programada": (ahora + timedelta(hours=1), ahora + timedelta(hours=3)),
            "activa": (ahora - timedelta(hours=1), ahora + timedelta(hours=1)),
            "cerrada": (ahora - timedelta(hours=3), ahora - timedelta(hours=1)),
        }
        for esperado, (inicio, fin) in casos.items():
            with self.subTest(esperado):
                evaluacion = self.crear_evaluacion(inicio, fin)

                self.assertEqual(self.pedir(self.url(evaluacion)).json()["estado"], esperado)

    # Los mismos problemas que ve un estudiante: aprobados y no descartados (ADR-2).
    def test_no_entrega_los_problemas_que_ya_no_se_ven(self):
        visible = self.crear_problema(titulo="Visible")
        archivado = self.crear_problema(titulo="Archivado")
        evaluacion = self.crear_evaluacion(
            timezone.now(), timezone.now() + timedelta(hours=1), problemas=[visible, archivado]
        )
        archivado.archivar()

        self.assertEqual(self.pedir(self.url(evaluacion)).json()["problemas"], [visible.pk])

    def test_una_evaluacion_inexistente_da_404(self):
        evaluacion = self.crear_evaluacion(timezone.now(), timezone.now() + timedelta(hours=1))

        self.assertEqual(self.pedir(f"/internal/evaluaciones/{evaluacion.pk + 100_000}/").status_code, 404)

    def test_no_trae_nada_del_contenido_de_los_problemas(self):
        problema = self.crear_problema(titulo="TITULO-SECRETO-XYZ")
        evaluacion = self.crear_evaluacion(
            timezone.now(), timezone.now() + timedelta(hours=1), problemas=[problema]
        )

        texto = self.pedir(self.url(evaluacion)).content.decode()

        self.assertNotIn("TITULO-SECRETO-XYZ", texto)
        self.assertNotIn("Suma dos números", texto)
