from django.db import migrations, models


# Primer paso de tres: las columnas nacen vacías y sin unique, porque los cursos que ya existen
# no tienen código todavía. Si nacieran con unique y un valor por defecto, todos recibirían el mismo.
class Migration(migrations.Migration):

    dependencies = [
        ("cursos", "0001_initial"),
    ]

    operations = [
        migrations.AddField(
            model_name="curso",
            name="codigo_invitacion",
            field=models.CharField(max_length=8, null=True),
        ),
        migrations.AddField(
            model_name="curso",
            name="codigo_vence",
            field=models.DateTimeField(null=True),
        ),
    ]
