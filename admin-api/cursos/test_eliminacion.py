# CA-008.1: un curso vacío se elimina. CA-008.2: un curso con problemas no se elimina.
# CA-008.3: un curso con estudiantes inscritos no se elimina (HU28 T5, los inscritos los informa user-api).
# CA-008.4: el bloqueo explica la causa.
from unittest.mock import patch

from django.conf import settings
from django.contrib.auth.models import User
from django.test import TestCase
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.tokens import crear_tokens
from cursos.inscritos import UserApiNoDisponible
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from problemas.models import Problema
from problemas.serializers import DIFICULTADES

CONTRASENA = "Clave-Falsa-2026"


class EliminarCursoTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        # user-api no existe en las pruebas: por defecto el curso no tiene inscritos. Cada prueba
        # de inscritos cambia este valor.
        parche = patch("cursos.views.inscritos_de", return_value=[])
        self.inscritos_de = parche.start()
        self.addCleanup(parche.stop)

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

    # --- HU28-T5: estudiantes inscritos ---
    def test_curso_con_inscritos_no_se_elimina(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)
        self.inscritos_de.return_value = [{"email": "ana@example.com", "fecha": "2026-10-20T10:00:00Z"}]

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 409)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())
        self.inscritos_de.assert_called_once_with(curso.pk)

    def test_el_bloqueo_explica_que_el_curso_tiene_inscritos(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)
        self.inscritos_de.return_value = [{"email": "ana@example.com", "fecha": "2026-10-20T10:00:00Z"}]

        mensaje = self.borrar(curso).data["detail"].lower()

        self.assertIn("eliminar", mensaje)
        self.assertIn("inscritos", mensaje)

    def test_curso_sin_inscritos_ni_problemas_se_elimina_despues_de_preguntar(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 204)
        self.assertFalse(Curso.objects.filter(pk=curso.pk).exists())
        self.inscritos_de.assert_called_once_with(curso.pk)

    def test_si_user_api_no_responde_el_curso_no_se_elimina(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.autenticar(docente)
        self.inscritos_de.side_effect = UserApiNoDisponible("sin conexión")

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 503)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())

    def test_con_problemas_responde_sin_consultar_a_user_api(self):
        docente = self.crear_docente("docente-a")
        curso = self.crear_curso(docente)
        self.crear_problema(curso)
        self.autenticar(docente)

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 409)
        self.assertIn("problemas", response.data["detail"].lower())
        self.inscritos_de.assert_not_called()

    def test_un_curso_ajeno_da_403_sin_consultar_a_user_api(self):
        dueno = self.crear_docente("docente-a")
        curso = self.crear_curso(dueno)
        self.autenticar(self.crear_docente("docente-b"))

        response = self.borrar(curso)

        self.assertEqual(response.status_code, 403)
        self.assertTrue(Curso.objects.filter(pk=curso.pk).exists())
        self.inscritos_de.assert_not_called()

    def test_un_curso_inexistente_da_404(self):
        self.autenticar(self.crear_docente("docente-a"))

        self.assertEqual(self.client.delete("/api/cursos/999999/").status_code, 404)
