# HU39-T3: la entrada de un caso de prueba no puede superar el tope de ai-service, o el caso nunca se podría ejecutar.
from cursos.models import Curso
from cursos.tests import CursosTestCase
from problemas.models import CasoPrueba, Problema
from problemas.serializers import MAX_ENTRADA


class LimiteDeEntradaTests(CursosTestCase):
    def crear(self, entrada):
        docente = self.crear_docente("docente-limite")
        curso = Curso.objects.create(docente=docente, nombre="Curso de prueba", lenguaje="python")
        self.autenticar(docente)
        datos = {
            "titulo": "Eco largo",
            "enunciado": "Imprime lo que lees.",
            "lenguaje": "python",
            "dificultad": "facil",
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
            "casos_prueba": [{"entrada": entrada, "salida_esperada": "x", "es_publico": False}],
        }
        return self.client.post(f"/api/cursos/{curso.pk}/problemas/", datos, format="json")

    def test_una_entrada_mas_larga_que_el_tope_se_rechaza_con_un_mensaje_claro(self):
        respuesta = self.crear("a" * (MAX_ENTRADA + 1))

        self.assertEqual(respuesta.status_code, 400)
        error = respuesta.data["casos_prueba"][0]["entrada"][0]
        self.assertEqual(error.code, "max_length")
        self.assertIn("10000 caracteres", str(error))
        self.assertEqual(Problema.objects.count(), 0)

    def test_una_entrada_exactamente_en_el_tope_se_acepta(self):
        respuesta = self.crear("a" * MAX_ENTRADA)

        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(len(CasoPrueba.objects.get().entrada), MAX_ENTRADA)
