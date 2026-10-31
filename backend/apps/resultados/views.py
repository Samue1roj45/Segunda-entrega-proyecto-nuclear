"""Views de Resultado."""

from rest_framework import mixins, viewsets
from rest_framework.decorators import action
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .facade import ConsultaResultadosFacade, RetroalimentacionFacade
from .serializers import FeedbackDocenteSerializer, ResultadoSerializer


class ResultadoViewSet(mixins.RetrieveModelMixin, mixins.ListModelMixin, viewsets.GenericViewSet):
    permission_classes = [IsAuthenticated]
    serializer_class = ResultadoSerializer

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.consulta = ConsultaResultadosFacade()
        self.retro = RetroalimentacionFacade()

    def get_queryset(self):
        return self.consulta.listar(self.request.user)

    def retrieve(self, request, *args, **kwargs):
        resultado = self.consulta.obtener(request.user, kwargs['pk'])
        return Response(ResultadoSerializer(resultado).data)

    @action(detail=True, methods=['post'], url_path='feedback-docente')
    def feedback_docente(self, request, pk=None):
        ser = FeedbackDocenteSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        resultado = self.retro.registrar(
            request.user, int(pk), ser.validated_data['feedback_docente'],
        )
        return Response(ResultadoSerializer(resultado).data)
