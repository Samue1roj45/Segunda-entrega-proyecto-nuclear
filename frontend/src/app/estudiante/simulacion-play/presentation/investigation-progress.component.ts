import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { InvestigationProgressView } from '../services/investigation-stats.service';

@Component({
  selector: 'app-investigation-progress',
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="inv-progress" aria-label="Progreso de investigación">
      <span class="title"><mat-icon>track_changes</mat-icon> Avance investigativo</span>
      <ul>
        <li>
          <span>Entrevistas</span>
          <strong>{{ stats().entrevistasHechas }}/{{ stats().entrevistasTotal }}</strong>
        </li>
        <li>
          <span>Documentos</span>
          <strong>{{ stats().documentosRevisados }}/{{ stats().documentosTotal }}</strong>
        </li>
        <li>
          <span>Observaciones</span>
          <strong>{{ stats().observaciones }}</strong>
        </li>
      </ul>
    </div>
  `,
  styles: `
    .inv-progress {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      padding: 0.45rem 0.65rem;
      border-radius: 8px;
      background: rgba(15, 23, 42, 0.5);
      border: 1px solid rgba(100, 116, 139, 0.35);
      font-size: 0.72rem;
      color: #cbd5e1;
    }
    .title {
      display: flex;
      align-items: center;
      gap: 0.3rem;
      font-weight: 700;
      color: #67e8f9;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      font-size: 0.65rem;
    }
    .title mat-icon { font-size: 14px; width: 14px; height: 14px; }
    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem 0.85rem;
    }
    li {
      display: flex;
      gap: 0.35rem;
      align-items: baseline;
    }
    li strong { color: #f8fafc; font-variant-numeric: tabular-nums; }
  `,
})
export class InvestigationProgressComponent {
  readonly stats = input.required<InvestigationProgressView>();
}
