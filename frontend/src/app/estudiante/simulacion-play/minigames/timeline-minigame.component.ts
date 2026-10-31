import { CommonModule } from '@angular/common';
import { Component, OnInit, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { TimelineEventDef } from '../data/minigames.content';

@Component({
  selector: 'app-timeline-minigame',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <section class="mg-shell">
      <header>
        <mat-icon>timeline</mat-icon>
        <div>
          <h2>{{ title() }}</h2>
          <p>{{ subtitle() }}</p>
        </div>
      </header>
      <p class="hint">Ordena los hechos según tu reconstrucción del caso (usa las flechas).</p>
      <ul class="events">
        @for (ev of order(); track ev.id; let i = $index) {
          <li>
            <span class="num">{{ i + 1 }}</span>
            <span class="label">{{ ev.label }}</span>
            <div class="actions">
              <button type="button" mat-icon-button (click)="move(i, -1)" [disabled]="i === 0" aria-label="Subir">
                <mat-icon>arrow_upward</mat-icon>
              </button>
              <button type="button" mat-icon-button (click)="move(i, 1)" [disabled]="i === order().length - 1" aria-label="Bajar">
                <mat-icon>arrow_downward</mat-icon>
              </button>
            </div>
          </li>
        }
      </ul>
      @if (submitted()) {
        <p class="narrative-msg">El equipo archiva tu reconstrucción cronológica en el expediente del caso.</p>
      }
      <button mat-flat-button color="primary" (click)="submit()">
        {{ submitted() ? 'Continuar' : 'Entregar al equipo' }}
      </button>
    </section>
  `,
  styles: `
    .mg-shell { display: flex; flex-direction: column; gap: 1rem; }
    header { display: flex; gap: 0.75rem; align-items: flex-start; }
    header mat-icon { color: var(--mat-sys-primary); }
    h2 { margin: 0; font-size: 1.2rem; }
    header p, .hint { margin: 0; font-size: 0.88rem; color: var(--mat-sys-on-surface-variant); line-height: 1.45; }
    .events { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.5rem; }
    li {
      display: grid;
      grid-template-columns: 28px 1fr auto;
      gap: 0.5rem;
      align-items: center;
      padding: 0.65rem 0.75rem;
      border-radius: 10px;
      background: var(--mat-sys-surface-container-low);
    }
    .num { font-weight: 700; color: var(--mat-sys-primary); }
    .label { font-size: 0.9rem; }
    .narrative-msg { margin: 0; padding: 0.65rem; border-radius: 8px; background: var(--mat-sys-surface-container); font-size: 0.88rem; }
  `,
})
export class TimelineMinigameComponent implements OnInit {
  readonly title = input('Reconstrucción cronológica');
  readonly subtitle = input('Organiza los eventos según la información que reuniste.');
  readonly events = input.required<TimelineEventDef[]>();
  readonly completed = output<void>();

  readonly order = signal<TimelineEventDef[]>([]);
  readonly submitted = signal(false);

  ngOnInit(): void {
    this.order.set([...this.events()]);
  }

  move(index: number, dir: -1 | 1): void {
    if (this.submitted()) return;
    const list = [...this.order()];
    const target = index + dir;
    if (target < 0 || target >= list.length) return;
    [list[index], list[target]] = [list[target], list[index]];
    this.order.set(list);
  }

  submit(): void {
    if (this.submitted()) {
      this.completed.emit();
      return;
    }
    this.submitted.set(true);
  }
}
