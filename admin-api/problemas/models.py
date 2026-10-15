from django.db import models
from django.db.models import Q

from cursos.models import Curso


# Fuera de Problema para que su Meta pueda usarlas: una clase interna no ve las otras.
class Plataforma(models.TextChoices):
    WEB = "web", "Solo web"
    MOVIL = "movil", "Solo móvil"
    AMBAS = "ambas", "Web y móvil"


class TipoEjercicio(models.TextChoices):
    ESCRITURA_CODIGO = "escritura_codigo", "Escritura de código"
    PREDICCION_SALIDA = "prediccion_salida", "Predicción de salida"
    OPCION_MULTIPLE = "opcion_multiple", "Opción múltiple"
    COMPLETAR_ESPACIO = "completar_espacio", "Completar el espacio"


class Problema(models.Model):
    # Alias: el código existente usa Problema.Plataforma y Problema.TipoEjercicio.
    Plataforma = Plataforma
    TipoEjercicio = TipoEjercicio

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
        constraints = [
            # Misma regla que ProblemaSerializer.validate (HU15 T1), a propósito: el serializer da
            # el mensaje claro al docente y la base frena el admin, el shell o un INSERT directo.
            models.CheckConstraint(
                condition=~(Q(plataforma=Plataforma.MOVIL) & Q(tipo_ejercicio=TipoEjercicio.ESCRITURA_CODIGO))
                & ~(
                    Q(plataforma=Plataforma.WEB)
                    & Q(
                        tipo_ejercicio__in=[
                            TipoEjercicio.PREDICCION_SALIDA,
                            TipoEjercicio.OPCION_MULTIPLE,
                            TipoEjercicio.COMPLETAR_ESPACIO,
                        ]
                    )
                ),
                name="problema_plataforma_tipo_valido",
            )
        ]
