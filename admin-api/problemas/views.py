from django.db import transaction
from django.shortcuts import get_object_or_404
from rest_framework import serializers, status
from rest_framework.generics import CreateAPIView, ListAPIView, RetrieveUpdateAPIView
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from cuentas.permissions import EsDocente
from cursos.models import Curso
from problemas.completitud import faltante_de
from problemas.generacion import guardar_generados
from problemas.ia import IaNoDisponible, IaRespuestaInvalida, generar_problemas
from problemas.models import Problema
from problemas.serializers import (
    GenerarSerializer,
    ProblemaPendienteSerializer,
    ProblemaSerializer,
)

MENSAJE_IA_CAIDA = "El servicio de IA no está disponible en este momento. No se guardó nada: intenta generar de nuevo en un momento."
MENSAJE_IA_INVALIDA = "La IA devolvió una respuesta que no se pudo usar. No se guardó nada: intenta generar de nuevo."


class CrearProblemaView(CreateAPIView):
    serializer_class = ProblemaSerializer
    permission_classes = [IsAuthenticated, EsDocente]

    def perform_create(self, serializer):
        # Se busca el curso junto con su docente: un curso ajeno da el mismo 404 que uno
        # inexistente, para no revelar qué cursos existen.
        curso = get_object_or_404(Curso, pk=self.kwargs["curso_id"], docente=self.request.user.docente)
        # La revisión obligatoria es para lo que genera la IA (ADR-2). Quien escribe un problema a mano
        # ya lo revisó: si está completo nace aprobado; si falta algo queda como borrador sin aprobar.
        with transaction.atomic():
            problema = serializer.save(curso=curso)
            if faltante_de(problema) is None:
                problema.registrar_aprobacion(self.request.user.docente)


# HU17: el docente pide varios problemas y quedan en su bandeja de revisión, nunca publicados (ADR-2).
class GenerarProblemasView(APIView):
    permission_classes = [IsAuthenticated, EsDocente]

    def post(self, request, curso_id):
        # Un curso ajeno da el mismo 404 que uno inexistente, igual que al crear un problema.
        curso = get_object_or_404(Curso, pk=curso_id, docente=request.user.docente)
        entrada = GenerarSerializer(data=request.data)
        entrada.is_valid(raise_exception=True)
        datos = entrada.validated_data
        solicitud = {
            "tema": datos["tema"],
            "nivel": datos["nivel"],
            "lenguaje": datos.get("lenguaje") or curso.lenguaje,
            "cantidad": datos["cantidad"],
            "plataforma": datos["plataforma"],
        }
        # HU17-T6: sea porque la IA está caída o porque respondió algo inservible, no se guarda nada y el
        # docente recibe un mensaje claro. `reintentar` le dice al formulario que muestre el botón.
        try:
            problemas = guardar_generados(curso, solicitud, generar_problemas(solicitud))
        except IaNoDisponible:
            return Response(
                {"detail": MENSAJE_IA_CAIDA, "reintentar": True}, status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        except IaRespuestaInvalida:
            return Response({"detail": MENSAJE_IA_INVALIDA, "reintentar": True}, status=status.HTTP_502_BAD_GATEWAY)
        return Response({"problemas": ProblemaSerializer(problemas, many=True).data}, status=status.HTTP_201_CREATED)


# HU18-T1: la bandeja de revisión. Solo lo que generó la IA y el docente aún no decide, de sus propios cursos.
# ?curso=<id> la reduce a un curso (HU18-T3); un curso ajeno da una lista vacía y no revela que existe.
class PendientesView(ListAPIView):
    permission_classes = [IsAuthenticated, EsDocente]
    serializer_class = ProblemaPendienteSerializer

    def get_queryset(self):
        # select_related y prefetch_related: una consulta por tabla y no una por problema.
        consulta = (
            Problema.objects.pendientes_de_revision()
            .filter(curso__docente=self.request.user.docente)
            .select_related("curso")
            .prefetch_related("casos_prueba", "opciones")
            .order_by("-fecha_creacion", "-id")
        )
        curso = self.request.query_params.get("curso")
        if curso is not None:
            if not curso.isdecimal():
                raise serializers.ValidationError({"curso": "Indica el id numérico del curso."})
            consulta = consulta.filter(curso_id=int(curso))
        return consulta


def problema_del_docente(request, pk):
    # Un problema de otro docente da el mismo 404 que uno inexistente, igual que al crearlo. select_for_update
    # (dentro de una transacción) evita que aprobar y descartar a la vez dejen un problema en dos estados.
    consulta = Problema.objects.select_for_update(of=("self",)).filter(curso__docente=request.user.docente)
    return get_object_or_404(consulta, pk=pk)


# HU19-T1: leer un problema y corregirlo antes de aprobarlo. Solo el docente dueño del curso: uno ajeno da 404.
# Solo PUT (reemplazo completo) y no PATCH: validate necesita plataforma y tipo_ejercicio juntos.
# curso, generado_por_ia, aprobado_por_docente, fecha_aprobacion y archivado son de solo lectura en el
# serializer: si el cuerpo los trae, se ignoran. Aprobar es su propia acción, con quién y cuándo (HU19-T2).
class ProblemaView(RetrieveUpdateAPIView):
    permission_classes = [IsAuthenticated, EsDocente]
    serializer_class = ProblemaPendienteSerializer
    http_method_names = ["get", "put", "head", "options"]

    def get_queryset(self):
        return (
            Problema.objects.filter(curso__docente=self.request.user.docente)
            .select_related("curso")
            .prefetch_related("casos_prueba", "opciones")
        )

    def put(self, request, *args, **kwargs):
        # Bloquea la fila mientras se guarda: así no se aprueba a la vez con los datos anteriores.
        with transaction.atomic():
            problema_del_docente(request, kwargs["pk"])
            return super().put(request, *args, **kwargs)


# HU19-T2: «Aprobar» deja el problema listo para publicarse: aprobado_por_docente=True y quién y cuándo (CA-19.4, 19.5).
# Un problema incompleto no se aprueba (CA-19.6, HU19-T4): 400 con el motivo bajo el nombre del campo que falta.
class AprobarProblemaView(APIView):
    permission_classes = [IsAuthenticated, EsDocente]

    def post(self, request, pk):
        with transaction.atomic():
            problema = problema_del_docente(request, pk)
            if problema.archivado:
                return Response(
                    {"detail": "Este problema fue descartado y ya no se puede aprobar."}, status=status.HTTP_409_CONFLICT
                )
            # Aprobar dos veces (doble clic) no es un error y no cambia quién ni cuándo aprobó.
            if not problema.aprobado_por_docente:
                pendiente = faltante_de(problema)
                if pendiente:
                    campo, mensaje = pendiente
                    return Response({campo: [mensaje]}, status=status.HTTP_400_BAD_REQUEST)
                problema.registrar_aprobacion(request.user.docente)
        return Response(ProblemaSerializer(problema).data)


# HU20-T1: «Descartar» archiva el problema, no lo borra (CA-20.3, HU20-T2). El cuadro de confirmación es de la
# pantalla (CA-20.2); aquí se exige el envío explícito de confirmar=true, para que ninguna llamada suelta descarte
# un problema por accidente. Cancelar el cuadro no llama a la API, así que no cambia nada.
class DescartarProblemaView(APIView):
    permission_classes = [IsAuthenticated, EsDocente]

    def post(self, request, pk):
        with transaction.atomic():
            # Primero el problema: uno ajeno da 404 aunque falte la confirmación.
            problema = problema_del_docente(request, pk)
            if not isinstance(request.data, dict) or request.data.get("confirmar") is not True:
                return Response(
                    {"confirmar": ["Confirma el descarte: el problema se archiva y deja de aparecer en la bandeja."]},
                    status=status.HTTP_400_BAD_REQUEST,
                )
            if problema.aprobado_por_docente and not problema.archivado:
                # Ya lo ven los estudiantes (y pronto tendrá envíos): descartarlo lo haría desaparecer de su avance.
                return Response(
                    {"detail": "Este problema ya fue aprobado y está publicado: no se puede descartar."},
                    status=status.HTTP_409_CONFLICT,
                )
            # Descartar dos veces (doble clic) no es un error.
            if not problema.archivado:
                problema.archivar()
        return Response(ProblemaSerializer(problema).data)
