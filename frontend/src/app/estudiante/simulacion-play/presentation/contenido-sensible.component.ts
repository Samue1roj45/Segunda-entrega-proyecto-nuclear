import { Component, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-contenido-sensible',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="warn-title">
      <article class="modal-card">
        <mat-icon class="warn-icon">health_and_safety</mat-icon>
        <h2 id="warn-title">Contenido sensible</h2>
        <p class="lead">Esta simulación aborda temas que pueden resultar emotivamente intensos:</p>
        <ul>
          <li>Violencia intrafamiliar</li>
          <li>Violencia basada en género</li>
          <li>Feminicidio</li>
          <li>Muerte de una menor de edad</li>
        </ul>
        <p class="note">No contiene imágenes explícitas. El enfoque es profesional e investigativo.</p>
        <div class="actions">
          <button mat-stroked-button type="button" (click)="salir.emit()">
            <mat-icon>logout</mat-icon>
            Salir
          </button>
          <button mat-flat-button color="primary" type="button" (click)="continuar.emit()">
            Continuar
            <mat-icon>arrow_forward</mat-icon>
          </button>
        </div>
      </article>
    </div>
  `,
  styles: `
    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 70;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
      background: rgba(15, 23, 42, 0.88);
    }
    .modal-card {
      max-width: 480px;
      width: 100%;
      padding: 2rem;
      border-radius: 16px;
      background: var(--mat-sys-surface);
      text-align: center;
      box-shadow: 0 24px 48px rgba(0, 0, 0, 0.25);
    }
    .warn-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: var(--mat-sys-error);
    }
    h2 { margin: 0.5rem 0 1rem; font-size: 1.35rem; }
    .lead { margin: 0 0 0.75rem; color: var(--mat-sys-on-surface-variant); line-height: 1.45; }
    ul {
      text-align: left;
      margin: 0 auto 1rem;
      max-width: 320px;
      line-height: 1.6;
    }
    .note {
      margin: 0 0 1.5rem;
      font-size: 0.88rem;
      color: var(--mat-sys-on-surface-variant);
    }
    .actions {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
      flex-wrap: wrap;
    }
  `,
})
export class ContenidoSensibleComponent {
  readonly continuar = output<void>();
  readonly salir = output<void>();
}
