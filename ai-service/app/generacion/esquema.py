import json
import re
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, ValidationError, model_validator

# Mismas listas que admin-api (cursos/serializers.py y problemas/models.py): si cambian allá, cambian aquí.
LENGUAJES = ("python", "java", "javascript")
NIVELES = ("facil", "medio", "dificil")
PLATAFORMAS = ("web", "movil", "ambas")
TIPOS = ("escritura_codigo", "prediccion_salida", "opcion_multiple", "completar_espacio")

# Mismas combinaciones que acepta admin-api: web solo escribe código, móvil nunca lo ejecuta.
TIPOS_POR_PLATAFORMA = {"web": TIPOS[:1], "movil": TIPOS[1:], "ambas": TIPOS}


class ErrorDeFormato(ValueError):
    """La respuesta del LLM no cumple el formato fijo; el mensaje dice qué falló."""


class Solicitud(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    tema: str = Field(min_length=1, max_length=200)
    nivel: Literal["facil", "medio", "dificil"]
    lenguaje: Literal["python", "java", "javascript"]
    cantidad: int = Field(ge=1, le=10)
    plataforma: Literal["web", "movil", "ambas"]


class CasoPrueba(BaseModel):
    entrada: str = ""
    salida_esperada: str = Field(min_length=1)
    es_publico: bool


class Opcion(BaseModel):
    texto: str = Field(min_length=1)
    es_correcta: bool


# nivel y lenguaje no vienen del LLM: se toman de la solicitud, para no depender de que los repita bien.
class ProblemaGenerado(BaseModel):
    titulo: str = Field(min_length=1, max_length=150)
    enunciado: str = Field(min_length=1)
    tipo_ejercicio: Literal["escritura_codigo", "prediccion_salida", "opcion_multiple", "completar_espacio"]
    casos_prueba: list[CasoPrueba] = []
    opciones: list[Opcion] = []
    respuesta_esperada: str = ""

    @model_validator(mode="after")
    def _datos_segun_el_tipo(self):
        # Cada tipo lleva su dato de calificación y ninguno de los de otros tipos.
        tipo = self.tipo_ejercicio
        if tipo == "escritura_codigo":
            if not self.casos_prueba:
                raise ValueError("escritura_codigo necesita al menos un caso de prueba")
        elif self.casos_prueba:
            raise ValueError(f"{tipo} no lleva casos de prueba")

        if tipo == "opcion_multiple":
            if not 2 <= len(self.opciones) <= 6:
                raise ValueError("opcion_multiple necesita entre 2 y 6 opciones")
            if sum(o.es_correcta for o in self.opciones) != 1:
                raise ValueError("opcion_multiple necesita exactamente una opción correcta")
        elif self.opciones:
            raise ValueError(f"{tipo} no lleva opciones")

        if tipo in ("prediccion_salida", "completar_espacio"):
            if not self.respuesta_esperada.strip():
                raise ValueError(f"{tipo} necesita respuesta_esperada")
        elif self.respuesta_esperada:
            raise ValueError(f"{tipo} no lleva respuesta_esperada")
        return self


class RespuestaIA(BaseModel):
    problemas: list[ProblemaGenerado]


# Algunos modelos envuelven el JSON en ```json ... ``` aunque se les pida que no.
_CERCA = re.compile(r"```(?:json)?\s*(.*?)```", re.DOTALL)


def validar_respuesta(texto: str, solicitud: Solicitud) -> list[ProblemaGenerado]:
    """Convierte el texto del LLM en problemas válidos o lanza ErrorDeFormato."""
    coincidencia = _CERCA.search(texto)
    contenido = (coincidencia.group(1) if coincidencia else texto).strip()
    try:
        datos = json.loads(contenido)
    except json.JSONDecodeError as error:
        raise ErrorDeFormato(f"La respuesta no es JSON válido: {error.msg}") from error

    try:
        problemas = RespuestaIA.model_validate(datos).problemas
    except ValidationError as error:
        primero = error.errors()[0]
        lugar = ".".join(str(parte) for parte in primero["loc"])
        raise ErrorDeFormato(f"Formato inválido en {lugar}: {primero['msg']}") from error

    if len(problemas) != solicitud.cantidad:
        raise ErrorDeFormato(f"Se pidieron {solicitud.cantidad} problemas y llegaron {len(problemas)}")
    permitidos = TIPOS_POR_PLATAFORMA[solicitud.plataforma]
    for problema in problemas:
        if problema.tipo_ejercicio not in permitidos:
            raise ErrorDeFormato(
                f"El tipo {problema.tipo_ejercicio} no sirve para la plataforma {solicitud.plataforma}"
            )
    return problemas
