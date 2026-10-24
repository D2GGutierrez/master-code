# CA-028.1 y CA-028.4 (HU28-T4): con dos docentes, cada uno ve solo los inscritos de su curso.
# Completa a test_inscritos.py (un solo docente): aquí user-api responde distinto para cada curso,
# así se distingue de quién es cada lista, y se comprueba que los rechazos no llegan a user-api.
from unittest.mock import patch

from django.contrib.auth.models import Group, User

from cuentas.tokens import crear_tokens
from cursos.inscritos import UserApiNoDisponible
from cursos.models import Curso
from cursos.tests import CursosTestCase

CURSO_INSCRITOS = "cursos.views.inscritos_de"


class InscritosSoloDeCursosPropiosTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente_a = self.crear_docente("docente-a")
        self.docente_b = self.crear_docente("docente-b")
        self.curso_a = Curso.objects.create(
            docente=self.docente_a, nombre="Algoritmos I", lenguaje="python"
        )
        self.curso_b = Curso.objects.create(
            docente=self.docente_b, nombre="Java Básico", lenguaje="java"
        )
        # Lo que contestaría user-api para cada curso: correos distintos para saber de dónde vino cada lista.
        self.inscritos = {
            self.curso_a.pk: [
                {"email": "ana@example.com", "fecha": "2026-10-20T10:00:00Z"}
            ],
            self.curso_b.pk: [
                {"email": "beto@example.com", "fecha": "2026-10-21T10:00:00Z"},
                {"email": "bruno@example.com", "fecha": "2026-10-22T10:00:00Z"},
            ],
        }

    def pedir(self, curso_id, **parche):
        parche.setdefault("side_effect", lambda curso_id: self.inscritos[curso_id])
        with patch(CURSO_INSCRITOS, **parche) as pregunta:
            respuesta = self.client.get(f"/api/cursos/{curso_id}/inscritos/")
        return respuesta, pregunta

    def correos(self, respuesta):
        return [inscrito["email"] for inscrito in respuesta.json()]

    def test_el_docente_a_ve_solo_los_inscritos_de_su_curso(self):
        self.autenticar(self.docente_a)

        respuesta, pregunta = self.pedir(self.curso_a.pk)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(self.correos(respuesta), ["ana@example.com"])
        pregunta.assert_called_once_with(self.curso_a.pk)

    def test_el_docente_b_ve_solo_los_inscritos_de_su_curso(self):
        self.autenticar(self.docente_b)

        respuesta, pregunta = self.pedir(self.curso_b.pk)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            self.correos(respuesta), ["beto@example.com", "bruno@example.com"]
        )
        pregunta.assert_called_once_with(self.curso_b.pk)

    def test_un_docente_no_ve_los_inscritos_del_curso_del_otro_ni_se_consulta_a_user_api(
        self,
    ):
        casos = [
            (self.docente_a, self.curso_b, ["beto@example.com", "bruno@example.com"]),
            (self.docente_b, self.curso_a, ["ana@example.com"]),
        ]
        for docente, curso_ajeno, correos_ajenos in casos:
            with self.subTest(docente=docente.nombre):
                self.autenticar(docente)

                respuesta, pregunta = self.pedir(curso_ajeno.pk)

                self.assertEqual(respuesta.status_code, 403)
                for correo in correos_ajenos:
                    self.assertNotIn(correo, respuesta.content.decode())
                pregunta.assert_not_called()

    def test_curso_inexistente_sin_token_o_estudiante_no_llegan_a_user_api(self):
        estudiante = User.objects.create_user(
            username="eva@example.com",
            email="eva@example.com",
            password="Clave-Falsa-2026",
        )
        # El rol estudiante es un grupo: sin él, crear_tokens no emite token.
        estudiante.groups.add(Group.objects.get_or_create(name="estudiante")[0])
        _, access_estudiante = crear_tokens(estudiante, "user-web")
        casos = {
            "curso inexistente": (
                f"Bearer {crear_tokens(self.docente_a.user, 'admin-web')[1]}",
                999999,
                404,
            ),
            "sin token": (None, self.curso_a.pk, 401),
            "estudiante": (f"Bearer {access_estudiante}", self.curso_a.pk, 403),
        }
        for nombre, (cabecera, curso_id, esperado) in casos.items():
            with self.subTest(nombre):
                self.client.credentials(
                    **({"HTTP_AUTHORIZATION": cabecera} if cabecera else {})
                )

                respuesta, pregunta = self.pedir(curso_id)

                self.assertEqual(respuesta.status_code, esperado)
                pregunta.assert_not_called()

    def test_con_user_api_caido_el_dueno_recibe_503_y_el_ajeno_sigue_recibiendo_403(
        self,
    ):
        caido = {"side_effect": UserApiNoDisponible("sin conexión")}
        self.autenticar(self.docente_a)

        propio, _ = self.pedir(self.curso_a.pk, **caido)
        ajeno, pregunta = self.pedir(self.curso_b.pk, **caido)

        self.assertEqual(propio.status_code, 503)
        self.assertTrue(propio.json()["detail"].strip())
        self.assertEqual(ajeno.status_code, 403)
        pregunta.assert_not_called()
