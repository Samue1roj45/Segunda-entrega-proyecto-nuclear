import { Injectable, computed, signal } from '@angular/core';

import { NarrativeDelta } from '../data/decisions.content';

export interface NarrativeAxes {
  confianzaFamiliar: number;
  proteccionDerechos: number;
  calidadIntervencion: number;
  riesgoRevictimizacion: number;
}

export interface NarrativeAxisMeta {
  key: keyof NarrativeAxes;
  label: string;
  icon: string;
  /** true = higher is better; false = lower is better (riesgo). */
  higherIsBetter: boolean;
}

export const NARRATIVE_AXES: NarrativeAxisMeta[] = [
  { key: 'confianzaFamiliar', label: 'Confianza Familiar', icon: 'family_restroom', higherIsBetter: true },
  { key: 'proteccionDerechos', label: 'Protección de Derechos', icon: 'gavel', higherIsBetter: true },
  { key: 'calidadIntervencion', label: 'Calidad de Intervención', icon: 'verified', higherIsBetter: true },
  { key: 'riesgoRevictimizacion', label: 'Riesgo de Revictimización', icon: 'warning', higherIsBetter: false },
];

const INITIAL: NarrativeAxes = {
  confianzaFamiliar: 50,
  proteccionDerechos: 50,
  calidadIntervencion: 50,
  riesgoRevictimizacion: 40,
};

@Injectable({ providedIn: 'root' })
export class NarrativeStateService {
  private readonly axes = signal<NarrativeAxes>({ ...INITIAL });

  readonly state = this.axes.asReadonly();

  readonly displayBars = computed(() =>
    NARRATIVE_AXES.map((meta) => ({
      ...meta,
      value: this.axes()[meta.key],
    })),
  );

  applyDelta(delta: NarrativeDelta): void {
    this.axes.update((prev) => ({
      confianzaFamiliar: this.clamp(prev.confianzaFamiliar + delta.confianzaFamiliar),
      proteccionDerechos: this.clamp(prev.proteccionDerechos + delta.proteccionDerechos),
      calidadIntervencion: this.clamp(prev.calidadIntervencion + delta.calidadIntervencion),
      riesgoRevictimizacion: this.clamp(prev.riesgoRevictimizacion + delta.riesgoRevictimizacion),
    }));
  }

  applyMinigameBonus(kind: 'timeline' | 'route' | 'risk', scoreRatio: number): void {
    const bonus = Math.round(scoreRatio * 8);
    const penalty = Math.round((1 - scoreRatio) * 6);
    if (kind === 'timeline') {
      this.applyDelta({
        confianzaFamiliar: 0,
        proteccionDerechos: bonus,
        calidadIntervencion: bonus,
        riesgoRevictimizacion: penalty,
      });
    } else if (kind === 'route') {
      this.applyDelta({
        confianzaFamiliar: bonus / 2,
        proteccionDerechos: bonus,
        calidadIntervencion: bonus,
        riesgoRevictimizacion: -bonus / 2,
      });
    } else {
      this.applyDelta({
        confianzaFamiliar: 0,
        proteccionDerechos: bonus,
        calidadIntervencion: bonus / 2,
        riesgoRevictimizacion: -bonus,
      });
    }
  }

  reset(): void {
    this.axes.set({ ...INITIAL });
  }

  private clamp(v: number): number {
    return Math.max(0, Math.min(100, v));
  }
}
