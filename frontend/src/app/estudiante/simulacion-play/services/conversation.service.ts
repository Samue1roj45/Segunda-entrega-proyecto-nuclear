import { Injectable, inject, signal } from '@angular/core';

import { COMISARIA_SOURCES } from '../data/investigation/comisaria.investigation';
import { HOSPITAL_SOURCES } from '../data/investigation/hospital.investigation';
import {
  DialogueTier,
  SourceInvestigationDef,
  TierDialogueBlock,
} from '../data/investigation/investigation.types';
import { SceneId } from '../data/scene-types';
import { EVIDENCIAS_CATALOG } from '../data/investigation/evidencias.catalog';
import { PERSONAS_CATALOG } from '../data/investigation/personas.catalog';
import { ExpedienteCasoService } from './expediente-caso.service';
import { ExplorationContextService } from './exploration-context.service';

export interface ConversationResult {
  lines: { speaker?: string; text: string }[];
  tierPlayed: DialogueTier;
  clueIds: string[];
  hasMore: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly expediente = inject(ExpedienteCasoService);
  private readonly exploration = inject(ExplorationContextService);

  /** Tier completado por fuente (0 = ninguno, 1-3 = último tier jugado). */
  private readonly tierCompleted = signal<Record<string, number>>({});

  private sourceKey(sceneId: SceneId, kind: 'npc' | 'object', sourceId: string): string {
    return `${sceneId}:${kind}:${sourceId}`;
  }

  private sourcesFor(sceneId: SceneId): SourceInvestigationDef[] {
    return sceneId === 'hospital' ? HOSPITAL_SOURCES : COMISARIA_SOURCES;
  }

  getSource(sceneId: SceneId, kind: 'npc' | 'object', sourceId: string): SourceInvestigationDef | undefined {
    return this.sourcesFor(sceneId).find((s) => s.sourceId === sourceId && s.kind === kind);
  }

  /** Indicador para canvas: null | 'more' | 'new' */
  getNpcIndicator(sceneId: SceneId, npcId: string): 'more' | 'new' | null {
    const next = this.getNextTierBlock(sceneId, 'npc', npcId);
    if (!next) return null;
    const key = this.sourceKey(sceneId, 'npc', npcId);
    const done = this.tierCompleted()[key] ?? 0;
    if (done === 0) return 'new';
    return 'more';
  }

  startConversation(
    sceneId: SceneId,
    kind: 'npc' | 'object',
    sourceId: string,
  ): ConversationResult | null {
    const block = this.getNextTierBlock(sceneId, kind, sourceId);
    if (!block) return null;

    const key = this.sourceKey(sceneId, kind, sourceId);
    const source = this.getSource(sceneId, kind, sourceId);
    const maxTier = source?.tiers.length ?? 0;
    const hasMore = block.tier < maxTier;

    return {
      lines: block.lines,
      tierPlayed: block.tier,
      clueIds: block.clueIds,
      hasMore,
    };
  }

  completeConversation(
    sceneId: SceneId,
    kind: 'npc' | 'object',
    sourceId: string,
    tierPlayed: DialogueTier,
    clueIds: string[],
  ): void {
    const key = this.sourceKey(sceneId, kind, sourceId);
    const prev = this.tierCompleted()[key] ?? 0;
    if (tierPlayed > prev) {
      this.tierCompleted.update((m) => ({ ...m, [key]: tierPlayed }));
    }

    if (kind === 'npc') {
      this.expediente.discoverPersona(sourceId, tierPlayed);
    } else {
      this.expediente.discoverEvidencia(sourceId);
    }

    const ubicacion = this.exploration.roomLabel();

    let informante = sourceId;
    if (kind === 'npc') {
      informante = PERSONAS_CATALOG.find((p) => p.id === sourceId)?.nombre ?? sourceId;
    } else {
      informante = EVIDENCIAS_CATALOG.find((e) => e.id === sourceId)?.titulo ?? sourceId;
    }

    for (const clueId of clueIds) {
      this.expediente.addClue(clueId, {
        fuente: kind === 'npc' ? 'entrevista' : 'documento',
        informante,
        ubicacion,
      });
    }
  }

  private getNextTierBlock(
    sceneId: SceneId,
    kind: 'npc' | 'object',
    sourceId: string,
  ): TierDialogueBlock | null {
    const source = this.getSource(sceneId, kind, sourceId);
    if (!source) return null;

    const key = this.sourceKey(sceneId, kind, sourceId);
    const done = this.tierCompleted()[key] ?? 0;

    for (const block of source.tiers) {
      if (block.tier <= done) continue;
      if (!this.meetsRequirements(block)) continue;
      return block;
    }
    return null;
  }

  private meetsRequirements(block: TierDialogueBlock): boolean {
    const clueCount = this.expediente.collectedClueIds().length;
    if (block.requiresMinClues && clueCount < block.requiresMinClues) return false;
    if (block.requiresClueIds?.length) {
      const have = this.expediente.collectedClueIds();
      if (!block.requiresClueIds.every((id) => have.includes(id))) return false;
    }
    return true;
  }

  documentsReviewedCount(sceneId: SceneId): number {
    const sources = this.sourcesFor(sceneId).filter((s) => s.kind === 'object');
    let n = 0;
    for (const s of sources) {
      const key = this.sourceKey(sceneId, 'object', s.sourceId);
      if ((this.tierCompleted()[key] ?? 0) > 0) n++;
    }
    return n;
  }

  reset(): void {
    this.tierCompleted.set({});
  }
}
