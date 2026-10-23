# Migración de datos (escrita a mano: Django no genera el contenido de un RunPython).

from django.db import migrations


# Hasta ahora todos los problemas los creó un docente a mano, así que él ya los revisó: los
# completos pasan a «aprobados» para que no desaparezcan de la vista del estudiante cuando
# HU31 empiece a filtrar por aprobado_por_docente. Solo se aprueban los que se pueden
# calificar: escritura de código con al menos un caso de prueba. Los de celular no tienen
# todavía dónde guardar su respuesta ni sus alternativas, así que están incompletos y quedan
# sin aprobar hasta que el docente los complete.
def aprobar_existentes(apps, schema_editor):
    Problema = apps.get_model("problemas", "Problema")
    completos = (
        Problema.objects.filter(
            aprobado_por_docente=False,
            generado_por_ia=False,
            archivado=False,
            tipo_ejercicio="escritura_codigo",
            casos_prueba__isnull=False,
        )
        .select_related("curso")
        .distinct()
    )
    for problema in completos:
        problema.aprobado_por_docente = True
        # El autor es el docente del curso; no se conserva otra fecha que la de creación.
        problema.aprobado_por_id = problema.curso.docente_id
        problema.fecha_aprobacion = problema.fecha_creacion
        problema.save(update_fields=["aprobado_por_docente", "aprobado_por", "fecha_aprobacion"])


class Migration(migrations.Migration):

    dependencies = [
        ("problemas", "0007_campos_de_ia_y_opciones"),
    ]

    operations = [
        # Sin marcha atrás: volver a «sin aprobar» no devuelve información que se haya perdido.
        migrations.RunPython(aprobar_existentes, migrations.RunPython.noop),
    ]
