# API interna de problemas (D3): la consume user-api con la cabecera X-Internal-Key.
# HU31: aquí se decide qué problemas puede ver un estudiante, y es la única puerta. Toda consulta parte de
# Problema.objects.visibles_para_estudiante(): ninguna ruta de esta API alcanza un problema sin aprobar
# ni descartado, ni siquiera conociendo su id (ADR-2).
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.response import Response

from cursos.interno import VistaInterna
from cursos.models import Curso
from problemas.models import Plataforma, Problema
from problemas.serializers import (
    ProblemaCalificacionSerializer,
    ProblemaInternoSerializer,
    ProblemaResumenSerializer,
)

# Quien consulta es un cliente web o móvil, nunca «ambas»: un problema para ambas plataformas sale en las dos.
PLATAFORMAS_CLIENTE = (Plataforma.WEB, Plataforma.MOVIL)


class ProblemasDelCursoView(VistaInterna):
    # HU29-T1 y HU31-T1. Los problemas que el estudiante puede ver en un curso. Que el estudiante esté inscrito
    # lo comprueba user-api (es quien tiene las inscripciones, ADR-1); aquí solo se decide qué problemas son visibles.
    # ?plataforma=web|movil deja los de esa plataforma y los de ambas.
    def get(self, request, curso_id):
        # Un curso que no existe es 404; uno que existe pero aún no tiene aprobados es una lista vacía.
        get_object_or_404(Curso, pk=curso_id)
        problemas = Problema.objects.visibles_para_estudiante().filter(curso_id=curso_id)
        plataforma = request.query_params.get("plataforma", "").strip()
        if plataforma:
            if plataforma not in PLATAFORMAS_CLIENTE:
                return Response({"detail": "plataforma debe ser web o movil."}, status=status.HTTP_400_BAD_REQUEST)
            problemas = problemas.filter(plataforma__in=[plataforma, Plataforma.AMBAS])
        return Response(ProblemaResumenSerializer(problemas.order_by("id"), many=True).data)


class ProblemaView(VistaInterna):
    # HU31-T2 y HU32-T1. Un problema por id, con solo los casos públicos. Si no está aprobado, está descartado o no
    # existe, la respuesta es la misma 404: así adivinar ids no revela cuáles existen sin aprobar.
    def get(self, request, problema_id):
        problema = get_object_or_404(Problema.objects.visibles_para_estudiante(), pk=problema_id)
        return Response(ProblemaInternoSerializer(problema).data)


class ProblemaParaCalificarView(VistaInterna):
    # HU36-T2. Los casos COMPLETOS de un problema visible, ocultos incluidos: user-api los necesita para calificar un
    # envío. Es la única ruta que los entrega y solo con la clave interna; user-api nunca los reenvía al estudiante
    # (HU39). Parte de visibles_para_estudiante() como las demás: un problema sin aprobar o descartado da 404.
    def get(self, request, problema_id):
        problema = get_object_or_404(Problema.objects.visibles_para_estudiante(), pk=problema_id)
        return Response(ProblemaCalificacionSerializer(problema).data)
