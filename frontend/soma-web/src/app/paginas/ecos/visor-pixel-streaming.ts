import { Component, ElementRef, OnDestroy, OnInit, computed, effect, inject, input, output, signal, viewChild } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/** Lo que SOMA le entrega al juego por el puente: el mismo código de un solo uso del enlace ecos://. */
export interface SesionParaJuego { sesionId: string; codigo: string; api: string; }

/**
 * Mensajes que llegan del reproductor (pixel-streaming/soma-puente.js):
 * - puente: si el reproductor tiene el puente de SOMA instalado (si no, el estudiante escribe el código en el juego).
 * - estado: conexión del video (conectando, conectado, video, desconectado).
 * - entregada: el puente ya le pasó el código de sesión al juego.
 * - juego: datos que envía Unreal con «Send Pixel Streaming Response» (JSON libre: acto, punto, texto…).
 */
export type MensajeVisor =
  | { tipo: 'puente'; instalado: boolean }
  | { tipo: 'estado'; estado: string }
  | { tipo: 'entregada' }
  | { tipo: 'juego'; datos: Record<string, unknown> };

/** Identificador de los mensajes del puente, para no confundirlos con los de otras páginas o extensiones. */
export const FUENTE_PUENTE = 'ecos-ps';
const MAX_SALUDOS = 15;

/**
 * Reproductor de Pixel Streaming dentro de la página. Solo acepta mensajes que vengan del iframe y de su origen, y solo le
 * envía datos a ese origen; el código de sesión se entrega por postMessage (no por la URL, para que no quede en historiales).
 */
@Component({
  selector: 'app-visor-pixel-streaming',
  template: `
    <div class="visor" #contenedor>
      <iframe #marco [src]="direccion()" title="Ecos de una decisión (Pixel Streaming)"
              allow="autoplay; fullscreen; gamepad; microphone; clipboard-read; clipboard-write; xr-spatial-tracking"
              allowfullscreen referrerpolicy="origin" (load)="alCargar()"></iframe>
    </div>
    <div class="barra">
      <button type="button" class="boton-suave boton-chico" (click)="pantallaCompleta()">Pantalla completa</button>
      <button type="button" class="boton-suave boton-chico" (click)="recargar()">Reconectar</button>
    </div>
  `,
  styles: `
    :host { display: block; }
    .visor { position: relative; width: 100%; aspect-ratio: 16 / 9; border-radius: 16px; overflow: hidden; background: #0d0a16; box-shadow: 0 18px 40px -22px rgba(20, 10, 40, .6); }
    .visor:fullscreen { border-radius: 0; }
    iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
    .barra { display: flex; gap: .5rem; justify-content: flex-end; margin-top: .6rem; }
  `,
})
export class VisorPixelStreaming implements OnInit, OnDestroy {
  readonly url = input.required<string>();
  readonly origen = input.required<string>();
  readonly sesion = input<SesionParaJuego | null>(null);
  readonly mensaje = output<MensajeVisor>();

  private readonly sanitizador = inject(DomSanitizer);
  private readonly marco = viewChild.required<ElementRef<HTMLIFrameElement>>('marco');
  private readonly contenedor = viewChild.required<ElementRef<HTMLElement>>('contenedor');
  private readonly recarga = signal(0);
  private readonly puenteListo = signal(false);
  private entregadaA: string | null = null;
  private saludos = 0;
  private temporizador?: ReturnType<typeof setInterval>;
  private readonly escuchar = (e: MessageEvent) => this.recibir(e);

  /** La URL viene del backend, que solo publica http(s) absolutas; aquí se vuelve a comprobar antes de confiar en ella. */
  protected readonly direccion = computed<SafeResourceUrl | null>(() => {
    this.recarga();
    const u = this.url();
    return /^https?:\/\//i.test(u) ? this.sanitizador.bypassSecurityTrustResourceUrl(u) : null;
  });

  constructor() {
    // En cuanto el puente responde y hay sesión, se le entrega el código al juego (una sola vez por sesión).
    effect(() => {
      const s = this.sesion();
      if (this.puenteListo() && s && this.entregadaA !== s.sesionId) {
        this.enviar({ tipo: 'sesion', sesionId: s.sesionId, codigo: s.codigo, api: s.api });
        this.entregadaA = s.sesionId;
      }
    });
  }

  ngOnInit(): void { window.addEventListener('message', this.escuchar); }

  ngOnDestroy(): void {
    window.removeEventListener('message', this.escuchar);
    clearInterval(this.temporizador);
  }

  /** Cada carga del iframe vuelve a saludar: si el puente está, contesta «listo»; si no, se avisa tras unos intentos. */
  protected alCargar(): void {
    this.puenteListo.set(false);
    this.entregadaA = null;
    this.saludos = 0;
    clearInterval(this.temporizador);
    this.mensaje.emit({ tipo: 'estado', estado: 'conectando' });
    this.temporizador = setInterval(() => {
      if (this.puenteListo()) { clearInterval(this.temporizador); return; }
      if (++this.saludos > MAX_SALUDOS) {
        clearInterval(this.temporizador);
        this.mensaje.emit({ tipo: 'puente', instalado: false });
        return;
      }
      this.enviar({ tipo: 'hola' });
    }, 1000);
  }

  protected pantallaCompleta(): void { void this.contenedor().nativeElement.requestFullscreen?.(); }

  protected recargar(): void { this.recarga.update((n) => n + 1); }

  private enviar(datos: Record<string, unknown>): void {
    this.marco().nativeElement.contentWindow?.postMessage({ fuente: 'soma', ...datos }, this.origen());
  }

  private recibir(e: MessageEvent): void {
    if (e.origin !== this.origen() || e.source !== this.marco().nativeElement.contentWindow) { return; }
    const d = e.data as { fuente?: string; tipo?: string; estado?: string; datos?: unknown } | null;
    if (!d || d.fuente !== FUENTE_PUENTE) { return; }
    switch (d.tipo) {
      case 'listo':
        if (!this.puenteListo()) { this.puenteListo.set(true); this.mensaje.emit({ tipo: 'puente', instalado: true }); }
        break;
      case 'estado':
        if (typeof d.estado === 'string') { this.mensaje.emit({ tipo: 'estado', estado: d.estado }); }
        break;
      case 'entregada':
        this.mensaje.emit({ tipo: 'entregada' });
        break;
      case 'juego':
        if (d.datos && typeof d.datos === 'object' && !Array.isArray(d.datos)) {
          this.mensaje.emit({ tipo: 'juego', datos: d.datos as Record<string, unknown> });
        }
        break;
    }
  }
}
