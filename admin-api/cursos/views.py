from rest_framework.generics import CreateAPIView
from rest_framework.permissions import IsAuthenticated

from cuentas.permissions import EsDocente
from cursos.serializers import CursoSerializer


class CrearCursoView(CreateAPIView):
    serializer_class = CursoSerializer
    permission_classes = [IsAuthenticated, EsDocente]

    def perform_create(self, serializer):
        # El dueño sale del token y no del cuerpo, para que nadie cree cursos a nombre de otro docente.
        serializer.save(docente=self.request.user.docente)
