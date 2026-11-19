from django.conf import settings
from django.db import models


# Perfil del docente; el login lo maneja el usuario de Django (ADR-4).
class Docente(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    nombre = models.CharField(max_length=150)
    email = models.EmailField()
    institucion = models.CharField(max_length=150)

    class Meta:
        db_table = "admin_docente"


# Confirma que quien se registró controla el correo, con un código de 6 dígitos que se le envía (así un correo inventado
# no sirve). No hay fila para las cuentas anteriores a esta función ni para las que entran con Google (Google ya verificó el
# correo): sin fila, el correo cuenta como verificado.
class VerificacionCorreo(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="verificacion")
    # El código no se guarda: solo su huella (ver cuentas/verificacion.py).
    codigo_hash = models.CharField(max_length=64, blank=True)
    vence = models.DateTimeField()
    # Intentos fallidos con el código actual; al llegar al máximo hay que pedir uno nuevo.
    intentos = models.PositiveSmallIntegerField(default=0)
    # Cuándo se envió el último código: limita los reenvíos.
    enviado = models.DateTimeField()
    verificado = models.BooleanField(default=False)

    class Meta:
        db_table = "admin_verificacion_correo"

