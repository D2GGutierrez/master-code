from django.db import migrations, models

import cursos.models


# Tercer paso: ya todos los cursos tienen código, ahora sí es obligatorio y único.
class Migration(migrations.Migration):

    dependencies = [
        ("cursos", "0003_codigos_para_cursos_existentes"),
    ]

    operations = [
        migrations.AlterField(
            model_name="curso",
            name="codigo_invitacion",
            field=models.CharField(default=cursos.models.generar_codigo, max_length=8, unique=True),
        ),
        migrations.AlterField(
            model_name="curso",
            name="codigo_vence",
            field=models.DateTimeField(default=cursos.models.vence_por_defecto),
        ),
    ]
