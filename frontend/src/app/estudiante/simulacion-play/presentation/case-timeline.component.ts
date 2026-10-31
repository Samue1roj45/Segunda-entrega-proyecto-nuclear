import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ExpedienteCasoService } from '../services/expediente-caso.service';

@Component({
  selector: 'app-case-timeline',
  imports: [CommonModule, MatIconModule],
  template: `
    <section class="case-tl" aria-label="Cronología del caso">
      <header>
        <mat-icon>schedule</mat-icon>
        <span>Cronología reconstruida</span>
      </header>
      <ol>
        @for (e of expediente.timeline(); track e.id) {
          <li [class.hidden]="!e.discovered" [class.visible]="e.discovered">
            @if (e.discovered) {
              <span class="time">{{ e.timeLabel ?? '—' }}</span>
              <span class="evt">{{ e.label }}</span>
            } @else {
              <span class="time muted">—</span>
              <span class="evt muted">Por determinar con más fuentes</span>
            }
          </li>
        }
      </ol>
    </section>
  `,
  styles: `
    .case-tl {
      margin-top: 0.75rem;
      padding: 0.65rem 0.75rem;
      border-radius: 4px;
      background: rgba(255, 255, 255, 0.7);
      border: 1px solid #d6d3d1;
      border-left: 3px solid #a16207;
      font-family: Georgia, serif;
    }
    header {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.65rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #92400e;
      margin-bottom: 0.45rem;
      font-family: system-ui, sans-serif;
    }
    header mat-icon { font-size: 15px; width: 15px; height: 15px; }
    ol {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    li {
      display: grid;
      grid-template-columns: 3.5rem 1fr;
      gap: 0.5rem;
      align-items: start;
      padding: 0.35rem 0;
      border-bottom: 1px dotted #d6d3d1;
    }
    li:last-child { border-bottom: none; }
    li.hidden { opacity: 0.4; }
    li.visible .time { color: #92400e; font-weight: 700; }
    .time {
      font-size: 0.72rem;
      font-variant-numeric: tabular-nums;
      font-family: system-ui, sans-serif;
    }
    .evt { font-size: 0.8rem; line-height: 1.35; color: #334155; }
    .muted { color: #a8a29e; font-style: italic; }
  `,
})
export class CaseTimelineComponent {
  readonly expediente = inject(ExpedienteCasoService);
}
