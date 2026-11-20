import { Injectable, NgZone, inject } from '@angular/core';

/**
 * Efectos de puntero para todo el sitio, con un solo oyente: foco de luz que sigue al cursor sobre el cristal
 * (variables --mx/--my) e inclinación 3D suave en tarjetas (.kpi, .tarjeta-item). Se apagan con «reducir animaciones».
 */
@Injectable({ providedIn: 'root' })
export class EfectosService {
  private readonly zona = inject(NgZone);
  private pendiente: PointerEvent | null = null;
  private inclinado: HTMLElement | null = null;

  iniciar(): void {
    if (typeof document === 'undefined' || matchMedia('(pointer: coarse)').matches) return;
    this.zona.runOutsideAngular(() => {
      document.addEventListener('pointermove', (e) => { if (!this.pendiente) requestAnimationFrame(() => this.aplicar()); this.pendiente = e; }, { passive: true });
      document.addEventListener('pointerleave', () => this.soltar(), { passive: true });
    });
  }

  private quieto(): boolean { return document.documentElement.hasAttribute('data-quieto') || matchMedia('(prefers-reduced-motion: reduce)').matches; }

  private aplicar(): void {
    const e = this.pendiente; this.pendiente = null;
    if (!e || this.quieto()) return;
    const objetivo = e.target as HTMLElement | null;
    const vidrio = objetivo?.closest<HTMLElement>('.cristal');
    if (vidrio) { const r = vidrio.getBoundingClientRect(); vidrio.style.setProperty('--mx', `${e.clientX - r.left}px`); vidrio.style.setProperty('--my', `${e.clientY - r.top}px`); }
    const tarjeta = objetivo?.closest<HTMLElement>('.kpi, .tarjeta-item');
    if (this.inclinado && this.inclinado !== tarjeta) this.soltar();
    if (tarjeta) {
      const r = tarjeta.getBoundingClientRect(), dx = (e.clientX - r.left) / r.width - .5, dy = (e.clientY - r.top) / r.height - .5;
      tarjeta.style.rotate = `${-dy} ${dx} 0 ${Math.hypot(dx, dy) * 9}deg`;
      this.inclinado = tarjeta;
    }
  }

  private soltar(): void { if (this.inclinado) { this.inclinado.style.rotate = ''; this.inclinado = null; } }
}
