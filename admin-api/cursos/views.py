from django.db.models import Count
from rest_framework.generics import ListCreateAPIView
from rest_framework.permissions import IsAuthenticated

from cuentas.permissions import EsDocente
from cursos.models import Curso
from cursos.serializers import CursoListaSerializer, CursoSerializer


class CursosView(ListCreateAPIView):
    permission_classes = [IsAuthenticated, EsDocente]

    def get_queryset(self):
        # annotate cuenta los problemas en la misma consulta (COUNT + GROUP BY), no una por curso.
        # El id desempata cursos con la misma fecha para que el orden sea estable.
        return (
            Curso.objects.filter(docente=self.request.user.docente)
            .annotate(cantidad_problemas=Count("problema"))
            .order_by("-fecha_creacion", "-id")
        )

    def get_serializer_class(self):
        # Al listar se agrega cantidad_problemas; la respuesta de crear no cambia.
        if self.request.method == "GET":
            return CursoListaSerializer
        return CursoSerializer

    def perform_create(self, serializer):
        # El dueño sale del token y no del cuerpo, para que nadie cree cursos a nombre de otro docente.
        serializer.save(docente=self.request.user.docente)
