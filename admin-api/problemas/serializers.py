from django.db import transaction
from rest_framework import serializers

from cursos.serializers import LENGUAJES
from problemas.models import CasoPrueba, Problema

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


class CasoPruebaSerializer(serializers.ModelSerializer):
    class Meta:
        model = CasoPrueba
        # problema no va: lo asigna ProblemaSerializer.create con el problema recién creado.
        fields = ["id", "entrada", "salida_esperada", "es_publico"]


class ProblemaSerializer(serializers.ModelSerializer):
    lenguaje = serializers.ChoiceField(choices=LENGUAJES)
    dificultad = serializers.ChoiceField(choices=DIFICULTADES)
    casos_prueba = CasoPruebaSerializer(many=True, required=False)

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
            "casos_prueba",
        ]
        # curso sale de la URL en la vista: si el cuerpo lo trae, se ignora.
        read_only_fields = ["curso"]

    def validate(self, attrs):
        # Solo llega aquí si los dos campos ya son valores válidos de su lista.
        mensaje = COMBINACIONES_IMPOSIBLES.get((attrs["plataforma"], attrs["tipo_ejercicio"]))
        if mensaje:
            # El error va en plataforma para que el formulario lo muestre bajo ese selector.
            raise serializers.ValidationError({"plataforma": mensaje})
        if attrs["tipo_ejercicio"] == Problema.TipoEjercicio.ESCRITURA_CODIGO and not attrs.get("casos_prueba"):
            raise serializers.ValidationError(
                {
                    "casos_prueba": "Agrega al menos un caso de prueba: los problemas de escritura "
                    "de código se califican con casos de prueba."
                }
            )
        return attrs

    # atomic: si falla un caso, tampoco queda el problema sin sus casos.
    @transaction.atomic
    def create(self, validated_data):
        casos = validated_data.pop("casos_prueba", [])
        problema = Problema.objects.create(**validated_data)
        for caso in casos:
            CasoPrueba.objects.create(problema=problema, **caso)
        return problema


# Vista del estudiante (CA-012.4). Todavía no los usa ninguna ruta: son para la futura API del
# estudiante (HU25/HU32), que debe usarlos a ellos y no ProblemaSerializer, que expone los ocultos.
# No borrarlos por parecer código sin uso.
class CasoPruebaEstudianteSerializer(serializers.ModelSerializer):
    # Sin es_publico ni id: al estudiante solo le llegan casos públicos y no necesita saber más.
    class Meta:
        model = CasoPrueba
        fields = ["entrada", "salida_esperada"]


class ProblemaEstudianteSerializer(serializers.ModelSerializer):
    # Sin curso ni fecha_creacion: son datos internos que el estudiante no necesita para resolver.
    casos_prueba = serializers.SerializerMethodField()

    class Meta:
        model = Problema
        fields = [
            "id",
            "titulo",
            "enunciado",
            "lenguaje",
            "dificultad",
            "plataforma",
            "tipo_ejercicio",
            "casos_prueba",
        ]

    def get_casos_prueba(self, problema):
        # Único lugar donde se filtra: los ocultos solo sirven para calificar y nunca salen.
        publicos = problema.casos_prueba.filter(es_publico=True)
        return CasoPruebaEstudianteSerializer(publicos, many=True).data
