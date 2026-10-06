from django.shortcuts import get_object_or_404
from rest_framework.generics import CreateAPIView
from rest_framework.permissions import IsAuthenticated

from cuentas.permissions import EsDocente
from cursos.models import Curso
from problemas.serializers import ProblemaSerializer


class CrearProblemaView(CreateAPIView):
    serializer_class = ProblemaSerializer
    permission_classes = [IsAuthenticated, EsDocente]

    def perform_create(self, serializer):
        # Se busca el curso junto con su docente: un curso ajeno da el mismo 404 que uno
        # inexistente, para no revelar qué cursos existen.
        curso = get_object_or_404(Curso, pk=self.kwargs["curso_id"], docente=self.request.user.docente)
        serializer.save(curso=curso)
