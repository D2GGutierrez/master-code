from django.contrib.auth.models import User
from rest_framework import status
from rest_framework.generics import CreateAPIView
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.exceptions import TokenError
from rest_framework_simplejwt.tokens import RefreshToken

from cuentas.serializers import (
    LoginSerializer,
    RefreshSerializer,
    RegistroDocenteSerializer,
)
from cuentas.tokens import crear_tokens

CREDENCIALES_INCORRECTAS = {"detail": "Correo o contraseña incorrectos."}
SESION_INVALIDA = {"detail": "La sesión no es válida o ya venció."}


class RegistroDocenteView(CreateAPIView):
    serializer_class = RegistroDocenteSerializer
    permission_classes = [AllowAny]
    # Sin autenticación: quien se registra todavía no tiene cuenta.
    authentication_classes = []


class LoginView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        datos = serializer.validated_data

        # No se usa authenticate(): devuelve None para inactivos y no permitiría
        # distinguir la cuenta pendiente de activación.
        user = User.objects.filter(username=datos["email"]).first()
        if user is None:
            # Hashea igual que con un usuario real para que el tiempo de respuesta no delate si el correo existe.
            User().set_password(datos["password"])
        if user is None or not user.check_password(datos["password"]):
            return Response(CREDENCIALES_INCORRECTAS, status=status.HTTP_401_UNAUTHORIZED)
        if not user.is_active:
            return Response(
                {"detail": "Tu cuenta está pendiente de activación por el administrador."},
                status=status.HTTP_403_FORBIDDEN,
            )

        try:
            refresh, access = crear_tokens(user, datos["cliente"])
        except ValueError:
            return Response(
                {"detail": "Esta cuenta no tiene un rol asignado."}, status=status.HTTP_403_FORBIDDEN
            )
        return Response({"access": access, "refresh": refresh})


class RefreshView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RefreshSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            # Al abrirlo se validan la firma, el vencimiento y la lista de revocados.
            viejo = RefreshToken(serializer.validated_data["refresh"])
        except TokenError:
            return Response(SESION_INVALIDA, status=status.HTTP_401_UNAUTHORIZED)
        user = User.objects.filter(pk=viejo["sub"], is_active=True).first()
        if user is None:
            return Response(SESION_INVALIDA, status=status.HTTP_401_UNAUTHORIZED)

        viejo.blacklist()
        # Se emite con crear_tokens y no con la rotación de simplejwt, que reiniciaría
        # el exp con la duración web y el móvil perdería sus días.
        refresh, access = crear_tokens(user, viejo["cliente"])
        return Response({"access": access, "refresh": refresh})


class LogoutView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RefreshSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Solo se revoca el refresh: el access sigue valiendo hasta que vence (máx. 15 min),
        # porque un JWT sin estado no se puede anular antes.
        try:
            RefreshToken(serializer.validated_data["refresh"]).blacklist()
        except TokenError:
            pass  # Ya revocado, vencido o inválido: la sesión ya no sirve, así que también es 204.
        return Response(status=status.HTTP_204_NO_CONTENT)
