from rest_framework import serializers

from cursos.models import Curso

# Única lista de lenguajes permitidos; el modelo guarda texto libre.
LENGUAJES = ["python", "java", "javascript"]


class CursoSerializer(serializers.ModelSerializer):
    lenguaje = serializers.ChoiceField(choices=LENGUAJES)

    class Meta:
        model = Curso
        # docente no está aquí: se toma del token en la vista. id y fecha_creacion son de solo lectura.
        fields = ["id", "nombre", "lenguaje", "fecha_creacion"]
