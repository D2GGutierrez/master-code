# HU17-T5 (CA-17.4) y HU17-T6 (CA-17.5): validar lo que genera la IA y guardarlo como pendiente;
# si la IA falla o responde mal, un error claro y nada guardado.
from copy import deepcopy
from unittest import mock

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.ia import IaNoDisponible, IaRespuestaInvalida
from problemas.models import CasoPrueba, OpcionRespuesta, Problema
from problemas.views import MENSAJE_IA_CAIDA, MENSAJE_IA_INVALIDA

ESCRITURA = {
    "titulo": "Doble de un número",
    "enunciado": "Lee un entero y muestra su doble.",
    "tipo_ejercicio": "escritura_codigo",
    "casos_prueba": [
        {"entrada": "2", "salida_esperada": "4", "es_publico": True},
        {"entrada": "0", "salida_esperada": "0", "es_publico": False},
    ],
}
PREDICCION = {
    "titulo": "¿Qué imprime?",
    "enunciado": "print(2 + 3)",
    "tipo_ejercicio": "prediccion_salida",
    "respuesta_esperada": "5",
}
OPCION = {
    "titulo": "Elige la salida",
    "enunciado": "print(2 + 3)",
    "tipo_ejercicio": "opcion_multiple",
    "opciones": [{"texto": "4", "es_correcta": False}, {"texto": "5", "es_correcta": True}],
}
COMPLETAR = {
    "titulo": "Completa",
    "enunciado": "print(2 ____ 3)",
    "tipo_ejercicio": "completar_espacio",
    "respuesta_esperada": "+",
}
CUERPO = {"tema": "operadores", "nivel": "facil", "cantidad": 4}

# Se reemplaza el cliente en la vista: aquí se prueba qué hace admin-api con lo que llega, no la red.
CLIENTE = "problemas.views.generar_problemas"


class GenerarProblemasTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.curso = Curso.objects.create(docente=self.docente, nombre="Algoritmos I", lenguaje="java")
        self.autenticar(self.docente)
        self.url = f"/api/cursos/{self.curso.pk}/problemas/generar/"

    def generar(self, cuerpo=None):
        return self.client.post(self.url, CUERPO if cuerpo is None else cuerpo, format="json")

    def tanda(self, *problemas):
        return deepcopy(list(problemas))

    # --- CA-17.4: lo válido se guarda como pendiente ---

    def test_lo_valido_se_guarda_de_ia_y_sin_aprobar(self):
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, PREDICCION, OPCION, COMPLETAR)):
            response = self.generar()

        self.assertEqual(response.status_code, 201)
        self.assertEqual(len(response.data["problemas"]), 4)
        self.assertEqual(Problema.objects.count(), 4)
        for problema in Problema.objects.all():
            self.assertTrue(problema.generado_por_ia)
            self.assertFalse(problema.aprobado_por_docente)
            self.assertIsNone(problema.aprobado_por)
            self.assertIsNone(problema.fecha_aprobacion)
            self.assertFalse(problema.archivado)
            self.assertEqual(problema.curso, self.curso)
        self.assertTrue(all(p["generado_por_ia"] and not p["aprobado_por_docente"] for p in response.data["problemas"]))

    def test_nunca_quedan_visibles_para_el_estudiante(self):
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, PREDICCION, OPCION, COMPLETAR)):
            self.generar()

        self.assertEqual(Problema.objects.visibles_para_estudiante().count(), 0)
        self.assertEqual(Problema.objects.pendientes_de_revision().count(), 4)

    def test_guarda_los_casos_las_alternativas_y_la_respuesta_esperada(self):
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, PREDICCION, OPCION, COMPLETAR)):
            self.generar()

        escritura = Problema.objects.get(tipo_ejercicio="escritura_codigo")
        self.assertEqual(
            list(escritura.casos_prueba.values_list("entrada", "salida_esperada", "es_publico")),
            [("2", "4", True), ("0", "0", False)],
        )
        opcion = Problema.objects.get(tipo_ejercicio="opcion_multiple")
        self.assertEqual(list(opcion.opciones.values_list("texto", "es_correcta")), [("4", False), ("5", True)])
        self.assertEqual(Problema.objects.get(tipo_ejercicio="prediccion_salida").respuesta_esperada, "5")
        self.assertEqual(Problema.objects.get(tipo_ejercicio="completar_espacio").respuesta_esperada, "+")
        self.assertEqual(CasoPrueba.objects.count(), 2)
        self.assertEqual(OpcionRespuesta.objects.count(), 2)

    def test_la_dificultad_y_el_lenguaje_salen_de_la_solicitud_y_la_plataforma_del_tipo(self):
        cuerpo = {**CUERPO, "nivel": "dificil", "lenguaje": "python", "cantidad": 2}
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, OPCION)):
            self.generar(cuerpo)

        escritura = Problema.objects.get(tipo_ejercicio="escritura_codigo")
        opcion = Problema.objects.get(tipo_ejercicio="opcion_multiple")
        for problema in (escritura, opcion):
            self.assertEqual(problema.dificultad, "dificil")
            self.assertEqual(problema.lenguaje, "python")
        # Pidió «ambas» (el valor por defecto), pero cada problema va solo a la plataforma de su tipo.
        self.assertEqual(escritura.plataforma, "web")
        self.assertEqual(opcion.plataforma, "movil")

    def test_la_solicitud_a_la_ia_lleva_los_datos_del_docente_con_sus_valores_por_defecto(self):
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA)) as cliente:
            self.generar({"tema": "  bucles  ", "nivel": "medio", "cantidad": 1, "plataforma": "web"})

        # El lenguaje por defecto es el del curso (java), y el tema llega sin espacios sobrantes.
        cliente.assert_called_once_with(
            {"tema": "bucles", "nivel": "medio", "lenguaje": "java", "cantidad": 1, "plataforma": "web"}
        )

    def test_sin_plataforma_pide_ambas(self):
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, PREDICCION, OPCION, COMPLETAR)) as cliente:
            self.generar()

        self.assertEqual(cliente.call_args.args[0]["plataforma"], "ambas")

    # --- CA-17.4: lo inválido no se guarda ---

    def tandas_invalidas(self):
        sin_enunciado = {**ESCRITURA, "enunciado": ""}
        sin_casos = {k: v for k, v in ESCRITURA.items() if k != "casos_prueba"}
        sin_correcta = {**OPCION, "opciones": [{"texto": "4", "es_correcta": False}, {"texto": "5", "es_correcta": False}]}
        dos_correctas = {**OPCION, "opciones": [{"texto": "4", "es_correcta": True}, {"texto": "5", "es_correcta": True}]}
        una_opcion = {**OPCION, "opciones": [{"texto": "4", "es_correcta": True}]}
        sin_respuesta = {k: v for k, v in PREDICCION.items() if k != "respuesta_esperada"}
        titulo_largo = {**ESCRITURA, "titulo": "x" * 151}
        tipo_raro = {**ESCRITURA, "tipo_ejercicio": "ensayo"}
        mezcla = {**PREDICCION, "opciones": [{"texto": "A", "es_correcta": True}]}
        return {
            "sin enunciado": [sin_enunciado],
            "escritura sin casos": [sin_casos],
            "opción sin correcta": [sin_correcta],
            "opción con dos correctas": [dos_correctas],
            "opción con una sola alternativa": [una_opcion],
            "predicción sin respuesta": [sin_respuesta],
            "título demasiado largo": [titulo_largo],
            "tipo desconocido": [tipo_raro],
            "datos de otro tipo": [mezcla],
            "no es un objeto": ["texto suelto"],
        }

    def test_un_problema_invalido_no_se_guarda_y_el_error_es_claro(self):
        for nombre, tanda in self.tandas_invalidas().items():
            with self.subTest(nombre), mock.patch(CLIENTE, return_value=deepcopy(tanda)):
                response = self.generar({**CUERPO, "cantidad": 1})

                self.assertEqual(response.status_code, 502)
                self.assertEqual(response.data, {"detail": MENSAJE_IA_INVALIDA, "reintentar": True})
                self.assertEqual(Problema.objects.count(), 0)

    def test_si_uno_de_varios_falla_no_se_guarda_ninguno(self):
        malo = {**OPCION, "opciones": []}
        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, PREDICCION, malo, COMPLETAR)):
            response = self.generar()

        self.assertEqual(response.status_code, 502)
        self.assertEqual(Problema.objects.count(), 0)
        self.assertEqual(CasoPrueba.objects.count(), 0)

    def test_una_cantidad_distinta_de_la_pedida_no_se_guarda(self):
        for tanda in ([ESCRITURA], [ESCRITURA] * 5, []):
            with self.subTest(llegaron=len(tanda)), mock.patch(CLIENTE, return_value=self.tanda(*tanda)):
                response = self.generar({**CUERPO, "cantidad": 3})

                self.assertEqual(response.status_code, 502)
                self.assertEqual(Problema.objects.count(), 0)

    def test_un_tipo_que_no_sirve_para_la_plataforma_pedida_no_se_guarda(self):
        pedidos = [("web", OPCION), ("movil", ESCRITURA)]
        for plataforma, problema in pedidos:
            with self.subTest(plataforma=plataforma), mock.patch(CLIENTE, return_value=self.tanda(problema)):
                response = self.generar({**CUERPO, "cantidad": 1, "plataforma": plataforma})

                self.assertEqual(response.status_code, 502)
                self.assertEqual(Problema.objects.count(), 0)

    # --- CA-17.5 / HU17-T6: la IA caída o con respuesta inservible ---

    def test_con_la_ia_caida_hay_un_error_claro_con_reintentar_y_no_se_guarda_nada(self):
        with mock.patch(CLIENTE, side_effect=IaNoDisponible("ConnectionRefusedError")):
            response = self.generar()

        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.data, {"detail": MENSAJE_IA_CAIDA, "reintentar": True})
        self.assertEqual(Problema.objects.count(), 0)

    def test_si_ai_service_dice_que_la_ia_respondio_mal_es_502_y_no_se_guarda_nada(self):
        with mock.patch(CLIENTE, side_effect=IaRespuestaInvalida("HTTP 502")):
            response = self.generar()

        self.assertEqual(response.status_code, 502)
        self.assertEqual(response.data, {"detail": MENSAJE_IA_INVALIDA, "reintentar": True})
        self.assertEqual(Problema.objects.count(), 0)

    def test_los_mensajes_no_filtran_detalles_internos(self):
        with mock.patch(CLIENTE, side_effect=IaNoDisponible("HTTPConnectionPool(host='ai-service', port=8002)")):
            texto = str(self.generar().data)

        self.assertNotIn("ai-service", texto)
        self.assertNotIn("8002", texto)

    def test_despues_de_un_fallo_reintentar_funciona(self):
        with mock.patch(CLIENTE, side_effect=IaNoDisponible("x")):
            self.assertEqual(self.generar().status_code, 503)

        with mock.patch(CLIENTE, return_value=self.tanda(ESCRITURA, PREDICCION, OPCION, COMPLETAR)):
            self.assertEqual(self.generar().status_code, 201)

        self.assertEqual(Problema.objects.count(), 4)

    # --- la solicitud del docente ---

    def test_una_solicitud_invalida_da_400_y_no_gasta_la_ia(self):
        invalidas = {
            "sin tema": {"nivel": "facil", "cantidad": 2},
            "tema vacío": {**CUERPO, "tema": "   "},
            "tema larguísimo": {**CUERPO, "tema": "x" * 201},
            "nivel desconocido": {**CUERPO, "nivel": "experto"},
            "lenguaje desconocido": {**CUERPO, "lenguaje": "cobol"},
            "cantidad cero": {**CUERPO, "cantidad": 0},
            "cantidad mayor al tope": {**CUERPO, "cantidad": 11},
            "cantidad sin número": {**CUERPO, "cantidad": "muchos"},
            "plataforma desconocida": {**CUERPO, "plataforma": "escritorio"},
        }
        for nombre, cuerpo in invalidas.items():
            with self.subTest(nombre), mock.patch(CLIENTE) as cliente:
                response = self.generar(cuerpo)

                self.assertEqual(response.status_code, 400)
                cliente.assert_not_called()

    def test_un_curso_ajeno_da_404_y_no_gasta_la_ia(self):
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="Ajeno", lenguaje=LENGUAJES[0])

        with mock.patch(CLIENTE) as cliente:
            response = self.client.post(f"/api/cursos/{ajeno.pk}/problemas/generar/", CUERPO, format="json")

        self.assertEqual(response.status_code, 404)
        cliente.assert_not_called()

    def test_un_curso_inexistente_da_404(self):
        with mock.patch(CLIENTE) as cliente:
            response = self.client.post("/api/cursos/999999/problemas/generar/", CUERPO, format="json")

        self.assertEqual(response.status_code, 404)
        cliente.assert_not_called()

    def test_sin_sesion_da_401(self):
        self.client.credentials()

        with mock.patch(CLIENTE) as cliente:
            response = self.generar()

        self.assertEqual(response.status_code, 401)
        cliente.assert_not_called()
