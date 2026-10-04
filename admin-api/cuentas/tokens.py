from rest_framework_simplejwt.tokens import RefreshToken


def crear_tokens(user, cliente):
    # El rol no se guarda: se deduce del perfil. Sin perfil conocido no se emite token.
    if not hasattr(user, "docente"):
        raise ValueError(f"El usuario {user.pk} no tiene un perfil con rol conocido.")

    refresh = RefreshToken.for_user(user)
    # El estándar JWT pide sub como texto y simplejwt deja el id como número.
    refresh["sub"] = str(user.pk)
    refresh["email"] = user.email
    refresh["rol"] = "docente"
    refresh["institucion"] = user.docente.institucion
    refresh["cliente"] = cliente
    # El access copia los claims del refresh, por eso se agregan una sola vez.
    return str(refresh), str(refresh.access_token)
