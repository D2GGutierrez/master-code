from django.shortcuts import get_object_or_404
from rest_framework.generics import CreateAPIView
from rest_framework.permissions import IsAuthenticated

from cuentas.permissions import EsDocente
from cursos.models import Curso
from evaluaciones.serializers import EvaluacionSerializer


class CrearEvaluacionView(CreateAPIView):
    serializer_class = EvaluacionSerializer
    permission_classes = [IsAuthenticated, EsDocente]

    # HU61 T2: crea la evaluación de un curso del docente. El curso se busca junto con su dueño: uno ajeno da el mismo
    # 404 que uno inexistente (HU44-T4). Las reglas de las fechas y de los problemas son de HU61-T4 y HU62-T3.
    def perform_create(self, serializer):
        curso = get_object_or_404(Curso, pk=self.kwargs["curso_id"], docente=self.request.user.docente)
        serializer.save(curso=curso)
