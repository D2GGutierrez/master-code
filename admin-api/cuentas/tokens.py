from datetime import timedelta

from django.conf import settings
from rest_framework_simplejwt.tokens import RefreshToken


def crear_tokens(user, cliente):
    # El rol no se guarda: se deduce del perfil Docente o del grupo estudiante.
    # Sin rol conocido no se emite token.
    if hasattr(user, "docente"):
        rol, institucion = "docente", user.docente.institucion
    elif user.groups.filter(name="estudiante").exists():
        # El estudiante no tiene perfil en admin-api: la institución sale del dominio del correo.
        rol, institucion = "estudiante", user.email.rsplit("@", 1)[-1].lower()
    else:
        raise ValueError(f"El usuario {user.pk} no tiene un perfil con rol conocido.")

    refresh = RefreshToken.for_user(user)
    if cliente == "mobile":
        refresh.set_exp(lifetime=timedelta(days=settings.JWT_REFRESH_DAYS_MOBILE))
    # El estándar JWT pide sub como texto y simplejwt deja el id como número.
    refresh["sub"] = str(user.pk)
    refresh["email"] = user.email
    refresh["rol"] = rol
    refresh["institucion"] = institucion
    refresh["cliente"] = cliente
    # El access copia los claims del refresh, por eso se agregan una sola vez.
    return str(refresh), str(refresh.access_token)
