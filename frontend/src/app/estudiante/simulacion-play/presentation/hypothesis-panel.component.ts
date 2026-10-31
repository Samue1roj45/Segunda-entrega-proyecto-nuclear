import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { HYPOTHESIS_OPTIONS } from '../data/investigation/hypothesis.options';

@Component({
  selector: 'app-hypothesis-panel',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="gate-overlay">
      <article class="hyp-card">
        <mat-icon>psychology</mat-icon>
        <h2>{{ title() }}</h2>
        <p>{{ subtitle() }}</p>
        <div class="options">
          @for (opt of options; track opt.id) {
            <button type="button" class="hyp-opt" (click)="selected.emit(opt.id)">
              {{ opt.label }}
            </button>
          }
        </div>
      </article>
    </div>
  `,
  styles: `
    .gate-overlay {
      position: fixed;
      inset: 0;
      z-index: 30;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(15, 23, 42, 0.55);
      padding: 1rem;
    }
    .hyp-card {
      max-width: 480px;
      width: 100%;
      padding: 1.75rem;
      background: var(--mat-sys-surface);
      border-radius: 16px;
      text-align: center;
    }
    .hyp-card mat-icon { font-size: 40px; width: 40px; height: 40px; color: var(--mat-sys-primary); }
    h2 { margin: 0.5rem 0; font-size: 1.25rem; }
    p { margin: 0 0 1rem; color: var(--mat-sys-on-surface-variant); font-size: 0.9rem; line-height: 1.45; }
    .options { display: flex; flex-direction: column; gap: 0.5rem; text-align: left; }
    .hyp-opt {
      padding: 0.75rem 1rem;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 10px;
      background: var(--mat-sys-surface-container-low);
      cursor: pointer;
      font-size: 0.92rem;
      text-align: left;
    }
    .hyp-opt:hover { border-color: var(--mat-sys-primary); background: var(--mat-sys-primary-container); }
  `,
})
export class HypothesisPanelComponent {
  readonly title = input('Formular hipótesis');
  readonly subtitle = input('Registra tu interpretación profesional del caso hasta ahora. No hay calificación.');
  readonly selected = output<string>();
  readonly options = HYPOTHESIS_OPTIONS;
}
