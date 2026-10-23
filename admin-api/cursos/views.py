from django.contrib.auth.models import User
from django.db.models import Count, ProtectedError
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.filters import SearchFilter
from rest_framework.generics import ListCreateAPIView, UpdateAPIView
from rest_framework.mixins import DestroyModelMixin
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from cuentas.permissions import EsDocente, EsPropietario
from cursos.inscritos import UserApiNoDisponible, inscritos_de
from cursos.models import Curso
from cursos.serializers import CursoListaSerializer, CursoSerializer


class CursosView(ListCreateAPIView):
    permission_classes = [IsAuthenticated, EsDocente]
    # ?search= busca por nombre (parcial, sin distinguir mayúsculas). Se aplica sobre
    # get_queryset, que ya filtra por el docente del token: nunca ve cursos ajenos.
    filter_backends = [SearchFilter]
    search_fields = ["nombre"]

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


class EditarCursoView(DestroyModelMixin, UpdateAPIView):
    # Todos los cursos: así uno ajeno da 403 (EsPropietario) y uno inexistente 404.
    queryset = Curso.objects.all()
    # Mismas validaciones que al crear; docente no está en sus campos, así que no se puede cambiar.
    serializer_class = CursoSerializer
    permission_classes = [IsAuthenticated, EsDocente, EsPropietario]

    def delete(self, request, *args, **kwargs):
        # El mixin da destroy, pero UpdateAPIView no expone el método DELETE.
        return self.destroy(request, *args, **kwargs)

    def destroy(self, request, *args, **kwargs):
        try:
            return super().destroy(request, *args, **kwargs)
        except ProtectedError:
            # PROTECT en Problema.curso (HU12 T1): sin esto, DRF respondería un 500.
            return Response(
                {"detail": "No se puede eliminar el curso porque ya tiene problemas creados."},
                status=status.HTTP_409_CONFLICT,
            )


class InscritosView(APIView):
    permission_classes = [IsAuthenticated, EsDocente]

    def get(self, request, pk):
        # Se busca entre todos los cursos y luego se comprueba el dueño: uno ajeno da 403 y uno
        # inexistente 404, igual que al editar o eliminar (CA-28.4).
        curso = get_object_or_404(Curso, pk=pk)
        if curso.docente_id != request.user.docente.pk:
            return Response(
                {"detail": "Solo puedes ver los inscritos de tus propios cursos."},
                status=status.HTTP_403_FORBIDDEN,
            )
        try:
            inscritos = inscritos_de(curso.pk)
        except UserApiNoDisponible:
            return Response(
                {"detail": "No se pudo consultar a los inscritos. Intenta de nuevo en un momento."},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )
        # El nombre vive en la cuenta (admin_schema); user-api solo conoce el email. Una sola
        # consulta para todos, no una por estudiante.
        nombres = dict(User.objects.filter(email__in=[i["email"] for i in inscritos]).values_list("email", "first_name"))
        return Response(
            [
                {
                    "email": inscrito["email"],
                    "nombre": nombres.get(inscrito["email"], ""),
                    "fecha_inscripcion": inscrito["fecha"],
                }
                for inscrito in inscritos
            ]
        )
