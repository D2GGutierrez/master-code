from rest_framework.permissions import BasePermission


class EsDocente(BasePermission):
    # Se mira el perfil en la base y no el claim rol del token: un token válido
    # de otro rol (estudiante, HU21) no basta para entrar.
    def has_permission(self, request, view):
        return hasattr(request.user, "docente")


class EsPropietario(BasePermission):
    # DRF la llama al buscar el objeto (get_object), después de EsDocente:
    # por eso request.user.docente ya existe. Se comparan ids para no consultar el docente del curso.
    def has_object_permission(self, request, view, obj):
        return obj.docente_id == request.user.docente.pk
