# CA-008.1: un curso vacío se elimina. CA-008.2: un curso con problemas no se elimina.
# CA-008.4: el bloqueo explica la causa. CA-008.3 (estudiantes inscritos) es del Sprint 2.
from django.conf import settings
from django.contrib.auth.models import User
from django.test import TestCase
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from problemas.models import Problema
from problemas.serializers import DIFICULTADES

CONTRASENA = "Clave-Falsa-2026"


class EliminarCursoTests(TestCase):
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

    def crear_curso(self, docente):
        return Curso.objects.create(
            docente=docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0]
        )

    def crear_problema(self, curso):
        # Valores tomados del código, como en problemas/tests.py.
        return Problema.objects.create(
            curso=curso,
            titulo="Suma de dos números",
            enunciado="Lee dos enteros e imprime su suma.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.WEB,
            tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
        )

    def borrar(self, curso):
        return self.client.delete(f"/api/cursos/{curso.pk}/")

    def test_curso_vacio_se_elimina(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 204)
        self.assertFalse(Curso.objects.filter(pk=curso.pk).exists())

    def test_curso_con_problemas_no_se_elimina(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        problema = self.crear_problema(curso)
        self.autenticar(docente)

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 409)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())
        self.assertTrue(Problema.objects.filter(pk=problema.pk).exists())

    def test_el_bloqueo_explica_que_el_curso_tiene_problemas(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.crear_problema(curso)
        self.autenticar(docente)

        response = self.borrar(curso)

        mensaje = response.data["detail"]
        self.assertTrue(mensaje.strip())
        # Palabras clave de la causa, sin depender de la redacción completa.
        self.assertIn("eliminar", mensaje.lower())
        self.assertIn("problemas", mensaje.lower())

    def test_sin_sesion_no_se_puede_eliminar(self):
        curso = self.crear_curso(self.crear_docente("docente-a"))

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 401)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())

    def test_sin_su_ultimo_problema_el_curso_si_se_elimina(self):
        # Control: el mismo curso se bloquea con un problema y se borra cuando ya no tiene.
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        problema = self.crear_problema(curso)
        self.autenticar(docente)
        self.assertEqual(self.borrar(curso).status_code, 409)

        problema.delete()
        response = self.borrar(curso)

        self.assertEqual(response.status_code, 204)
        self.assertFalse(Curso.objects.filter(pk=curso.pk).exists())
