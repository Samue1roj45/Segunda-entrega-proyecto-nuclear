import { CommonModule } from '@angular/common';
import { Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { RiskCardDef } from '../data/minigames.content';

@Component({
  selector: 'app-risk-minigame',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <section class="mg-shell">
      <header>
        <mat-icon>category</mat-icon>
        <div>
          <h2>{{ title() }}</h2>
          <p>{{ subtitle() }}</p>
        </div>
      </header>
      <p class="hint">Clasifica cada elemento en la columna correcta.</p>
      <div class="bins">
        <div class="bin risk">
          <h3><mat-icon>warning</mat-icon> Factores de riesgo</h3>
          @for (card of cards(); track card.id) {
            @if (assignments()[card.id] === 'risk') {
              <button type="button" class="chip" (click)="unassign(card.id)">{{ card.text }}</button>
            }
          }
        </div>
        <div class="bin protection">
          <h3><mat-icon>shield</mat-icon> Medidas de protección</h3>
          @for (card of cards(); track card.id) {
            @if (assignments()[card.id] === 'protection') {
              <button type="button" class="chip" (click)="unassign(card.id)">{{ card.text }}</button>
            }
          }
        </div>
      </div>
      <div class="pool">
        <h4>Por clasificar</h4>
        @for (card of unassigned(); track card.id) {
          <div class="card-row">
            <span>{{ card.text }}</span>
            <div>
              <button mat-stroked-button type="button" (click)="assign(card.id, 'risk')">Riesgo</button>
              <button mat-stroked-button type="button" (click)="assign(card.id, 'protection')">Protección</button>
            </div>
          </div>
        }
      </div>
      @if (submitted()) {
        <p class="narrative-msg">El expediente incorpora tu clasificación de riesgos y medidas de protección.</p>
      }
      <button mat-flat-button color="primary" [disabled]="unassigned().length > 0 && !submitted()" (click)="submit()">
        {{ submitted() ? 'Continuar' : 'Entregar al equipo' }}
      </button>
    </section>
  `,
  styles: `
    .mg-shell { display: flex; flex-direction: column; gap: 1rem; }
    .bins { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
    .bin {
      min-height: 120px;
      padding: 0.65rem;
      border-radius: 10px;
      border: 2px dashed var(--mat-sys-outline-variant);
    }
    .bin.risk { border-color: #f87171; background: #fef2f2; }
    .bin.protection { border-color: #34d399; background: #ecfdf5; }
    .bin h3 { margin: 0 0 0.5rem; font-size: 0.85rem; display: flex; align-items: center; gap: 0.35rem; }
    .chip {
      display: block;
      width: 100%;
      margin-bottom: 0.35rem;
      padding: 0.4rem 0.5rem;
      border: none;
      border-radius: 6px;
      background: rgba(255,255,255,0.8);
      font-size: 0.78rem;
      text-align: left;
      cursor: pointer;
    }
    .pool h4 { margin: 0 0 0.5rem; font-size: 0.85rem; }
    .card-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 0;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      font-size: 0.88rem;
    }
    .narrative-msg { margin: 0; padding: 0.65rem; border-radius: 8px; background: var(--mat-sys-surface-container); font-size: 0.88rem; }
    @media (max-width: 700px) { .bins { grid-template-columns: 1fr; } }
  `,
})
export class RiskMinigameComponent {
  readonly title = input('Clasificación riesgo / protección');
  readonly subtitle = input('Diferencia factores que aumentan el daño de medidas que lo reducen.');
  readonly cards = input.required<RiskCardDef[]>();
  readonly completed = output<void>();

  readonly assignments = signal<Record<string, 'risk' | 'protection'>>({});
  readonly submitted = signal(false);

  unassigned(): RiskCardDef[] {
    const a = this.assignments();
    return this.cards().filter((c) => !a[c.id]);
  }

  assign(id: string, bin: 'risk' | 'protection'): void {
    if (this.submitted()) return;
    this.assignments.update((prev) => ({ ...prev, [id]: bin }));
  }

  unassign(id: string): void {
    if (this.submitted()) return;
    this.assignments.update((prev) => {
      const next = { ...prev };
      delete next[id];
      return next;
    });
  }

  submit(): void {
    if (this.submitted()) {
      this.completed.emit();
      return;
    }
    this.submitted.set(true);
  }
}
