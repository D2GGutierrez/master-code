from django.db import models
from django.db.models import Q
from django.utils import timezone

from cuentas.models import Docente
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
    # Quién aprobó el problema y cuándo (HU19 T3). Vacíos mientras no se aprueba.
    # SET_NULL: si el docente se elimina, el problema y la fecha de aprobación se conservan.
    aprobado_por = models.ForeignKey(
        Docente, null=True, blank=True, on_delete=models.SET_NULL, related_name="problemas_aprobados"
    )
    fecha_aprobacion = models.DateTimeField(null=True, blank=True)

    # Descartar archiva, no borra (HU20 T2): el problema sigue en la base pero ya no se muestra.
    archivado = models.BooleanField(default=False)

    def archivar(self):
        self.archivado = True
        self.save(update_fields=["archivado"])

    # La acción «Aprobar» (HU19 T2) la usará: así quién y cuándo siempre se guardan juntos.
    def registrar_aprobacion(self, docente):
        self.aprobado_por = docente
        self.fecha_aprobacion = timezone.now()
        self.save(update_fields=["aprobado_por", "fecha_aprobacion"])

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


class CasoPrueba(models.Model):
    # CASCADE: un caso no tiene sentido sin su problema; si el problema se borra, sus casos también.
    problema = models.ForeignKey(Problema, on_delete=models.CASCADE, related_name="casos_prueba")
    # blank=True: algunos programas no reciben entrada.
    entrada = models.TextField(blank=True)
    salida_esperada = models.TextField()
    # False por seguridad: un caso nuevo queda oculto (solo para calificar) salvo que el docente lo publique.
    es_publico = models.BooleanField(default=False)

    class Meta:
        db_table = "admin_caso_prueba"
