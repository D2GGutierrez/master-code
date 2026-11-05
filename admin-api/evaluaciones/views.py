from django.shortcuts import get_object_or_404
from rest_framework.generics import CreateAPIView
from rest_framework.permissions import IsAuthenticated

from cuentas.permissions import EsDocente
from cursos.models import Curso
from evaluaciones.serializers import EvaluacionSerializer


class CrearEvaluacionView(CreateAPIView):
    serializer_class = EvaluacionSerializer
    permission_classes = [IsAuthenticated, EsDocente]

    # HU61 T2 y HU62: crea la evaluación de un curso del docente junto con sus problemas. El curso se busca antes de
    # validar el cuerpo, con su dueño: uno ajeno da el mismo 404 que uno inexistente (HU44-T4), sin llegar a decir qué
    # problemas existen. La regla de las fechas es de HU61-T4.
    def get_serializer_context(self):
        contexto = super().get_serializer_context()
        contexto["curso"] = get_object_or_404(Curso, pk=self.kwargs["curso_id"], docente=self.request.user.docente)
        return contexto

    def perform_create(self, serializer):
        serializer.save(curso=serializer.context["curso"])
