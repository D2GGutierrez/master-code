from app import config
from app.providers.sandbox.base import ProveedorSandbox
from app.providers.sandbox.judge0 import Judge0
from app.providers.sandbox.mock import Mock
from app.providers.sandbox.piston import Piston


# Único lugar que conoce a todos los proveedores: los routers piden «el activo» y no saben cuál es.
# Cambiar de motor es cambiar SANDBOX_PROVIDER (HU08-T3).
def proveedor_activo() -> ProveedorSandbox:
    nombre = config.proveedor_sandbox()
    if nombre == "judge0":
        return Judge0(config.url_judge0())
    if nombre == "piston":
        return Piston(config.url_piston())
    return Mock()
