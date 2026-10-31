"""Patrón Proxy: control de acceso por rol a participaciones, resultados y reportes."""

from django.shortcuts import get_object_or_404
from rest_framework.exceptions import PermissionDenied

from apps.participaciones.models import Participacion
from apps.practicas.models import Practica
from apps.usuarios.models import Usuario

from .models import Resultado


class AccesoEvaluacionProxy:
    """Filtra y autoriza consultas/operaciones según el rol del usuario autenticado."""

    def participaciones_visibles(self, user):
        qs = Participacion.objects.select_related(
            'practica__caso', 'practica__docente', 'estudiante', 'autorizacion', 'resultado',
        ).prefetch_related('respuestas_seleccionadas')
        if not user or not user.is_authenticated:
            raise PermissionDenied('Autenticación requerida.')
        if user.rol == Usuario.Rol.ADMIN:
            return qs
        if user.rol == Usuario.Rol.DOCENTE:
            return qs.filter(practica__docente=user)
        return qs.filter(estudiante__usuario=user)

    def resultados_visibles(self, user):
        qs = Resultado.objects.select_related(
            'participacion__estudiante',
            'participacion__practica__caso',
            'participacion__practica__docente',
            'docente_feedback',
        )
        if not user or not user.is_authenticated:
            raise PermissionDenied('Autenticación requerida.')
        if user.rol == Usuario.Rol.ADMIN:
            return qs
        if user.rol == Usuario.Rol.DOCENTE:
            return qs.filter(participacion__practica__docente=user)
        return qs.filter(participacion__estudiante__usuario=user)

    def asegurar_participacion(self, user, pk) -> Participacion:
        return get_object_or_404(self.participaciones_visibles(user), pk=pk)

    def asegurar_resultado(self, user, pk) -> Resultado:
        return get_object_or_404(self.resultados_visibles(user), pk=pk)

    def asegurar_practica_docente(self, user, practica_id) -> Practica:
        practica = get_object_or_404(Practica, pk=practica_id)
        if user.rol == Usuario.Rol.ADMIN:
            return practica
        if user.rol == Usuario.Rol.DOCENTE and practica.docente_id == user.id:
            return practica
        raise PermissionDenied('No autorizado para consultar esta práctica.')

    def asegurar_docente_de_participacion(self, user, participacion: Participacion) -> None:
        if user.rol == Usuario.Rol.ADMIN:
            return
        if user.rol == Usuario.Rol.DOCENTE and participacion.practica.docente_id == user.id:
            return
        raise PermissionDenied('Solo el docente de la práctica puede registrar retroalimentación.')
