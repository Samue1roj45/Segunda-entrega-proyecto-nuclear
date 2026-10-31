import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

import { HYPOTHESIS_OPTIONS } from '../data/investigation/hypothesis.options';
import { ExpedienteCasoService } from '../services/expediente-caso.service';
import { CaseTimelineComponent } from './case-timeline.component';

type SectionId =
  | 'caso'
  | 'personas'
  | 'antecedentes'
  | 'evidencias'
  | 'riesgo'
  | 'pendiente'
  | 'observaciones'
  | 'contradicciones'
  | 'hechos'
  | 'hipotesis'
  | 'timeline';

@Component({
  selector: 'app-expediente-caso',
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCheckboxModule, CaseTimelineComponent],
  template: `
    @if (layout() === 'overlay' && !open()) {
      <!-- cerrado -->
    } @else {
      @if (layout() === 'overlay') {
        <div class="expediente-backdrop" (click)="closed.emit()"></div>
      }
      <aside
        class="expediente-panel"
        [class.embedded]="layout() === 'embedded'"
        role="complementary"
        aria-label="Registro profesional del caso">
        <header class="dossier-head">
          <div class="dossier-tab"></div>
          <div>
            <span class="dossier-kicker">Bitácora de investigación</span>
            <h2>Expediente clínico-institucional</h2>
            <p class="dossier-meta">{{ investigacionLabel() }} · {{ expediente.clueCount() }} hallazgos</p>
          </div>
          <button type="button" mat-icon-button (click)="closed.emit()" aria-label="Cerrar expediente">
            <mat-icon>close</mat-icon>
          </button>
        </header>

        @if (unlockPulse()) {
          <div class="unlock-banner" aria-live="polite">
            <mat-icon>edit_note</mat-icon>
            <div>
              <strong>Registro actualizado en bitácora</strong>
              @if (expediente.ultimoRegistro(); as r) {
                <p class="unlock-detail">
                  <span>{{ fuenteLabel(r.fuente) }}</span> · {{ r.ubicacion }} · {{ r.informante }}
                </p>
                <p class="unlock-excerpt">{{ r.dato }}</p>
              }
            </div>
          </div>
        }

        <nav class="section-nav">
          @for (s of visibleSections(); track s.id) {
            <button
              type="button"
              [class.active]="activeSection() === s.id"
              (click)="activeSection.set(s.id)">
              <mat-icon>{{ s.icon }}</mat-icon>
              {{ s.label }}
            </button>
          }
        </nav>

        <div class="section-body">
          @if (activeSection() === 'caso') {
            <p class="case-intro">
              Investigación psicosocial en curso. El registro se actualiza conforme entrevistas,
              observaciones y revisión de antecedentes.
            </p>
            @if (expediente.hypothesisHospitalLabel()) {
              <p><strong>Hipótesis hospital:</strong> {{ expediente.hypothesisHospitalLabel() }}</p>
            }
            @if (expediente.hypothesisComisariaLabel()) {
              <p><strong>Hipótesis institucional:</strong> {{ expediente.hypothesisComisariaLabel() }}</p>
            }
            <ul class="syn-mini">
              <li>{{ expediente.personas().filter(p => p.estado !== 'BLOQUEADO').length }} personas identificadas</li>
              <li>{{ expediente.evidencias().length }} antecedentes documentales</li>
              <li>{{ expediente.contradicciones().length }} tensiones entre fuentes</li>
            </ul>
            @if (layout() === 'embedded') {
              <app-case-timeline />
            }
          }

          @if (activeSection() === 'personas') {
            <ul class="personas-list">
              @for (p of expediente.personas(); track p.id) {
                <li [class.unknown]="p.estado === 'BLOQUEADO'">
                  <span class="avatar" [style.background]="p.estado === 'BLOQUEADO' ? '#94a3b8' : p.color">
                    @if (p.estado === 'BLOQUEADO') { ? } @else { {{ p.shortLabel.charAt(0) }} }
                  </span>
                  <div>
                    @if (p.estado === 'BLOQUEADO') {
                      <strong>Por entrevistar</strong>
                      <span class="rol">Aún no localizado en el escenario</span>
                    } @else {
                      <strong>{{ p.nombre }}</strong>
                      <span class="rol">{{ p.rol }}</span>
                      <span class="estado">{{ estadoLabel(p.estado) }}</span>
                    }
                  </div>
                </li>
              }
            </ul>
          }

          @if (activeSection() === 'antecedentes') {
            @if (expediente.evidencias().length === 0) {
              <p class="empty">Antecedentes pendientes: revisa historias, protocolos y registros en el escenario.</p>
            } @else {
              <ul class="evidencias-list">
                @for (e of expediente.evidencias(); track e.id) {
                  <li>
                    <mat-icon>folder</mat-icon>
                    <div>
                      <strong>{{ e.titulo }}</strong>
                      <p>{{ e.descripcion }}</p>
                    </div>
                  </li>
                }
              </ul>
            }
          }

          @if (activeSection() === 'evidencias') {
            @if (expediente.pistas().length === 0) {
              <p class="empty">Sin evidencias registradas aún.</p>
            } @else {
              <ul class="pistas-list">
                @for (p of expediente.pistas(); track p.id) {
                  <li>{{ p.text }}</li>
                }
              </ul>
            }
          }

          @if (activeSection() === 'riesgo') {
            <p class="hint">Factores derivados de tu investigación (sin calificación).</p>
            <ul class="pistas-list">
              @for (c of expediente.pistas(); track c.id) {
                @if (c.text.toLowerCase().includes('riesgo') || c.text.toLowerCase().includes('violencia')) {
                  <li>{{ c.text }}</li>
                }
              }
            </ul>
            @if (expediente.contradicciones().length) {
              @for (c of expediente.contradicciones(); track c.id) {
                <article class="contra-card">
                  <mat-icon>warning</mat-icon>
                  <div>
                    <strong>{{ c.title }}</strong>
                    <p>{{ c.narrative }}</p>
                  </div>
                </article>
              }
            }
          }

          @if (activeSection() === 'pendiente') {
            <ul class="pendiente-list">
              @for (p of pendientes(); track p) {
                <li><mat-icon>radio_button_unchecked</mat-icon> {{ p }}</li>
              }
              @if (pendientes().length === 0) {
                <li class="done"><mat-icon>check_circle</mat-icon> Sin pendientes críticos en esta fase</li>
              }
            </ul>
          }

          @if (activeSection() === 'observaciones') {
            @if (expediente.registroProfesional().length === 0) {
              <p class="empty">Tus observaciones profesionales aparecerán al registrar entrevistas y hallazgos.</p>
            } @else {
              <ul class="registro-list">
                @for (r of expediente.registroProfesional(); track r.id) {
                  <li>
                    <span class="reg-meta">{{ fuenteLabel(r.fuente) }} · {{ r.ubicacion }}</span>
                    <strong>{{ r.informante }}</strong>
                    <p>{{ r.dato }}</p>
                  </li>
                }
              </ul>
            }
          }

          @if (activeSection() === 'contradicciones') {
            @if (expediente.contradicciones().length === 0) {
              <p class="empty">Cuando dos fuentes no coincidan, aparecerán aquí.</p>
            } @else {
              @for (c of expediente.contradicciones(); track c.id) {
                <article class="contra-card">
                  <mat-icon>warning</mat-icon>
                  <div>
                    <strong>{{ c.title }}</strong>
                    <p>{{ c.narrative }}</p>
                  </div>
                </article>
              }
            }
          }

          @if (activeSection() === 'hechos') {
            <p class="hint">Hechos que consideras sustentados (sin retroalimentación inmediata).</p>
            <ul class="hechos-list">
              @for (h of expediente.hechos(); track h.id) {
                <li [class.locked]="!h.unlocked">
                  <mat-checkbox
                    [disabled]="!h.unlocked"
                    [checked]="h.marked"
                    (change)="expediente.toggleHecho(h.id)">
                    {{ h.label }}
                  </mat-checkbox>
                </li>
              }
            </ul>
          }

          @if (activeSection() === 'hipotesis') {
            @if (!expediente.hypothesisHospitalLabel() && !expediente.hypothesisComisariaLabel()) {
              <p class="empty">Formularás hipótesis al cerrar cada fase de investigación.</p>
            } @else {
              @if (expediente.hypothesisHospitalLabel()) {
                <p><strong>Hospital:</strong> {{ expediente.hypothesisHospitalLabel() }}</p>
              }
              @if (expediente.hypothesisComisariaLabel()) {
                <p><strong>Comisaría:</strong> {{ expediente.hypothesisComisariaLabel() }}</p>
              }
            }
          }

          @if (activeSection() === 'timeline') {
            <app-case-timeline />
          }
        </div>
      </aside>
    }
  `,
  styles: `
    .expediente-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.35);
      z-index: 40;
    }
    .expediente-panel {
      display: flex;
      flex-direction: column;
      background-color: #fffef5;
      background-image:
        linear-gradient(90deg, transparent 39px, #e7e5e4 39px, #e7e5e4 40px, transparent 40px),
        repeating-linear-gradient(
          transparent,
          transparent 1.55rem,
          rgba(161, 98, 7, 0.12) 1.55rem,
          rgba(161, 98, 7, 0.12) calc(1.55rem + 1px)
        );
      border-left: 4px solid #a16207;
      font-family: Georgia, 'Times New Roman', serif;
      min-height: 0;
      position: relative;
    }
    .expediente-panel::before {
      content: '';
      position: absolute;
      left: 12px;
      top: 0;
      bottom: 0;
      width: 2px;
      background: repeating-linear-gradient(
        #d6d3d1 0,
        #d6d3d1 8px,
        transparent 8px,
        transparent 16px
      );
      opacity: 0.6;
      pointer-events: none;
    }
    .expediente-panel:not(.embedded) {
      position: fixed;
      top: 0;
      right: 0;
      width: min(420px, 92vw);
      height: 100vh;
      z-index: 50;
      box-shadow: -8px 0 32px rgba(0, 0, 0, 0.12);
      animation: slideIn 0.28s ease-out;
    }
    .expediente-panel.embedded {
      width: 100%;
      height: 100%;
      border-left: none;
      border-radius: 0 12px 12px 0;
      box-shadow:
        inset 4px 0 0 #a16207,
        inset 0 0 40px rgba(161, 98, 7, 0.06);
    }
    @keyframes slideIn {
      from { transform: translateX(100%); }
      to { transform: translateX(0); }
    }
    .unlock-banner {
      display: flex;
      align-items: flex-start;
      gap: 0.4rem;
      margin: 0 0.75rem 0 2rem;
      padding: 0.55rem 0.7rem;
      border-radius: 4px;
      background: #fffbeb;
      border: 1px solid #fde68a;
      border-left: 3px solid #a16207;
      color: #44403c;
      font-size: 0.78rem;
      animation: unlockPop 0.5s ease;
      font-family: Georgia, serif;
    }
    .unlock-banner mat-icon { color: #a16207; font-size: 18px; width: 18px; height: 18px; }
    .unlock-banner strong {
      display: block;
      font-size: 0.72rem;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: #92400e;
      font-family: system-ui, sans-serif;
    }
    .unlock-detail {
      margin: 0.2rem 0 0;
      font-size: 0.68rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: #78716c;
      font-family: system-ui, sans-serif;
    }
    .unlock-excerpt {
      margin: 0.25rem 0 0;
      font-size: 0.8rem;
      line-height: 1.4;
      color: #334155;
      font-style: italic;
    }
    .registro-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.55rem;
    }
    .registro-list li {
      padding: 0.55rem 0.65rem;
      border-radius: 8px;
      background: #fffbeb;
      border: 1px solid #fde68a;
    }
    .reg-meta {
      display: block;
      font-size: 0.65rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: #a16207;
      margin-bottom: 0.15rem;
    }
    .registro-list strong { display: block; font-size: 0.82rem; color: #0f172a; }
    .registro-list p { margin: 0.25rem 0 0; font-size: 0.82rem; line-height: 1.4; color: #334155; }
    @keyframes unlockPop {
      0% { opacity: 0; transform: translateY(-8px); }
      100% { opacity: 1; transform: translateY(0); }
    }
    .dossier-head {
      display: flex;
      gap: 0.75rem;
      padding: 1rem 1rem 0.75rem 2.5rem;
      border-bottom: 2px double #ca8a04;
      align-items: flex-start;
      flex-shrink: 0;
      background: rgba(255, 254, 245, 0.92);
      z-index: 1;
    }
    .dossier-tab {
      width: 12px;
      align-self: stretch;
      background: #ca8a04;
      border-radius: 4px 0 0 4px;
      flex-shrink: 0;
    }
    .dossier-kicker {
      font-size: 0.7rem;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #a16207;
      font-weight: 700;
    }
    .dossier-head h2 { margin: 0.15rem 0 0; font-size: 1.05rem; }
    .dossier-meta { margin: 0; font-size: 0.78rem; color: #64748b; }
    .section-nav {
      display: flex;
      flex-wrap: wrap;
      gap: 0.2rem;
      padding: 0.45rem 0.65rem;
      border-bottom: 1px solid #e2e8f0;
      flex-shrink: 0;
    }
    .section-nav button {
      display: inline-flex;
      align-items: center;
      gap: 0.15rem;
      padding: 0.3rem 0.45rem;
      border: none;
      border-radius: 8px;
      background: transparent;
      font-size: 0.68rem;
      cursor: pointer;
      color: #475569;
    }
    .section-nav button.active {
      background: #fef3c7;
      color: #92400e;
      font-weight: 600;
    }
    .section-nav mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .section-body {
      flex: 1;
      overflow-y: auto;
      padding: 0.85rem 0.85rem 0.85rem 2.25rem;
      z-index: 1;
    }
    .case-intro, .empty, .hint {
      font-size: 0.85rem;
      color: #64748b;
      line-height: 1.45;
    }
    .syn-mini { margin: 0.75rem 0 0; padding-left: 1.1rem; font-size: 0.82rem; line-height: 1.5; }
    .personas-list, .evidencias-list, .pistas-list, .hechos-list, .timeline-list, .pendiente-list, .obs-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.55rem;
    }
    .personas-list li {
      display: flex;
      gap: 0.65rem;
      padding: 0.5rem;
      border-radius: 8px;
      background: #f8fafc;
    }
    .avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-weight: 700;
      flex-shrink: 0;
    }
    .rol, .estado { display: block; font-size: 0.76rem; color: #64748b; }
    .estado { color: #0ea5e9; font-weight: 600; }
    .evidencias-list li, .pendiente-list li {
      display: flex;
      gap: 0.5rem;
      padding: 0.55rem;
      background: #f1f5f9;
      border-radius: 8px;
      font-size: 0.85rem;
    }
    .pendiente-list li.done { color: #059669; }
    .pistas-list li {
      padding: 0.5rem 0.6rem;
      border-left: 3px solid #0ea5e9;
      background: #f0f9ff;
      border-radius: 0 8px 8px 0;
      font-size: 0.86rem;
      line-height: 1.4;
    }
    .obs-list li {
      padding: 0.55rem;
      background: #fffbeb;
      border-radius: 8px;
      border: 1px solid #fde68a;
    }
    .obs-time { font-size: 0.68rem; font-weight: 700; color: #a16207; text-transform: uppercase; }
    .obs-list p { margin: 0.25rem 0 0; font-size: 0.84rem; }
    .contra-card {
      display: flex;
      gap: 0.5rem;
      padding: 0.65rem;
      background: #fff7ed;
      border: 1px solid #fdba74;
      border-radius: 10px;
      margin-bottom: 0.45rem;
    }
    .contra-card mat-icon { color: #ea580c; }
    .timeline-list li {
      display: flex;
      gap: 0.5rem;
      font-size: 0.88rem;
      color: #94a3b8;
    }
    .timeline-list li.discovered { color: #0f172a; font-weight: 500; }
    .slot { width: 1.25rem; font-weight: 700; }
  `,
})
export class ExpedienteCasoComponent {
  readonly expediente = inject(ExpedienteCasoService);

  readonly open = input(true);
  readonly layout = input<'embedded' | 'overlay'>('overlay');
  readonly investigacionLabel = input('Urgencias');
  readonly unlockPulse = input(false);
  readonly closed = output<void>();

  readonly activeSection = signal<SectionId>('caso');

  readonly embeddedSections = [
    { id: 'caso' as const, label: 'Caso', icon: 'assignment' },
    { id: 'personas' as const, label: 'Entrevistados', icon: 'people' },
    { id: 'antecedentes' as const, label: 'Antecedentes', icon: 'folder' },
    { id: 'evidencias' as const, label: 'Evidencias', icon: 'search' },
    { id: 'riesgo' as const, label: 'Riesgo', icon: 'shield' },
    { id: 'pendiente' as const, label: 'Pendiente', icon: 'pending' },
    { id: 'observaciones' as const, label: 'Observaciones', icon: 'edit_note' },
  ];

  readonly overlayExtraSections = [
    { id: 'contradicciones' as const, label: 'Contradicciones', icon: 'warning' },
    { id: 'hechos' as const, label: 'Hechos', icon: 'fact_check' },
    { id: 'hipotesis' as const, label: 'Hipótesis', icon: 'psychology' },
    { id: 'timeline' as const, label: 'Línea temporal', icon: 'timeline' },
  ];

  readonly visibleSections = computed(() =>
    this.layout() === 'embedded'
      ? this.embeddedSections
      : [...this.embeddedSections, ...this.overlayExtraSections],
  );

  readonly pendientes = computed(() => {
    const items: string[] = [];
    const locked = this.expediente.personas().filter((p) => p.estado === 'BLOQUEADO');
    if (locked.length) items.push(`Localizar o entrevistar: ${locked.length} persona(s)`);
    if (this.expediente.evidencias().length < 2) items.push('Revisar más antecedentes documentales');
    if (this.expediente.pistas().length < 5) items.push('Ampliar entrevistas y observaciones');
    return items;
  });

  readonly hypothesisOptions = HYPOTHESIS_OPTIONS;

  fuenteLabel(fuente: string): string {
    const map: Record<string, string> = {
      entrevista: 'Entrevista',
      documento: 'Documento',
      observacion: 'Observación',
      declaracion: 'Declaración',
    };
    return map[fuente] ?? fuente;
  }

  estadoLabel(estado: string): string {
    const map: Record<string, string> = {
      DESCUBIERTO: 'Identificado',
      ENTREVISTA_T1: 'Entrevistado · nivel 1',
      ENTREVISTA_T2: 'Entrevistado · nivel 2',
      ENTREVISTA_T3: 'Entrevistado · nivel 3',
    };
    return map[estado] ?? estado;
  }
}
