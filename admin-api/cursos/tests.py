# CA-005.1: crear con nombre. CA-005.2: elegir el lenguaje.
# CA-005.3: no se guarda sin nombre. CA-005.4: no se guarda sin lenguaje.
from django.conf import settings
from django.contrib.auth.models import User
from django.test import TestCase
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.serializers import LENGUAJES

CURSOS_URL = "/api/cursos/"
CONTRASENA = "Clave-Falsa-2026"


class CrearCursoTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    def crear_docente(self, usuario):
        email = f"{usuario}@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(
            username=email, email=email, password=CONTRASENA, is_active=True
        )
        return Docente.objects.create(
            user=user,
            nombre=usuario,
            email=email,
            institucion=settings.DOMINIO_INSTITUCIONAL,
        )

    def autenticar(self, docente):
        # Token real, como el que emite el login: la petición pasa por JWTAuthentication.
        _, access = crear_tokens(docente.user, "admin-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

    def test_sin_nombre_devuelve_error_en_nombre_y_no_guarda(self):
        self.autenticar(self.crear_docente("docente-a"))

        response = self.client.post(
            CURSOS_URL, {"lenguaje": LENGUAJES[0]}, format="json"
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("nombre", response.data)
        self.assertEqual(Curso.objects.count(), 0)

    def test_nombre_solo_con_espacios_devuelve_error_en_nombre_y_no_guarda(self):
        self.autenticar(self.crear_docente("docente-a"))

        response = self.client.post(
            CURSOS_URL, {"nombre": "   ", "lenguaje": LENGUAJES[0]}, format="json"
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("nombre", response.data)
        self.assertEqual(Curso.objects.count(), 0)

    def test_sin_lenguaje_devuelve_error_en_lenguaje_y_no_guarda(self):
        self.autenticar(self.crear_docente("docente-a"))

        response = self.client.post(
            CURSOS_URL, {"nombre": "Algoritmos I"}, format="json"
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("lenguaje", response.data)
        self.assertEqual(Curso.objects.count(), 0)

    def test_curso_valido_queda_asociado_al_docente_que_lo_creo(self):
        docente_a = self.crear_docente("docente-a")
        docente_b = self.crear_docente("docente-b")
        self.autenticar(docente_a)

        response = self.client.post(
            CURSOS_URL,
            {"nombre": "Algoritmos I", "lenguaje": LENGUAJES[0]},
            format="json",
        )

        self.assertEqual(response.status_code, 201)
        curso = Curso.objects.get()
        self.assertEqual(curso.nombre, "Algoritmos I")
        self.assertEqual(curso.lenguaje, LENGUAJES[0])
        self.assertEqual(curso.docente, docente_a)
        self.assertNotEqual(curso.docente, docente_b)

    def test_propietario_en_el_cuerpo_se_ignora_y_manda_el_token(self):
        docente_a = self.crear_docente("docente-a")
        docente_b = self.crear_docente("docente-b")
        self.autenticar(docente_a)
        datos = {
            "nombre": "Algoritmos I",
            "lenguaje": LENGUAJES[0],
            "docente": docente_b.pk,
        }

        response = self.client.post(CURSOS_URL, datos, format="json")

        # docente no es un campo del serializer: DRF lo ignora y el dueño sale del token.
        self.assertEqual(response.status_code, 201)
        self.assertEqual(Curso.objects.get().docente, docente_a)
        self.assertEqual(Curso.objects.filter(docente=docente_b).count(), 0)
