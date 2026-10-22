from datetime import timedelta

from django.conf import settings
from django.db import migrations
from django.utils import timezone

from cursos.models import generar_codigo


def dar_codigo_a_cursos_existentes(apps, schema_editor):
    Curso = apps.get_model("cursos", "Curso")
    usados = set()
    vence = timezone.now() + timedelta(days=settings.INVITACION_DIAS)
    for curso in Curso.objects.all():
        codigo = generar_codigo()
        while codigo in usados:
            codigo = generar_codigo()
        usados.add(codigo)
        curso.codigo_invitacion = codigo
        curso.codigo_vence = vence
        curso.save(update_fields=["codigo_invitacion", "codigo_vence"])


class Migration(migrations.Migration):

    dependencies = [
        ("cursos", "0002_curso_codigo_invitacion_codigo_vence"),
    ]

    operations = [
        # Sin marcha atrás útil: quitar las columnas (0002) ya borra los códigos.
        migrations.RunPython(dar_codigo_a_cursos_existentes, migrations.RunPython.noop),
    ]
