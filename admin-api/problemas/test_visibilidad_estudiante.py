# HU31-T4 (CA-031.1 a CA-031.3): la visibilidad para el estudiante la decide el estado del problema, con datos
# reales y la API interna que consume user-api. Completa a test_interno.py, que ya prueba el listado y el detalle
# con cada estado puesto directamente en el modelo. Aquí van los cambios de estado por las acciones reales del
# docente (Aprobar y Descartar) y que un borrador o un descartado no se distinguen de un id inexistente.
from problemas.models import CasoPrueba
from problemas.test_interno import InternoBase

# Centinela fácil de buscar en el texto completo de una respuesta.
TITULO_OCULTO = "TITULO_SIN_APROBAR_XYZ"


class VisibilidadParaElEstudianteTests(InternoBase):
    def setUp(self):
        super().setUp()
        # Las acciones del docente van con su JWT; la API interna solo mira la clave (VistaInterna).
        self.autenticar(self.docente)

    def pendiente_completo(self):
        # De IA, sin revisar y con un caso: se puede aprobar.
        problema = self.crear_problema(estado="pendiente", titulo=TITULO_OCULTO)
        CasoPrueba.objects.create(
            problema=problema, entrada="2 3", salida_esperada="5", es_publico=True
        )
        return problema

    def listado(self):
        respuesta = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/")
        self.assertEqual(respuesta.status_code, 200)
        return respuesta

    def ids_del_listado(self):
        return [fila["id"] for fila in self.listado().json()]

    def detalle(self, problema_id):
        return self.pedir(f"/internal/problemas/{problema_id}/")

    def test_un_borrador_y_un_descartado_responden_igual_que_un_inexistente(self):
        inexistente = self.detalle(999999)
        for estado in ("borrador", "archivado"):
            with self.subTest(estado=estado):
                problema = self.crear_problema(estado=estado, titulo=TITULO_OCULTO)

                respuesta = self.detalle(problema.pk)

                self.assertEqual(respuesta.status_code, 404)
                self.assertEqual(respuesta.content, inexistente.content)

    def test_aprobarlo_por_la_accion_real_lo_hace_visible_en_el_listado_y_en_el_detalle(
        self,
    ):
        visible = self.crear_problema()
        problema = self.pendiente_completo()
        self.assertEqual(self.ids_del_listado(), [visible.pk])
        self.assertNotIn(TITULO_OCULTO, self.listado().content.decode())
        self.assertEqual(self.detalle(problema.pk).status_code, 404)

        aprobado = self.client.post(f"/api/problemas/{problema.pk}/aprobar/")

        self.assertEqual(aprobado.status_code, 200)
        self.assertEqual(self.ids_del_listado(), [visible.pk, problema.pk])
        detalle = self.detalle(problema.pk)
        self.assertEqual(detalle.status_code, 200)
        self.assertEqual(detalle.json()["titulo"], TITULO_OCULTO)

    def test_descartarlo_por_la_accion_real_lo_deja_oculto_y_ya_no_se_puede_aprobar(
        self,
    ):
        problema = self.pendiente_completo()
        inexistente = self.detalle(999999)

        descartado = self.client.post(
            f"/api/problemas/{problema.pk}/descartar/",
            {"confirmar": True},
            format="json",
        )
        aprobado = self.client.post(f"/api/problemas/{problema.pk}/aprobar/")

        self.assertEqual(descartado.status_code, 200)
        # Ninguna acción lo vuelve visible: aprobar un descartado da 409 y no cambia nada.
        self.assertEqual(aprobado.status_code, 409)
        self.assertEqual(self.ids_del_listado(), [])
        self.assertEqual(self.detalle(problema.pk).content, inexistente.content)
