"""Fixtures compartidas para pruebas del módulo de evaluación."""

from datetime import timedelta

from django.utils import timezone
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from apps.academico.models import Estudiante
from apps.casos.models import Caso, Escenario, Pregunta, Respuesta, Rubrica
from apps.practicas.models import AutorizacionEstudiante, Practica
from apps.usuarios.models import Usuario


def crear_docente(username='docente1', email='docente1@uni.edu'):
    return Usuario.objects.create_user(
        username=username,
        email=email,
        password='ClaveSegura1',
        first_name='Ana',
        last_name='Docente',
        rol=Usuario.Rol.DOCENTE,
    )


def crear_estudiante(docente, correo='est1@uni.edu', first='Luis', last='Estudiante', username='est1'):
    usuario = Usuario.objects.create_user(
        username=username,
        email=correo,
        password='ClaveSegura1',
        first_name=first,
        last_name=last,
        rol=Usuario.Rol.ESTUDIANTE,
    )
    estudiante = Estudiante.objects.create(
        correo=correo,
        first_name=first,
        last_name=last,
        docente_creador=docente,
        usuario=usuario,
    )
    estudiante.docentes.add(docente)
    return estudiante


def crear_caso_con_rubrica(docente, escala=100):
    caso = Caso.objects.create(
        nombre='Caso prueba',
        docente_creador=docente,
        estado=Caso.Estado.VALIDADO,
    )
    escenario = Escenario.objects.create(caso=caso, orden=1, titulo='Escenario 1')
    p1 = Pregunta.objects.create(escenario=escenario, orden=1, enunciado='Pregunta 1', peso=2)
    p2 = Pregunta.objects.create(escenario=escenario, orden=2, enunciado='Pregunta 2', peso=2)
    r1_ok = Respuesta.objects.create(pregunta=p1, texto='Correcta 1', es_correcta=True, orden=1)
    r1_bad = Respuesta.objects.create(pregunta=p1, texto='Incorrecta 1', es_correcta=False, orden=2)
    r2_ok = Respuesta.objects.create(pregunta=p2, texto='Correcta 2', es_correcta=True, orden=1)
    r2_bad = Respuesta.objects.create(pregunta=p2, texto='Incorrecta 2', es_correcta=False, orden=2)
    Rubrica.objects.create(caso=caso, escala_maxima=escala, descripcion='Rúbrica de prueba')
    return {
        'caso': caso,
        'p1': p1,
        'p2': p2,
        'r1_ok': r1_ok,
        'r1_bad': r1_bad,
        'r2_ok': r2_ok,
        'r2_bad': r2_bad,
    }


def crear_practica(docente, caso, inicio=None, fin=None, estado=Practica.Estado.EN_CURSO):
    ahora = timezone.now()
    return Practica.objects.create(
        nombre='Práctica 1',
        caso=caso,
        docente=docente,
        fecha_inicio=inicio or (ahora - timedelta(hours=1)),
        fecha_fin=fin or (ahora + timedelta(hours=3)),
        tiempo_max_min=60,
        estado=estado,
        mensaje_personalizado='Bienvenidos',
    )


def autorizar(practica, estudiante) -> AutorizacionEstudiante:
    return AutorizacionEstudiante.objects.create(practica=practica, estudiante=estudiante)


def token_estudiante(estudiante, autorizacion):
    refresh = RefreshToken.for_user(estudiante.usuario)
    refresh['rol'] = Usuario.Rol.ESTUDIANTE
    refresh['estudiante_id'] = estudiante.id
    refresh['practica_id'] = autorizacion.practica_id
    refresh['autorizacion_id'] = autorizacion.id
    return str(refresh.access_token)


def token_usuario(usuario):
    refresh = RefreshToken.for_user(usuario)
    refresh['rol'] = usuario.rol
    return str(refresh.access_token)


def cliente(token) -> APIClient:
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f'Bearer {token}')
    return client
