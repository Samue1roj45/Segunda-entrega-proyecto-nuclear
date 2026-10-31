import { CommonModule } from '@angular/common';
import { Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { RouteStepDef } from '../data/minigames.content';

@Component({
  selector: 'app-route-minigame',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <section class="mg-shell">
      <header>
        <mat-icon>route</mat-icon>
        <div>
          <h2>{{ title() }}</h2>
          <p>{{ subtitle() }}</p>
        </div>
      </header>
      <p class="hint">Selecciona los pasos que integrarías en la ruta de atención (mínimo 3).</p>
      <div class="steps">
        @for (step of steps(); track step.id) {
          <button
            type="button"
            class="step-card"
            [class.selected]="selected().has(step.id)"
            (click)="toggle(step.id)">
            <mat-icon>{{ selected().has(step.id) ? 'check_circle' : 'radio_button_unchecked' }}</mat-icon>
            <div>
              <strong>{{ step.label }}</strong>
              <span>{{ step.description }}</span>
            </div>
          </button>
        }
      </div>
      @if (submitted()) {
        <p class="narrative-msg">La mesa interinstitucional registra tu propuesta de ruta de atención.</p>
      }
      <button mat-flat-button color="primary" [disabled]="selected().size < 3 && !submitted()" (click)="submit()">
        {{ submitted() ? 'Continuar' : 'Entregar al equipo' }}
      </button>
    </section>
  `,
  styles: `
    .mg-shell { display: flex; flex-direction: column; gap: 1rem; }
    header { display: flex; gap: 0.75rem; }
    header mat-icon { color: var(--mat-sys-primary); }
    h2 { margin: 0; font-size: 1.2rem; }
    header p, .hint { margin: 0; font-size: 0.88rem; color: var(--mat-sys-on-surface-variant); }
    .steps { display: flex; flex-direction: column; gap: 0.5rem; }
    .step-card {
      display: flex;
      gap: 0.65rem;
      align-items: flex-start;
      text-align: left;
      padding: 0.75rem;
      border-radius: 10px;
      border: 2px solid var(--mat-sys-outline-variant);
      background: var(--mat-sys-surface);
      cursor: pointer;
    }
    .step-card.selected { border-color: var(--mat-sys-primary); background: color-mix(in srgb, var(--mat-sys-primary) 8%, transparent); }
    .step-card strong { display: block; font-size: 0.92rem; }
    .step-card span { font-size: 0.82rem; color: var(--mat-sys-on-surface-variant); }
    .narrative-msg { margin: 0; padding: 0.65rem; border-radius: 8px; background: var(--mat-sys-surface-container); font-size: 0.88rem; }
  `,
})
export class RouteMinigameComponent {
  readonly title = input('Ruta de atención');
  readonly subtitle = input('Diseña la secuencia interinstitucional coherente con el caso.');
  readonly steps = input.required<RouteStepDef[]>();
  readonly completed = output<void>();

  readonly selected = signal<Set<string>>(new Set());
  readonly submitted = signal(false);

  toggle(id: string): void {
    if (this.submitted()) return;
    const next = new Set(this.selected());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.selected.set(next);
  }

  submit(): void {
    if (this.submitted()) {
      this.completed.emit();
      return;
    }
    this.submitted.set(true);
  }
}
