import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { DetallePreguntaResultado, Resultado } from '../core/models/practicas.model';
import { SimulacionService } from '../core/services/simulacion.service';
import {
  CaseClosureSnapshot,
  decisionesPorEscenario,
  fortalezasYAreas,
  loadClosureSnapshot,
} from './services/case-result-analysis';

@Component({
  selector: 'app-resultado',
  imports: [
    CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="page bg-gradient">
      @if (loading()) {
        <div class="splash">
          <div class="spinner"></div>
          <p>Calculando tu resultado…</p>
        </div>
      }

      @if (resultado(); as r) {
        <!-- Hero con nota -->
        <section class="hero elevated-card anim-bounce-in">
          <div class="hero-bg" [style.--pct]="porcentaje() + '%'"></div>

          <span class="emoji-tier">{{ tierEmoji() }}</span>
          <h1 class="display titulo-tier">{{ tierTitulo() }}</h1>
          <p class="sub-tier">{{ tierMensaje() }}</p>

          <div class="circulo" [style.--pct]="porcentaje() + '%'">
            <span class="valor">{{ r.nota_final }}</span>
            <span class="max">/ 100</span>
          </div>

          <p class="caso-info">
            <mat-icon>menu_book</mat-icon> {{ r.caso_nombre }} · {{ r.practica_nombre }}
          </p>
        </section>

        <!-- Stats -->
        <section class="stats">
          <div class="stat-card ok">
            <mat-icon>check_circle</mat-icon>
            <div>
              <strong>{{ r.correctas }}</strong>
              <span>correctas</span>
            </div>
          </div>
          <div class="stat-card err">
            <mat-icon>cancel</mat-icon>
            <div>
              <strong>{{ r.incorrectas }}</strong>
              <span>incorrectas</span>
            </div>
          </div>
          <div class="stat-card ne">
            <mat-icon>help_outline</mat-icon>
            <div>
              <strong>{{ r.no_respondidas }}</strong>
              <span>sin responder</span>
            </div>
          </div>
        </section>

        @if (r.feedback_docente) {
          <mat-card class="feedback elevated-card anim-fade-up">
            <div class="feedback-head">
              <mat-icon>chat_bubble</mat-icon>
              <h3>Feedback de tu docente</h3>
            </div>
            <p>{{ r.feedback_docente }}</p>
          </mat-card>
        }

        <h2 class="seccion-titulo"><mat-icon>folder_open</mat-icon> Bloque 1 — Resumen de investigación</h2>
        <mat-card class="elevated-card resumen-card">
          @if (snapshot(); as s) {
            <p><strong>Personas identificadas:</strong> {{ s.personas.length }}</p>
            <ul class="compact">@for (p of s.personas; track p.nombre) { <li>{{ p.nombre }} ({{ p.rol }})</li> }</ul>
            <p><strong>Evidencias:</strong> {{ s.evidencias.length }}</p>
            <ul class="compact">@for (e of s.evidencias; track e.titulo) { <li>{{ e.titulo }}</li> }</ul>
            <p><strong>Contradicciones detectadas:</strong> {{ s.contradicciones.length }}</p>
            @if (s.contradicciones.length) {
              <ul class="compact">@for (c of s.contradicciones; track c.title) { <li>{{ c.title }}</li> }</ul>
            }
            <p><strong>Hechos marcados por ti:</strong> {{ s.hechosMarcados.length }}</p>
            <ul class="compact">@for (h of s.hechosMarcados; track h.label) { <li>{{ h.label }}</li> }</ul>
            <p><strong>Hipótesis hospital:</strong> {{ s.hypothesisHospital || '—' }}</p>
            <p><strong>Hipótesis comisaría:</strong> {{ s.hypothesisComisaria || '—' }}</p>
            <p><strong>Línea temporal reconstruida:</strong></p>
            <ul class="compact">@for (t of s.timeline; track t.label) { <li>{{ t.label }}</li> }</ul>
          } @else {
            <p class="status">Resumen de expediente no disponible en esta sesión.</p>
          }
        </mat-card>

        <h2 class="seccion-titulo"><mat-icon>school</mat-icon> Bloque 2 — Retroalimentación académica</h2>

        @if (decisionesHospital().length) {
          <h3 class="sub-bloque">Hospital</h3>
          @for (d of decisionesHospital(); track d.pregunta_id) {
            <article class="pregunta-card elevated-card debrief">
              <header class="card-head"><h3>Hospital — {{ d.enunciado }}</h3></header>
              @if (d.respuesta_elegida; as resp) {
                <div class="tu-respuesta"><span class="etiqueta">Tu decisión:</span><span>{{ resp.texto }}</span></div>
                @if (resp.es_correcta) {
                  <div class="veredicto ok"><mat-icon>check_circle</mat-icon><strong>✔ Buena decisión</strong></div>
                  @if (resp.retroalimentacion) { <p class="explica">{{ resp.retroalimentacion }}</p> }
                  @if (resp.justificacion) { <p class="principio"><strong>Principios:</strong> {{ resp.justificacion }}</p> }
                } @else {
                  <div class="veredicto warn"><mat-icon>warning</mat-icon><strong>⚠ Oportunidad de mejora</strong></div>
                  @if (resp.retroalimentacion) { <p class="explica">{{ resp.retroalimentacion }}</p> }
                  @if (d.respuestas_correctas.length > 0) {
                    <div class="recomendada">
                      <strong>✔ Opción recomendada:</strong> {{ d.respuestas_correctas[0].texto }}
                      @if (d.respuestas_correctas[0].justificacion) { <p class="explica">{{ d.respuestas_correctas[0].justificacion }}</p> }
                      @if (d.respuestas_correctas[0].retroalimentacion) { <p class="explica">{{ d.respuestas_correctas[0].retroalimentacion }}</p> }
                    </div>
                  }
                }
              }
            </article>
          }
        }
        @if (decisionesComisaria().length) {
          <h3 class="sub-bloque">Comisaría</h3>
          @for (d of decisionesComisaria(); track d.pregunta_id) {
            <article class="pregunta-card elevated-card debrief">
              <header class="card-head"><h3>Comisaría — {{ d.enunciado }}</h3></header>
              @if (d.respuesta_elegida; as resp) {
                <div class="tu-respuesta"><span class="etiqueta">Tu decisión:</span><span>{{ resp.texto }}</span></div>
                @if (resp.es_correcta) {
                  <div class="veredicto ok"><mat-icon>check_circle</mat-icon><strong>✔ Buena decisión</strong></div>
                  @if (resp.retroalimentacion) { <p class="explica">{{ resp.retroalimentacion }}</p> }
                  @if (resp.justificacion) { <p class="principio"><strong>Principios:</strong> {{ resp.justificacion }}</p> }
                } @else {
                  <div class="veredicto warn"><mat-icon>warning</mat-icon><strong>⚠ Oportunidad de mejora</strong></div>
                  @if (resp.retroalimentacion) { <p class="explica">{{ resp.retroalimentacion }}</p> }
                  @if (d.respuestas_correctas.length > 0) {
                    <div class="recomendada">
                      <strong>✔ Opción recomendada:</strong> {{ d.respuestas_correctas[0].texto }}
                      @if (d.respuestas_correctas[0].justificacion) { <p class="explica">{{ d.respuestas_correctas[0].justificacion }}</p> }
                      @if (d.respuestas_correctas[0].retroalimentacion) { <p class="explica">{{ d.respuestas_correctas[0].retroalimentacion }}</p> }
                    </div>
                  }
                }
              }
            </article>
          }
        }

        <h2 class="seccion-titulo"><mat-icon>thumb_up</mat-icon> Bloque 3 — Fortalezas detectadas</h2>
        <mat-card class="elevated-card lista-card">
          <ul>@for (f of fortalezas(); track f) { <li>{{ f }}</li> }</ul>
        </mat-card>

        <h2 class="seccion-titulo"><mat-icon>trending_up</mat-icon> Bloque 4 — Áreas de mejora</h2>
        <mat-card class="elevated-card lista-card areas">
          <ul>@for (a of areas(); track a) { <li>{{ a }}</li> }</ul>
        </mat-card>

        <div class="actions">
          <button mat-flat-button color="primary" class="salir-btn" (click)="salir()">
            <mat-icon>logout</mat-icon> Cerrar sesión
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .page {
      min-height: 100vh;
      padding: 1.5rem;
      max-width: 820px;
      margin: 0 auto;
      display: flex; flex-direction: column; gap: 1.25rem;
    }

    .splash {
      flex: 1;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 1rem; padding: 4rem 1rem; text-align: center;
      p { margin: 0; color: var(--mat-sys-on-surface-variant); }
    }
    .spinner {
      width: 56px; height: 56px;
      border: 5px solid color-mix(in srgb, var(--mat-sys-primary) 15%, transparent);
      border-top-color: var(--mat-sys-primary);
      border-radius: 50%;
      animation: spin 0.9s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .hero {
      padding: 2.5rem 2rem;
      text-align: center;
      position: relative;
      overflow: hidden;
    }
    .hero-bg {
      position: absolute; inset: 0;
      background:
        radial-gradient(circle at 50% 0%, color-mix(in srgb, var(--mat-sys-primary) 25%, transparent), transparent 60%),
        radial-gradient(circle at 50% 100%, color-mix(in srgb, var(--mat-sys-tertiary) 20%, transparent), transparent 60%);
      z-index: 0;
    }
    .hero > * { position: relative; z-index: 1; }

    .emoji-tier { font-size: 3.5rem; display: block; margin-bottom: 0.25rem; }
    .titulo-tier {
      margin: 0;
      font-size: 1.8rem;
      background: linear-gradient(135deg, var(--mat-sys-primary), var(--mat-sys-tertiary));
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
    .sub-tier { margin: 0.25rem 0 1.25rem; color: var(--mat-sys-on-surface-variant); font-size: 1rem; }

    .circulo {
      width: 160px; height: 160px; border-radius: 50%;
      background: conic-gradient(var(--mat-sys-primary) var(--pct), var(--mat-sys-surface-container) 0);
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      position: relative;
      margin: 0 auto 1rem;
    }
    .circulo::before {
      content: ''; position: absolute; inset: 10px;
      border-radius: 50%; background: var(--mat-sys-surface);
    }
    .valor, .max { position: relative; z-index: 1; }
    .valor {
      font-family: 'Plus Jakarta Sans', sans-serif;
      font-size: 2.6rem; font-weight: 700; color: var(--mat-sys-primary);
      line-height: 1;
    }
    .max { font-size: 0.9rem; color: var(--mat-sys-on-surface-variant); margin-top: 4px; }

    .caso-info {
      margin: 0;
      display: inline-flex; align-items: center; gap: 0.35rem;
      padding: 0.45rem 1rem;
      background: var(--mat-sys-surface-container);
      border-radius: 999px;
      font-size: 0.9rem;
      mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--mat-sys-primary); }
    }

    .stats {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      @media (max-width: 480px) { grid-template-columns: 1fr; }
    }
    .stat-card {
      padding: 1.25rem;
      background: var(--mat-sys-surface);
      border-radius: 16px;
      display: flex; align-items: center; gap: 0.75rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);
      border-left: 4px solid var(--mat-sys-outline-variant);

      mat-icon { font-size: 32px; width: 32px; height: 32px; }
      strong { display: block; font-size: 1.8rem; font-weight: 700; line-height: 1; font-family: 'Plus Jakarta Sans', sans-serif; }
      span { font-size: 0.85rem; color: var(--mat-sys-on-surface-variant); }

      &.ok  { border-left-color: var(--mat-sys-primary);   mat-icon { color: var(--mat-sys-primary); } }
      &.err { border-left-color: var(--mat-sys-error);     mat-icon { color: var(--mat-sys-error); } }
      &.ne  { border-left-color: var(--mat-sys-outline);   mat-icon { color: var(--mat-sys-on-surface-variant); } }
    }

    .feedback {
      padding: 1.5rem;
      .feedback-head {
        display: flex; align-items: center; gap: 0.5rem;
        margin-bottom: 0.5rem;
        mat-icon { color: var(--mat-sys-tertiary); }
        h3 { margin: 0; font-size: 1.1rem; font-weight: 600; }
      }
      p { margin: 0; line-height: 1.6; }
    }

    .seccion-titulo {
      display: flex; align-items: center; gap: 0.5rem;
      font-size: 1.25rem; font-weight: 600;
      font-family: 'Plus Jakarta Sans', sans-serif;
      margin: 0.5rem 0 0;
      mat-icon { color: var(--mat-sys-primary); }
    }

    .pregunta-card {
      padding: 1.5rem;
      border-left: 6px solid var(--mat-sys-outline-variant);

      &.correcta { border-left-color: var(--mat-sys-primary); }
      &.incorrecta { border-left-color: var(--mat-sys-error); }
      &.no-respondida { border-left-color: var(--mat-sys-on-surface-variant); opacity: 0.95; }
    }

    .card-head {
      display: flex; align-items: flex-start; gap: 0.75rem;
      margin-bottom: 0.75rem;

      .num {
        flex-shrink: 0;
        width: 32px; height: 32px;
        border-radius: 10px;
        background: var(--mat-sys-surface-container);
        color: var(--mat-sys-on-surface);
        display: inline-flex; align-items: center; justify-content: center;
        font-weight: 700;
      }
      h3 { margin: 0; font-size: 1rem; font-weight: 600; line-height: 1.4; flex: 1; }

      .ok-icon  { color: var(--mat-sys-primary); }
      .err-icon { color: var(--mat-sys-error); }
      .ne-icon  { color: var(--mat-sys-on-surface-variant); }
    }

    .status { color: var(--mat-sys-on-surface-variant); margin: 0; font-style: italic; }

    .tu-respuesta {
      padding: 0.85rem 1rem;
      background: var(--mat-sys-surface-container-low);
      border-radius: 10px;
      margin-bottom: 0.5rem;
      .etiqueta { display: block; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--mat-sys-on-surface-variant); margin-bottom: 0.25rem; }
    }

    .retro {
      display: flex; gap: 0.5rem;
      padding: 0.75rem 1rem;
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
      border-radius: 10px;
      margin-bottom: 0.5rem;
      mat-icon { flex-shrink: 0; margin-top: 2px; }
      p { margin: 0; line-height: 1.5; }
    }

    .resumen-card p { margin: 0.35rem 0; line-height: 1.45; }
    .compact { margin: 0.25rem 0 0.75rem; padding-left: 1.25rem; font-size: 0.9rem; }
    .sub-bloque { margin: 1rem 0 0.5rem; font-size: 1.05rem; font-weight: 600; }
    .lista-card ul { margin: 0; padding-left: 1.25rem; line-height: 1.55; }
    .lista-card.areas { border-left: 4px solid var(--mat-sys-tertiary); }
    .debrief { border-left: 6px solid var(--mat-sys-outline-variant); }
    .veredicto {
      display: flex; align-items: center; gap: 0.4rem;
      margin: 0.5rem 0;
      &.ok { color: var(--mat-sys-primary); }
      &.warn { color: #b45309; }
    }
    .explica, .principio { margin: 0.35rem 0; line-height: 1.5; font-size: 0.92rem; }
    .recomendada {
      margin-top: 0.75rem;
      padding: 0.75rem 1rem;
      background: color-mix(in srgb, var(--mat-sys-primary) 8%, transparent);
      border-radius: 10px;
    }

    .correctas {
      margin-top: 0.5rem;
      padding: 0.75rem 1rem;
      background: color-mix(in srgb, var(--mat-sys-primary) 8%, transparent);
      border-radius: 10px;

      strong {
        display: flex; align-items: center; gap: 0.35rem;
        color: var(--mat-sys-primary);
        margin-bottom: 0.25rem;
        mat-icon { font-size: 18px; width: 18px; height: 18px; }
      }
      ul { margin: 0; padding-left: 1.5rem; }
      li { line-height: 1.5; }
      .just { color: var(--mat-sys-on-surface-variant); font-size: 0.85rem; }
    }

    .actions {
      display: flex; justify-content: center;
      margin: 1.5rem 0 0;
    }
    .salir-btn {
      height: 48px; padding: 0 1.5rem;
      border-radius: 14px; font-weight: 600;
      display: inline-flex; align-items: center; gap: 0.4rem;
    }
  `],
})
export class ResultadoEstudiante implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly servicio = inject(SimulacionService);

  readonly loading = signal(true);
  readonly resultado = signal<Resultado | null>(null);
  readonly snapshot = signal<CaseClosureSnapshot | null>(null);

  readonly decisionesHospital = computed(() => {
    const r = this.resultado();
    if (!r) return [] as DetallePreguntaResultado[];
    return decisionesPorEscenario(r.detalle_preguntas).hospital;
  });

  readonly decisionesComisaria = computed(() => {
    const r = this.resultado();
    if (!r) return [] as DetallePreguntaResultado[];
    return decisionesPorEscenario(r.detalle_preguntas).comisaria;
  });

  readonly decisionesOtros = computed(() => {
    const r = this.resultado();
    if (!r) return [] as DetallePreguntaResultado[];
    return decisionesPorEscenario(r.detalle_preguntas).otros;
  });

  readonly fortalezas = computed(() => {
    const r = this.resultado();
    if (!r) return [];
    return fortalezasYAreas(this.snapshot(), r).fortalezas;
  });

  readonly areas = computed(() => {
    const r = this.resultado();
    if (!r) return [];
    return fortalezasYAreas(this.snapshot(), r).areas;
  });
  readonly porcentaje = computed(() => {
    const r = this.resultado();
    if (!r) return 0;
    return Math.round(Number(r.nota_final));
  });

  readonly tierEmoji = computed(() => {
    const p = this.porcentaje();
    if (p >= 90) return '🏆';
    if (p >= 70) return '🎉';
    if (p >= 50) return '👍';
    if (p > 0) return '🌱';
    return '💪';
  });

  readonly tierTitulo = computed(() => {
    const p = this.porcentaje();
    if (p >= 90) return '¡Excelente!';
    if (p >= 70) return '¡Muy bien!';
    if (p >= 50) return 'Buen intento';
    if (p > 0) return 'Hay camino por recorrer';
    return 'Práctica registrada';
  });

  readonly tierMensaje = computed(() => {
    const p = this.porcentaje();
    if (p >= 90) return 'Dominio sobresaliente del caso.';
    if (p >= 70) return 'Comprendiste muy bien la situación.';
    if (p >= 50) return 'Vas por buen camino, sigue practicando.';
    if (p > 0) return 'Revisa la retroalimentación para mejorar.';
    return 'Lee con detalle la retroalimentación abajo.';
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) { this.router.navigate(['/estudiante']); return; }
    this.servicio.obtenerResultado(id).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.snapshot.set(loadClosureSnapshot(r.participacion));
        this.loading.set(false);
      },
      error: () => { this.router.navigate(['/estudiante']); },
    });
  }

  salir() {
    const r = this.resultado();
    if (r) {
      sessionStorage.removeItem(`simulacion-closure:${r.participacion}`);
    }
    localStorage.clear();
    this.router.navigate(['/acceso-estudiante']);
  }
}
