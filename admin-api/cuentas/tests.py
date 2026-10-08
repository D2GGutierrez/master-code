# CA-001.1: registro válido y cuenta inactiva. CA-001.2: dominio no permitido.
# CA-001.3: correo repetido. Además, login rechazado mientras la cuenta está inactiva.
import io
import logging
from contextlib import redirect_stderr, redirect_stdout
from datetime import datetime, timedelta
from unittest.mock import patch

from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.auth.tokens import default_token_generator
from django.test import TestCase, override_settings
from rest_framework.test import APIClient
from rest_framework_simplejwt.utils import aware_utcnow

from cuentas.models import Docente
from cuentas.recuperacion import generar_uid_token
from cuentas.tokens import crear_tokens

REGISTRO_URL = "/api/auth/registro/docente/"
REGISTRO_ESTUDIANTE_URL = "/api/auth/registro/estudiante/"
LOGIN_URL = "/api/auth/login/"
NUEVA_CONTRASENA_URL = "/api/auth/nueva-contrasena/"
REFRESH_URL = "/api/auth/refresh/"
# Endpoint protegido (IsAuthenticated + EsDocente) para probar el access token.
CURSOS_URL = "/api/cursos/"
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


# Guarda cada registro de logging que llega, con su texto ya formateado (incluye tracebacks).
class CapturaDeLogs(logging.Handler):
    def __init__(self):
        super().__init__(level=logging.DEBUG)
        self.textos = []

    def emit(self, record):
        self.textos.append(self.format(record))
        self.textos.append(repr(record.msg))
        self.textos.append(repr(record.args))


# CA-004.4: ninguna contraseña en los logs, en la consola ni en las respuestas de error.
class SinContrasenasEnLogsTests(TestCase):
    # Única y fácil de buscar; no es una contraseña real.
    CONTRASENA_TRAZABLE = "Trazable-HU04-7f3Kq9-NoDebeAparecerEnLogs"

    def setUp(self):
        self.client = APIClient()

    def enviar_capturando(self, url, datos):
        # Handler propio en el logger raíz y no assertLogs: la prueba sigue valiendo aunque no haya logs.
        # Se baja el nivel a DEBUG para ver también los registros que hoy se descartan.
        captura = CapturaDeLogs()
        raiz, django_logger = logging.getLogger(), logging.getLogger("django")
        niveles = raiz.level, django_logger.level
        raiz.addHandler(captura)
        raiz.setLevel(logging.DEBUG)
        django_logger.setLevel(logging.DEBUG)
        salida = io.StringIO()
        try:
            with redirect_stdout(salida), redirect_stderr(salida):
                response = self.client.post(url, datos, format="json")
        finally:
            raiz.removeHandler(captura)
            raiz.setLevel(niveles[0])
            django_logger.setLevel(niveles[1])
        return response, "\n".join(captura.textos), salida.getvalue()

    def assertSinContrasena(self, response, logs, salida):
        self.assertNotIn(self.CONTRASENA_TRAZABLE, logs)
        self.assertNotIn(self.CONTRASENA_TRAZABLE, salida)
        self.assertNotIn(self.CONTRASENA_TRAZABLE, response.content.decode())

    def crear_docente_activo(self):
        email = f"docente@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(username=email, email=email, password=CONTRASENA)
        Docente.objects.create(
            user=user, nombre="Docente de Prueba", email=email, institucion=settings.DOMINIO_INSTITUCIONAL
        )
        return user

    def datos_registro(self, email):
        return {"nombre": "Persona de Prueba", "email": email, "password": self.CONTRASENA_TRAZABLE}

    def test_registro_docente_valido(self):
        datos = self.datos_registro(f"docente@{settings.DOMINIO_INSTITUCIONAL}")

        response, logs, salida = self.enviar_capturando(REGISTRO_URL, datos)

        self.assertEqual(response.status_code, 201)
        self.assertSinContrasena(response, logs, salida)

    def test_registro_docente_con_otro_dominio(self):
        datos = self.datos_registro("docente@otro-dominio.com")

        response, logs, salida = self.enviar_capturando(REGISTRO_URL, datos)

        self.assertEqual(response.status_code, 400)
        self.assertSinContrasena(response, logs, salida)

    def test_registro_docente_con_correo_repetido(self):
        self.crear_docente_activo()
        datos = self.datos_registro(f"docente@{settings.DOMINIO_INSTITUCIONAL}")

        response, logs, salida = self.enviar_capturando(REGISTRO_URL, datos)

        self.assertEqual(response.status_code, 400)
        self.assertSinContrasena(response, logs, salida)

    def test_registro_estudiante_valido(self):
        datos = self.datos_registro(f"estudiante@{settings.DOMINIO_INSTITUCIONAL}")

        response, logs, salida = self.enviar_capturando(REGISTRO_ESTUDIANTE_URL, datos)

        self.assertEqual(response.status_code, 201)
        self.assertSinContrasena(response, logs, salida)

    def test_registro_estudiante_con_otro_dominio(self):
        datos = self.datos_registro("estudiante@otro-dominio.com")

        response, logs, salida = self.enviar_capturando(REGISTRO_ESTUDIANTE_URL, datos)

        self.assertEqual(response.status_code, 400)
        self.assertSinContrasena(response, logs, salida)

    def test_login_correcto(self):
        user = self.crear_docente_activo()
        user.set_password(self.CONTRASENA_TRAZABLE)
        user.save()
        datos = {"email": user.email, "password": self.CONTRASENA_TRAZABLE, "cliente": "admin-web"}

        response, logs, salida = self.enviar_capturando(LOGIN_URL, datos)

        self.assertEqual(response.status_code, 200)
        self.assertSinContrasena(response, logs, salida)

    def test_login_con_credenciales_incorrectas(self):
        user = self.crear_docente_activo()
        datos = {"email": user.email, "password": self.CONTRASENA_TRAZABLE, "cliente": "admin-web"}

        response, logs, salida = self.enviar_capturando(LOGIN_URL, datos)

        self.assertEqual(response.status_code, 401)
        self.assertSinContrasena(response, logs, salida)

    def test_nueva_contrasena_con_enlace_valido(self):
        uid, token = generar_uid_token(self.crear_docente_activo())
        datos = {"uid": uid, "token": token, "password": self.CONTRASENA_TRAZABLE}

        response, logs, salida = self.enviar_capturando(NUEVA_CONTRASENA_URL, datos)

        self.assertEqual(response.status_code, 200)
        self.assertSinContrasena(response, logs, salida)

    def test_nueva_contrasena_con_token_invalido(self):
        uid, _ = generar_uid_token(self.crear_docente_activo())
        datos = {"uid": uid, "token": "token-inventado", "password": self.CONTRASENA_TRAZABLE}

        response, logs, salida = self.enviar_capturando(NUEVA_CONTRASENA_URL, datos)

        self.assertEqual(response.status_code, 400)
        self.assertSinContrasena(response, logs, salida)

    def test_nueva_contrasena_debil_no_se_repite_en_el_error(self):
        # Solo números: la rechaza NumericPasswordValidator y la respuesta trae sus mensajes.
        self.CONTRASENA_TRAZABLE = "730418562930174856"
        uid, token = generar_uid_token(self.crear_docente_activo())
        datos = {"uid": uid, "token": token, "password": self.CONTRASENA_TRAZABLE}

        response, logs, salida = self.enviar_capturando(NUEVA_CONTRASENA_URL, datos)

        self.assertEqual(response.status_code, 400)
        self.assertIn("password", response.data)
        self.assertSinContrasena(response, logs, salida)


# CA-002.2: la sesión se renueva con el refresh. CA-002.3: las peticiones usan el access vigente.
class ExpiracionYRenovacionTests(TestCase):
    ACCESS = settings.SIMPLE_JWT["ACCESS_TOKEN_LIFETIME"]
    REFRESH = settings.SIMPLE_JWT["REFRESH_TOKEN_LIFETIME"]
    UN_MINUTO = timedelta(minutes=1)

    def setUp(self):
        self.client = APIClient()

    def crear_docente_activo(self):
        email = f"docente@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(
            username=email, email=email, password=CONTRASENA, is_active=True
        )
        Docente.objects.create(
            user=user,
            nombre="Docente de Prueba",
            email=email,
            institucion=settings.DOMINIO_INSTITUCIONAL,
        )
        return user

    def emitir_tokens(self, user, hace=timedelta(0)):
        # Se atrasa el reloj de simplejwt solo al emitir: el token nace con iat y exp en el
        # pasado y después se valida con el reloj real, como si hubiera pasado ese tiempo.
        emitido = aware_utcnow() - hace
        with patch(
            "rest_framework_simplejwt.tokens.aware_utcnow", return_value=emitido
        ):
            return crear_tokens(user, "admin-web")

    def pedir_cursos(self, access):
        return self.client.get(CURSOS_URL, HTTP_AUTHORIZATION=f"Bearer {access}")

    def test_access_vigente_permite_entrar(self):
        _, access = self.emitir_tokens(self.crear_docente_activo())

        response = self.pedir_cursos(access)

        self.assertEqual(response.status_code, 200)

    def test_access_vencido_se_rechaza_con_401(self):
        _, access = self.emitir_tokens(
            self.crear_docente_activo(), hace=self.ACCESS + self.UN_MINUTO
        )

        response = self.pedir_cursos(access)

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.data["code"], "token_not_valid")

    def test_access_vencido_se_renueva_con_el_refresh_vigente(self):
        # Pasó lo justo para vencer el access, pero el refresh (más largo) sigue vigente.
        self.assertGreater(self.REFRESH, self.ACCESS + self.UN_MINUTO)
        refresh, access = self.emitir_tokens(
            self.crear_docente_activo(), hace=self.ACCESS + self.UN_MINUTO
        )
        self.assertEqual(self.pedir_cursos(access).status_code, 401)

        renovacion = self.client.post(REFRESH_URL, {"refresh": refresh}, format="json")

        self.assertEqual(renovacion.status_code, 200)
        self.assertNotEqual(renovacion.data["access"], access)
        self.assertEqual(self.pedir_cursos(renovacion.data["access"]).status_code, 200)
        # Rotación: llega un refresh nuevo y el anterior ya no sirve.
        self.assertNotEqual(renovacion.data["refresh"], refresh)
        reuso = self.client.post(REFRESH_URL, {"refresh": refresh}, format="json")
        self.assertEqual(reuso.status_code, 401)

    def test_refresh_vencido_no_renueva_ni_entrega_tokens(self):
        refresh, _ = self.emitir_tokens(
            self.crear_docente_activo(), hace=self.REFRESH + self.UN_MINUTO
        )

        response = self.client.post(REFRESH_URL, {"refresh": refresh}, format="json")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.data["detail"], "La sesión no es válida o ya venció.")
        self.assertNotIn("access", response.data)
        self.assertNotIn("refresh", response.data)
