import hashlib
import hmac
import logging
import secrets
from datetime import timedelta

from django.conf import settings
from django.core.mail import send_mail
from django.utils import timezone

from cuentas.models import VerificacionCorreo

logger = logging.getLogger(__name__)

MAX_INTENTOS = 5
# Segundos que hay que esperar entre un código y el siguiente.
ESPERA_REENVIO = 60

# Resultados de comprobar un código.
CORRECTO = "correcto"
INCORRECTO = "incorrecto"
VENCIDO = "vencido"
DEMASIADOS_INTENTOS = "demasiados_intentos"
SIN_PENDIENTE = "sin_pendiente"


def _huella(user, codigo):
    # HMAC con la clave secreta del servidor: con la base filtrada no se pueden probar los 1 000 000 de códigos sin ella.
    return hmac.new(settings.SECRET_KEY.encode(), f"{user.pk}:{codigo}".encode(), hashlib.sha256).hexdigest()


def requiere_verificacion(user):
    verificacion = VerificacionCorreo.objects.filter(user=user).first()
    return verificacion is not None and not verificacion.verificado


def confirmar_correo(user):
    # Para cuando otra vía ya comprobó el correo (Google).
    VerificacionCorreo.objects.filter(user=user, verificado=False).update(verificado=True, codigo_hash="")


def generar_codigo(user):
    # Crea (o reemplaza) el código pendiente de la cuenta y lo devuelve en claro, solo para enviarlo.
    codigo = f"{secrets.randbelow(10**6):06d}"
    ahora = timezone.now()
    VerificacionCorreo.objects.update_or_create(
        user=user,
        defaults={
            "codigo_hash": _huella(user, codigo),
            "vence": ahora + timedelta(minutes=settings.VERIFICACION_MINUTOS),
            "intentos": 0,
            "enviado": ahora,
            "verificado": False,
        },
    )
    return codigo


def enviar_codigo(user, codigo):
    mensaje = (
        f"Tu código de verificación de MASTER CODE es: {codigo}\n\n"
        f"Escríbelo en la pantalla para activar tu cuenta. Vence en {settings.VERIFICACION_MINUTOS} minutos.\n"
        "Si no creaste una cuenta en MASTER CODE, ignora este mensaje."
    )
    try:
        send_mail("Tu código de MASTER CODE", mensaje, None, [user.email])
    except OSError as error:
        # OSError cubre los errores de SMTP y de red. Solo se registra el tipo: el mensaje podría traer datos del servidor.
        logger.error("No se pudo enviar el código de verificación: %s", type(error).__name__)
        return False
    return True


def iniciar_verificacion(user):
    # Deja la cuenta pendiente de verificar y envía el primer código. False si el correo no salió.
    return enviar_codigo(user, generar_codigo(user))


def puede_reenviar(verificacion):
    return timezone.now() >= verificacion.enviado + timedelta(seconds=ESPERA_REENVIO)


def comprobar_codigo(user, codigo):
    verificacion = VerificacionCorreo.objects.filter(user=user).first()
    if verificacion is None or verificacion.verificado or not verificacion.codigo_hash:
        return SIN_PENDIENTE
    if timezone.now() > verificacion.vence:
        return VENCIDO
    if verificacion.intentos >= MAX_INTENTOS:
        return DEMASIADOS_INTENTOS
    if not hmac.compare_digest(verificacion.codigo_hash, _huella(user, codigo.strip())):
        verificacion.intentos += 1
        verificacion.save(update_fields=["intentos"])
        return INCORRECTO
    verificacion.verificado = True
    verificacion.codigo_hash = ""
    verificacion.save(update_fields=["verificado", "codigo_hash"])
    return CORRECTO
