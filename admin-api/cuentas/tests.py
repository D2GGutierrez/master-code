# CA-001.1: registro válido y cuenta inactiva. CA-001.2: dominio no permitido.
# CA-001.3: correo repetido. Además, login rechazado mientras la cuenta está inactiva.
from django.conf import settings
from django.contrib.auth.models import User
from django.test import TestCase, override_settings
from rest_framework.test import APIClient

from cuentas.models import Docente

REGISTRO_URL = "/api/auth/registro/docente/"
REGISTRO_ESTUDIANTE_URL = "/api/auth/registro/estudiante/"
LOGIN_URL = "/api/auth/login/"
CONTRASENA = "Clave-Falsa-2026"


# Dominio fijo para que la prueba no dependa del .env de cada PC ni del CI.
@override_settings(DOMINIO_INSTITUCIONAL="ejemplo.edu.pe")
class RegistroDocenteTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.datos = {
            "nombre": "Docente de Prueba",
            "email": "docente@ejemplo.edu.pe",
            "password": CONTRASENA,
        }

    def test_registro_valido_crea_cuenta_inactiva_con_docente(self):
        response = self.client.post(REGISTRO_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 201)
        user = User.objects.get(username="docente@ejemplo.edu.pe")
        self.assertFalse(user.is_active)
        docente = Docente.objects.get(user=user)
        self.assertEqual(docente.nombre, "Docente de Prueba")
        self.assertEqual(docente.email, "docente@ejemplo.edu.pe")
        self.assertEqual(docente.institucion, "ejemplo.edu.pe")
        self.assertEqual(User.objects.count(), 1)
        self.assertEqual(Docente.objects.count(), 1)

    def test_registro_con_otro_dominio_se_rechaza_sin_crear_nada(self):
        self.datos["email"] = "docente@otro-dominio.com"

        response = self.client.post(REGISTRO_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertIn("email", response.data)
        self.assertEqual(User.objects.count(), 0)
        self.assertEqual(Docente.objects.count(), 0)

    def test_registro_con_correo_repetido_se_rechaza_sin_duplicar(self):
        self.client.post(REGISTRO_URL, self.datos, format="json")

        response = self.client.post(REGISTRO_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertIn("email", response.data)
        self.assertEqual(User.objects.count(), 1)
        self.assertEqual(Docente.objects.count(), 1)

    def test_login_con_cuenta_inactiva_falla_con_mensaje_de_activacion(self):
        self.client.post(REGISTRO_URL, self.datos, format="json")

        response = self.client.post(
            LOGIN_URL,
            {"email": self.datos["email"], "password": CONTRASENA, "cliente": "admin-web"},
            format="json",
        )

        self.assertEqual(response.status_code, 403)
        self.assertEqual(
            response.data["detail"], "Tu cuenta está pendiente de activación por el administrador."
        )
        self.assertNotIn("access", response.data)
        self.assertNotIn("refresh", response.data)


# HU21: el estudiante queda activo desde el registro y su rol es el grupo "estudiante".
class RegistroEstudianteTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        # Se lee en setUp para usar el mismo dominio que valida el backend.
        self.email = f"estudiante@{settings.DOMINIO_INSTITUCIONAL}"
        self.datos = {
            "nombre": "Estudiante de Prueba",
            "email": self.email,
            "password": CONTRASENA,
        }

    def test_registro_valido_crea_cuenta_activa_con_rol_estudiante(self):
        response = self.client.post(REGISTRO_ESTUDIANTE_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 201)
        user = User.objects.get(username=self.email)
        self.assertTrue(user.is_active)
        self.assertEqual(user.first_name, "Estudiante de Prueba")
        self.assertTrue(user.groups.filter(name="estudiante").exists())
        # El perfil del estudiante vive en user-api: aquí no se crea un Docente.
        self.assertEqual(Docente.objects.count(), 0)
        self.assertEqual(User.objects.count(), 1)

    def test_registro_con_correo_repetido_se_rechaza_sin_duplicar(self):
        self.client.post(REGISTRO_ESTUDIANTE_URL, self.datos, format="json")

        response = self.client.post(REGISTRO_ESTUDIANTE_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertIn("email", response.data)
        self.assertEqual(User.objects.count(), 1)
