import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { InterviewTopicView } from '../services/interview.service';

@Component({
  selector: 'app-interview-panel',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="interview-overlay" role="dialog" aria-labelledby="interview-title">
      <article class="interview-card">
        <div class="card-accent"></div>
        <header>
          <aside class="portrait-wrap">
            @if (portraitUrl()) {
              <img [src]="portraitUrl()" alt="" class="portrait-img" />
            } @else {
              <span class="portrait-fallback" [style.background]="portraitColor()">
                <mat-icon>person</mat-icon>
              </span>
            }
          </aside>
          <div class="identity">
            @if (npcRole()) {
              <span class="kicker">{{ npcRole() | uppercase }}</span>
            }
            <h2 id="interview-title">{{ npcLabel() }}</h2>
            @if (npcEmotion()) {
              <p class="emotion">{{ npcEmotion() }}</p>
            }
            <p class="sub">Indaga con cuidado profesional — cada línea abre una vía de investigación</p>
          </div>
          <button type="button" class="close" (click)="finished.emit()" aria-label="Finalizar entrevista">
            <mat-icon>close</mat-icon>
          </button>
        </header>
        <ul class="topics">
          @for (t of topics(); track t.id) {
            <li>
              <button
                type="button"
                class="topic-btn"
                [disabled]="t.locked || t.done"
                (click)="topicSelected.emit(t.id)">
                <mat-icon>{{ t.done ? 'check_circle' : t.locked ? 'lock' : 'record_voice_over' }}</mat-icon>
                <span class="topic-text">{{ t.label }}</span>
                @if (t.lockReason) {
                  <small>{{ t.lockReason }}</small>
                }
              </button>
            </li>
          }
        </ul>
        <footer>
          <button mat-stroked-button (click)="finished.emit()">
            <mat-icon>logout</mat-icon>
            Finalizar entrevista
          </button>
        </footer>
      </article>
    </div>
  `,
  styles: `
    .interview-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(2, 6, 23, 0.72);
      backdrop-filter: blur(6px);
      z-index: 20;
      padding: 1rem;
    }

    .interview-card {
      position: relative;
      background: linear-gradient(180deg, #fefce8 0%, #fffbeb 12%, #ffffff 100%);
      border-radius: 4px 16px 16px 4px;
      max-width: 440px;
      width: 100%;
      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
      overflow: hidden;
      border: 1px solid #d6d3d1;
    }

    .card-accent {
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 6px;
      background: linear-gradient(180deg, #ca8a04, #a16207);
    }

    header {
      display: grid;
      grid-template-columns: auto 1fr auto;
      gap: 0.85rem;
      padding: 1.1rem 1.1rem 0.65rem 1.25rem;
      align-items: start;
      border-bottom: 1px dashed #d6d3d1;
    }

    .portrait-wrap {
      width: 96px;
      height: 118px;
      border-radius: 8px 8px 28px 8px;
      overflow: hidden;
      box-shadow:
        0 12px 28px rgba(0, 0, 0, 0.28),
        inset 0 0 0 1px rgba(255, 255, 255, 0.4);
      border: 2px solid #d6d3d1;
      background: #1e293b;
    }

    .portrait-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center 8%;
    }

    .portrait-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .portrait-fallback mat-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
    }

    .kicker {
      display: block;
      font-size: 0.62rem;
      font-weight: 800;
      letter-spacing: 0.14em;
      color: #a16207;
      margin-bottom: 0.2rem;
    }

    h2 {
      margin: 0;
      font-size: 1.15rem;
      color: #0f172a;
      line-height: 1.25;
    }

    .emotion {
      margin: 0.2rem 0 0;
      font-size: 0.78rem;
      color: #b45309;
      font-style: italic;
    }

    .sub {
      margin: 0.45rem 0 0;
      font-size: 0.8rem;
      color: #64748b;
      line-height: 1.4;
    }

    .close {
      border: none;
      background: transparent;
      cursor: pointer;
      color: #94a3b8;
    }

    .topics {
      list-style: none;
      margin: 0;
      padding: 0.65rem 1.1rem 0.65rem 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.45rem;
      background: repeating-linear-gradient(
        transparent,
        transparent 2.4rem,
        rgba(214, 211, 209, 0.35) 2.4rem,
        rgba(214, 211, 209, 0.35) calc(2.4rem + 1px)
      );
    }

    .topic-btn {
      width: 100%;
      text-align: left;
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.55rem;
      align-items: start;
      padding: 0.7rem 0.8rem;
      border: 1px solid #e7e5e4;
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.92);
      cursor: pointer;
      font: inherit;
      transition: border-color 0.2s, box-shadow 0.2s;
    }

    .topic-btn:not(:disabled):hover {
      border-color: #ca8a04;
      box-shadow: 0 4px 12px rgba(161, 98, 7, 0.12);
    }

    .topic-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .topic-text {
      font-size: 0.9rem;
      color: #1e293b;
      line-height: 1.35;
    }

    .topic-btn small {
      grid-column: 2;
      color: #94a3b8;
      font-size: 0.74rem;
    }

    footer {
      padding: 0.75rem 1.1rem 1rem 1.25rem;
      border-top: 1px solid #e7e5e4;
      background: #fafaf9;
    }
  `,
})
export class InterviewPanelComponent {
  npcLabel = input.required<string>();
  npcRole = input('');
  npcEmotion = input('');
  portraitUrl = input('');
  portraitColor = input('#6366f1');
  topics = input.required<InterviewTopicView[]>();

  topicSelected = output<string>();
  finished = output<void>();
}
