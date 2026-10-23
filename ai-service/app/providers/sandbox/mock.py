from app.providers.sandbox.base import (
    Ejecucion,
    Estado,
    LenguajeNoSoportado,
    ProveedorSandbox,
    Resultado,
)

# Mismos lenguajes que Judge0: el mock se comporta como un motor real para quien lo usa.
LENGUAJES = ("python", "java", "javascript")

# Si el código contiene uno de estos textos, el mock simula ese resultado. Así quien construye una
# pantalla (Carlos, Gael) o una prueba puede provocar cada estado sin tener ningún motor instalado.
# Se revisan en este orden. El texto puede ir en un comentario del código.
MARCADORES = {
    "MOCK_ERROR_SERVICIO": (Estado.ERROR_SERVICIO, "El motor de ejecución simulado no respondió."),
    "MOCK_ERROR_COMPILACION": (Estado.ERROR_COMPILACION, "error de compilación simulado"),
    "MOCK_TIEMPO_AGOTADO": (Estado.TIEMPO_AGOTADO, "Tiempo límite excedido (simulado)."),
    "MOCK_ERROR_EJECUCION": (Estado.ERROR_EJECUCION, "error de ejecución simulado"),
}


class Mock(ProveedorSandbox):
    """No ejecuta nada: responde siempre lo mismo para la misma entrada (CI y desarrollo sin motor).

    Sin marcador, el programa «imprime lo que recibe»: la salida es igual a la entrada.
    """

    def ejecutar(self, ejecucion: Ejecucion) -> Resultado:
        if ejecucion.lenguaje.lower() not in LENGUAJES:
            raise LenguajeNoSoportado(ejecucion.lenguaje)

        for marcador, (estado, mensaje) in MARCADORES.items():
            if marcador in ejecucion.codigo:
                return Resultado(estado, error=mensaje, tiempo_ms=1)
        return Resultado(Estado.OK, salida=ejecucion.entrada, tiempo_ms=1)
