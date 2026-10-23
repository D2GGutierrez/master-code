import json

from app.generacion.esquema import TIPOS_POR_PLATAFORMA, Solicitud
from app.providers.llm.base import ProveedorLLM

# Un fragmento mínimo por lenguaje: imprime 5 (2 + 3).
_IMPRIMIR = {
    "python": "print(2 + 3)",
    "java": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(2 + 3);\n    }\n}",
    "javascript": "console.log(2 + 3);",
}
_HUECO = {
    "python": "print(2 ____ 3)",
    "java": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(2 ____ 3);\n    }\n}",
    "javascript": "console.log(2 ____ 3);",
}


# Responde siempre lo mismo para la misma solicitud y siempre con el formato correcto: sirve para
# desarrollar admin-web y admin-api, y para el CI, sin clave ni red (HU17-T3). No sabe de programación:
# los problemas son de relleno y llevan «[Mock]» para que nadie los confunda con los de un LLM.
class Mock(ProveedorLLM):
    def generar(self, solicitud: Solicitud) -> str:
        tipos = TIPOS_POR_PLATAFORMA[solicitud.plataforma]
        problemas = [
            _problema(tipos[indice % len(tipos)], indice + 1, solicitud) for indice in range(solicitud.cantidad)
        ]
        return json.dumps({"problemas": problemas}, ensure_ascii=False)


def _problema(tipo: str, numero: int, solicitud: Solicitud) -> dict:
    # Los 150 caracteres son el tope de titulo; el tema puede traer hasta 200.
    problema = {"titulo": f"[Mock] {numero}. {solicitud.tema}"[:150], "tipo_ejercicio": tipo}
    lenguaje = solicitud.lenguaje
    if tipo == "escritura_codigo":
        problema["enunciado"] = (
            f"[Mock] Escribe un programa en {lenguaje} que lea un número entero y muestre su doble. "
            f"Tema: {solicitud.tema}."
        )
        problema["casos_prueba"] = [
            {"entrada": "2", "salida_esperada": "4", "es_publico": True},
            {"entrada": "10", "salida_esperada": "20", "es_publico": True},
            {"entrada": "0", "salida_esperada": "0", "es_publico": False},
        ]
    elif tipo == "prediccion_salida":
        problema["enunciado"] = f"[Mock] ¿Qué imprime este código en {lenguaje}?\n\n{_IMPRIMIR[lenguaje]}"
        problema["respuesta_esperada"] = "5"
    elif tipo == "opcion_multiple":
        problema["enunciado"] = f"[Mock] ¿Qué imprime este código en {lenguaje}?\n\n{_IMPRIMIR[lenguaje]}"
        problema["opciones"] = [
            {"texto": "4", "es_correcta": False},
            {"texto": "5", "es_correcta": True},
            {"texto": "6", "es_correcta": False},
            {"texto": "23", "es_correcta": False},
        ]
    else:  # completar_espacio
        problema["enunciado"] = f"[Mock] Completa el hueco para que el código imprima 5.\n\n{_HUECO[lenguaje]}"
        problema["respuesta_esperada"] = "+"
    return problema
