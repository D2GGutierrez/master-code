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
