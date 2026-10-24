# HU19-T1: leer y corregir un problema antes de aprobarlo (GET y PUT /api/problemas/<id>/).
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from problemas.models import CasoPrueba, Problema
from problemas.serializers import DIFICULTADES
from problemas.test_revision import ESCRITURA, PREDICCION, RevisionBase


def url(problema_id):
    return f"/api/problemas/{problema_id}/"


class EdicionDeProblemaTests(RevisionBase):
    def datos_validos(self, **cambios):
        datos = {
            "titulo": "Suma corregida",
            "enunciado": "<p>Lee dos enteros.</p>\n<p>Imprime su suma.</p>",
            "lenguaje": LENGUAJES[0],
            "dificultad": DIFICULTADES[1],
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": ESCRITURA,
            "casos_prueba": [
                {"entrada": "2   3", "salida_esperada": "5", "es_publico": True},
                {"entrada": "10 -4", "salida_esperada": "6", "es_publico": False},
            ],
        }
        datos.update(cambios)
        return datos

    # --- GET ---
    def test_el_docente_lee_su_problema_con_casos_ocultos_y_nombre_del_curso(self):
        problema = self.crear_problema()
        CasoPrueba.objects.create(problema=problema, entrada="1 1", salida_esperada="2", es_publico=False)

        response = self.client.get(url(problema.pk))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["curso_nombre"], "Algoritmos I")
        self.assertTrue(response.data["generado_por_ia"])
        self.assertFalse(response.data["aprobado_por_docente"])
        self.assertEqual([c["es_publico"] for c in response.data["casos_prueba"]], [True, False])
        for campo in ("opciones", "respuesta_esperada", "enunciado", "plataforma", "tipo_ejercicio"):
            self.assertIn(campo, response.data)

    def test_un_problema_de_otro_docente_da_404_igual_que_uno_inexistente(self):
        otro = self.crear_docente("docente-b")
        ajeno = self.crear_problema(curso=Curso.objects.create(docente=otro, nombre="Ajeno", lenguaje=LENGUAJES[0]))

        self.assertEqual(self.client.get(url(ajeno.pk)).status_code, 404)
        self.assertEqual(self.client.get(url(999999)).status_code, 404)

    def test_sin_token_da_401_y_un_estudiante_no_entra(self):
        problema = self.crear_problema()
        self.client.credentials()
        self.assertEqual(self.client.get(url(problema.pk)).status_code, 401)

        self.autenticar_estudiante()
        self.assertEqual(self.client.get(url(problema.pk)).status_code, 403)

    # --- PUT ---
    def test_editar_guarda_los_cambios_y_reemplaza_los_casos(self):
        problema = self.crear_problema()
        anterior = problema.casos_prueba.get()

        response = self.client.put(url(problema.pk), self.datos_validos(), format="json")

        self.assertEqual(response.status_code, 200)
        problema.refresh_from_db()
        self.assertEqual(problema.titulo, "Suma corregida")
        self.assertEqual(problema.enunciado, "<p>Lee dos enteros.</p>\n<p>Imprime su suma.</p>")
        casos = list(problema.casos_prueba.order_by("pk").values_list("entrada", "salida_esperada", "es_publico"))
        # Los espacios internos se conservan; los de los extremos los recorta DRF (trim_whitespace), igual que al crear.
        self.assertEqual(casos, [("2   3", "5", True), ("10 -4", "6", False)])
        self.assertFalse(CasoPrueba.objects.filter(pk=anterior.pk).exists())

    def test_el_cuerpo_no_puede_aprobar_ni_cambiar_el_origen_ni_el_curso(self):
        problema = self.crear_problema()
        otro_curso = Curso.objects.create(docente=self.docente, nombre="Otro", lenguaje=LENGUAJES[0])
        datos = self.datos_validos(
            aprobado_por_docente=True, generado_por_ia=False, fecha_aprobacion="2026-01-01T00:00:00Z", curso=otro_curso.pk
        )

        response = self.client.put(url(problema.pk), datos, format="json")

        self.assertEqual(response.status_code, 200)
        problema.refresh_from_db()
        self.assertFalse(problema.aprobado_por_docente)
        self.assertIsNone(problema.fecha_aprobacion)
        self.assertTrue(problema.generado_por_ia)
        self.assertEqual(problema.curso_id, self.curso.pk)

    def test_una_combinacion_imposible_da_400_en_plataforma_y_no_cambia_nada(self):
        problema = self.crear_problema()

        response = self.client.put(
            url(problema.pk), self.datos_validos(plataforma=Problema.Plataforma.MOVIL), format="json"
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("plataforma", response.data)
        problema.refresh_from_db()
        self.assertEqual(problema.titulo, "Suma")
        self.assertEqual(problema.casos_prueba.count(), 1)

    def test_cambiar_de_tipo_no_deja_la_respuesta_esperada_anterior(self):
        problema = self.crear_problema(tipo=PREDICCION)

        response = self.client.put(url(problema.pk), self.datos_validos(), format="json")

        self.assertEqual(response.status_code, 200)
        problema.refresh_from_db()
        self.assertEqual(problema.respuesta_esperada, "")

    def test_no_se_edita_un_problema_ajeno_y_patch_no_existe(self):
        otro = self.crear_docente("docente-b")
        ajeno = self.crear_problema(curso=Curso.objects.create(docente=otro, nombre="Ajeno", lenguaje=LENGUAJES[0]))

        self.assertEqual(self.client.put(url(ajeno.pk), self.datos_validos(), format="json").status_code, 404)
        ajeno.refresh_from_db()
        self.assertEqual(ajeno.titulo, "Suma")
        propio = self.crear_problema()
        self.assertEqual(self.client.patch(url(propio.pk), {"titulo": "x"}, format="json").status_code, 405)
