# HU61-T2: POST /api/cursos/<id>/evaluaciones/ crea la evaluación de un curso propio.
from datetime import timedelta

from django.contrib.auth.models import Group, User
from django.utils import timezone

from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.tests import CursosTestCase
from evaluaciones.models import Evaluacion


class CrearEvaluacionTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.curso = Curso.objects.create(docente=self.docente, nombre="Algoritmos I", lenguaje="python")
        self.autenticar(self.docente)
        inicio = timezone.now() + timedelta(days=1)
        self.cuerpo = {
            "fecha_inicio": inicio.isoformat(),
            "fecha_fin": (inicio + timedelta(hours=2)).isoformat(),
            "limite_minutos": 90,
        }

    def url(self, curso_id=None):
        return f"/api/cursos/{curso_id or self.curso.pk}/evaluaciones/"

    def test_crea_la_evaluacion_del_curso_con_fechas_y_limite(self):
        response = self.client.post(self.url(), self.cuerpo, format="json")

        self.assertEqual(response.status_code, 201)
        evaluacion = Evaluacion.objects.get()
        self.assertEqual(evaluacion.curso, self.curso)
        self.assertEqual(evaluacion.limite_minutos, 90)
        self.assertEqual(evaluacion.fecha_fin - evaluacion.fecha_inicio, timedelta(hours=2))
        self.assertEqual(response.data["id"], evaluacion.pk)
        self.assertEqual(response.data["curso"], self.curso.pk)
        self.assertEqual(response.data["limite_minutos"], 90)

    def test_la_respuesta_trae_el_estado_calculado(self):
        response = self.client.post(self.url(), self.cuerpo, format="json")

        self.assertEqual(response.data["estado"], "programada")

    def test_una_ventana_que_ya_empezo_sale_activa(self):
        ahora = timezone.now()
        cuerpo = {
            "fecha_inicio": (ahora - timedelta(hours=1)).isoformat(),
            "fecha_fin": (ahora + timedelta(hours=1)).isoformat(),
            "limite_minutos": 30,
        }

        response = self.client.post(self.url(), cuerpo, format="json")

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data["estado"], "activa")

    def test_cada_campo_obligatorio_faltante_da_400_con_su_error_y_no_guarda(self):
        for campo in ["fecha_inicio", "fecha_fin", "limite_minutos"]:
            with self.subTest(campo):
                cuerpo = {k: v for k, v in self.cuerpo.items() if k != campo}

                response = self.client.post(self.url(), cuerpo, format="json")

                self.assertEqual(response.status_code, 400)
                self.assertIn(campo, response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_fecha_con_formato_invalido_da_400(self):
        response = self.client.post(self.url(), {**self.cuerpo, "fecha_inicio": "mañana"}, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertIn("fecha_inicio", response.data)

    def test_limite_cero_negativo_o_no_numerico_da_400(self):
        for limite in [0, -5, "mucho"]:
            with self.subTest(limite):
                response = self.client.post(self.url(), {**self.cuerpo, "limite_minutos": limite}, format="json")

                self.assertEqual(response.status_code, 400)
                self.assertIn("limite_minutos", response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_el_curso_sale_de_la_ruta_no_del_cuerpo(self):
        otro = Curso.objects.create(docente=self.docente, nombre="Otro curso", lenguaje="python")

        self.client.post(self.url(), {**self.cuerpo, "curso": otro.pk}, format="json")

        self.assertEqual(Evaluacion.objects.get().curso, self.curso)

    def test_un_curso_ajeno_da_el_mismo_404_que_uno_inexistente_y_no_guarda(self):
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="De B", lenguaje="python")

        de_otro = self.client.post(self.url(ajeno.pk), self.cuerpo, format="json")
        que_no_existe = self.client.post(self.url(ajeno.pk + 100_000), self.cuerpo, format="json")

        self.assertEqual(de_otro.status_code, 404)
        self.assertEqual(que_no_existe.status_code, 404)
        self.assertEqual(de_otro.content, que_no_existe.content)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_sin_token_da_401(self):
        self.client.credentials()

        response = self.client.post(self.url(), self.cuerpo, format="json")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_una_cuenta_que_no_es_docente_da_403_y_no_guarda(self):
        user = User.objects.create_user(
            username="estudiante@ejemplo.edu.pe", email="estudiante@ejemplo.edu.pe", password="Clave-Falsa-2026"
        )
        user.groups.add(Group.objects.get_or_create(name="estudiante")[0])
        _, access = crear_tokens(user, "user-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

        response = self.client.post(self.url(), self.cuerpo, format="json")

        self.assertEqual(response.status_code, 403)
        self.assertEqual(Evaluacion.objects.count(), 0)
