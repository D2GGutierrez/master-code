from decimal import Decimal

from django.db import transaction
from rest_framework import serializers

from evaluaciones.models import Evaluacion, EvaluacionProblema
from problemas.models import Problema

MENSAJE_SIN_PROBLEMAS = "Elige al menos un problema para la evaluación."
MENSAJE_RANGO_PUNTAJE = "Los puntos deben estar entre 0 y 999.99, con hasta 2 decimales."


# Un problema dentro de la evaluación con lo que vale (HU62, HU63-T2): sirve para recibirlo y para devolverlo, así la
# respuesta tiene la misma forma que el cuerpo. Las reglas del puntaje son las de EvaluacionProblema.puntaje:
# max_digits=5 y decimal_places=2 (hasta 999.99) y mínimo 0. Un puntaje de 0 se acepta (un problema de práctica).
class ProblemaDeEvaluacionSerializer(serializers.Serializer):
    # source: al escribir queda en validated_data["problema_id"]; al leer sale de EvaluacionProblema.problema_id.
    problema = serializers.IntegerField(source="problema_id", min_value=1)
    # Obligatorio: sin él, el problema valdría 1 sin que el docente lo hubiera elegido.
    puntaje = serializers.DecimalField(
        max_digits=5,
        decimal_places=2,
        min_value=Decimal(0),
        error_messages={
            "required": "Escribe cuántos puntos vale este problema.",
            "null": "Escribe cuántos puntos vale este problema.",
            "invalid": "Los puntos deben ser un número.",
            "min_value": "Los puntos no pueden ser negativos.",
            "max_digits": MENSAJE_RANGO_PUNTAJE,
            "max_whole_digits": MENSAJE_RANGO_PUNTAJE,
            "max_decimal_places": "Los puntos admiten hasta 2 decimales.",
        },
    )


class EvaluacionSerializer(serializers.ModelSerializer):
    # Se calcula al responder (HU64 T1); no es una columna ni se acepta en el cuerpo.
    estado = serializers.SerializerMethodField()
    # Los problemas que contiene, cada uno con su puntaje: [{"problema": 12, "puntaje": 30}] (HU62, HU63-T2). Se guardan
    # en EvaluacionProblema. Con many=True, un error de un problema sale por su posición en la lista (desde 0) y solo
    # aparecen las posiciones con error: {"problemas": {"1": {"puntaje": ["..."]}}}. La lista vacía se revisa en
    # validate_problemas: así su mensaje sale como los demás de la lista, y no dentro de "non_field_errors".
    problemas = ProblemaDeEvaluacionSerializer(
        many=True,
        write_only=True,
        error_messages={"required": MENSAJE_SIN_PROBLEMAS},
    )

    class Meta:
        model = Evaluacion
        # curso no se acepta en el cuerpo: sale de la ruta, y solo si es del docente del token.
        fields = ["id", "curso", "fecha_inicio", "fecha_fin", "limite_minutos", "problemas", "estado"]
        read_only_fields = ["curso"]
        # Un límite de 0 minutos dejaría la evaluación sin tiempo para resolverla.
        extra_kwargs = {"limite_minutos": {"min_value": 1}}

    def get_estado(self, evaluacion):
        return evaluacion.estado()

    # HU61 T4 (CA-61.4): la ventana debe tener duración. Con el fin igual al inicio la evaluación nunca estaría activa.
    def validate(self, datos):
        if datos["fecha_fin"] <= datos["fecha_inicio"]:
            raise serializers.ValidationError({"fecha_fin": "La fecha de fin debe ser posterior a la de inicio."})
        return datos

    # Cada problema debe existir, ser del curso de la evaluación y estar aprobado y no descartado (los mismos que ve un
    # estudiante). Un solo mensaje para los tres motivos: no se revela si un id existe en un curso que no es del docente.
    def validate_problemas(self, items):
        if not items:
            raise serializers.ValidationError(MENSAJE_SIN_PROBLEMAS)
        ids = [item["problema_id"] for item in items]
        if len(set(ids)) != len(ids):
            raise serializers.ValidationError("No repitas problemas en la evaluación.")
        validos = set(
            Problema.objects.visibles_para_estudiante()
            .filter(curso=self.context["curso"], pk__in=ids)
            .values_list("pk", flat=True)
        )
        invalidos = [str(i) for i in ids if i not in validos]
        if invalidos:
            raise serializers.ValidationError(
                f"Estos problemas no existen, no son de este curso o no están aprobados: {', '.join(invalidos)}."
            )
        return items

    # La evaluación y sus problemas se guardan juntas o no se guarda nada.
    def create(self, validated_data):
        items = validated_data.pop("problemas")
        with transaction.atomic():
            evaluacion = super().create(validated_data)
            EvaluacionProblema.objects.bulk_create(
                [
                    EvaluacionProblema(evaluacion=evaluacion, problema_id=item["problema_id"], puntaje=item["puntaje"])
                    for item in items
                ]
            )
        return evaluacion

    # Los problemas salen en el orden en que se guardaron (el del cuerpo), con el puntaje como texto de 2 decimales.
    def to_representation(self, evaluacion):
        datos = super().to_representation(evaluacion)
        filas = EvaluacionProblema.objects.filter(evaluacion=evaluacion).order_by("pk")
        datos["problemas"] = ProblemaDeEvaluacionSerializer(filas, many=True).data
        return datos
