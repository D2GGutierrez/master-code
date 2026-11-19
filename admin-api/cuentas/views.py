import logging

from django.conf import settings
from django.contrib.auth.models import Group, User
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.core.mail import send_mail
from django.db import IntegrityError, transaction
from rest_framework import status
from rest_framework.generics import CreateAPIView
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.exceptions import TokenError
from rest_framework_simplejwt.tokens import RefreshToken

from cuentas.google import GoogleNoDisponible, GoogleNoValido, verificar_token_google
from cuentas.models import Docente
from cuentas.recuperacion import generar_uid_token, usuario_del_token
from cuentas.serializers import (
    GoogleLoginSerializer,
    LoginSerializer,
    NuevaContrasenaSerializer,
    RecuperarSerializer,
    RefreshSerializer,
    RegistroDocenteSerializer,
    RegistroEstudianteSerializer,
)
from cuentas.tokens import crear_tokens, jwk_publico

logger = logging.getLogger(__name__)

CREDENCIALES_INCORRECTAS = {"detail": "Correo o contraseña incorrectos."}
SESION_INVALIDA = {"detail": "La sesión no es válida o ya venció."}
ENLACE_ENVIADO = {"detail": "Si el correo está registrado, te enviamos un enlace para recuperar tu contraseña."}
ENLACE_INVALIDO = {"detail": "El enlace no es válido o ya venció."}


class RegistroDocenteView(CreateAPIView):
    serializer_class = RegistroDocenteSerializer
    permission_classes = [AllowAny]
    # Sin autenticación: quien se registra todavía no tiene cuenta.
    authentication_classes = []


class RegistroEstudianteView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RegistroEstudianteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(
            {"detail": "Registro exitoso. Ya puedes iniciar sesión."}, status=status.HTTP_201_CREATED
        )


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


class GoogleLoginView(APIView):
    # Entrar con la cuenta institucional de Google. Google certifica el correo; este servidor decide el rol:
    # admin-web solo deja pasar a los docentes autorizados (DOCENTES_AUTORIZADOS); user-web y la app, a estudiantes.
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = GoogleLoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        datos = serializer.validated_data

        try:
            persona = verificar_token_google(datos["id_token"])
        except GoogleNoValido as error:
            return Response({"detail": str(error)}, status=status.HTTP_401_UNAUTHORIZED)
        except GoogleNoDisponible as error:
            return Response({"detail": str(error)}, status=status.HTTP_503_SERVICE_UNAVAILABLE)

        email = persona["email"]
        nombre = (persona.get("name") or email.split("@")[0])[:150]
        try:
            with transaction.atomic():
                if datos["cliente"] == "admin-web":
                    user, error = self._docente(email, nombre)
                else:
                    user, error = self._estudiante(email, nombre)
        except IntegrityError:
            # Dos pestañas entraron a la vez con una cuenta nueva: la otra ya la creó, se vuelve a intentar al entrar.
            return Response({"detail": "Inténtalo de nuevo."}, status=status.HTTP_409_CONFLICT)
        if error is not None:
            return Response({"detail": error}, status=status.HTTP_403_FORBIDDEN)

        refresh, access = crear_tokens(user, datos["cliente"])
        return Response({"access": access, "refresh": refresh})

    @staticmethod
    def _docente(email, nombre):
        user = User.objects.filter(username=email).first()
        autorizado = email in settings.DOCENTES_AUTORIZADOS
        if user is not None and not hasattr(user, "docente"):
            return None, "Esta cuenta no es de docente."
        # Un docente que el administrador ya activó sigue entrando aunque no esté en la lista.
        if not autorizado and (user is None or not user.is_active):
            return None, "Tu correo no está en la lista de docentes autorizados. Pídele al administrador que lo agregue."
        if user is None:
            user = User(username=email, email=email)
            user.set_unusable_password()
            user.save()
            Docente.objects.create(user=user, nombre=nombre, email=email, institucion=settings.DOMINIO_INSTITUCIONAL)
        elif not user.is_active:
            user.is_active = True
            user.save(update_fields=["is_active"])
        return user, None

    @staticmethod
    def _estudiante(email, nombre):
        user = User.objects.filter(username=email).first()
        if user is None:
            if email in settings.DOCENTES_AUTORIZADOS:
                return None, "Este correo es de docente: entra por el panel de docentes."
            user = User.objects.create_user(username=email, email=email, password=None, first_name=nombre[:150])
            grupo, _ = Group.objects.get_or_create(name="estudiante")
            user.groups.add(grupo)
        elif not user.is_active:
            return None, "Tu cuenta está pendiente de activación por el administrador."
        return user, None


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


class RecuperarView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = RecuperarSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        # Una cuenta pendiente de activación no puede recuperar la contraseña.
        user = User.objects.filter(username=serializer.validated_data["email"], is_active=True).first()
        if user is not None:
            uid, token = generar_uid_token(user)
            # El estudiante cambia la contraseña en user-web; el docente, en admin-web.
            if user.groups.filter(name="estudiante").exists():
                base, asunto, cuenta = (
                    settings.USER_WEB_URL,
                    "Recupera tu contraseña de MASTER CODE",
                    "tu cuenta de estudiante de MASTER CODE",
                )
            else:
                base, asunto, cuenta = (
                    settings.ADMIN_WEB_URL,
                    "Recupera tu contraseña — MASTER CODE",
                    "tu cuenta de MASTER CODE",
                )
            enlace = f"{base}/nueva-contrasena?uid={uid}&token={token}"
            mensaje = (
                f"Recibimos una solicitud para recuperar la contraseña de {cuenta}.\n"
                f"Abre este enlace para elegir una nueva:\n\n{enlace}\n\n"
                f"El enlace vence en {settings.PASSWORD_RESET_TIMEOUT // 60} minutos. "
                "Si no lo pediste, ignora este mensaje."
            )
            try:
                send_mail(asunto, mensaje, None, [user.email])
            except OSError as error:
                # OSError cubre los errores de SMTP y de red. Se registra solo el tipo:
                # el mensaje del error podría traer el enlace o datos del servidor.
                logger.error("No se pudo enviar el correo de recuperación: %s", type(error).__name__)

        # Misma respuesta exista o no la cuenta (y aunque falle el envío): así no se revela qué correos están registrados.
        return Response(ENLACE_ENVIADO)


class NuevaContrasenaView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def post(self, request):
        serializer = NuevaContrasenaSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        datos = serializer.validated_data

        user = usuario_del_token(datos["uid"], datos["token"])
        if user is None:
            return Response(ENLACE_INVALIDO, status=status.HTTP_400_BAD_REQUEST)
        # Se valida antes de guardar: una contraseña débil no cambia nada y el token sigue sirviendo.
        try:
            validate_password(datos["password"], user)
        except ValidationError as error:
            return Response({"password": error.messages}, status=status.HTTP_400_BAD_REQUEST)

        user.set_password(datos["password"])
        user.save()
        return Response({"detail": "Tu contraseña se actualizó."})


class JwksView(APIView):
    # Pública: user-api la descarga de servidor a servidor para verificar los tokens (ADR-4).
    # Solo expone la clave pública.
    permission_classes = [AllowAny]
    authentication_classes = []

    def get(self, request):
        try:
            jwk = jwk_publico(settings.SIMPLE_JWT["VERIFYING_KEY"])
        except (ValueError, TypeError) as error:
            # Falla cerrado: un JWKS vacío con 200 haría que user-api rechace todo sin saber por qué.
            logger.error("No se pudo publicar el JWKS: %s", type(error).__name__)
            return Response(
                {"detail": "Clave pública no disponible."}, status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        return Response({"keys": [jwk]}, headers={"Cache-Control": "public, max-age=300"})
