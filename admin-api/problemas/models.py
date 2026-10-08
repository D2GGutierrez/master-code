from django.db import models

from cursos.models import Curso


class Problema(models.Model):
    class Plataforma(models.TextChoices):
        WEB = "web", "Solo web"
        MOVIL = "movil", "Solo móvil"
        AMBAS = "ambas", "Web y móvil"

    class TipoEjercicio(models.TextChoices):
        ESCRITURA_CODIGO = "escritura_codigo", "Escritura de código"
        PREDICCION_SALIDA = "prediccion_salida", "Predicción de salida"
        OPCION_MULTIPLE = "opcion_multiple", "Opción múltiple"
        COMPLETAR_ESPACIO = "completar_espacio", "Completar el espacio"

    # PROTECT: la base impide borrar un curso que tiene problemas (HU12 lo convierte en 409).
    curso = models.ForeignKey(Curso, on_delete=models.PROTECT)
    titulo = models.CharField(max_length=150)
    enunciado = models.TextField()
    # Texto libre: los valores permitidos de lenguaje y dificultad los valida el endpoint (HU13 T2).
    lenguaje = models.CharField(max_length=50)
    dificultad = models.CharField(max_length=20)
    # Sin valor por defecto: el docente siempre debe elegirlos al crear el problema.
    plataforma = models.CharField(max_length=10, choices=Plataforma.choices)
    tipo_ejercicio = models.CharField(max_length=20, choices=TipoEjercicio.choices)
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "admin_problema"
