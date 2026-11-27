import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Component, OnDestroy, OnInit, computed, effect, inject, input, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { AvanceSesionEcos, ConfigPixelStreaming, EcosService, EstadoEcos, SesionEcos } from '../../core/ecos.service';
import { MensajeVisor, SesionParaJuego, VisorPixelStreaming } from './visor-pixel-streaming';

/** Dirección de la API que recibe el juego por Pixel Streaming (ver apiParaStreaming). */
export function apiParaJuego(ubicacion: { hostname: string; origin: string }, apiDelBackend: string): string {
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(ubicacion.hostname);
  return local ? apiDelBackend : `${ubicacion.origin}/api/ecos/v1`;
}

/**
 * "Ecos de una decisión" para el estudiante. El juego (Unreal Engine) corre en este computador: SOMA crea una sesión,
 * muestra el código de un solo uso y el enlace ecos:// que abre el juego ya conectado; al terminar, el juego envía el
 * resultado y SOMA calcula la nota. Mientras hay un código vigente la página consulta el avance cada pocos segundos.
 * Con Pixel Streaming (ECOS_PIXEL_STREAMING_URL) el juego se ve dentro de esta misma página y su avance sale en «En vivo».
 */
@Component({
  selector: 'app-ecos-estudiante',
  imports: [DatePipe, DecimalPipe, VisorPixelStreaming],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2 class="sr-only">Ecos de una decisión</h2>
        <p class="suave">Videojuego en Unreal Engine: vives seis actos y tomas decisiones. Tu nota la calcula SOMA con tus respuestas.</p></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }

      @if (estado(); as s) {
        @if (!s.caso) { <p class="cristal vacio">El caso aún no está disponible. Tu docente o el administrador debe cargarlo primero.</p> }
        @else {
          <section class="cristal bloque">
            <h3>{{ s.caso }}</h3>
            <p class="meta"><span>Intentos usados {{ s.usados }} de {{ s.maxIntentos }}</span><span>Te quedan {{ s.restantes }}</span></p>

            @if (sesion(); as c) {
              @if (streaming(); as ps) {
                <div class="juego-en-pagina">
                  <app-visor-pixel-streaming [url]="ps.url" [origen]="ps.origen" [sesion]="sesionJuego()" (mensaje)="alMensaje($event)" />
                  <aside class="en-vivo cristal" aria-live="polite" aria-label="Lo que pasa en el juego">
                    <h4>En vivo</h4>
                    <dl>
                      <div><dt>Conexión</dt><dd>{{ textoConexion() }}</dd></div>
                      <div><dt>Juego</dt><dd>{{ textoFase() }}</dd></div>
                      <div><dt>Acto</dt><dd>{{ acto() ?? '—' }} de 6</dd></div>
                      <div><dt>Último punto</dt><dd>{{ punto() ?? '—' }}</dd></div>
                    </dl>
                    @if (resultadoSesion(); as r) {
                      <p class="nota-vivo">{{ r.nota | number: '1.1-1' }} <small>/ 5.0 · {{ r.nivel }}</small></p>
                    }
                    @if (faseActual() === 'CREADA' && puente() !== 'instalado') {
                      <p class="ayuda codigo-mini">Si el juego te pide un código, escribe <strong>{{ c.codigo }}</strong> (vale {{ restante() }}).</p>
                    }
                    @if (feed().length) {
                      <ol class="feed">
                        @for (f of feed(); track f.id) { <li><time>{{ f.hora | date: 'HH:mm:ss' }}</time> {{ f.texto }}</li> }
                      </ol>
                    }
                  </aside>
                </div>
              } @else {
              <div class="codigo-caja" aria-live="polite">
                <p class="et">Tu código de sesión</p>
                <p class="codigo">{{ c.codigo }}</p>
                <p class="ayuda">Vale {{ restante() }} y se usa una sola vez.</p>
                @switch (faseActual()) {
                  @case ('CREADA') { <p class="aviso-info">Esperando que abras el juego…</p> }
                  @case ('CANJEADA') { <p class="aviso-ok">¡Juego conectado! Juega hasta el informe final; el resultado llegará solo.</p> }
                  @case ('CERRADA') { <p class="aviso-ok">¡Resultado recibido! Ya puedes ver tu nota abajo.</p> }
                }
              </div>
              <div class="acciones">
                <a class="boton-primario" [href]="enlaceSeguro(c)">Abrir el juego</a>
                <button type="button" class="boton-suave" (click)="copiar(c.codigo)">Copiar código</button>
              </div>
              <details class="ayuda-juego">
                <summary>El juego no se abre</summary>
                <ol>
                  <li>Instala el juego en este computador (si es la primera vez, el instalador de Ecos registra el enlace <code>ecos://</code>).</li>
                  <li>O ábrelo a mano desde una terminal con este comando:
                    <pre>CitySample.exe -EcosSesion={{ c.codigo }} -EcosApi={{ api(c) }}</pre></li>
                  <li>Si el código venció, vuelve a esta página y genera otro.</li>
                </ol>
              </details>
              }
            } @else if (s.disponible) {
              <div class="acciones"><button type="button" class="boton-primario" [disabled]="ocupado()" (click)="jugar()">Jugar</button></div>
              <p class="ayuda">{{ streaming() ? 'El juego se abrirá aquí mismo, dentro de esta página.' : 'Se generará un código para abrir el juego en este computador.' }}</p>
            } @else {
              <p class="aviso-info">Ya usaste todos tus intentos.</p>
            }
          </section>

          @if (s.intentos.length) {
            <section class="cristal bloque">
              <h3>Mis intentos</h3>
              <div class="tarjetas">
                @for (i of s.intentos; track i.sesionId) {
                  <article class="cristal tarjeta-item">
                    <span [class]="'etiqueta ' + (i.estado === 'CERRADA' ? 'ok' : '')">{{ etiqueta(i.estado) }}</span>
                    @if (i.oficial) { <span class="etiqueta ok" title="Tu mejor intento es el que cuenta">Nota oficial</span> }
                    <h4>Intento {{ i.intento }}</h4>
                    @if (i.nota !== null) {
                      <p class="nota-grande">{{ i.nota | number: '1.1-1' }} <small>/ 5.0 · {{ i.nivel }}</small></p>
                      <p class="meta"><span>{{ i.puntaje | number: '1.0-2' }} puntos</span><span>{{ desenlace(i.desenlace) }}</span></p>
                      <p class="ayuda">{{ i.fin | date: 'd MMM y, HH:mm' }}</p>
                    } @else { <p class="suave">Sin resultado todavía.</p> }
                  </article>
                }
              </div>
            </section>
          }
        }
      } @else if (!error()) { <div class="esqueleto" role="status" aria-label="Cargando"><i></i><i></i><i></i></div> }
    </div>
  `,
  styles: `
    .codigo-caja { text-align: center; padding: 1.2rem; margin: 1rem 0; border-radius: 18px; background: rgba(255,255,255,.6); }
    .et { text-transform: uppercase; letter-spacing: .16em; font-size: .72rem; color: var(--lila-700); margin: 0; }
    .codigo { font: 700 2.4rem/1.2 ui-monospace, SFMono-Regular, Menlo, monospace; letter-spacing: .18em; margin: .2rem 0; }
    .ayuda-juego { margin-top: 1rem; } .ayuda-juego pre { white-space: pre-wrap; word-break: break-all; padding: .7rem; border-radius: 10px; background: rgba(0,0,0,.06); }
    .nota-grande { font-size: 2.2rem; font-weight: 600; margin: 0; } .nota-grande small { font-size: .9rem; font-weight: 400; color: var(--tinta-suave); }
    a.boton-primario { text-decoration: none; display: inline-block; }
    .juego-en-pagina { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 1rem; align-items: start; margin-top: 1rem; }
    @media (max-width: 1100px) { .juego-en-pagina { grid-template-columns: 1fr; } }
    .en-vivo { padding: 1rem 1.1rem; display: grid; gap: .6rem; }
    .en-vivo h4 { margin: 0; font-size: 1rem; }
    .en-vivo dl { margin: 0; display: grid; gap: .35rem; }
    .en-vivo dl > div { display: flex; justify-content: space-between; gap: .8rem; font-size: .88rem; }
    .en-vivo dt { color: var(--tinta-suave); } .en-vivo dd { margin: 0; font-weight: 600; text-align: right; }
    .nota-vivo { font-size: 1.8rem; font-weight: 600; margin: 0; } .nota-vivo small { font-size: .85rem; font-weight: 400; color: var(--tinta-suave); }
    .codigo-mini { margin: 0; }
    .feed { list-style: none; margin: 0; padding: 0; display: grid; gap: .3rem; max-height: 220px; overflow: auto; font-size: .82rem; }
    .feed time { color: var(--tinta-suave); font-variant-numeric: tabular-nums; margin-right: .35rem; }
  `,
})
export class EcosEstudiante implements OnInit, OnDestroy {
  constructor() {
    // Apertura automática (solo con autoIniciar y Pixel Streaming): una vez, cuando ya se sabe que hay intentos disponibles.
    effect(() => {
      const s = this.estado();
      if (!this.autoIniciar() || this.autoIntentado || !this.streamingConsultado() || !s) { return; }
      this.autoIntentado = true;
      if (this.streaming() && s.caso && s.disponible && !this.sesion()) { this.jugar(); }
    });
  }

  private readonly ecos = inject(EcosService);
  private readonly sanitizador = inject(DomSanitizer);
  protected readonly estado = signal<EstadoEcos | null>(null);
  protected readonly sesion = signal<SesionEcos | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  /**
   * Dentro de la participación (Mis casos → Comenzar) el estudiante ya eligió jugar: con Pixel Streaming el juego se abre
   * solo, sin pedir otro clic en «Jugar». En la página «Ecos de una decisión» del menú sigue el botón.
   */
  readonly autoIniciar = input(false);
  private readonly streamingConsultado = signal(false);
  private autoIntentado = false;
  private readonly ahora = signal(Date.now());
  private temporizador?: ReturnType<typeof setInterval>;
  private ciclo = 0;

  // ---- Pixel Streaming: el juego se ve en la página y lo que informa se refleja en el panel «En vivo».
  /** Reproductor configurado en el servidor (ECOS_PIXEL_STREAMING_URL); null = se usa el enlace ecos:// como antes. */
  protected readonly streaming = signal<ConfigPixelStreaming | null>(null);
  protected readonly puente = signal<'esperando' | 'instalado' | 'ausente'>('esperando');
  private readonly conexion = signal('conectando');
  private readonly avance = signal<AvanceSesionEcos | null>(null);
  private readonly actoJuego = signal<number | null>(null);
  private readonly puntoJuego = signal<string | null>(null);
  protected readonly feed = signal<{ id: number; hora: Date; texto: string }[]>([]);
  private idFeed = 0;
  private faseAnunciada: string | null = null;

  protected readonly acto = computed(() => this.actoJuego() ?? this.avance()?.ultimoActo ?? null);
  protected readonly punto = computed(() => this.puntoJuego() ?? this.avance()?.ultimoPunto ?? null);
  protected readonly resultadoSesion = computed(() => {
    const c = this.sesion();
    const i = c ? this.estado()?.intentos.find((x) => x.sesionId === c.sesionId) : undefined;
    return i && i.nota !== null ? i : null;
  });
  protected readonly textoConexion = computed(() => ({
    conectando: 'Conectando con el servidor…', conectado: 'Conectado', video: 'Transmitiendo', desconectado: 'Desconectado',
    'sin-puente': 'Solo video (sin puente)',
  } as Record<string, string>)[this.conexion()] ?? this.conexion());
  protected readonly textoFase = computed(() => ({
    CREADA: 'Esperando que el juego se conecte', CANJEADA: 'Jugando', CERRADA: 'Terminado', VENCIDA: 'El código venció',
  } as Record<string, string>)[this.faseActual() ?? ''] ?? '—');

  /** Fase de la sesión que se está mostrando, según el último estado consultado. */
  protected readonly faseActual = computed(() => {
    const c = this.sesion();
    return c ? (this.estado()?.intentos.find((i) => i.sesionId === c.sesionId)?.estado ?? 'CREADA') : null;
  });
  protected readonly restante = computed(() => {
    const c = this.sesion();
    if (!c) { return ''; }
    const s = Math.max(0, Math.floor((new Date(c.venceEn).getTime() - this.ahora()) / 1000));
    return s === 0 ? '0:00 (venció)' : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
  });

  ngOnInit(): void {
    this.cargar();
    this.ecos.pixelStreaming().subscribe({
      next: (p) => { this.streaming.set(p.activo ? p : null); this.streamingConsultado.set(true); },
      error: () => { this.streaming.set(null); this.streamingConsultado.set(true); },
    });
    this.temporizador = setInterval(() => {
      this.ahora.set(Date.now());
      const c = this.sesion();
      if (c && ++this.ciclo % 4 === 0 && this.faseActual() !== 'CERRADA') {
        this.cargar();
        if (this.streaming()) { this.consultarAvance(c.sesionId); }
      }
    }, 1000);
  }

  ngOnDestroy(): void { clearInterval(this.temporizador); }

  /** Datos que el puente le entrega al juego: el código de un solo uso y la dirección de la API de SOMA. */
  protected readonly sesionJuego = computed<SesionParaJuego | null>(() => {
    const c = this.sesion();
    return c ? { sesionId: c.sesionId, codigo: c.codigo, api: this.apiParaStreaming(c) } : null;
  });

  /**
   * Con Pixel Streaming el juego corre en otro servidor y necesita una dirección pública de SOMA. Esta misma página la
   * tiene: su servidor reenvía /api al backend. Así no hace falta configurar ECOS_API_URL. En localhost esa dirección no
   * le sirve a un servidor remoto, así que ahí se usa la que publica el backend (la del enlace ecos://).
   */
  private apiParaStreaming(c: SesionEcos): string { return apiParaJuego(window.location, this.api(c)); }

  protected alMensaje(m: MensajeVisor): void {
    switch (m.tipo) {
      case 'puente':
        this.puente.set(m.instalado ? 'instalado' : 'ausente');
        if (!m.instalado) {
          this.conexion.set('sin-puente');
          this.anotar('El reproductor no tiene el puente de SOMA: si el juego lo pide, escribe el código.');
        }
        break;
      case 'estado':
        this.conexion.set(m.estado);
        break;
      case 'entregada':
        this.anotar('Código de sesión entregado al juego.');
        break;
      case 'juego': {
        const d = m.datos;
        if (typeof d['acto'] === 'number') { this.actoJuego.set(d['acto']); }
        if (typeof d['punto'] === 'string') { this.puntoJuego.set(d['punto']); }
        const texto = typeof d['texto'] === 'string' ? d['texto'] : typeof d['mensaje'] === 'string' ? d['mensaje'] : this.resumen(d);
        this.anotar(texto);
        // Lo que el juego anuncia se confirma contra SOMA, que es la fuente de verdad (estado y nota).
        const c = this.sesion();
        if (c) { this.cargar(); this.consultarAvance(c.sesionId); }
        break;
      }
    }
  }

  private consultarAvance(sesionId: string): void {
    this.ecos.avance(sesionId).subscribe({ next: (a) => this.avance.set(a), error: () => undefined });
  }

  private anotar(texto: string): void {
    const t = texto.length > 160 ? texto.slice(0, 157) + '…' : texto;
    this.feed.update((f) => [{ id: ++this.idFeed, hora: new Date(), texto: t }, ...f].slice(0, 12));
  }

  private resumen(d: Record<string, unknown>): string {
    return Object.entries(d).filter(([, v]) => ['string', 'number', 'boolean'].includes(typeof v)).map(([k, v]) => `${k}: ${v}`).join(' · ') || 'Evento del juego';
  }

  protected jugar(): void {
    this.ocupado.set(true);
    this.error.set(null);
    this.ecos.crearSesion().subscribe({
      next: (c) => { this.sesion.set(c); this.ocupado.set(false); this.cargar(); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.ocupado.set(false); this.cargar(); },
    });
  }

  private cargar(): void {
    this.ecos.estado().subscribe({
      next: (s) => { this.estado.set(s); this.anunciarFase(); },
      error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)),
    });
  }

  /** En modo streaming, cada cambio de fase confirmado por SOMA queda en el panel «En vivo». */
  private anunciarFase(): void {
    const f = this.faseActual();
    if (!this.streaming() || !f || f === this.faseAnunciada) { return; }
    this.faseAnunciada = f;
    const r = this.resultadoSesion();
    const textos: Record<string, string> = {
      CANJEADA: 'El juego se conectó con SOMA.',
      CERRADA: r && r.nota !== null ? `SOMA recibió el resultado: ${r.nota.toFixed(1)} / 5.0 (${r.nivel}).` : 'SOMA recibió el resultado.',
    };
    if (textos[f]) { this.anotar(textos[f]); }
  }

  /**
   * Angular marca como insegura cualquier URL que no sea http(s), así que el esquema ecos:// se autoriza a mano. Solo se
   * autoriza si empieza por ecos://iniciar? (lo que genera el backend); cualquier otra cosa se reemplaza por '#'.
   */
  protected enlaceSeguro(c: SesionEcos): SafeUrl {
    return c.enlace.startsWith('ecos://iniciar?') ? this.sanitizador.bypassSecurityTrustUrl(c.enlace) : '#';
  }
  protected api(c: SesionEcos): string { return decodeURIComponent(c.enlace.split('&api=')[1] ?? ''); }
  protected copiar(codigo: string): void { void navigator.clipboard?.writeText(codigo); }
  protected etiqueta(e: string): string { return ({ CREADA: 'Sin abrir', CANJEADA: 'En juego', CERRADA: 'Terminado' } as Record<string, string>)[e] ?? e; }
  protected desenlace(d: string | null): string {
    return ({ FIN_SOSTENIDA: 'Final sostenido', FIN_FRAGIL: 'Final frágil', FIN_ROTA: 'Final roto' } as Record<string, string>)[d ?? ''] ?? '';
  }
}
