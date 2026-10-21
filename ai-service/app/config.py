import os

PROVEEDORES = ("mock", "gemini", "github", "groq")


# Se lee del entorno en cada llamada y no al importar: así las pruebas pueden cambiar las variables.
def proveedor_llm() -> str:
    valor = os.environ.get("LLM_PROVIDER", "mock").strip().lower() or "mock"
    if valor not in PROVEEDORES:
        raise ValueError(f"LLM_PROVIDER debe ser uno de {', '.join(PROVEEDORES)}")
    return valor


# La clave nunca se imprime ni se devuelve en una respuesta: solo se lee aquí.
def clave_llm() -> str:
    return os.environ.get("LLM_API_KEY", "")


# Mensaje para el arranque; devuelve None si la configuración está completa.
def problema_de_configuracion() -> str | None:
    proveedor = proveedor_llm()
    if proveedor != "mock" and not clave_llm():
        return f"LLM_PROVIDER={proveedor} necesita LLM_API_KEY; sin clave solo sirve el proveedor mock."
    return None
