from django.conf import settings
from google.auth.exceptions import TransportError
from google.auth.transport import requests as peticiones_google
from google.oauth2 import id_token


class GoogleNoValido(Exception):
    """El token de Google no sirve para entrar. El mensaje se puede mostrar a la persona."""


class GoogleNoDisponible(Exception):
    """No se pudo hablar con Google (o el inicio con Google no está configurado)."""


def verificar_token_google(token):
    # Devuelve los datos de la persona que Google certifica: correo, nombre y dominio.
    if not settings.GOOGLE_CLIENT_IDS:
        raise GoogleNoDisponible("El inicio de sesión con Google no está configurado.")
    try:
        # Comprueba la firma, el vencimiento, el emisor y que el token sea para uno de nuestros ID de cliente.
        datos = id_token.verify_oauth2_token(token, peticiones_google.Request(), settings.GOOGLE_CLIENT_IDS)
    except TransportError as error:
        raise GoogleNoDisponible("No pudimos comunicarnos con Google. Inténtalo de nuevo.") from error
    except ValueError as error:
        raise GoogleNoValido("No pudimos verificar tu cuenta de Google.") from error

    dominio = settings.DOMINIO_INSTITUCIONAL.lower()
    email = str(datos.get("email", "")).lower()
    # hd es el dominio de Google Workspace que Google certifica; se exige además de mirar el correo,
    # porque una cuenta personal puede tener cualquier correo pero nunca un hd institucional.
    if not datos.get("email_verified") or str(datos.get("hd", "")).lower() != dominio or email.rsplit("@", 1)[-1] != dominio:
        raise GoogleNoValido(f"Entra con tu cuenta institucional @{settings.DOMINIO_INSTITUCIONAL}.")
    datos["email"] = email
    return datos
