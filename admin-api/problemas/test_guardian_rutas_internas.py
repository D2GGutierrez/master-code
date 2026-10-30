# HU31-T3 (CA-031.3): guardián de la auditoría de rutas internas. user-api solo ve problemas a través de /internal/,
# así que cada ruta de ahí se revisó a mano. Si alguien agrega una nueva, esta prueba falla hasta que se audite.
from django.test import SimpleTestCase
from django.urls import URLResolver, get_resolver

# Rutas internas auditadas (HU31-T3), con el motivo de su veredicto.
AUDITADAS = {
    # NO APLICA: datos de cursos (id, nombre, lenguaje, institución), sin problemas ni conteos.
    "internal/cursos/",
    # NO APLICA: un curso por código de invitación; mismos datos que la anterior.
    "internal/cursos/por-codigo/<str:codigo>/",
    # OK: «publicado» = tiene algún problema de visibles_para_estudiante() (cursos/interno.py).
    "internal/cursos/publicados/",
    # OK: parte de visibles_para_estudiante() (problemas/interno.py, ProblemasDelCursoView).
    "internal/cursos/<int:curso_id>/problemas/",
    # OK: get_object_or_404 sobre visibles_para_estudiante() (problemas/interno.py, ProblemaView).
    "internal/problemas/<int:problema_id>/",
    # OK: get_object_or_404 sobre visibles_para_estudiante() (problemas/interno.py, ProblemaParaCalificarView).
    # Entrega los casos ocultos a propósito: solo con la clave interna y para que user-api califique (HU36-T2).
    # user-api no los reenvía; lo comprueban sus pruebas (HU39).
    "internal/problemas/<int:problema_id>/calificacion/",
}


def rutas(patrones, prefijo=""):
    for patron in patrones:
        ruta = prefijo + str(patron.pattern)
        if isinstance(patron, URLResolver):
            yield from rutas(patron.url_patterns, ruta)
        else:
            yield ruta


class GuardianRutasInternasTests(SimpleTestCase):
    def test_cada_ruta_interna_esta_auditada(self):
        internas = {
            ruta
            for ruta in rutas(get_resolver().url_patterns)
            if ruta.startswith("internal/")
        }

        nuevas = sorted(internas - AUDITADAS)
        self.assertFalse(
            nuevas,
            f"Ruta interna nueva sin auditar: {nuevas}. Revisa que aplique visibles_para_estudiante() "
            "y agrégala a la lista AUDITADAS.",
        )
        # Una ruta auditada que ya no existe deja la lista desactualizada: también se corrige.
        self.assertFalse(
            sorted(AUDITADAS - internas),
            "La lista AUDITADAS tiene rutas que ya no existen.",
        )
