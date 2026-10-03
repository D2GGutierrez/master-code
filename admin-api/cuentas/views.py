from rest_framework.generics import CreateAPIView
from rest_framework.permissions import AllowAny

from cuentas.serializers import RegistroDocenteSerializer


class RegistroDocenteView(CreateAPIView):
    serializer_class = RegistroDocenteSerializer
    permission_classes = [AllowAny]
    # Sin autenticación: quien se registra todavía no tiene cuenta.
    authentication_classes = []
