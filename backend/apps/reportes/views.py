"""Endpoints de Reportes (admin métricas + descarga PDF/Excel/JSON)."""

from django.db.models import Avg
from django.http import HttpResponse
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from apps.academico.models import Estudiante, Grupo
from apps.casos.models import Caso
from apps.practicas.models import Practica
from apps.resultados.models import Resultado
from apps.usuarios.models import Usuario
from apps.usuarios.permissions import EsAdmin, EsDocenteOAdmin

from .facade import ReporteFacade


@api_view(['GET'])
@permission_classes([EsAdmin])
def admin_metricas(request):
    """Métricas globales para el dashboard del administrador (RF04, RF44)."""
    return Response({
        'docentes': Usuario.objects.filter(rol=Usuario.Rol.DOCENTE).count(),
        'estudiantes': Estudiante.objects.count(),
        'grupos': Grupo.objects.count(),
        'casos': Caso.objects.count(),
        'practicas': Practica.objects.count(),
        'practicas_por_estado': {
            estado: Practica.objects.filter(estado=estado).count()
            for estado, _ in Practica.Estado.choices
        },
        'resultados': Resultado.objects.count(),
        'nota_promedio': Resultado.objects.aggregate(avg=Avg('nota_final'))['avg'] or 0,
    })


def _filtros(request) -> dict:
    return {
        'estado': request.query_params.get('estado'),
        'estudiante': request.query_params.get('estudiante'),
    }


@api_view(['GET'])
@permission_classes([IsAuthenticated, EsDocenteOAdmin])
def reporte_practica_json(request, practica_id: int):
    data = ReporteFacade().json(request.user, practica_id, _filtros(request))
    return Response(data)


@api_view(['GET'])
@permission_classes([IsAuthenticated, EsDocenteOAdmin])
def reporte_practica_pdf(request, practica_id: int):
    contenido = ReporteFacade().pdf_bytes(request.user, practica_id, _filtros(request))
    resp = HttpResponse(contenido, content_type='application/pdf')
    resp['Content-Disposition'] = f'attachment; filename="reporte-practica-{practica_id}.pdf"'
    return resp


@api_view(['GET'])
@permission_classes([IsAuthenticated, EsDocenteOAdmin])
def reporte_practica_excel(request, practica_id: int):
    contenido = ReporteFacade().excel_bytes(request.user, practica_id, _filtros(request))
    resp = HttpResponse(
        contenido,
        content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    )
    resp['Content-Disposition'] = f'attachment; filename="reporte-practica-{practica_id}.xlsx"'
    return resp
