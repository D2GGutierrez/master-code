from django.db import models

from cursos.models import Curso


# Examen de un curso con una ventana de tiempo (HU61 T1). Los problemas que contiene (HU62) y sus
# puntos (HU63) irán en una relación aparte; el estado programada/activa/cerrada se calcula (HU64).
class Evaluacion(models.Model):
    curso = models.ForeignKey(Curso, on_delete=models.CASCADE)
    fecha_inicio = models.DateTimeField()
    fecha_fin = models.DateTimeField()
    # Minutos que tiene el estudiante para resolverla una vez que entra, dentro de la ventana de inicio a fin.
    limite_minutos = models.PositiveIntegerField()

    class Meta:
        db_table = "admin_evaluacion"
