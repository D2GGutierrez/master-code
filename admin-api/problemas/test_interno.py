# D3 + HU31: la API interna de problemas que consume user-api. Dos cosas se prueban a fondo:
# que sin la clave interna no entra nadie y que ninguna ruta entrega un problema sin aprobar (ADR-2).
from django.test import override_settings
from django.utils.dateparse import parse_datetime

from cursos.models import Curso
from cursos.serializers import LENGUAJES
from cursos.tests import CursosTestCase
from problemas.models import CasoPrueba, OpcionRespuesta, Problema
from problemas.serializers import DIFICULTADES

CLAVE = "clave-interna-solo-para-pruebas"
ESCRITURA = Problema.TipoEjercicio.ESCRITURA_CODIGO
PREDICCION = Problema.TipoEjercicio.PREDICCION_SALIDA
OPCION = Problema.TipoEjercicio.OPCION_MULTIPLE

# Centinelas fáciles de buscar en el texto completo de la respuesta.
ENTRADA_OCULTA = "ENTRADA_OCULTA_XYZ"
SALIDA_OCULTA = "SALIDA_OCULTA_XYZ"
RESPUESTA_SECRETA = "RESPUESTA_SECRETA_XYZ"
ALTERNATIVA_CORRECTA = "ALTERNATIVA_CORRECTA_XYZ"


@override_settings(INTERNAL_API_KEY=CLAVE)
class InternoBase(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.curso = Curso.objects.create(docente=self.docente, nombre="Algoritmos I", lenguaje=LENGUAJES[0])

    def crear_problema(self, curso=None, tipo=ESCRITURA, plataforma=None, estado="aprobado", **extra):
        # estado: "aprobado" (visible), "pendiente" (IA sin revisar), "borrador" (manual incompleto, sin aprobar)
        # o "archivado" (aprobado y luego descartado de la vista).
        extra.setdefault("generado_por_ia", estado == "pendiente")
        problema = Problema.objects.create(
            curso=curso or self.curso,
            titulo=extra.pop("titulo", "Suma"),
            enunciado="Suma dos números.",
            lenguaje=LENGUAJES[0],
            dificultad=extra.pop("dificultad", DIFICULTADES[0]),
            plataforma=plataforma or (Problema.Plataforma.WEB if tipo == ESCRITURA else Problema.Plataforma.MOVIL),
            tipo_ejercicio=tipo,
            **extra,
        )
        if estado in ("aprobado", "archivado"):
            problema.registrar_aprobacion(self.docente)
        if estado == "archivado":
            problema.archivar()
        return problema

    def pedir(self, url, clave=CLAVE):
        cabeceras = {"HTTP_X_INTERNAL_KEY": clave} if clave is not None else {}
        return self.client.get(url, **cabeceras)


class ClaveInternaTests(InternoBase):
    def rutas(self):
        problema = self.crear_problema()
        return [f"/internal/cursos/{self.curso.pk}/problemas/", f"/internal/problemas/{problema.pk}/"]

    def test_sin_clave_se_rechaza(self):
        for url in self.rutas():
            with self.subTest(url=url):
                self.assertEqual(self.pedir(url, clave=None).status_code, 403)

    def test_con_clave_equivocada_se_rechaza(self):
        for url in self.rutas():
            with self.subTest(url=url):
                self.assertEqual(self.pedir(url, clave="otra").status_code, 403)

    @override_settings(INTERNAL_API_KEY="")
    def test_si_el_servidor_no_tiene_clave_configurada_se_rechaza_todo(self):
        for url in self.rutas():
            with self.subTest(url=url):
                self.assertEqual(self.pedir(url, clave="").status_code, 403)

    def test_un_token_de_docente_no_reemplaza_la_clave(self):
        self.autenticar(self.docente)
        for url in self.rutas():
            with self.subTest(url=url):
                self.assertEqual(self.pedir(url, clave=None).status_code, 403)


class ProblemasDelCursoTests(InternoBase):
    def ids(self, url=None):
        respuesta = self.pedir(url or f"/internal/cursos/{self.curso.pk}/problemas/")
        self.assertEqual(respuesta.status_code, 200)
        return [fila["id"] for fila in respuesta.json()]

    def test_trae_solo_los_aprobados(self):
        aprobado = self.crear_problema()
        self.crear_problema(estado="pendiente")
        self.crear_problema(estado="borrador")

        self.assertEqual(self.ids(), [aprobado.pk])

    def test_un_aprobado_que_se_archivo_no_sale(self):
        visible = self.crear_problema()
        self.crear_problema(estado="archivado")

        self.assertEqual(self.ids(), [visible.pk])

    def test_un_problema_aprobado_de_ia_si_sale(self):
        # La IA nunca publica sola, pero lo que el docente aprobó se ve igual que lo escrito a mano.
        problema = self.crear_problema(generado_por_ia=True)

        self.assertEqual(self.ids(), [problema.pk])

    def test_no_mezcla_problemas_de_otro_curso(self):
        otro = Curso.objects.create(docente=self.docente, nombre="Otro", lenguaje=LENGUAJES[0])
        propio = self.crear_problema()
        self.crear_problema(curso=otro)

        self.assertEqual(self.ids(), [propio.pk])

    def test_un_curso_sin_aprobados_da_lista_vacia(self):
        self.crear_problema(estado="pendiente")

        respuesta = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), [])

    def test_un_curso_inexistente_da_404(self):
        self.assertEqual(self.pedir("/internal/cursos/999999/problemas/").status_code, 404)

    def test_cada_fila_trae_lo_que_pide_el_listado_y_nada_de_la_respuesta(self):
        problema = self.crear_problema(titulo="Suma de dos", dificultad="medio")
        CasoPrueba.objects.create(problema=problema, entrada=ENTRADA_OCULTA, salida_esperada=SALIDA_OCULTA)

        respuesta = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/")

        fila = respuesta.json()[0]
        self.assertEqual(
            set(fila),
            {"id", "titulo", "lenguaje", "dificultad", "plataforma", "tipo_ejercicio", "fecha_publicacion"},
        )
        self.assertEqual(fila["titulo"], "Suma de dos")
        self.assertEqual(fila["dificultad"], "medio")
        self.assertEqual(fila["tipo_ejercicio"], ESCRITURA)
        self.assertNotIn("XYZ", respuesta.content.decode())

    def test_la_fecha_de_publicacion_es_la_de_aprobacion(self):
        problema = self.crear_problema()
        problema.refresh_from_db()

        fila = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/").json()[0]

        # DRF escribe la fecha en la zona horaria del proyecto: se compara el instante, no el texto.
        self.assertEqual(parse_datetime(fila["fecha_publicacion"]), problema.fecha_aprobacion)

    def test_sale_en_orden_de_creacion(self):
        primero = self.crear_problema(titulo="B")
        segundo = self.crear_problema(titulo="A")

        self.assertEqual(self.ids(), [primero.pk, segundo.pk])

    # --- ?plataforma ---
    def preparar_plataformas(self):
        web = self.crear_problema(plataforma=Problema.Plataforma.WEB)
        movil = self.crear_problema(tipo=PREDICCION, plataforma=Problema.Plataforma.MOVIL)
        ambas = self.crear_problema(plataforma=Problema.Plataforma.AMBAS)
        return web, movil, ambas

    def test_sin_filtro_salen_todas_las_plataformas(self):
        web, movil, ambas = self.preparar_plataformas()

        self.assertEqual(self.ids(), [web.pk, movil.pk, ambas.pk])

    def test_plataforma_web_deja_web_y_ambas(self):
        web, _, ambas = self.preparar_plataformas()

        self.assertEqual(self.ids(f"/internal/cursos/{self.curso.pk}/problemas/?plataforma=web"), [web.pk, ambas.pk])

    def test_plataforma_movil_deja_movil_y_ambas(self):
        _, movil, ambas = self.preparar_plataformas()

        self.assertEqual(self.ids(f"/internal/cursos/{self.curso.pk}/problemas/?plataforma=movil"), [movil.pk, ambas.pk])

    def test_el_filtro_de_plataforma_no_deja_pasar_lo_no_aprobado(self):
        visible = self.crear_problema(plataforma=Problema.Plataforma.AMBAS)
        self.crear_problema(plataforma=Problema.Plataforma.AMBAS, estado="pendiente")

        self.assertEqual(self.ids(f"/internal/cursos/{self.curso.pk}/problemas/?plataforma=web"), [visible.pk])

    def test_una_plataforma_que_no_existe_da_400(self):
        for valor in ("ambas", "ios", "WEB"):
            with self.subTest(valor=valor):
                respuesta = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/?plataforma={valor}")
                self.assertEqual(respuesta.status_code, 400)


class ProblemaPorIdTests(InternoBase):
    def test_un_problema_aprobado_se_entrega_con_lo_del_estudiante_y_su_curso(self):
        problema = self.crear_problema(titulo="Suma de dos", dificultad="facil")
        CasoPrueba.objects.create(problema=problema, entrada="2 3", salida_esperada="5", es_publico=True)

        respuesta = self.pedir(f"/internal/problemas/{problema.pk}/")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(
            respuesta.json(),
            {
                "id": problema.pk,
                "titulo": "Suma de dos",
                "enunciado": "Suma dos números.",
                "lenguaje": LENGUAJES[0],
                "dificultad": "facil",
                "plataforma": "web",
                "tipo_ejercicio": ESCRITURA,
                "casos_prueba": [{"entrada": "2 3", "salida_esperada": "5"}],
                "curso_id": self.curso.pk,
            },
        )

    def test_los_casos_ocultos_no_viajan(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="2 3", salida_esperada="5", es_publico=True)
        CasoPrueba.objects.create(problema=problema, entrada=ENTRADA_OCULTA, salida_esperada=SALIDA_OCULTA)

        respuesta = self.pedir(f"/internal/problemas/{problema.pk}/")

        self.assertEqual(len(respuesta.json()["casos_prueba"]), 1)
        texto = respuesta.content.decode()
        self.assertNotIn(ENTRADA_OCULTA, texto)
        self.assertNotIn(SALIDA_OCULTA, texto)

    def test_la_respuesta_esperada_y_la_alternativa_correcta_no_viajan(self):
        prediccion = self.crear_problema(tipo=PREDICCION, respuesta_esperada=RESPUESTA_SECRETA)
        opcion = self.crear_problema(tipo=OPCION)
        OpcionRespuesta.objects.create(problema=opcion, texto=ALTERNATIVA_CORRECTA, es_correcta=True)

        for problema in (prediccion, opcion):
            with self.subTest(tipo=problema.tipo_ejercicio):
                texto = self.pedir(f"/internal/problemas/{problema.pk}/").content.decode()
                self.assertNotIn(RESPUESTA_SECRETA, texto)
                self.assertNotIn(ALTERNATIVA_CORRECTA, texto)

    def test_no_salen_los_datos_internos_del_problema(self):
        problema = self.crear_problema(generado_por_ia=True)

        cuerpo = self.pedir(f"/internal/problemas/{problema.pk}/").json()

        for campo in ("aprobado_por_docente", "aprobado_por", "generado_por_ia", "archivado", "fecha_aprobacion", "fecha_creacion"):
            self.assertNotIn(campo, cuerpo)

    def test_un_problema_sin_aprobar_da_404(self):
        for estado in ("pendiente", "borrador"):
            with self.subTest(estado=estado):
                problema = self.crear_problema(estado=estado)
                self.assertEqual(self.pedir(f"/internal/problemas/{problema.pk}/").status_code, 404)

    def test_un_problema_archivado_da_404(self):
        problema = self.crear_problema(estado="archivado")

        self.assertEqual(self.pedir(f"/internal/problemas/{problema.pk}/").status_code, 404)

    def test_un_problema_inexistente_da_404(self):
        self.assertEqual(self.pedir("/internal/problemas/999999/").status_code, 404)

    def test_sin_aprobar_e_inexistente_responden_igual(self):
        # Si el cuerpo fuera distinto, adivinar ids revelaría cuáles existen sin aprobar.
        pendiente = self.crear_problema(estado="pendiente")

        sin_aprobar = self.pedir(f"/internal/problemas/{pendiente.pk}/")
        inexistente = self.pedir("/internal/problemas/999999/")

        self.assertEqual(sin_aprobar.content, inexistente.content)

    def test_aprobar_un_pendiente_lo_hace_visible_y_descartarlo_lo_oculta(self):
        problema = self.crear_problema(estado="pendiente")
        url = f"/internal/problemas/{problema.pk}/"
        self.assertEqual(self.pedir(url).status_code, 404)

        problema.registrar_aprobacion(self.docente)
        self.assertEqual(self.pedir(url).status_code, 200)

        problema.archivar()
        self.assertEqual(self.pedir(url).status_code, 404)
