import secrets
from datetime import timedelta

from django.conf import settings
from django.db import models
from django.utils import timezone

from cuentas.models import Docente

# Sin 0/O ni 1/I/L: el docente dicta o escribe el código y esos caracteres se confunden.
ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"


def generar_codigo():
    # secrets y no random: el código da acceso al curso, no debe poder adivinarse.
    return "".join(secrets.choice(ALFABETO_CODIGO) for _ in range(8))


def vence_por_defecto():
    return timezone.now() + timedelta(days=settings.INVITACION_DIAS)


class Curso(models.Model):
    docente = models.ForeignKey(Docente, on_delete=models.CASCADE)
    nombre = models.CharField(max_length=150)
    # Texto libre: la lista de lenguajes permitidos la valida el endpoint (HU09 T2).
    lenguaje = models.CharField(max_length=50)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    # Código que el docente comparte para que los estudiantes se inscriban (HU26 T1).
    # unique: la base impide que dos cursos compartan código aunque el azar lo repitiera.
    codigo_invitacion = models.CharField(max_length=8, unique=True, default=generar_codigo)
    codigo_vence = models.DateTimeField(default=vence_por_defecto)

    class Meta:
        db_table = "admin_curso"
