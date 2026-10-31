import { describe, expect, it } from 'vitest';

import { CasoDetalle } from '../../../core/models/casos.model';
import { EstadoCaso } from '../../../core/models/casos.model';
import { COMISARIA_DECISION, HOSPITAL_DECISION } from '../data/decisions.content';
import { DecisionBridgeService } from './decision-bridge.service';

/** Caso académico de referencia alineado con escenario orden 1/2 y 3 respuestas por pregunta. */
function mockCasoAlineado(): CasoDetalle {
  return {
    id: 42,
    nombre: 'Violencia familiar — práctica demo',
    descripcion: '',
    area_psicosocial: 'Violencia',
    tiempo_estimado_min: 30,
    estado: EstadoCaso.Validado,
    estado_display: 'Validado',
    docente_creador: 1,
    docente_creador_username: 'docente',
    escenarios_count: 2,
    fecha_creacion: '',
    fecha_actualizacion: '',
    desarrollo_situacional: '',
    contexto_historia: '',
    escenarios: [
      {
        id: 101,
        caso: 42,
        orden: 1,
        titulo: 'Urgencias hospitalarias',
        narrativa: 'Atención EPICEE',
        recursos_multimedia: [],
        preguntas: [
          {
            id: 1001,
            escenario: 101,
            orden: 1,
            enunciado: '¿Qué acción en urgencias?',
            peso: 2,
            respuestas: [
              { id: 10001, pregunta: 1001, orden: 1, texto: 'Activar protocolo integral EPICEE', es_correcta: true, justificacion: '', retroalimentacion: '' },
              { id: 10002, pregunta: 1001, orden: 2, texto: 'Priorizar entrevista familiar', es_correcta: false, justificacion: '', retroalimentacion: '' },
              { id: 10003, pregunta: 1001, orden: 3, texto: 'Esperar nueva valoración médica', es_correcta: false, justificacion: '', retroalimentacion: '' },
            ],
          },
        ],
      },
      {
        id: 102,
        caso: 42,
        orden: 2,
        titulo: 'Comisaría de Familia',
        narrativa: 'Medidas de protección',
        recursos_multimedia: [],
        preguntas: [
          {
            id: 1002,
            escenario: 102,
            orden: 1,
            enunciado: '¿Qué medida en comisaría?',
            peso: 3,
            respuestas: [
              { id: 10004, pregunta: 1002, orden: 1, texto: 'Medida de protección inmediata', es_correcta: true, justificacion: '', retroalimentacion: '' },
              { id: 10005, pregunta: 1002, orden: 2, texto: 'Acompañamiento psicosocial gradual', es_correcta: false, justificacion: '', retroalimentacion: '' },
              { id: 10006, pregunta: 1002, orden: 3, texto: 'Espacio de conciliación familiar', es_correcta: false, justificacion: '', retroalimentacion: '' },
            ],
          },
        ],
      },
    ],
    rubrica: null,
  };
}

describe('DecisionBridgeService', () => {
  const bridge = new DecisionBridgeService();

  it('mapea 100% opciones con caso alineado por orden', () => {
    const audit = bridge.audit(mockCasoAlineado());
    expect(audit.optionsMappedPercent).toBe(100);
    expect(audit.decisionsMappedPercent).toBe(100);
    expect(audit.canCompleteWithFullTraceability).toBe(true);
  });

  it('resuelve hospital opción 1 con ids concretos', () => {
    const ref = bridge.resolve(mockCasoAlineado(), HOSPITAL_DECISION, 1, 'protocolo-integral');
    expect(ref).toEqual({ pregunta_id: 1001, respuesta_id: 10001 });
  });

  it('mapea escenario por título si orden hospital es 3', () => {
    const caso = mockCasoAlineado();
    caso.escenarios[0].orden = 3;
    caso.escenarios[0].titulo = 'Urgencias hospital EPICEE';
    const audit = bridge.audit(caso);
    expect(audit.hospital.anyOptionMapped).toBe(true);
    expect(audit.hospital.optionsMappedPercent).toBe(100);
  });

  it('falla trazabilidad completa sin escenarios', () => {
    const audit = bridge.audit({ ...mockCasoAlineado(), escenarios: [] });
    expect(audit.canCompleteWithFullTraceability).toBe(false);
    expect(audit.optionsMappedPercent).toBe(0);
  });

  it('mapea respuesta por texto si orden respuesta difiere', () => {
    const caso = mockCasoAlineado();
    caso.escenarios[0].preguntas[0].respuestas[0].orden = 9;
    const ref = bridge.resolve(caso, HOSPITAL_DECISION, 1, 'protocolo-integral');
    expect(ref?.respuesta_id).toBe(10001);
  });
});
