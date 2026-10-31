"""Views de Participacion: iniciar, responder, finalizar, listar, progreso."""

from rest_framework import mixins, status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from apps.casos.models import Pregunta, Respuesta
from apps.usuarios.models import Usuario

from .facade import ParticipacionFacade
from .serializers import (
    ParticipacionConCasoSerializer,
    ParticipacionSerializer,
    ResponderSerializer,
)


class ParticipacionViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin, viewsets.GenericViewSet):
    """
    Endpoints:
    - GET  /api/participaciones/          listado (docente: sus prácticas; estudiante: las propias)
    - POST /api/participaciones/iniciar/
    - GET  /api/participaciones/{id}/
    - POST /api/participaciones/{id}/responder/
    - POST /api/participaciones/{id}/finalizar/
    - GET  /api/participaciones/{id}/progreso/
    """

    permission_classes = [IsAuthenticated]
    serializer_class = ParticipacionSerializer

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.facade = ParticipacionFacade()

    def get_queryset(self):
        return self.facade.listar(self.request.user, {
            'practica': self.request.query_params.get('practica'),
            'estudiante': self.request.query_params.get('estudiante'),
            'estado': self.request.query_params.get('estado'),
            'numero_intento': self.request.query_params.get('numero_intento'),
            'fecha_desde': self.request.query_params.get('fecha_desde'),
            'fecha_hasta': self.request.query_params.get('fecha_hasta'),
        })

    def retrieve(self, request, pk=None):
        part = self.facade.obtener(request.user, pk)
        return Response(ParticipacionConCasoSerializer(part).data)

    @action(detail=False, methods=['post'], url_path='iniciar')
    def iniciar(self, request):
        autorizacion_id = None
        if request.auth is not None and hasattr(request.auth, 'payload'):
            autorizacion_id = request.auth.payload.get('autorizacion_id')
        part = self.facade.iniciar(request.user, autorizacion_id)
        return Response(ParticipacionConCasoSerializer(part).data)

    @action(detail=True, methods=['post'], url_path='responder')
    def responder(self, request, pk=None):
        ser = ResponderSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        part = self.facade.obtener(request.user, pk)
        pregunta = Pregunta.objects.filter(escenario__caso=part.practica.caso).filter(
            pk=ser.validated_data['pregunta_id'],
        ).first()
        if pregunta is None:
            raise ValidationError('La pregunta no pertenece al caso de esta práctica.')
        respuesta = Respuesta.objects.filter(pregunta=pregunta, pk=ser.validated_data['respuesta_id']).first()
        if respuesta is None:
            raise ValidationError('La respuesta no pertenece a la pregunta indicada.')
        self.facade.responder(request.user, pk, pregunta, respuesta)
        return Response({'detail': 'guardado'}, status=status.HTTP_200_OK)

    @action(detail=True, methods=['post'], url_path='finalizar')
    def finalizar(self, request, pk=None):
        _part, resultado = self.facade.finalizar(request.user, pk)
        return Response({
            'detail': 'Participación finalizada.',
            'resultado_id': resultado.id,
        }, status=status.HTTP_200_OK)

    @action(detail=True, methods=['get'], url_path='progreso')
    def progreso(self, request, pk=None):
        part = self.facade.obtener(request.user, pk)
        total_preg = Pregunta.objects.filter(escenario__caso=part.practica.caso).count()
        respondidas = part.respuestas_seleccionadas.count()
        return Response({
            'participacion_id': part.id,
            'estado': part.estado,
            'numero_intento': part.numero_intento,
            'total_preguntas': total_preg,
            'respondidas': respondidas,
            'tiempo_usado_seg': part.tiempo_usado_seg,
        })
