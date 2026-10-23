from problemas.models import TipoEjercicio

MIN_OPCIONES = 2
MAX_OPCIONES = 6


# Qué le falta a un problema para poder calificarse. Una sola regla para tres lugares: guardar lo que
# genera la IA (HU17 T5), aprobar (HU19 T2, CA-19.6) y decidir si uno creado a mano nace aprobado.
# Devuelve (campo, mensaje) con lo primero que falta, o None si está completo. El campo es el mismo
# nombre que usa el formulario, para que el mensaje salga debajo del campo que hay que corregir.
# Recibe valores simples y no un Problema: sirve antes de guardar, con lo que viene de la IA.
def faltante(tipo, casos_prueba, opciones, respuesta_esperada):
    if tipo == TipoEjercicio.ESCRITURA_CODIGO:
        if not casos_prueba:
            return (
                "casos_prueba",
                "Agrega al menos un caso de prueba: los problemas de escritura de código se califican con casos de prueba.",
            )
    elif tipo == TipoEjercicio.OPCION_MULTIPLE:
        if not MIN_OPCIONES <= len(opciones) <= MAX_OPCIONES:
            return "opciones", f"Agrega entre {MIN_OPCIONES} y {MAX_OPCIONES} alternativas."
        if any(not str(opcion.get("texto", "")).strip() for opcion in opciones):
            return "opciones", "Ninguna alternativa puede estar vacía."
        if sum(bool(opcion.get("es_correcta")) for opcion in opciones) != 1:
            return "opciones", "Marca exactamente una alternativa como correcta."
    elif not (respuesta_esperada or "").strip():
        # prediccion_salida y completar_espacio.
        return "respuesta_esperada", "Escribe la respuesta esperada: es lo que se compara con lo que responde el estudiante."
    return None


# Lo mismo, para un problema que ya está en la base.
def faltante_de(problema):
    return faltante(
        problema.tipo_ejercicio,
        list(problema.casos_prueba.values("entrada", "salida_esperada")),
        list(problema.opciones.values("texto", "es_correcta")),
        problema.respuesta_esperada,
    )
