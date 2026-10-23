# D3: la API interna que consume user-api. Sin clave, o con otra, no entra nadie.
from datetime import timedelta

from django.test import override_settings
from django.utils import timezone

from cursos.models import Curso
from cursos.tests import CursosTestCase

CLAVE = "clave-interna-solo-para-pruebas"


@override_settings(INTERNAL_API_KEY=CLAVE)
class ApiInternaTests(CursosTestCase):
    def crear_curso(self, nombre="Algoritmos I", docente=None):
        docente = docente or self.crear_docente(f"docente-{nombre}")
        return Curso.objects.create(docente=docente, nombre=nombre, lenguaje="python")

    def pedir(self, url, clave=CLAVE):
        cabeceras = {"HTTP_X_INTERNAL_KEY": clave} if clave is not None else {}
        return self.client.get(url, **cabeceras)

    # --- clave ---
    def test_sin_clave_se_rechaza(self):
        curso = self.crear_curso()

        respuesta = self.pedir(f"/internal/cursos/por-codigo/{curso.codigo_invitacion}/", clave=None)

        self.assertEqual(respuesta.status_code, 403)

    def test_con_clave_equivocada_se_rechaza(self):
        curso = self.crear_curso()

        respuesta = self.pedir(f"/internal/cursos/por-codigo/{curso.codigo_invitacion}/", clave="otra")

        self.assertEqual(respuesta.status_code, 403)

    @override_settings(INTERNAL_API_KEY="")
    def test_si_el_servidor_no_tiene_clave_configurada_se_rechaza_todo(self):
        respuesta = self.pedir("/internal/cursos/?ids=1", clave="")

        self.assertEqual(respuesta.status_code, 403)

    def test_un_token_de_docente_no_reemplaza_la_clave(self):
        docente = self.crear_docente("docente-a")
        self.autenticar(docente)

        self.assertEqual(self.pedir("/internal/cursos/?ids=1", clave=None).status_code, 403)

    # --- por código ---
    def test_un_codigo_valido_devuelve_el_curso_y_su_institucion(self):
        curso = self.crear_curso()

        respuesta = self.pedir(f"/internal/cursos/por-codigo/{curso.codigo_invitacion}/")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            respuesta.json(),
            {
                "id": curso.pk,
                "nombre": "Algoritmos I",
                "lenguaje": "python",
                "institucion": curso.docente.institucion,
            },
        )

    def test_el_codigo_se_acepta_en_minusculas(self):
        curso = self.crear_curso()

        respuesta = self.pedir(f"/internal/cursos/por-codigo/{curso.codigo_invitacion.lower()}/")

        self.assertEqual(respuesta.status_code, 200)

    def test_un_codigo_inexistente_da_404(self):
        self.assertEqual(self.pedir("/internal/cursos/por-codigo/ZZZZZZZZ/").status_code, 404)

    def test_un_codigo_vencido_da_410(self):
        curso = self.crear_curso()
        Curso.objects.filter(pk=curso.pk).update(codigo_vence=timezone.now() - timedelta(seconds=1))

        respuesta = self.pedir(f"/internal/cursos/por-codigo/{curso.codigo_invitacion}/")

        self.assertEqual(respuesta.status_code, 410)

    # --- por ids ---
    def test_por_ids_devuelve_solo_los_que_existen(self):
        a = self.crear_curso("A")
        b = self.crear_curso("B")

        respuesta = self.pedir(f"/internal/cursos/?ids={b.pk},{a.pk},999999")

        self.assertEqual([c["id"] for c in respuesta.json()], sorted([a.pk, b.pk]))

    def test_por_ids_sin_ids_devuelve_lista_vacia(self):
        self.assertEqual(self.pedir("/internal/cursos/").json(), [])

    def test_por_ids_con_texto_da_400(self):
        self.assertEqual(self.pedir("/internal/cursos/?ids=1,abc").status_code, 400)

    def test_por_ids_con_demasiados_ids_da_400(self):
        ids = ",".join(str(n) for n in range(101))

        self.assertEqual(self.pedir(f"/internal/cursos/?ids={ids}").status_code, 400)
