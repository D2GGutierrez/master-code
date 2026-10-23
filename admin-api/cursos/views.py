from django.contrib.auth.models import User
from django.db.models import Count, ProtectedError, Q
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

MENSAJE_CON_PROBLEMAS = "No se puede eliminar el curso porque ya tiene problemas creados."
MENSAJE_CON_INSCRITOS = "No se puede eliminar el curso porque ya tiene estudiantes inscritos."


class CursosView(ListCreateAPIView):
    permission_classes = [IsAuthenticated, EsDocente]
    # ?search= busca por nombre (parcial, sin distinguir mayúsculas). Se aplica sobre
    # get_queryset, que ya filtra por el docente del token: nunca ve cursos ajenos.
    filter_backends = [SearchFilter]
    search_fields = ["nombre"]

    def get_queryset(self):
        # annotate cuenta los problemas en la misma consulta (COUNT + GROUP BY), no una por curso.
        # Los descartados (archivados) no cuentan (CA-20.4): el filtro va dentro del COUNT, en la misma consulta.
        # El id desempata cursos con la misma fecha para que el orden sea estable.
        return (
            Curso.objects.filter(docente=self.request.user.docente)
            .annotate(
                cantidad_problemas=Count(
                    "problema", filter=Q(problema__archivado=False)
                )
            )
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
        # get_object aplica EsPropietario: un curso ajeno da 403 antes de preguntar nada a user-api.
        curso = self.get_object()
        # Primero lo que se sabe aquí mismo: con problemas ya no hace falta consultar a user-api.
        if curso.problema_set.exists():
            return Response({"detail": MENSAJE_CON_PROBLEMAS}, status=status.HTTP_409_CONFLICT)
        # CA-12.3 (HU28 T5): los inscritos viven en user-api, que responde por su API interna.
        try:
            tiene_inscritos = bool(inscritos_de(curso.pk))
        except UserApiNoDisponible:
            # Sin poder comprobarlo no se borra: eliminar un curso con inscritos deja inscripciones huérfanas.
            return Response(
                {"detail": "No se pudo comprobar si el curso tiene estudiantes inscritos. Intenta de nuevo en un momento."},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )
        if tiene_inscritos:
            return Response({"detail": MENSAJE_CON_INSCRITOS}, status=status.HTTP_409_CONFLICT)
        try:
            self.perform_destroy(curso)
        except ProtectedError:
            # PROTECT en Problema.curso (HU12 T1): si alguien creó un problema justo ahora, la base
            # lo frena; sin esto, DRF respondería un 500.
            return Response({"detail": MENSAJE_CON_PROBLEMAS}, status=status.HTTP_409_CONFLICT)
        return Response(status=status.HTTP_204_NO_CONTENT)


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
