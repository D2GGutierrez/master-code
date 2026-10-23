from app import config
from app.providers.llm.base import ProveedorLLM
from app.providers.llm.gemini import Gemini
from app.providers.llm.mock import Mock
from app.providers.llm.openai_compatible import CompatibleOpenAI

URL_GROQ = "https://api.groq.com/openai/v1/chat/completions"


# Único lugar que conoce a todos los proveedores de LLM: el router pide «el activo» y no sabe cuál es.
# Cambiar de proveedor es cambiar LLM_PROVIDER (y LLM_API_KEY).
def proveedor_activo() -> ProveedorLLM:
    nombre = config.proveedor_llm()
    if nombre == "gemini":
        return Gemini(config.clave_llm(), config.modelo_llm())
    if nombre == "groq":
        return CompatibleOpenAI("Groq", URL_GROQ, config.clave_llm(), config.modelo_llm())
    return Mock()
