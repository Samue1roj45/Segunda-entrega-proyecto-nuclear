import { Injectable, computed, signal } from '@angular/core';

import { CLUES_CATALOG } from '../data/investigation/clues.catalog';
import { CONTRADICTIONS_CATALOG } from '../data/investigation/contradictions.catalog';
import { HECHOS_CANDIDATOS } from '../data/investigation/confirmed-facts.catalog';
import { EVIDENCIAS_CATALOG } from '../data/investigation/evidencias.catalog';
import { HYPOTHESIS_OPTIONS } from '../data/investigation/hypothesis.options';
import {
  PersonaEstado,
  DialogueTier,
} from '../data/investigation/investigation.types';
import { PERSONAS_CATALOG } from '../data/investigation/personas.catalog';
import { TIMELINE_CATALOG } from '../data/investigation/timeline.catalog';
import { SceneId } from '../data/scene-types';

export interface PistaEntry {
  id: string;
  text: string;
  discoveredAt: string;
  sceneId: SceneId;
}

export interface EvidenciaEntry {
  id: string;
  titulo: string;
  descripcion: string;
  discoveredAt: string;
  sceneId: SceneId;
}

export interface ContradiccionEntry {
  id: string;
  title: string;
  narrative: string;
  detectedAt: string;
}

export interface PersonaView {
  id: string;
  nombre: string;
  rol: string;
  color: string;
  shortLabel: string;
  sceneId: SceneId;
  estado: PersonaEstado;
}

export interface TimelineView {
  id: string;
  label: string;
  timeLabel?: string;
  discovered: boolean;
  order: number;
}

export interface HechoView {
  id: string;
  label: string;
  unlocked: boolean;
  marked: boolean;
}

export type RegistroFuente = 'entrevista' | 'documento' | 'observacion' | 'declaracion';

export interface RegistroProfesionalEntry {
  id: string;
  at: string;
  fuente: RegistroFuente;
  ubicacion: string;
  informante: string;
  dato: string;
  clueId?: string;
}

@Injectable({ providedIn: 'root' })
export class ExpedienteCasoService {
  private readonly clueIds = signal<string[]>([]);
  private readonly evidenciaIds = signal<string[]>([]);
  private readonly personaEstado = signal<Record<string, PersonaEstado>>({});
  private readonly markedFacts = signal<Set<string>>(new Set());
  private readonly hypothesisHospital = signal<string | null>(null);
  private readonly hypothesisComisaria = signal<string | null>(null);
  private readonly contradictions = signal<ContradiccionEntry[]>([]);
  private readonly decisionsTrace = signal<{ hospital?: string; comisaria?: string }>({});
  private readonly registros = signal<RegistroProfesionalEntry[]>([]);
  private readonly observedIds = signal<Set<string>>(new Set());

  readonly clueCount = computed(() => this.clueIds().length);

  readonly registroProfesional = this.registros.asReadonly();

  readonly ultimoRegistro = computed(() => {
    const list = this.registros();
    return list.length ? list[list.length - 1] : null;
  });

  readonly observationsCount = computed(() => this.observedIds().size);

  /** IDs de pistas registradas (lectura para desbloqueos de diálogo). */
  collectedClueIds(): string[] {
    return this.clueIds();
  }

  readonly pistas = computed<PistaEntry[]>(() =>
    this.clueIds()
      .map((id) => {
        const c = CLUES_CATALOG[id];
        if (!c) return null;
        return {
          id,
          text: c.text,
          discoveredAt: '',
          sceneId: c.sceneId,
        };
      })
      .filter((x): x is PistaEntry => x !== null),
  );

  readonly evidencias = computed<EvidenciaEntry[]>(() =>
    this.evidenciaIds()
      .map((id) => {
        const e = EVIDENCIAS_CATALOG.find((x) => x.id === id);
        if (!e) return null;
        return {
          id,
          titulo: e.titulo,
          descripcion: e.descripcion,
          discoveredAt: new Date().toISOString(),
          sceneId: e.sceneId,
        };
      })
      .filter((x): x is EvidenciaEntry => x !== null),
  );

  readonly personas = computed<PersonaView[]>(() =>
    PERSONAS_CATALOG.map((p) => ({
      id: p.id,
      nombre: p.nombre,
      rol: p.rol,
      color: p.color,
      shortLabel: p.shortLabel,
      sceneId: p.sceneId,
      estado: this.personaEstado()[p.id] ?? 'BLOQUEADO',
    })),
  );

  readonly contradicciones = this.contradictions.asReadonly();

  readonly hechos = computed<HechoView[]>(() => {
    const clues = this.clueIds();
    return HECHOS_CANDIDATOS.map((h) => ({
      id: h.id,
      label: h.label,
      unlocked: h.unlockClueIds.some((id) => clues.includes(id)),
      marked: this.markedFacts().has(h.id),
    }));
  });

  readonly timeline = computed<TimelineView[]>(() => {
    const clues = this.clueIds();
    return TIMELINE_CATALOG.map((t) => ({
      id: t.id,
      label: t.label,
      timeLabel: t.timeLabel,
      order: t.order,
      discovered: t.unlockClueIds.some((id) => clues.includes(id)),
    })).sort((a, b) => a.order - b.order);
  });

  readonly hypothesisHospitalLabel = computed(() => {
    const id = this.hypothesisHospital();
    return HYPOTHESIS_OPTIONS.find((o) => o.id === id)?.label ?? null;
  });

  readonly hypothesisComisariaLabel = computed(() => {
    const id = this.hypothesisComisaria();
    return HYPOTHESIS_OPTIONS.find((o) => o.id === id)?.label ?? null;
  });

  readonly hospitalInvestigationPercent = computed(() =>
    this.percentForScene('hospital', {
      minClues: 7,
      minPersonas: 4,
      minEvidencias: 2,
      minContradicciones: 1,
      minHechosMarcados: 1,
    }),
  );

  readonly comisariaInvestigationPercent = computed(() => {
    const comisariaClues = this.clueIds().filter((id) => id.startsWith('c-')).length;
    let score = 0;
    const targets = [
      comisariaClues >= 5,
      this.evidenciaIds().filter((id) =>
        EVIDENCIAS_CATALOG.find((e) => e.id === id && e.sceneId === 'comisaria'),
      ).length >= 2,
      this.contradictions().filter((c) => c.id.startsWith('con-v') || c.id.startsWith('con-')).length >= 1,
      this.personas().filter((p) => p.sceneId === 'comisaria' && p.estado !== 'BLOQUEADO').length >= 2,
    ];
    score = targets.filter(Boolean).length;
    return Math.round((score / 4) * 100);
  });

  readonly hospitalReady = computed(() => this.hospitalInvestigationPercent() >= 100);
  readonly comisariaReady = computed(() => this.comisariaInvestigationPercent() >= 100);

  addClue(
    clueId: string,
    ctx?: { fuente?: RegistroFuente; informante?: string; ubicacion?: string },
  ): void {
    const c = CLUES_CATALOG[clueId];
    if (!c) return;

    if (this.clueIds().includes(clueId)) return;

    this.clueIds.update((list) => [...list, clueId]);
    this.scanContradictions();

    this.appendRegistro({
      fuente: ctx?.fuente ?? 'declaracion',
      informante: ctx?.informante ?? 'Fuente del caso',
      ubicacion: ctx?.ubicacion ?? (c.sceneId === 'hospital' ? 'Hospital' : 'Comisaría'),
      dato: c.text,
      clueId,
    });
  }

  registerObservacion(
    observationId: string,
    label: string,
    text: string,
    ubicacion: string,
  ): boolean {
    if (this.observedIds().has(observationId)) return false;
    this.observedIds.update((s) => {
      const n = new Set(s);
      n.add(observationId);
      return n;
    });
    this.appendRegistro({
      fuente: 'observacion',
      informante: 'Entorno / ' + label,
      ubicacion,
      dato: text,
    });
    return true;
  }

  hasObserved(observationId: string): boolean {
    return this.observedIds().has(observationId);
  }

  registerDocumento(titulo: string, ubicacion: string, resumen: string): void {
    this.appendRegistro({
      fuente: 'documento',
      informante: titulo,
      ubicacion,
      dato: resumen,
    });
  }

  private appendRegistro(
    partial: Omit<RegistroProfesionalEntry, 'id' | 'at'> & { clueId?: string },
  ): void {
    const entry: RegistroProfesionalEntry = {
      id: `reg-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      at: new Date().toISOString(),
      ...partial,
    };
    this.registros.update((list) => [...list, entry]);
  }

  discoverPersona(personaId: string, tier: DialogueTier): void {
    const estadoMap: Record<DialogueTier, PersonaEstado> = {
      1: 'ENTREVISTA_T1',
      2: 'ENTREVISTA_T2',
      3: 'ENTREVISTA_T3',
    };
    const next = estadoMap[tier];
    const prev = this.personaEstado()[personaId];
    const order: PersonaEstado[] = [
      'BLOQUEADO',
      'DESCUBIERTO',
      'ENTREVISTA_T1',
      'ENTREVISTA_T2',
      'ENTREVISTA_T3',
    ];
    const prevIdx = order.indexOf(prev ?? 'BLOQUEADO');
    const nextIdx = order.indexOf(next);
    if (nextIdx > prevIdx) {
      this.personaEstado.update((m) => ({ ...m, [personaId]: next }));
    } else if (!prev || prev === 'BLOQUEADO') {
      this.personaEstado.update((m) => ({ ...m, [personaId]: 'DESCUBIERTO' }));
    }
  }

  discoverEvidencia(evidenciaId: string): void {
    if (this.evidenciaIds().includes(evidenciaId)) return;
    this.evidenciaIds.update((list) => [...list, evidenciaId]);
  }

  toggleHecho(hechoId: string): void {
    const h = HECHOS_CANDIDATOS.find((x) => x.id === hechoId);
    if (!h) return;
    const unlocked = h.unlockClueIds.some((id) => this.clueIds().includes(id));
    if (!unlocked) return;

    this.markedFacts.update((set) => {
      const next = new Set(set);
      if (next.has(hechoId)) next.delete(hechoId);
      else next.add(hechoId);
      return next;
    });
  }

  setHypothesis(scene: 'hospital' | 'comisaria', optionId: string): void {
    if (scene === 'hospital') this.hypothesisHospital.set(optionId);
    else this.hypothesisComisaria.set(optionId);
  }

  recordDecision(scene: 'hospital' | 'comisaria', actionTitle: string): void {
    this.decisionsTrace.update((t) => ({
      ...t,
      [scene]: actionTitle,
    }));
  }

  getSnapshot(participacionId: number) {
    return {
      participacionId,
      closedAt: new Date().toISOString(),
      personas: this.personas().filter((p) => p.estado !== 'BLOQUEADO'),
      evidencias: this.evidencias(),
      pistas: this.pistas(),
      contradicciones: this.contradictions(),
      hechosMarcados: this.hechos().filter((h) => h.marked),
      timeline: this.timeline().filter((t) => t.discovered),
      hypothesisHospital: this.hypothesisHospitalLabel(),
      hypothesisComisaria: this.hypothesisComisariaLabel(),
      decisions: this.decisionsTrace(),
      clueCount: this.clueCount(),
    };
  }

  reset(): void {
    this.clueIds.set([]);
    this.evidenciaIds.set([]);
    this.personaEstado.set({});
    this.markedFacts.set(new Set());
    this.hypothesisHospital.set(null);
    this.hypothesisComisaria.set(null);
    this.contradictions.set([]);
    this.decisionsTrace.set({});
    this.registros.set([]);
    this.observedIds.set(new Set());
  }

  private scanContradictions(): void {
    const have = new Set(this.clueIds());
    const existing = new Set(this.contradictions().map((c) => c.id));

    for (const def of CONTRADICTIONS_CATALOG) {
      if (existing.has(def.id)) continue;
      if (have.has(def.clueIdA) && have.has(def.clueIdB)) {
        this.contradictions.update((list) => [
          ...list,
          {
            id: def.id,
            title: def.title,
            narrative: def.narrative,
            detectedAt: new Date().toISOString(),
          },
        ]);
      }
    }
  }

  private percentForScene(
    sceneId: SceneId,
    req: {
      minClues: number;
      minPersonas: number;
      minEvidencias: number;
      minContradicciones: number;
      minHechosMarcados: number;
    },
  ): number {
    const prefix = sceneId === 'hospital' ? 'h-' : 'c-';
    const clues = this.clueIds().filter((id) => id.startsWith(prefix)).length;
    const personas = this.personas().filter(
      (p) => p.sceneId === sceneId && p.estado !== 'BLOQUEADO' && p.estado !== 'DESCUBIERTO',
    ).length;
    const evidencias = this.evidenciaIds().filter((id) =>
      EVIDENCIAS_CATALOG.some((e) => e.id === id && e.sceneId === sceneId),
    ).length;
    const contras = this.contradictions().filter((c) =>
      CONTRADICTIONS_CATALOG.find((d) => d.id === c.id)?.sceneId === sceneId,
    ).length;
    const hechos = this.markedFacts().size;

    const checks = [
      clues >= req.minClues,
      personas >= req.minPersonas,
      evidencias >= req.minEvidencias,
      contras >= req.minContradicciones,
      hechos >= req.minHechosMarcados,
    ];
    return Math.round((checks.filter(Boolean).length / checks.length) * 100);
  }
}
