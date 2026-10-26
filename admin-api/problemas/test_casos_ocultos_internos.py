# HU32-T4 (CA-012.4): los casos ocultos no viajan al estudiante, por la ruta interna que consume user-api.
# Completa a test_interno.py (un público y un oculto en el detalle y el listado, con centinelas) y a
# test_casos_ocultos.py (el serializer). Aquí van el problema con todos sus casos ocultos por la ruta real
# y el docente que vuelve oculto un caso que antes era público.
from problemas.models import CasoPrueba, Problema
from problemas.test_interno import InternoBase

# Centinelas: textos que solo pueden aparecer si el caso viajó.
ENTRADA_OCULTA = "OCULTO-ENTRADA-7391"
SALIDA_OCULTA = "OCULTO-SALIDA-5528"
ENTRADA_PUBLICA = "PUBLICO-ENTRADA-1846"
SALIDA_PUBLICA = "PUBLICO-SALIDA-2275"


class CasosOcultosEnLaRutaInternaTests(InternoBase):
    def detalle(self, problema):
        respuesta = self.pedir(f"/internal/problemas/{problema.pk}/")
        self.assertEqual(respuesta.status_code, 200)
        return respuesta

    def test_con_todos_los_casos_ocultos_el_detalle_trae_una_lista_vacia(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(
            problema=problema, entrada=ENTRADA_OCULTA, salida_esperada=SALIDA_OCULTA
        )
        CasoPrueba.objects.create(
            problema=problema, entrada="otra entrada", salida_esperada="otra salida"
        )

        respuesta = self.detalle(problema)

        self.assertEqual(respuesta.json()["casos_prueba"], [])
        texto = respuesta.content.decode()
        self.assertNotIn(ENTRADA_OCULTA, texto)
        self.assertNotIn(SALIDA_OCULTA, texto)
        self.assertNotIn("otra entrada", texto)

    def test_un_caso_que_el_docente_vuelve_oculto_deja_de_viajar_y_uno_nuevo_oculto_nunca_viaja(
        self,
    ):
        problema = self.crear_problema()
        CasoPrueba.objects.create(
            problema=problema,
            entrada=ENTRADA_PUBLICA,
            salida_esperada=SALIDA_PUBLICA,
            es_publico=True,
        )
        # Control: mientras es público, el estudiante sí lo ve.
        self.assertIn(ENTRADA_PUBLICA, self.detalle(problema).content.decode())
        self.autenticar(self.docente)

        # La acción real de edición (HU19-T1): PUT reemplaza los casos por los que se envían.
        editado = self.client.put(
            f"/api/problemas/{problema.pk}/",
            {
                "titulo": "Suma",
                "enunciado": "Suma dos números.",
                "lenguaje": problema.lenguaje,
                "dificultad": problema.dificultad,
                "plataforma": Problema.Plataforma.WEB,
                "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
                "casos_prueba": [
                    {
                        "entrada": ENTRADA_PUBLICA,
                        "salida_esperada": SALIDA_PUBLICA,
                        "es_publico": False,
                    },
                    {
                        "entrada": ENTRADA_OCULTA,
                        "salida_esperada": SALIDA_OCULTA,
                        "es_publico": False,
                    },
                ],
            },
            format="json",
        )

        self.assertEqual(editado.status_code, 200)
        respuesta = self.detalle(problema)
        self.assertEqual(respuesta.json()["casos_prueba"], [])
        texto = respuesta.content.decode()
        for centinela in (
            ENTRADA_PUBLICA,
            SALIDA_PUBLICA,
            ENTRADA_OCULTA,
            SALIDA_OCULTA,
        ):
            self.assertNotIn(centinela, texto)
