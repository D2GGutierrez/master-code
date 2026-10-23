# CA-28.1 y CA-28.4: el docente ve los inscritos de sus cursos y de ningún otro.
import json
import urllib.error
from unittest.mock import MagicMock, patch

from django.contrib.auth.models import Group, User
from django.test import SimpleTestCase, override_settings

from cuentas.tokens import crear_tokens
from cursos.inscritos import UserApiNoDisponible, inscritos_de
from cursos.models import Curso
from cursos.tests import CursosTestCase

CURSO_INSCRITOS = "cursos.views.inscritos_de"


class ListaDeInscritosTests(CursosTestCase):
    def crear_curso(self, docente):
        return Curso.objects.create(docente=docente, nombre="Algoritmos I", lenguaje="python")

    def crear_estudiante(self, email, nombre):
        user = User.objects.create_user(username=email, email=email, first_name=nombre, password="Clave-Falsa-2026")
        # El rol estudiante es un grupo: sin él, crear_tokens no emite token.
        user.groups.add(Group.objects.get_or_create(name="estudiante")[0])
        return user

    def test_el_docente_ve_los_inscritos_de_su_curso_con_su_nombre(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.crear_estudiante("ana@example.com", "Ana Pérez")
        self.autenticar(docente)

        with patch(CURSO_INSCRITOS, return_value=[{"email": "ana@example.com", "fecha": "2026-10-20T10:00:00Z"}]) as pregunta:
            respuesta = self.client.get(f"/api/cursos/{curso.pk}/inscritos/")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            respuesta.json(),
            [{"email": "ana@example.com", "nombre": "Ana Pérez", "fecha_inscripcion": "2026-10-20T10:00:00Z"}],
        )
        pregunta.assert_called_once_with(curso.pk)

    def test_un_inscrito_sin_cuenta_conocida_sale_con_nombre_vacio(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)

        with patch(CURSO_INSCRITOS, return_value=[{"email": "fantasma@example.com", "fecha": "2026-10-20T10:00:00Z"}]):
            respuesta = self.client.get(f"/api/cursos/{curso.pk}/inscritos/")

        self.assertEqual(respuesta.json()[0]["nombre"], "")

    def test_un_curso_sin_inscritos_devuelve_lista_vacia(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)

        with patch(CURSO_INSCRITOS, return_value=[]):
            respuesta = self.client.get(f"/api/cursos/{curso.pk}/inscritos/")

        self.assertEqual(respuesta.json(), [])

    def test_los_inscritos_de_un_curso_ajeno_dan_403_y_no_se_consulta_a_user_api(self):
        dueno = self.crear_docente("docente-a")
        curso = self.crear_curso(dueno)
        self.autenticar(self.crear_docente("docente-b"))

        with patch(CURSO_INSCRITOS) as pregunta:
            respuesta = self.client.get(f"/api/cursos/{curso.pk}/inscritos/")

        self.assertEqual(respuesta.status_code, 403)
        pregunta.assert_not_called()

    def test_un_curso_inexistente_da_404(self):
        self.autenticar(self.crear_docente("docente-a"))

        self.assertEqual(self.client.get("/api/cursos/999999/inscritos/").status_code, 404)

    def test_sin_token_da_401(self):
        curso = self.crear_curso(self.crear_docente("docente-a"))

        self.assertEqual(self.client.get(f"/api/cursos/{curso.pk}/inscritos/").status_code, 401)

    def test_un_estudiante_no_entra(self):
        curso = self.crear_curso(self.crear_docente("docente-a"))
        estudiante = self.crear_estudiante("ana@example.com", "Ana")
        _, access = crear_tokens(estudiante, "user-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

        self.assertEqual(self.client.get(f"/api/cursos/{curso.pk}/inscritos/").status_code, 403)

    def test_si_user_api_no_responde_da_503_con_mensaje(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)

        with patch(CURSO_INSCRITOS, side_effect=UserApiNoDisponible("sin conexión")):
            respuesta = self.client.get(f"/api/cursos/{curso.pk}/inscritos/")

        self.assertEqual(respuesta.status_code, 503)
        self.assertIn("detail", respuesta.json())


@override_settings(USER_API_URL="http://user-api:8081/", INTERNAL_API_KEY="clave-de-prueba")
class ClienteDeUserApiTests(SimpleTestCase):
    def respuesta(self, cuerpo):
        simulada = MagicMock()
        simulada.__enter__.return_value.read.return_value = json.dumps(cuerpo).encode()
        return simulada

    def test_pide_la_url_interna_con_la_clave(self):
        with patch("urllib.request.urlopen", return_value=self.respuesta([])) as abrir:
            inscritos_de(7)

        peticion = abrir.call_args.args[0]
        self.assertEqual(peticion.full_url, "http://user-api:8081/internal/cursos/7/inscritos")
        self.assertEqual(peticion.get_header("X-internal-key"), "clave-de-prueba")
        self.assertEqual(abrir.call_args.kwargs["timeout"], 5)

    def test_devuelve_la_lista_que_manda_user_api(self):
        lista = [{"email": "ana@example.com", "fecha": "2026-10-20T10:00:00Z"}]

        with patch("urllib.request.urlopen", return_value=self.respuesta(lista)):
            self.assertEqual(inscritos_de(7), lista)

    def test_un_fallo_de_red_o_http_es_user_api_no_disponible(self):
        for fallo in (urllib.error.URLError("sin conexión"), TimeoutError(), urllib.error.HTTPError("u", 500, "x", {}, None)):
            with patch("urllib.request.urlopen", side_effect=fallo), self.assertRaises(UserApiNoDisponible):
                inscritos_de(7)

    def test_una_respuesta_que_no_es_lista_es_user_api_no_disponible(self):
        with patch("urllib.request.urlopen", return_value=self.respuesta({"error": "x"})), self.assertRaises(UserApiNoDisponible):
            inscritos_de(7)
