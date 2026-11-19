import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { relojDe } from '../../compartido/formato';
import { mensajeDeError } from '../../core/con-operaciones';
import { Contexto, Herramienta, Progreso } from '../../core/modelos/dominio';
import { ToastService } from '../../core/toast.service';
import { SomaApiService } from '../../core/soma-api.service';

/**
 * Participación del estudiante en un caso (RF-10, RF-11, RF-23): lee el contexto, responde (puede volver y cambiar),
 * ve su progreso y el tiempo restante, usa las herramientas del caso y finaliza. El servidor es quien cuenta el tiempo
 * y cierra el intento al vencer; aquí solo se muestra el reloj.
 */
@Component({
  selector: 'app-participar',
  imports: [RouterLink],
  template: `
    <div class="pagina">
      <p class="migas"><a routerLink="/mis-sesiones">Mis casos</a> / {{ contexto()?.nombreCaso ?? 'Participación' }}</p>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }
      @if (cargando()) { <p class="suave">Cargando el caso…</p> }

      @if (contexto(); as c) {
        <header class="cristal bloque cabecera-participacion">
          <div>
            <h2>{{ c.nombreCaso }}</h2>
            @if (progreso(); as p) {
              <p class="suave">{{ p.preguntasRespondidas }} de {{ p.totalPreguntas }} preguntas respondidas</p>
              <div class="barra-progreso" role="progressbar" [attr.aria-valuenow]="p.porcentajeProgreso" aria-valuemin="0" aria-valuemax="100"><span [style.width.%]="p.porcentajeProgreso"></span></div>
            }
          </div>
          <div class="reloj" [class.urgente]="segundos() <= 300" [class.critico]="segundos() <= 60" role="timer" aria-live="off">
            <svg viewBox="0 0 100 100" class="anillo" aria-hidden="true">
              <circle cx="50" cy="50" r="44" class="pista" />
              <circle cx="50" cy="50" r="44" class="avance" [attr.stroke-dasharray]="circunferencia" [attr.stroke-dashoffset]="circunferencia * (1 - fraccion())" />
            </svg>
            <div class="num"><small>Tiempo restante</small><strong>{{ reloj(segundos()) }}</strong></div>
          </div>
          <span class="guardado" [class.visible]="guardadoVisible()" role="status"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7.5" /></svg> Respuesta guardada</span>
        </header>

        @if (c.contextoHistoria) { <section class="cristal bloque"><h3>Contexto</h3><p class="texto-largo">{{ c.contextoHistoria }}</p></section> }
        @if (c.desarrolloSituacional) { <section class="cristal bloque"><h3>Desarrollo de la situación</h3><p class="texto-largo">{{ c.desarrolloSituacional }}</p></section> }

        @if (herramientas().length) {
          <section class="cristal bloque">
            <h3>Herramientas de apoyo</h3>
            <ul class="lista-simple">
              @for (h of herramientas(); track h.idHerramienta) {
                <li class="fila-item"><span class="texto"><strong>{{ h.nombre }}</strong> <span class="etiqueta">{{ h.tipo }}</span>
                  @if (h.descripcion) { <br /><span class="ayuda">{{ h.descripcion }}</span> }
                  @if (h.contenido) { <br />@if (esEnlace(h.contenido)) { <a [href]="h.contenido" target="_blank" rel="noopener noreferrer">{{ h.contenido }}</a> } @else { <span class="texto-largo">{{ h.contenido }}</span> } }
                </span></li>
              }
            </ul>
          </section>
        }

        @for (e of c.escenarios; track e.idEscenario) {
          <section class="cristal bloque">
            <h3>Escenario {{ e.orden }} · {{ e.nombre }}</h3>
            <p class="texto-largo suave">{{ e.contexto }}</p>
            @for (p of e.preguntas; track p.idPregunta) {
              <fieldset class="bloque-interno pregunta">
                <legend>{{ p.orden }}. {{ p.enunciado }}</legend>
                @for (o of p.opciones; track o.idRespuesta) {
                  <label class="opcion" [class.elegida]="elegida(p.idPregunta, p.idRespuestaSeleccionada) === o.idRespuesta">
                    <input type="radio" [name]="'p' + p.idPregunta" [checked]="elegida(p.idPregunta, p.idRespuestaSeleccionada) === o.idRespuesta"
                           [disabled]="cerrada()" (change)="responder(p.idPregunta, o.idRespuesta)" />
                    <span class="codigo">{{ o.codigo }}</span> <span>{{ o.texto }}</span>
                  </label>
                }
              </fieldset>
            }
          </section>
        }

        <section class="cristal bloque">
          @if (cerrada()) {
            <p class="aviso-ok">Tu intento terminó. Puedes ver tu resultado cuando el docente lo publique.</p>
            <a routerLink="/mis-resultados" class="boton-primario enlace-boton" style="width:fit-content">Ir a mis resultados</a>
          } @else {
            <p class="suave">Revisa tus respuestas. Puedes volver y cambiarlas hasta que finalices o se acabe el tiempo.</p>
            <div class="acciones"><button type="button" class="boton-primario" [disabled]="ocupado()" (click)="finalizar()">Finalizar intento</button></div>
          }
        </section>
      }
    </div>
  `,
  styles: `
    :host-context([data-tema='noche']) .reloj .pista { stroke: rgba(255,255,255,.14); } :host-context([data-tema='noche']) .reloj strong { color: #f1ecff; }
    :host-context([data-tema='noche']) .guardado { background: rgba(92,184,143,.22); color: #8fe0bb; }
    :host-context([data-tema='noche']) .opcion { background: rgba(255,255,255,.06); } :host-context([data-tema='noche']) .opcion:hover { background: rgba(255,255,255,.12); }
    :host-context([data-tema='noche']) .opcion.elegida { background: rgba(139,111,224,.22); border-color: #a692ea; } :host-context([data-tema='noche']) legend { color: #efe9ff; }
    .cabecera-participacion { display: flex; flex-wrap: wrap; gap: 1.2rem; justify-content: space-between; align-items: center; position: sticky; top: .6rem; z-index: 3; }
    .cabecera-participacion > div:first-child { flex: 1; min-width: 240px; display: grid; gap: .5rem; }
    .reloj { position: relative; width: 132px; height: 132px; display: grid; place-items: center; }
    .anillo { position: absolute; inset: 0; width: 100%; height: 100%; transform: rotate(-90deg); }
    .anillo circle { fill: none; stroke-width: 7; } .anillo .pista { stroke: rgba(94,74,138,.16); }
    .anillo .avance { stroke: url(#x); stroke: var(--lila-700); stroke-linecap: round; transition: stroke-dashoffset 1s linear, stroke .4s; }
    .reloj.urgente .avance { stroke: #d98a5b; } .reloj.critico .avance { stroke: var(--alerta); animation: late 1s ease-in-out infinite; }
    @keyframes late { 50% { opacity: .55; } }
    .num { position: relative; text-align: center; display: grid; line-height: 1.1; }
    .guardado { position: absolute; right: 1.4rem; bottom: -.9rem; display: inline-flex; align-items: center; gap: .35rem; padding: .3rem .75rem; border-radius: 999px; font-size: .78rem; color: #2d6a4f; background: #d8efe4;
      opacity: 0; transform: translateY(6px); transition: opacity .25s, transform .25s; pointer-events: none; }
    .guardado.visible { opacity: 1; transform: none; } .guardado svg { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }
    .cabecera-participacion { padding-bottom: 1.6rem; }
    .reloj small { color: var(--tinta-suave); font-size: .7rem; }
    .reloj strong { font-family: var(--serif); font-size: 1.9rem; color: var(--lila-900); font-variant-numeric: tabular-nums; }
    .reloj.urgente strong { color: var(--alerta); }
    .texto-largo { white-space: pre-line; line-height: 1.55; }
    fieldset.pregunta { margin: 0; border: 1px solid rgba(255,255,255,.7); }
    legend { font-weight: 500; padding: 0 .4rem; color: var(--lila-900); }
    .opcion { display: flex; gap: .7rem; align-items: flex-start; padding: .65rem .8rem; border-radius: 12px; background: rgba(255,255,255,.5); cursor: pointer; border: 1px solid transparent; transition: background .15s, border-color .15s; }
    .opcion:hover { background: rgba(255,255,255,.85); }
    .opcion.elegida { background: #fff; border-color: var(--lila-500); }
  `,
})
export class Participar implements OnInit, OnDestroy {
  private readonly api = inject(SomaApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  private id = 0;
  private totalSeg = 0;
  private avisado5 = false;
  private avisado1 = false;
  protected readonly circunferencia = 2 * Math.PI * 44;
  protected readonly guardadoVisible = signal(false);
  private ocultarGuardado: ReturnType<typeof setTimeout> | undefined;
  protected readonly fraccion = computed(() => (this.totalSeg ? Math.min(1, this.segundos() / this.totalSeg) : 1));
  private reloj$: ReturnType<typeof setInterval> | undefined;
  private sincronizar$: ReturnType<typeof setInterval> | undefined;

  protected readonly reloj = relojDe;
  protected readonly contexto = signal<Contexto | null>(null);
  protected readonly progreso = signal<Progreso | null>(null);
  protected readonly herramientas = signal<Herramienta[]>([]);
  protected readonly segundos = signal(0);
  protected readonly cerrada = signal(false);
  protected readonly cargando = signal(true);
  protected readonly ocupado = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly marcadas = signal<Record<number, number>>({});
  protected readonly total = computed(() => this.contexto()?.escenarios.reduce((n, e) => n + e.preguntas.length, 0) ?? 0);

  ngOnInit(): void {
    this.id = Number(this.ruta.snapshot.paramMap.get('id'));
    this.api.contexto(this.id).subscribe({
      next: (c) => { this.contexto.set(c); this.cargando.set(false); },
      error: (e: HttpErrorResponse) => { this.cargando.set(false); this.error.set(mensajeDeError(e)); },
    });
    this.api.herramientasParticipacion(this.id).subscribe({ next: (h) => this.herramientas.set(h), error: () => this.herramientas.set([]) });
    this.actualizarProgreso();
    this.sincronizarTiempo();
    this.reloj$ = setInterval(() => { this.segundos.update((s) => Math.max(0, s - 1)); this.avisarTiempo(); }, 1000);
    this.sincronizar$ = setInterval(() => this.sincronizarTiempo(), 30_000);
  }

  ngOnDestroy(): void { clearInterval(this.reloj$); clearInterval(this.sincronizar$); clearTimeout(this.ocultarGuardado); }

  private avisarTiempo(): void {
    const s = this.segundos();
    if (s <= 300 && s > 60 && !this.avisado5 && !this.cerrada()) { this.avisado5 = true; this.toasts.info('Quedan 5 minutos. Revisa tus respuestas.'); }
    if (s <= 60 && s > 0 && !this.avisado1 && !this.cerrada()) { this.avisado1 = true; this.toasts.error('¡Queda 1 minuto! Tus respuestas ya están guardadas.'); }
  }

  protected elegida(pregunta: number, delServidor: number | null): number | null { return this.marcadas()[pregunta] ?? delServidor; }
  protected esEnlace(texto: string): boolean { return /^https?:\/\//i.test(texto.trim()); }

  private actualizarProgreso(): void { this.api.progreso(this.id).subscribe({ next: (p) => this.progreso.set(p), error: () => undefined }); }

  private sincronizarTiempo(): void {
    this.api.tiempo(this.id).subscribe({
      next: (t) => {
        this.segundos.set(t.segundosRestantes);
        if (t.segundosRestantes > this.totalSeg) this.totalSeg = t.segundosRestantes;
        if (t.expirada || t.estado === 'FINALIZADA' || t.estado === 'INCOMPLETA') { this.cerrada.set(true); clearInterval(this.reloj$); }
      },
      error: () => undefined,
    });
  }

  protected responder(pregunta: number, respuesta: number): void {
    this.error.set(null);
    this.api.responder(this.id, pregunta, respuesta).subscribe({
      next: () => {
        this.marcadas.update((m) => ({ ...m, [pregunta]: respuesta })); this.actualizarProgreso();
        this.guardadoVisible.set(true); clearTimeout(this.ocultarGuardado); this.ocultarGuardado = setTimeout(() => this.guardadoVisible.set(false), 1800);
      },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.sincronizarTiempo(); },
    });
  }

  protected finalizar(): void {
    const p = this.progreso();
    const faltan = p ? p.totalPreguntas - p.preguntasRespondidas : 0;
    const texto = faltan > 0 ? `Te faltan ${faltan} pregunta(s) por responder. ¿Finalizar de todos modos?` : '¿Finalizar tu intento?';
    if (!confirm(texto)) { return; }
    this.ocupado.set(true); this.error.set(null);
    this.api.finalizarParticipacion(this.id).subscribe({
      next: () => { this.ocupado.set(false); this.cerrada.set(true); clearInterval(this.reloj$); void this.router.navigate(['/mis-resultados']); },
      error: (e: HttpErrorResponse) => { this.ocupado.set(false); this.error.set(mensajeDeError(e)); },
    });
  }
}
