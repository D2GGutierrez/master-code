import base64
import hashlib
import json
from datetime import timedelta

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from django.conf import settings
from jwt.algorithms import RSAAlgorithm
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


def jwk_publico(clave_publica_pem):
    # Lanza ValueError si el PEM no es válido y TypeError si la clave no es RSA.
    clave = serialization.load_pem_public_key(clave_publica_pem.encode())
    # Se revisa a mano: con otra clave (por ejemplo EC) to_jwk falla con un error inesperado.
    if not isinstance(clave, rsa.RSAPublicKey):
        raise TypeError("La clave pública no es RSA.")
    publica = RSAAlgorithm.to_jwk(clave, as_dict=True)
    n, e = publica["n"], publica["e"]
    # kid = huella RFC 7638: SHA-256 del JSON con e, kty y n en orden y sin espacios.
    # Es estable: la misma clave da siempre el mismo kid.
    huella = hashlib.sha256(json.dumps({"e": e, "kty": "RSA", "n": n}, separators=(",", ":")).encode()).digest()
    kid = base64.urlsafe_b64encode(huella).rstrip(b"=").decode()
    # Se arma a mano para publicar solo estos campos (to_jwk agrega key_ops).
    return {"kty": "RSA", "use": "sig", "alg": "RS256", "kid": kid, "n": n, "e": e}
