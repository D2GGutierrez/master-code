# API interna de evaluaciones (D3, D11): la consume user-api con la cabecera X-Internal-Key. user-api no tiene las
# evaluaciones (viven en admin_schema, ADR-1): aquí le dice cuándo es la ventana y si está activa.
from django.shortcuts import get_object_or_404
from rest_framework.response import Response

from cursos.interno import VistaInterna
from evaluaciones.models import Evaluacion, EvaluacionProblema
from problemas.models import Problema


class EvaluacionView(VistaInterna):
    # HU64-T2. La ventana, el límite de tiempo y el estado calculado (HU64-T1): el estado sale de aquí para que haya una
    # sola regla de ventana. Los problemas son solo ids y solo los que un estudiante puede ver hoy (aprobados y no
    # descartados, ADR-2): uno archivado después de crear la evaluación ya no se entrega. Que el estudiante pueda
    # entrar lo decide user-api con el estado y con sus inscripciones.
    def get(self, request, evaluacion_id):
        evaluacion = get_object_or_404(Evaluacion, pk=evaluacion_id)
        problemas = (
            EvaluacionProblema.objects.filter(
                evaluacion=evaluacion, problema__in=Problema.objects.visibles_para_estudiante()
            )
            .order_by("pk")
            .values_list("problema_id", flat=True)
        )
        return Response(
            {
                "id": evaluacion.pk,
                "curso_id": evaluacion.curso_id,
                "fecha_inicio": evaluacion.fecha_inicio,
                "fecha_fin": evaluacion.fecha_fin,
                "limite_minutos": evaluacion.limite_minutos,
                "estado": evaluacion.estado(),
                "problemas": list(problemas),
            }
        )
