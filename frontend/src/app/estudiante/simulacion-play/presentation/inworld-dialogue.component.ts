import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { DialogueLine } from '../data/hospital-scene.content';

@Component({
  selector: 'app-inworld-dialogue',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="cinema-bar top" aria-hidden="true"></div>
    <div class="iw-dialogue" role="dialog" aria-live="polite">
      <aside class="portrait-column">
        <div class="portrait-ring" [style.--portrait-color]="portraitColor()">
          @if (portraitUrl() && kind() === 'npc') {
            <img class="portrait-img" [src]="portraitUrl()" alt="" />
          } @else {
            <div class="portrait" [style.background]="portraitColor()">
              <mat-icon>{{ kind() === 'npc' ? 'person' : 'description' }}</mat-icon>
            </div>
          }
        </div>
        @if (subtitle()) {
          <span class="portrait-kicker">{{ subtitle() | uppercase }}</span>
        }
        @if (emotion()) {
          <span class="portrait-mood">{{ emotion() }}</span>
        }
      </aside>
      <article class="dialogue-panel">
        <header>
          <div class="speaker-block">
            <strong>{{ title() }}</strong>
          </div>
          @if (progress()) {
            <span class="prog">{{ progress() }}</span>
          }
          <button type="button" class="close" (click)="closed.emit()" aria-label="Cerrar">
            <mat-icon>close</mat-icon>
          </button>
        </header>
        @if (line(); as l) {
          @if (l.speaker) {
            <span class="speaker">{{ l.speaker }}</span>
          }
          <p class="text">{{ l.text }}</p>
        }
        <footer>
          <button mat-flat-button class="advance-btn" (click)="advance.emit()">
            {{ isLast() ? registerLabel() : 'Continuar' }}
            <mat-icon>chevron_right</mat-icon>
          </button>
        </footer>
      </article>
    </div>
    <div class="cinema-bar bottom" aria-hidden="true"></div>
  `,
  styles: `
    :host {
      position: absolute;
      inset: 0;
      z-index: 50;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      pointer-events: none;
    }

    .cinema-bar {
      flex-shrink: 0;
      height: 8%;
      min-height: 28px;
      background: linear-gradient(180deg, #020617, rgba(2, 6, 23, 0.92));
      pointer-events: none;
    }

    .cinema-bar.bottom {
      background: linear-gradient(0deg, #020617, transparent);
      min-height: 12px;
      height: 4%;
    }

    .iw-dialogue {
      display: flex;
      align-items: flex-end;
      gap: 0;
      padding: 0 2rem 1.25rem;
      pointer-events: auto;
      animation: dialogueIn 0.5s cubic-bezier(0.22, 1, 0.36, 1);
      max-height: 46%;
    }

    @keyframes dialogueIn {
      from { opacity: 0; transform: translateY(16px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .portrait-column {
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.4rem;
      padding-right: 1.15rem;
      min-width: 120px;
      margin-bottom: 0.35rem;
    }

    .portrait-ring {
      padding: 4px;
      border-radius: 20px;
      background: linear-gradient(160deg, var(--portrait-color, #0ea5e9) 0%, #020617 70%);
      box-shadow:
        0 16px 40px rgba(0, 0, 0, 0.6),
        inset 0 1px 0 rgba(255, 255, 255, 0.12);
    }

    .portrait,
    .portrait-img {
      width: 108px;
      height: 128px;
      border-radius: 16px 16px 24px 16px;
      border: 2px solid rgba(255, 255, 255, 0.22);
      object-fit: cover;
      object-position: center 12%;
    }

    .portrait {
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        color: #fff;
        font-size: 40px;
        width: 40px;
        height: 40px;
        opacity: 0.9;
      }
    }

    .portrait-kicker {
      font-size: 0.62rem;
      font-weight: 800;
      letter-spacing: 0.12em;
      color: #94a3b8;
      text-align: center;
      max-width: 110px;
      line-height: 1.3;
    }

    .portrait-mood {
      font-size: 0.68rem;
      color: #fcd34d;
      font-style: italic;
      text-align: center;
    }

    .dialogue-panel {
      flex: 1;
      min-width: 0;
      padding: 1rem 1.25rem 1.1rem;
      border-radius: 4px 16px 16px 4px;
      background: linear-gradient(135deg, rgba(15, 23, 42, 0.97) 0%, rgba(2, 6, 23, 0.98) 100%);
      border: 1px solid rgba(148, 163, 184, 0.25);
      border-left: 4px solid var(--portrait-color, #38bdf8);
      color: #f1f5f9;
      box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
    }

    header {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }

    .speaker-block strong {
      font-size: 1.05rem;
      font-weight: 700;
      letter-spacing: 0.02em;
    }

    header .prog {
      font-size: 0.7rem;
      opacity: 0.65;
      white-space: nowrap;
    }

    header .close {
      background: transparent;
      border: none;
      color: #64748b;
      cursor: pointer;
      padding: 0;
      margin-left: auto;
      display: flex;
    }

    .speaker {
      display: block;
      font-size: 0.68rem;
      font-weight: 700;
      color: #7dd3fc;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      margin-bottom: 0.25rem;
    }

    .text {
      margin: 0;
      line-height: 1.6;
      font-size: 1rem;
      color: #e2e8f0;
    }

    footer {
      margin-top: 0.85rem;
    }

    .advance-btn {
      background: linear-gradient(90deg, #0369a1, #0ea5e9) !important;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      font-size: 0.78rem;
      font-weight: 600;
    }
  `,
})
export class InworldDialogueComponent {
  readonly title = input.required<string>();
  readonly subtitle = input('');
  readonly emotion = input('');
  readonly portraitUrl = input('');
  readonly line = input<DialogueLine | null>(null);
  readonly progress = input('');
  readonly isLast = input(false);
  readonly kind = input<'npc' | 'object'>('npc');
  readonly portraitColor = input('#0ea5e9');
  readonly registerLabel = input('Registrar en bitácora');

  readonly advance = output<void>();
  readonly closed = output<void>();
}
