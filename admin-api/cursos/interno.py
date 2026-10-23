# API interna (D3): la consume user-api por la red de Docker con la cabecera X-Internal-Key.
# user-api no puede leer admin_schema (ADR-1), así que pregunta aquí por los datos de los cursos.
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from cuentas.permissions import ClaveInterna
from cursos.models import Curso

MAX_IDS = 100


def datos_curso(curso):
    # Solo lo que user-api necesita para mostrar un curso; nada del docente salvo su institución.
    return {
        "id": curso.pk,
        "nombre": curso.nombre,
        "lenguaje": curso.lenguaje,
        "institucion": curso.docente.institucion,
    }


class VistaInterna(APIView):
    # Sin JWT: estas rutas no las usa una persona. Sin autenticadores, DRF responde 403 a la clave mala.
    authentication_classes = []
    permission_classes = [ClaveInterna]


class CursoPorCodigoView(VistaInterna):
    def get(self, request, codigo):
        # Los códigos se guardan en mayúsculas; el estudiante puede escribirlo como quiera.
        curso = get_object_or_404(Curso.objects.select_related("docente"), codigo_invitacion=codigo.strip().upper())
        if curso.codigo_vence <= timezone.now():
            return Response({"detail": "El código de invitación venció."}, status=status.HTTP_410_GONE)
        return Response(datos_curso(curso))


class CursosPorIdsView(VistaInterna):
    # GET ?ids=1,2,3: los cursos que existan, sin error por los que no (por ejemplo, uno eliminado).
    def get(self, request):
        try:
            ids = [int(valor) for valor in request.query_params.get("ids", "").split(",") if valor.strip()]
        except ValueError:
            return Response({"detail": "ids debe ser una lista de números."}, status=status.HTTP_400_BAD_REQUEST)
        if len(ids) > MAX_IDS:
            return Response({"detail": f"Máximo {MAX_IDS} ids por consulta."}, status=status.HTTP_400_BAD_REQUEST)
        cursos = Curso.objects.filter(pk__in=ids).select_related("docente").order_by("pk")
        return Response([datos_curso(curso) for curso in cursos])
