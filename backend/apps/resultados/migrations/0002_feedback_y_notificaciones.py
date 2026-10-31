import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('academico', '0001_initial'),
        ('resultados', '0001_initial'),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AddField(
            model_name='resultado',
            name='docente_feedback',
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name='retroalimentaciones_registradas',
                to=settings.AUTH_USER_MODEL,
                verbose_name='docente que registra la retroalimentación',
            ),
        ),
        migrations.AddField(
            model_name='resultado',
            name='fecha_feedback',
            field=models.DateTimeField(blank=True, null=True, verbose_name='fecha de retroalimentación'),
        ),
        migrations.CreateModel(
            name='NotificacionEnvio',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('tipo', models.CharField(choices=[('CONVOCATORIA', 'Convocatoria'), ('CAMBIO_CONVOCATORIA', 'Cambio de convocatoria'), ('CALIFICACION', 'Calificación'), ('RETROALIMENTACION', 'Retroalimentación')], max_length=32)),
                ('event_key', models.CharField(max_length=191, unique=True, verbose_name='clave anti-duplicado')),
                ('destinatario', models.EmailField(max_length=254)),
                ('asunto', models.CharField(max_length=200)),
                ('cuerpo', models.TextField()),
                ('estado', models.CharField(choices=[('PENDIENTE', 'Pendiente'), ('ENVIADO', 'Enviado'), ('ERROR', 'Error')], default='PENDIENTE', max_length=16)),
                ('error_mensaje', models.TextField(blank=True)),
                ('practica_id', models.IntegerField(blank=True, null=True)),
                ('resultado_id', models.IntegerField(blank=True, null=True)),
                ('fecha_creacion', models.DateTimeField(auto_now_add=True)),
                ('fecha_envio', models.DateTimeField(blank=True, null=True)),
                ('estudiante', models.ForeignKey(
                    on_delete=django.db.models.deletion.CASCADE,
                    related_name='notificaciones_envio',
                    to='academico.estudiante',
                )),
            ],
            options={
                'verbose_name': 'Envío de notificación',
                'verbose_name_plural': 'Envíos de notificación',
                'ordering': ['-fecha_creacion'],
            },
        ),
    ]
