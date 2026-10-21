# CA-015.4: el estudiante completa el cambio de contraseña. CA-015.3: el enlace trae lo necesario.
# CA-014.2: la misma cuenta, ya recuperada, entra también por la app móvil.
import re
from urllib.parse import parse_qs, urlparse

from django.conf import settings
from django.contrib.auth.models import Group, User
from django.core import mail
from django.test import TestCase, override_settings
from django.utils.encoding import force_bytes
from django.utils.http import urlsafe_base64_encode
from rest_framework.test import APIClient

from cuentas.models import Docente

RECUPERAR_URL = "/api/auth/recuperar/"
NUEVA_CONTRASENA_URL = "/api/auth/nueva-contrasena/"
LOGIN_URL = "/api/auth/login/"
# Inventadas y solo para estas pruebas.
CONTRASENA_VIEJA = "Clave-Vieja-HU23-2026"
CONTRASENA_NUEVA = "Clave-Nueva-HU23-2026"


# URLs distintas y falsas: así se ve a cuál de las dos webs apunta el enlace.
# Django usa el backend de correo en memoria durante las pruebas (mail.outbox).
@override_settings(
    USER_WEB_URL="http://user-web.prueba", ADMIN_WEB_URL="http://admin-web.prueba"
)
class RecuperacionContrasenaEstudianteTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    def crear_estudiante(self, usuario="estudiante"):
        # Igual que el registro de HU21: cuenta activa con el grupo "estudiante".
        email = f"{usuario}@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(
            username=email, email=email, password=CONTRASENA_VIEJA, is_active=True
        )
        grupo, _ = Group.objects.get_or_create(name="estudiante")
        user.groups.add(grupo)
        return user

    def pedir_enlace(self, email):
        return self.client.post(RECUPERAR_URL, {"email": email}, format="json")

    def enlace_del_correo(self):
        # Se lee del correo enviado: el token no se inventa ni se genera aparte.
        self.assertEqual(len(mail.outbox), 1)
        return re.search(r"\S+/nueva-contrasena\?\S+", mail.outbox[0].body).group(0)

    def uid_y_token(self, enlace):
        parametros = parse_qs(urlparse(enlace).query)
        return parametros["uid"][0], parametros["token"][0]

    def cambiar(self, uid, token, password=CONTRASENA_NUEVA):
        return self.client.post(
            NUEVA_CONTRASENA_URL,
            {"uid": uid, "token": token, "password": password},
            format="json",
        )

    def login(self, email, password, cliente):
        return self.client.post(
            LOGIN_URL,
            {"email": email, "password": password, "cliente": cliente},
            format="json",
        )

    def recuperar(self, user):
        # Pedir el enlace y cambiar la contraseña con el uid y token del correo.
        self.pedir_enlace(user.email)
        uid, token = self.uid_y_token(self.enlace_del_correo())
        self.assertEqual(self.cambiar(uid, token).status_code, 200)
        return uid, token

    def test_flujo_completo_y_la_cuenta_entra_por_la_web_y_por_el_movil(self):
        user = self.crear_estudiante()

        pedido = self.pedir_enlace(user.email)
        self.assertEqual(pedido.status_code, 200)
        self.assertEqual(mail.outbox[0].to, [user.email])
        uid, token = self.uid_y_token(self.enlace_del_correo())
        cambio = self.cambiar(uid, token)
        self.assertEqual(cambio.status_code, 200)

        for cliente in ["user-web", "mobile"]:
            with self.subTest(cliente=cliente):
                response = self.login(user.email, CONTRASENA_NUEVA, cliente)

                self.assertEqual(response.status_code, 200)
                self.assertIn("access", response.data)
                self.assertIn("refresh", response.data)

    def test_la_contrasena_anterior_deja_de_funcionar(self):
        user = self.crear_estudiante()
        self.recuperar(user)

        response = self.login(user.email, CONTRASENA_VIEJA, "user-web")

        self.assertEqual(response.status_code, 401)
        self.assertNotIn("access", response.data)

    def test_el_enlace_es_de_un_solo_uso(self):
        user = self.crear_estudiante()
        uid, token = self.recuperar(user)

        response = self.cambiar(uid, token, password="Otra-Clave-HU23-2026")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["detail"], "El enlace no es válido o ya venció.")
        user.refresh_from_db()
        self.assertTrue(user.check_password(CONTRASENA_NUEVA))

    def test_token_alterado_o_uid_de_otra_cuenta_se_rechazan(self):
        user = self.crear_estudiante()
        otro = self.crear_estudiante("otro-estudiante")
        self.pedir_enlace(user.email)
        uid, token = self.uid_y_token(self.enlace_del_correo())
        token_alterado = token[:-1] + ("a" if token[-1] != "a" else "b")
        # Mismo formato que el enlace (id en base64), sin pedirle otro correo a la otra cuenta.
        uid_de_otro = urlsafe_base64_encode(force_bytes(otro.pk))

        for nombre, (u, t) in {
            "token alterado": (uid, token_alterado),
            "uid de otra cuenta": (uid_de_otro, token),
        }.items():
            with self.subTest(caso=nombre):
                response = self.cambiar(u, t)

                self.assertEqual(response.status_code, 400)
        for cuenta in [user, otro]:
            cuenta.refresh_from_db()
            self.assertTrue(cuenta.check_password(CONTRASENA_VIEJA))

    def test_contrasena_nueva_invalida_se_rechaza_y_la_anterior_sigue(self):
        user = self.crear_estudiante()
        self.pedir_enlace(user.email)
        uid, token = self.uid_y_token(self.enlace_del_correo())

        # Corta, común y solo números: la rechazan los validadores de Django.
        response = self.cambiar(uid, token, password="123456")

        self.assertEqual(response.status_code, 400)
        self.assertIn("password", response.data)
        self.assertEqual(
            self.login(user.email, CONTRASENA_VIEJA, "mobile").status_code, 200
        )

    def test_correo_no_registrado_recibe_la_misma_respuesta_sin_enviar_correo(self):
        user = self.crear_estudiante()
        registrado = self.pedir_enlace(user.email)
        mail.outbox.clear()

        no_registrado = self.pedir_enlace(f"nadie@{settings.DOMINIO_INSTITUCIONAL}")

        self.assertEqual(no_registrado.status_code, registrado.status_code)
        self.assertEqual(no_registrado.data, registrado.data)
        self.assertEqual(len(mail.outbox), 0)

    def test_el_enlace_del_estudiante_apunta_a_user_web(self):
        user = self.crear_estudiante()
        self.pedir_enlace(user.email)

        enlace = self.enlace_del_correo()

        self.assertTrue(enlace.startswith(f"{settings.USER_WEB_URL}/nueva-contrasena?"))
        self.assertNotIn(settings.ADMIN_WEB_URL, mail.outbox[0].body)

    def test_cuenta_inactiva_no_recibe_enlace_ni_puede_entrar(self):
        # RecuperarView solo busca cuentas activas: un docente pendiente de activación no recupera.
        email = f"docente-inactivo@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(
            username=email, email=email, password=CONTRASENA_VIEJA, is_active=False
        )
        Docente.objects.create(
            user=user,
            nombre="Docente inactivo",
            email=email,
            institucion=settings.DOMINIO_INSTITUCIONAL,
        )

        response = self.pedir_enlace(email)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(mail.outbox), 0)
        self.assertEqual(
            self.login(email, CONTRASENA_VIEJA, "admin-web").status_code, 403
        )
