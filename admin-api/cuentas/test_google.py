# Inicio de sesión con Google: se simula la respuesta de Google (verify_oauth2_token), no se llama a internet.
from unittest.mock import patch

from django.contrib.auth.models import Group, User
from django.test import TestCase, override_settings
from google.auth.exceptions import TransportError
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import AccessToken

from cuentas.models import Docente

GOOGLE_URL = "/api/auth/google/"
VERIFICAR = "cuentas.google.id_token.verify_oauth2_token"


def datos_de_google(email="ana@ejemplo.edu.pe", **cambios):
    datos = {"email": email, "email_verified": True, "hd": "ejemplo.edu.pe", "name": "Ana Pérez"}
    datos.update(cambios)
    return datos


@override_settings(
    DOMINIO_INSTITUCIONAL="ejemplo.edu.pe",
    GOOGLE_CLIENT_IDS=["cliente-web.apps.googleusercontent.com"],
    DOCENTES_AUTORIZADOS={"profe@ejemplo.edu.pe"},
)
class GoogleLoginTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    def entrar(self, cliente="user-web", **google):
        with patch(VERIFICAR, return_value=datos_de_google(**google)):
            return self.client.post(GOOGLE_URL, {"id_token": "token-de-google", "cliente": cliente}, format="json")

    def rol_del_access(self, response):
        return AccessToken(response.data["access"])["rol"]

    # --- Estudiantes (user-web y app) ---

    def test_estudiante_nuevo_se_crea_activo_con_grupo_y_sin_contrasena(self):
        response = self.entrar()

        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.rol_del_access(response), "estudiante")
        user = User.objects.get(username="ana@ejemplo.edu.pe")
        self.assertTrue(user.is_active)
        self.assertEqual(user.first_name, "Ana Pérez")
        self.assertFalse(user.has_usable_password())
        self.assertTrue(user.groups.filter(name="estudiante").exists())

    def test_estudiante_que_ya_existe_no_se_duplica(self):
        self.entrar()
        response = self.entrar(cliente="mobile")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(User.objects.filter(username="ana@ejemplo.edu.pe").count(), 1)
        self.assertEqual(AccessToken(response.data["access"])["cliente"], "mobile")

    def test_cuenta_creada_con_contrasena_entra_con_google_sin_perder_la_contrasena(self):
        user = User.objects.create_user(username="ana@ejemplo.edu.pe", email="ana@ejemplo.edu.pe", password="Clave-Falsa-2026")
        user.groups.add(Group.objects.get_or_create(name="estudiante")[0])

        response = self.entrar()

        self.assertEqual(response.status_code, 200)
        user.refresh_from_db()
        self.assertTrue(user.check_password("Clave-Falsa-2026"))

    def test_correo_de_docente_no_puede_crear_cuenta_de_estudiante(self):
        response = self.entrar(email="profe@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 403)
        self.assertFalse(User.objects.exists())

    def test_estudiante_inactivo_no_entra(self):
        User.objects.create_user(username="ana@ejemplo.edu.pe", email="ana@ejemplo.edu.pe", is_active=False)

        self.assertEqual(self.entrar().status_code, 403)

    def test_el_refresh_que_entrega_google_sirve_para_renovar(self):
        refresh = self.entrar().data["refresh"]

        response = self.client.post("/api/auth/refresh/", {"refresh": refresh}, format="json")

        self.assertEqual(response.status_code, 200)

    # --- Docentes (admin-web) ---

    def test_docente_autorizado_se_crea_activo_con_perfil(self):
        response = self.entrar(cliente="admin-web", email="profe@ejemplo.edu.pe", name="Profe Uno")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.rol_del_access(response), "docente")
        docente = Docente.objects.get(email="profe@ejemplo.edu.pe")
        self.assertEqual(docente.nombre, "Profe Uno")
        self.assertEqual(docente.institucion, "ejemplo.edu.pe")
        self.assertTrue(docente.user.is_active)

    def test_correo_que_no_esta_en_la_lista_no_entra_como_docente(self):
        response = self.entrar(cliente="admin-web")

        self.assertEqual(response.status_code, 403)
        self.assertIn("lista de docentes", response.data["detail"])
        self.assertFalse(User.objects.exists())
        self.assertFalse(Docente.objects.exists())

    def test_cuenta_de_estudiante_no_entra_al_panel_de_docentes(self):
        self.entrar(email="profe@ejemplo.edu.pe")  # rechazada: no deja cuenta
        User.objects.create_user(username="profe@ejemplo.edu.pe", email="profe@ejemplo.edu.pe")

        response = self.entrar(cliente="admin-web", email="profe@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 403)
        self.assertEqual(response.data["detail"], "Esta cuenta no es de docente.")

    def test_docente_registrado_con_contrasena_se_activa_al_entrar_con_google_si_esta_en_la_lista(self):
        user = User.objects.create_user(username="profe@ejemplo.edu.pe", email="profe@ejemplo.edu.pe", is_active=False)
        Docente.objects.create(user=user, nombre="Profe", email=user.email, institucion="ejemplo.edu.pe")

        response = self.entrar(cliente="admin-web", email="profe@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 200)
        user.refresh_from_db()
        self.assertTrue(user.is_active)

    def test_docente_pendiente_que_no_esta_en_la_lista_sigue_sin_entrar(self):
        user = User.objects.create_user(username="otro@ejemplo.edu.pe", email="otro@ejemplo.edu.pe", is_active=False)
        Docente.objects.create(user=user, nombre="Otro", email=user.email, institucion="ejemplo.edu.pe")

        response = self.entrar(cliente="admin-web", email="otro@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 403)
        user.refresh_from_db()
        self.assertFalse(user.is_active)

    def test_docente_ya_activado_por_el_administrador_entra_aunque_no_este_en_la_lista(self):
        user = User.objects.create_user(username="otro@ejemplo.edu.pe", email="otro@ejemplo.edu.pe")
        Docente.objects.create(user=user, nombre="Otro", email=user.email, institucion="ejemplo.edu.pe")

        response = self.entrar(cliente="admin-web", email="otro@ejemplo.edu.pe")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.rol_del_access(response), "docente")

    # --- Verificación del token de Google ---

    def test_correo_de_otro_dominio_se_rechaza(self):
        response = self.entrar(email="ana@gmail.com", hd="gmail.com")

        self.assertEqual(response.status_code, 401)
        self.assertFalse(User.objects.exists())

    def test_cuenta_personal_con_correo_del_dominio_pero_sin_hd_se_rechaza(self):
        datos = datos_de_google()
        del datos["hd"]
        with patch(VERIFICAR, return_value=datos):
            response = self.client.post(GOOGLE_URL, {"id_token": "x", "cliente": "user-web"}, format="json")

        self.assertEqual(response.status_code, 401)

    def test_hd_de_otro_dominio_se_rechaza_aunque_el_correo_parezca_institucional(self):
        self.assertEqual(self.entrar(hd="otro.edu.pe").status_code, 401)

    def test_correo_sin_verificar_se_rechaza(self):
        self.assertEqual(self.entrar(email_verified=False).status_code, 401)

    def test_token_invalido_o_para_otro_cliente_se_rechaza(self):
        with patch(VERIFICAR, side_effect=ValueError("Wrong recipient")):
            response = self.client.post(GOOGLE_URL, {"id_token": "x", "cliente": "user-web"}, format="json")

        self.assertEqual(response.status_code, 401)

    def test_se_pasan_los_id_de_cliente_configurados_como_audiencia(self):
        with patch(VERIFICAR, return_value=datos_de_google()) as verificar:
            self.client.post(GOOGLE_URL, {"id_token": "x", "cliente": "user-web"}, format="json")

        self.assertEqual(verificar.call_args.args[2], ["cliente-web.apps.googleusercontent.com"])

    def test_si_no_se_puede_hablar_con_google_responde_503(self):
        with patch(VERIFICAR, side_effect=TransportError("sin red")):
            response = self.client.post(GOOGLE_URL, {"id_token": "x", "cliente": "user-web"}, format="json")

        self.assertEqual(response.status_code, 503)

    @override_settings(GOOGLE_CLIENT_IDS=[])
    def test_sin_id_de_cliente_el_inicio_con_google_esta_apagado(self):
        response = self.client.post(GOOGLE_URL, {"id_token": "x", "cliente": "user-web"}, format="json")

        self.assertEqual(response.status_code, 503)

    def test_faltan_datos(self):
        self.assertEqual(self.client.post(GOOGLE_URL, {"cliente": "user-web"}, format="json").status_code, 400)
        self.assertEqual(self.client.post(GOOGLE_URL, {"id_token": "x", "cliente": "otro"}, format="json").status_code, 400)
