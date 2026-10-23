import os

PROVEEDORES = ("mock", "gemini", "groq")
# Modelo que se usa si LLM_MODEL no está definida. Gemini usa un alias que Google mantiene apuntando a su
# Flash vigente, para que el servicio no se rompa cuando retiran un modelo concreto.
MODELO_POR_DEFECTO = {"gemini": "gemini-flash-latest", "groq": "llama-3.3-70b-versatile"}
# Motores que ejecutan el código de los estudiantes (HU08). mock no ejecuta nada: es para CI y desarrollo.
PROVEEDORES_SANDBOX = ("mock", "judge0", "piston")


# Se lee del entorno en cada llamada y no al importar: así las pruebas pueden cambiar las variables.
def proveedor_llm() -> str:
    valor = os.environ.get("LLM_PROVIDER", "mock").strip().lower() or "mock"
    if valor == "github":
        raise ValueError("LLM_PROVIDER=github ya no existe: GitHub Models fue retirado. Usa mock, gemini o groq.")
    if valor not in PROVEEDORES:
        raise ValueError(f"LLM_PROVIDER debe ser uno de {', '.join(PROVEEDORES)}")
    return valor


# Qué motor de ejecución se usa (HU08-T3): una sola variable, sin tocar los routers.
def proveedor_sandbox() -> str:
    valor = os.environ.get("SANDBOX_PROVIDER", "mock").strip().lower() or "mock"
    if valor not in PROVEEDORES_SANDBOX:
        raise ValueError(f"SANDBOX_PROVIDER debe ser uno de {', '.join(PROVEEDORES_SANDBOX)}")
    return valor


# Nombres del servicio dentro de la red de Docker (docker-compose.yml).
def url_judge0() -> str:
    return os.environ.get("JUDGE0_URL", "").strip() or "http://judge0-server:2358"


def url_piston() -> str:
    return os.environ.get("PISTON_URL", "").strip() or "http://piston:2000"


# La clave nunca se imprime ni se devuelve en una respuesta: solo se lee aquí.
def clave_llm() -> str:
    return os.environ.get("LLM_API_KEY", "")


# Modelo del proveedor activo. Los nombres de modelo cambian con frecuencia: por eso se puede
# reemplazar con LLM_MODEL sin tocar el código.
def modelo_llm() -> str:
    return os.environ.get("LLM_MODEL", "").strip() or MODELO_POR_DEFECTO.get(proveedor_llm(), "")


# Mensaje para el arranque; devuelve None si la configuración está completa.
def problema_de_configuracion() -> str | None:
    # Un SANDBOX_PROVIDER mal escrito falla al arrancar (ValueError), no con la primera ejecución.
    proveedor_sandbox()
    proveedor = proveedor_llm()
    if proveedor != "mock" and not clave_llm():
        return f"LLM_PROVIDER={proveedor} necesita LLM_API_KEY; sin clave solo sirve el proveedor mock."
    return None
