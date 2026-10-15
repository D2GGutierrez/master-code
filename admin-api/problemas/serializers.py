from rest_framework import serializers

from cursos.serializers import LENGUAJES
from problemas.models import Problema

# Única lista de dificultades permitidas; el modelo guarda texto libre.
DIFICULTADES = ["facil", "medio", "dificil"]

SOLO_CELULAR = (
    "Este tipo de ejercicio se responde en el celular: elige la plataforma móvil o ambas, "
    "o cambia el tipo a escritura de código."
)
# (plataforma, tipo_ejercicio) que no se pueden publicar, con el motivo y cómo corregirlo.
# Cualquier otra combinación, incluida ambas con cualquier tipo, se acepta.
COMBINACIONES_IMPOSIBLES = {
    ("movil", "escritura_codigo"): (
        "El celular no ejecuta código: elige la plataforma web o ambas, o cambia el tipo de ejercicio."
    ),
    ("web", "prediccion_salida"): SOLO_CELULAR,
    ("web", "opcion_multiple"): SOLO_CELULAR,
    ("web", "completar_espacio"): SOLO_CELULAR,
}


class ProblemaSerializer(serializers.ModelSerializer):
    lenguaje = serializers.ChoiceField(choices=LENGUAJES)
    dificultad = serializers.ChoiceField(choices=DIFICULTADES)

    class Meta:
        model = Problema
        # plataforma y tipo_ejercicio se validan con los choices del modelo.
        fields = [
            "id",
            "curso",
            "titulo",
            "enunciado",
            "lenguaje",
            "dificultad",
            "plataforma",
            "tipo_ejercicio",
            "fecha_creacion",
        ]
        # curso sale de la URL en la vista: si el cuerpo lo trae, se ignora.
        read_only_fields = ["curso"]

    def validate(self, attrs):
        # Solo llega aquí si los dos campos ya son valores válidos de su lista.
        mensaje = COMBINACIONES_IMPOSIBLES.get((attrs["plataforma"], attrs["tipo_ejercicio"]))
        if mensaje:
            # El error va en plataforma para que el formulario lo muestre bajo ese selector.
            raise serializers.ValidationError({"plataforma": mensaje})
        return attrs
