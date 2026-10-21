# CA-012.4: los casos ocultos no se envían al estudiante. CA-012.2: cada caso es público u oculto.
import json

from django.conf import settings
from django.contrib.auth.models import User
from django.test import TestCase
from rest_framework.test import APIClient

from cuentas.models import Docente
from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.serializers import LENGUAJES
from problemas.models import CasoPrueba, Problema
from problemas.serializers import (
    DIFICULTADES,
    ProblemaEstudianteSerializer,
    ProblemaSerializer,
)

CONTRASENA = "Clave-Falsa-2026"
# Centinelas fáciles de buscar en el texto completo del JSON.
ENTRADA_PUBLICA = "ENTRADA_PUBLICA_XYZ"
SALIDA_PUBLICA = "SALIDA_PUBLICA_XYZ"
ENTRADA_OCULTA = "ENTRADA_OCULTA_XYZ"
SALIDA_OCULTA = "SALIDA_OCULTA_XYZ"


class CasosOcultosNoSeExponenAlEstudianteTests(TestCase):
    def setUp(self):
        self.client = APIClient()

    # Copiados de cursos/tests.py: importar su clase haría correr sus pruebas dos veces.
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

    def crear_curso(self):
        return Curso.objects.create(
            docente=self.crear_docente("docente-a"),
            nombre="Algoritmos I",
            lenguaje=LENGUAJES[0],
        )

    def crear_problema(self, con_publico=True):
        problema = Problema.objects.create(
            curso=self.crear_curso(),
            titulo="Suma de dos números",
            enunciado="Lee dos enteros e imprime su suma.",
            lenguaje=LENGUAJES[0],
            dificultad=DIFICULTADES[0],
            plataforma=Problema.Plataforma.WEB,
            tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
        )
        if con_publico:
            CasoPrueba.objects.create(
                problema=problema,
                entrada=ENTRADA_PUBLICA,
                salida_esperada=SALIDA_PUBLICA,
                es_publico=True,
            )
        CasoPrueba.objects.create(
            problema=problema,
            entrada=ENTRADA_OCULTA,
            salida_esperada=SALIDA_OCULTA,
            es_publico=False,
        )
        return problema

    def vista_estudiante(self, problema):
        return ProblemaEstudianteSerializer(problema).data

    def test_el_estudiante_ve_el_caso_publico_con_su_entrada_y_salida(self):
        datos = self.vista_estudiante(self.crear_problema())

        self.assertEqual(
            datos["casos_prueba"],
            [{"entrada": ENTRADA_PUBLICA, "salida_esperada": SALIDA_PUBLICA}],
        )

    def test_el_caso_oculto_no_aparece_en_ninguna_parte_del_json(self):
        # Texto completo y no una clave: cubre campos anidados y listas.
        texto = json.dumps(self.vista_estudiante(self.crear_problema()))

        self.assertNotIn(ENTRADA_OCULTA, texto)
        self.assertNotIn(SALIDA_OCULTA, texto)

    def test_problema_con_todos_los_casos_ocultos_sale_con_lista_vacia(self):
        datos = self.vista_estudiante(self.crear_problema(con_publico=False))

        self.assertEqual(datos["casos_prueba"], [])
        self.assertEqual(datos["titulo"], "Suma de dos números")

    def test_no_revela_es_publico_ni_cuantos_casos_ocultos_hay(self):
        datos = self.vista_estudiante(self.crear_problema())

        self.assertNotIn("es_publico", json.dumps(datos))
        # Solo el público: el tamaño de la lista no delata que existe un oculto.
        self.assertEqual(len(datos["casos_prueba"]), 1)

    def test_control_el_serializer_del_docente_si_incluye_el_caso_oculto(self):
        texto = json.dumps(ProblemaSerializer(self.crear_problema()).data)

        self.assertIn(ENTRADA_OCULTA, texto)
        self.assertIn(SALIDA_OCULTA, texto)

    def test_control_la_respuesta_de_creacion_al_docente_trae_ambos_casos(self):
        curso = self.crear_curso()
        self.autenticar(curso.docente)
        datos = {
            "titulo": "Suma de dos números",
            "enunciado": "Lee dos enteros e imprime su suma.",
            "lenguaje": LENGUAJES[0],
            "dificultad": DIFICULTADES[0],
            "plataforma": Problema.Plataforma.WEB,
            "tipo_ejercicio": Problema.TipoEjercicio.ESCRITURA_CODIGO,
            "casos_prueba": [
                {
                    "entrada": ENTRADA_PUBLICA,
                    "salida_esperada": SALIDA_PUBLICA,
                    "es_publico": True,
                },
                {
                    "entrada": ENTRADA_OCULTA,
                    "salida_esperada": SALIDA_OCULTA,
                    "es_publico": False,
                },
            ],
        }

        response = self.client.post(
            f"/api/cursos/{curso.pk}/problemas/", datos, format="json"
        )

        self.assertEqual(response.status_code, 201)
        casos = {
            (caso["entrada"], caso["es_publico"])
            for caso in response.data["casos_prueba"]
        }
        self.assertEqual(casos, {(ENTRADA_PUBLICA, True), (ENTRADA_OCULTA, False)})
