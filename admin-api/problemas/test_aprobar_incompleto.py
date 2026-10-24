# HU19-T4 (CA-023.6, con CA-023.4 y CA-023.5): un problema incompleto no se aprueba, por la ruta real.
# Completa a AprobarTests (test_revision.py), que ya prueba escritura sin casos, opción múltiple sin
# alternativas o sin correcta y predicción sin respuesta. Aquí van las demás formas de «incompleto»
# de problemas/completitud.py, que no quede nada a medias (tampoco aprobado_por) y la edición.
from django.utils import timezone

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from problemas.models import CasoPrueba, OpcionRespuesta, Problema
from problemas.serializers import DIFICULTADES
from problemas.test_revision import ESCRITURA, OPCION, PREDICCION, RevisionBase

COMPLETAR = Problema.TipoEjercicio.COMPLETAR_ESPACIO


class AprobarProblemaIncompletoTests(RevisionBase):
    def aprobar(self, problema):
        return self.client.post(f"/api/problemas/{problema.pk}/aprobar/")

    def opcion_multiple(self, *alternativas):
        problema = self.crear_problema(OPCION, completo=False)
        for texto, es_correcta in alternativas:
            OpcionRespuesta.objects.create(
                problema=problema, texto=texto, es_correcta=es_correcta
            )
        return problema

    def assert_sigue_pendiente(self, problema):
        # Contra la base y no contra la respuesta: la marca, quién y cuándo siguen vacíos.
        problema.refresh_from_db()
        self.assertFalse(problema.aprobado_por_docente)
        self.assertIsNone(problema.aprobado_por)
        self.assertIsNone(problema.fecha_aprobacion)
        self.assertIn(problema.pk, self.ids_pendientes())

    def test_ninguna_forma_de_incompleto_se_aprueba_ni_deja_nada_a_medias(self):
        con_espacios = self.crear_problema(PREDICCION, completo=False)
        Problema.objects.filter(pk=con_espacios.pk).update(respuesta_esperada="   ")
        casos = {
            "escritura sin casos de prueba": (
                self.crear_problema(ESCRITURA, completo=False),
                "casos_prueba",
                "caso de prueba",
            ),
            "opción múltiple con una sola alternativa": (
                self.opcion_multiple(("5", True)),
                "opciones",
                "entre 2 y 6",
            ),
            "opción múltiple con siete alternativas": (
                self.opcion_multiple(
                    ("1", True), *[(str(n), False) for n in range(2, 8)]
                ),
                "opciones",
                "entre 2 y 6",
            ),
            "opción múltiple con una alternativa vacía": (
                self.opcion_multiple(("5", True), ("  ", False)),
                "opciones",
                "vacía",
            ),
            "opción múltiple con dos correctas": (
                self.opcion_multiple(("5", True), ("6", True)),
                "opciones",
                "exactamente una",
            ),
            "completar el espacio sin respuesta esperada": (
                self.crear_problema(COMPLETAR, completo=False),
                "respuesta_esperada",
                "respuesta esperada",
            ),
            "predicción con respuesta solo de espacios": (
                con_espacios,
                "respuesta_esperada",
                "respuesta esperada",
            ),
        }
        for nombre, (problema, campo, motivo) in casos.items():
            with self.subTest(nombre):
                response = self.aprobar(problema)

                self.assertEqual(response.status_code, 400)
                self.assertIn(motivo, response.json()[campo][0])
                self.assert_sigue_pendiente(problema)

    def test_el_mismo_problema_una_vez_completo_si_se_aprueba_con_quien_y_cuando(self):
        problema = self.crear_problema(ESCRITURA, completo=False)
        self.assertEqual(self.aprobar(problema).status_code, 400)
        CasoPrueba.objects.create(
            problema=problema, entrada="2 3", salida_esperada="5", es_publico=True
        )
        antes = timezone.now()

        response = self.aprobar(problema)

        self.assertEqual(response.status_code, 200)
        problema.refresh_from_db()
        self.assertTrue(problema.aprobado_por_docente)
        self.assertEqual(problema.aprobado_por, self.docente)
        self.assertGreaterEqual(problema.fecha_aprobacion, antes)
        self.assertNotIn(problema.pk, self.ids_pendientes())

    def datos_de_edicion(self, tipo, **cambios):
        datos = {
            "titulo": "Suma",
            "enunciado": "Suma dos números.",
            "lenguaje": LENGUAJES[0],
            "dificultad": DIFICULTADES[0],
            "plataforma": Problema.Plataforma.WEB
            if tipo == ESCRITURA
            else Problema.Plataforma.MOVIL,
            "tipo_ejercicio": tipo,
        }
        datos.update(cambios)
        return datos

    def test_la_edicion_no_deja_quitar_los_casos_a_un_problema_de_escritura(self):
        problema = self.crear_problema(ESCRITURA)

        response = self.client.put(
            f"/api/problemas/{problema.pk}/",
            self.datos_de_edicion(ESCRITURA, casos_prueba=[]),
            format="json",
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("casos_prueba", response.json())
        self.assertEqual(problema.casos_prueba.count(), 1)

    def test_quitarle_las_alternativas_al_editar_deja_un_borrador_que_no_se_aprueba(
        self,
    ):
        problema = self.crear_problema(OPCION)
        editado = self.client.put(
            f"/api/problemas/{problema.pk}/",
            self.datos_de_edicion(OPCION, opciones=[]),
            format="json",
        )
        self.assertEqual(editado.status_code, 200)

        response = self.aprobar(problema)

        self.assertEqual(response.status_code, 400)
        self.assertIn("opciones", response.json())
        self.assert_sigue_pendiente(problema)

    def test_un_docente_ajeno_recibe_404_y_no_se_entera_de_que_esta_incompleto(self):
        ajeno = Curso.objects.create(
            docente=self.crear_docente("docente-b"),
            nombre="Ajeno",
            lenguaje=LENGUAJES[0],
        )
        problema = self.crear_problema(ESCRITURA, curso=ajeno, completo=False)

        response = self.aprobar(problema)

        # 404 y no 400: el motivo «le faltan casos» solo lo ve el dueño.
        self.assertEqual(response.status_code, 404)
        self.assertNotIn("casos_prueba", response.json())
        problema.refresh_from_db()
        self.assertFalse(problema.aprobado_por_docente)
        self.assertIsNone(problema.aprobado_por)
