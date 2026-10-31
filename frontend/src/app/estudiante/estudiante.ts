import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { AuthService } from '../core/auth/auth.service';
import {
  AccesoEstudianteRespuesta,
  EstadoPractica,
  Resultado,
} from '../core/models/practicas.model';
import { SimulacionService } from '../core/services/simulacion.service';

type PracticaActiva = AccesoEstudianteRespuesta['practica'];

interface PracticaDashboard {
  id: number;
  nombre: string;
  estado: 'Pendiente' | 'En progreso' | 'Completada';
  progreso: number;
  fechaLimite: string | null;
}

interface CompetenciaDashboard {
  nombre: string;
  avance: number;
  evolucion: string;
}

interface InsigniaDashboard {
  nombre: string;
  descripcion: string;
  fecha: string;
  icono: string;
}

@Component({
  selector: 'app-estudiante',
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  template: `
    <section class="student-dashboard">
      <header class="hero-card" id="perfil">
        <div class="hero-copy">
          <span class="eyebrow">Centro de entrenamiento</span>
          <h1>Hola, {{ nombreEstudiante() }}</h1>
          <p class="tagline">
            Plataforma de simulación profesional en psicología social.
          </p>

          @if (practicaPendiente(); as practica) {
            <div class="mission-block">
              <span class="mission-label">Simulación activa</span>
              <strong>{{ practica.nombre }}</strong>
              <mat-progress-bar mode="determinate" [value]="practica.progreso" />
              <span class="mission-meta">{{ practica.progreso }}% · {{ practica.estado }}</span>
            </div>
          }

          <a mat-flat-button color="primary" class="primary-action" routerLink="/estudiante/simulacion">
            <mat-icon>sports_esports</mat-icon>
            <span>Iniciar simulación</span>
          </a>
        </div>

        @if (practicaPendiente(); as practica) {
          <aside class="pending-panel">
            <span class="panel-kicker">Práctica pendiente</span>
            <h2>{{ practica.nombre }}</h2>
            <div class="pending-meta">
              <span>{{ practica.estado }}</span>
              @if (practica.fechaLimite) {
                <span>Vence {{ practica.fechaLimite | date:'mediumDate' }}</span>
              }
            </div>
            <mat-progress-bar mode="determinate" [value]="practica.progreso" />
          </aside>
        } @else {
          <aside class="pending-panel calm">
            <span class="panel-kicker">Estado académico</span>
            <h2>Sin práctica pendiente</h2>
            <p>Cuando tengas una simulación asignada aparecerá aquí.</p>
          </aside>
        }
      </header>

      <section class="metrics-grid" aria-label="Métricas académicas">
        @for (metric of metricas(); track metric.label) {
          <article class="metric-card">
            <div class="metric-icon">
              <mat-icon>{{ metric.icon }}</mat-icon>
            </div>
            <div>
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
            </div>
          </article>
        }
      </section>

      <section class="content-grid">
        <article class="panel wide" id="practicas">
          <div class="section-head">
            <div>
              <span class="section-kicker">Mis prácticas</span>
              <h2>Simulaciones asignadas</h2>
            </div>
            <a mat-stroked-button routerLink="/participaciones">
              <mat-icon>history</mat-icon>
              Participaciones
            </a>
          </div>

          @if (practicas().length === 0) {
            <p class="empty">Aún no hay prácticas asociadas a tu sesión.</p>
          } @else {
            <div class="practice-list">
              @for (practica of practicas(); track practica.id) {
                <div class="practice-row">
                  <div class="practice-main">
                    <h3>{{ practica.nombre }}</h3>
                    <span class="status" [class.done]="practica.estado === 'Completada'">
                      {{ practica.estado }}
                    </span>
                  </div>
                  <div class="practice-progress">
                    <mat-progress-bar mode="determinate" [value]="practica.progreso" />
                    <span>{{ practica.progreso }}%</span>
                  </div>
                  <div class="practice-date">
                    @if (practica.fechaLimite) {
                      {{ practica.fechaLimite | date:'mediumDate' }}
                    } @else {
                      Sin fecha límite
                    }
                  </div>
                  <a
                    mat-flat-button
                    color="primary"
                    routerLink="/estudiante/simulacion"
                    [class.hidden-action]="practica.estado === 'Completada'">
                    Continuar
                  </a>
                </div>
              }
            </div>
          }
        </article>

        <article class="panel" id="resultados">
          <div class="section-head compact">
            <div>
              <span class="section-kicker">Resultados recientes</span>
              <h2>Retroalimentación</h2>
            </div>
          </div>

          @if (resultadosRecientes().length === 0) {
            <p class="empty">Completa una práctica para ver tus resultados.</p>
          } @else {
            <div class="result-list">
              @for (resultado of resultadosRecientes(); track resultado.id) {
                <a class="result-item" [routerLink]="['/estudiante/resultado', resultado.id]">
                  <div class="score">{{ resultado.nota_final }}</div>
                  <div>
                    <h3>{{ resultado.practica_nombre }}</h3>
                    <p>{{ resumenFeedback(resultado) }}</p>
                    <span>{{ resultado.fecha_calculo | date:'mediumDate' }}</span>
                  </div>
                </a>
              }
            </div>
          }
        </article>

        <article class="panel" id="progreso">
          <div class="section-head compact">
            <div>
              <span class="section-kicker">Progreso académico</span>
              <h2>Competencias</h2>
            </div>
          </div>

          <div class="competency-list">
            @for (competencia of competencias(); track competencia.nombre) {
              <div class="competency">
                <div class="competency-head">
                  <span>{{ competencia.nombre }}</span>
                  <strong>{{ competencia.avance }}%</strong>
                </div>
                <mat-progress-bar mode="determinate" [value]="competencia.avance" />
                <small>{{ competencia.evolucion }}</small>
              </div>
            }
          </div>
        </article>

        <article class="panel" id="insignias">
          <div class="section-head compact">
            <div>
              <span class="section-kicker">Insignias</span>
              <h2>Logros obtenidos</h2>
            </div>
          </div>

          <div class="badge-list">
            @for (insignia of insignias(); track insignia.nombre) {
              <div class="badge-item">
                <mat-icon>{{ insignia.icono }}</mat-icon>
                <div>
                  <h3>{{ insignia.nombre }}</h3>
                  <p>{{ insignia.descripcion }}</p>
                  <span>{{ insignia.fecha | date:'mediumDate' }}</span>
                </div>
              </div>
            }
          </div>
        </article>

        <article class="panel" id="historial">
          <div class="section-head compact">
            <div>
              <span class="section-kicker">Historial</span>
              <h2>Accesos rápidos</h2>
            </div>
          </div>

          <div class="quick-links">
            <a routerLink="/participaciones">
              <mat-icon>timeline</mat-icon>
              <span>Historial completo</span>
            </a>
            <a routerLink="/resultados">
              <mat-icon>bar_chart</mat-icon>
              <span>Resultados</span>
            </a>
            <a href="#resultados">
              <mat-icon>forum</mat-icon>
              <span>Retroalimentaciones</span>
            </a>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [`
    .student-dashboard {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      max-width: 1280px;
      margin: 0 auto;
    }

    .hero-card {
      display: grid;
      grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.8fr);
      gap: 1.5rem;
      padding: 2rem;
      border-radius: 18px;
      color: #f3f7fb;
      background:
        linear-gradient(135deg, rgba(34, 211, 238, 0.22), rgba(17, 24, 39, 0.94) 42%, rgba(251, 191, 36, 0.12)),
        #111827;
      box-shadow: 0 22px 50px rgba(8, 15, 26, 0.22);
      overflow: hidden;
    }

    .hero-copy {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 0.85rem;
    }

    .eyebrow,
    .section-kicker,
    .panel-kicker {
      font-size: 0.74rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }

    .eyebrow {
      color: #67e8f9;
    }

    h1,
    h2,
    h3,
    p {
      margin: 0;
    }

    h1 {
      font-family: 'Plus Jakarta Sans', sans-serif;
      font-size: 2.35rem;
      line-height: 1.05;
      font-weight: 700;
    }

    .hero-copy p,
    .tagline {
      color: rgba(243, 247, 251, 0.78);
      font-size: 1rem;
    }

    .mission-block {
      width: min(520px, 100%);
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
      padding: 0.85rem 1rem;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.12);
    }

    .mission-label {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #67e8f9;
    }

    .mission-meta {
      font-size: 0.82rem;
      opacity: 0.85;
    }

    .hero-card mat-progress-bar {
      --mdc-linear-progress-active-indicator-color: #22d3ee;
      --mdc-linear-progress-track-color: rgba(255, 255, 255, 0.18);
      border-radius: 999px;
      overflow: hidden;
    }

    .primary-action {
      margin-top: 0.45rem;
      min-height: 48px;
      border-radius: 8px;
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
    }

    .pending-panel {
      align-self: stretch;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 0.8rem;
      padding: 1.25rem;
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 12px;
      background: rgba(14, 20, 27, 0.72);
      backdrop-filter: blur(10px);

      h2 {
        font-size: 1.2rem;
        font-weight: 700;
      }

      p {
        color: rgba(243, 247, 251, 0.7);
        line-height: 1.45;
      }
    }

    .pending-panel.calm {
      background: rgba(255, 255, 255, 0.06);
    }

    .pending-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;

      span {
        padding: 0.3rem 0.55rem;
        border-radius: 6px;
        background: rgba(255, 255, 255, 0.1);
        color: rgba(243, 247, 251, 0.85);
        font-size: 0.8rem;
      }
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 0.9rem;
    }

    .metric-card,
    .panel {
      background: var(--mat-sys-surface);
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
    }

    .metric-card {
      display: flex;
      align-items: center;
      gap: 0.9rem;
      padding: 1rem;

      strong {
        display: block;
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 1.55rem;
        line-height: 1;
      }

      span {
        color: var(--mat-sys-on-surface-variant);
        font-size: 0.82rem;
      }
    }

    .metric-icon {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: var(--mat-sys-primary);
      background: color-mix(in srgb, var(--mat-sys-primary) 12%, transparent);
      flex: 0 0 auto;
    }

    .content-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
      gap: 1rem;
      align-items: start;
    }

    .panel {
      padding: 1.15rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .panel.wide {
      grid-row: span 2;
    }

    .section-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;

      h2 {
        margin-top: 0.15rem;
        font-family: 'Plus Jakarta Sans', sans-serif;
        font-size: 1.12rem;
        font-weight: 700;
      }
    }

    .section-head.compact {
      align-items: flex-start;
    }

    .section-kicker {
      color: var(--mat-sys-primary);
    }

    .empty {
      padding: 1rem;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-low);
      color: var(--mat-sys-on-surface-variant);
      line-height: 1.45;
    }

    .practice-list,
    .result-list,
    .competency-list,
    .badge-list,
    .quick-links {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .practice-row {
      display: grid;
      grid-template-columns: minmax(180px, 1.2fr) minmax(130px, 0.75fr) minmax(110px, 0.65fr) auto;
      gap: 0.85rem;
      align-items: center;
      padding: 0.85rem;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-low);
    }

    .practice-main {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      min-width: 0;

      h3 {
        font-size: 0.98rem;
        font-weight: 700;
      }
    }

    .status {
      width: fit-content;
      padding: 0.22rem 0.5rem;
      border-radius: 6px;
      color: #075985;
      background: rgba(34, 211, 238, 0.18);
      font-size: 0.76rem;
      font-weight: 700;
    }

    .status.done {
      color: #047857;
      background: rgba(52, 211, 153, 0.18);
    }

    .practice-progress {
      display: grid;
      grid-template-columns: 1fr auto;
      align-items: center;
      gap: 0.55rem;
      font-size: 0.82rem;
      color: var(--mat-sys-on-surface-variant);
    }

    .practice-date {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
    }

    .hidden-action {
      visibility: hidden;
    }

    .result-item {
      display: grid;
      grid-template-columns: 58px 1fr;
      gap: 0.8rem;
      padding: 0.85rem;
      border-radius: 8px;
      color: inherit;
      text-decoration: none;
      background: var(--mat-sys-surface-container-low);

      &:hover {
        background: var(--mat-sys-surface-container);
      }

      h3 {
        font-size: 0.95rem;
        font-weight: 700;
      }

      p {
        margin-top: 0.2rem;
        color: var(--mat-sys-on-surface-variant);
        line-height: 1.4;
      }

      span {
        display: inline-block;
        margin-top: 0.35rem;
        font-size: 0.78rem;
        color: var(--mat-sys-on-surface-variant);
      }
    }

    .score {
      height: 58px;
      border-radius: 8px;
      display: grid;
      place-items: center;
      color: #064e3b;
      background: rgba(52, 211, 153, 0.18);
      font-family: 'Plus Jakarta Sans', sans-serif;
      font-weight: 800;
    }

    .competency {
      display: flex;
      flex-direction: column;
      gap: 0.45rem;
    }

    .competency-head {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      font-size: 0.9rem;
      font-weight: 700;
    }

    .competency small {
      color: var(--mat-sys-on-surface-variant);
    }

    .badge-item {
      display: grid;
      grid-template-columns: 42px 1fr;
      gap: 0.75rem;
      align-items: start;
      padding: 0.8rem;
      border-radius: 8px;
      background: var(--mat-sys-surface-container-low);

      mat-icon {
        color: #b45309;
        background: rgba(251, 191, 36, 0.18);
        border-radius: 8px;
        padding: 0.55rem;
        width: 24px;
        height: 24px;
      }

      h3 {
        font-size: 0.94rem;
        font-weight: 700;
      }

      p,
      span {
        color: var(--mat-sys-on-surface-variant);
        font-size: 0.84rem;
        line-height: 1.4;
      }
    }

    .quick-links a {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      min-height: 46px;
      padding: 0 0.85rem;
      border-radius: 8px;
      color: inherit;
      text-decoration: none;
      background: var(--mat-sys-surface-container-low);

      mat-icon {
        color: var(--mat-sys-primary);
      }

      &:hover {
        background: var(--mat-sys-surface-container);
      }
    }

    @media (max-width: 1020px) {
      .hero-card,
      .content-grid {
        grid-template-columns: 1fr;
      }

      .metrics-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 720px) {
      .hero-card {
        padding: 1.25rem;
      }

      h1 {
        font-size: 1.75rem;
      }

      .metrics-grid {
        grid-template-columns: 1fr;
      }

      .section-head,
      .practice-row {
        grid-template-columns: 1fr;
      }

      .section-head {
        align-items: stretch;
      }

      .hidden-action {
        display: none;
      }
    }
  `],
})
export class Estudiante implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly simulacion = inject(SimulacionService);

  readonly usuario = this.auth.usuario;
  readonly loadingResultados = signal(false);
  readonly resultados = signal<Resultado[]>([]);
  readonly practicaActiva = signal<PracticaActiva | null>(this.loadPracticaActiva());

  readonly nombreEstudiante = computed(() => {
    const usuario = this.usuario();
    return usuario?.first_name || usuario?.nombre_completo || usuario?.username || 'estudiante';
  });

  readonly promedio = computed(() => {
    const resultados = this.resultados();
    if (resultados.length === 0) return 0;
    const total = resultados.reduce((sum, r) => sum + Number(r.nota_final || 0), 0);
    return Math.round((total / resultados.length) * 10) / 10;
  });

  readonly practicas = computed<PracticaDashboard[]>(() => {
    const porId = new Map<number, PracticaDashboard>();
    const activa = this.practicaActiva();

    if (activa) {
      porId.set(activa.id, {
        id: activa.id,
        nombre: activa.nombre,
        estado: this.estadoPractica(activa.estado),
        progreso: this.progresoPractica(activa.id, activa.estado),
        fechaLimite: activa.fecha_fin,
      });
    }

    for (const resultado of this.resultados()) {
      if (!porId.has(resultado.practica_id)) {
        porId.set(resultado.practica_id, {
          id: resultado.practica_id,
          nombre: resultado.practica_nombre,
          estado: 'Completada',
          progreso: 100,
          fechaLimite: resultado.fecha_calculo,
        });
      }
    }

    return Array.from(porId.values());
  });

  readonly practicaPendiente = computed(() =>
    this.practicas().find((practica) => practica.estado !== 'Completada') ?? null,
  );

  readonly resultadosRecientes = computed(() =>
    [...this.resultados()]
      .sort((a, b) => new Date(b.fecha_calculo).getTime() - new Date(a.fecha_calculo).getTime())
      .slice(0, 3),
  );

  readonly metricas = computed(() => [
    { label: 'Prácticas asignadas', value: String(this.practicas().length), icon: 'assignment' },
    { label: 'Prácticas completadas', value: String(this.resultados().length), icon: 'workspace_premium' },
    { label: 'Promedio académico', value: this.promedio() ? this.promedio().toFixed(1) : '0.0', icon: 'grade' },
    { label: 'Racha de actividad', value: `${this.rachaActividad()} días`, icon: 'local_fire_department' },
  ]);

  readonly competencias = computed<CompetenciaDashboard[]>(() => {
    const promedio = this.promedio();
    const base = promedio || (this.practicaActiva() ? 35 : 0);
    return [
      {
        nombre: 'Análisis situacional',
        avance: this.limitar(base + 8),
        evolucion: this.textoEvolucion(base),
      },
      {
        nombre: 'Toma de decisiones',
        avance: this.limitar(base + (this.resultados().length > 1 ? 6 : 0)),
        evolucion: this.textoEvolucion(base - 4),
      },
      {
        nombre: 'Argumentación psicosocial',
        avance: this.limitar(base - 2),
        evolucion: this.textoEvolucion(base - 8),
      },
    ];
  });

  readonly insignias = computed<InsigniaDashboard[]>(() => {
    const fecha = this.resultados()[0]?.fecha_calculo || new Date().toISOString();
    const insignias: InsigniaDashboard[] = [
      {
        nombre: 'Inicio académico',
        descripcion: 'Acceso activo a simulaciones psicosociales.',
        fecha,
        icono: 'school',
      },
    ];

    if (this.resultados().length > 0) {
      insignias.push({
        nombre: 'Primera práctica completada',
        descripcion: 'Finalizaste una simulación y revisaste tu resultado.',
        fecha,
        icono: 'military_tech',
      });
    }

    if (this.promedio() >= 85) {
      insignias.push({
        nombre: 'Desempeño destacado',
        descripcion: 'Alcanzaste un promedio académico superior.',
        fecha,
        icono: 'emoji_events',
      });
    }

    return insignias;
  });

  ngOnInit(): void {
    this.cargarResultados();
  }

  resumenFeedback(resultado: Resultado): string {
    const texto = resultado.feedback_docente?.trim() || 'Revisa el detalle para ver la retroalimentación de la práctica.';
    return texto.length > 110 ? `${texto.slice(0, 110)}...` : texto;
  }

  private cargarResultados(): void {
    this.loadingResultados.set(true);
    this.simulacion.listarResultados().subscribe({
      next: (pagina) => {
        this.resultados.set(pagina.results);
        this.loadingResultados.set(false);
      },
      error: () => {
        this.resultados.set([]);
        this.loadingResultados.set(false);
      },
    });
  }

  private loadPracticaActiva(): PracticaActiva | null {
    const raw = localStorage.getItem('simulador.practica_activa');
    if (!raw) return null;
    try {
      return JSON.parse(raw) as PracticaActiva;
    } catch {
      return null;
    }
  }

  private estadoPractica(estado: EstadoPractica): PracticaDashboard['estado'] {
    if (estado === EstadoPractica.Finalizada) return 'Completada';
    if (estado === EstadoPractica.EnCurso) return 'En progreso';
    return 'Pendiente';
  }

  private progresoPractica(id: number, estado: EstadoPractica): number {
    if (this.resultados().some((resultado) => resultado.practica_id === id)) return 100;
    if (estado === EstadoPractica.EnCurso) return 45;
    return 15;
  }

  private rachaActividad(): number {
    if (this.resultados().length > 0) return Math.min(7, this.resultados().length + 1);
    return this.practicaActiva() ? 1 : 0;
  }

  private textoEvolucion(valor: number): string {
    if (valor >= 80) return 'Evolución alta y sostenida';
    if (valor >= 50) return 'Evolución estable';
    if (valor > 0) return 'Evolución inicial';
    return 'Sin medición suficiente';
  }

  private limitar(valor: number): number {
    return Math.max(0, Math.min(100, Math.round(valor)));
  }
}
