import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { ResultadoDetalle, Retroalimentacion } from '../../core/modelos/dominio';
import { ToastService } from '../../core/toast.service';
import { SomaApiService } from '../../core/soma-api.service';

/** Resultados propios del estudiante (módulo 5): nota de cada intento, detalle por pregunta y retroalimentación del docente. */
@Component({
  selector: 'app-mis-resultados',
  imports: [DatePipe, DecimalPipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Mis resultados</h2></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (celebrar()) { <div class="confeti" aria-hidden="true">@for (c of confeti; track $index) { <i [style.left.%]="c.x" [style.animation-delay]="c.d + 's'" [style.background]="c.c" [style.--g]="c.g + 'deg'"></i> }</div> }

      @if (destacado(); as d) {
        <section class="cristal destacado">
          <div class="medalla" aria-hidden="true">{{ proporcion(d) >= .7 ? '🎉' : '📘' }}</div>
          <div>
            <p class="sobre-d">{{ proporcion(d) >= .7 ? '¡Muy buen trabajo!' : 'Tu resultado más reciente' }}</p>
            <h3>{{ d.practicaNombre }}</h3>
            <p class="nota-grande">{{ d.notaFinal | number: '1.1-2' }} <small>/ {{ d.escalaMaxima }}</small></p>
            @for (t of retroDestacada(); track t.id) { <blockquote><strong>Mensaje de tu docente</strong>{{ t.texto }}</blockquote> }
          </div>
        </section>
      }

      @if (historiales().length) {
        <section class="cristal bloque">
          <h3>Evolución por intentos</h3>
          @for (h of historiales(); track h.nombre) {
            <div class="historial">
              <span class="et">{{ h.nombre }}</span>
              <svg viewBox="0 0 220 56" role="img" [attr.aria-label]="'Evolución de la nota en ' + h.nombre">
                <polyline [attr.points]="h.linea" fill="none" stroke="var(--lila-700)" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
                @for (p of h.puntos; track p.i) { <circle [attr.cx]="p.x" [attr.cy]="p.y" r="4.5" fill="#fff" stroke="var(--lila-700)" stroke-width="2.4" /> }
              </svg>
              <span class="cambio" [class.sube]="h.delta > 0" [class.baja]="h.delta < 0">{{ h.delta > 0 ? '▲' : h.delta < 0 ? '▼' : '=' }} {{ h.delta === 0 ? 'igual' : (h.delta > 0 ? '+' : '') + (h.delta * 100 | number: '1.0-0') + ' pts' }}</span>
            </div>
          }
        </section>
      }

      @if (cargando()) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i><i></i></div> }
      @else if (lista().length === 0) { <p class="cristal vacio">Aún no tienes resultados. Aparecerán cuando termines un caso y se evalúe.</p> }
      @else {
        <div class="tarjetas">
          @for (r of lista(); track r.id) {
            <article class="cristal tarjeta-item">
              @if (r.oficial) { <span class="etiqueta ok">Nota oficial</span> }
              <h3>{{ r.practicaNombre }}</h3>
              <p class="meta"><span>Intento {{ r.numeroIntento }}</span><span>{{ r.fechaCalculo | date: 'd MMM y' }}</span></p>
              <p class="nota-grande">{{ r.notaFinal | number: '1.1-2' }} <small>/ {{ r.escalaMaxima }}</small></p>
              <p class="meta"><span>{{ r.correctas }} correctas</span><span>{{ r.incorrectas }} incorrectas</span><span>{{ r.noRespondidas }} sin responder</span></p>
              <div class="acciones"><button type="button" class="boton-suave boton-chico" (click)="abrir(r)">{{ abierto()?.id === r.id ? 'Ocultar detalle' : 'Ver detalle' }}</button></div>
            </article>
          }
        </div>
      }

      @if (abierto(); as r) {
        <section class="cristal bloque">
          <h3>Detalle · {{ r.practicaNombre }} (intento {{ r.numeroIntento }})</h3>
          <ul class="lista-simple">
            @for (d of r.detallePreguntas; track d.preguntaId) {
              <li class="fila-item"><span class="texto">{{ d.enunciado }}</span>
                <span [class]="'etiqueta ' + (!d.respondida ? '' : d.correcta ? 'ok' : 'mal')">{{ !d.respondida ? 'Sin responder' : d.correcta ? 'Correcta' : 'Incorrecta' }}</span></li>
            }
          </ul>
          <h4>Retroalimentación de tu docente</h4>
          @for (t of retros(); track t.id) { <div class="bloque-interno"><p style="white-space:pre-line">{{ t.texto }}</p><p class="ayuda">{{ t.fechaCreacion | date: 'd MMM y, HH:mm' }}</p></div> }
          @empty { <p class="suave">Todavía no hay retroalimentación para este intento.</p> }
        </section>
      }
    </div>
  `,
  styles: `
    .destacado { display: flex; gap: 1.3rem; align-items: center; padding: 1.6rem 1.8rem; background: radial-gradient(90% 140% at 100% 0%, rgba(var(--d-201_184_240),.6), transparent 60%), linear-gradient(120deg, rgba(255,255,255,.78), rgba(var(--d-236_229_250),.5)); }
    .medalla { font-size: 3.4rem; animation: salta 1.6s ease-out both; } @keyframes salta { 0% { transform: scale(.3) rotate(-20deg); opacity: 0; } 60% { transform: scale(1.2) rotate(8deg); } 100% { transform: none; opacity: 1; } }
    .sobre-d { text-transform: uppercase; letter-spacing: .18em; font-size: .7rem; color: var(--lila-700); }
    blockquote { margin: .8rem 0 0; padding: .8rem 1rem; border-left: 4px solid var(--lila-500); border-radius: 0 14px 14px 0; background: rgba(255,255,255,.6); white-space: pre-line; line-height: 1.5; }
    blockquote strong { display: block; font-size: .72rem; letter-spacing: .12em; text-transform: uppercase; color: var(--lila-700); margin-bottom: .2rem; }
    .historial { display: grid; grid-template-columns: minmax(100px, 30%) 1fr auto; gap: 1rem; align-items: center; padding: .4rem 0; } .historial svg { width: 100%; max-height: 56px; overflow: visible; }
    .cambio { font-size: .85rem; color: var(--tinta-suave); } .cambio.sube { color: #2d6a4f; } .cambio.baja { color: var(--alerta); }
    .confeti { position: fixed; inset: 0; pointer-events: none; z-index: 120; overflow: hidden; }
    .confeti i { position: absolute; top: -14px; width: 9px; height: 14px; border-radius: 2px; animation: cae 3.2s cubic-bezier(.3,.6,.4,1) both; transform: rotate(var(--g)); }
    @keyframes cae { to { transform: translateY(105vh) rotate(calc(var(--g) + 540deg)); opacity: .9; } }
    :host-context([data-tema='noche']) .destacado { background: radial-gradient(90% 140% at 100% 0%, rgba(var(--n-s5),.5), transparent 60%), linear-gradient(120deg, rgba(var(--n-s3),.6), rgba(var(--n-s4),.5)); }
    :host-context([data-tema='noche']) blockquote { background: rgba(255,255,255,.07); } :host-context([data-tema='noche']) .nota-grande { color: var(--n-f1ecff); }
    :host-context([data-tema='noche']) .historial circle { fill: var(--n-231b45); }
    @media (prefers-reduced-motion: reduce) { .confeti { display: none; } .medalla { animation: none; } }
    .nota-grande { font-family: var(--serif); font-size: 2.8rem; line-height: 1; color: var(--lila-900); } .nota-grande small { font-size: 1.1rem; color: var(--tinta-suave); }`,
})
export class MisResultados implements OnInit {
  private readonly api = inject(SomaApiService);
  protected readonly lista = signal<ResultadoDetalle[]>([]);
  protected readonly abierto = signal<ResultadoDetalle | null>(null);
  protected readonly retros = signal<Retroalimentacion[]>([]);
  protected readonly cargando = signal(true);
  private readonly toasts = inject(ToastService);
  protected readonly celebrar = signal(false);
  protected readonly retroDestacada = signal<Retroalimentacion[]>([]);
  protected readonly confeti = Array.from({ length: 46 }, (_, i) => ({ x: (i * 37) % 100, d: (i % 12) * 0.12, g: (i * 53) % 360, c: ['var(--n-8b6fe0)', 'var(--d-c3a9ff)', '#e8b8dd', '#aebcf0', '#f0cfa0', '#7fd1ae'][i % 6] }));
  protected readonly destacado = computed(() => [...this.lista()].sort((a, b) => b.fechaCalculo.localeCompare(a.fechaCalculo))[0] ?? null);
  protected proporcion(r: ResultadoDetalle): number { return r.escalaMaxima ? Number(r.notaFinal) / Number(r.escalaMaxima) : 0; }
  /** Evolución de la nota entre intentos de un mismo caso (solo para casos con más de un intento). */
  protected readonly historiales = computed(() => {
    const m = new Map<string, ResultadoDetalle[]>();
    for (const r of this.lista()) m.set(r.practicaNombre, [...(m.get(r.practicaNombre) ?? []), r]);
    return [...m].filter(([, v]) => v.length > 1).map(([nombre, v]) => {
      const o = [...v].sort((a, b) => a.numeroIntento - b.numeroIntento);
      const props = o.map((r) => this.proporcion(r));
      const puntos = props.map((p, i) => ({ i, x: 8 + (i * 204) / Math.max(1, props.length - 1), y: 48 - p * 40 }));
      return { nombre, puntos, linea: puntos.map((p) => `${p.x},${p.y}`).join(' '), delta: props[props.length - 1] - props[0] };
    });
  });
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.resultadosFiltrados().subscribe({
      next: (r) => {
        this.lista.set(r); this.cargando.set(false);
        const d = this.destacado();
        if (d) {
          this.api.retroalimentaciones(d.practicaId, d.participacionId).subscribe({ next: (t) => this.retroDestacada.set(t.slice(-1)), error: () => undefined });
          const clave = `soma.celebrado.${d.id}`;
          try {
            if (this.proporcion(d) >= 0.7 && !sessionStorage.getItem(clave)) { sessionStorage.setItem(clave, '1'); this.celebrar.set(true); this.toasts.ok('¡Felicitaciones por tu resultado!'); setTimeout(() => this.celebrar.set(false), 4200); }
          } catch { /* sin almacenamiento */ }
        }
      },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
  }

  protected abrir(r: ResultadoDetalle): void {
    if (this.abierto()?.id === r.id) { this.abierto.set(null); return; }
    this.abierto.set(r);
    this.api.retroalimentaciones(r.practicaId, r.participacionId).subscribe({ next: (t) => this.retros.set(t), error: () => this.retros.set([]) });
  }
}
