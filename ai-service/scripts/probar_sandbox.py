"""Prueba el motor de ejecución real de punta a punta, pasando por POST /execute de ai-service.

Sirve para comprobar un Piston (o un Judge0) de verdad antes de una presentación: ejecuta programas que deben
funcionar y programas que el motor debe frenar (bucle infinito, memoria, red, bomba de procesos) y dice qué
estado devolvió cada uno. Solo usa la biblioteca estándar, así que corre con cualquier Python 3.10 o superior.

Antes: ai-service con SANDBOX_PROVIDER=piston (o judge0) y el motor con sus lenguajes instalados (docs/piston.md).
Uso, desde la raíz del repositorio:
    python ai-service/scripts/probar_sandbox.py
    python ai-service/scripts/probar_sandbox.py --url http://localhost:8002 --lenguajes python,javascript
Termina con código 0 si todo salió como se espera y con 1 si algo falló. Cada caso puede tardar hasta unos 15
segundos (los que deben frenarse esperan el límite de tiempo), así que la corrida completa tarda alrededor de un minuto.
"""

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from collections.abc import Callable
from dataclasses import dataclass

# Un caso que tarda más que esto no se frenó a tiempo: los límites del motor son de 5 y 10 segundos.
MAXIMO_SEGUNDOS = 30


@dataclass
class Caso:
    nombre: str
    lenguaje: str
    codigo: str
    entrada: str = ""
    # Estados que se aceptan en la respuesta; 400 en `http` es «lenguaje no soportado».
    estados: tuple[str, ...] = ("ok",)
    http: int = 200
    # Comprobación extra sobre la respuesta; devuelve el motivo del fallo, o None si está bien.
    comprobar: Callable[[dict], str | None] | None = None


def _salida_es(esperada: str) -> Callable[[dict], str | None]:
    def comprobar(resultado: dict) -> str | None:
        obtenida = resultado["salida"].strip()
        return None if obtenida == esperada else f"salida {obtenida[:60]!r}, esperaba {esperada!r}"

    return comprobar


def _error_contiene(texto: str) -> Callable[[dict], str | None]:
    def comprobar(resultado: dict) -> str | None:
        return None if texto in resultado["error"] else f"el error no menciona {texto!r}: {resultado['error'][:80]!r}"

    return comprobar


def _salida_no_contiene(texto: str) -> Callable[[dict], str | None]:
    def comprobar(resultado: dict) -> str | None:
        return f"la salida contiene {texto!r}" if texto in resultado["salida"] else None

    return comprobar


def _error_no_vacio(resultado: dict) -> str | None:
    return None if resultado["error"].strip() else "no devolvió el mensaje del error"


def _salida_larga(resultado: dict) -> str | None:
    largo = len(resultado["salida"].strip())
    return None if largo == 5000 else f"la salida llegó con {largo} caracteres de 5000 (¿PISTON_OUTPUT_MAX_SIZE?)"


PYTHON = [
    Caso("hola mundo", "python", 'print("hola mundo")', comprobar=_salida_es("hola mundo")),
    Caso("lee la entrada", "python", "print(int(input()) * 2)", entrada="21\n", comprobar=_salida_es("42")),
    Caso("salida de 5000 caracteres", "python", 'print("x" * 5000)', comprobar=_salida_larga),
    Caso(
        "error de ejecución",
        "python",
        'raise ValueError("boom")',
        estados=("error_ejecucion",),
        comprobar=_error_contiene("ValueError"),
    ),
    Caso("bucle infinito", "python", "while True:\n    pass", estados=("tiempo_agotado",)),
    Caso(
        "memoria sin límite",
        "python",
        'datos = []\nwhile True:\n    datos.append("a" * 10_000_000)',
        estados=("error_ejecucion", "tiempo_agotado"),
    ),
    Caso(
        "conexión a internet",
        "python",
        "import socket\n"
        "for ip in ('1.1.1.1', '8.8.8.8'):\n"
        "    try:\n"
        "        socket.create_connection((ip, 53), timeout=3).close()\n"
        "        print('CONECTO')\n"
        "    except OSError:\n"
        "        pass\n",
        estados=("ok", "error_ejecucion"),
        comprobar=_salida_no_contiene("CONECTO"),
    ),
    Caso(
        "bomba de procesos",
        "python",
        "import os\nwhile True:\n    os.fork()",
        estados=("error_ejecucion", "tiempo_agotado"),
    ),
]

JAVASCRIPT = [
    Caso("hola mundo", "javascript", 'console.log("hola mundo")', comprobar=_salida_es("hola mundo")),
    Caso(
        "lee la entrada",
        "javascript",
        "const t = require('fs').readFileSync(0, 'utf8').trim();\nconsole.log(Number(t) * 2);",
        entrada="21\n",
        comprobar=_salida_es("42"),
    ),
    Caso("bucle infinito", "javascript", "while (true) {}", estados=("tiempo_agotado",)),
]

JAVA = [
    Caso(
        "hola mundo",
        "java",
        'public class Main {\n    public static void main(String[] args) {\n        System.out.println("hola mundo");\n    }\n}',
        comprobar=_salida_es("hola mundo"),
    ),
    Caso(
        "lee la entrada",
        "java",
        "import java.util.Scanner;\n"
        "public class Main {\n"
        "    public static void main(String[] args) {\n"
        "        Scanner s = new Scanner(System.in);\n"
        "        System.out.println(s.nextInt() * 2);\n"
        "    }\n"
        "}",
        entrada="21\n",
        comprobar=_salida_es("42"),
    ),
    Caso(
        "error de compilación",
        "java",
        'public class Main {\n    public static void main(String[] args) {\n        System.out.println("falta el punto y coma")\n    }\n}',
        estados=("error_compilacion",),
        comprobar=_error_no_vacio,
    ),
    Caso(
        "bucle infinito",
        "java",
        "public class Main {\n    public static void main(String[] args) {\n        while (true) {}\n    }\n}",
        estados=("tiempo_agotado",),
    ),
]

SIN_SOPORTE = Caso("lenguaje que el motor no tiene", "cobol", "DISPLAY 'HOLA'.", http=400)

POR_LENGUAJE = {"python": PYTHON, "javascript": JAVASCRIPT, "java": JAVA}


def llamar(url: str, caso: Caso) -> tuple[int, dict]:
    cuerpo = json.dumps({"codigo": caso.codigo, "lenguaje": caso.lenguaje, "entrada": caso.entrada}).encode()
    peticion = urllib.request.Request(
        f"{url}/execute", data=cuerpo, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(peticion, timeout=60) as respuesta:
            return respuesta.status, json.loads(respuesta.read())
    except urllib.error.HTTPError as error:
        return error.code, {}


def evaluar(caso: Caso, estado_http: int, resultado: dict, segundos: float) -> str | None:
    if estado_http != caso.http:
        return f"HTTP {estado_http}, esperaba {caso.http}"
    if caso.http != 200:
        return None
    if resultado["estado"] == "error_servicio":
        return "el motor falló (estado error_servicio): revisa que el lenguaje esté instalado en el motor y los logs de ai-service"
    if resultado["estado"] not in caso.estados:
        return f"estado {resultado['estado']!r}, esperaba {' o '.join(caso.estados)}"
    if segundos > MAXIMO_SEGUNDOS:
        return f"tardó {segundos:.0f} s: no se frenó a tiempo"
    if caso.comprobar is not None:
        return caso.comprobar(resultado)
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description="Prueba el motor de ejecución a través de POST /execute.")
    parser.add_argument("--url", default=os.environ.get("AI_SERVICE_URL", "http://localhost:8002"))
    parser.add_argument("--lenguajes", default="python,javascript,java", help="los que tiene instalados el motor")
    argumentos = parser.parse_args()
    url = argumentos.url.rstrip("/")

    nombres = [nombre.strip() for nombre in argumentos.lenguajes.split(",") if nombre.strip()]
    desconocidos = [nombre for nombre in nombres if nombre not in POR_LENGUAJE]
    if desconocidos:
        print(f"Lenguajes que esta prueba no conoce: {', '.join(desconocidos)}. Usa python, javascript o java.")
        return 2

    try:
        with urllib.request.urlopen(f"{url}/health", timeout=5) as respuesta:
            respuesta.read()
    except (urllib.error.URLError, TimeoutError):
        print(f"ai-service no responde en {url}. Levántalo (docker compose up -d ai-service) o pasa --url.")
        return 2

    print(f"ai-service en {url}; lenguajes: {', '.join(nombres)}")
    casos = [caso for nombre in nombres for caso in POR_LENGUAJE[nombre]] + [SIN_SOPORTE]
    fallos = 0
    for caso in casos:
        inicio = time.monotonic()
        try:
            estado_http, resultado = llamar(url, caso)
            segundos = time.monotonic() - inicio
            motivo = evaluar(caso, estado_http, resultado, segundos)
            estado = resultado.get("estado", f"HTTP {estado_http}")
        except (urllib.error.URLError, TimeoutError, ValueError, KeyError) as error:
            segundos = time.monotonic() - inicio
            motivo = f"la llamada falló: {error}"
            estado = "-"
        etiqueta = "OK  " if motivo is None else "MAL "
        print(f"{etiqueta}  {caso.lenguaje}: {caso.nombre} [{estado}, {segundos:.1f} s]" + (f" -> {motivo}" if motivo else ""))
        fallos += motivo is not None

    print()
    if fallos:
        print(f"{fallos} de {len(casos)} casos no salieron como se espera. Pega esta salida al revisar (docs/piston.md, sección 5).")
        return 1
    print(f"Los {len(casos)} casos salieron como se espera.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
