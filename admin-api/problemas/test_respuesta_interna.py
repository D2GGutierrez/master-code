# H1 (HU45 a HU48): la ruta interna que entrega a user-api la respuesta esperada y las alternativas con su marca de
# correcta, para corregir ejercicios del celular. Se prueba lo más delicado: que sin la clave interna no entra nadie
# y que parte de visibles_para_estudiante(), así que un problema sin aprobar o descartado no se puede corregir ni
# conociendo su id (ADR-2).
from django.test import override_settings

from problemas.models import OpcionRespuesta
from problemas.test_interno import (
    ALTERNATIVA_CORRECTA,
    ESCRITURA,
    OPCION,
    PREDICCION,
    RESPUESTA_SECRETA,
    InternoBase,
)


def url(problema_id):
    return f"/internal/problemas/{problema_id}/respuesta/"


class ClaveDeRespuestaTests(InternoBase):
    def test_sin_clave_con_clave_equivocada_o_sin_clave_en_el_servidor_se_rechaza(self):
        problema = self.crear_problema(tipo=PREDICCION, respuesta_esperada=RESPUESTA_SECRETA)

        self.assertEqual(self.pedir(url(problema.pk), clave=None).status_code, 403)
        self.assertEqual(self.pedir(url(problema.pk), clave="otra").status_code, 403)
        with override_settings(INTERNAL_API_KEY=""):
            self.assertEqual(self.pedir(url(problema.pk), clave="").status_code, 403)

    def test_un_token_de_docente_no_reemplaza_la_clave(self):
        problema = self.crear_problema(tipo=PREDICCION, respuesta_esperada=RESPUESTA_SECRETA)
        self.autenticar(self.docente)

        self.assertEqual(self.pedir(url(problema.pk), clave=None).status_code, 403)

    def test_el_rechazo_no_filtra_la_respuesta(self):
        problema = self.crear_problema(tipo=PREDICCION, respuesta_esperada=RESPUESTA_SECRETA)

        respuesta = self.pedir(url(problema.pk), clave="otra")

        self.assertNotIn("SECRETA", respuesta.content.decode())


class RespuestaDeUnProblemaTests(InternoBase):
    def test_trae_la_respuesta_esperada_de_una_prediccion_de_salida(self):
        problema = self.crear_problema(tipo=PREDICCION, respuesta_esperada=RESPUESTA_SECRETA)

        respuesta = self.pedir(url(problema.pk))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            respuesta.json(),
            {
                "id": problema.pk,
                "curso_id": self.curso.pk,
                "tipo_ejercicio": PREDICCION,
                "respuesta_esperada": RESPUESTA_SECRETA,
                "opciones": [],
            },
        )

    def test_trae_las_alternativas_en_orden_con_su_marca_de_correcta(self):
        problema = self.crear_problema(tipo=OPCION)
        primera = OpcionRespuesta.objects.create(problema=problema, texto="Una", es_correcta=False)
        correcta = OpcionRespuesta.objects.create(problema=problema, texto=ALTERNATIVA_CORRECTA, es_correcta=True)
        tercera = OpcionRespuesta.objects.create(problema=problema, texto="Tres", es_correcta=False)

        cuerpo = self.pedir(url(problema.pk)).json()

        self.assertEqual(
            cuerpo["opciones"],
            [
                {"id": primera.pk, "texto": "Una", "es_correcta": False},
                {"id": correcta.pk, "texto": ALTERNATIVA_CORRECTA, "es_correcta": True},
                {"id": tercera.pk, "texto": "Tres", "es_correcta": False},
            ],
        )
        self.assertEqual(cuerpo["respuesta_esperada"], "")

    def test_no_mezcla_alternativas_de_otro_problema(self):
        problema = self.crear_problema(tipo=OPCION)
        otro = self.crear_problema(tipo=OPCION, titulo="Otro")
        OpcionRespuesta.objects.create(problema=problema, texto="Mía", es_correcta=True)
        OpcionRespuesta.objects.create(problema=otro, texto="De otro", es_correcta=True)

        opciones = self.pedir(url(problema.pk)).json()["opciones"]

        self.assertEqual([o["texto"] for o in opciones], ["Mía"])

    def test_trae_solo_lo_necesario_para_corregir(self):
        problema = self.crear_problema(tipo=PREDICCION, respuesta_esperada="5")

        cuerpo = self.pedir(url(problema.pk)).json()

        self.assertEqual(set(cuerpo), {"id", "curso_id", "tipo_ejercicio", "respuesta_esperada", "opciones"})

    def test_un_problema_de_codigo_trae_la_respuesta_vacia(self):
        problema = self.crear_problema(tipo=ESCRITURA)

        cuerpo = self.pedir(url(problema.pk)).json()

        self.assertEqual(cuerpo["respuesta_esperada"], "")
        self.assertEqual(cuerpo["opciones"], [])

    def test_sin_aprobar_descartado_o_inexistente_da_la_misma_404(self):
        # Mismo cuerpo en los tres casos: adivinar ids no revela cuáles existen sin aprobar.
        cuerpos = []
        for estado in ("pendiente", "borrador", "archivado"):
            problema = self.crear_problema(tipo=PREDICCION, estado=estado, respuesta_esperada=RESPUESTA_SECRETA)
            respuesta = self.pedir(url(problema.pk))
            with self.subTest(estado=estado):
                self.assertEqual(respuesta.status_code, 404)
                self.assertNotIn("SECRETA", respuesta.content.decode())
            cuerpos.append(respuesta.content)
        inexistente = self.pedir(url(999999))
        self.assertEqual(inexistente.status_code, 404)
        self.assertEqual(set(cuerpos), {inexistente.content})

    def test_un_problema_que_se_archiva_despues_deja_de_poder_corregirse(self):
        problema = self.crear_problema(tipo=PREDICCION, respuesta_esperada="5")
        self.assertEqual(self.pedir(url(problema.pk)).status_code, 200)

        problema.archivar()

        self.assertEqual(self.pedir(url(problema.pk)).status_code, 404)
