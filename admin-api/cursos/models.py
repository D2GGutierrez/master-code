from django.db import models

from cuentas.models import Docente


class Curso(models.Model):
    docente = models.ForeignKey(Docente, on_delete=models.CASCADE)
    nombre = models.CharField(max_length=150)
    # Texto libre: la lista de lenguajes permitidos la valida el endpoint (HU09 T2).
    lenguaje = models.CharField(max_length=50)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "admin_curso"
