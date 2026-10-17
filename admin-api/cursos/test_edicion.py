# CA-006.3: un docente no puede modificar el curso de otro. CA-006.1: sí puede modificar el suyo.
from django.conf import settings
from django.contrib.auth.models import User
from django.test import TestCase
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.serializers import LENGUAJES

CONTRASENA = "Clave-Falsa-2026"
NOMBRE_ORIGINAL = "Algoritmos I"
NOMBRE_NUEVO = "Algoritmos II"


class EditarCursoAjenoTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    # Copiados de cursos/tests.py: importar su clase haría correr sus pruebas dos veces.
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

    def preparar_curso_de_a(self):
        docente_a = self.crear_docente("docente-a")
        docente_b = self.crear_docente("docente-b")
        curso = Curso.objects.create(
            docente=docente_a, nombre=NOMBRE_ORIGINAL, lenguaje=LENGUAJES[0]
        )
        return docente_a, docente_b, curso

    def cuerpo_valido(self):
        # Cuerpo completo: sirve igual para PUT y para PATCH.
        return {"nombre": NOMBRE_NUEVO, "lenguaje": LENGUAJES[1]}

    def assertCursoSinCambios(self, curso, docente_a):
        curso.refresh_from_db()
        self.assertEqual(curso.nombre, NOMBRE_ORIGINAL)
        self.assertEqual(curso.lenguaje, LENGUAJES[0])
        self.assertEqual(curso.docente, docente_a)

    def test_docente_ajeno_recibe_403_y_el_curso_no_cambia(self):
        for metodo in ["put", "patch"]:
            with self.subTest(metodo=metodo):
                docente_a, docente_b, curso = self.preparar_curso_de_a()
                self.autenticar(docente_b)

                response = getattr(self.client, metodo)(
                    f"/api/cursos/{curso.pk}/", self.cuerpo_valido(), format="json"
                )

                self.assertEqual(response.status_code, 403)
                self.assertCursoSinCambios(curso, docente_a)
                # Se borran para que la siguiente vuelta cree sus propios datos.
                Curso.objects.all().delete()
                User.objects.all().delete()

    def test_el_cuerpo_no_permite_asignarse_el_curso_ajeno(self):
        docente_a, docente_b, curso = self.preparar_curso_de_a()
        self.autenticar(docente_b)
        datos = {**self.cuerpo_valido(), "docente": docente_b.pk}

        response = self.client.patch(f"/api/cursos/{curso.pk}/", datos, format="json")

        self.assertEqual(response.status_code, 403)
        self.assertCursoSinCambios(curso, docente_a)

    def test_el_propietario_si_puede_modificar_su_curso(self):
        # Control: el mismo cuerpo funciona con el dueño, así que el 403 es por no serlo.
        for metodo in ["put", "patch"]:
            with self.subTest(metodo=metodo):
                docente_a, _, curso = self.preparar_curso_de_a()
                self.autenticar(docente_a)

                response = getattr(self.client, metodo)(
                    f"/api/cursos/{curso.pk}/", self.cuerpo_valido(), format="json"
                )

                self.assertEqual(response.status_code, 200)
                curso.refresh_from_db()
                self.assertEqual(curso.nombre, NOMBRE_NUEVO)
                self.assertEqual(curso.lenguaje, LENGUAJES[1])
                self.assertEqual(curso.docente, docente_a)
                Curso.objects.all().delete()
                User.objects.all().delete()
