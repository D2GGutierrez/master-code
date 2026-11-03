# HU44-T1 (CA-44.1, CA-44.2, CA-44.4, D10): el docente ve los envíos de su curso agrupados por problema, y de ningún
# curso ajeno. Los envíos viven en user-api: aquí se simula su respuesta y se comprueba qué se le pregunta.
import json
import urllib.error
from unittest.mock import MagicMock, patch

from django.contrib.auth.models import Group, User
from django.test import SimpleTestCase, override_settings

from cuentas.tokens import crear_tokens
from cursos.inscritos import UserApiNoDisponible, resumen_envios_de
from cursos.models import Curso
from problemas.test_interno import InternoBase

RESUMEN = "cursos.views.resumen_envios_de"


def url(curso):
    return f"/api/cursos/{curso.pk}/resumen-envios/"


class ResumenDeEnviosDelCursoTests(InternoBase):
    def test_cada_problema_trae_sus_intentos_y_correctos(self):
        suma = self.crear_problema(titulo="Suma", dificultad="facil")
        ordena = self.crear_problema(titulo="Ordena", dificultad="dificil")
        self.autenticar(self.docente)

        filas = [
            {"problema_id": suma.pk, "intentos": 5, "correctos": 2},
            {"problema_id": ordena.pk, "intentos": 4, "correctos": 0},
        ]
        with patch(RESUMEN, return_value=filas) as pregunta:
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            respuesta.json(),
            [
                {"id": suma.pk, "titulo": "Suma", "dificultad": "facil", "intentos": 5, "correctos": 2},
                {"id": ordena.pk, "titulo": "Ordena", "dificultad": "dificil", "intentos": 4, "correctos": 0},
            ],
        )
        pregunta.assert_called_once_with([suma.pk, ordena.pk])

    def test_un_problema_sin_envios_sale_con_ceros(self):
        con_envios = self.crear_problema(titulo="Con envíos")
        sin_envios = self.crear_problema(titulo="Sin envíos")
        self.autenticar(self.docente)

        with patch(RESUMEN, return_value=[{"problema_id": con_envios.pk, "intentos": 1, "correctos": 1}]):
            cuerpo = self.client.get(url(self.curso)).json()

        self.assertEqual([(f["id"], f["intentos"], f["correctos"]) for f in cuerpo],
                         [(con_envios.pk, 1, 1), (sin_envios.pk, 0, 0)])

    def test_solo_cuentan_los_problemas_que_ve_el_estudiante(self):
        visible = self.crear_problema(titulo="Visible")
        self.crear_problema(titulo="Pendiente", estado="pendiente")
        self.crear_problema(titulo="Borrador", estado="borrador")
        self.crear_problema(titulo="Descartado", estado="archivado")
        self.autenticar(self.docente)

        with patch(RESUMEN, return_value=[]) as pregunta:
            cuerpo = self.client.get(url(self.curso)).json()

        self.assertEqual([f["titulo"] for f in cuerpo], ["Visible"])
        pregunta.assert_called_once_with([visible.pk])

    def test_no_mezcla_problemas_de_otro_curso(self):
        propio = self.crear_problema(titulo="Propio")
        otro_curso = Curso.objects.create(docente=self.docente, nombre="Otro", lenguaje=self.curso.lenguaje)
        self.crear_problema(curso=otro_curso, titulo="De otro curso")
        self.autenticar(self.docente)

        with patch(RESUMEN, return_value=[]) as pregunta:
            cuerpo = self.client.get(url(self.curso)).json()

        self.assertEqual([f["titulo"] for f in cuerpo], ["Propio"])
        pregunta.assert_called_once_with([propio.pk])

    def test_un_curso_sin_problemas_visibles_da_lista_vacia_sin_consultar_a_user_api(self):
        self.crear_problema(estado="pendiente")
        self.autenticar(self.docente)

        with patch(RESUMEN) as pregunta:
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), [])
        pregunta.assert_not_called()

    def test_si_user_api_no_responde_da_503_con_mensaje(self):
        self.crear_problema()
        self.autenticar(self.docente)

        with patch(RESUMEN, side_effect=UserApiNoDisponible("sin conexión")):
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 503)
        self.assertIn("detail", respuesta.json())


class AccesoAlResumenDeEnviosTests(InternoBase):
    # CA-44.4: el docente solo ve datos de sus propios cursos.
    def test_un_curso_ajeno_da_403_y_no_se_consulta_a_user_api(self):
        self.crear_problema()
        self.autenticar(self.crear_docente("docente-b"))

        with patch(RESUMEN) as pregunta:
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 403)
        pregunta.assert_not_called()

    def test_un_curso_inexistente_da_404(self):
        self.autenticar(self.docente)

        self.assertEqual(self.client.get("/api/cursos/999999/resumen-envios/").status_code, 404)

    def test_sin_token_da_401(self):
        self.assertEqual(self.client.get(url(self.curso)).status_code, 401)

    def test_un_estudiante_no_entra(self):
        estudiante = User.objects.create_user(username="ana@example.com", email="ana@example.com", password="Clave-Falsa-2026")
        estudiante.groups.add(Group.objects.get_or_create(name="estudiante")[0])
        _, access = crear_tokens(estudiante, "user-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

        with patch(RESUMEN) as pregunta:
            respuesta = self.client.get(url(self.curso))

        self.assertEqual(respuesta.status_code, 403)
        pregunta.assert_not_called()


@override_settings(USER_API_URL="http://user-api:8081/", INTERNAL_API_KEY="clave-de-prueba")
class ClienteDelResumenDeEnviosTests(SimpleTestCase):
    def respuesta(self, cuerpo):
        simulada = MagicMock()
        simulada.__enter__.return_value.read.return_value = json.dumps(cuerpo).encode()
        return simulada

    def test_pide_la_url_interna_con_los_ids_y_la_clave(self):
        with patch("urllib.request.urlopen", return_value=self.respuesta([])) as abrir:
            resumen_envios_de([7, 8, 9])

        peticion = abrir.call_args.args[0]
        self.assertEqual(peticion.full_url, "http://user-api:8081/internal/envios/resumen?problemas=7,8,9")
        self.assertEqual(peticion.get_header("X-internal-key"), "clave-de-prueba")
        self.assertEqual(abrir.call_args.kwargs["timeout"], 5)

    def test_devuelve_la_lista_que_manda_user_api(self):
        lista = [{"problema_id": 7, "intentos": 3, "correctos": 1}]

        with patch("urllib.request.urlopen", return_value=self.respuesta(lista)):
            self.assertEqual(resumen_envios_de([7]), lista)

    def test_un_fallo_de_red_o_http_es_user_api_no_disponible(self):
        for fallo in (urllib.error.URLError("sin conexión"), TimeoutError(), urllib.error.HTTPError("u", 500, "x", {}, None)):
            with patch("urllib.request.urlopen", side_effect=fallo), self.assertRaises(UserApiNoDisponible):
                resumen_envios_de([7])

    def test_una_respuesta_que_no_es_lista_es_user_api_no_disponible(self):
        with patch("urllib.request.urlopen", return_value=self.respuesta({"error": "x"})), self.assertRaises(UserApiNoDisponible):
            resumen_envios_de([7])
