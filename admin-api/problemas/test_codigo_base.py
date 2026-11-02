# HU34-T1 (CA-34.1): el problema guarda y devuelve su código base («starter code»), y le llega al estudiante.
from problemas.models import CasoPrueba, Problema
from problemas.test_interno import InternoBase

BASE = "def suma(a, b):\n    # tu código aquí\n"


class CodigoBaseTests(InternoBase):
    def datos(self, **cambios):
        datos = {
            "titulo": "Suma",
            "enunciado": "Suma dos números.",
            "lenguaje": self.curso.lenguaje,
            "dificultad": "facil",
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
            "casos_prueba": [{"entrada": "2 3", "salida_esperada": "5", "es_publico": True}],
        }
        datos.update(cambios)
        return datos

    def test_un_problema_nuevo_nace_sin_codigo_base(self):
        self.assertEqual(self.crear_problema().codigo_base, "")

    def test_el_docente_crea_un_problema_con_codigo_base_y_lo_recibe_de_vuelta(self):
        self.autenticar(self.docente)

        response = self.client.post(
            f"/api/cursos/{self.curso.pk}/problemas/", self.datos(codigo_base=BASE), format="json"
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data["codigo_base"], BASE)
        self.assertEqual(Problema.objects.get(pk=response.data["id"]).codigo_base, BASE)

    def test_sin_codigo_base_en_el_cuerpo_queda_vacio(self):
        self.autenticar(self.docente)

        response = self.client.post(f"/api/cursos/{self.curso.pk}/problemas/", self.datos(), format="json")

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data["codigo_base"], "")

    def test_el_docente_edita_el_codigo_base_y_un_put_sin_el_lo_vacia(self):
        problema = self.crear_problema(generado_por_ia=True, estado="pendiente", codigo_base=BASE)
        CasoPrueba.objects.create(problema=problema, entrada="2 3", salida_esperada="5", es_publico=True)
        self.autenticar(self.docente)

        nuevo = self.client.put(f"/api/problemas/{problema.pk}/", self.datos(codigo_base="x = 1\n"), format="json")
        self.assertEqual(nuevo.status_code, 200)
        self.assertEqual(nuevo.data["codigo_base"], "x = 1\n")

        # PUT es reemplazo completo (HU19-T1): lo que no viene queda vacío.
        sin = self.client.put(f"/api/problemas/{problema.pk}/", self.datos(), format="json")
        self.assertEqual(sin.status_code, 200)
        self.assertEqual(sin.data["codigo_base"], "")

    def test_el_estudiante_recibe_el_codigo_base_por_la_ruta_interna(self):
        problema = self.crear_problema(codigo_base=BASE)

        cuerpo = self.pedir(f"/internal/problemas/{problema.pk}/").json()

        self.assertEqual(cuerpo["codigo_base"], BASE)

    def test_un_problema_sin_codigo_base_lo_entrega_vacio(self):
        problema = self.crear_problema()

        self.assertEqual(self.pedir(f"/internal/problemas/{problema.pk}/").json()["codigo_base"], "")

    def test_el_codigo_base_no_sale_en_el_listado_ni_en_la_calificacion(self):
        problema = self.crear_problema(codigo_base=BASE)

        listado = self.pedir(f"/internal/cursos/{self.curso.pk}/problemas/")
        calificacion = self.pedir(f"/internal/problemas/{problema.pk}/calificacion/")

        self.assertNotIn("codigo_base", listado.content.decode())
        self.assertNotIn("codigo_base", calificacion.content.decode())
