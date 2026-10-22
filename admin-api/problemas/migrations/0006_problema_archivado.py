from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("problemas", "0005_problema_aprobado_por_fecha_aprobacion"),
    ]

    operations = [
        migrations.AddField(
            model_name="problema",
            name="archivado",
            field=models.BooleanField(default=False),
        ),
    ]
