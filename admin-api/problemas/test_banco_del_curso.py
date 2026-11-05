# HU62-T2 (servidor): GET /api/cursos/<id>/problemas/ lista el banco del curso para elegir los de una evaluación.
from cursos.models import Curso
from cursos.tests import CursosTestCase
from problemas.models import Problema


class BancoDelCursoTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.curso = Curso.objects.create(docente=self.docente, nombre="Algoritmos I", lenguaje="python")
        self.autenticar(self.docente)

    def crear_problema(self, titulo, curso=None, aprobado=True, archivado=False, **extra):
        datos = {
            "enunciado": "Texto largo del enunciado.",
            "lenguaje": "python",
            "dificultad": "facil",
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
            **extra,
        }
        problema = Problema.objects.create(curso=curso or self.curso, titulo=titulo, archivado=archivado, **datos)
        if aprobado:
            problema.registrar_aprobacion(self.docente)
        return problema

    def url(self, curso_id=None):
        return f"/api/cursos/{curso_id or self.curso.pk}/problemas/"

    def test_lista_los_problemas_del_curso_con_lo_necesario_para_elegirlos(self):
        suma = self.crear_problema("Suma", dificultad="medio")

        response = self.client.get(self.url())

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            [
                {
                    "id": suma.pk,
                    "titulo": "Suma",
                    "dificultad": "medio",
                    "tipo_ejercicio": "escritura_codigo",
                    "plataforma": "web",
                    "aprobado_por_docente": True,
                }
            ],
        )

    def test_trae_tambien_los_sin_aprobar_marcados_para_que_la_pantalla_los_distinga(self):
        aprobado = self.crear_problema("Aprobado")
        borrador = self.crear_problema("Borrador", aprobado=False)

        por_id = {fila["id"]: fila for fila in self.client.get(self.url()).json()}

        self.assertTrue(por_id[aprobado.pk]["aprobado_por_docente"])
        self.assertFalse(por_id[borrador.pk]["aprobado_por_docente"])

    def test_no_trae_los_descartados_ni_los_de_otros_cursos(self):
        propio = self.crear_problema("Propio")
        self.crear_problema("Descartado", archivado=True)
        otro_curso = Curso.objects.create(docente=self.docente, nombre="Otro curso", lenguaje="python")
        self.crear_problema("De otro curso", curso=otro_curso)

        ids = [fila["id"] for fila in self.client.get(self.url()).json()]

        self.assertEqual(ids, [propio.pk])

    def test_van_ordenados_por_id(self):
        primero = self.crear_problema("Primero")
        segundo = self.crear_problema("Segundo")
        tercero = self.crear_problema("Tercero")

        ids = [fila["id"] for fila in self.client.get(self.url()).json()]

        self.assertEqual(ids, [primero.pk, segundo.pk, tercero.pk])

    def test_un_curso_sin_problemas_da_lista_vacia(self):
        response = self.client.get(self.url())

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

    def test_un_curso_ajeno_da_el_mismo_404_que_uno_inexistente(self):
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="De B", lenguaje="python")

        de_otro = self.client.get(self.url(ajeno.pk))
        que_no_existe = self.client.get(self.url(ajeno.pk + 100_000))

        self.assertEqual(de_otro.status_code, 404)
        self.assertEqual(que_no_existe.status_code, 404)
        self.assertEqual(de_otro.content, que_no_existe.content)

    def test_sin_token_da_401(self):
        self.client.credentials()

        self.assertEqual(self.client.get(self.url()).status_code, 401)

    # La ruta sigue sirviendo para crear: agregar el listado no cambió el POST.
    def test_el_post_sigue_creando_problemas(self):
        cuerpo = {
            "titulo": "Nuevo",
            "enunciado": "Suma dos números.",
            "lenguaje": "python",
            "dificultad": "facil",
            "plataforma": "web",
            "tipo_ejercicio": "escritura_codigo",
            "casos_prueba": [{"entrada": "1 2", "salida_esperada": "3", "es_publico": True}],
        }

        response = self.client.post(self.url(), cuerpo, format="json")

        self.assertEqual(response.status_code, 201)
        self.assertEqual(Problema.objects.filter(curso=self.curso).count(), 1)
