from django.db import transaction
from rest_framework import serializers

from evaluaciones.models import Evaluacion, EvaluacionProblema
from problemas.models import Problema


class EvaluacionSerializer(serializers.ModelSerializer):
    # Se calcula al responder (HU64 T1); no es una columna ni se acepta en el cuerpo.
    estado = serializers.SerializerMethodField()
    # Los ids de los problemas que contiene (HU62). Se guardan en EvaluacionProblema; la respuesta los devuelve aparte.
    problemas = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        allow_empty=False,
        write_only=True,
        error_messages={
            "required": "Elige al menos un problema para la evaluación.",
            "empty": "Elige al menos un problema para la evaluación.",
        },
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
    def validate_problemas(self, ids):
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
        return ids

    # La evaluación y sus problemas se guardan juntas o no se guarda nada.
    def create(self, validated_data):
        ids = validated_data.pop("problemas")
        with transaction.atomic():
            evaluacion = super().create(validated_data)
            EvaluacionProblema.objects.bulk_create(
                [EvaluacionProblema(evaluacion=evaluacion, problema_id=i) for i in ids]
            )
        return evaluacion

    def to_representation(self, evaluacion):
        datos = super().to_representation(evaluacion)
        datos["problemas"] = list(
            EvaluacionProblema.objects.filter(evaluacion=evaluacion).order_by("pk").values_list("problema_id", flat=True)
        )
        return datos
