from django.contrib.auth.hashers import check_password, make_password
from django.test import SimpleTestCase


# SimpleTestCase no crea base de datos de prueba.
class HealthTests(SimpleTestCase):
    def test_health_responde_ok(self):
        response = self.client.get("/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})


# Avisa si alguien cambia el hasher por defecto sin querer (CA-004.1).
class HashContrasenaTests(SimpleTestCase):
    def test_contrasena_se_guarda_con_pbkdf2_sha256(self):
        contrasena = "clave-falsa-123"

        guardado = make_password(contrasena)

        self.assertTrue(guardado.startswith("pbkdf2_sha256$"))
        self.assertNotIn(contrasena, guardado)
        self.assertTrue(check_password(contrasena, guardado))
