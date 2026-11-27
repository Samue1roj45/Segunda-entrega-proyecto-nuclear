import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { mensajeDeError } from '../../core/con-operaciones';
import { EcosService, EstadoEcos, SesionEcos } from '../../core/ecos.service';

/**
 * "Ecos de una decisión" para el estudiante. El juego (Unreal Engine) corre en este computador: SOMA crea una sesión,
 * muestra el código de un solo uso y el enlace ecos:// que abre el juego ya conectado; al terminar, el juego envía el
 * resultado y SOMA calcula la nota. Mientras hay un código vigente la página consulta el avance cada pocos segundos.
 */
@Component({
  selector: 'app-ecos-estudiante',
  imports: [DatePipe, DecimalPipe],
  template: `
    <div class="pagina">
      <div class="cabecera-pagina"><h2>Ecos de una decisión</h2>
        <p class="suave">Videojuego en Unreal Engine: vives seis actos y tomas decisiones. Tu nota la calcula SOMA con tus respuestas.</p></div>
      @if (error(); as e) { <p class="alerta-error" role="alert">{{ e }}</p> }

      @if (estado(); as s) {
        @if (!s.caso) { <p class="cristal vacio">El caso aún no está disponible. Tu docente o el administrador debe cargarlo primero.</p> }
        @else {
          <section class="cristal bloque">
            <h3>{{ s.caso }}</h3>
            <p class="meta"><span>Intentos usados {{ s.usados }} de {{ s.maxIntentos }}</span><span>Te quedan {{ s.restantes }}</span></p>

            @if (sesion(); as c) {
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
            } @else if (s.disponible) {
              <div class="acciones"><button type="button" class="boton-primario" [disabled]="ocupado()" (click)="jugar()">Jugar</button></div>
              <p class="ayuda">Se generará un código para abrir el juego en este computador.</p>
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
  `,
})
export class EcosEstudiante implements OnInit, OnDestroy {
  private readonly ecos = inject(EcosService);
  private readonly sanitizador = inject(DomSanitizer);
  protected readonly estado = signal<EstadoEcos | null>(null);
  protected readonly sesion = signal<SesionEcos | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  private readonly ahora = signal(Date.now());
  private temporizador?: ReturnType<typeof setInterval>;
  private ciclo = 0;

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
    this.temporizador = setInterval(() => {
      this.ahora.set(Date.now());
      const c = this.sesion();
      if (c && ++this.ciclo % 4 === 0 && this.faseActual() !== 'CERRADA') { this.cargar(); }
    }, 1000);
  }

  ngOnDestroy(): void { clearInterval(this.temporizador); }

  protected jugar(): void {
    this.ocupado.set(true);
    this.error.set(null);
    this.ecos.crearSesion().subscribe({
      next: (c) => { this.sesion.set(c); this.ocupado.set(false); this.cargar(); },
      error: (e: HttpErrorResponse) => { this.error.set(mensajeDeError(e)); this.ocupado.set(false); this.cargar(); },
    });
  }

  private cargar(): void {
    this.ecos.estado().subscribe({ next: (s) => this.estado.set(s), error: (e: HttpErrorResponse) => this.error.set(mensajeDeError(e)) });
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
