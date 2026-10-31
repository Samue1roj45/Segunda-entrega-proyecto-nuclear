import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('participaciones', '0001_initial'),
        ('practicas', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='participacion',
            name='numero_intento',
            field=models.PositiveIntegerField(default=1, verbose_name='número de intento'),
        ),
        migrations.AlterField(
            model_name='participacion',
            name='autorizacion',
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.CASCADE,
                related_name='participaciones',
                to='practicas.autorizacionestudiante',
            ),
        ),
        migrations.AlterModelOptions(
            name='participacion',
            options={
                'ordering': ['-inicio', '-id'],
                'verbose_name': 'Participación',
                'verbose_name_plural': 'Participaciones',
            },
        ),
        migrations.AddConstraint(
            model_name='participacion',
            constraint=models.UniqueConstraint(
                fields=('autorizacion', 'numero_intento'),
                name='unique_intento_por_autorizacion',
            ),
        ),
    ]
