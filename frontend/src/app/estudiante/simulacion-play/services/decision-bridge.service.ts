import { Injectable } from '@angular/core';

import { CasoDetalle, Escenario, Pregunta, Respuesta } from '../../../core/models/casos.model';
import { COMISARIA_DECISION, HOSPITAL_DECISION, SceneDecisionDef } from '../data/decisions.content';
import { OPTION_BACKEND_KEYWORDS, SCENE_BACKEND_MATCHERS } from '../data/decision-mapping.config';

export interface BackendAnswerRef {
  pregunta_id: number;
  respuesta_id: number;
}

export type MappingStrategy =
  | 'orden-exacto'
  | 'escenario-por-titulo'
  | 'pregunta-unica'
  | 'respuesta-por-orden'
  | 'respuesta-por-texto'
  | 'no-resuelto';

export interface ResolvedMapping {
  pregunta_id: number;
  respuesta_id: number;
  strategy: MappingStrategy;
  escenarioId: number;
  escenarioTitulo: string;
  preguntaOrden: number;
  respuestaOrden: number;
}

export interface OptionMappingStatus {
  optionId: string;
  actionTitle: string;
  respuestaOrdenNarrativa: number;
  mapped: boolean;
  strategy: MappingStrategy;
  ref: BackendAnswerRef | null;
  backendRespuestaTexto?: string;
}

export interface DecisionMappingStatus {
  sceneId: string;
  escenarioOrdenConfig: number;
  preguntaOrdenConfig: number;
  escenarioFound: boolean;
  preguntaFound: boolean;
  options: OptionMappingStatus[];
  optionsMappedPercent: number;
  anyOptionMapped: boolean;
}

export interface AcademicMappingAudit {
  casoId: number | null;
  casoNombre: string;
  escenariosBackend: { id: number; orden: number; titulo: string; preguntas: number }[];
  hospital: DecisionMappingStatus;
  comisaria: DecisionMappingStatus;
  /** % de las 6 opciones narrativas (3+3) con ref válido. */
  optionsMappedPercent: number;
  /** % de los 2 puntos de decisión con al menos una opción mapeable. */
  decisionsMappedPercent: number;
  /** El estudiante puede registrar hospital + comisaría al elegir cualquier opción válida en cada escena. */
  canCompleteWithFullTraceability: boolean;
  warnings: string[];
}

@Injectable({ providedIn: 'root' })
export class DecisionBridgeService {
  /**
   * Resuelve IDs del backend sin exponer enunciados al estudiante.
   */
  resolve(
    caso: CasoDetalle | undefined | null,
    decision: SceneDecisionDef,
    respuestaOrden: number,
    optionId?: string,
  ): BackendAnswerRef | null {
    const d = this.resolveDetailed(caso, decision, respuestaOrden, optionId);
    return d ? { pregunta_id: d.pregunta_id, respuesta_id: d.respuesta_id } : null;
  }

  resolveDetailed(
    caso: CasoDetalle | undefined | null,
    decision: SceneDecisionDef,
    respuestaOrden: number,
    optionId?: string,
  ): ResolvedMapping | null {
    if (!caso?.escenarios?.length) return null;

    const escenario = this.findEscenario(caso, decision);
    if (!escenario) return null;

    const pregunta = this.findPregunta(escenario, decision.preguntaOrden);
    if (!pregunta?.respuestas?.length) return null;

    const resolved = this.findRespuesta(pregunta, respuestaOrden, optionId);
    if (!resolved) return null;

    return {
      pregunta_id: pregunta.id,
      respuesta_id: resolved.respuesta.id,
      strategy: resolved.strategy,
      escenarioId: escenario.id,
      escenarioTitulo: escenario.titulo,
      preguntaOrden: pregunta.orden,
      respuestaOrden: resolved.respuesta.orden,
    };
  }

  audit(caso: CasoDetalle | undefined | null): AcademicMappingAudit {
    const warnings: string[] = [];

    if (!caso?.escenarios?.length) {
      warnings.push('El caso no incluye escenarios en la participación. Ninguna decisión se registrará.');
      return this.emptyAudit(caso, warnings);
    }

    const hospital = this.auditDecision(caso, HOSPITAL_DECISION);
    const comisaria = this.auditDecision(caso, COMISARIA_DECISION);

    const totalOptions = hospital.options.length + comisaria.options.length;
    const mappedOptions =
      hospital.options.filter((o) => o.mapped).length +
      comisaria.options.filter((o) => o.mapped).length;

    const decisionsOk = (hospital.anyOptionMapped ? 1 : 0) + (comisaria.anyOptionMapped ? 1 : 0);

    if (!hospital.anyOptionMapped) {
      warnings.push(
        `Hospital: no se encontró escenario/pregunta/respuestas alineados (config orden escenario=${HOSPITAL_DECISION.escenarioOrden}, pregunta=${HOSPITAL_DECISION.preguntaOrden}).`,
      );
    }
    if (!comisaria.anyOptionMapped) {
      warnings.push(
        `Comisaría: no se encontró escenario/pregunta/respuestas alineados (config orden escenario=${COMISARIA_DECISION.escenarioOrden}, pregunta=${COMISARIA_DECISION.preguntaOrden}).`,
      );
    }

    if (caso.escenarios.length < 2) {
      warnings.push('El caso tiene menos de 2 escenarios; la decisión de comisaría puede no mapear.');
    }

    return {
      casoId: caso?.id ?? null,
      casoNombre: caso?.nombre ?? '(sin caso)',
      escenariosBackend: caso.escenarios.map((e) => ({
        id: e.id,
        orden: e.orden,
        titulo: e.titulo,
        preguntas: e.preguntas?.length ?? 0,
      })),
      hospital,
      comisaria,
      optionsMappedPercent: totalOptions ? Math.round((mappedOptions / totalOptions) * 100) : 0,
      decisionsMappedPercent: Math.round((decisionsOk / 2) * 100),
      canCompleteWithFullTraceability: hospital.anyOptionMapped && comisaria.anyOptionMapped,
      warnings,
    };
  }

  /** Etiqueta legible para documentación / depuración (no mostrar al estudiante). */
  formatMappingExample(
    caso: CasoDetalle | null | undefined,
    decision: SceneDecisionDef,
    optionId: string,
    respuestaOrden: number,
  ): string {
    const option = decision.options.find((o) => o.id === optionId);
    const detailed = this.resolveDetailed(caso, decision, respuestaOrden, optionId);
    if (!detailed || !option) {
      return `${option?.actionTitle ?? optionId} → NO MAPEADO`;
    }
    return [
      `Decisión narrativa: "${option.actionTitle}"`,
      `→ Escenario backend #${detailed.escenarioId} (orden ${decision.escenarioOrden}, título: "${detailed.escenarioTitulo}")`,
      `→ Pregunta backend id=${detailed.pregunta_id} (orden ${detailed.preguntaOrden})`,
      `→ Respuesta backend id=${detailed.respuesta_id} (orden ${detailed.respuestaOrden}, estrategia: ${detailed.strategy})`,
    ].join('\n');
  }

  private auditDecision(caso: CasoDetalle, decision: SceneDecisionDef): DecisionMappingStatus {
    const escenario = this.findEscenario(caso, decision);
    const pregunta = escenario ? this.findPregunta(escenario, decision.preguntaOrden) : null;

    const options: OptionMappingStatus[] = decision.options.map((opt) => {
      const detailed = this.resolveDetailed(caso, decision, opt.respuestaOrden, opt.id);
      return {
        optionId: opt.id,
        actionTitle: opt.actionTitle,
        respuestaOrdenNarrativa: opt.respuestaOrden,
        mapped: !!detailed,
        strategy: detailed?.strategy ?? 'no-resuelto',
        ref: detailed
          ? { pregunta_id: detailed.pregunta_id, respuesta_id: detailed.respuesta_id }
          : null,
        backendRespuestaTexto: detailed
          ? this.findRespuestaTexto(escenario!, pregunta!, detailed.respuestaOrden)
          : undefined,
      };
    });

    const mappedCount = options.filter((o) => o.mapped).length;

    return {
      sceneId: decision.sceneId,
      escenarioOrdenConfig: decision.escenarioOrden,
      preguntaOrdenConfig: decision.preguntaOrden,
      escenarioFound: !!escenario,
      preguntaFound: !!pregunta,
      options,
      optionsMappedPercent: options.length
        ? Math.round((mappedCount / options.length) * 100)
        : 0,
      anyOptionMapped: mappedCount > 0,
    };
  }

  private findEscenario(caso: CasoDetalle, decision: SceneDecisionDef): Escenario | null {
    const byOrden = caso.escenarios.find((e) => e.orden === decision.escenarioOrden);
    if (byOrden) return byOrden;

    const matcher = SCENE_BACKEND_MATCHERS[decision.sceneId];
    const scored = caso.escenarios
      .map((e) => ({
        e,
        score: this.scoreEscenario(
          e,
          matcher.tituloKeywords,
          matcher.narrativaKeywords,
          matcher.escenarioOrdenPreferido,
        ),
      }))
      .filter((x) => x.score > 0)
      .sort((a, b) => b.score - a.score);

    return scored[0]?.e ?? null;
  }

  private scoreEscenario(
    escenario: Escenario,
    tituloKw: string[],
    narrativaKw: string[],
    preferOrden: number,
  ): number {
    const blob = `${escenario.titulo} ${escenario.narrativa}`.toLowerCase();
    let score = 0;
    for (const kw of [...tituloKw, ...narrativaKw]) {
      if (blob.includes(kw.toLowerCase())) score += kw.length > 4 ? 2 : 1;
    }
    if (escenario.orden === preferOrden) score += 2;
    return score;
  }

  private findPregunta(escenario: Escenario, preguntaOrden: number): Pregunta | null {
    const preguntas = escenario.preguntas ?? [];
    if (!preguntas.length) return null;

    const byOrden = preguntas.find((p) => p.orden === preguntaOrden);
    if (byOrden) return byOrden;

    if (preguntas.length === 1) return preguntas[0];

    return [...preguntas].sort((a, b) => a.orden - b.orden)[0] ?? null;
  }

  private findRespuesta(
    pregunta: Pregunta,
    respuestaOrden: number,
    optionId?: string,
  ): { respuesta: Respuesta; strategy: MappingStrategy } | null {
    const respuestas = pregunta.respuestas ?? [];

    const byOrden = respuestas.find((r) => r.orden === respuestaOrden);
    if (byOrden) {
      return { respuesta: byOrden, strategy: 'respuesta-por-orden' };
    }

    if (optionId && OPTION_BACKEND_KEYWORDS[optionId]) {
      const keywords = OPTION_BACKEND_KEYWORDS[optionId];
      const byText = respuestas
        .map((r) => ({
          r,
          score: keywords.filter((kw) => r.texto.toLowerCase().includes(kw.toLowerCase())).length,
        }))
        .filter((x) => x.score > 0)
        .sort((a, b) => b.score - a.score)[0];
      if (byText) {
        return { respuesta: byText.r, strategy: 'respuesta-por-texto' };
      }
    }

    if (respuestas.length === 1) {
      return { respuesta: respuestas[0], strategy: 'respuesta-por-orden' };
    }

    const byIndex = respuestas.sort((a, b) => a.orden - b.orden)[respuestaOrden - 1];
    if (byIndex) {
      return { respuesta: byIndex, strategy: 'respuesta-por-orden' };
    }

    return null;
  }

  private findRespuestaTexto(
    escenario: Escenario,
    pregunta: Pregunta,
    respuestaOrden: number,
  ): string | undefined {
    return pregunta.respuestas?.find((r) => r.orden === respuestaOrden)?.texto;
  }

  private emptyAudit(caso: CasoDetalle | null | undefined, warnings: string[]): AcademicMappingAudit {
    const emptyDecision = (d: SceneDecisionDef): DecisionMappingStatus => ({
      sceneId: d.sceneId,
      escenarioOrdenConfig: d.escenarioOrden,
      preguntaOrdenConfig: d.preguntaOrden,
      escenarioFound: false,
      preguntaFound: false,
      options: d.options.map((o) => ({
        optionId: o.id,
        actionTitle: o.actionTitle,
        respuestaOrdenNarrativa: o.respuestaOrden,
        mapped: false,
        strategy: 'no-resuelto' as MappingStrategy,
        ref: null,
      })),
      optionsMappedPercent: 0,
      anyOptionMapped: false,
    });

    return {
      casoId: caso?.id ?? null,
      casoNombre: caso?.nombre ?? '(sin caso)',
      escenariosBackend: [],
      hospital: emptyDecision(HOSPITAL_DECISION),
      comisaria: emptyDecision(COMISARIA_DECISION),
      optionsMappedPercent: 0,
      decisionsMappedPercent: 0,
      canCompleteWithFullTraceability: false,
      warnings,
    };
  }
}
