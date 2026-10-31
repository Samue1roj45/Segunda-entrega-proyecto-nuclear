import { Injectable, inject, signal } from '@angular/core';

import {
  InterviewTopicDef,
  topicsForScene,
} from '../data/investigation/interview-topics.catalog';
import { SceneId } from '../data/scene-types';
import { PERSONAS_CATALOG } from '../data/investigation/personas.catalog';
import { ExpedienteCasoService } from './expediente-caso.service';
import { ExplorationContextService } from './exploration-context.service';

export interface InterviewTopicView {
  id: string;
  label: string;
  done: boolean;
  locked: boolean;
  lockReason?: string;
}

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly expediente = inject(ExpedienteCasoService);
  private readonly exploration = inject(ExplorationContextService);

  private readonly completedTopics = signal<Set<string>>(new Set());

  getTopicsForNpc(sceneId: SceneId, npcId: string): InterviewTopicView[] {
    const all = topicsForScene(sceneId).filter((t) => t.npcId === npcId);
    const done = this.completedTopics();
    return all.map((t) => {
      const isDone = done.has(t.id);
      const lock = this.lockReason(t, done);
      return {
        id: t.id,
        label: t.label,
        done: isDone,
        locked: !!lock,
        lockReason: lock ?? undefined,
      };
    });
  }

  getTopic(topicId: string): InterviewTopicDef | undefined {
    return [...topicsForScene('hospital'), ...topicsForScene('comisaria')].find(
      (t) => t.id === topicId,
    );
  }

  hasAvailableTopics(sceneId: SceneId, npcId: string): boolean {
    return this.getTopicsForNpc(sceneId, npcId).some((t) => !t.done && !t.locked);
  }

  startTopic(topicId: string): InterviewTopicDef | null {
    const topic = this.getTopic(topicId);
    if (!topic) return null;
    const done = this.completedTopics();
    if (done.has(topicId) || this.lockReason(topic, done)) return null;
    return topic;
  }

  completeTopic(topic: InterviewTopicDef): void {
    this.completedTopics.update((s) => {
      const next = new Set(s);
      next.add(topic.id);
      return next;
    });

    const prevTier = this.maxTierForNpc(topic.sceneId, topic.npcId);
    if (topic.tier > prevTier) {
      this.expediente.discoverPersona(topic.npcId, topic.tier);
    }

    const persona = PERSONAS_CATALOG.find((p) => p.id === topic.npcId);
    const ubicacion = this.exploration.roomLabel();

    for (const clueId of topic.clueIds) {
      this.expediente.addClue(clueId, {
        fuente: 'entrevista',
        informante: persona?.nombre ?? topic.npcId,
        ubicacion,
      });
    }
  }

  private maxTierForNpc(sceneId: SceneId, npcId: string): number {
    const done = this.completedTopics();
    let max = 0;
    for (const t of topicsForScene(sceneId)) {
      if (t.npcId === npcId && done.has(t.id) && t.tier > max) {
        max = t.tier;
      }
    }
    return max;
  }

  private lockReason(topic: InterviewTopicDef, done: Set<string>): string | null {
    if (done.has(topic.id)) return 'Ya registraste esta línea de entrevista.';
    if (topic.requiresTopicIds?.length) {
      const missing = topic.requiresTopicIds.find((id) => !done.has(id));
      if (missing) return 'Explora otros temas de la entrevista primero.';
    }
    const clueCount = this.expediente.collectedClueIds().length;
    if (topic.requiresMinClues && clueCount < topic.requiresMinClues) {
      return `Necesitas más pistas del caso (${topic.requiresMinClues} mínimo).`;
    }
    if (topic.requiresClueIds?.length) {
      const have = this.expediente.collectedClueIds();
      if (!topic.requiresClueIds.every((id) => have.includes(id))) {
        return 'Aún no tienes evidencia suficiente para esta pregunta.';
      }
    }
    return null;
  }

  completedTopicsCount(sceneId: SceneId): number {
    const done = this.completedTopics();
    return topicsForScene(sceneId).filter((t) => done.has(t.id)).length;
  }

  reset(): void {
    this.completedTopics.set(new Set());
  }
}
