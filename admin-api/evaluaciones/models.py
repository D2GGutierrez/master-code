from django.db import models

from cursos.models import Curso
from problemas.models import Problema


# Examen de un curso con una ventana de tiempo (HU61 T1). Los problemas que contiene van en EvaluacionProblema (HU62)
# y sus puntos (HU63) se agregarán a esa misma relación; el estado programada/activa/cerrada se calcula (HU64).
class Evaluacion(models.Model):
    curso = models.ForeignKey(Curso, on_delete=models.CASCADE)
    fecha_inicio = models.DateTimeField()
    fecha_fin = models.DateTimeField()
    # Minutos que tiene el estudiante para resolverla una vez que entra, dentro de la ventana de inicio a fin.
    limite_minutos = models.PositiveIntegerField()

    class Meta:
        db_table = "admin_evaluacion"


# Qué problemas contiene cada evaluación (HU62 T1): una fila por problema de la evaluación. Que el problema sea del
# mismo curso que la evaluación no lo asegura la base, sino la vista que la crea (HU62 T2).
class EvaluacionProblema(models.Model):
    evaluacion = models.ForeignKey(Evaluacion, on_delete=models.CASCADE)
    # PROTECT: la base impide borrar un problema que está en una evaluación. Los problemas no se borran, se descartan.
    problema = models.ForeignKey(Problema, on_delete=models.PROTECT)

    class Meta:
        db_table = "admin_evaluacion_problema"
        constraints = [
            # Un problema entra una sola vez en la misma evaluación; en otras evaluaciones puede repetirse.
            models.UniqueConstraint(fields=["evaluacion", "problema"], name="uq_evaluacion_problema"),
        ]
