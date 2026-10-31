import { Injectable, computed, signal } from '@angular/core';

import { EXPLORATION_GATING, PlayNpcDef, PlayObjectDef, SceneId } from '../data/scene-types';

export interface BitacoraEntry {
  id: string;
  sceneId: SceneId;
  type: 'npc' | 'object';
  sourceLabel: string;
  category: string;
  title: string;
  summary: string;
  recordedAt: string;
}

export interface SceneExplorationProgress {
  npcsVisited: number;
  objectsInspected: number;
  npcsRequired: number;
  objectsRequired: number;
  complete: boolean;
}

@Injectable({ providedIn: 'root' })
export class InvestigationService {
  private readonly npcKeys = signal<Set<string>>(new Set());
  private readonly objectKeys = signal<Set<string>>(new Set());
  readonly entries = signal<BitacoraEntry[]>([]);

  readonly clueCount = computed(() => this.entries().length);

  readonly hospitalProgress = computed(() =>
    this.progressForScene('hospital'),
  );

  readonly comisariaProgress = computed(() =>
    this.progressForScene('comisaria'),
  );

  readonly hospitalComplete = computed(() => this.hospitalProgress().complete);
  readonly comisariaComplete = computed(() => this.comisariaProgress().complete);
  readonly explorationComplete = computed(
    () => this.hospitalComplete() && this.comisariaComplete(),
  );

  registerNpc(sceneId: SceneId, npc: PlayNpcDef): void {
    const key = this.key(sceneId, 'npc', npc.id);
    if (this.npcKeys().has(key)) return;

    const nextNpc = new Set(this.npcKeys());
    nextNpc.add(key);
    this.npcKeys.set(nextNpc);

    const newEntries = npc.clues.map((clue) => this.toEntry(sceneId, 'npc', npc.label, clue));
    this.entries.update((list) => [...list, ...newEntries]);
  }

  registerObject(sceneId: SceneId, obj: PlayObjectDef): void {
    const key = this.key(sceneId, 'object', obj.id);
    if (this.objectKeys().has(key)) return;

    const nextObj = new Set(this.objectKeys());
    nextObj.add(key);
    this.objectKeys.set(nextObj);

    const newEntries = obj.clues.map((clue) => this.toEntry(sceneId, 'object', obj.label, clue));
    this.entries.update((list) => [...list, ...newEntries]);
  }

  hasVisitedNpc(sceneId: SceneId, npcId: string): boolean {
    return this.npcKeys().has(this.key(sceneId, 'npc', npcId));
  }

  hasInspectedObject(sceneId: SceneId, objectId: string): boolean {
    return this.objectKeys().has(this.key(sceneId, 'object', objectId));
  }

  progressForScene(sceneId: SceneId): SceneExplorationProgress {
    const npcsVisited = [...this.npcKeys()].filter((k) => k.startsWith(`${sceneId}:npc:`)).length;
    const objectsInspected = [...this.objectKeys()].filter((k) =>
      k.startsWith(`${sceneId}:object:`),
    ).length;
    return {
      npcsVisited,
      objectsInspected,
      npcsRequired: EXPLORATION_GATING.minNpcs,
      objectsRequired: EXPLORATION_GATING.minObjects,
      complete:
        npcsVisited >= EXPLORATION_GATING.minNpcs &&
        objectsInspected >= EXPLORATION_GATING.minObjects,
    };
  }

  reset(): void {
    this.npcKeys.set(new Set());
    this.objectKeys.set(new Set());
    this.entries.set([]);
  }

  private key(sceneId: SceneId, type: 'npc' | 'object', id: string): string {
    return `${sceneId}:${type}:${id}`;
  }

  private toEntry(
    sceneId: SceneId,
    type: 'npc' | 'object',
    sourceLabel: string,
    clue: { category: string; title: string; summary: string },
  ): BitacoraEntry {
    return {
      id: `${sceneId}-${type}-${sourceLabel}-${clue.title}`.replace(/\s+/g, '-').toLowerCase(),
      sceneId,
      type,
      sourceLabel,
      category: clue.category,
      title: clue.title,
      summary: clue.summary,
      recordedAt: new Date().toISOString(),
    };
  }
}
