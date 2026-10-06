from django.db import models

from cursos.models import Curso


class Problema(models.Model):
    # PROTECT: la base impide borrar un curso que tiene problemas (HU12 lo convierte en 409).
    curso = models.ForeignKey(Curso, on_delete=models.PROTECT)
    titulo = models.CharField(max_length=150)
    enunciado = models.TextField()
    # Texto libre: los valores permitidos de lenguaje y dificultad los valida el endpoint (HU13 T2).
    lenguaje = models.CharField(max_length=50)
    dificultad = models.CharField(max_length=20)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "admin_problema"
