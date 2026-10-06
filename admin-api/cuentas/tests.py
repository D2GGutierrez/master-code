# CA-001.1: registro válido y cuenta inactiva. CA-001.2: dominio no permitido.
# CA-001.3: correo repetido. Además, login rechazado mientras la cuenta está inactiva.
from datetime import datetime, timedelta
from unittest.mock import patch

from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.auth.tokens import default_token_generator
from django.test import TestCase, override_settings
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.recuperacion import generar_uid_token

REGISTRO_URL = "/api/auth/registro/docente/"
REGISTRO_ESTUDIANTE_URL = "/api/auth/registro/estudiante/"
LOGIN_URL = "/api/auth/login/"
NUEVA_CONTRASENA_URL = "/api/auth/nueva-contrasena/"
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


# CA-003.3: el enlace vence. CA-003.4: el enlace es de un solo uso.
class RecuperacionContrasenaTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        email = f"docente@{settings.DOMINIO_INSTITUCIONAL}"
        self.user = User.objects.create_user(
            username=email, email=email, password=CONTRASENA
        )
        # El login solo emite tokens a usuarios con perfil Docente.
        Docente.objects.create(
            user=self.user,
            nombre="Docente de Prueba",
            email=email,
            institucion=settings.DOMINIO_INSTITUCIONAL,
        )
        uid, token = generar_uid_token(self.user)
        self.datos = {"uid": uid, "token": token, "password": "Nueva-Clave-2026"}

    def test_enlace_valido_cambia_la_contrasena_y_permite_iniciar_sesion(self):
        response = self.client.post(NUEVA_CONTRASENA_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 200)
        login = self.client.post(
            LOGIN_URL,
            {
                "email": self.user.email,
                "password": "Nueva-Clave-2026",
                "cliente": "admin-web",
            },
            format="json",
        )
        self.assertEqual(login.status_code, 200)
        self.assertIn("access", login.data)

    def test_enlace_vencido_no_cambia_la_contrasena(self):
        # Se adelanta el reloj del generador un segundo más allá del tiempo de vida.
        despues_de_vencer = datetime.now() + timedelta(
            seconds=settings.PASSWORD_RESET_TIMEOUT + 1
        )
        with patch.object(
            default_token_generator, "_now", return_value=despues_de_vencer
        ):
            response = self.client.post(NUEVA_CONTRASENA_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["detail"], "El enlace no es válido o ya venció.")
        self.user.refresh_from_db()
        self.assertTrue(self.user.check_password(CONTRASENA))

    def test_enlace_usado_no_permite_un_segundo_cambio(self):
        self.client.post(NUEVA_CONTRASENA_URL, self.datos, format="json")
        self.datos["password"] = "Otra-Clave-2026"

        response = self.client.post(NUEVA_CONTRASENA_URL, self.datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["detail"], "El enlace no es válido o ya venció.")
        self.user.refresh_from_db()
        self.assertTrue(self.user.check_password("Nueva-Clave-2026"))
        self.assertFalse(self.user.check_password("Otra-Clave-2026"))
