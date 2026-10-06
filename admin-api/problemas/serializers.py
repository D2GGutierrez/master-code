from rest_framework import serializers

from cursos.serializers import LENGUAJES
from problemas.models import Problema

# Única lista de dificultades permitidas; el modelo guarda texto libre.
DIFICULTADES = ["facil", "medio", "dificil"]


class ProblemaSerializer(serializers.ModelSerializer):
    lenguaje = serializers.ChoiceField(choices=LENGUAJES)
    dificultad = serializers.ChoiceField(choices=DIFICULTADES)

    class Meta:
        model = Problema
        fields = ["id", "curso", "titulo", "enunciado", "lenguaje", "dificultad", "fecha_creacion"]
        # curso sale de la URL en la vista: si el cuerpo lo trae, se ignora.
        read_only_fields = ["curso"]
