# HU25-T1: GET /internal/cursos/publicados/, los cursos que un estudiante puede ofrecerse a abrir.
# Un curso está publicado cuando tiene al menos un problema aprobado y no descartado.
from django.contrib.auth.models import User
from django.test import override_settings

from cuentas.models import Docente
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.models import Problema
from problemas.serializers import DIFICULTADES

CLAVE = "clave-interna-solo-para-pruebas"
URL = "/internal/cursos/publicados/"


@override_settings(INTERNAL_API_KEY=CLAVE)
class CursosPublicadosTests(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")

    def crear_curso(self, nombre="Algoritmos I", docente=None):
        return Curso.objects.create(docente=docente or self.docente, nombre=nombre, lenguaje=LENGUAJES[0])

    def crear_problema(self, curso, estado="aprobado"):
        problema = Problema.objects.create(
            curso=curso,
            titulo="Suma",
            enunciado="Suma dos números.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.WEB,
            tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
            generado_por_ia=estado == "pendiente",
        )
        if estado in ("aprobado", "archivado"):
            problema.registrar_aprobacion(self.docente)
        if estado == "archivado":
            problema.archivar()
        return problema

    def docente_de(self, institucion, usuario="docente-b"):
        # Un docente de otra institución: crear_docente siempre usa la del dominio configurado.
        user = User.objects.create_user(username=f"{usuario}@{institucion}", email=f"{usuario}@{institucion}", password="x")
        return Docente.objects.create(user=user, nombre=usuario, email=user.email, institucion=institucion)

    def pedir(self, consulta="", clave=CLAVE):
        cabeceras = {"HTTP_X_INTERNAL_KEY": clave} if clave is not None else {}
        return self.client.get(URL + consulta, **cabeceras)

    def ids(self, consulta=""):
        respuesta = self.pedir(consulta)
        self.assertEqual(respuesta.status_code, 200)
        return [curso["id"] for curso in respuesta.json()]

    # --- clave ---
    def test_sin_clave_se_rechaza(self):
        self.assertEqual(self.pedir(clave=None).status_code, 403)

    def test_con_clave_equivocada_se_rechaza(self):
        self.assertEqual(self.pedir(clave="otra").status_code, 403)

    @override_settings(INTERNAL_API_KEY="")
    def test_si_el_servidor_no_tiene_clave_configurada_se_rechaza_todo(self):
        self.assertEqual(self.pedir(clave="").status_code, 403)

    def test_un_token_de_docente_no_reemplaza_la_clave(self):
        self.autenticar(self.docente)

        self.assertEqual(self.pedir(clave=None).status_code, 403)

    # --- qué es un curso publicado ---
    def test_un_curso_con_un_problema_aprobado_esta_publicado(self):
        curso = self.crear_curso()
        self.crear_problema(curso)

        self.assertEqual(self.ids(), [curso.pk])

    def test_un_curso_sin_problemas_no_esta_publicado(self):
        self.crear_curso()

        self.assertEqual(self.ids(), [])

    def test_un_curso_con_solo_problemas_sin_aprobar_no_esta_publicado(self):
        curso = self.crear_curso()
        self.crear_problema(curso, estado="pendiente")
        self.crear_problema(curso, estado="borrador")

        self.assertEqual(self.ids(), [])

    def test_un_curso_con_solo_problemas_archivados_no_esta_publicado(self):
        curso = self.crear_curso()
        self.crear_problema(curso, estado="archivado")

        self.assertEqual(self.ids(), [])

    def test_un_curso_aparece_una_sola_vez_aunque_tenga_varios_aprobados(self):
        curso = self.crear_curso()
        self.crear_problema(curso)
        self.crear_problema(curso)
        self.crear_problema(curso, estado="pendiente")

        self.assertEqual(self.ids(), [curso.pk])

    def test_aprobar_el_primer_problema_publica_el_curso(self):
        curso = self.crear_curso()
        problema = self.crear_problema(curso, estado="pendiente")
        self.assertEqual(self.ids(), [])

        problema.registrar_aprobacion(self.docente)

        self.assertEqual(self.ids(), [curso.pk])

    # --- qué se devuelve ---
    def test_devuelve_el_curso_con_su_institucion_y_nada_mas(self):
        curso = self.crear_curso()
        self.crear_problema(curso)

        self.assertEqual(
            self.pedir().json(),
            [
                {
                    "id": curso.pk,
                    "nombre": "Algoritmos I",
                    "lenguaje": LENGUAJES[0],
                    "institucion": self.docente.institucion,
                }
            ],
        )

    def test_sale_ordenado_por_nombre(self):
        b = self.crear_curso("B curso")
        a = self.crear_curso("A curso")
        self.crear_problema(b)
        self.crear_problema(a)

        self.assertEqual(self.ids(), [a.pk, b.pk])

    def test_resuelve_el_listado_con_una_sola_consulta(self):
        for nombre in ("A", "B", "C"):
            self.crear_problema(self.crear_curso(nombre))

        with self.assertNumQueries(1):
            self.pedir()

    # --- filtros opcionales ---
    def test_filtra_por_institucion_sin_importar_mayusculas(self):
        propio = self.crear_curso("Propio")
        ajeno = self.crear_curso("Ajeno", docente=self.docente_de("otra.edu.pe"))
        self.crear_problema(propio)
        self.crear_problema(ajeno)

        self.assertEqual(self.ids(f"?institucion={self.docente.institucion.upper()}"), [propio.pk])
        self.assertEqual(self.ids("?institucion=otra.edu.pe"), [ajeno.pk])

    def test_una_institucion_sin_cursos_da_lista_vacia(self):
        self.crear_problema(self.crear_curso())

        self.assertEqual(self.ids("?institucion=no-existe.edu.pe"), [])

    def test_busca_por_parte_del_nombre_sin_importar_mayusculas(self):
        algoritmos = self.crear_curso("Algoritmos I")
        bases = self.crear_curso("Bases de datos")
        self.crear_problema(algoritmos)
        self.crear_problema(bases)

        self.assertEqual(self.ids("?buscar=ALGORIT"), [algoritmos.pk])
        self.assertEqual(self.ids("?buscar=datos"), [bases.pk])
        self.assertEqual(self.ids("?buscar=zzz"), [])

    def test_los_filtros_se_combinan_y_no_publican_lo_que_no_esta_publicado(self):
        publicado = self.crear_curso("Algoritmos I")
        borrador = self.crear_curso("Algoritmos II")
        self.crear_problema(publicado)
        self.crear_problema(borrador, estado="pendiente")

        self.assertEqual(self.ids(f"?buscar=algoritmos&institucion={self.docente.institucion}"), [publicado.pk])

    def test_filtros_vacios_equivalen_a_no_filtrar(self):
        curso = self.crear_curso()
        self.crear_problema(curso)

        self.assertEqual(self.ids("?buscar=&institucion="), [curso.pk])
