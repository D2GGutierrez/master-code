# CA-011.1 y CA-011.2: se valida la relación plataforma-tipo y se rechazan las imposibles.
# CA-011.3: escritura de código no se acepta en solo móvil. CA-011.4: el error viaja en "plataforma".
from itertools import product

from django.conf import settings
from django.contrib.auth.models import User
from django.db import IntegrityError, transaction
from django.test import TestCase
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from problemas.models import Plataforma, Problema, TipoEjercicio
from problemas.serializers import DIFICULTADES

CONTRASENA = "Clave-Falsa-2026"

# Las 12 combinaciones salen de las opciones del modelo, no se escriben a mano.
TODAS = list(product(Plataforma.values, TipoEjercicio.values))
# Imposibles según el texto de HU15; todas las demás (incluida "ambas") son válidas.
IMPOSIBLES = {
    (Plataforma.MOVIL, TipoEjercicio.ESCRITURA_CODIGO),
    (Plataforma.WEB, TipoEjercicio.PREDICCION_SALIDA),
    (Plataforma.WEB, TipoEjercicio.OPCION_MULTIPLE),
    (Plataforma.WEB, TipoEjercicio.COMPLETAR_ESPACIO),
}
VALIDAS = [combinacion for combinacion in TODAS if combinacion not in IMPOSIBLES]


class CombinacionesBase(TestCase):
    # Copiados de cursos/tests.py: importar su clase haría correr sus pruebas dos veces.
    def setUp(self):
        self.client = APIClient()

    def crear_docente(self, usuario):
        email = f"{usuario}@{settings.DOMINIO_INSTITUCIONAL}"
        user = User.objects.create_user(
            username=email, email=email, password=CONTRASENA, is_active=True
        )
        return Docente.objects.create(
            user=user,
            nombre=usuario,
            email=email,
            institucion=settings.DOMINIO_INSTITUCIONAL,
        )

    def autenticar(self, docente):
        # Token real, como el que emite el login: la petición pasa por JWTAuthentication.
        _, access = crear_tokens(docente.user, "admin-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

    def crear_curso(self, usuario):
        return Curso.objects.create(
            docente=self.crear_docente(usuario),
            nombre="Algoritmos I",
            lenguaje=LENGUAJES[0],
        )


class CombinacionesPlataformaTipoApiTests(CombinacionesBase):
    def preparar_curso_propio(self, usuario="docente-a"):
        curso = self.crear_curso(usuario)
        self.autenticar(curso.docente)
        return f"/api/cursos/{curso.pk}/problemas/"

    def cuerpo(self, plataforma, tipo):
        return {
            "titulo": "Suma de dos números",
            "enunciado": "Lee dos enteros e imprime su suma.",
            "lenguaje": LENGUAJES[0],
            "dificultad": DIFICULTADES[0],
            "plataforma": plataforma,
            "tipo_ejercicio": tipo,
            # Escritura de código exige al menos un caso (HU16); se manda siempre para que
            # el único motivo posible de rechazo sea la combinación.
            "casos_prueba": [{"entrada": "2 3", "salida_esperada": "5"}],
        }

    def test_las_doce_combinaciones_se_guardan_o_se_rechazan_segun_la_regla(self):
        self.assertEqual(len(TODAS), 12)
        for indice, (plataforma, tipo) in enumerate(TODAS):
            with self.subTest(plataforma=plataforma, tipo=tipo):
                url = self.preparar_curso_propio(f"docente-{indice}")
                antes = Problema.objects.count()

                response = self.client.post(
                    url, self.cuerpo(plataforma, tipo), format="json"
                )

                if (plataforma, tipo) in IMPOSIBLES:
                    self.assertEqual(response.status_code, 400)
                    self.assertIn("plataforma", response.data)
                    self.assertEqual(Problema.objects.count(), antes)
                else:
                    self.assertEqual(response.status_code, 201)
                    problema = Problema.objects.get(pk=response.data["id"])
                    self.assertEqual(problema.plataforma, plataforma)
                    self.assertEqual(problema.tipo_ejercicio, tipo)

    def test_escritura_de_codigo_en_movil_explica_el_motivo(self):
        url = self.preparar_curso_propio()

        response = self.client.post(
            url,
            self.cuerpo(Plataforma.MOVIL, TipoEjercicio.ESCRITURA_CODIGO),
            format="json",
        )

        self.assertEqual(response.status_code, 400)
        mensaje = " ".join(str(error) for error in response.data["plataforma"]).lower()
        self.assertTrue(mensaje.strip())
        self.assertIn("celular", mensaje)
        self.assertIn("código", mensaje)

    def test_sin_plataforma_o_sin_tipo_se_rechaza_como_campo_obligatorio(self):
        # Ninguno tiene valor por defecto: falta uno y el error es "required" en ese campo.
        for campo in ["plataforma", "tipo_ejercicio"]:
            with self.subTest(falta=campo):
                url = self.preparar_curso_propio(f"docente-{campo}")
                datos = self.cuerpo(Plataforma.WEB, TipoEjercicio.ESCRITURA_CODIGO)
                del datos[campo]

                response = self.client.post(url, datos, format="json")

                self.assertEqual(response.status_code, 400)
                self.assertEqual(response.data[campo][0].code, "required")
                self.assertEqual(Problema.objects.count(), 0)

    def test_sin_sesion_no_se_puede_crear(self):
        curso = self.crear_curso("docente-a")

        response = self.client.post(
            f"/api/cursos/{curso.pk}/problemas/",
            self.cuerpo(Plataforma.WEB, TipoEjercicio.ESCRITURA_CODIGO),
            format="json",
        )

        self.assertEqual(response.status_code, 401)
        self.assertEqual(Problema.objects.count(), 0)


# HU15 T2: la misma regla en la base, para lo que no pasa por el serializer (admin, shell, scripts).
class CombinacionesPlataformaTipoBaseDatosTests(CombinacionesBase):
    def crear_problema(self, curso, plataforma, tipo):
        return Problema.objects.create(
            curso=curso,
            titulo="Suma de dos números",
            enunciado="Lee dos enteros e imprime su suma.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=plataforma,
            tipo_ejercicio=tipo,
        )

    def test_la_base_rechaza_las_combinaciones_imposibles(self):
        curso = self.crear_curso("docente-a")
        for plataforma, tipo in sorted(IMPOSIBLES):
            # atomic: el error de la base no rompe la transacción del resto de la prueba.
            with (
                self.subTest(plataforma=plataforma, tipo=tipo),
                self.assertRaises(IntegrityError),
                transaction.atomic(),
            ):
                self.crear_problema(curso, plataforma, tipo)
        self.assertEqual(Problema.objects.count(), 0)

    def test_la_base_acepta_las_combinaciones_validas(self):
        curso = self.crear_curso("docente-a")
        for plataforma, tipo in VALIDAS:
            with self.subTest(plataforma=plataforma, tipo=tipo):
                self.crear_problema(curso, plataforma, tipo)
        self.assertEqual(Problema.objects.count(), len(VALIDAS))
