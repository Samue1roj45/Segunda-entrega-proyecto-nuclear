import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { DialogueLine } from '../data/hospital-scene.content';

@Component({
  selector: 'app-document-viewer',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="doc-overlay" role="dialog" aria-labelledby="doc-title">
      <article class="doc-card">
        <header>
          <mat-icon>description</mat-icon>
          <h2 id="doc-title">{{ title() }}</h2>
          <button type="button" class="close" (click)="closed.emit()" aria-label="Cerrar sin registrar">
            <mat-icon>close</mat-icon>
          </button>
        </header>
        <div class="doc-body">
          @for (line of lines(); track $index) {
            @if (line.speaker) {
              <h3>{{ line.speaker }}</h3>
            }
            <p>{{ line.text }}</p>
          }
          @if (hasMore()) {
            <p class="more-hint">
              <mat-icon>info</mat-icon>
              Podrás revisar más secciones tras avanzar en la investigación.
            </p>
          }
        </div>
        <footer>
          @if (canRegister()) {
            <button mat-flat-button color="primary" (click)="register.emit()">
              <mat-icon>folder_open</mat-icon>
              Registrar en expediente
            </button>
          } @else {
            <button mat-stroked-button disabled>Ya registrado</button>
          }
          <button mat-button (click)="closed.emit()">Cerrar</button>
        </footer>
      </article>
    </div>
  `,
  styles: `
    .doc-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(15, 23, 42, 0.6);
      z-index: 20;
      padding: 1rem;
    }
    .doc-card {
      background: #fffef8;
      border: 1px solid #d6d3d1;
      border-radius: 8px;
      max-width: 520px;
      width: 100%;
      max-height: 85%;
      display: flex;
      flex-direction: column;
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
    }
    header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      border-bottom: 2px solid #78716c;
      background: #f5f5f4;
    }
    h2 {
      flex: 1;
      margin: 0;
      font-size: 1rem;
    }
    .close {
      border: none;
      background: transparent;
      cursor: pointer;
    }
    .doc-body {
      padding: 1rem 1.25rem;
      overflow-y: auto;
      flex: 1;
      font-family: Georgia, 'Times New Roman', serif;
      font-size: 0.95rem;
      line-height: 1.55;
      color: #292524;
    }
    .doc-body h3 {
      margin: 1rem 0 0.25rem;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #78716c;
    }
    .doc-body p {
      margin: 0 0 0.75rem;
    }
    .more-hint {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      font-family: system-ui, sans-serif;
      font-size: 0.8rem;
      color: #64748b;
      margin-top: 1rem;
    }
    footer {
      display: flex;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      border-top: 1px solid #e7e5e4;
      flex-wrap: wrap;
    }
  `,
})
export class DocumentViewerComponent {
  title = input.required<string>();
  lines = input.required<DialogueLine[]>();
  canRegister = input(true);
  hasMore = input(false);

  register = output<void>();
  closed = output<void>();
}
