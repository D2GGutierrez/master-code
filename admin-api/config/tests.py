from django.test import SimpleTestCase


# SimpleTestCase no crea base de datos de prueba.
class HealthTests(SimpleTestCase):
    def test_health_responde_ok(self):
        response = self.client.get("/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})
