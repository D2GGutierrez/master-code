# HU36-T2: la ruta interna que entrega a user-api los casos completos (ocultos incluidos) para calificar un envío.
# Se prueba lo más delicado: que sin la clave interna no entra nadie y que parte de visibles_para_estudiante(), así que
# un problema sin aprobar o descartado no se puede calificar ni conociendo su id (ADR-2).
from django.test import override_settings

from problemas.models import CasoPrueba
from problemas.test_interno import ESCRITURA, InternoBase


def url(problema_id):
    return f"/internal/problemas/{problema_id}/calificacion/"


class ClaveDeCalificacionTests(InternoBase):
    def test_sin_clave_con_clave_equivocada_o_sin_clave_en_el_servidor_se_rechaza(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="1", salida_esperada="1")

        self.assertEqual(self.pedir(url(problema.pk), clave=None).status_code, 403)
        self.assertEqual(self.pedir(url(problema.pk), clave="otra").status_code, 403)
        with override_settings(INTERNAL_API_KEY=""):
            self.assertEqual(self.pedir(url(problema.pk), clave="").status_code, 403)

    def test_un_token_de_docente_no_reemplaza_la_clave(self):
        problema = self.crear_problema()
        self.autenticar(self.docente)

        self.assertEqual(self.pedir(url(problema.pk), clave=None).status_code, 403)

    def test_el_rechazo_no_filtra_ningun_caso(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="ENTRADA_SECRETA", salida_esperada="SALIDA_SECRETA")

        respuesta = self.pedir(url(problema.pk), clave="otra")

        self.assertNotIn("SECRETA", respuesta.content.decode())


class CalificacionDeUnProblemaTests(InternoBase):
    def test_trae_todos_los_casos_en_orden_con_su_marca_de_publico(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="2 3", salida_esperada="5", es_publico=True)
        CasoPrueba.objects.create(problema=problema, entrada="10 20", salida_esperada="30")
        CasoPrueba.objects.create(problema=problema, entrada="", salida_esperada="0")

        respuesta = self.pedir(url(problema.pk))

        self.assertEqual(respuesta.status_code, 200)
        cuerpo = respuesta.json()
        self.assertEqual(
            cuerpo["casos_prueba"],
            [
                {"entrada": "2 3", "salida_esperada": "5", "es_publico": True},
                {"entrada": "10 20", "salida_esperada": "30", "es_publico": False},
                {"entrada": "", "salida_esperada": "0", "es_publico": False},
            ],
        )

    def test_trae_el_curso_el_lenguaje_y_el_tipo_pero_no_el_enunciado_ni_la_respuesta(self):
        problema = self.crear_problema(respuesta_esperada="RESPUESTA_SECRETA_XYZ")
        CasoPrueba.objects.create(problema=problema, entrada="1", salida_esperada="1")

        cuerpo = self.pedir(url(problema.pk)).json()

        self.assertEqual(set(cuerpo), {"id", "curso_id", "lenguaje", "tipo_ejercicio", "casos_prueba"})
        self.assertEqual(cuerpo["id"], problema.pk)
        self.assertEqual(cuerpo["curso_id"], self.curso.pk)
        self.assertEqual(cuerpo["lenguaje"], problema.lenguaje)
        self.assertEqual(cuerpo["tipo_ejercicio"], ESCRITURA)

    def test_no_mezcla_casos_de_otro_problema(self):
        problema = self.crear_problema()
        otro = self.crear_problema(titulo="Otro")
        CasoPrueba.objects.create(problema=problema, entrada="a", salida_esperada="b")
        CasoPrueba.objects.create(problema=otro, entrada="c", salida_esperada="d")

        casos = self.pedir(url(problema.pk)).json()["casos_prueba"]

        self.assertEqual([c["entrada"] for c in casos], ["a"])

    def test_un_problema_aprobado_de_ia_tambien_se_califica(self):
        problema = self.crear_problema(generado_por_ia=True)
        CasoPrueba.objects.create(problema=problema, entrada="1", salida_esperada="1")

        self.assertEqual(self.pedir(url(problema.pk)).status_code, 200)

    def test_sin_aprobar_descartado_o_inexistente_da_la_misma_404(self):
        # Mismo cuerpo en los tres casos: adivinar ids no revela cuáles existen sin aprobar.
        cuerpos = []
        for estado in ("pendiente", "borrador", "archivado"):
            problema = self.crear_problema(estado=estado)
            CasoPrueba.objects.create(problema=problema, entrada="E_OCULTA_XYZ", salida_esperada="S_OCULTA_XYZ")
            respuesta = self.pedir(url(problema.pk))
            with self.subTest(estado=estado):
                self.assertEqual(respuesta.status_code, 404)
                self.assertNotIn("XYZ", respuesta.content.decode())
            cuerpos.append(respuesta.content)
        inexistente = self.pedir(url(999999))
        self.assertEqual(inexistente.status_code, 404)
        self.assertEqual(set(cuerpos), {inexistente.content})

    def test_un_problema_que_se_archiva_despues_deja_de_poder_calificarse(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="1", salida_esperada="1")
        self.assertEqual(self.pedir(url(problema.pk)).status_code, 200)

        problema.archivar()

        self.assertEqual(self.pedir(url(problema.pk)).status_code, 404)

    def test_la_ruta_publica_del_estudiante_sigue_sin_traer_los_casos_ocultos(self):
        # Control cruzado: agregar esta ruta no abrió ninguna otra.
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="OCULTA_XYZ", salida_esperada="OCULTA_XYZ")

        texto = self.pedir(f"/internal/problemas/{problema.pk}/").content.decode()

        self.assertNotIn("OCULTA_XYZ", texto)
