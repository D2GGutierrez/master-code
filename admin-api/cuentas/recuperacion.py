from django.contrib.auth.models import User
from django.contrib.auth.tokens import default_token_generator
from django.utils.encoding import force_bytes
from django.utils.http import urlsafe_base64_decode, urlsafe_base64_encode

# El token se calcula con el hash de la contraseña actual: al cambiarla deja de
# servir, por eso es de un solo uso sin guardar nada en la base.


def generar_uid_token(user):
    return urlsafe_base64_encode(force_bytes(user.pk)), default_token_generator.make_token(user)


def usuario_del_token(uid, token):
    try:
        user = User.objects.get(pk=urlsafe_base64_decode(uid).decode())
    except (ValueError, User.DoesNotExist):
        # ValueError: uid que no es base64 o que no es un número.
        return None
    return user if default_token_generator.check_token(user, token) else None
