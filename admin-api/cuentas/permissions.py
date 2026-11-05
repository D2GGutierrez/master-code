import secrets

from django.conf import settings
from rest_framework.permissions import BasePermission


class EsDocente(BasePermission):
    # Se mira el perfil en la base y no el claim rol del token: un token válido
    # de otro rol (estudiante, HU21) no basta para entrar.
    def has_permission(self, request, view):
        return hasattr(request.user, "docente")


class ClaveInterna(BasePermission):
    # D3: solo user-api conoce la clave. Se usa en vistas sin JWT (authentication_classes = []).
    def has_permission(self, request, view):
        clave = settings.INTERNAL_API_KEY
        enviada = request.headers.get("X-Internal-Key", "")
        # Sin clave configurada se niega todo. compare_digest tarda lo mismo acierte o no,
        # para que el tiempo de respuesta no revele cuántos caracteres coinciden.
        return bool(clave) and secrets.compare_digest(enviada.encode(), clave.encode())
