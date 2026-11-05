# HU44-T4 (decisión de producto): un curso de otro docente responde exactamente igual que uno que no existe, así un
# docente no puede saber si un id existe. Tampoco se pregunta nada a user-api en ninguno de los dos casos.
from unittest.mock import patch

from cursos.models import Curso
from cursos.tests import CursosTestCase

INSCRITOS = "cursos.views.inscritos_de"
RESUMEN = "cursos.views.resumen_envios_de"


class CursoAjenoIgualQueInexistenteTests(CursosTestCase):
    def test_las_cuatro_rutas_responden_igual_a_un_curso_ajeno_y_a_uno_inexistente(self):
        dueno = self.crear_docente("docente-a")
        ajeno = Curso.objects.create(docente=dueno, nombre="Curso de A", lenguaje="python")
        inexistente = ajeno.pk + 100_000
        self.autenticar(self.crear_docente("docente-b"))
        cuerpo = {"nombre": "Curso cambiado", "lenguaje": "python"}
        rutas = {
            "editar (PUT)": lambda pk: self.client.put(f"/api/cursos/{pk}/", cuerpo, format="json"),
            "editar (PATCH)": lambda pk: self.client.patch(f"/api/cursos/{pk}/", cuerpo, format="json"),
            "eliminar (DELETE)": lambda pk: self.client.delete(f"/api/cursos/{pk}/"),
            "inscritos": lambda pk: self.client.get(f"/api/cursos/{pk}/inscritos/"),
            "resumen de envíos": lambda pk: self.client.get(f"/api/cursos/{pk}/resumen-envios/"),
        }
        for nombre, pedir in rutas.items():
            with self.subTest(nombre), patch(INSCRITOS) as inscritos, patch(RESUMEN) as resumen:
                de_otro = pedir(ajeno.pk)
                que_no_existe = pedir(inexistente)

                self.assertEqual(de_otro.status_code, 404)
                self.assertEqual(que_no_existe.status_code, 404)
                self.assertEqual(de_otro.content, que_no_existe.content)
                inscritos.assert_not_called()
                resumen.assert_not_called()

        # El curso ajeno sigue intacto después de intentar editarlo y borrarlo.
        ajeno.refresh_from_db()
        self.assertEqual(ajeno.nombre, "Curso de A")
