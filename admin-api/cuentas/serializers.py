from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.auth.password_validation import validate_password
from django.db import transaction
from rest_framework import serializers

from cuentas.models import Docente
from cuentas.validators import validar_dominio_institucional


class RegistroDocenteSerializer(serializers.Serializer):
    nombre = serializers.CharField(max_length=150)
    email = serializers.EmailField(validators=[validar_dominio_institucional])
    password = serializers.CharField(write_only=True)

    def validate_email(self, value):
        email = value.lower()
        if User.objects.filter(username__iexact=email).exists():
            raise serializers.ValidationError("Ya existe una cuenta con este correo.")
        return email

    def validate_password(self, value):
        validate_password(value)
        return value

    # atomic: si falla el Docente, tampoco queda el usuario.
    @transaction.atomic
    def create(self, validated_data):
        email = validated_data["email"]
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


class LoginSerializer(serializers.Serializer):
    # EmailField ya quita los espacios de los extremos; aquí solo se pasa a minúsculas.
    email = serializers.EmailField()
    password = serializers.CharField()
    cliente = serializers.ChoiceField(choices=["admin-web", "user-web", "mobile"])

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
