import { Injectable, NgZone, inject } from '@angular/core';
import { SonidoService } from './sonido.service';

/**
 * Efectos de puntero para todo el sitio, con un solo oyente: foco de luz que sigue al cursor sobre el cristal
 * (variables --mx/--my) e inclinación 3D suave en tarjetas (.kpi, .tarjeta-item). Se apagan con «reducir animaciones».
 */
@Injectable({ providedIn: 'root' })
export class EfectosService {
  private readonly zona = inject(NgZone);
  private readonly sonido = inject(SonidoService);
  private magnetico: HTMLElement | null = null;
  private pendiente: PointerEvent | null = null;
  private inclinado: HTMLElement | null = null;

  iniciar(): void {
    if (typeof document === 'undefined') return;
    if (matchMedia('(pointer: coarse)').matches) { document.addEventListener('pointerdown', (e) => this.onda(e), { passive: true }); return; }
    this.zona.runOutsideAngular(() => {
      document.addEventListener('pointermove', (e) => { if (!this.pendiente) requestAnimationFrame(() => this.aplicar()); this.pendiente = e; }, { passive: true });
      document.addEventListener('pointerleave', () => this.soltar(), { passive: true });
      document.addEventListener('pointerdown', (e) => this.onda(e), { passive: true });
    });
  }

  private quieto(): boolean { return document.documentElement.hasAttribute('data-quieto') || matchMedia('(prefers-reduced-motion: reduce)').matches; }

  private aplicar(): void {
    const e = this.pendiente; this.pendiente = null;
    if (!e || this.quieto()) return;
    const objetivo = e.target as HTMLElement | null;
    const vidrio = objetivo?.closest<HTMLElement>('.cristal');
    if (vidrio) { const r = vidrio.getBoundingClientRect(); vidrio.style.setProperty('--mx', `${e.clientX - r.left}px`); vidrio.style.setProperty('--my', `${e.clientY - r.top}px`); }
    const boton = objetivo?.closest<HTMLElement>('.boton-primario:not(:disabled)');
    if (this.magnetico && this.magnetico !== boton) { this.magnetico.style.translate = ''; this.magnetico = null; }
    if (boton) { const r = boton.getBoundingClientRect(); boton.style.translate = `${(e.clientX - r.left - r.width / 2) * .14}px ${(e.clientY - r.top - r.height / 2) * .22}px`; this.magnetico = boton; }
    const tarjeta = objetivo?.closest<HTMLElement>('.kpi, .tarjeta-item');
    if (this.inclinado && this.inclinado !== tarjeta) this.soltar();
    if (tarjeta) {
      const r = tarjeta.getBoundingClientRect(), dx = (e.clientX - r.left) / r.width - .5, dy = (e.clientY - r.top) / r.height - .5;
      tarjeta.style.rotate = `${-dy} ${dx} 0 ${Math.hypot(dx, dy) * 9}deg`;
      this.inclinado = tarjeta;
    }
  }

  /** Onda de luz al pulsar un botón y clic suave (si el sonido está activo). */
  private onda(e: PointerEvent): void {
    const b = (e.target as HTMLElement | null)?.closest<HTMLElement>('.boton-primario, .boton-suave');
    if (!b || (b as HTMLButtonElement).disabled) return;
    this.sonido.clic();
    if (this.quieto()) return;
    const r = b.getBoundingClientRect(), d = Math.max(r.width, r.height) * 2;
    const s = document.createElement('span');
    s.className = 'onda'; s.style.cssText = `width:${d}px;height:${d}px;left:${e.clientX - r.left - d / 2}px;top:${e.clientY - r.top - d / 2}px`;
    b.appendChild(s); s.addEventListener('animationend', () => s.remove());
  }

  private soltar(): void {
    if (this.magnetico) { this.magnetico.style.translate = ''; this.magnetico = null; } if (this.inclinado) { this.inclinado.style.rotate = ''; this.inclinado = null; } }
}
