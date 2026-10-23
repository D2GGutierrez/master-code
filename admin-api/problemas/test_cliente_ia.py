# HU17-T5/T6: el cliente de ai-service contra un servidor HTTP de verdad en 127.0.0.1.
import json
import socket
import threading
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

from django.test import SimpleTestCase, override_settings

from problemas.ia import IaNoDisponible, IaRespuestaInvalida, generar_problemas

SOLICITUD = {"tema": "bucles", "nivel": "facil", "lenguaje": "python", "cantidad": 1, "plataforma": "web"}
PROBLEMA = {"titulo": "Suma", "enunciado": "Suma.", "tipo_ejercicio": "escritura_codigo"}


class ServidorFalso:
    """ai-service de mentira: responde lo que cada prueba le indique y recuerda lo que le llegó."""

    def __init__(self):
        self.codigo = 200
        self.cuerpo = json.dumps({"problemas": [PROBLEMA]}).encode()
        self.demora = 0
        self.recibido = {}
        servidor = self

        class Manejador(BaseHTTPRequestHandler):
            def do_POST(self):
                largo = int(self.headers["Content-Length"])
                servidor.recibido = {"ruta": self.path, "cuerpo": json.loads(self.rfile.read(largo))}
                time.sleep(servidor.demora)
                try:
                    self.send_response(servidor.codigo)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    self.wfile.write(servidor.cuerpo)
                except OSError:
                    pass  # el cliente ya se rindió (prueba de tiempo agotado)

            def log_message(self, *args):
                pass

        self.http = HTTPServer(("127.0.0.1", 0), Manejador)
        self.url = f"http://127.0.0.1:{self.http.server_port}"
        threading.Thread(target=self.http.serve_forever, daemon=True).start()

    def cerrar(self):
        self.http.shutdown()
        self.http.server_close()


class ClienteIaTests(SimpleTestCase):
    def setUp(self):
        self.ia = ServidorFalso()
        self.addCleanup(self.ia.cerrar)
        configuracion = override_settings(AI_SERVICE_URL=self.ia.url)
        configuracion.enable()
        self.addCleanup(configuracion.disable)

    def test_envia_la_solicitud_a_generate_y_devuelve_los_problemas(self):
        self.assertEqual(generar_problemas(SOLICITUD), [PROBLEMA])

        self.assertEqual(self.ia.recibido["ruta"], "/generate")
        self.assertEqual(self.ia.recibido["cuerpo"], SOLICITUD)

    def test_502_es_una_respuesta_invalida_de_la_ia(self):
        self.ia.codigo = 502
        self.ia.cuerpo = b'{"detail": "La IA devolvi\\u00f3 una respuesta inv\\u00e1lida"}'

        with self.assertRaises(IaRespuestaInvalida):
            generar_problemas(SOLICITUD)

    def test_422_es_un_contrato_roto_y_no_un_fallo_de_red(self):
        self.ia.codigo = 422

        with self.assertRaises(IaRespuestaInvalida):
            generar_problemas(SOLICITUD)

    def test_503_y_otros_5xx_son_servicio_no_disponible(self):
        for codigo in (503, 500, 504):
            with self.subTest(codigo=codigo):
                self.ia.codigo = codigo

                with self.assertRaises(IaNoDisponible):
                    generar_problemas(SOLICITUD)

    def test_una_respuesta_200_que_no_cumple_el_contrato_es_invalida(self):
        for cuerpo in (b"esto no es json", b"[]", b'{"otra": 1}', b'{"problemas": "no es lista"}', b"null"):
            with self.subTest(cuerpo=cuerpo):
                self.ia.cuerpo = cuerpo

                with self.assertRaises(IaRespuestaInvalida):
                    generar_problemas(SOLICITUD)

    def test_si_ai_service_esta_apagado_no_esta_disponible(self):
        with socket.socket() as libre:
            libre.bind(("127.0.0.1", 0))
            puerto = libre.getsockname()[1]

        with override_settings(AI_SERVICE_URL=f"http://127.0.0.1:{puerto}"), self.assertRaises(IaNoDisponible):
            generar_problemas(SOLICITUD)

    def test_si_tarda_mas_que_el_tiempo_maximo_no_esta_disponible(self):
        self.ia.demora = 1.5

        with self.assertRaises(IaNoDisponible):
            generar_problemas(SOLICITUD, timeout=0.3)
