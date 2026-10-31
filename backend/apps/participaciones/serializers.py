"""Serializers de Participacion y RespuestaSeleccionada."""

from rest_framework import serializers

from apps.casos.serializers import CasoDetalleSerializer

from .models import Participacion, RespuestaSeleccionada


class RespuestaSeleccionadaSerializer(serializers.ModelSerializer):
    class Meta:
        model = RespuestaSeleccionada
        fields = ('id', 'participacion', 'pregunta', 'respuesta_elegida', 'timestamp')
        read_only_fields = ('id', 'timestamp')


class ParticipacionSerializer(serializers.ModelSerializer):
    estado_display = serializers.CharField(source='get_estado_display', read_only=True)
    practica_nombre = serializers.CharField(source='practica.nombre', read_only=True)
    estudiante_correo = serializers.CharField(source='estudiante.correo', read_only=True)
    estudiante_nombre = serializers.CharField(source='estudiante.nombre_completo', read_only=True)
    respuestas_seleccionadas = RespuestaSeleccionadaSerializer(many=True, read_only=True)
    nota_final = serializers.SerializerMethodField()
    feedback_docente = serializers.SerializerMethodField()

    class Meta:
        model = Participacion
        fields = (
            'id', 'practica', 'practica_nombre', 'estudiante', 'estudiante_correo',
            'estudiante_nombre', 'autorizacion', 'numero_intento',
            'inicio', 'fin', 'tiempo_usado_seg',
            'estado', 'estado_display',
            'nota_final', 'feedback_docente',
            'respuestas_seleccionadas',
        )
        read_only_fields = fields

    def get_nota_final(self, obj: Participacion):
        if obj.es_consultable_resultado:
            return obj.resultado.nota_final
        return None

    def get_feedback_docente(self, obj: Participacion):
        if obj.es_consultable_resultado:
            return obj.resultado.feedback_docente
        return ''


class ParticipacionConCasoSerializer(ParticipacionSerializer):
    """Versión expandida con el caso anidado. Oculta claves de evaluación mientras el intento está en curso."""

    caso = serializers.SerializerMethodField()

    class Meta(ParticipacionSerializer.Meta):
        fields = ParticipacionSerializer.Meta.fields + ('caso',)

    def get_caso(self, obj: Participacion):
        data = CasoDetalleSerializer(obj.practica.caso).data
        if obj.estado == Participacion.Estado.EN_CURSO:
            self._ocultar_evaluacion(data)
        return data

    def _ocultar_evaluacion(self, data: dict) -> None:
        for escenario in data.get('escenarios') or []:
            for pregunta in escenario.get('preguntas') or []:
                for respuesta in pregunta.get('respuestas') or []:
                    respuesta.pop('es_correcta', None)
                    respuesta.pop('justificacion', None)
                    respuesta.pop('retroalimentacion', None)


class ResponderSerializer(serializers.Serializer):
    pregunta_id = serializers.IntegerField(min_value=1)
    respuesta_id = serializers.IntegerField(min_value=1)
