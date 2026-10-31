import { Component, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { INTRO_NARRATIVA_ESCENAS } from '../data/intro-narrativa.content';

@Component({
  selector: 'app-intro-narrativa',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="intro-shell">
      @for (escena of escenas; track escena.id; let i = $index) {
        @if (index() === i) {
          <article class="escena">
            <div
              class="visual"
              [style.background]="visualBg(escena)">
              <img [src]="escena.imageUrl" [alt]="escena.id" (error)="onImgError(escena.id)" />
            </div>
            <div class="copy">
              @for (line of escena.lines; track line) {
                <p>{{ line }}</p>
              }
            </div>
          </article>
        }
      }
      <footer class="intro-foot">
        <span class="progress">{{ index() + 1 }} / {{ escenas.length }}</span>
        <button mat-flat-button color="primary" (click)="siguiente()">
          @if (index() < escenas.length - 1) {
            <span>Siguiente</span>
          } @else {
            <span>Continuar</span>
          }
          <mat-icon iconPositionEnd>{{ index() < escenas.length - 1 ? 'arrow_forward' : 'check' }}</mat-icon>
        </button>
      </footer>
    </div>
  `,
  styles: `
    .intro-shell {
      position: fixed;
      inset: 0;
      z-index: 60;
      display: flex;
      flex-direction: column;
      background: #0f172a;
      color: #f1f5f9;
    }
    .escena {
      flex: 1;
      display: flex;
      flex-direction: column;
      animation: fadeIn 0.6s ease;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .visual {
      flex: 1;
      min-height: 45vh;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }
    .visual img {
      max-width: min(900px, 95vw);
      max-height: 50vh;
      object-fit: contain;
      border-radius: 12px;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.45);
    }
    .copy {
      padding: 1.5rem 2rem 1rem;
      max-width: 720px;
      margin: 0 auto;
      text-align: center;
    }
    .copy p {
      margin: 0 0 0.65rem;
      font-size: 1.05rem;
      line-height: 1.55;
      color: #cbd5e1;
    }
    .intro-foot {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem 1.5rem;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }
    .progress { font-size: 0.9rem; color: #94a3b8; }
    button mat-icon { margin-left: 0.35rem; }
  `,
})
export class IntroNarrativaComponent {
  readonly finished = output<void>();

  readonly escenas = INTRO_NARRATIVA_ESCENAS;
  readonly index = signal(0);
  private readonly brokenImages = signal<Set<string>>(new Set());

  visualBg(escena: (typeof INTRO_NARRATIVA_ESCENAS)[0]): string {
    if (this.brokenImages().has(escena.id)) return escena.fallbackGradient;
    return '#0f172a';
  }

  onImgError(id: string): void {
    this.brokenImages.update((s) => new Set(s).add(id));
  }

  siguiente(): void {
    if (this.index() < this.escenas.length - 1) {
      this.index.update((i) => i + 1);
      return;
    }
    this.finished.emit();
  }
}
