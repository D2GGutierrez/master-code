# ADR-2: lo que genera la IA nace sin aprobar, y solo lo aprobado y no descartado llega al estudiante.
# También: la regla de «completo», las alternativas y la aprobación de los problemas creados a mano.
import importlib

from django.apps import apps
from django.db import IntegrityError, transaction
from django.utils import timezone

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.completitud import faltante, faltante_de
from problemas.models import CasoPrueba, OpcionRespuesta, Problema
from problemas.serializers import DIFICULTADES

ESCRITURA = Problema.TipoEjercicio.ESCRITURA_CODIGO
PREDICCION = Problema.TipoEjercicio.PREDICCION_SALIDA
OPCION = Problema.TipoEjercicio.OPCION_MULTIPLE
COMPLETAR = Problema.TipoEjercicio.COMPLETAR_ESPACIO


class ProblemasBase(CursosTestCase):
    def crear_curso(self, usuario="docente-a"):
        return Curso.objects.create(docente=self.crear_docente(usuario), nombre="Algoritmos I", lenguaje=LENGUAJES[0])

    def crear_problema(self, curso, tipo=ESCRITURA, **extra):
        plataforma = Problema.Plataforma.WEB if tipo == ESCRITURA else Problema.Plataforma.MOVIL
        return Problema.objects.create(
            curso=curso,
            titulo="Suma",
            enunciado="Suma dos números.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=plataforma,
            tipo_ejercicio=tipo,
            **extra,
        )


class MarcasDeIaYAprobacionTests(ProblemasBase):
    def test_un_problema_nuevo_no_es_de_ia_ni_esta_aprobado(self):
        problema = self.crear_problema(self.crear_curso())

        self.assertFalse(problema.generado_por_ia)
        self.assertFalse(problema.aprobado_por_docente)
        self.assertEqual(problema.respuesta_esperada, "")

    def test_registrar_aprobacion_marca_el_problema_como_aprobado(self):
        curso = self.crear_curso()
        problema = self.crear_problema(curso)

        problema.registrar_aprobacion(curso.docente)

        problema.refresh_from_db()
        self.assertTrue(problema.aprobado_por_docente)
        self.assertEqual(problema.aprobado_por, curso.docente)
        self.assertIsNotNone(problema.fecha_aprobacion)

    def test_la_base_no_acepta_un_aprobado_sin_fecha(self):
        problema = self.crear_problema(self.crear_curso())

        with self.assertRaises(IntegrityError), transaction.atomic():
            Problema.objects.filter(pk=problema.pk).update(aprobado_por_docente=True, fecha_aprobacion=None)

    def test_pendientes_son_solo_los_de_ia_sin_aprobar_ni_archivar(self):
        curso = self.crear_curso()
        pendiente = self.crear_problema(curso, generado_por_ia=True)
        aprobado = self.crear_problema(curso, generado_por_ia=True)
        aprobado.registrar_aprobacion(curso.docente)
        descartado = self.crear_problema(curso, generado_por_ia=True)
        descartado.archivar()
        self.crear_problema(curso)  # manual, sin aprobar: un borrador, no es de la bandeja

        self.assertEqual(list(Problema.objects.pendientes_de_revision()), [pendiente])
        self.assertNotIn(aprobado, Problema.objects.pendientes_de_revision())
        self.assertNotIn(descartado, Problema.objects.pendientes_de_revision())

    def test_el_estudiante_solo_ve_los_aprobados_y_no_archivados(self):
        curso = self.crear_curso()
        visible = self.crear_problema(curso, generado_por_ia=True)
        visible.registrar_aprobacion(curso.docente)
        self.crear_problema(curso, generado_por_ia=True)  # pendiente
        archivado = self.crear_problema(curso)
        archivado.registrar_aprobacion(curso.docente)
        archivado.archivar()

        self.assertEqual(list(Problema.objects.visibles_para_estudiante()), [visible])


class OpcionesDeRespuestaTests(ProblemasBase):
    def test_las_alternativas_salen_en_el_orden_en_que_se_crearon(self):
        problema = self.crear_problema(self.crear_curso(), OPCION)
        for texto in ["B", "A", "C"]:
            OpcionRespuesta.objects.create(problema=problema, texto=texto)

        self.assertEqual([o.texto for o in problema.opciones.all()], ["B", "A", "C"])

    def test_una_alternativa_nueva_es_incorrecta_por_defecto(self):
        problema = self.crear_problema(self.crear_curso(), OPCION)

        self.assertFalse(OpcionRespuesta.objects.create(problema=problema, texto="A").es_correcta)

    def test_al_borrar_el_problema_se_borran_sus_alternativas(self):
        problema = self.crear_problema(self.crear_curso(), OPCION)
        OpcionRespuesta.objects.create(problema=problema, texto="A")

        problema.delete()

        self.assertEqual(OpcionRespuesta.objects.count(), 0)


class CompletitudTests(ProblemasBase):
    def alternativas(self, *correctas):
        return [{"texto": f"opción {i}", "es_correcta": c} for i, c in enumerate(correctas)]

    def test_escritura_de_codigo_necesita_un_caso(self):
        self.assertEqual(faltante(ESCRITURA, [], [], "")[0], "casos_prueba")
        self.assertIsNone(faltante(ESCRITURA, [{"entrada": "", "salida_esperada": "1"}], [], ""))

    def test_opcion_multiple_necesita_entre_2_y_6_con_una_sola_correcta(self):
        self.assertEqual(faltante(OPCION, [], [], "")[0], "opciones")
        self.assertEqual(faltante(OPCION, [], self.alternativas(True), "")[0], "opciones")
        self.assertEqual(faltante(OPCION, [], self.alternativas(*[False] * 7), "")[0], "opciones")
        self.assertIn("exactamente una", faltante(OPCION, [], self.alternativas(False, False), "")[1])
        self.assertIn("exactamente una", faltante(OPCION, [], self.alternativas(True, True), "")[1])
        self.assertIsNone(faltante(OPCION, [], self.alternativas(True, False), ""))
        self.assertIsNone(faltante(OPCION, [], self.alternativas(True, *[False] * 5), ""))

    def test_una_alternativa_vacia_no_vale(self):
        opciones = [{"texto": "A", "es_correcta": True}, {"texto": "  ", "es_correcta": False}]

        self.assertIn("vacía", faltante(OPCION, [], opciones, "")[1])

    def test_prediccion_y_completar_necesitan_respuesta_esperada(self):
        for tipo in (PREDICCION, COMPLETAR):
            with self.subTest(tipo=tipo):
                self.assertEqual(faltante(tipo, [], [], "")[0], "respuesta_esperada")
                self.assertEqual(faltante(tipo, [], [], "   ")[0], "respuesta_esperada")
                self.assertIsNone(faltante(tipo, [], [], "5"))

    def test_faltante_de_lee_las_relaciones_del_problema(self):
        problema = self.crear_problema(self.crear_curso(), OPCION)
        self.assertEqual(faltante_de(problema)[0], "opciones")

        OpcionRespuesta.objects.create(problema=problema, texto="A", es_correcta=True)
        OpcionRespuesta.objects.create(problema=problema, texto="B")

        self.assertIsNone(faltante_de(problema))


class CreacionManualTests(ProblemasBase):
    def preparar(self):
        curso = self.crear_curso()
        self.autenticar(curso.docente)
        return curso, f"/api/cursos/{curso.pk}/problemas/"

    def cuerpo(self, tipo, **extra):
        plataforma = Problema.Plataforma.WEB if tipo == ESCRITURA else Problema.Plataforma.MOVIL
        return {
            "titulo": "Suma",
            "enunciado": "Suma dos números.",
            "lenguaje": LENGUAJES[0],
            "dificultad": DIFICULTADES[0],
            "plataforma": plataforma,
            "tipo_ejercicio": tipo,
            **extra,
        }

    def test_un_problema_completo_creado_a_mano_nace_aprobado_por_su_autor(self):
        curso, url = self.preparar()
        antes = timezone.now()

        response = self.client.post(
            url, self.cuerpo(ESCRITURA, casos_prueba=[{"entrada": "2 3", "salida_esperada": "5"}]), format="json"
        )

        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data["aprobado_por_docente"])
        self.assertFalse(response.data["generado_por_ia"])
        problema = Problema.objects.get(pk=response.data["id"])
        self.assertEqual(problema.aprobado_por, curso.docente)
        self.assertGreaterEqual(problema.fecha_aprobacion, antes)

    def test_un_borrador_incompleto_se_guarda_pero_sin_aprobar(self):
        _, url = self.preparar()

        response = self.client.post(url, self.cuerpo(OPCION), format="json")

        self.assertEqual(response.status_code, 201)
        self.assertFalse(response.data["aprobado_por_docente"])
        self.assertIsNone(response.data["fecha_aprobacion"])

    def test_opcion_multiple_con_sus_alternativas_se_guarda_aprobada(self):
        _, url = self.preparar()
        alternativas = [{"texto": "4", "es_correcta": False}, {"texto": "5", "es_correcta": True}]

        response = self.client.post(url, self.cuerpo(OPCION, opciones=alternativas), format="json")

        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data["aprobado_por_docente"])
        self.assertEqual([o["texto"] for o in response.data["opciones"]], ["4", "5"])
        self.assertEqual(OpcionRespuesta.objects.filter(problema_id=response.data["id"]).count(), 2)

    def test_prediccion_con_respuesta_esperada_se_guarda_aprobada(self):
        _, url = self.preparar()

        response = self.client.post(url, self.cuerpo(PREDICCION, respuesta_esperada="5"), format="json")

        self.assertEqual(response.status_code, 201)
        self.assertTrue(response.data["aprobado_por_docente"])
        self.assertEqual(Problema.objects.get(pk=response.data["id"]).respuesta_esperada, "5")

    def test_cada_dato_de_respuesta_solo_va_con_su_tipo(self):
        _, url = self.preparar()
        casos = [
            (self.cuerpo(PREDICCION, opciones=[{"texto": "A", "es_correcta": True}]), "opciones"),
            (self.cuerpo(OPCION, respuesta_esperada="5"), "respuesta_esperada"),
            (
                self.cuerpo(ESCRITURA, casos_prueba=[{"entrada": "", "salida_esperada": "1"}], respuesta_esperada="x"),
                "respuesta_esperada",
            ),
        ]
        for cuerpo, campo in casos:
            with self.subTest(campo=campo, tipo=cuerpo["tipo_ejercicio"]):
                response = self.client.post(url, cuerpo, format="json")

                self.assertEqual(response.status_code, 400)
                self.assertIn(campo, response.data)
        self.assertEqual(Problema.objects.count(), 0)

    def test_el_cuerpo_no_puede_aprobarse_a_si_mismo_ni_marcarse_como_de_ia(self):
        _, url = self.preparar()
        cuerpo = self.cuerpo(OPCION, aprobado_por_docente=True, generado_por_ia=True, archivado=True)

        response = self.client.post(url, cuerpo, format="json")

        self.assertEqual(response.status_code, 201)
        problema = Problema.objects.get(pk=response.data["id"])
        self.assertFalse(problema.aprobado_por_docente)
        self.assertFalse(problema.generado_por_ia)
        self.assertFalse(problema.archivado)


class MigracionDeProblemasExistentesTests(ProblemasBase):
    def aprobar_existentes(self):
        migracion = importlib.import_module("problemas.migrations.0008_aprobar_problemas_manuales_existentes")
        migracion.aprobar_existentes(apps, None)

    def test_aprueba_solo_los_de_escritura_con_casos_y_no_toca_el_resto(self):
        curso = self.crear_curso()
        con_casos = self.crear_problema(curso)
        CasoPrueba.objects.create(problema=con_casos, entrada="", salida_esperada="1")
        sin_casos = self.crear_problema(curso)
        movil = self.crear_problema(curso, OPCION)
        de_ia = self.crear_problema(curso, generado_por_ia=True)
        CasoPrueba.objects.create(problema=de_ia, entrada="", salida_esperada="1")
        descartado = self.crear_problema(curso)
        CasoPrueba.objects.create(problema=descartado, entrada="", salida_esperada="1")
        descartado.archivar()

        self.aprobar_existentes()

        con_casos.refresh_from_db()
        self.assertTrue(con_casos.aprobado_por_docente)
        self.assertEqual(con_casos.aprobado_por, curso.docente)
        self.assertEqual(con_casos.fecha_aprobacion, con_casos.fecha_creacion)
        for intacto in (sin_casos, movil, de_ia, descartado):
            intacto.refresh_from_db()
            self.assertFalse(intacto.aprobado_por_docente, intacto.pk)

    def test_un_problema_con_varios_casos_se_aprueba_una_sola_vez(self):
        problema = self.crear_problema(self.crear_curso())
        for salida in ("1", "2", "3"):
            CasoPrueba.objects.create(problema=problema, entrada="", salida_esperada=salida)

        self.aprobar_existentes()
        self.aprobar_existentes()

        problema.refresh_from_db()
        self.assertTrue(problema.aprobado_por_docente)
