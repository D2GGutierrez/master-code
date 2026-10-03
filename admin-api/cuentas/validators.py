from django.conf import settings
from rest_framework import serializers


# Se reutiliza en el registro de estudiantes (HU21).
def validar_dominio_institucional(email):
    dominio = email.rsplit("@", 1)[-1].lower()
    # Comparación exacta: un subdominio como x.dominio no cuenta.
    if dominio != settings.DOMINIO_INSTITUCIONAL.lower():
        raise serializers.ValidationError(
            f"El correo debe ser del dominio institucional @{settings.DOMINIO_INSTITUCIONAL}."
        )
