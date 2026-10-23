from django.db import transaction

from problemas.completitud import faltante
from problemas.ia import IaRespuestaInvalida
from problemas.models import Plataforma, TipoEjercicio
from problemas.serializers import ProblemaSerializer

# Qué tipos puede traer cada plataforma pedida (igual que ai-service y que la regla de HU15).
TIPOS_POR_PLATAFORMA = {
    Plataforma.WEB: {TipoEjercicio.ESCRITURA_CODIGO},
    Plataforma.MOVIL: {TipoEjercicio.PREDICCION_SALIDA, TipoEjercicio.OPCION_MULTIPLE, TipoEjercicio.COMPLETAR_ESPACIO},
    Plataforma.AMBAS: set(TipoEjercicio.values),
}


# Cada problema guardado va a una sola plataforma, la que su tipo permite: el celular no ejecuta código y la
# web no responde los otros tres (HU15). Pedir «ambas» significa «genera para las dos», no que cada
# problema se publique en las dos; por eso la plataforma del problema sale de su tipo y no de la solicitud.
def plataforma_del_tipo(tipo):
    return Plataforma.WEB if tipo == TipoEjercicio.ESCRITURA_CODIGO else Plataforma.MOVIL


# Valida lo que devolvió la IA con las mismas reglas que un problema creado a mano (HU15, HU16) más la de
# «completo» (completitud.py), y recién entonces guarda. Todo o nada: si un solo problema falla, no se guarda
# ninguno (CA-17.5). Lo guardado nace con generado_por_ia=True y aprobado_por_docente=False: la IA nunca
# publica sola (ADR-2).
def guardar_generados(curso, solicitud, generados):
    if len(generados) != solicitud["cantidad"]:
        raise IaRespuestaInvalida(f"se pidieron {solicitud['cantidad']} problemas y llegaron {len(generados)}")
    permitidos = TIPOS_POR_PLATAFORMA[solicitud["plataforma"]]

    validados = []
    for generado in generados:
        if not isinstance(generado, dict) or generado.get("tipo_ejercicio") not in permitidos:
            raise IaRespuestaInvalida("tipo de ejercicio no permitido para la plataforma pedida")
        datos = {
            "titulo": generado.get("titulo"),
            "enunciado": generado.get("enunciado"),
            "tipo_ejercicio": generado["tipo_ejercicio"],
            "plataforma": plataforma_del_tipo(generado["tipo_ejercicio"]),
            # Estos dos los decide el docente en la solicitud: no se confía en lo que la IA repita.
            "lenguaje": solicitud["lenguaje"],
            "dificultad": solicitud["nivel"],
            "casos_prueba": generado.get("casos_prueba") or [],
            "opciones": generado.get("opciones") or [],
            "respuesta_esperada": generado.get("respuesta_esperada") or "",
        }
        serializador = ProblemaSerializer(data=datos)
        if not serializador.is_valid():
            raise IaRespuestaInvalida(f"problema inválido: {sorted(serializador.errors)}")
        datos_ok = serializador.validated_data
        pendiente = faltante(
            datos_ok["tipo_ejercicio"],
            datos_ok.get("casos_prueba", []),
            datos_ok.get("opciones", []),
            datos_ok.get("respuesta_esperada", ""),
        )
        if pendiente:
            raise IaRespuestaInvalida(f"problema incompleto: {pendiente[0]}")
        validados.append(serializador)

    # atomic: si falla la base a mitad de camino, tampoco queda una parte de la tanda.
    with transaction.atomic():
        return [serializador.save(curso=curso, generado_por_ia=True) for serializador in validados]

