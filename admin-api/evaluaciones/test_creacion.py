# HU61-T2 y HU62: POST /api/cursos/<id>/evaluaciones/ crea la evaluación de un curso propio con sus problemas.
from datetime import timedelta

from django.contrib.auth.models import Group, User
from django.utils import timezone

from cuentas.tokens import crear_tokens
from cursos.models import Curso
from cursos.tests import CursosTestCase
from evaluaciones.models import Evaluacion, EvaluacionProblema
from problemas.models import Problema


# Preparación común: un docente con su curso, dos problemas aprobados y un cuerpo válido.
class EvaluacionBase(CursosTestCase):
    def setUp(self):
        super().setUp()
        self.docente = self.crear_docente("docente-a")
        self.curso = Curso.objects.create(docente=self.docente, nombre="Algoritmos I", lenguaje="python")
        self.suma = self.crear_problema("Suma")
        self.ordena = self.crear_problema("Ordena")
        self.autenticar(self.docente)
        inicio = timezone.now() + timedelta(days=1)
        self.cuerpo = {
            "fecha_inicio": inicio.isoformat(),
            "fecha_fin": (inicio + timedelta(hours=2)).isoformat(),
            "limite_minutos": 90,
            "problemas": [self.suma.pk, self.ordena.pk],
        }

    def crear_problema(self, titulo, curso=None, aprobado=True, archivado=False):
        problema = Problema.objects.create(
            curso=curso or self.curso,
            titulo=titulo,
            enunciado="Resuelve.",
            lenguaje="python",
            dificultad="facil",
            plataforma=Problema.Plataforma.WEB,
            tipo_ejercicio=Problema.TipoEjercicio.ESCRITURA_CODIGO,
            archivado=archivado,
        )
        if aprobado:
            problema.registrar_aprobacion(self.docente)
        return problema

    def url(self, curso_id=None):
        return f"/api/cursos/{curso_id or self.curso.pk}/evaluaciones/"

    def publicar(self, **cambios):
        return self.client.post(self.url(), {**self.cuerpo, **cambios}, format="json")


class CrearEvaluacionTests(EvaluacionBase):
    def test_crea_la_evaluacion_del_curso_con_fechas_y_limite(self):
        response = self.publicar()

        self.assertEqual(response.status_code, 201)
        evaluacion = Evaluacion.objects.get()
        self.assertEqual(evaluacion.curso, self.curso)
        self.assertEqual(evaluacion.limite_minutos, 90)
        self.assertEqual(evaluacion.fecha_fin - evaluacion.fecha_inicio, timedelta(hours=2))
        self.assertEqual(response.data["id"], evaluacion.pk)
        self.assertEqual(response.data["curso"], self.curso.pk)
        self.assertEqual(response.data["limite_minutos"], 90)

    def test_la_respuesta_trae_el_estado_calculado(self):
        response = self.publicar()

        self.assertEqual(response.data["estado"], "programada")

    def test_una_ventana_que_ya_empezo_sale_activa(self):
        ahora = timezone.now()

        response = self.publicar(
            fecha_inicio=(ahora - timedelta(hours=1)).isoformat(),
            fecha_fin=(ahora + timedelta(hours=1)).isoformat(),
            limite_minutos=30,
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.data["estado"], "activa")

    def test_cada_campo_obligatorio_faltante_da_400_con_su_error_y_no_guarda(self):
        for campo in ["fecha_inicio", "fecha_fin", "limite_minutos", "problemas"]:
            with self.subTest(campo):
                cuerpo = {k: v for k, v in self.cuerpo.items() if k != campo}

                response = self.client.post(self.url(), cuerpo, format="json")

                self.assertEqual(response.status_code, 400)
                self.assertIn(campo, response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    # CA-061.4: la fecha de fin debe ser posterior a la de inicio.
    def test_un_fin_anterior_al_inicio_da_400_con_mensaje_en_fecha_fin_y_no_guarda(self):
        inicio = timezone.now() + timedelta(days=1)

        response = self.publicar(
            fecha_inicio=inicio.isoformat(), fecha_fin=(inicio - timedelta(hours=1)).isoformat()
        )

        self.assertEqual(response.status_code, 400)
        self.assertEqual(
            [str(error) for error in response.data["fecha_fin"]], ["La fecha de fin debe ser posterior a la de inicio."]
        )
        self.assertEqual(Evaluacion.objects.count(), 0)
        self.assertEqual(EvaluacionProblema.objects.count(), 0)

    def test_un_fin_igual_al_inicio_tambien_da_400(self):
        inicio = timezone.now() + timedelta(days=1)

        response = self.publicar(fecha_inicio=inicio.isoformat(), fecha_fin=inicio.isoformat())

        self.assertEqual(response.status_code, 400)
        self.assertIn("fecha_fin", response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_un_fin_un_segundo_despues_del_inicio_se_acepta(self):
        inicio = timezone.now() + timedelta(days=1)

        response = self.publicar(
            fecha_inicio=inicio.isoformat(), fecha_fin=(inicio + timedelta(seconds=1)).isoformat()
        )

        self.assertEqual(response.status_code, 201)

    def test_fecha_con_formato_invalido_da_400(self):
        response = self.publicar(fecha_inicio="mañana")

        self.assertEqual(response.status_code, 400)
        self.assertIn("fecha_inicio", response.data)

    def test_limite_cero_negativo_o_no_numerico_da_400(self):
        for limite in [0, -5, "mucho"]:
            with self.subTest(limite):
                response = self.publicar(limite_minutos=limite)

                self.assertEqual(response.status_code, 400)
                self.assertIn("limite_minutos", response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_el_curso_sale_de_la_ruta_no_del_cuerpo(self):
        otro = Curso.objects.create(docente=self.docente, nombre="Otro curso", lenguaje="python")

        self.publicar(curso=otro.pk)

        self.assertEqual(Evaluacion.objects.get().curso, self.curso)

    def test_un_curso_ajeno_da_el_mismo_404_que_uno_inexistente_y_no_guarda(self):
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="De B", lenguaje="python")

        de_otro = self.client.post(self.url(ajeno.pk), self.cuerpo, format="json")
        que_no_existe = self.client.post(self.url(ajeno.pk + 100_000), self.cuerpo, format="json")

        self.assertEqual(de_otro.status_code, 404)
        self.assertEqual(que_no_existe.status_code, 404)
        self.assertEqual(de_otro.content, que_no_existe.content)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_sin_token_da_401(self):
        self.client.credentials()

        response = self.client.post(self.url(), self.cuerpo, format="json")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_una_cuenta_que_no_es_docente_da_403_y_no_guarda(self):
        user = User.objects.create_user(
            username="estudiante@ejemplo.edu.pe", email="estudiante@ejemplo.edu.pe", password="Clave-Falsa-2026"
        )
        user.groups.add(Group.objects.get_or_create(name="estudiante")[0])
        _, access = crear_tokens(user, "user-web")
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {access}")

        response = self.client.post(self.url(), self.cuerpo, format="json")

        self.assertEqual(response.status_code, 403)
        self.assertEqual(Evaluacion.objects.count(), 0)


# CA-062.1 y CA-062.2: los problemas elegidos se guardan con la evaluación; CA-062.3: al menos uno.
class ProblemasDeLaEvaluacionTests(EvaluacionBase):
    def test_guarda_los_problemas_elegidos_y_la_respuesta_los_devuelve(self):
        response = self.publicar(problemas=[self.ordena.pk, self.suma.pk])

        self.assertEqual(response.status_code, 201)
        guardados = EvaluacionProblema.objects.filter(evaluacion_id=response.data["id"])
        self.assertEqual({fila.problema_id for fila in guardados}, {self.suma.pk, self.ordena.pk})
        self.assertEqual(response.data["problemas"], [self.ordena.pk, self.suma.pk])

    # HU63-T1: la API todavía no recibe puntajes; cada problema queda con el valor por defecto.
    def test_cada_problema_queda_con_un_punto_por_defecto(self):
        response = self.publicar()

        puntajes = EvaluacionProblema.objects.filter(evaluacion_id=response.data["id"]).values_list("puntaje", flat=True)
        self.assertEqual({float(p) for p in puntajes}, {1.0})

    def test_un_solo_problema_basta(self):
        response = self.publicar(problemas=[self.suma.pk])

        self.assertEqual(response.status_code, 201)
        self.assertEqual(EvaluacionProblema.objects.count(), 1)

    def test_una_evaluacion_sin_problemas_da_400_y_no_guarda_nada(self):
        for problemas in [[], None]:
            with self.subTest(problemas):
                response = self.publicar(problemas=problemas)

                self.assertEqual(response.status_code, 400)
                self.assertIn("problemas", response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    # CA-062.3: el mensaje le dice al docente qué hacer, tanto con la lista vacía como sin el campo.
    def test_el_mensaje_de_una_evaluacion_sin_problemas_pide_elegir_al_menos_uno(self):
        sin_campo = {k: v for k, v in self.cuerpo.items() if k != "problemas"}
        for cuerpo in [{**self.cuerpo, "problemas": []}, sin_campo]:
            with self.subTest(cuerpo.get("problemas", "sin campo")):
                response = self.client.post(self.url(), cuerpo, format="json")

                self.assertEqual(response.status_code, 400)
                self.assertEqual(
                    [str(error) for error in response.data["problemas"]],
                    ["Elige al menos un problema para la evaluación."],
                )

    def test_un_problema_puede_estar_en_otra_evaluacion_del_mismo_curso(self):
        self.assertEqual(self.publicar(problemas=[self.suma.pk]).status_code, 201)

        self.assertEqual(self.publicar(problemas=[self.suma.pk]).status_code, 201)

        self.assertEqual(EvaluacionProblema.objects.filter(problema=self.suma).count(), 2)

    def test_los_problemas_que_no_sirven_dan_400_en_problemas_y_no_guardan_nada(self):
        otro_curso = Curso.objects.create(docente=self.docente, nombre="Otro curso", lenguaje="python")
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="De B", lenguaje="python")
        malos = {
            "no existe": self.suma.pk + 100_000,
            "de otro curso del mismo docente": self.crear_problema("De otro curso", curso=otro_curso).pk,
            "de un curso ajeno": self.crear_problema("De B", curso=ajeno).pk,
            "sin aprobar": self.crear_problema("Borrador", aprobado=False).pk,
            "descartado": self.crear_problema("Descartado", archivado=True).pk,
        }
        for motivo, problema_id in malos.items():
            with self.subTest(motivo):
                response = self.publicar(problemas=[self.suma.pk, problema_id])

                self.assertEqual(response.status_code, 400)
                self.assertIn(str(problema_id), str(response.data["problemas"]))
                # Los buenos no se nombran: solo el que no sirve.
                self.assertNotIn(f": {self.suma.pk}", str(response.data["problemas"]))
        self.assertEqual(Evaluacion.objects.count(), 0)
        self.assertEqual(EvaluacionProblema.objects.count(), 0)

    # No se revela si el problema existe en un curso ajeno: da el mismo mensaje que uno que no existe.
    def test_un_problema_ajeno_da_el_mismo_mensaje_que_uno_inexistente(self):
        ajeno = Curso.objects.create(docente=self.crear_docente("docente-b"), nombre="De B", lenguaje="python")
        de_otro = self.crear_problema("De B", curso=ajeno)

        ajeno_response = self.publicar(problemas=[de_otro.pk])
        inexistente_response = self.publicar(problemas=[de_otro.pk + 100_000])

        self.assertEqual(
            str(ajeno_response.data["problemas"]).replace(str(de_otro.pk), "X"),
            str(inexistente_response.data["problemas"]).replace(str(de_otro.pk + 100_000), "X"),
        )

    def test_repetir_un_problema_da_400(self):
        response = self.publicar(problemas=[self.suma.pk, self.suma.pk])

        self.assertEqual(response.status_code, 400)
        self.assertIn("problemas", response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)

    def test_ids_que_no_son_enteros_positivos_dan_400(self):
        for problemas in ["uno", ["uno"], [0], [-3], [1.5]]:
            with self.subTest(problemas):
                response = self.publicar(problemas=problemas)

                self.assertEqual(response.status_code, 400)
                self.assertIn("problemas", response.data)
        self.assertEqual(Evaluacion.objects.count(), 0)
