"""Prueba el prompt de generación con un LLM real (solo Gemini) y valida cada respuesta.

Uso, desde ai-service/, con LLM_API_KEY en el entorno (nunca se imprime):
    python -m scripts.probar_prompt
    python -m scripts.probar_prompt --modelo gemini-2.5-flash --plataforma ambas
La clave real va solo en tu entorno local; HU17-T3 hará la capa de proveedores definitiva.
"""

import argparse
import json
import sys
import urllib.error
import urllib.request

from app.config import clave_llm
from app.generacion.esquema import ErrorDeFormato, Solicitud, validar_respuesta
from app.generacion.prompt import construir_prompt

TEMAS = [
    ("bucles for", "facil", "python"),
    ("recursividad", "medio", "java"),
    ("arreglos y recorridos", "facil", "javascript"),
    ("cadenas de texto", "medio", "python"),
    ("ordenamiento", "dificil", "java"),
]


def preguntar(prompt: str, modelo: str, clave: str) -> str:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent"
    cuerpo = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"responseMimeType": "application/json"},
    }
    peticion = urllib.request.Request(
        url,
        data=json.dumps(cuerpo).encode(),
        headers={"Content-Type": "application/json", "x-goog-api-key": clave},
        method="POST",
    )
    with urllib.request.urlopen(peticion, timeout=120) as respuesta:
        datos = json.loads(respuesta.read())
    return datos["candidates"][0]["content"]["parts"][0]["text"]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--modelo", default="gemini-2.5-flash")
    parser.add_argument("--plataforma", default="web", choices=["web", "movil", "ambas"])
    parser.add_argument("--cantidad", type=int, default=2)
    argumentos = parser.parse_args()

    clave = clave_llm()
    if not clave:
        print("Falta LLM_API_KEY en el entorno.")
        return 2

    validas = 0
    for tema, nivel, lenguaje in TEMAS:
        solicitud = Solicitud(
            tema=tema, nivel=nivel, lenguaje=lenguaje, cantidad=argumentos.cantidad, plataforma=argumentos.plataforma
        )
        try:
            texto = preguntar(construir_prompt(solicitud), argumentos.modelo, clave)
            validar_respuesta(texto, solicitud)
            print(f"OK    {tema} ({nivel}, {lenguaje})")
            validas += 1
        except ErrorDeFormato as error:
            print(f"MAL   {tema}: {error}")
        except urllib.error.HTTPError as error:
            # Solo el código HTTP: el cuerpo del error no se imprime por si repite datos de la petición.
            print(f"ERROR {tema}: el proveedor respondió HTTP {error.code}")
        except (urllib.error.URLError, KeyError, IndexError, json.JSONDecodeError) as error:
            print(f"ERROR {tema}: {type(error).__name__}")

    print(f"{validas} de {len(TEMAS)} respuestas válidas")
    return 0 if validas == len(TEMAS) else 1


if __name__ == "__main__":
    sys.exit(main())
