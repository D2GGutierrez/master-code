from django.db import transaction
from rest_framework import serializers

from cursos.serializers import LENGUAJES
from problemas.models import CasoPrueba, OpcionRespuesta, Problema

# Única lista de dificultades permitidas; el modelo guarda texto libre.
DIFICULTADES = ["facil", "medio", "dificil"]

# Mismo tope que ai-service (esquema.Solicitud): una solicitud mayor se rechaza antes de gastar la IA.
MAX_CANTIDAD = 10

# Los tipos que se corrigen comparando con un texto exacto (los demás usan casos de prueba o alternativas).
RESPUESTA_ESPERADA_EN = (Problema.TipoEjercicio.PREDICCION_SALIDA, Problema.TipoEjercicio.COMPLETAR_ESPACIO)

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


class GenerarSerializer(serializers.Serializer):
    # Lo que pide el docente al generar (HU17 T4): nombres de la historia, no los del modelo (nivel = dificultad).
    tema = serializers.CharField(max_length=200)
    nivel = serializers.ChoiceField(choices=DIFICULTADES)
    # Si no se indica, el del curso.
    lenguaje = serializers.ChoiceField(choices=LENGUAJES, required=False)
    cantidad = serializers.IntegerField(min_value=1, max_value=MAX_CANTIDAD)
    # Sin selector en el formulario de HU17: por defecto genera para las dos plataformas.
    plataforma = serializers.ChoiceField(choices=Problema.Plataforma.values, default=Problema.Plataforma.AMBAS)


class CasoPruebaSerializer(serializers.ModelSerializer):
    class Meta:
        model = CasoPrueba
        # problema no va: lo asigna ProblemaSerializer.create con el problema recién creado.
        fields = ["id", "entrada", "salida_esperada", "es_publico"]


class OpcionRespuestaSerializer(serializers.ModelSerializer):
    class Meta:
        model = OpcionRespuesta
        # problema no va: lo asigna ProblemaSerializer.create con el problema recién creado.
        fields = ["id", "texto", "es_correcta"]


class ProblemaSerializer(serializers.ModelSerializer):
    lenguaje = serializers.ChoiceField(choices=LENGUAJES)
    dificultad = serializers.ChoiceField(choices=DIFICULTADES)
    casos_prueba = CasoPruebaSerializer(many=True, required=False)
    opciones = OpcionRespuestaSerializer(many=True, required=False)

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
            "opciones",
            "respuesta_esperada",
            "generado_por_ia",
            "aprobado_por_docente",
            "fecha_aprobacion",
            "archivado",
        ]
        # curso sale de la URL en la vista: si el cuerpo lo trae, se ignora. Lo demás lo deciden las
        # acciones (generar, aprobar, descartar) y nunca el cuerpo: así nadie se aprueba a sí mismo.
        read_only_fields = ["curso", "generado_por_ia", "aprobado_por_docente", "fecha_aprobacion", "archivado"]

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
        # Cada dato de respuesta va con su tipo. Que estén completos no se exige aquí (se puede guardar
        # un borrador): se exige al aprobar, ver problemas/completitud.py.
        if attrs.get("opciones") and attrs["tipo_ejercicio"] != Problema.TipoEjercicio.OPCION_MULTIPLE:
            raise serializers.ValidationError({"opciones": "Solo los ejercicios de opción múltiple llevan alternativas."})
        if attrs.get("respuesta_esperada") and attrs["tipo_ejercicio"] not in RESPUESTA_ESPERADA_EN:
            raise serializers.ValidationError(
                {"respuesta_esperada": "Solo los ejercicios de predicción de salida y de completar el espacio llevan respuesta esperada."}
            )
        return attrs

    # atomic: si falla un caso o una alternativa, tampoco queda el problema a medias.
    @transaction.atomic
    def create(self, validated_data):
        casos = validated_data.pop("casos_prueba", [])
        opciones = validated_data.pop("opciones", [])
        problema = Problema.objects.create(**validated_data)
        for caso in casos:
            CasoPrueba.objects.create(problema=problema, **caso)
        for opcion in opciones:
            OpcionRespuesta.objects.create(problema=problema, **opcion)
        return problema


# Bandeja de revisión (HU18): el problema completo, para revisarlo sin otra consulta, y el nombre del curso,
# porque la bandeja mezcla problemas de varios cursos. generado_por_ia es la marca «generado por IA» (CA-18.2).
class ProblemaPendienteSerializer(ProblemaSerializer):
    curso_nombre = serializers.CharField(source="curso.nombre", read_only=True)

    class Meta(ProblemaSerializer.Meta):
        fields = [*ProblemaSerializer.Meta.fields, "curso_nombre"]


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
