import { Injectable, computed, inject } from '@angular/core';

import { COMISARIA_SOURCES } from '../data/investigation/comisaria.investigation';
import { HOSPITAL_SOURCES } from '../data/investigation/hospital.investigation';
import { EVIDENCIAS_CATALOG } from '../data/investigation/evidencias.catalog';
import { topicsForScene } from '../data/investigation/interview-topics.catalog';
import { SceneId } from '../data/scene-types';
import { ConversationService } from './conversation.service';
import { ExpedienteCasoService } from './expediente-caso.service';
import { InterviewService } from './interview.service';

export interface InvestigationProgressView {
  entrevistasHechas: number;
  entrevistasTotal: number;
  documentosRevisados: number;
  documentosTotal: number;
  observaciones: number;
}

@Injectable({ providedIn: 'root' })
export class InvestigationStatsService {
  private readonly interview = inject(InterviewService);
  private readonly conversation = inject(ConversationService);
  private readonly expediente = inject(ExpedienteCasoService);

  progressFor(sceneId: SceneId) {
    return computed((): InvestigationProgressView => ({
      entrevistasHechas: this.interview.completedTopicsCount(sceneId),
      entrevistasTotal: topicsForScene(sceneId).length,
      documentosRevisados: this.conversation.documentsReviewedCount(sceneId),
      documentosTotal: EVIDENCIAS_CATALOG.filter((e) => e.sceneId === sceneId).length,
      observaciones: this.expediente.observationsCount(),
    }));
  }
}
