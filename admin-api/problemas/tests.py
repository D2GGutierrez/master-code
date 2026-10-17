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
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
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


# CA-010.1, CA-010.2 y CA-010.4: combinaciones válidas de plataforma y tipo de ejercicio.
# Cada tipo tiene una sola plataforma posible (HU15): escritura de código solo en web,
# y los otros tres tipos solo en móvil. "ambas" no se prueba: no está definida para ningún tipo.
COMBINACIONES_VALIDAS = [
    (Problema.Plataforma.WEB, Problema.TipoEjercicio.ESCRITURA_CODIGO),
    (Problema.Plataforma.MOVIL, Problema.TipoEjercicio.PREDICCION_SALIDA),
    (Problema.Plataforma.MOVIL, Problema.TipoEjercicio.OPCION_MULTIPLE),
    (Problema.Plataforma.MOVIL, Problema.TipoEjercicio.COMPLETAR_ESPACIO),
]


class CombinacionesValidasTests(CursosTestCase):
    def preparar_curso_propio(self, usuario):
        docente = self.crear_docente(usuario)
        curso = Curso.objects.create(
            docente=docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0]
        )
        self.autenticar(docente)
        return curso

    def test_las_combinaciones_usan_exactamente_las_opciones_del_modelo(self):
        # Si cambian las opciones del modelo, esta prueba avisa que hay que revisar la lista.
        self.assertEqual(set(Problema.Plataforma.values), {"web", "movil", "ambas"})
        self.assertEqual(
            set(Problema.TipoEjercicio.values),
            {
                "escritura_codigo",
                "prediccion_salida",
                "opcion_multiple",
                "completar_espacio",
            },
        )
        # Cada tipo de ejercicio aparece una sola vez en la lista.
        tipos = [tipo for _, tipo in COMBINACIONES_VALIDAS]
        self.assertEqual(sorted(tipos), sorted(Problema.TipoEjercicio.values))

    def test_cada_combinacion_valida_se_guarda_con_su_plataforma_y_tipo(self):
        for indice, (plataforma, tipo) in enumerate(COMBINACIONES_VALIDAS):
            with self.subTest(plataforma=plataforma, tipo_ejercicio=tipo):
                # Docente y curso propios en cada combinación.
                curso = self.preparar_curso_propio(f"docente-{indice}")
                datos = {
                    "titulo": "Suma de dos números",
                    "enunciado": "Lee dos enteros e imprime su suma.",
                    "lenguaje": LENGUAJES[0],
                    "dificultad": DIFICULTADES[0],
                    "plataforma": plataforma,
                    "tipo_ejercicio": tipo,
                }

                response = self.client.post(
                    f"/api/cursos/{curso.pk}/problemas/", datos, format="json"
                )

                self.assertEqual(response.status_code, 201)
                self.assertEqual(response.data["plataforma"], plataforma)
                self.assertEqual(response.data["tipo_ejercicio"], tipo)
                guardados = Problema.objects.filter(curso=curso)
                self.assertEqual(guardados.count(), 1)
                self.assertEqual(guardados.get().plataforma, plataforma)
                self.assertEqual(guardados.get().tipo_ejercicio, tipo)
