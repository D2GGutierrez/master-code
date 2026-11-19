from django.conf import settings
from django.contrib.auth.models import Group, User
from django.contrib.auth.password_validation import validate_password
from django.db import transaction
from rest_framework import serializers

from cuentas.models import Docente
from cuentas.validators import validar_dominio_institucional
from cuentas.verificacion import requiere_verificacion


# Campos y validaciones comunes al registro de docentes y de estudiantes.
class RegistroSerializer(serializers.Serializer):
    nombre = serializers.CharField(max_length=150)
    email = serializers.EmailField(validators=[validar_dominio_institucional])
    password = serializers.CharField(write_only=True)

    def validate_email(self, value):
        email = value.lower()
        existente = User.objects.filter(username__iexact=email).first()
        # Una cuenta que nadie verificó no bloquea el correo: si no, cualquiera podría registrar el correo de otra persona
        # y dejarla sin poder crear su cuenta. Se reemplaza al crear la nueva (ver liberar_correo).
        if existente is not None and not requiere_verificacion(existente):
            raise serializers.ValidationError("Ya existe una cuenta con este correo.")
        return email

    @staticmethod
    def liberar_correo(email):
        # Borra la cuenta sin verificar de ese correo. Nunca pudo iniciar sesión, así que no tiene datos.
        for user in User.objects.filter(username__iexact=email):
            if requiere_verificacion(user):
                user.delete()

    def validate_password(self, value):
        validate_password(value)
        return value


class RegistroDocenteSerializer(RegistroSerializer):
    # atomic: si falla el Docente, tampoco queda el usuario.
    @transaction.atomic
    def create(self, validated_data):
        email = validated_data["email"]
        self.liberar_correo(email)
        # Inactiva hasta que el Product Owner la active desde el admin de Django.
        user = User(username=email, email=email, is_active=False)
        user.set_password(validated_data["password"])
        user.save()
        return Docente.objects.create(
            user=user,
            nombre=validated_data["nombre"],
            email=email,
            institucion=settings.DOMINIO_INSTITUCIONAL,
        )


class RegistroEstudianteSerializer(RegistroSerializer):
    # atomic: si falla la asignación del grupo, tampoco queda el usuario.
    @transaction.atomic
    def create(self, validated_data):
        email = validated_data["email"]
        self.liberar_correo(email)
        # Activa desde el registro: los estudiantes no esperan aprobación. Igual no pueden iniciar sesión hasta verificar
        # el correo con el código (ver cuentas/verificacion.py).
        user = User.objects.create_user(
            username=email,
            email=email,
            password=validated_data["password"],
            first_name=validated_data["nombre"],
        )
        # El rol vive en un grupo y no en una tabla: el perfil del estudiante es de user-api (HU21 T1).
        grupo, _ = Group.objects.get_or_create(name="estudiante")
        user.groups.add(grupo)
        return user


class LoginSerializer(serializers.Serializer):
    # EmailField ya quita los espacios de los extremos; aquí solo se pasa a minúsculas.
    email = serializers.EmailField()
    password = serializers.CharField()
    cliente = serializers.ChoiceField(choices=["admin-web", "user-web", "mobile"])

    def validate_email(self, value):
        return value.lower()


class GoogleLoginSerializer(serializers.Serializer):
    # El token de identidad que entrega Google al iniciar sesión (JWT firmado por Google).
    id_token = serializers.CharField(max_length=4096)
    cliente = serializers.ChoiceField(choices=["admin-web", "user-web", "mobile"])


class VerificarCorreoSerializer(serializers.Serializer):
    email = serializers.EmailField()
    codigo = serializers.CharField(max_length=10)

    def validate_email(self, value):
        return value.lower()


class ReenviarCodigoSerializer(serializers.Serializer):
    email = serializers.EmailField()

    def validate_email(self, value):
        return value.lower()


class RefreshSerializer(serializers.Serializer):
    refresh = serializers.CharField()


class RecuperarSerializer(serializers.Serializer):
    # EmailField ya quita los espacios de los extremos; aquí solo se pasa a minúsculas.
    email = serializers.EmailField()

    def validate_email(self, value):
        return value.lower()


class NuevaContrasenaSerializer(serializers.Serializer):
    uid = serializers.CharField()
    token = serializers.CharField()
    password = serializers.CharField()
