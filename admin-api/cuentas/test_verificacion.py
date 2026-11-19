# Verificación del correo con un código de 6 dígitos que llega al correo institucional.
import re
from datetime import timedelta
from unittest.mock import patch

from django.contrib.auth.models import User
from django.core import mail
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework.test import APIClient

from cuentas.models import Docente, VerificacionCorreo

REGISTRO_ESTUDIANTE_URL = "/api/auth/registro/estudiante/"
REGISTRO_DOCENTE_URL = "/api/auth/registro/docente/"
VERIFICAR_URL = "/api/auth/verificar-correo/"
REENVIAR_URL = "/api/auth/reenviar-codigo/"
LOGIN_URL = "/api/auth/login/"
CONTRASENA = "Clave-Falsa-2026"
CORREO = "ana@ejemplo.edu.pe"


def codigo_del_ultimo_correo():
    return re.search(r"\b(\d{6})\b", mail.outbox[-1].body).group(1)


@override_settings(DOMINIO_INSTITUCIONAL="ejemplo.edu.pe", DOCENTES_AUTORIZADOS={"profe@ejemplo.edu.pe"})
class VerificacionCorreoTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    def registrar_estudiante(self, email=CORREO):
        return self.client.post(
            REGISTRO_ESTUDIANTE_URL, {"nombre": "Ana", "email": email, "password": CONTRASENA}, format="json"
        )

    def verificar(self, codigo, email=CORREO):
        return self.client.post(VERIFICAR_URL, {"email": email, "codigo": codigo}, format="json")

    def entrar(self, email=CORREO, cliente="user-web"):
        return self.client.post(
            LOGIN_URL, {"email": email, "password": CONTRASENA, "cliente": cliente}, format="json"
        )

    # --- Registro ---

    def test_registro_envia_un_codigo_de_6_digitos_al_correo(self):
        response = self.registrar_estudiante()

        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data["codigo_enviado"])
        self.assertEqual(response.data["email"], CORREO)
        self.assertEqual(len(mail.outbox), 1)
        self.assertEqual(mail.outbox[0].to, [CORREO])
        self.assertRegex(mail.outbox[0].body, r"\b\d{6}\b")

    def test_el_codigo_no_se_guarda_en_claro(self):
        self.registrar_estudiante()

        verificacion = VerificacionCorreo.objects.get(user__username=CORREO)

        self.assertNotIn(codigo_del_ultimo_correo(), verificacion.codigo_hash)
        self.assertEqual(len(verificacion.codigo_hash), 64)

    def test_si_el_correo_no_sale_la_cuenta_se_crea_y_se_avisa(self):
        with patch("cuentas.verificacion.send_mail", side_effect=OSError("smtp caído")):
            response = self.registrar_estudiante()

        self.assertEqual(response.status_code, 201)
        self.assertFalse(response.data["codigo_enviado"])
        self.assertTrue(User.objects.filter(username=CORREO).exists())

    # --- Sin verificar no se entra ---

    def test_sin_verificar_el_login_pide_el_codigo(self):
        self.registrar_estudiante()

        response = self.entrar()

        self.assertEqual(response.status_code, 403)
        self.assertTrue(response.data["verificar_correo"])
        self.assertNotIn("access", response.data)

    def test_con_contrasena_incorrecta_no_se_revela_si_falta_verificar(self):
        self.registrar_estudiante()

        response = self.client.post(
            LOGIN_URL, {"email": CORREO, "password": "otra-clave", "cliente": "user-web"}, format="json"
        )

        self.assertEqual(response.status_code, 401)

    # --- Verificar ---

    def test_con_el_codigo_correcto_el_estudiante_ya_puede_entrar(self):
        self.registrar_estudiante()

        response = self.verificar(codigo_del_ultimo_correo())

        self.assertEqual(response.status_code, 200)
        self.assertIn("Ya puedes iniciar sesión", response.data["detail"])
        self.assertEqual(self.entrar().status_code, 200)

    def test_el_codigo_sirve_una_sola_vez(self):
        self.registrar_estudiante()
        codigo = codigo_del_ultimo_correo()
        self.verificar(codigo)

        self.assertEqual(self.verificar(codigo).status_code, 400)

    def test_codigo_incorrecto_se_rechaza_y_la_cuenta_sigue_sin_entrar(self):
        self.registrar_estudiante()

        response = self.verificar("000000" if codigo_del_ultimo_correo() != "000000" else "111111")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["detail"], "El código no es correcto.")
        self.assertEqual(self.entrar().status_code, 403)

    def test_tras_5_intentos_fallidos_ni_el_codigo_correcto_sirve(self):
        self.registrar_estudiante()
        correcto = codigo_del_ultimo_correo()
        incorrecto = "000000" if correcto != "000000" else "111111"
        for _ in range(5):
            self.verificar(incorrecto)

        response = self.verificar(correcto)

        self.assertEqual(response.status_code, 400)
        self.assertIn("Demasiados intentos", response.data["detail"])
        self.assertEqual(self.entrar().status_code, 403)

    def test_un_codigo_vencido_no_sirve(self):
        self.registrar_estudiante()
        VerificacionCorreo.objects.update(vence=timezone.now() - timedelta(seconds=1))

        response = self.verificar(codigo_del_ultimo_correo())

        self.assertEqual(response.status_code, 400)
        self.assertIn("venció", response.data["detail"])

    def test_correo_que_no_existe_recibe_la_misma_respuesta_generica(self):
        response = self.verificar("123456", email="nadie@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 400)
        self.assertIn("no es válido", response.data["detail"])

    # --- Reenviar ---

    def test_reenviar_manda_un_codigo_nuevo_y_el_anterior_deja_de_servir(self):
        self.registrar_estudiante()
        anterior = codigo_del_ultimo_correo()
        VerificacionCorreo.objects.update(enviado=timezone.now() - timedelta(seconds=61))

        response = self.client.post(REENVIAR_URL, {"email": CORREO}, format="json")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(mail.outbox), 2)
        nuevo = codigo_del_ultimo_correo()
        if nuevo != anterior:
            self.assertEqual(self.verificar(anterior).status_code, 400)
        self.assertEqual(self.verificar(nuevo).status_code, 200)

    def test_reenviar_antes_de_un_minuto_se_rechaza(self):
        self.registrar_estudiante()

        response = self.client.post(REENVIAR_URL, {"email": CORREO}, format="json")

        self.assertEqual(response.status_code, 429)
        self.assertEqual(len(mail.outbox), 1)

    def test_reenviar_a_un_correo_desconocido_responde_igual_y_no_envia_nada(self):
        response = self.client.post(REENVIAR_URL, {"email": "nadie@ejemplo.edu.pe"}, format="json")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(mail.outbox), 0)

    # --- Correos de otras personas ---

    def test_registrar_el_correo_de_otra_persona_no_la_deja_sin_poder_registrarse(self):
        # Alguien escribe el correo de Ana (que no es suyo) y no puede verificarlo.
        self.registrar_estudiante()
        # Ana se registra después con el mismo correo: reemplaza la cuenta sin verificar.
        response = self.registrar_estudiante()

        self.assertEqual(response.status_code, 201)
        self.assertEqual(User.objects.filter(username=CORREO).count(), 1)
        self.assertEqual(self.verificar(codigo_del_ultimo_correo()).status_code, 200)

    def test_un_correo_ya_verificado_no_se_puede_registrar_otra_vez(self):
        self.registrar_estudiante()
        self.verificar(codigo_del_ultimo_correo())

        response = self.registrar_estudiante()

        self.assertEqual(response.status_code, 400)
        self.assertIn("email", response.data)

    # --- Docentes ---

    def test_docente_autorizado_queda_activo_al_verificar(self):
        self.client.post(
            REGISTRO_DOCENTE_URL, {"nombre": "Profe", "email": "profe@ejemplo.edu.pe", "password": CONTRASENA}, format="json"
        )

        response = self.verificar(codigo_del_ultimo_correo(), email="profe@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.entrar("profe@ejemplo.edu.pe", "admin-web").status_code, 200)

    def test_docente_que_no_esta_en_la_lista_sigue_pendiente_del_product_owner(self):
        self.client.post(
            REGISTRO_DOCENTE_URL, {"nombre": "Otro", "email": "otro@ejemplo.edu.pe", "password": CONTRASENA}, format="json"
        )

        response = self.verificar(codigo_del_ultimo_correo(), email="otro@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 200)
        self.assertIn("Product Owner", response.data["detail"])
        self.assertFalse(Docente.objects.get(email="otro@ejemplo.edu.pe").user.is_active)
        self.assertEqual(self.entrar("otro@ejemplo.edu.pe", "admin-web").status_code, 403)

    # --- Cuentas anteriores y Google ---

    def test_una_cuenta_sin_fila_de_verificacion_cuenta_como_verificada(self):
        User.objects.create_user(username=CORREO, email=CORREO, password=CONTRASENA)
        # Sin grupo no tiene rol: llega hasta crear_tokens, no se detiene en la verificación.
        response = self.entrar()

        self.assertNotIn("verificar_correo", response.data)

    @override_settings(GOOGLE_CLIENT_IDS=["cliente.apps.googleusercontent.com"])
    def test_entrar_con_google_da_por_verificado_el_correo_pendiente(self):
        self.registrar_estudiante()
        datos = {"email": CORREO, "email_verified": True, "hd": "ejemplo.edu.pe", "name": "Ana"}

        with patch("cuentas.google.id_token.verify_oauth2_token", return_value=datos):
            response = self.client.post("/api/auth/google/", {"id_token": "x", "cliente": "user-web"}, format="json")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.entrar().status_code, 200)
