from rest_framework import serializers

from cursos.models import Curso

# Única lista de lenguajes permitidos; el modelo guarda texto libre.
LENGUAJES = ["python", "java", "javascript"]


class CursoSerializer(serializers.ModelSerializer):
    lenguaje = serializers.ChoiceField(choices=LENGUAJES)

    class Meta:
        model = Curso
        # docente no está aquí: se toma del token en la vista. id y fecha_creacion son de solo lectura.
        fields = ["id", "nombre", "lenguaje", "fecha_creacion", "codigo_invitacion", "codigo_vence"]
        # El código lo genera el sistema; el docente solo lo lee y lo comparte.
        read_only_fields = ["codigo_invitacion", "codigo_vence"]


class CursoListaSerializer(serializers.ModelSerializer):
    # Viene del annotate de la vista, no es una columna del modelo.
    cantidad_problemas = serializers.IntegerField(read_only=True)

    class Meta:
        model = Curso
        fields = ["id", "nombre", "lenguaje", "fecha_creacion", "cantidad_problemas", "codigo_invitacion", "codigo_vence"]
