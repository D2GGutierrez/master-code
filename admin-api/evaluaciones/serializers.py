from rest_framework import serializers

from evaluaciones.models import Evaluacion


class EvaluacionSerializer(serializers.ModelSerializer):
    # Se calcula al responder (HU64 T1); no es una columna ni se acepta en el cuerpo.
    estado = serializers.SerializerMethodField()

    class Meta:
        model = Evaluacion
        # curso no se acepta en el cuerpo: sale de la ruta, y solo si es del docente del token.
        fields = ["id", "curso", "fecha_inicio", "fecha_fin", "limite_minutos", "estado"]
        read_only_fields = ["curso"]
        # Un límite de 0 minutos dejaría la evaluación sin tiempo para resolverla.
        extra_kwargs = {"limite_minutos": {"min_value": 1}}

    def get_estado(self, evaluacion):
        return evaluacion.estado()
