from rest_framework.permissions import BasePermission


class EsDocente(BasePermission):
    # Se mira el perfil en la base y no el claim rol del token: un token válido
    # de otro rol (estudiante, HU21) no basta para entrar.
    def has_permission(self, request, view):
        return hasattr(request.user, "docente")
