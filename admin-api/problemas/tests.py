# CA-009.3: no se guarda un problema sin enunciado.
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.models import Problema
from problemas.serializers import DIFICULTADES


# CursosTestCase aporta crear_docente y autenticar (JWT real); no trae pruebas propias.
class CrearProblemaTests(CursosTestCase):
    def preparar_curso_propio(self):
        # Docente autenticado y dueño del curso: así el único fallo posible es el enunciado.
        docente = self.crear_docente("docente-a")
        curso = Curso.objects.create(
            docente=docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0]
        )
        self.autenticar(docente)
        return f"/api/cursos/{curso.pk}/problemas/"

    def datos_validos(self):
        return {
            "titulo": "Suma de dos números",
            "enunciado": "Lee dos enteros e imprime su suma.",
            "lenguaje": LENGUAJES[0],
            "dificultad": DIFICULTADES[0],
        }

    def test_sin_enunciado_devuelve_error_en_enunciado_y_no_guarda(self):
        url = self.preparar_curso_propio()
        datos = self.datos_validos()
        del datos["enunciado"]

        response = self.client.post(url, datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["enunciado"][0].code, "required")
        self.assertEqual(Problema.objects.count(), 0)

    def test_enunciado_vacio_devuelve_error_en_enunciado_y_no_guarda(self):
        url = self.preparar_curso_propio()
        datos = self.datos_validos()
        datos["enunciado"] = ""

        response = self.client.post(url, datos, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["enunciado"][0].code, "blank")
        self.assertEqual(Problema.objects.count(), 0)

    def test_enunciado_solo_con_espacios_devuelve_error_en_enunciado_y_no_guarda(self):
        url = self.preparar_curso_propio()
        datos = self.datos_validos()
        datos["enunciado"] = "   "

        response = self.client.post(url, datos, format="json")

        # DRF quita los espacios de los extremos y queda vacío.
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.data["enunciado"][0].code, "blank")
        self.assertEqual(Problema.objects.count(), 0)

    def test_con_enunciado_el_mismo_cuerpo_se_guarda(self):
        # Control: demuestra que las pruebas anteriores fallan solo por el enunciado.
        url = self.preparar_curso_propio()
        datos = self.datos_validos()

        response = self.client.post(url, datos, format="json")

        self.assertEqual(response.status_code, 201)
        self.assertEqual(Problema.objects.count(), 1)
        self.assertEqual(Problema.objects.get().enunciado, datos["enunciado"])
