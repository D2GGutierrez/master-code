"""Prueba el prompt de generación con el proveedor de LLM configurado y valida cada respuesta.

Usa la misma capa que POST /generate, así que lee LLM_PROVIDER, LLM_API_KEY y LLM_MODEL del entorno
(la clave nunca se imprime). Uso, desde ai-service/:
    LLM_PROVIDER=gemini LLM_API_KEY=... python -m scripts.probar_prompt
    LLM_PROVIDER=groq LLM_API_KEY=... python -m scripts.probar_prompt --plataforma ambas --cantidad 3
La clave real va solo en tu entorno local. Con mock (el valor por defecto) no se llama a ningún LLM.
"""

import argparse
import sys

from app import config
from app.generacion.esquema import ErrorDeFormato, Solicitud, validar_respuesta
from app.providers.llm.base import LLMNoDisponible
from app.providers.llm.seleccion import proveedor_activo

TEMAS = [
    ("bucles for", "facil", "python"),
    ("recursividad", "medio", "java"),
    ("arreglos y recorridos", "facil", "javascript"),
    ("cadenas de texto", "medio", "python"),
    ("ordenamiento", "dificil", "java"),
]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--plataforma", default="web", choices=["web", "movil", "ambas"])
    parser.add_argument("--cantidad", type=int, default=2)
    argumentos = parser.parse_args()

    nombre = config.proveedor_llm()
    modelo = config.modelo_llm()
    print(f"Proveedor: {nombre}" + (f" (modelo {modelo})" if modelo else ""))
    if nombre == "mock":
        print("Aviso: mock no llama a ningún LLM; define LLM_PROVIDER para probar el prompt de verdad.")
    proveedor = proveedor_activo()

    validas = 0
    for tema, nivel, lenguaje in TEMAS:
        solicitud = Solicitud(
            tema=tema, nivel=nivel, lenguaje=lenguaje, cantidad=argumentos.cantidad, plataforma=argumentos.plataforma
        )
        try:
            validar_respuesta(proveedor.generar(solicitud), solicitud)
            print(f"OK    {tema} ({nivel}, {lenguaje})")
            validas += 1
        except ErrorDeFormato as error:
            print(f"MAL   {tema}: {error}")
        except LLMNoDisponible as error:
            # El mensaje ya viene sin la clave ni el cuerpo de la respuesta (ver LLMNoDisponible).
            print(f"ERROR {tema}: {error}")

    print(f"{validas} de {len(TEMAS)} respuestas válidas")
    return 0 if validas == len(TEMAS) else 1


if __name__ == "__main__":
    sys.exit(main())
