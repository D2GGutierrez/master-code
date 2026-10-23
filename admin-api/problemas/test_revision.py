# HU18-T1 (bandeja de pendientes), HU19-T2 (Aprobar, con HU19-T4) y HU20-T1 (Descartar, con HU20-T4):
# la revisión de lo que genera la IA.
from datetime import timedelta

from django.contrib.auth.models import Group, User
from django.db import connection
from django.test.utils import CaptureQueriesContext
from django.utils import timezone

from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.models import CasoPrueba, OpcionRespuesta, Problema
from problemas.serializers import DIFICULTADES

ESCRITURA = Problema.TipoEjercicio.ESCRITURA_CODIGO
PREDICCION = Problema.TipoEjercicio.PREDICCION_SALIDA
OPCION = Problema.TipoEjercicio.OPCION_MULTIPLE

PENDIENTES = "/api/problemas/pendientes/"


class RevisionBase(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.curso = Curso.objects.create(docente=self.docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0])
        self.autenticar(self.docente)

    def crear_problema(self, tipo=ESCRITURA, curso=None, completo=True, **extra):
        # Por defecto, de IA y completo: lo normal en la bandeja.
        extra.setdefault("generado_por_ia", True)
        problema = Problema.objects.create(
            curso=curso or self.curso,
            titulo="Suma",
            enunciado="Suma dos números.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.WEB if tipo == ESCRITURA else Problema.Plataforma.MOVIL,
            tipo_ejercicio=tipo,
            respuesta_esperada="5" if completo and tipo == PREDICCION else "",
            **extra,
        )
        if completo and tipo == ESCRITURA:
            CasoPrueba.objects.create(problema=problema, entrada="2 3", salida_esperada="5", es_publico=True)
        if completo and tipo == OPCION:
            OpcionRespuesta.objects.create(problema=problema, texto="4", es_correcta=False)
            OpcionRespuesta.objects.create(problema=problema, texto="5", es_correcta=True)
        return problema

    def ids_pendientes(self, url=PENDIENTES):
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        return [p["id"] for p in response.data]

    def autenticar_estudiante(self):
        grupo, _ = Group.objects.get_or_create(name="estudiante")
        user = User.objects.create_user(username="e@ejemplo.edu.pe", email="e@ejemplo.edu.pe", password="x", is_active=True)
        user.groups.add(grupo)
        _, access = crear_tokens(user, "admin-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")


class PendientesTests(RevisionBase):
    def test_trae_solo_lo_generado_por_ia_sin_aprobar_ni_descartar(self):
        pendiente = self.crear_problema()
        aprobado = self.crear_problema()
        aprobado.registrar_aprobacion(self.docente)
        descartado = self.crear_problema()
        descartado.archivar()
        self.crear_problema(generado_por_ia=False)  # un borrador manual no es de la bandeja

        self.assertEqual(self.ids_pendientes(), [pendiente.pk])
        self.assertNotIn(aprobado.pk, self.ids_pendientes())
        self.assertNotIn(descartado.pk, self.ids_pendientes())

    def test_trae_solo_los_problemas_de_los_cursos_del_docente(self):
        propio = self.crear_problema()
        otro_curso = Curso.objects.create(docente=self.docente, nombre="Estructuras", lenguaje=LENGUAJES[0])
        tambien_propio = self.crear_problema(curso=otro_curso)
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="Ajeno", lenguaje=LENGUAJES[0])
        self.crear_problema(curso=ajeno)

        self.assertCountEqual(self.ids_pendientes(), [propio.pk, tambien_propio.pk])

    def test_cada_problema_trae_la_marca_de_ia_el_curso_y_lo_que_hay_que_revisar(self):
        problema = self.crear_problema(OPCION)

        item = self.client.get(PENDIENTES).data[0]

        self.assertEqual(item["id"], problema.pk)
        self.assertTrue(item["generado_por_ia"])
        self.assertFalse(item["aprobado_por_docente"])
        self.assertEqual(item["curso"], self.curso.pk)
        self.assertEqual(item["curso_nombre"], "Algoritmos I")
        self.assertEqual(item["tipo_ejercicio"], "opcion_multiple")
        self.assertEqual([o["texto"] for o in item["opciones"]], ["4", "5"])

    def test_un_problema_de_escritura_trae_sus_casos_y_uno_de_prediccion_su_respuesta(self):
        self.crear_problema(ESCRITURA)
        self.crear_problema(PREDICCION)

        por_tipo = {p["tipo_ejercicio"]: p for p in self.client.get(PENDIENTES).data}

        self.assertEqual(por_tipo["escritura_codigo"]["casos_prueba"][0]["salida_esperada"], "5")
        self.assertEqual(por_tipo["prediccion_salida"]["respuesta_esperada"], "5")

    def test_los_mas_nuevos_van_primero(self):
        viejo = self.crear_problema()
        nuevo = self.crear_problema()
        Problema.objects.filter(pk=viejo.pk).update(fecha_creacion=timezone.now() - timedelta(days=2))

        self.assertEqual(self.ids_pendientes(), [nuevo.pk, viejo.pk])

    def test_sin_pendientes_es_una_lista_vacia(self):
        self.assertEqual(self.client.get(PENDIENTES).data, [])

    def test_se_puede_filtrar_por_curso(self):
        otro = Curso.objects.create(docente=self.docente, nombre="Estructuras", lenguaje=LENGUAJES[0])
        en_primero = self.crear_problema()
        en_otro = self.crear_problema(curso=otro)

        self.assertEqual(self.ids_pendientes(f"{PENDIENTES}?curso={self.curso.pk}"), [en_primero.pk])
        self.assertEqual(self.ids_pendientes(f"{PENDIENTES}?curso={otro.pk}"), [en_otro.pk])

    def test_filtrar_por_un_curso_ajeno_o_inexistente_da_lista_vacia(self):
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="Ajeno", lenguaje=LENGUAJES[0])
        self.crear_problema(curso=ajeno)
        self.crear_problema()

        self.assertEqual(self.ids_pendientes(f"{PENDIENTES}?curso={ajeno.pk}"), [])
        self.assertEqual(self.ids_pendientes(f"{PENDIENTES}?curso=999999"), [])

    def test_un_curso_que_no_es_un_numero_da_400(self):
        for valor in ("abc", "", "-1", "1.5"):
            with self.subTest(valor=valor):
                response = self.client.get(f"{PENDIENTES}?curso={valor}")

                self.assertEqual(response.status_code, 400)
                self.assertIn("curso", response.data)

    def test_las_consultas_no_crecen_con_la_cantidad_de_problemas(self):
        self.crear_problema(OPCION)
        with CaptureQueriesContext(connection) as con_uno:
            self.client.get(PENDIENTES)
        for _ in range(5):
            self.crear_problema(OPCION)
            self.crear_problema(ESCRITURA)

        with CaptureQueriesContext(connection) as con_once:
            self.assertEqual(len(self.client.get(PENDIENTES).data), 11)

        self.assertEqual(len(con_once), len(con_uno))

    def test_sin_sesion_da_401_y_un_estudiante_da_403(self):
        self.client.credentials()
        self.assertEqual(self.client.get(PENDIENTES).status_code, 401)

        self.autenticar_estudiante()
        self.assertEqual(self.client.get(PENDIENTES).status_code, 403)


class AprobarTests(RevisionBase):
    def url(self, problema):
        return f"/api/problemas/{problema.pk}/aprobar/"

    def test_aprobar_marca_quien_y_cuando_y_lo_deja_visible_para_el_estudiante(self):
        problema = self.crear_problema()
        antes = timezone.now()

        response = self.client.post(self.url(problema))

        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data["aprobado_por_docente"])
        problema.refresh_from_db()
        self.assertTrue(problema.aprobado_por_docente)
        self.assertEqual(problema.aprobado_por, self.docente)
        self.assertGreaterEqual(problema.fecha_aprobacion, antes)
        self.assertIn(problema, Problema.objects.visibles_para_estudiante())

    def test_un_problema_aprobado_sale_de_la_bandeja(self):
        problema = self.crear_problema()
        otro = self.crear_problema()
        self.assertCountEqual(self.ids_pendientes(), [problema.pk, otro.pk])

        self.client.post(self.url(problema))

        self.assertEqual(self.ids_pendientes(), [otro.pk])

    def test_aprobar_uno_no_aprueba_los_demas(self):
        problema = self.crear_problema()
        otro = self.crear_problema()

        self.client.post(self.url(problema))

        otro.refresh_from_db()
        self.assertFalse(otro.aprobado_por_docente)
        self.assertNotIn(otro, Problema.objects.visibles_para_estudiante())

    def test_un_problema_incompleto_no_se_aprueba_y_el_error_va_en_el_campo_que_falta(self):
        casos = {
            "escritura sin casos de prueba": (self.crear_problema(ESCRITURA, completo=False), "casos_prueba"),
            "opción múltiple sin alternativas": (self.crear_problema(OPCION, completo=False), "opciones"),
            "predicción sin respuesta esperada": (self.crear_problema(PREDICCION, completo=False), "respuesta_esperada"),
        }
        for nombre, (problema, campo) in casos.items():
            with self.subTest(nombre):
                response = self.client.post(self.url(problema))

                self.assertEqual(response.status_code, 400)
                self.assertIn(campo, response.data)
                self.assertTrue(str(response.data[campo][0]).strip())
                problema.refresh_from_db()
                self.assertFalse(problema.aprobado_por_docente)
                self.assertIsNone(problema.fecha_aprobacion)
                self.assertIn(problema.pk, self.ids_pendientes())

    def test_una_opcion_multiple_sin_alternativa_correcta_no_se_aprueba(self):
        problema = self.crear_problema(OPCION, completo=False)
        OpcionRespuesta.objects.create(problema=problema, texto="4")
        OpcionRespuesta.objects.create(problema=problema, texto="5")

        response = self.client.post(self.url(problema))

        self.assertEqual(response.status_code, 400)
        self.assertIn("exactamente una", response.data["opciones"][0])

    def test_aprobar_dos_veces_no_cambia_quien_ni_cuando(self):
        problema = self.crear_problema()
        self.client.post(self.url(problema))
        problema.refresh_from_db()
        fecha = problema.fecha_aprobacion

        response = self.client.post(self.url(problema))

        self.assertEqual(response.status_code, 200)
        problema.refresh_from_db()
        self.assertEqual(problema.fecha_aprobacion, fecha)

    def test_un_problema_descartado_no_se_aprueba(self):
        problema = self.crear_problema()
        problema.archivar()

        response = self.client.post(self.url(problema))

        self.assertEqual(response.status_code, 409)
        problema.refresh_from_db()
        self.assertFalse(problema.aprobado_por_docente)

    def test_el_problema_de_otro_docente_da_404_y_no_cambia(self):
        ajeno_curso = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="Ajeno", lenguaje=LENGUAJES[0])
        ajeno = self.crear_problema(curso=ajeno_curso)

        response = self.client.post(self.url(ajeno))

        self.assertEqual(response.status_code, 404)
        ajeno.refresh_from_db()
        self.assertFalse(ajeno.aprobado_por_docente)

    def test_un_problema_inexistente_da_404(self):
        self.assertEqual(self.client.post("/api/problemas/999999/aprobar/").status_code, 404)

    def test_sin_sesion_401_estudiante_403_y_solo_acepta_post(self):
        problema = self.crear_problema()
        self.assertEqual(self.client.get(self.url(problema)).status_code, 405)

        self.autenticar_estudiante()
        self.assertEqual(self.client.post(self.url(problema)).status_code, 403)

        self.client.credentials()
        self.assertEqual(self.client.post(self.url(problema)).status_code, 401)
        problema.refresh_from_db()
        self.assertFalse(problema.aprobado_por_docente)


class DescartarTests(RevisionBase):
    def url(self, problema):
        return f"/api/problemas/{problema.pk}/descartar/"

    def descartar(self, problema, **cuerpo):
        return self.client.post(self.url(problema), cuerpo, format="json")

    def test_sin_confirmar_no_descarta_nada(self):
        problema = self.crear_problema()
        sin_confirmar = [{}, {"confirmar": False}, {"confirmar": "true"}, {"confirmar": 1}, {"confirmar": None}]
        for cuerpo in sin_confirmar:
            with self.subTest(cuerpo=cuerpo):
                response = self.descartar(problema, **cuerpo)

                self.assertEqual(response.status_code, 400)
                self.assertIn("confirmar", response.data)
                problema.refresh_from_db()
                self.assertFalse(problema.archivado)
        self.assertIn(problema.pk, self.ids_pendientes())

    def test_un_cuerpo_que_no_es_un_objeto_tampoco_descarta(self):
        problema = self.crear_problema()

        response = self.client.post(self.url(problema), [True], format="json")

        self.assertEqual(response.status_code, 400)
        problema.refresh_from_db()
        self.assertFalse(problema.archivado)

    def test_confirmado_lo_archiva_y_el_problema_sigue_en_la_base(self):
        problema = self.crear_problema()

        response = self.descartar(problema, confirmar=True)

        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data["archivado"])
        self.assertTrue(Problema.objects.filter(pk=problema.pk).exists())
        problema.refresh_from_db()
        self.assertTrue(problema.archivado)

    def test_al_descartar_se_conservan_sus_casos_y_alternativas(self):
        escritura = self.crear_problema(ESCRITURA)
        opcion = self.crear_problema(OPCION)

        self.descartar(escritura, confirmar=True)
        self.descartar(opcion, confirmar=True)

        self.assertEqual(escritura.casos_prueba.count(), 1)
        self.assertEqual(opcion.opciones.count(), 2)

    def test_un_descartado_no_vuelve_a_la_bandeja_ni_al_catalogo_del_estudiante(self):
        problema = self.crear_problema()
        otro = self.crear_problema()

        self.descartar(problema, confirmar=True)

        self.assertEqual(self.ids_pendientes(), [otro.pk])
        self.assertNotIn(problema, Problema.objects.visibles_para_estudiante())
        self.assertNotIn(problema, Problema.objects.pendientes_de_revision())

    def test_descartar_dos_veces_no_es_un_error(self):
        problema = self.crear_problema()
        self.descartar(problema, confirmar=True)

        self.assertEqual(self.descartar(problema, confirmar=True).status_code, 200)

    def test_un_problema_ya_aprobado_no_se_descarta(self):
        problema = self.crear_problema()
        problema.registrar_aprobacion(self.docente)

        response = self.descartar(problema, confirmar=True)

        self.assertEqual(response.status_code, 409)
        problema.refresh_from_db()
        self.assertFalse(problema.archivado)
        self.assertIn(problema, Problema.objects.visibles_para_estudiante())

    def test_el_problema_de_otro_docente_da_404_aunque_falte_la_confirmacion(self):
        ajeno_curso = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="Ajeno", lenguaje=LENGUAJES[0])
        ajeno = self.crear_problema(curso=ajeno_curso)

        self.assertEqual(self.descartar(ajeno).status_code, 404)
        self.assertEqual(self.descartar(ajeno, confirmar=True).status_code, 404)

        ajeno.refresh_from_db()
        self.assertFalse(ajeno.archivado)

    def test_un_problema_inexistente_da_404(self):
        response = self.client.post("/api/problemas/999999/descartar/", {"confirmar": True}, format="json")

        self.assertEqual(response.status_code, 404)

    def test_sin_sesion_401_estudiante_403_y_solo_acepta_post(self):
        problema = self.crear_problema()
        self.assertEqual(self.client.get(self.url(problema)).status_code, 405)

        self.autenticar_estudiante()
        self.assertEqual(self.descartar(problema, confirmar=True).status_code, 403)

        self.client.credentials()
        self.assertEqual(self.descartar(problema, confirmar=True).status_code, 401)
        problema.refresh_from_db()
        self.assertFalse(problema.archivado)
